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
 * Setelah pairing sukses (via AndroidTvPairingClient), client ini
 * connect ke TV port 6466 dan kirim perintah:
 * - D-Pad, Home, Back, Recent
 * - Volume Up/Down/Mute
 * - Power, Sleep, Wake
 * - Media Play/Pause/Rewind/FastForward
 * - Mouse movement + click
 * - Voice (Google Assistant)
 * - Text input (IME)
 * - App launch
 */
class AndroidTvRemoteClient(private val context: Context) : TvProtocol {

    companion object {
        private const val TAG = "AtvRemote"
        private const val COMMAND_PORT = 6466
        private const val PAIRING_PORT = 6467
    }

    override val protocolName: String = "Android TV Remote v2"
    override val brand: String = "ANDROID_TV"

    private var socket: SSLSocket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
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
                needsPairing = false

                // Cek apakah sudah pernah pairing
                if (!TlsHelper.hasPairingData(context)) {
                    // Belum pernah pairing → minta user pairing dulu
                    needsPairing = true
                    onResult(false, "PAIRING_NEEDED")
                    return@withContext
                }

                // Kalau ada pairingCode dari user → proses pairing
                if (pairingCode.isNotBlank()) {
                    val success = doPairing(tv.ip, pairingCode)
                    if (success) {
                        connected = true
                        onResult(true, "Pairing berhasil!")
                    } else {
                        onResult(false, "Pairing gagal")
                    }
                    return@withContext
                }

                // Coba connect langsung (pakai cert tersimpan)
                val success = doConnect(tv.ip)
                if (success) {
                    connected = true
                    onResult(true, "Terhubung ke TV")
                } else {
                    // Cert lama mungkin kadaluarsa → pairing ulang
                    needsPairing = true
                    TlsHelper.clearPairingData(context)
                    onResult(false, "PAIRING_NEEDED")
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
            input = null
            output = null
            socket = null
            connected = false
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
    // PAIRING + CONNECT
    // ==========================================================

    private suspend fun doPairing(host: String, pin: String): Boolean {
        val client = AndroidTvPairingClient(context)
        pairingClient = client

        // Step 1: Start pairing
        val (startOk, startMsg) = client.startPairing(host)
        if (!startOk) {
            Log.e(TAG, "startPairing failed: $startMsg")
            return false
        }

        // Step 2: Kirim PIN
        val (pinOk, pinMsg) = client.sendPin(pin)
        if (!pinOk) {
            Log.e(TAG, "sendPin failed: $pinMsg")
            return false
        }

        client.disconnect()

        // Step 3: Connect pakai cert yang tersimpan
        return doConnect(host)
    }

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

    private fun sendKeyLong(keyCode: RemoteMessageProto.RemoteKeyCode, direction: RemoteMessageProto.RemoteDirection) {
        val message = RemoteMessageProto.RemoteMessage.newBuilder()
            .setRemoteKeyCode(keyCode)
            .setRemoteDirection(direction)
            .build()
        sendMessage(message)
    }

    private fun sendMouseMove(payload: String) {
        // Payload format: "dx|dy" (float)
        val parts = payload.split("|")
        if (parts.size < 2) return
        val dx = parts[0].toFloatOrNull() ?: 0f
        val dy = parts[1].toFloatOrNull() ?: 0f

        // Kirim sebagai DPAD berulang (fallback sementara)
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
            // Protobuf length-delimited
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
