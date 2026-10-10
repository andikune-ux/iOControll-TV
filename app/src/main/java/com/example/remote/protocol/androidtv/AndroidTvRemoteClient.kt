package dev.andikuneiocontroll.remote.protocol.androidtv

import android.content.Context
import android.util.Log
import dev.andikuneiocontroll.remote.discovery.DiscoveredTv
import dev.andikuneiocontroll.remote.protocol.TvCommand
import dev.andikuneiocontroll.remote.protocol.TvProtocol
import dev.andikuneiocontroll.remote.protocol.androidtv.proto.RemoteMessageProto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

/**
 * AndroidTvRemoteClient — Client remote control Android TV Remote v2.
 *
 * Flow pairing (2 TAHAP):
 *   Tahap 1: connect(tv, "")     → panggil startPairing() → TV tampilkan kode
 *   Tahap 2: connect(tv, "DF0C4B") → panggil sendPin(code) → TV verifikasi
 *
 * Setelah pairing sukses, connect ke port 6466 untuk kirim perintah.
 */
class AndroidTvRemoteClient(private val context: Context) : TvProtocol {

    companion object {
        private const val TAG = "AtvRemote"
        private const val COMMAND_PORT = 6466
    }

    override val protocolName: String = "Android TV Remote v2"
    override val brand: String = "ANDROID_TV"

    private var socket: SSLSocket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null

    /** Sesi pairing yang sedang aktif — dipakai antar panggilan connect() */
    private var pairingClient: AndroidTvPairingClient? = null

    @Volatile
    private var connected: Boolean = false

    @Volatile
    var needsPairing: Boolean = false
        private set

    @Volatile
    var pairingHost: String = ""
        private set

    // ==========================================================
    // TvProtocol INTERFACE
    // ==========================================================

    override fun canHandle(tv: DiscoveredTv): Boolean {
        return tv.brand.uppercase() in listOf("ANDROID_TV", "GOOGLE_TV", "FIRE_TV")
    }

    override suspend fun connect(
        tv: DiscoveredTv,
        pairingCode: String,
        onResult: (Boolean, String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                pairingHost = tv.ip

                val hasCert = TlsHelper.hasPairingData(context)

                // ==================================================
                // KASUS A — Sudah pernah pairing → langsung connect
                // ==================================================
                if (hasCert && pairingCode.isBlank()) {
                    if (doConnect(tv.ip)) {
                        connected = true
                        needsPairing = false
                        onResult(true, "Terhubung ke TV")
                    } else {
                        // Cert rusak/kadaluarsa → pairing ulang
                        TlsHelper.clearPairingData(context)
                        needsPairing = true
                        onResult(false, "PAIRING_NEEDED")
                    }
                    return@withContext
                }

                // ==================================================
                // KASUS B — Belum ada kode → MULAI pairing
                //           TV akan menampilkan kode di layar
                // ==================================================
                if (pairingCode.isBlank()) {
                    // Kalau sesi pairing sudah aktif, jangan mulai ulang
                    if (pairingClient != null) {
                        needsPairing = true
                        onResult(false, "PAIRING_NEEDED")
                        return@withContext
                    }

                    val client = AndroidTvPairingClient(context)
                    val (ok, msg) = client.startPairing(tv.ip)
                    if (!ok) {
                        client.disconnect()
                        needsPairing = false
                        onResult(false, "Gagal mulai pairing: $msg")
                        return@withContext
                    }

                    // Simpan sesi — TV sudah menampilkan kode
                    pairingClient = client
                    needsPairing = true
                    onResult(false, "PAIRING_NEEDED")
                    return@withContext
                }

                // ==================================================
                // KASUS C — Ada kode dari user → kirim ke TV
                // ==================================================
                val client = pairingClient
                if (client == null) {
                    needsPairing = false
                    onResult(false, "Sesi pairing kadaluarsa, pilih TV ulang")
                    return@withContext
                }

                val (ok, msg) = client.sendPin(pairingCode)
                client.disconnect()
                pairingClient = null

                if (!ok) {
                    needsPairing = true
                    onResult(false, "Kode salah: $msg")
                    return@withContext
                }

                // Pairing sukses → connect ke port 6466
                needsPairing = false
                if (doConnect(tv.ip)) {
                    connected = true
                    onResult(true, "Pairing berhasil!")
                } else {
                    onResult(false, "Pairing OK, gagal connect ke TV")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                input?.close()
                output?.close()
                socket?.close()
            } catch (_: Exception) {}
            try {
                pairingClient?.disconnect()
            } catch (_: Exception) {}
            input = null
            output = null
            socket = null
            pairingClient = null
            connected = false
            needsPairing = false
        }
    }

    override fun isConnected(): Boolean = connected

