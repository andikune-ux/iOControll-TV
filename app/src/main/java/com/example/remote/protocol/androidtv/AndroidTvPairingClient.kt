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
 * Urutan protokol yang BENAR (Remote v2):
 * 1. TLS connect ke TV port 6467
 * 2. HP → TV  : PairingRequest (service_name + client_name)
 * 3. TV → HP  : PairingRequestAck
 * 4. TV → HP  : PairingOption (encoding yang TV terima)
 * 5. HP → TV  : PairingConfiguration (encoding + client_role)
 * 6. TV       : TAMPILKAN PIN 6 digit di layar
 * 7. HP → TV  : PairingSecret (SPAKE2 message dari password=PIN)
 * 8. TV → HP  : PairingSecretAck (SPAKE2 message TV)
 * 9. HP       : verifikasi + simpan cert TV
 */
class AndroidTvPairingClient(private val context: Context) {

    companion object {
        private const val TAG = "AtvPairing"
        private const val PORT = 6467
        private const val SERVICE_NAME = "atvremote"
        private const val CLIENT_NAME = "atvremote"
        // Timeout per-message (ms)
        private const val READ_TIMEOUT_MS = 5000
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
     * Langkah 1 — Buka koneksi TLS + kirim pairing request + tunggu PairingOption.
     * Setelah fungsi ini return true, TV SUDAH menampilkan PIN di layar.
     */
    suspend fun startPairing(host: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                TlsHelper.init()

                // 1. Buat SSLContext dari TlsHelper
                val sslContext: SSLContext = TlsHelper.buildSslContext(context)

                // 2. Connect TLS ke TV
                val s = sslContext.socketFactory.createSocket() as SSLSocket
                s.connect(InetSocketAddress(host, PORT), 5000)
                s.startHandshake()

                socket = s
                input = DataInputStream(s.getInputStream())
                output = DataOutputStream(s.getOutputStream())

                // 3. Kirim PairingRequest
                val request = PairingMessageProto.PairingMessage.newBuilder()
                    .setPairingRequest(
                        PairingMessageProto.PairingRequest.newBuilder()
                            .setServiceName(SERVICE_NAME)
                            .setClientName(CLIENT_NAME)
                            .build()
                    )
                    .build()
                sendMessage(request)

                // 4. Baca PairingRequestAck dari TV
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
                Log.d(TAG, "PairingRequestAck diterima")

                // 5. Baca PairingOption dari TV — INI LANGKAH YANG HILANG SEBELUMNYA
                val option = readMessage()
                if (option == null) {
                    lastError = "TV tidak mengirim PairingOption"
                    disconnect()
                    return@withContext false to lastError
                }
                if (!option.hasPairingOption()) {
                    lastError = "Respons TV bukan PairingOption"
                    disconnect()
                    return@withContext false to lastError
                }

                // 6. Ambil encoding yang TV sediakan (fallback "HEX")
                val encodingFromTv = try {
                    val enc = option.pairingOption.encoding
                    if (enc.isNullOrEmpty()) "HEX" else enc
                } catch (_: Exception) {
                    "HEX"
                }
                Log.d(TAG, "PairingOption encoding=$encodingFromTv")

                // 7. Kirim PairingConfiguration pakai encoding dari TV
                val config = PairingMessageProto.PairingMessage.newBuilder()
                    .setPairingConfiguration(
                        PairingMessageProto.PairingConfiguration.newBuilder()
                            .setEncoding(encodingFromTv)
                            .setClientRole("1")
                            .build()
                    )
                    .build()
                sendMessage(config)

                // 8. Baca respons TV — biasanya PairingOption lagi,
                //    atau TV sudah langsung menampilkan PIN.
                //    Kalau dapat PairingOption/ack, anggap sukses.
                val confirm = readMessage()
                if (confirm == null) {
                    lastError = "TV tidak konfirmasi PairingConfiguration"
                    disconnect()
                    return@withContext false to lastError
                }
                Log.d(TAG, "Respons setelah config: " +
                    "ack=${confirm.hasPairingRequestAck()}, " +
                    "option=${confirm.hasPairingOption()}, " +
                    "secretAck=${confirm.hasPairingSecretAck()}")

                // 9. Sampai sini TV sudah menampilkan PIN di layar
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
     * Langkah 2 — Kirim PIN dari TV ke HP.
     * Setelah TV tampilkan PIN, panggil ini.
     */
    suspend fun sendPin(pin: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                if (output == null) {
                    return@withContext false to "Belum connect ke TV"
                }

                // 1. Init SPAKE2 dengan password = PIN
                val s2 = Spake2(
                    isClient = true,
                    myName = CLIENT_NAME.toByteArray(),
                    theirName = SERVICE_NAME.toByteArray(),
                    password = pin.toByteArray()
                )
                spake2 = s2

                // 2. Generate SPAKE2 message pertama
                val spakeMsg = s2.start()

                // 3. Kirim PairingSecret ke TV
                val secretMsg = PairingMessageProto.PairingMessage.newBuilder()
                    .setPairingSecret(
                        PairingMessageProto.PairingSecret.newBuilder()
                            .setSecret(com.google.protobuf.ByteString.copyFrom(spakeMsg))
                            .build()
                    )
                    .build()
                sendMessage(secretMsg)

                // 4. Tunggu PairingSecretAck dari TV
                val response = readMessage()
                if (response == null || !response.hasPairingSecretAck()) {
                    lastError = "TV tolak pairing (PIN salah?)"
                    disconnect()
                    return@withContext false to lastError
                }

                // 5. Hitung shared key dari SPAKE2 message TV
                val tvSecret = response.pairingSecretAck.secret.toByteArray()
                s2.finish(tvSecret)

                // 6. Simpan cert TV untuk auto-trust berikutnya
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
    // INTERNAL: Read / Write message
    // ==========================================================

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
