package dev.andikuneiocontroll.remote.protocol.adb

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

/**
 * AdbClient — client koneksi ADB ke TV Android.
 *
 * Alur koneksi:
 * 1. Connect socket ke TV:5555
 * 2. Kirim CNXN (connect) + public key
 * 3. Kalau TV belum recognize key → TV kirim AUTH TOKEN
 * 4. Sign token dengan RSA → kirim AUTH SIGNATURE
 * 5. TV accept → kirim CNXN balik
 * 6. Kirim OPEN "shell:..." → dapat OKAY
 * 7. Kirim WRTE dengan command → baca response
 *
 * Setelah terhubung, kita bisa kirim perintah shell seperti:
 * - "input keyevent KEYCODE_DPAD_UP"
 * - "input keyevent KEYCODE_HOME"
 * - "am start -a android.intent.action.VIEW ..."
 * - "input text 'hello'"
 */
class AdbClient(private val context: Context) {

    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null

    private val localIdCounter = AtomicInteger(1)

    @Volatile
    var isConnected: Boolean = false
        private set

    @Volatile
    var lastError: String = ""
        private set

    /**
     * Connect ke TV. Return (sukses, pesan).
     */
    suspend fun connect(ip: String, port: Int = AdbProtocol.DEFAULT_PORT): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                disconnect()

                // 1. Connect socket
                val s = Socket()
                s.connect(InetSocketAddress(ip, port), 5000)
                s.tcpNoDelay = true

                socket = s
                input = DataInputStream(s.getInputStream())
                output = DataOutputStream(s.getOutputStream())

                // 2. Kirim CNXN
                val keyPair = AdbCrypto.getOrCreateKeyPair(context)
                val publicKey = AdbCrypto.getAdbPublicKey(keyPair)
                val cnxn = AdbProtocol.buildCnxn(publicKey)
                output?.write(cnxn)
                output?.flush()

                // 3. Tunggu balasan (loop sampai CNXN atau AUTH)
                var authenticated = false
                var connected = false
                var attempts = 0

                while (!connected && attempts < 10) {
                    attempts++

                    val headerBytes = ByteArray(24)
                    val read = input?.read(headerBytes) ?: 0
                    if (read < 24) {
                        lastError = "Gagal baca header dari TV"
                        return@withContext false to lastError
                    }

                    val header = AdbProtocol.parseHeader(headerBytes)

                    if (!AdbProtocol.isMagicValid(header)) {
                        lastError = "Header MAGIC tidak valid"
                        return@withContext false to lastError
                    }

                    val payload = if (header.dataLength > 0) {
                        val buf = ByteArray(header.dataLength)
                        input?.readFully(buf)
                        buf
                    } else ByteArray(0)

                    when (header.command) {
                        AdbProtocol.CMD_CNXN -> {
                            // TV accept
                            connected = true
                        }

                        AdbProtocol.CMD_AUTH -> {
                            if (header.arg0 == AdbProtocol.AUTH_TYPE_TOKEN) {
                                // TV minta signature — sign token
                                val signature = AdbCrypto.signToken(keyPair, payload)
                                val authMsg = AdbProtocol.buildAuth(
                                    AdbProtocol.AUTH_TYPE_SIGNATURE,
                                    signature
                                )
                                output?.write(authMsg)
                                output?.flush()
                            } else if (header.arg0 == AdbProtocol.AUTH_TYPE_RSA_KEY) {
                                // TV belum recognize key → kirim public key
                                val authMsg = AdbProtocol.buildAuth(
                                    AdbProtocol.AUTH_TYPE_RSA_KEY,
                                    publicKey
                                )
                                output?.write(authMsg)
                                output?.flush()
                            }
                        }

                        else -> {
                            // Command lain saat handshake — abaikan
                        }
                    }
                }

                if (!connected) {
                    lastError = "Handshake gagal setelah $attempts percobaan"
                    disconnect()
                    return@withContext false to lastError
                }

                isConnected = true
                lastError = ""
                true to "Terhubung ke TV ($ip:$port)"

            } catch (e: java.net.SocketTimeoutException) {
                lastError = "Timeout: TV tidak merespons. Pastikan ADB Debugging aktif & TV di WiFi sama."
                disconnect()
                false to lastError
            } catch (e: java.net.ConnectException) {
                lastError = "Tidak bisa connect ke TV. Cek IP & port ADB."
                disconnect()
                false to lastError
            } catch (e: Exception) {
                lastError = "Error: ${e.message}"
                disconnect()
                false to lastError
            }
        }
    }

    /**
     * Kirim perintah shell ADB.
     * Contoh: sendShell("input keyevent KEYCODE_HOME")
     */
    suspend fun sendShell(command: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isConnected || socket == null) {
                    return@withContext false to "Belum terhubung ke TV"
                }

                val localId = localIdCounter.getAndIncrement()

                // 1. OPEN shell
                val openMsg = AdbProtocol.buildOpen(localId, "shell:$command")
                output?.write(openMsg)
                output?.flush()

                // 2. Tunggu OKAY dari TV
                var remoteId = -1
                var okayReceived = false
                var loops = 0

                while (!okayReceived && loops < 50) {
                    loops++
                    val headerBytes = ByteArray(24)
                    val read = input?.read(headerBytes) ?: 0
                    if (read < 24) break

                    val header = AdbProtocol.parseHeader(headerBytes)
                    val payload = if (header.dataLength > 0) {
                        val buf = ByteArray(header.dataLength)
                        input?.readFully(buf)
                        buf
                    } else ByteArray(0)

                    when (header.command) {
                        AdbProtocol.CMD_OKAY -> {
                            remoteId = header.arg0
                            okayReceived = true
                        }
                        AdbProtocol.CMD_CLSE -> {
                            return@withContext false to "TV menolak perintah (CLSE)"
                        }
                        AdbProtocol.CMD_WRTE -> {
                            // Output dari command sebelumnya — kirim OKAY
                            val okay = AdbProtocol.buildOkay(localId, header.arg0)
                            output?.write(okay)
                            output?.flush()
                        }
                        AdbProtocol.CMD_CNXN -> {
                            // Ping dari TV — abaikan
                        }
                    }
                }

                if (!okayReceived) {
                    return@withContext false to "TV tidak merespons perintah"
                }

                // 3. Kirim OKAY untuk acknowledge
                val okayAck = AdbProtocol.buildOkay(localId, remoteId)
                output?.write(okayAck)
                output?.flush()

                // 4. Kirim CLSE untuk close session
                val closeMsg = AdbProtocol.buildClose(localId, remoteId)
                output?.write(closeMsg)
                output?.flush()

                true to "Perintah terkirim"

            } catch (e: Exception) {
                false to "Error: ${e.message}"
            }
        }
    }

    /**
     * Disconnect dari TV.
     */
    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                input?.close()
                output?.close()
                socket?.close()
            } catch (_: Exception) {}
            input = null
            output = null
            socket = null
            isConnected = false
        }
    }

    /**
     * Cek masih terkoneksi dengan ping (kirim command kosong).
     */
    suspend fun ping(): Boolean {
        if (!isConnected) return false
        return try {
            val (ok, _) = sendShell("echo ping")
            ok
        } catch (e: Exception) {
            false
        }
    }
}
