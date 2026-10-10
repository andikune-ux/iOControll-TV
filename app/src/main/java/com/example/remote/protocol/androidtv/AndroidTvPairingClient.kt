package dev.andikuneiocontroll.remote.protocol.androidtv

import android.content.Context
import android.util.Log
import dev.andikuneiocontroll.remote.protocol.androidtv.proto.PairingMessageProto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

/**
 * AndroidTvPairingClient — Handle pairing flow Android TV Remote v2.
 *
 * V3.1 — Simple socket, no bindSocket, log super detail.
 */
class AndroidTvPairingClient(private val context: Context) {

    companion object {
        private const val TAG = "AtvPairing"
        private const val PORT = 6467
        private const val SERVICE_NAME = "atvremote"
        private const val PROTOCOL_VERSION = 2
        private const val STATUS_OK = 200
        private const val CLIENT_ROLE_INPUT = 1
        private const val ENC_ALPHANUMERIC = 1
        private const val SYMBOL_LENGTH = 6

        private const val CONNECT_TIMEOUT_MS = 15000
        private const val READ_TIMEOUT_MS = 15000

        private fun isIpv6(host: String): Boolean = host.contains(":")
        private fun isIpv6LinkLocal(host: String): Boolean {
            val lower = host.lowercase()
            return lower.startsWith("fe80:") || lower.startsWith("fe80%")
        }
    }

    private var socket: SSLSocket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var spake2: Spake2? = null

    private val clientName: String = TlsHelper.getOrCreateClientName(context)

    @Volatile var isPaired: Boolean = false
        private set

    @Volatile var lastError: String = ""
        private set

    suspend fun startPairing(host: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "========== START PAIRING ==========")
                Log.d(TAG, "host=$host, port=$PORT, client='$clientName'")

                if (host.isBlank()) {
                    lastError = "Host kosong"
                    return@withContext false to lastError
                }
                if (isIpv6(host)) {
                    lastError = if (isIpv6LinkLocal(host)) {
                        "Alamat IPv6 link-local tidak didukung. Pilih TV yang pakai IPv4."
                    } else {
                        "Alamat IPv6 tidak didukung. Pilih TV yang pakai IPv4."
                    }
                    return@withContext false to lastError
                }

                TlsHelper.init()
                val sslContext: SSLContext = TlsHelper.buildSslContext(context)
                Log.d(TAG, "SSLContext = ${sslContext.protocol}")

                // ── 1. Bikin SSLSocket ──────────────────────────────
                Log.d(TAG, "[1/4] Creating SSLSocket…")
                val s = sslContext.socketFactory.createSocket() as SSLSocket
                s.soTimeout = READ_TIMEOUT_MS

                // ── 2. TCP connect ──────────────────────────────────
                Log.d(TAG, "[2/4] TCP connect ke $host:$PORT (timeout=${CONNECT_TIMEOUT_MS}ms)…")
                val t0 = System.currentTimeMillis()
                s.connect(InetSocketAddress(host, PORT), CONNECT_TIMEOUT_MS)
                val connectMs = System.currentTimeMillis() - t0
                Log.d(TAG, "[2/4] ✅ TCP connected dalam ${connectMs}ms")

                // ── 3. TLS handshake ────────────────────────────────
                Log.d(TAG, "[3/4] TLS handshake start…")
                val t1 = System.currentTimeMillis()
                s.startHandshake()
                val hsMs = System.currentTimeMillis() - t1
                Log.d(TAG, "[3/4] ✅ TLS handshake OK dalam ${hsMs}ms")

                socket = s
                input = DataInputStream(s.getInputStream())
                output = DataOutputStream(s.getOutputStream())

                // ── 4. Kirim PairingRequest ─────────────────────────
                Log.d(TAG, "[4/4] Kirim PairingRequest…")
                val request = PairingMessageProto.PairingMessage.newBuilder()
                    .setProtocolVersion(PROTOCOL_VERSION)
                    .setStatus(STATUS_OK)
                    .setPairingRequest(
                        PairingMessageProto.PairingRequest.newBuilder()
                            .setServiceName(SERVICE_NAME)
                            .setClientName(clientName)
                            .build()
                    )
                    .build()
                sendMessage(request)
                Log.d(TAG, "[4/4] ✅ PairingRequest terkirim (${request.serializedSize} bytes)")

