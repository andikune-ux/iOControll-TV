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
 * AdbClient — Client koneksi ADB ke TV Android.
 *
 * V2 (Update):
 * - Support Wireless Debugging pairing (Android 11+)
 * - Auto-detect: kalau belum paired → minta pairing dulu
 * - Kalau sudah paired → langsung connect
 *
 * Alur:
 * 1. Cek isPaired(host) → kalau belum, user harus pairing
 * 2. Pairing: buka AdbPairing.pair() dengan code 6 digit
 * 3. Connect: buka socket ke port 5555 (atau custom)
 * 4. Handshake: kirim CNXN + public key
 * 5. Kalau TV belum recognize key → kirim AUTH
 * 6. Kirim command via OPEN + WRTE
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
     * Cek apakah host sudah pernah di-pair.
     */
    fun isPaired(host: String): Boolean {
        return AdbPairing.isPaired(context, host)
    }

    /**
     * Pair ke TV (sekali saja).
     * Setelah sukses, connect() bisa dipanggil.
     */
    suspend fun pair(
        host: String,
        pairingPort: Int,
        pairingCode: String
    ): Pair<Boolean, String> {
        return AdbPairing.pair(context, host, pairingPort, pairingCode)
    }

    /**
     * Connect ke TV (setelah pairing, atau kalau sudah pernah connect).
     */
    suspend fun connect(ip: String, port: Int = AdbProtocol.DEFAULT_PORT): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                disconnect()

                // Connect socket
                val s = Socket()
                s.connect(InetSocketAddress(ip, port), 5000)
                s.tcpNoDelay = true

                socket = s
                input = DataInputStream(s.getInputStream())
                output = DataOutputStream(s.getOutputStream())

                // Kirim CNXN dengan public key
                val keyPair = AdbCrypto.getOrCreateKeyPair(context)
                val publicKey = AdbCrypto.getAdbPublicKey(keyPair)
                val cnxn = AdbProtocol.buildCnxn(publicKey)
                output?.write(cnxn)
                output?.flush()

                // Tunggu balasan (CNXN = sukses, AUTH = butuh signature)
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

                        else -> {}
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
                lastError = "Timeout: TV tidak merespons. Pastikan Wireless Debugging aktif."
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
     */
    suspend fun sendShell(command: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isConnected || socket == null) {
                    return@withContext false to "Belum terhubung ke TV"
                }

                val localId = localIdCounter.getAndIncrement()

                // OPEN shell
                val openMsg = AdbProtocol.buildOpen(localId, "shell:$command")
                output?.write(openMsg)
                output?.flush()

                // Tunggu OKAY
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
                            val okay = AdbProtocol.buildOkay(localId, header.arg0)
                            output?.write(okay)
                            output?.flush()
                        }
                        AdbProtocol.CMD_CNXN -> {}
                    }
                }

                if (!okayReceived) {
                    return@withContext false to "TV tidak merespons perintah"
                }

                // ACK + CLOSE
                val okayAck = AdbProtocol.buildOkay(localId, remoteId)
                output?.write(okayAck)
                output?.flush()

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
     * Ping TV (cek masih hidup).
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
