package dev.andikuneiocontroll.remote.protocol

import dev.andikuneiocontroll.remote.discovery.DiscoveredTv

/**
 * TvProtocol — interface umum untuk semua protokol TV.
 *
 * V2 (Update):
 * - Tambah method pair() dengan default implementation (return null)
 * - Protokol yang butuh pairing: Android TV (ADB), Samsung, LG, Vizio
 * - Protokol yang tidak butuh: Roku, Philips
 */
interface TvProtocol {

    /** Nama protokol (contoh: "Android TV Remote v2") */
    val protocolName: String

    /** Brand TV (contoh: "ANDROID_TV") */
    val brand: String

    /**
     * Cek apakah protocol ini bisa connect ke TV target.
     */
    fun canHandle(tv: DiscoveredTv): Boolean

    /**
     * Connect ke TV. Kalau butuh pairing, akan return error PAIRING_NEEDED.
     *
     * @param tv Target TV
     * @param pairingCode PIN 6 digit (khusus Android TV) — bisa kosong
     * @param onResult Callback: (sukses, pesan)
     */
    suspend fun connect(
        tv: DiscoveredTv,
        pairingCode: String = "",
        onResult: (Boolean, String) -> Unit
    )

    /**
     * Disconnect dari TV.
     */
    suspend fun disconnect()

    /**
     * Kirim command ke TV.
     */
    suspend fun sendCommand(command: String, payload: String = "")

    /**
     * Cek status koneksi.
     */
    fun isConnected(): Boolean

    /**
     * Pairing (khusus Android TV ADB / Wireless Debugging).
     * Protokol yang tidak butuh pairing: return null.
     *
     * @param host IP TV
     * @param pairingPort Port pairing (37000-44000)
     * @param pairingCode Kode 6 digit dari TV
     * @return Pair(sukses, pesan) — null kalau protokol tidak support pairing
     */
    suspend fun pair(
        host: String,
        pairingPort: Int,
        pairingCode: String
    ): Pair<Boolean, String>? = null
}

/**
 * Command standard yang harus didukung semua protokol.
 */
object TvCommand {
    // Navigation
    const val DPAD_UP = "DPAD_UP"
    const val DPAD_DOWN = "DPAD_DOWN"
    const val DPAD_LEFT = "DPAD_LEFT"
    const val DPAD_RIGHT = "DPAD_RIGHT"
    const val DPAD_OK = "DPAD_OK"
    const val DPAD_CENTER = "DPAD_CENTER"

    // System
    const val HOME = "HOME"
    const val BACK = "BACK"
    const val RECENTS = "RECENTS"
    const val NOTIFICATIONS = "NOTIFICATIONS"
    const val POWER = "POWER"
    const val POWER_OFF = "POWER_OFF"
    const val POWER_ON = "POWER_ON"
    const val SLEEP = "SLEEP"
    const val WAKE = "WAKE"

    // Media
    const val PLAY = "PLAY"
    const val PAUSE = "PAUSE"
    const val PLAY_PAUSE = "PLAY_PAUSE"
    const val STOP = "STOP"
    const val REWIND = "REWIND"
    const val FORWARD = "FORWARD"
    const val NEXT = "NEXT"
    const val PREVIOUS = "PREVIOUS"

    // Volume
    const val VOLUME_UP = "VOLUME_UP"
    const val VOLUME_DOWN = "VOLUME_DOWN"
    const val VOLUME_MUTE = "VOLUME_MUTE"
    const val VOLUME_SET = "VOLUME_SET"

    // Channel
    const val CHANNEL_UP = "CHANNEL_UP"
    const val CHANNEL_DOWN = "CHANNEL_DOWN"

    // Input
    const val INPUT_HDMI1 = "INPUT_HDMI1"
    const val INPUT_HDMI2 = "INPUT_HDMI2"
    const val INPUT_HDMI3 = "INPUT_HDMI3"
    const val INPUT_HDMI4 = "INPUT_HDMI4"
    const val INPUT_AV1 = "INPUT_AV1"
    const val INPUT_AV2 = "INPUT_AV2"
    const val INPUT_TV = "INPUT_TV"
    const val INPUT_USB = "INPUT_USB"

    // Mouse
    const val MOUSE_MOVE = "MOUSE_MOVE"
    const val MOUSE_CLICK = "MOUSE_CLICK"
    const val MOUSE_RIGHT_CLICK = "MOUSE_RIGHT_CLICK"
    const val MOUSE_SCROLL = "MOUSE_SCROLL"

    // Text
    const val INPUT_TEXT = "INPUT_TEXT"
    const val KEY_DELETE = "KEY_DELETE"
    const val KEY_ENTER = "KEY_ENTER"

    // Voice
    const val VOICE_START = "VOICE_START"
    const val VOICE_STOP = "VOICE_STOP"

    // Numbers
    const val NUM_0 = "NUM_0"
    const val NUM_1 = "NUM_1"
    const val NUM_2 = "NUM_2"
    const val NUM_3 = "NUM_3"
    const val NUM_4 = "NUM_4"
    const val NUM_5 = "NUM_5"
    const val NUM_6 = "NUM_6"
    const val NUM_7 = "NUM_7"
    const val NUM_8 = "NUM_8"
    const val NUM_9 = "NUM_9"

    // Colors
    const val COLOR_RED = "COLOR_RED"
    const val COLOR_GREEN = "COLOR_GREEN"
    const val COLOR_YELLOW = "COLOR_YELLOW"
    const val COLOR_BLUE = "COLOR_BLUE"

    // Custom
    const val LAUNCH_APP = "LAUNCH_APP"
    const val CAST_START = "CAST_START"
    const val CAST_STOP = "CAST_STOP"
    const val COPY_TEXT = "COPY_TEXT"
    const val ROTATE_SCREEN = "ROTATE_SCREEN"
}