                // ── 5. Baca PairingRequestAck ───────────────────────
                Log.d(TAG, "Menunggu PairingRequestAck…")
                val ack = readMessage()
                if (ack == null) {
                    lastError = "TV tidak merespons pairing request"
                    disconnect()
                    return@withContext false to lastError
                }
                logMessageFields("PairingRequestAck", ack)

                if (ack.status != 0 && ack.status != STATUS_OK) {
                    lastError = "TV tolak (status=${ack.status})"
                    disconnect()
                    return@withContext false to lastError
                }
                if (!ack.hasPairingRequestAck()) {
                    lastError = "TV kirim message tak dikenal (status=${ack.status})"
                    disconnect()
                    return@withContext false to lastError
                }
                Log.d(TAG, "✅ PairingRequestAck: server='${ack.pairingRequestAck.serverName}'")

                // ── 6. Baca PairingOption ───────────────────────────
                Log.d(TAG, "Menunggu PairingOption…")
                val option = readMessage()
                if (option == null) {
                    lastError = "TV tidak kirim PairingOption"
                    disconnect()
                    return@withContext false to lastError
                }
                logMessageFields("PairingOption", option)

                if (!option.hasPairingOption()) {
                    lastError = "Respons TV bukan PairingOption (status=${option.status})"
                    disconnect()
                    return@withContext false to lastError
                }
                Log.d(TAG, "✅ PairingOption: input=${option.pairingOption.inputEncodingsList.size}, output=${option.pairingOption.outputEncodingsList.size}")

                // ── 7. Kirim PairingConfiguration ───────────────────
                Log.d(TAG, "Kirim PairingConfiguration…")
                val encoding = PairingMessageProto.PairingEncoding.newBuilder()
                    .setType(ENC_ALPHANUMERIC)
                    .setSymbolLength(SYMBOL_LENGTH)
                    .build()

                val config = PairingMessageProto.PairingMessage.newBuilder()
                    .setProtocolVersion(PROTOCOL_VERSION)
                    .setStatus(STATUS_OK)
                    .setPairingConfiguration(
                        PairingMessageProto.PairingConfiguration.newBuilder()
                            .setEncoding(encoding)
                            .setClientRole(CLIENT_ROLE_INPUT)
                            .build()
                    )
                    .build()
                sendMessage(config)
                Log.d(TAG, "✅ PairingConfiguration terkirim")

                // ── 8. Baca PairingConfigurationAck ─────────────────
                Log.d(TAG, "Menunggu PairingConfigurationAck…")
                val configAck = readMessage()
                if (configAck == null) {
                    lastError = "TV tidak balas PairingConfiguration"
                    disconnect()
                    return@withContext false to lastError
                }
                logMessageFields("PairingConfigurationAck", configAck)

                if (!configAck.hasPairingConfigurationAck()) {
                    lastError = "Respons TV bukan PairingConfigurationAck (status=${configAck.status})"
                    disconnect()
                    return@withContext false to lastError
                }

