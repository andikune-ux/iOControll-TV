package dev.andikuneiocontroll.remote.protocol.androidtv

import android.content.Context
import android.provider.Settings
import android.util.Log
import dev.andikuneiocontroll.remote.protocol.androidtv.proto.PairingMessageProto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

/**
 * AndroidTvPairingClient — Handle pairing flow Android TV Remote v2.
 *
 * Protokol lengkap sesuai pairingmessage.proto:
 * 1. TLS connect ke TV port 6467  (WAJIB IPv4, IPv6 link-local tidak support)
 * 2. HP → TV : PairingRequest (service_name + client_name UNIK)
 * 3. TV → HP : PairingRequestAck (server_name)   status=200 = OK
 * 4. TV → HP : PairingOption (input_encodings + output_encodings)
 * 5. HP → TV : PairingConfiguration (encoding + client_role="1")
 * 6. TV → HP : PairingConfigurationAck   ← trigger PIN muncul di TV
 * 7. TV     : TAMPILKAN PIN 6 karakter
 * 8. HP → TV : PairingSecret (SPAKE2)
 * 9. TV → HP : PairingSecretAck
 *
 * PENTING: client_name HARUS unik per HP supaya TV tidak menolak
 * pairing dengan status=2 (name collision dengan client lain).
 * client_name di-generate sekali & disimpan di SharedPreferences.
 */
class AndroidTvPairingClient(private val context: Context) {

    companion object {
        private const val TAG = "AtvPairing"
        private const val PORT = 6467
        private const val SERVICE_NAME = "atvremote"   // WAJIB "atvremote"
        private const val CLIENT_ROLE = "1"

        private const val PREFS_NAME = "atv_pairing_prefs"
        private const val KEY_CLIENT_NAME = "client_name"

        /** Cek apakah host adalah IPv6 (mengandung ':') */
        private fun isIpv6(host: String): Boolean = host.contains(":")

        /** Cek apakah host IPv6 link-local (fe80::/10) */
        private fun isIpv6LinkLocal(host: String): Boolean {
            val lower = host.lowercase()
            return lower.startsWith("fe80:") || lower.startsWith("fe80%")
        }
    }

    private var socket: SSLSocket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var spake2: Spake2? = null

    /** client_name unik — di-resolve sekali di constructor */
    private val clientName: String = resolveClientName(context)

    @Volatile
    var isPaired: Boolean = false
        private set

    @Volatile
    var lastError: String = ""
        private set