    override suspend fun sendCommand(command: String, payload: String) {
        if (!connected || output == null) return

        withContext(Dispatchers.IO) {
            try {
                when (command) {
                    TvCommand.MOUSE_MOVE -> sendMouseMove(payload)
                    TvCommand.MOUSE_CLICK -> sendKey(RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_CENTER)
                    TvCommand.MOUSE_RIGHT_CLICK -> sendKey(RemoteMessageProto.RemoteKeyCode.KEYCODE_BACK)
                    TvCommand.MOUSE_SCROLL -> sendMouseScroll(payload)
                    TvCommand.INPUT_TEXT -> sendText(payload)
                    TvCommand.LAUNCH_APP -> sendAppLink(payload)
                    TvCommand.VOICE_START -> sendKey(RemoteMessageProto.RemoteKeyCode.KEYCODE_VOICE_ASSIST)
                    else -> {
                        val keyCode = mapCommandToKeyCode(command)
                        if (keyCode != null) sendKey(keyCode)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================================
    // CONNECT ke port 6466 (command channel)
    // ==========================================================

    private suspend fun doConnect(host: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                TlsHelper.init()
                val sslContext: SSLContext = TlsHelper.buildSslContext(context)
                val s = sslContext.socketFactory.createSocket() as SSLSocket
                s.connect(InetSocketAddress(host, COMMAND_PORT), 5000)
                s.startHandshake()

                socket = s
                input = DataInputStream(s.getInputStream())
                output = DataOutputStream(s.getOutputStream())

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // ==========================================================
    // SEND COMMANDS
    // ==========================================================

    private fun sendKey(keyCode: RemoteMessageProto.RemoteKeyCode) {
        val message = RemoteMessageProto.RemoteMessage.newBuilder()
            .setRemoteKeyCode(keyCode)
            .setRemoteDirection(RemoteMessageProto.RemoteDirection.SHORT)
            .build()
        sendMessage(message)
    }

    private fun sendMouseMove(payload: String) {
        val parts = payload.split("|")
        if (parts.size < 2) return
        val dx = parts[0].toFloatOrNull() ?: 0f
        val dy = parts[1].toFloatOrNull() ?: 0f

        if (Math.abs(dx) > Math.abs(dy)) {
            sendKey(
                if (dx > 0) RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_RIGHT
                else RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_LEFT
            )
        } else {
            sendKey(
                if (dy > 0) RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_DOWN
                else RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_UP
            )
        }
    }

    private fun sendMouseScroll(payload: String) {
        val amount = payload.toFloatOrNull() ?: 0f
        sendKey(
            if (amount > 0) RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_DOWN
            else RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_UP
        )
    }

    private fun sendText(text: String) {
        for (ch in text) {
            val keyCode = charToKeyCode(ch)
            if (keyCode != null) {
                sendKey(keyCode)
            }
        }
    }

    private fun sendAppLink(link: String) {
        val message = RemoteMessageProto.RemoteMessage.newBuilder()
            .setRemoteAppLinkLaunchRequest(
                RemoteMessageProto.RemoteAppLinkLaunchRequest.newBuilder()
                    .setAppLink(link)
                    .build()
            )
            .build()
        sendMessage(message)
    }

    private fun sendMessage(message: RemoteMessageProto.RemoteMessage) {
        try {
            val bytes = message.toByteArray()
            writeVarint(bytes.size)
            output?.write(bytes)
            output?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
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

    // ==========================================================
    // MAPPING COMMAND → KEYCODE
    // ==========================================================

    private fun mapCommandToKeyCode(command: String): RemoteMessageProto.RemoteKeyCode? {
        return when (command) {
            TvCommand.DPAD_UP -> RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_UP
            TvCommand.DPAD_DOWN -> RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_DOWN
            TvCommand.DPAD_LEFT -> RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_LEFT
            TvCommand.DPAD_RIGHT -> RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_RIGHT
            TvCommand.DPAD_OK, TvCommand.DPAD_CENTER -> RemoteMessageProto.RemoteKeyCode.KEYCODE_DPAD_CENTER

            TvCommand.HOME -> RemoteMessageProto.RemoteKeyCode.KEYCODE_HOME
            TvCommand.BACK -> RemoteMessageProto.RemoteKeyCode.KEYCODE_BACK
            TvCommand.RECENTS -> RemoteMessageProto.RemoteKeyCode.KEYCODE_APP_SWITCH
            TvCommand.NOTIFICATIONS -> RemoteMessageProto.RemoteKeyCode.KEYCODE_NOTIFICATION

            TvCommand.POWER -> RemoteMessageProto.RemoteKeyCode.KEYCODE_POWER
            TvCommand.POWER_OFF -> RemoteMessageProto.RemoteKeyCode.KEYCODE_POWER
            TvCommand.POWER_ON -> RemoteMessageProto.RemoteKeyCode.KEYCODE_POWER
            TvCommand.SLEEP -> RemoteMessageProto.RemoteKeyCode.KEYCODE_SLEEP
            TvCommand.WAKE -> RemoteMessageProto.RemoteKeyCode.KEYCODE_WAKEUP

            TvCommand.VOLUME_UP -> RemoteMessageProto.RemoteKeyCode.KEYCODE_VOLUME_UP
            TvCommand.VOLUME_DOWN -> RemoteMessageProto.RemoteKeyCode.KEYCODE_VOLUME_DOWN
            TvCommand.VOLUME_MUTE -> RemoteMessageProto.RemoteKeyCode.KEYCODE_VOLUME_MUTE

            TvCommand.CHANNEL_UP -> RemoteMessageProto.RemoteKeyCode.KEYCODE_CHANNEL_UP
            TvCommand.CHANNEL_DOWN -> RemoteMessageProto.RemoteKeyCode.KEYCODE_CHANNEL_DOWN

            TvCommand.PLAY -> RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_PLAY
            TvCommand.PAUSE -> RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_PAUSE
            TvCommand.PLAY_PAUSE -> RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_PLAY_PAUSE
            TvCommand.STOP -> RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_STOP
            TvCommand.REWIND -> RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_REWIND
            TvCommand.FORWARD -> RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD
            TvCommand.NEXT -> RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_NEXT
            TvCommand.PREVIOUS -> RemoteMessageProto.RemoteKeyCode.KEYCODE_MEDIA_PREVIOUS

            TvCommand.KEY_DELETE -> RemoteMessageProto.RemoteKeyCode.KEYCODE_DEL
            TvCommand.KEY_ENTER -> RemoteMessageProto.RemoteKeyCode.KEYCODE_ENTER

            TvCommand.NUM_0 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_0
            TvCommand.NUM_1 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_1
            TvCommand.NUM_2 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_2
            TvCommand.NUM_3 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_3
            TvCommand.NUM_4 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_4
            TvCommand.NUM_5 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_5
            TvCommand.NUM_6 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_6
            TvCommand.NUM_7 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_7
            TvCommand.NUM_8 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_8
            TvCommand.NUM_9 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_9

            TvCommand.COLOR_RED -> RemoteMessageProto.RemoteKeyCode.KEYCODE_PROG_RED
            TvCommand.COLOR_GREEN -> RemoteMessageProto.RemoteKeyCode.KEYCODE_PROG_GREEN
            TvCommand.COLOR_YELLOW -> RemoteMessageProto.RemoteKeyCode.KEYCODE_PROG_YELLOW
            TvCommand.COLOR_BLUE -> RemoteMessageProto.RemoteKeyCode.KEYCODE_PROG_BLUE

            TvCommand.INPUT_HDMI1 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_TV_INPUT_HDMI_1
            TvCommand.INPUT_HDMI2 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_TV_INPUT_HDMI_2
            TvCommand.INPUT_HDMI3 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_TV_INPUT_HDMI_3
            TvCommand.INPUT_HDMI4 -> RemoteMessageProto.RemoteKeyCode.KEYCODE_TV_INPUT_HDMI_4
            TvCommand.INPUT_TV -> RemoteMessageProto.RemoteKeyCode.KEYCODE_TV

            TvCommand.VOICE_START -> RemoteMessageProto.RemoteKeyCode.KEYCODE_VOICE_ASSIST

            else -> null
        }
    }

    private fun charToKeyCode(ch: Char): RemoteMessageProto.RemoteKeyCode? {
        return when (ch) {
            'a', 'A' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_A
            'b', 'B' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_B
            'c', 'C' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_C
            'd', 'D' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_D
            'e', 'E' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_E
            'f', 'F' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_F
            'g', 'G' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_G
            'h', 'H' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_H
            'i', 'I' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_I
            'j', 'J' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_J
            'k', 'K' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_K
            'l', 'L' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_L
            'm', 'M' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_M
            'n', 'N' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_N
            'o', 'O' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_O
            'p', 'P' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_P
            'q', 'Q' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_Q
            'r', 'R' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_R
            's', 'S' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_S
            't', 'T' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_T
            'u', 'U' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_U
            'v', 'V' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_V
            'w', 'W' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_W
            'x', 'X' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_X
            'y', 'Y' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_Y
            'z', 'Z' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_Z
            '0' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_0
            '1' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_1
            '2' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_2
            '3' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_3
            '4' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_4
            '5' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_5
            '6' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_6
            '7' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_7
            '8' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_8
            '9' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_9
            ' ' -> RemoteMessageProto.RemoteKeyCode.KEYCODE_SPACE
            else -> null
        }
    }
}
