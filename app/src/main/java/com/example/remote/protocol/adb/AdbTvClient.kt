package dev.andikuneiocontroll.remote.protocol.adb

import android.content.Context
import dev.andikuneiocontroll.remote.discovery.DiscoveredTv
import dev.andikuneiocontroll.remote.protocol.TvCommand
import dev.andikuneiocontroll.remote.protocol.TvProtocol

/**
 * AdbTvClient — implementasi TvProtocol untuk ADB over WiFi.
 *
 * Syarat di TV:
 * - Developer Options aktif (tap Build Number 7x)
 * - ADB Debugging aktif
 * - TV & HP di WiFi yang sama
 */
class AdbTvClient(private val context: Context) : TvProtocol {

    override val protocolName: String = "ADB Wi-Fi"
    override val brand: String = "ANDROID_TV"

    private val adbClient = AdbClient(context)

    override fun canHandle(tv: DiscoveredTv): Boolean {
        return tv.brand.uppercase() in listOf(
            "ANDROID_TV", "GOOGLE_TV", "FIRE_TV",
            "SONY", "TCL", "HISENSE", "SHARP", "TOSHIBA"
        )
    }

    override suspend fun connect(
        tv: DiscoveredTv,
        pairingCode: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val port = if (tv.port > 0 && tv.port == 5555) tv.port else 5555
        val (success, message) = adbClient.connect(tv.ip, port)
        onResult(success, message)
    }

    override suspend fun disconnect() {
        adbClient.disconnect()
    }

    override fun isConnected(): Boolean = adbClient.isConnected

    override suspend fun sendCommand(command: String, payload: String) {
        if (!adbClient.isConnected) return

        val shellCmd = mapCommandToShell(command, payload) ?: return
        adbClient.sendShell(shellCmd)
    }

