package dev.andikuneiocontroll.remote.protocol.vizio

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
 * VizioClient — implementasi TvProtocol untuk Vizio SmartCast TV.
 *
 * Protokol: HTTPS REST + WebSocket di port 9000.
 * Pairing: Butuh PIN 4 digit yang ditampilkan TV (pairing code).
 *
 * Flow:
 * 1. POST /pairing/start → TV tampilkan PIN 4 digit
 * 2. User input PIN
 * 3. POST /pairing/pair dengan PIN → dapat auth token
 * 4. Simpan token, connect via WebSocket
 */
class VizioClient(private val context: Context) : TvProtocol {

    override val protocolName: String = "Vizio SmartCast"
    override val brand: String = "VIZIO"

    private var webSocket: WebSocket? = null
    private var targetIp: String = ""
    private var targetPort: Int = 9000
    private var authToken: String = ""

    @Volatile
    private var connected: Boolean = false

    private val client: OkHttpClient = buildTrustAllClient()
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    override fun canHandle(tv: DiscoveredTv): Boolean {
        return tv.brand.uppercase() == "VIZIO"
    }

    override suspend fun connect(
        tv: DiscoveredTv,
        pairingCode: String,
        onResult: (Boolean, String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                targetIp = tv.ip
                targetPort = if (tv.port > 0) tv.port else 9000

                // Kalau sudah punya token, langsung pakai
                if (authToken.isNotBlank()) {
                    if (openWebSocket()) {
                        connected = true
                        onResult(true, "Terhubung ke Vizio TV")
                    } else {
                        connected = false
                        onResult(false, "Token kadaluarsa. Perlu pairing ulang.")
                    }
                    return@withContext
                }

                // Pairing flow — butuh PIN
                if (pairingCode.isBlank()) {
                    // Kirim request pairing start → TV tampilkan PIN
                    val started = startPairing()
                    if (started) {
                        onResult(false, "PIN_NEEDED: TV menampilkan PIN 4 digit. Masukkan PIN.")
                    } else {
                        onResult(false, "Gagal memulai pairing")
                    }
                    return@withContext
                }

                // Kirim PIN ke TV
                val token = submitPin(pairingCode)
                if (token.isNullOrBlank()) {
                    onResult(false, "PIN salah atau ditolak TV")
                    return@withContext
                }

                authToken = token

                // Buka WebSocket dengan token
                if (openWebSocket()) {
                    connected = true
                    onResult(true, "Terhubung ke Vizio TV")
                } else {
                    connected = false
                    onResult(false, "Gagal membuka koneksi")
                }

            } catch (e: Exception) {
                connected = false
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    /**
     * Start pairing — kirim request ke TV, TV akan tampilkan PIN.
     */
    private suspend fun startPairing(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://$targetIp:$targetPort/pairing/start"
                val json = JSONObject().apply {
                    put("deviceId", getDeviceId())
                    put("deviceName", "iOControll Tv")
                }
                val body = json.toString().toRequestBody(JSON_MEDIA)
                val request = Request.Builder().url(url).post(body).build()
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                response.close()
                success
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Submit PIN ke TV → dapat auth token.
     */
    private suspend fun submitPin(pin: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://$targetIp:$targetPort/pairing/pair"
                val json = JSONObject().apply {
                    put("deviceId", getDeviceId())
                    put("challengeType", 1)
                    put("responseKey", pin)
                }
                val body = json.toString().toRequestBody(JSON_MEDIA)
                val request = Request.Builder().url(url).post(body).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                response.close()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val obj = JSONObject(responseBody)
                    obj.optString("authToken", "")
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Buka WebSocket ke TV.
     */
    private fun openWebSocket(): Boolean {
        return try {
            val url = "wss://$targetIp:$targetPort/websocket"
            val request = Request.Builder()
                .url(url)
                .addHeader("Auth", authToken)
                .build()

            val latch = java.util.concurrent.CountDownLatch(1)
            var opened = false

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    opened = true
                    // Kirim handshake
                    val handshake = JSONObject().apply {
                        put("REQUEST", "STARTCONTROL")
                        put("HELLO", JSONObject().apply {
                            put("DEVICEID", getDeviceId())
                            put("VERSION", "1")
                            put("AUTHTOKEN", authToken)
                        })
                    }
                    webSocket.send(handshake.toString())
                    latch.countDown()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    opened = false
                    latch.countDown()
                }
            })

            latch.await(5, TimeUnit.SECONDS)
            opened
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun disconnect() {
        try {
            // Kirim GOODBYE
            val bye = JSONObject().apply { put("REQUEST", "STOPCONTROL") }
            webSocket?.send(bye.toString())
            webSocket?.close(1000, "User disconnect")
        } catch (_: Exception) {}
        webSocket = null
        connected = false
        targetIp = ""
    }

    override fun isConnected(): Boolean = connected

    override suspend fun sendCommand(command: String, payload: String) {
        if (!connected || webSocket == null) return

        val vizioKey = mapCommandToVizioKey(command)
        if (vizioKey != null) {
            sendKey(vizioKey)
            return
        }

        when (command) {
            TvCommand.INPUT_TEXT -> {
                if (payload.isNotBlank()) sendText(payload)
            }
            TvCommand.INPUT_HDMI1 -> setInput("HDMI-1")
            TvCommand.INPUT_HDMI2 -> setInput("HDMI-2")
            TvCommand.INPUT_HDMI3 -> setInput("HDMI-3")
            TvCommand.INPUT_HDMI4 -> setInput("HDMI-4")
            TvCommand.INPUT_TV -> setInput("TUNER")
        }
    }

    /**
     * Mapping command universal → Vizio key.
     */
    private fun mapCommandToVizioKey(command: String): String? {
        return when (command) {
            // Navigation
            TvCommand.DPAD_UP -> "UP"
            TvCommand.DPAD_DOWN -> "DOWN"
            TvCommand.DPAD_LEFT -> "LEFT"
            TvCommand.DPAD_RIGHT -> "RIGHT"
            TvCommand.DPAD_OK, TvCommand.DPAD_CENTER -> "OK"

            // System
            TvCommand.HOME -> "HOME"
            TvCommand.BACK -> "BACK"
            TvCommand.RECENTS -> "MENU"
            TvCommand.POWER -> "POW_ON"
            TvCommand.POWER_OFF -> "POW_OFF"

            // Media
            TvCommand.PLAY -> "PLAY"
            TvCommand.PAUSE -> "PAUSE"
            TvCommand.STOP -> "STOP"
            TvCommand.REWIND -> "REWIND"
            TvCommand.FORWARD -> "FORWARD"

            // Volume
            TvCommand.VOLUME_UP -> "VOL_UP"
            TvCommand.VOLUME_DOWN -> "VOL_DOWN"
            TvCommand.VOLUME_MUTE -> "MUTE"

            // Channel
            TvCommand.CHANNEL_UP -> "CH_UP"
            TvCommand.CHANNEL_DOWN -> "CH_DOWN"

            // Numbers
            TvCommand.NUM_0 -> "NUM_0"
            TvCommand.NUM_1 -> "NUM_1"
            TvCommand.NUM_2 -> "NUM_2"
            TvCommand.NUM_3 -> "NUM_3"
            TvCommand.NUM_4 -> "NUM_4"
            TvCommand.NUM_5 -> "NUM_5"
            TvCommand.NUM_6 -> "NUM_6"
            TvCommand.NUM_7 -> "NUM_7"
            TvCommand.NUM_8 -> "NUM_8"
            TvCommand.NUM_9 -> "NUM_9"

            else -> null
        }
    }

    /**
     * Kirim key press.
     */
    private fun sendKey(key: String) {
        try {
            val json = JSONObject().apply {
                put("KEY", JSONObject().apply {
                    put("CODESET", 0)
                    put("CODE", getVizioKeyCode(key))
                    put("ACTION", "KEYPRESS")
                })
            }
            webSocket?.send(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Kirim text input.
     */
    private fun sendText(text: String) {
        try {
            val json = JSONObject().apply {
                put("KEY", JSONObject().apply {
                    put("CODESET", 0)
                    put("CODE", text)
                    put("ACTION", "KEYPRESS")
                })
            }
            webSocket?.send(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Set input source.
     */
    private fun setInput(input: String) {
        try {
            val json = JSONObject().apply {
                put("REQUEST", "MODIFY")
                put("VALUE", JSONObject().apply {
                    put("NAME", "current_input")
                    put("VALUE", input)
                })
                put("HASHVAL", 0)
            }
            webSocket?.send(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Convert key name → Vizio key code (internal enum).
     */
    private fun getVizioKeyCode(key: String): Int {
        return when (key) {
            "POW_ON" -> 0
            "POW_OFF" -> 1
            "VOL_UP" -> 2
            "VOL_DOWN" -> 3
            "MUTE" -> 4
            "CH_UP" -> 5
            "CH_DOWN" -> 6
            "UP" -> 8
            "DOWN" -> 9
            "LEFT" -> 10
            "RIGHT" -> 11
            "OK" -> 12
            "BACK" -> 13
            "HOME" -> 15
            "MENU" -> 16
            "PLAY" -> 17
            "PAUSE" -> 18
            "STOP" -> 19
            "REWIND" -> 20
            "FORWARD" -> 21
            "NUM_0" -> 22
            "NUM_1" -> 23
            "NUM_2" -> 24
            "NUM_3" -> 25
            "NUM_4" -> 26
            "NUM_5" -> 27
            "NUM_6" -> 28
            "NUM_7" -> 29
            "NUM_8" -> 30
            "NUM_9" -> 31
            else -> -1
        }
    }

    private fun getDeviceId(): String {
        return try {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "iocontroll-${System.currentTimeMillis()}"
        } catch (_: Exception) {
            "iocontroll-${System.currentTimeMillis()}"
        }
    }

    /**
     * Trust-all cert untuk Vizio self-signed.
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