                Log.d(TAG, "========== ✅ PAIRING READY — TV tampilkan kode ==========")
                true to "Masukkan kode yang tampil di layar TV"
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "❌ TIMEOUT: ${e.message}")
                lastError = "Timeout connect ke TV (${CONNECT_TIMEOUT_MS / 1000}s). Cek: HP & TV di WiFi sama? AP Isolation OFF?"
                disconnect()
                false to lastError
            } catch (e: java.net.ConnectException) {
                Log.e(TAG, "❌ CONNECTION REFUSED: ${e.message}")
                lastError = "TV tolak koneksi (port $PORT). Kemungkinan: firewall TV / TV di deep sleep"
                disconnect()
                false to lastError
            } catch (e: javax.net.ssl.SSLException) {
                Log.e(TAG, "❌ SSL ERROR: ${e.message}")
                lastError = "TLS gagal: ${e.message}"
                disconnect()
                false to lastError
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR: ${e::class.simpleName} — ${e.message}")
                e.printStackTrace()
                lastError = "Error: ${e.message}"
                disconnect()
                false to lastError
            }
        }
    }

    suspend fun sendPin(pin: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                if (output == null) {
                    return@withContext false to "Belum connect ke TV"
                }

                val s2 = Spake2(
                    isClient = true,
                    myName = clientName.toByteArray(),
                    theirName = SERVICE_NAME.toByteArray(),
                    password = pin.toByteArray()
                )
                spake2 = s2

                val spakeMsg = s2.start()

                val secretMsg = PairingMessageProto.PairingMessage.newBuilder()
                    .setProtocolVersion(PROTOCOL_VERSION)
                    .setStatus(STATUS_OK)
                    .setPairingSecret(
                        PairingMessageProto.PairingSecret.newBuilder()
                            .setSecret(com.google.protobuf.ByteString.copyFrom(spakeMsg))
                            .build()
                    )
                    .build()
                sendMessage(secretMsg)
                Log.d(TAG, "→ PairingSecret terkirim")

                val response = readMessage()
                if (response == null) {
                    lastError = "TV tidak balas PairingSecret"
                    disconnect()
                    return@withContext false to lastError
                }
                logMessageFields("PairingSecretAck", response)

                if (!response.hasPairingSecretAck()) {
                    lastError = "TV tolak pairing (kode salah?) status=${response.status}"
                    disconnect()
                    return@withContext false to lastError
                }
                Log.d(TAG, "← PairingSecretAck diterima")

                val tvSecret = response.pairingSecretAck.secret.toByteArray()
                s2.finish(tvSecret)

                try {
                    val session = socket?.session
                    val peerCerts = session?.peerCertificates
                    if (peerCerts != null && peerCerts.isNotEmpty()) {
                        TlsHelper.saveServerCertificate(context, peerCerts[0] as X509Certificate)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gagal simpan cert TV: ${e.message}")
                }

                isPaired = true
                true to "Pairing berhasil!"
            } catch (e: Exception) {
                e.printStackTrace()
                lastError = "Error pairing: ${e.message}"
                disconnect()
                false to lastError
            }
        }
    }

    fun disconnect() {
        try {
            input?.close()
            output?.close()
            socket?.close()
        } catch (_: Exception) {}
        input = null
        output = null
        socket = null
    }

    private fun logMessageFields(label: String, msg: PairingMessageProto.PairingMessage) {
        try {
            val fields = buildString {
                append("proto=").append(msg.protocolVersion)
                append(", status=").append(msg.status)
                if (msg.hasPairingRequest()) append(", request=yes")
                if (msg.hasPairingRequestAck()) append(", ack=yes")
                if (msg.hasPairingOption()) append(", option=yes")
                if (msg.hasPairingConfiguration()) append(", config=yes")
                if (msg.hasPairingConfigurationAck()) append(", configAck=yes")
                if (msg.hasPairingSecret()) append(", secret=yes")
                if (msg.hasPairingSecretAck()) append(", secretAck=yes")
            }
            Log.d(TAG, "← $label {$fields}")
        } catch (e: Exception) {
            Log.w(TAG, "logMessageFields error: ${e.message}")
        }
    }

    private fun sendMessage(message: PairingMessageProto.PairingMessage) {
        val bytes = message.toByteArray()
        writeVarint(bytes.size)
        output?.write(bytes)
        output?.flush()
    }

    private fun readMessage(): PairingMessageProto.PairingMessage? {
        return try {
            val length = readVarint() ?: return null
            if (length <= 0 || length > 1024 * 1024) return null
            val buffer = ByteArray(length)
            input?.readFully(buffer)
            PairingMessageProto.PairingMessage.parseFrom(buffer)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeVarint(value: Int) {
        var v = value
        while (v and 0x7F.inv() != 0) {
            output?.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        output?.writeByte(v and 0x7F)
    }

    private fun readVarint(): Int? {
        var result = 0
        var shift = 0
        while (shift < 32) {
            val b = input?.readByte()?.toInt() ?: return null
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
        return null
    }
}
