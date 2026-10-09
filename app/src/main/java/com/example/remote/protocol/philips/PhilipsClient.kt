package dev.andikuneiocontroll.remote.protocol.philips

import android.content.Context
import dev.andikuneiocontroll.remote.discovery.DiscoveredTv
import dev.andikuneiocontroll.remote.protocol.TvCommand
import dev.andikuneiocontroll.remote.protocol.TvProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * PhilipsClient — implementasi TvProtocol untuk Philips TV (JointSpace API).
 *
 * Protokol: HTTP/HTTPS REST di port 1925 (insecure) atau 1926 (secure).
 * Tidak butuh pairing — langsung connect (kecuali pairing mode aktif).
 *
 * API:
 * - GET /6/system  → info TV
 * - POST /6/input/key  → kirim key
 * - POST /6/input/text → kirim text
 */
class PhilipsClient(private val context: Context) : TvProtocol {

    override val protocolName: String = "Philips JointSpace"
    override val brand: String = "PHILIPS"

    private var targetIp: String = ""
    private var targetPort: Int = 1925
    private var useHttps: Boolean = false

    @Volatile
    private var connected: Boolean = false

    private val client: OkHttpClient = buildTrustAllClient()
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    override fun canHandle(tv: DiscoveredTv): Boolean {
        return tv.brand.uppercase() == "PHILIPS"
    }

    override suspend fun connect(
        tv: DiscoveredTv,
        pairingCode: String,
        onResult: (Boolean, String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                targetIp = tv.ip
                targetPort = if (tv.port > 0) tv.port else 1925

                // Coba HTTPS dulu (port 1926), fallback ke HTTP (1925)
                if (testConnection(https = true, port = 1926)) {
                    useHttps = true
                    targetPort = 1926
                    connected = true
                    onResult(true, "Terhubung ke Philips TV (HTTPS)")
                    return@withContext
                }

                if (testConnection(https = false, port = 1925)) {
                    useHttps = false
                    targetPort = 1925
                    connected = true
                    onResult(true, "Terhubung ke Philips TV (HTTP)")
                    return@withContext
                }

                connected = false
                onResult(false, "Gagal connect. Cek TV menyala & di WiFi sama.")

            } catch (e: Exception) {
                connected = false
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    private fun testConnection(https: Boolean, port: Int): Boolean {
        return try {
            val scheme = if (https) "https" else "http"
            val url = "$scheme://$targetIp:$port/6/system"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()
            success
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun disconnect() {
        connected = false
        targetIp = ""
    }

    override fun isConnected(): Boolean = connected

    override suspend fun sendCommand(command: String, payload: String) {
        if (!connected) return

        val key = mapCommandToPhilipsKey(command)
        if (key != null) {
            sendKey(key)
            return
        }

        when (command) {
            TvCommand.INPUT_TEXT -> {
                if (payload.isNotBlank()) sendText(payload)
            }
            TvCommand.LAUNCH_APP -> {
                if (payload.isNotBlank()) launchApp(payload)
            }
        }
    }

    /**
     * Mapping command universal → Philips key code.
     */
    private fun mapCommandToPhilipsKey(command: String): String? {
        return when (command) {
            // Navigation
            TvCommand.DPAD_UP -> "CursorUp"
            TvCommand.DPAD_DOWN -> "CursorDown"
            TvCommand.DPAD_LEFT -> "CursorLeft"
            TvCommand.DPAD_RIGHT -> "CursorRight"
            TvCommand.DPAD_OK, TvCommand.DPAD_CENTER -> "Confirm"

            // System
            TvCommand.HOME -> "Home"
            TvCommand.BACK -> "Back"
            TvCommand.RECENTS -> "Source"
            TvCommand.POWER -> "Standby"
            TvCommand.POWER_OFF -> "Standby"

            // Media
            TvCommand.PLAY -> "Play"
            TvCommand.PAUSE -> "Pause"
            TvCommand.PLAY_PAUSE -> "PlayPause"
            TvCommand.STOP -> "Stop"
            TvCommand.REWIND -> "Rewind"
            TvCommand.FORWARD -> "FastForward"

            // Volume
            TvCommand.VOLUME_UP -> "VolumeUp"
            TvCommand.VOLUME_DOWN -> "VolumeDown"
            TvCommand.VOLUME_MUTE -> "Mute"

            // Channel
            TvCommand.CHANNEL_UP -> "ChannelStepUp"
            TvCommand.CHANNEL_DOWN -> "ChannelStepDown"

            // Input
            TvCommand.INPUT_HDMI1 -> "Source"
            TvCommand.INPUT_HDMI2 -> "Source"
            TvCommand.INPUT_HDMI3 -> "Source"
            TvCommand.INPUT_HDMI4 -> "Source"
            TvCommand.INPUT_TV -> "WatchTV"

            // Numbers
            TvCommand.NUM_0 -> "Digit0"
            TvCommand.NUM_1 -> "Digit1"
            TvCommand.NUM_2 -> "Digit2"
            TvCommand.NUM_3 -> "Digit3"
            TvCommand.NUM_4 -> "Digit4"
            TvCommand.NUM_5 -> "Digit5"
            TvCommand.NUM_6 -> "Digit6"
            TvCommand.NUM_7 -> "Digit7"
            TvCommand.NUM_8 -> "Digit8"
            TvCommand.NUM_9 -> "Digit9"

            // Color
            TvCommand.COLOR_RED -> "Red"
            TvCommand.COLOR_GREEN -> "Green"
            TvCommand.COLOR_YELLOW -> "Yellow"
            TvCommand.COLOR_BLUE -> "Blue"

            // Info
            TvCommand.NOTIFICATIONS -> "Info"

            else -> null
        }
    }

    /**
     * Kirim key via POST /6/input/key.
     */
    private suspend fun sendKey(key: String) {
        withContext(Dispatchers.IO) {
            try {
                val scheme = if (useHttps) "https" else "http"
                val url = "$scheme://$targetIp:$targetPort/6/input/key"

                val json = JSONObject().apply {
                    put("key", key)
                }

                val body = json.toString().toRequestBody(JSON_MEDIA)
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().close()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Kirim text via POST /6/input/text.
     */
    private suspend fun sendText(text: String) {
        withContext(Dispatchers.IO) {
            try {
                val scheme = if (useHttps) "https" else "http"
                val url = "$scheme://$targetIp:$targetPort/6/input/text"

                val json = JSONObject().apply {
                    put("text", text)
                }

                val body = json.toString().toRequestBody(JSON_MEDIA)
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().close()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Launch app via POST /6/activities/launch.
     */
    private suspend fun launchApp(packageName: String) {
        withContext(Dispatchers.IO) {
            try {
                val scheme = if (useHttps) "https" else "http"
                val url = "$scheme://$targetIp:$targetPort/6/activities/launch"

                val json = JSONObject().apply {
                    put("intent", JSONObject().apply {
                        put("component", JSONObject().apply {
                            put("packageName", packageName)
                        })
                    })
                }

                val body = json.toString().toRequestBody(JSON_MEDIA)
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().close()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Ambil info TV.
     */
    suspend fun getSystemInfo(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val scheme = if (useHttps) "https" else "http"
                val url = "$scheme://$targetIp:$targetPort/6/system"
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

    /**
     * OkHttp client trust-all cert (Philips pakai self-signed di HTTPS).
     */
    private fun buildTrustAllClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }
}
