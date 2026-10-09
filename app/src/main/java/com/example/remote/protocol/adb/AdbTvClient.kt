package dev.andikuneiocontroll.remote.protocol.adb

import android.content.Context
import dev.andikuneiocontroll.remote.discovery.DiscoveredTv
import dev.andikuneiocontroll.remote.protocol.TvCommand
import dev.andikuneiocontroll.remote.protocol.TvProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AdbTvClient — implementasi TvProtocol untuk ADB over WiFi / Wireless Debugging.
 *
 * V2 (Update):
 * - Support Wireless Debugging pairing (Android 11+)
 * - Kalau belum paired → return "PAIRING_NEEDED" + info pairing
 * - Kalau sudah paired → langsung connect
 *
 * Skenario:
 * 1. User pilih TV → connect() dipanggil tanpa pairing code
 * 2. Kalau belum paired → error message: "Butuh pairing dulu"
 * 3. MainScreen buka AdbPairingDialog
 * 4. User input IP + Port + Code
 * 5. pair() dipanggil → sukses → connect() dipanggil lagi
 */
class AdbTvClient(private val context: Context) : TvProtocol {

    override val protocolName: String = "ADB Wi-Fi"
    override val brand: String = "ANDROID_TV"

    private val adbClient = AdbClient(context)

    // Flag untuk sinyal ke MainScreen: butuh pairing
    @Volatile
    var needsPairing: Boolean = false
        private set

    // Info pairing: port + host yang perlu di-pair
    @Volatile
    var pairingHost: String = ""
        private set

    @Volatile
    var pairingPort: Int = 0
        private set

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
        withContext(Dispatchers.IO) {
            try {
                // Reset flag
                needsPairing = false
                pairingHost = tv.ip
                pairingPort = 0

                // Cek apakah sudah pernah pairing
                if (!adbClient.isPaired(tv.ip)) {
                    // Belum paired → signal ke UI
                    needsPairing = true
                    pairingHost = tv.ip
                    onResult(
                        false,
                        "PAIRING_NEEDED: Aktifkan Wireless Debugging di TV, lalu masukkan kode pairing."
                    )
                    return@withContext
                }

                // Sudah paired → connect
                val port = if (tv.port > 0 && tv.port == 5555) tv.port else 5555
                val (success, message) = adbClient.connect(tv.ip, port)

                if (success) {
                    onResult(true, message)
                } else {
                    // Kalau gagal & isPaired sudah true tapi tetap gagal →
                    // mungkin pairing kadaluarsa
                    if (message.contains("timeout", ignoreCase = true) ||
                        message.contains("connect", ignoreCase = true)) {
                        needsPairing = true
                        pairingHost = tv.ip
                        onResult(false, "PAIRING_NEEDED: Pairing kadaluarsa. Ulangi pairing.")
                    } else {
                        onResult(false, message)
                    }
                }
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    /**
     * Pair ke TV. Dipanggil dari UI setelah user input code.
     * Setelah pairing sukses, panggil connect() lagi.
     */
    suspend fun pair(
        host: String,
        pairingPort: Int,
        pairingCode: String
    ): Pair<Boolean, String> {
        return adbClient.pair(host, pairingPort, pairingCode)
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
            TvCommand.DPAD_UP -> "input keyevent 19"
            TvCommand.DPAD_DOWN -> "input keyevent 20"
            TvCommand.DPAD_LEFT -> "input keyevent 21"
            TvCommand.DPAD_RIGHT -> "input keyevent 22"
            TvCommand.DPAD_OK, TvCommand.DPAD_CENTER -> "input keyevent 23"

            // ============ SYSTEM ============
            TvCommand.HOME -> "input keyevent 3"
            TvCommand.BACK -> "input keyevent 4"
            TvCommand.RECENTS -> "input keyevent 187"
            TvCommand.NOTIFICATIONS -> "input keyevent 83"
            TvCommand.POWER -> "input keyevent 26"
            TvCommand.POWER_OFF -> "input keyevent 26"
            TvCommand.POWER_ON -> "input keyevent 26"
            TvCommand.SLEEP -> "input keyevent 223"
            TvCommand.WAKE -> "input keyevent 224"

            // ============ MEDIA ============
            TvCommand.PLAY -> "input keyevent 126"
            TvCommand.PAUSE -> "input keyevent 127"
            TvCommand.PLAY_PAUSE -> "input keyevent 85"
            TvCommand.STOP -> "input keyevent 86"
            TvCommand.REWIND -> "input keyevent 89"
            TvCommand.FORWARD -> "input keyevent 90"
            TvCommand.NEXT -> "input keyevent 87"
            TvCommand.PREVIOUS -> "input keyevent 88"

            // ============ VOLUME ============
            TvCommand.VOLUME_UP -> "input keyevent 24"
            TvCommand.VOLUME_DOWN -> "input keyevent 25"
            TvCommand.VOLUME_MUTE -> "input keyevent 164"

            // ============ CHANNEL ============
            TvCommand.CHANNEL_UP -> "input keyevent 166"
            TvCommand.CHANNEL_DOWN -> "input keyevent 167"

            // ============ TEXT ============
            TvCommand.INPUT_TEXT -> {
                if (payload.isBlank()) null
                else "input text '${escapeShell(payload)}'"
            }
            TvCommand.KEY_DELETE -> "input keyevent 67"
            TvCommand.KEY_ENTER -> "input keyevent 66"

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
            TvCommand.COLOR_RED -> "input keyevent 183"
            TvCommand.COLOR_GREEN -> "input keyevent 184"
            TvCommand.COLOR_YELLOW -> "input keyevent 185"
            TvCommand.COLOR_BLUE -> "input keyevent 186"

            // ============ INPUT SOURCE ============
            TvCommand.INPUT_HDMI1 -> "input keyevent 243"
            TvCommand.INPUT_HDMI2 -> "input keyevent 244"
            TvCommand.INPUT_HDMI3 -> "input keyevent 245"
            TvCommand.INPUT_HDMI4 -> "input keyevent 246"
            TvCommand.INPUT_AV1 -> "input keyevent 247"
            TvCommand.INPUT_AV2 -> "input keyevent 248"
            TvCommand.INPUT_TV -> "input keyevent 170"

            // ============ VOICE ============
            TvCommand.VOICE_START -> "input keyevent 219"
            TvCommand.VOICE_STOP -> "input keyevent 4"

            // ============ CUSTOM ============
            TvCommand.LAUNCH_APP -> {
                if (payload.isBlank()) null
                else "monkey -p $payload -c android.intent.category.LAUNCHER 1"
            }

            else -> null
        }
    }

    private fun escapeShell(input: String): String {
        return input.replace("'", "'\\''")
    }

    suspend fun checkConnection(): Boolean = adbClient.ping()

    fun clearPairing(host: String? = null) {
        AdbPairing.clearPairing(context, host)
        needsPairing = false
    }
}
