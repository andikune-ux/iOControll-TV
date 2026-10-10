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
 * Protokol lengkap sesuai pairingmessage.proto:
 * 1. TLS connect ke TV port 6467
 * 2. HP → TV : PairingRequest (service_name + client_name)
 * 3. TV → HP : PairingRequestAck (server_name)
 * 4. TV → HP : PairingOption (input_encodings + output_encodings)
 * 5. HP → TV : PairingConfiguration (encoding dipilih + client_role="1")
 * 6. TV → HP : PairingConfigurationAck     ← INI yang trigger PIN muncul
 * 7. TV     : TAMPILKAN PIN 6 digit
 * 8. HP → TV : PairingSecret (SPAKE2 dari password=PIN)
 * 9. TV → HP : PairingSecretAck
 * 10. HP    : verifikasi + simpan cert TV
 */
class AndroidTvPairingClient(private val context: Context) {

    companion object {
        private const val TAG = "AtvPairing"
        private const val PORT = 6467
        private const val SERVICE_NAME = "atvremote"
        private const val CLIENT_NAME = "atvremote"
        private const val CLIENT_ROLE = "1"  // 1 = HP sebagai client
    }

    private var socket: SSLSocket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var spake2: Spake2? = null

    @Volatile
    var isPaired: Boolean = false
        private set

    @Volatile
    var lastError: String = ""
        private set

    /**
     * Langkah 1 — Buka koneksi TLS, jalan sampai PairingConfigurationAck.
     * Setelah fungsi ini return true, TV SUDAH menampilkan PIN.
     */
    suspend fun startPairing(host: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                TlsHelper.init()
                val sslContext: SSLContext = TlsHelper.buildSslContext(context)

                // ── 1. Connect TLS ─────────────────────────────
                val s = sslContext.socketFactory.createSocket() as SSLSocket
                s.connect(InetSocketAddress(host, PORT), 5000)
                s.startHandshake()
                socket = s
                input = DataInputStream(s.getInputStream())
                output = DataOutputStream(s.getOutputStream())

                // ── 2. Kirim PairingRequest ────────────────────
                val request = PairingMessageProto.PairingMessage.newBuilder()
                    .setPairingRequest(
                        PairingMessageProto.PairingRequest.newBuilder()
                            .setServiceName(SERVICE_NAME)
                            .setClientName(CLIENT_NAME)
                            .build()
                    )
                    .build()
                sendMessage(request)
                Log.d(TAG, "→ PairingRequest terkirim")

                // ── 3. Baca PairingRequestAck ──────────────────
                val ack = readMessage()
                if (ack == null) {
                    lastError = "TV tidak merespons pairing request"
                    disconnect()
                    return@withContext false to lastError
                }
                if (!ack.hasPairingRequestAck()) {
                    lastError = "TV menolak pairing request"
                    disconnect()
                    return@withContext false to lastError
                }
                Log.d(TAG, "← PairingRequestAck: server=${ack.pairingRequestAck.serverName}")

                // ── 4. Baca PairingOption ──────────────────────
                val option = readMessage()
                if (option == null || !option.hasPairingOption()) {
                    lastError = "TV tidak mengirim PairingOption"
                    disconnect()
                    return@withContext false to lastError
                }
                val inputEnc  = option.pairingOption.inputEncodings
                val outputEnc = option.pairingOption.outputEncodings
                Log.d(TAG, "← PairingOption: input='$inputEnc' output='$outputEnc'")

                // Pilih encoding dari yang TV sediakan
                val chosen = pickEncoding(inputEnc, outputEnc)
                Log.d(TAG, "Encoding dipilih: $chosen")

                // ── 5. Kirim PairingConfiguration ──────────────
                val config = PairingMessageProto.PairingMessage.newBuilder()
                    .setPairingConfiguration(
                        PairingMessageProto.PairingConfiguration.newBuilder()
                            .setEncoding(chosen)
                            .setClientRole(CLIENT_ROLE)
                            .build()
                    )
                    .build()
                sendMessage(config)
                Log.d(TAG, "→ PairingConfiguration terkirim (encoding=$chosen)")

                // ── 6. Baca PairingConfigurationAck ────────────
                //     Setelah ack ini TV menampilkan PIN di layar
                val configAck = readMessage()
                if (configAck == null) {
                    lastError = "TV tidak balas PairingConfiguration"
                    disconnect()
                    return@withContext false to lastError
                }
                if (!configAck.hasPairingConfigurationAck()) {
                    lastError = "Respons TV bukan PairingConfigurationAck"
                    disconnect()
                    return@withContext false to lastError
                }
                Log.d(TAG, "← PairingConfigurationAck diterima — TV menampilkan PIN")

                // ── 7. Siap terima PIN ─────────────────────────
                true to "Masukkan PIN yang tampil di layar TV"
            } catch (e: Exception) {
                e.printStackTrace()
                lastError = "Error connect: ${e.message}"
                disconnect()
                false to lastError
            }
        }
    }

    /**
     * Langkah 2 — Kirim PIN ke TV (SPAKE2).
     */
    suspend fun sendPin(pin: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                if (output == null) {
                    return@withContext false to "Belum connect ke TV"
                }

                val s2 = Spake2(
                    isClient = true,
                    myName = CLIENT_NAME.toByteArray(),
                    theirName = SERVICE_NAME.toByteArray(),
                    password = pin.toByteArray()
                )
                spake2 = s2

                val spakeMsg = s2.start()

                val secretMsg = PairingMessageProto.PairingMessage.newBuilder()
                    .setPairingSecret(
                        PairingMessageProto.PairingSecret.newBuilder()
                            .setSecret(com.google.protobuf.ByteString.copyFrom(spakeMsg))
                            .build()
                    )
                    .build()
                sendMessage(secretMsg)
                Log.d(TAG, "→ PairingSecret terkirim")

                val response = readMessage()
                if (response == null || !response.hasPairingSecretAck()) {
                    lastError = "TV tolak pairing (PIN salah?)"
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

    /**
     * Disconnect socket.
     */
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

    // ==========================================================
    // INTERNAL
    // ==========================================================

    /**
     * Pilih encoding yang akan dikirim ke TV.
     * TV bisa kirim CSV (mis. "HEX,BASE64") — kita prioritaskan HEX.
     */
    private fun pickEncoding(inputRaw: String, outputRaw: String): String {
        val all = (inputRaw + "," + outputRaw)
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        // Prioritas HEX
        all.firstOrNull { it.equals("HEX", ignoreCase = true) }?.let { return "HEX" }
        // Fallback: yang pertama tersedia
        return all.firstOrNull() ?: "HEX"
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
