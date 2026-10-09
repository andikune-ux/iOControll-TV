package dev.andikuneiocontroll.remote.protocol.samsung

import android.content.Context
import dev.andikuneiocontroll.remote.discovery.DiscoveredTv
import dev.andikuneiocontroll.remote.protocol.TvCommand
import dev.andikuneiocontroll.remote.protocol.TvProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * SamsungTizenClient — implementasi TvProtocol untuk Samsung Smart TV (Tizen).
 *
 * Protokol: WebSocket JSON di port 8002 (secure).
 * Endpoint: wss://<ip>:8002/api/v2/channels/samsung.remote.control?name=<base64-app-name>
 *
 * Pairing:
 * - Connect pertama kali → TV tampilkan popup "Allow?"
 * - User tap "Allow" di TV
 * - TV kirim token → disimpan untuk connect berikutnya (auto-accept)
 */
class SamsungTizenClient(private val context: Context) : TvProtocol {

    override val protocolName: String = "Samsung Tizen"
    override val brand: String = "SAMSUNG"

    private var webSocket: WebSocket? = null
    private var targetIp: String = ""
    private var targetPort: Int = 8002

    @Volatile
    private var connected: Boolean = false

    @Volatile
    var token: String = ""
        private set

    private val client: OkHttpClient = buildTrustAllClient()

    override fun canHandle(tv: DiscoveredTv): Boolean {
        return tv.brand.uppercase() == "SAMSUNG"
    }