    /**
     * Langkah 1 — Buka koneksi TLS, kirim request, tunggu PairingConfigurationAck.
     * Setelah fungsi ini return true, TV SUDAH menampilkan kode.
     */
    suspend fun startPairing(host: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                // ── Validasi host ──────────────────────────────
                if (host.isBlank()) {
                    lastError = "Host kosong"
                    return@withContext false to lastError
                }
                if (isIpv6(host)) {
                    lastError = if (isIpv6LinkLocal(host)) {
                        "Alamat IPv6 link-local tidak didukung. Pilih TV yang pakai IPv4 (contoh: 192.168.x.x)"
                    } else {
                        "Alamat IPv6 tidak didukung. Pilih TV yang pakai IPv4."
                    }
                    Log.w(TAG, "Tolak host IPv6: $host")
                    return@withContext false to lastError
                }

                TlsHelper.init()
                val sslContext: SSLContext = TlsHelper.buildSslContext(context)

                // ── 1. Connect TLS ─────────────────────────────
                val s = sslContext.socketFactory.createSocket() as SSLSocket
                s.connect(InetSocketAddress(host, PORT), 5000)
                s.startHandshake()
                socket = s
                input = DataInputStream(s.getInputStream())
                output = DataOutputStream(s.getOutputStream())
                Log.d(TAG, "TLS connected ke $host:$PORT (client=$clientName)")

                // ── 2. Kirim PairingRequest ────────────────────
                val request = PairingMessageProto.PairingMessage.newBuilder()
                    .setPairingRequest(
                        PairingMessageProto.PairingRequest.newBuilder()
                            .setServiceName(SERVICE_NAME)
                            .setClientName(clientName)
                            .build()
                    )
                    .build()
                sendMessage(request)
                Log.d(TAG, "→ PairingRequest terkirim (service=$SERVICE_NAME, client=$clientName)")

                // ── 3. Baca PairingRequestAck ──────────────────
                val ack = readMessage()
                if (ack == null) {
                    lastError = "TV tidak merespons pairing request"
                    disconnect()
                    return@withContext false to lastError
                }
                logMessageFields("PairingRequestAck", ack)

                // Cek status dulu — status != 0/200 = TV tolak di level protokol
                if (ack.status != 0 && ack.status != 200) {
                    lastError = "TV tolak pairing (status=${ack.status}) — coba hapus data pairing lama di TV"
                    disconnect()
                    return@withContext false to lastError
                }

                if (!ack.hasPairingRequestAck()) {
                    lastError = "TV kirim message tidak dikenal (status=${ack.status})"
                    disconnect()
                    return@withContext false to lastError
                }
                Log.d(TAG, "← PairingRequestAck: server='${ack.pairingRequestAck.serverName}'")

                // ── 4. Baca PairingOption ──────────────────────
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
                val inputEnc  = option.pairingOption.inputEncodings
                val outputEnc = option.pairingOption.outputEncodings
                Log.d(TAG, "← PairingOption: input='$inputEnc' output='$outputEnc'")

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
                Log.d(TAG, "→ PairingConfiguration terkirim (encoding=$chosen, role=$CLIENT_ROLE)")

                // ── 6. Baca PairingConfigurationAck ────────────
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

                Log.d(TAG, "← PairingConfigurationAck diterima — TV menampilkan kode")
                true to "Masukkan kode yang tampil di layar TV"
            } catch (e: Exception) {
                e.printStackTrace()
                lastError = "Error connect: ${e.message}"
                disconnect()
                false to lastError
            }
        }
    }

    /**
     * Langkah 2 — Kirim kode dari TV ke HP (SPAKE2).
     */
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
     * Resolve client_name unik & persisten.
     * Disimpan di SharedPreferences supaya konsisten antar sesi —
     * TV mengenali HP yang sama setelah pairing.
     */
    private fun resolveClientName(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_CLIENT_NAME, null)
        if (!existing.isNullOrBlank()) return existing

        val name = buildClientName(ctx)
        prefs.edit().putString(KEY_CLIENT_NAME, name).apply()
        Log.d(TAG, "Client name baru di-generate: $name")
        return name
    }

    /**
     * Build client_name unik.
     * Format: 32 hex char (16 byte random) — sama seperti Google TV official app.
     * Seed dari ANDROID_ID + timestamp + UUID biar dijamin unik.
     */
    private fun buildClientName(ctx: Context): String {
        val androidId = try {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) { null }

        // Random UUID hex (32 char) — dijamin unik
        val uuidHex = UUID.randomUUID().toString().replace("-", "").lowercase()

        // Log seed untuk debugging (jangan dipakai sebagai nama)
        Log.d(TAG, "Seed: androidId=${androidId?.take(6)}… timestamp=${System.currentTimeMillis()}")

        return uuidHex.take(32)
    }

    /** Pilih encoding dari yang TV sediakan — prioritas HEX. */
    private fun pickEncoding(inputRaw: String, outputRaw: String): String {
        val all = (inputRaw + "," + outputRaw)
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        all.firstOrNull { it.equals("HEX", ignoreCase = true) }?.let { return "HEX" }
        return all.firstOrNull() ?: "HEX"
    }

    /** Log semua field PairingMessage yang terisi — untuk debugging. */
    private fun logMessageFields(label: String, msg: PairingMessageProto.PairingMessage) {
        try {
            val fields = buildString {
                append("status=").append(msg.status)
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