    /**
     * Mapping command → shell ADB.
     */
    private fun mapCommandToShell(command: String, payload: String): String? {
        return when (command) {
            // ============ NAVIGASI ============
            TvCommand.DPAD_UP -> "input keyevent 19"        // KEYCODE_DPAD_UP
            TvCommand.DPAD_DOWN -> "input keyevent 20"      // KEYCODE_DPAD_DOWN
            TvCommand.DPAD_LEFT -> "input keyevent 21"      // KEYCODE_DPAD_LEFT
            TvCommand.DPAD_RIGHT -> "input keyevent 22"     // KEYCODE_DPAD_RIGHT
            TvCommand.DPAD_OK, TvCommand.DPAD_CENTER -> "input keyevent 23" // KEYCODE_DPAD_CENTER

            // ============ SYSTEM ============
            TvCommand.HOME -> "input keyevent 3"            // KEYCODE_HOME
            TvCommand.BACK -> "input keyevent 4"            // KEYCODE_BACK
            TvCommand.RECENTS -> "input keyevent 187"       // KEYCODE_APP_SWITCH
            TvCommand.NOTIFICATIONS -> "input keyevent 83"  // KEYCODE_NOTIFICATION
            TvCommand.POWER -> "input keyevent 26"          // KEYCODE_POWER
            TvCommand.POWER_OFF -> "input keyevent 26"
            TvCommand.POWER_ON -> "input keyevent 26"
            TvCommand.SLEEP -> "input keyevent 223"         // KEYCODE_SLEEP
            TvCommand.WAKE -> "input keyevent 224"          // KEYCODE_WAKEUP

            // ============ MEDIA ============
            TvCommand.PLAY -> "input keyevent 126"          // KEYCODE_MEDIA_PLAY
            TvCommand.PAUSE -> "input keyevent 127"         // KEYCODE_MEDIA_PAUSE
            TvCommand.PLAY_PAUSE -> "input keyevent 85"     // KEYCODE_MEDIA_PLAY_PAUSE
            TvCommand.STOP -> "input keyevent 86"           // KEYCODE_MEDIA_STOP
            TvCommand.REWIND -> "input keyevent 89"         // KEYCODE_MEDIA_REWIND
            TvCommand.FORWARD -> "input keyevent 90"        // KEYCODE_MEDIA_FAST_FORWARD
            TvCommand.NEXT -> "input keyevent 87"           // KEYCODE_MEDIA_NEXT
            TvCommand.PREVIOUS -> "input keyevent 88"       // KEYCODE_MEDIA_PREVIOUS

            // ============ VOLUME ============
            TvCommand.VOLUME_UP -> "input keyevent 24"      // KEYCODE_VOLUME_UP
            TvCommand.VOLUME_DOWN -> "input keyevent 25"    // KEYCODE_VOLUME_DOWN
            TvCommand.VOLUME_MUTE -> "input keyevent 164"   // KEYCODE_VOLUME_MUTE

            // ============ CHANNEL ============
            TvCommand.CHANNEL_UP -> "input keyevent 166"    // KEYCODE_CHANNEL_UP
            TvCommand.CHANNEL_DOWN -> "input keyevent 167"  // KEYCODE_CHANNEL_DOWN

            // ============ TEXT INPUT ============
            TvCommand.INPUT_TEXT -> {
                if (payload.isBlank()) null
                else "input text '${escapeShell(payload)}'"
            }
            TvCommand.KEY_DELETE -> "input keyevent 67"     // KEYCODE_DEL
            TvCommand.KEY_ENTER -> "input keyevent 66"      // KEYCODE_ENTER

            // ============ NUMBER ============
            TvCommand.NUM_0 -> "input keyevent 7"
            TvCommand.NUM_1 -> "input keyevent 8"
            TvCommand.NUM_2 -> "input keyevent 9"
            TvCommand.NUM_3 -> "input keyevent 10"
            TvCommand.NUM_4 -> "input keyevent 11"
            TvCommand.NUM_5 -> "input keyevent 12"
            TvCommand.NUM_6 -> "input keyevent 13"
            TvCommand.NUM_7 -> "input keyevent 14"
            TvCommand.NUM_8 -> "input keyevent 15"
            TvCommand.NUM_9 -> "input keyevent 16"

            // ============ COLOR ============
            TvCommand.COLOR_RED -> "input keyevent 183"     // KEYCODE_PROG_RED
            TvCommand.COLOR_GREEN -> "input keyevent 184"   // KEYCODE_PROG_GREEN
            TvCommand.COLOR_YELLOW -> "input keyevent 185"  // KEYCODE_PROG_YELLOW
            TvCommand.COLOR_BLUE -> "input keyevent 186"    // KEYCODE_PROG_BLUE

            // ============ INPUT SOURCE ============
            TvCommand.INPUT_HDMI1 -> "input keyevent 243"   // KEYCODE_TV_INPUT_HDMI_1
            TvCommand.INPUT_HDMI2 -> "input keyevent 244"
            TvCommand.INPUT_HDMI3 -> "input keyevent 245"
            TvCommand.INPUT_HDMI4 -> "input keyevent 246"
            TvCommand.INPUT_AV1 -> "input keyevent 247"
            TvCommand.INPUT_AV2 -> "input keyevent 248"
            TvCommand.INPUT_TV -> "input keyevent 170"      // KEYCODE_TV

            // ============ VOICE ============
            TvCommand.VOICE_START -> "input keyevent 219"   // KEYCODE_VOICE_ASSIST
            TvCommand.VOICE_STOP -> "input keyevent 4"      // back

            // ============ CUSTOM ============
            TvCommand.LAUNCH_APP -> {
                if (payload.isBlank()) null
                else "monkey -p $payload -c android.intent.category.LAUNCHER 1"
            }

            else -> null
        }
    }

    /**
     * Escape shell command (single quotes).
     */
    private fun escapeShell(input: String): String {
        return input.replace("'", "'\\''")
    }

    /**
     * Cek status koneksi via ping.
     */
    suspend fun checkConnection(): Boolean = adbClient.ping()
}
