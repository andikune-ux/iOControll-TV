package dev.andikuneiocontroll.remote.protocol.roku

import android.content.Context
import dev.andikuneiocontroll.remote.discovery.DiscoveredTv
import dev.andikuneiocontroll.remote.protocol.TvCommand
import dev.andikuneiocontroll.remote.protocol.TvProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * RokuEcpClient — implementasi TvProtocol untuk Roku TV via ECP (External Control Protocol).
 *
 * Roku ECP = HTTP REST API di port 8060.
 * Tidak perlu pairing — langsung connect.
 *
 * Command via URL:
 * - POST /keypress/Home
 * - POST /keypress/Up
 * - POST /launch/12 (app ID)
 */
class RokuEcpClient(private val context: Context) : TvProtocol {

    override val protocolName: String = "Roku ECP"
    override val brand: String = "ROKU"

    private var targetIp: String = ""
    private var targetPort: Int = 8060

    @Volatile
    private var connected: Boolean = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    override fun canHandle(tv: DiscoveredTv): Boolean {
        return tv.brand.uppercase() == "ROKU"
    }

    override suspend fun connect(
        tv: DiscoveredTv,
        pairingCode: String,
        onResult: (Boolean, String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                targetIp = tv.ip
                targetPort = if (tv.port > 0) tv.port else 8060

                // Test connect dengan query device info
                val url = "http://$targetIp:$targetPort/query/device-info"
                val request = Request.Builder().url(url).get().build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    connected = true
                    onResult(true, "Terhubung ke Roku TV")
                } else {
                    connected = false
                    onResult(false, "Gagal connect: HTTP ${response.code}")
                }
                response.close()
            } catch (e: Exception) {
                connected = false
                onResult(false, "Gagal connect: ${e.message}")
            }
        }
    }

    override suspend fun disconnect() {
        connected = false
        targetIp = ""
    }

    override fun isConnected(): Boolean = connected

    override suspend fun sendCommand(command: String, payload: String) {
        if (!connected) return

        val keyPress = mapCommandToRokuKey(command)
        if (keyPress != null) {
            sendKeyPress(keyPress)
            return
        }

        // Handle command khusus
        when (command) {
            TvCommand.INPUT_TEXT -> {
                if (payload.isNotBlank()) {
                    sendText(payload)
                }
            }
            TvCommand.LAUNCH_APP -> {
                if (payload.isNotBlank()) {
                    launchApp(payload)
                }
            }
        }
    }

    /**
     * Mapping command universal → Roku keypress.
     */
    private fun mapCommandToRokuKey(command: String): String? {
        return when (command) {
            // Navigation
            TvCommand.DPAD_UP -> "Up"
            TvCommand.DPAD_DOWN -> "Down"
            TvCommand.DPAD_LEFT -> "Left"
            TvCommand.DPAD_RIGHT -> "Right"
            TvCommand.DPAD_OK, TvCommand.DPAD_CENTER -> "Select"

            // System
            TvCommand.HOME -> "Home"
            TvCommand.BACK -> "Back"
            TvCommand.RECENTS -> "Home"

            // Media
            TvCommand.PLAY -> "Play"
            TvCommand.PAUSE -> "Play"  // Roku toggle
            TvCommand.PLAY_PAUSE -> "Play"
            TvCommand.REWIND -> "Rev"
            TvCommand.FORWARD -> "Fwd"

            // Volume
            TvCommand.VOLUME_UP -> "VolumeUp"
            TvCommand.VOLUME_DOWN -> "VolumeDown"
            TvCommand.VOLUME_MUTE -> "VolumeMute"

            // Power
            TvCommand.POWER -> "Power"
            TvCommand.POWER_OFF -> "PowerOff"
            TvCommand.POWER_ON -> "PowerOn"

            // Input
            TvCommand.INPUT_TV -> "InputTuner"
            TvCommand.INPUT_HDMI1 -> "InputHDMI1"
            TvCommand.INPUT_HDMI2 -> "InputHDMI2"
            TvCommand.INPUT_HDMI3 -> "InputHDMI3"
            TvCommand.INPUT_HDMI4 -> "InputHDMI4"
            TvCommand.INPUT_AV1 -> "InputAV1"

            // Voice
            TvCommand.VOICE_START -> "Voice"

            else -> null
        }
    }

    /**
     * Kirim keypress via ECP.
     */
    private suspend fun sendKeyPress(key: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$targetIp:$targetPort/keypress/$key"
                val request = Request.Builder().url(url).post(okhttp3.RequestBody.create(null, "")).build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Kirim text input (lit_ atau char_).
     */
    private suspend fun sendText(text: String) {
        withContext(Dispatchers.IO) {
            try {
                for (ch in text) {
                    val encoded = if (ch.isLetterOrDigit()) {
                        "Lit_${ch}"
                    } else {
                        "Lit_%${"%04x".format(ch.code)}"
                    }
                    val url = "http://$targetIp:$targetPort/keypress/$encoded"
                    val request = Request.Builder().url(url).post(okhttp3.RequestBody.create(null, "")).build()
                    client.newCall(request).execute().close()
                    kotlinx.coroutines.delay(50)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Launch app di Roku via app ID.
     * App ID populer:
     * - Netflix: 12
     * - YouTube: 837
     * - Prime Video: 13
     * - Disney+: 291097
     */
    suspend fun launchApp(appId: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$targetIp:$targetPort/launch/$appId"
                val request = Request.Builder().url(url).post(okhttp3.RequestBody.create(null, "")).build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Ambil device info (untuk verifikasi).
     */
    suspend fun getDeviceInfo(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://$targetIp:$targetPort/query/device-info"
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                response.close()
                body
            } catch (e: Exception) {
                null
            }
        }
    }
}
