package dev.andikuneiocontroll.remote.protocol.lg

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
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * LgWebOsClient — implementasi TvProtocol untuk LG Smart TV (webOS).
 *
 * Protokol: WebSocket SSAP di port 3001 (secure).
 * Endpoint: wss://<ip>:3001
 *
 * Pairing:
 * - Connect pertama → TV tampilkan popup "Allow?"
 * - User tap "Accept" di TV
 * - TV kirim client-key → disimpan untuk auto-connect
 */
class LgWebOsClient(private val context: Context) : TvProtocol {

    override val protocolName: String = "LG webOS"
    override val brand: String = "LG"

    private var webSocket: WebSocket? = null
    private var targetIp: String = ""
    private var targetPort: Int = 3001
    private val sessionId = UUID.randomUUID().toString().substring(0, 8)

    @Volatile
    private var connected: Boolean = false

    @Volatile
    private var handshakeDone: Boolean = false

    @Volatile
    var clientKey: String = ""
        private set

    private val client: OkHttpClient = buildTrustAllClient()

    override fun canHandle(tv: DiscoveredTv): Boolean {
        return tv.brand.uppercase() == "LG"
    }

    override suspend fun connect(
        tv: DiscoveredTv,
        pairingCode: String,
        onResult: (Boolean, String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                targetIp = tv.ip
                targetPort = if (tv.port > 0) tv.port else 3001

                val url = "wss://$targetIp:$targetPort"

                val request = Request.Builder()
                    .url(url)
                    .build()

                val resultLatch = java.util.concurrent.CountDownLatch(1)
                var resultSuccess = false
                var resultMessage = ""

                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        // Kirim handshake request
                        sendHandshakeRequest()
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleMessage(text) { success, msg ->
                            if (!handshakeDone) {
                                handshakeDone = true
                                connected = success
                                resultSuccess = success
                                resultMessage = msg
                                resultLatch.countDown()
                            }
                        }
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

                // Tunggu max 15 detik (user butuh waktu tap "Accept" di TV)
                val ok = resultLatch.await(15, TimeUnit.SECONDS)
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
        handshakeDone = false
        targetIp = ""
    }

    override fun isConnected(): Boolean = connected

    override suspend fun sendCommand(command: String, payload: String) {
        if (!connected || webSocket == null) return

        val uri = mapCommandToLgUri(command, payload)
        if (uri != null) {
            sendRequest(uri, payload)
        }
    }

    /**
     * Mapping command universal → LG SSAP URI.
     */
    private fun mapCommandToLgUri(command: String, payload: String): String? {
        return when (command) {
            // Navigation
            TvCommand.DPAD_UP -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.DPAD_DOWN -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.DPAD_LEFT -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.DPAD_RIGHT -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.DPAD_OK, TvCommand.DPAD_CENTER -> "ssap://com.webos.service.networkinput/getPointerInputSocket"

            // System
            TvCommand.HOME -> "ssap://system.launcher/launch"
            TvCommand.BACK -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.RECENTS -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.POWER -> "ssap://system/turnOff"
            TvCommand.POWER_OFF -> "ssap://system/turnOff"

            // Media
            TvCommand.PLAY -> "ssap://media.controls/play"
            TvCommand.PAUSE -> "ssap://media.controls/pause"
            TvCommand.STOP -> "ssap://media.controls/stop"
            TvCommand.REWIND -> "ssap://media.controls/rewind"
            TvCommand.FORWARD -> "ssap://media.controls/fastForward"

            // Volume
            TvCommand.VOLUME_UP -> "ssap://audio/volumeUp"
            TvCommand.VOLUME_DOWN -> "ssap://audio/volumeDown"
            TvCommand.VOLUME_MUTE -> "ssap://audio/setMute"

            // Channel
            TvCommand.CHANNEL_UP -> "ssap://tv/channelUp"
            TvCommand.CHANNEL_DOWN -> "ssap://tv/channelDown"

            // Input
            TvCommand.INPUT_HDMI1 -> "ssap://tv/switchInput"
            TvCommand.INPUT_HDMI2 -> "ssap://tv/switchInput"
            TvCommand.INPUT_HDMI3 -> "ssap://tv/switchInput"
            TvCommand.INPUT_HDMI4 -> "ssap://tv/switchInput"

            // Numbers
            TvCommand.NUM_0 -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.NUM_1 -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.NUM_2 -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.NUM_3 -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.NUM_4 -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.NUM_5 -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.NUM_6 -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.NUM_7 -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.NUM_8 -> "ssap://com.webos.service.networkinput/getPointerInputSocket"
            TvCommand.NUM_9 -> "ssap://com.webos.service.networkinput/getPointerInputSocket"

            else -> null
        }
    }

    /**
     * Kirim request SSAP.
     */
    private fun sendRequest(uri: String, payload: String) {
        try {
            val json = JSONObject().apply {
                put("id", sessionId)
                put("type", "request")
                put("uri", uri)

                val payloadObj = JSONObject()
                when (uri) {
                    "ssap://audio/setMute" -> {
                        val currentMute = payload.toBooleanOrNull() ?: true
                        payloadObj.put("mute", !currentMute)
                    }
                    "ssap://system.launcher/launch" -> {
                        payloadObj.put("id", "com.webos.app.home")
                    }
                    "ssap://tv/switchInput" -> {
                        payloadObj.put("inputId", payload)
                    }
                }
                if (payloadObj.length() > 0) {
                    put("payload", payloadObj)
                }
            }
            webSocket?.send(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun String.toBooleanOrNull(): Boolean? {
        return when (this.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    /**
     * Kirim handshake request pertama.
     */
    private fun sendHandshakeRequest() {
        try {
            val json = JSONObject().apply {
                put("id", sessionId)
                put("type", "register")
                put("payload", JSONObject().apply {
                    put("forcePairing", false)
                    put("pairingType", "PROMPT")
                    put("client-key", clientKey)
                    put("manifest", JSONObject().apply {
                        put("manifestVersion", 1)
                        put("appVersion", "1.0.0")
                        put("signed", JSONObject().apply {
                            put("created", "20261009")
                            put("appId", "com.iocontroll.tv")
                            put("vendorId", "com.iocontroll")
                            put("localizedAppNames", JSONObject().apply {
                                put("", "iOControll Tv")
                            })
                            put("permissions", org.json.JSONArray().apply {
                                put("TEST_SECURE")
                                put("CONTROL_INPUT_TEXT")
                                put("CONTROL_MOUSE_AND_KEYBOARD")
                                put("READ_INSTALLED_APPS")
                                put("READ_LGE_SDX")
                                put("READ_NOTIFICATIONS")
                                put("SEARCH")
                                put("WRITE_SETTINGS")
                                put("WRITE_NOTIFICATION_ALERT")
                                put("CONTROL_POWER")
                                put("READ_CURRENT_CHANNEL")
                                put("READ_RUNNING_APPS")
                                put("READ_UPDATE_INFO")
                                put("UPDATE_FROM_REMOTE_APP")
                                put("READ_TV_CHANNEL_LIST")
                                put("WRITE_NOTIFICATION_TOAST")
                                put("READ_POWER_STATE")
                                put("READ_COUNTRY_INFO")
                                put("READ_SETTINGS")
                                put("CONTROL_TV_SCREEN")
                                put("CONTROL_TV")
                                put("READ_APP_STATUS")
                                put("CONTROL_AUDIO")
                                put("CONTROL_DISPLAY")
                                put("CONTROL_INPUT_JOYSTICK")
                                put("CONTROL_INPUT_MEDIA_RECORDING")
                                put("CONTROL_INPUT_MEDIA_PLAYBACK")
                                put("CONTROL_INPUT_TV")
                                put("READ_INPUT_DEVICE_LIST")
                                put("READ_TV_CURRENT_TIME")
                                put("READ_TV_CHANNEL_LIST")
                            })
                        })
                        put("permissions", org.json.JSONArray().apply {
                            put("LAUNCH")
                            put("LAUNCH_WEBAPP")
                            put("APP_TO_APP")
                            put("CLOSE")
                            put("TEST_OPEN")
                            put("TEST_PROTECTED")
                            put("CONTROL_AUDIO")
                            put("CONTROL_DISPLAY")
                            put("CONTROL_INPUT_JOYSTICK")
                            put("CONTROL_INPUT_MEDIA_RECORDING")
                            put("CONTROL_INPUT_MEDIA_PLAYBACK")
                            put("CONTROL_INPUT_TV")
                            put("CONTROL_POWER")
                            put("READ_APP_STATUS")
                            put("READ_CURRENT_CHANNEL")
                            put("READ_INPUT_DEVICE_LIST")
                            put("READ_NETWORK_STATE")
                            put("READ_RUNNING_APPS")
                            put("READ_TV_CHANNEL_LIST")
                            put("WRITE_NOTIFICATION_TOAST")
                            put("READ_POWER_STATE")
                            put("READ_COUNTRY_INFO")
                            put("READ_SETTINGS")
                            put("CONTROL_TV_SCREEN")
                            put("CONTROL_TV")
                        })
                    })
                })
            }
            webSocket?.send(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handle incoming message.
     */
    private fun handleMessage(text: String, onHandshake: (Boolean, String) -> Unit) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "")

            if (type == "registered") {
                val key = json.optJSONObject("payload")?.optString("client-key", "") ?: ""
                if (key.isNotBlank()) {
                    clientKey = key
                }
                onHandshake(true, "Terhubung ke LG TV")
            } else if (type == "error") {
                val errorMsg = json.optString("error", "Unknown error")
                if (errorMsg.contains("401")) {
                    onHandshake(false, "Pairing ditolak. Coba hapus device di TV, ulangi connect.")
                } else {
                    onHandshake(false, "Error: $errorMsg")
                }
            }
        } catch (e: Exception) {
            // Bukan JSON valid
        }
    }

    /**
     * Build OkHttp client trust-all cert (LG pakai self-signed).
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
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
    }
}