    override suspend fun connect(
        tv: DiscoveredTv,
        pairingCode: String,
        onResult: (Boolean, String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                targetIp = tv.ip
                targetPort = if (tv.port > 0) tv.port else 8002

                // Build URL — name harus base64
                val appName = android.util.Base64.encodeToString(
                    "iOControll Tv".toByteArray(),
                    android.util.Base64.NO_WRAP
                )

                val url = StringBuilder("wss://$targetIp:$targetPort/api/v2/channels/samsung.remote.control")
                url.append("?name=$appName")
                if (token.isNotBlank()) {
                    url.append("&token=$token")
                }

                val request = Request.Builder()
                    .url(url.toString())
                    .build()

                val resultLatch = java.util.concurrent.CountDownLatch(1)
                var resultSuccess = false
                var resultMessage = ""

                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        connected = true
                        resultSuccess = true
                        resultMessage = if (token.isBlank()) {
                            "Terhubung. Tap 'Allow' di TV."
                        } else {
                            "Terhubung ke Samsung TV"
                        }
                        resultLatch.countDown()
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleMessage(text)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        connected = false
                        resultSuccess = false
                        resultMessage = "Gagal: ${t.message}"
                        resultLatch.countDown()
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        connected = false
                    }
                })

                // Tunggu max 10 detik (user butuh waktu tap "Allow" di TV)
                val ok = resultLatch.await(10, TimeUnit.SECONDS)
                if (!ok) {
                    connected = false
                    onResult(false, "Timeout: TV tidak merespons. Pastikan TV menyala & di WiFi sama.")
                } else {
                    onResult(resultSuccess, resultMessage)
                }

            } catch (e: Exception) {
                connected = false
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    override suspend fun disconnect() {
        try {
            webSocket?.close(1000, "User disconnect")
        } catch (_: Exception) {}
        webSocket = null
        connected = false
        targetIp = ""
    }

    override fun isConnected(): Boolean = connected

    override suspend fun sendCommand(command: String, payload: String) {
        if (!connected || webSocket == null) return

        val keyCode = mapCommandToSamsungKey(command)
        if (keyCode != null) {
            sendKey(keyCode)
            return
        }

        // Handle input text
        if (command == TvCommand.INPUT_TEXT && payload.isNotBlank()) {
            sendText(payload)
        }
    }

    /**
     * Mapping command universal → Samsung key code.
     */
    private fun mapCommandToSamsungKey(command: String): String? {
        return when (command) {
            // Navigation
            TvCommand.DPAD_UP -> "KEY_UP"
            TvCommand.DPAD_DOWN -> "KEY_DOWN"
            TvCommand.DPAD_LEFT -> "KEY_LEFT"
            TvCommand.DPAD_RIGHT -> "KEY_RIGHT"
            TvCommand.DPAD_OK, TvCommand.DPAD_CENTER -> "KEY_ENTER"

            // System
            TvCommand.HOME -> "KEY_HOME"
            TvCommand.BACK -> "KEY_RETURN"
            TvCommand.RECENTS -> "KEY_HOME"
            TvCommand.POWER -> "KEY_POWER"
            TvCommand.POWER_OFF -> "KEY_POWEROFF"

            // Media
            TvCommand.PLAY -> "KEY_PLAY"
            TvCommand.PAUSE -> "KEY_PAUSE"
            TvCommand.PLAY_PAUSE -> "KEY_PLAY_BACK"
            TvCommand.STOP -> "KEY_STOP"
            TvCommand.REWIND -> "KEY_REWIND"
            TvCommand.FORWARD -> "KEY_FF"

            // Volume
            TvCommand.VOLUME_UP -> "KEY_VOLUP"
            TvCommand.VOLUME_DOWN -> "KEY_VOLDOWN"
            TvCommand.VOLUME_MUTE -> "KEY_MUTE"

            // Channel
            TvCommand.CHANNEL_UP -> "KEY_CHUP"
            TvCommand.CHANNEL_DOWN -> "KEY_CHDOWN"

            // Input
            TvCommand.INPUT_HDMI1 -> "KEY_HDMI1"
            TvCommand.INPUT_HDMI2 -> "KEY_HDMI2"
            TvCommand.INPUT_HDMI3 -> "KEY_HDMI3"
            TvCommand.INPUT_HDMI4 -> "KEY_HDMI4"
            TvCommand.INPUT_TV -> "KEY_TV"
            TvCommand.INPUT_AV1 -> "KEY_AV1"
            TvCommand.INPUT_AV2 -> "KEY_AV2"

            // Numbers
            TvCommand.NUM_0 -> "KEY_0"
            TvCommand.NUM_1 -> "KEY_1"
            TvCommand.NUM_2 -> "KEY_2"
            TvCommand.NUM_3 -> "KEY_3"
            TvCommand.NUM_4 -> "KEY_4"
            TvCommand.NUM_5 -> "KEY_5"
            TvCommand.NUM_6 -> "KEY_6"
            TvCommand.NUM_7 -> "KEY_7"
            TvCommand.NUM_8 -> "KEY_8"
            TvCommand.NUM_9 -> "KEY_9"

            // Color
            TvCommand.COLOR_RED -> "KEY_RED"
            TvCommand.COLOR_GREEN -> "KEY_GREEN"
            TvCommand.COLOR_YELLOW -> "KEY_YELLOW"
            TvCommand.COLOR_BLUE -> "KEY_BLUE"

            // Voice
            TvCommand.VOICE_START -> "KEY_VOICE"

            // Info
            TvCommand.NOTIFICATIONS -> "KEY_INFO"

            else -> null
        }
    }

    /**
     * Kirim key press via JSON.
     */
    private fun sendKey(keyCode: String) {
        try {
            val json = JSONObject().apply {
                put("method", "ms.remote.control")
                put("params", JSONObject().apply {
                    put("Cmd", "Click")
                    put("DataOfCmd", keyCode)
                    put("Option", "false")
                    put("TypeOfRemote", "SendRemoteKey")
                })
            }
            webSocket?.send(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Kirim text via IME.
     */
    private suspend fun sendText(text: String) {
        try {
            // Base64 encode text
            val encoded = android.util.Base64.encodeToString(
                text.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )

            val json = JSONObject().apply {
                put("method", "ms.remote.control")
                put("params", JSONObject().apply {
                    put("Cmd", encoded)
                    put("DataOfCmd", "base64")
                    put("TypeOfRemote", "SendInputString")
                })
            }
            webSocket?.send(json.toString())
            delay(100)

            // Enter setelah text
            sendKey("KEY_ENTER")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handle incoming message (token dari TV).
     */
    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event", "")

            if (event == "ms.channel.connect") {
                val data = json.optJSONObject("data")
                val newToken = data?.optString("token", "") ?: ""
                if (newToken.isNotBlank()) {
                    token = newToken
                }
            }
        } catch (e: Exception) {
            // Bukan JSON valid
        }
    }

    /**
     * Build OkHttp client yang trust semua cert (Samsung pakai self-signed).
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
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)  // No timeout untuk WebSocket
            .build()
    }
}
