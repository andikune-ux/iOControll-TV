package com.example.remote

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.net.URLEncoder

data class ClientConnectionState(
    val isConnected: Boolean = false,
    val isPaired: Boolean = false,
    val targetIp: String = "",
    val targetPort: Int = 23017,
    val connectionMode: String = "None", // "TCP Socket" or "HTTP LAN"
    val errorMessage: String = "",
    val isAirMouseActive: Boolean = false,
    val latencyMs: Long = 0L
)

class RemoteClient(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private val _connectionState = MutableStateFlow(ClientConnectionState())
    val connectionState: StateFlow<ClientConnectionState> = _connectionState

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastSendTime = 0L

    fun connect(
        targetIp: String,
        port: Int = 23017,
        pairingCode: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        disconnect()

        val cleanIp = targetIp.trim()
        val cleanCode = pairingCode.trim()

        scope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            // 1. Coba sambungan utama: TCP Socket di port 23017
            var tcpSuccess = false
            var tcpError = ""

            try {
                val s = Socket()
                s.tcpNoDelay = true
                s.keepAlive = true
                s.soTimeout = 4000
                s.connect(InetSocketAddress(cleanIp, port), 3500)

                val w = PrintWriter(s.getOutputStream(), true)
                val r = BufferedReader(InputStreamReader(s.getInputStream()))

                // Kirim perintah PAIR
                w.println("PAIR|$cleanCode")
                w.flush()

                val response = r.readLine()?.trim()

                if (response != null && response.startsWith("PAIR_OK")) {
                    socket = s
                    writer = w
                    reader = r
                    tcpSuccess = true

                    val latency = System.currentTimeMillis() - startTime
                    withContext(Dispatchers.Main) {
                        _connectionState.value = ClientConnectionState(
                            isConnected = true,
                            isPaired = true,
                            targetIp = cleanIp,
                            targetPort = port,
                            connectionMode = "TCP Socket (Cepat)",
                            latencyMs = latency
                        )
                        onResult(true, "Berhasil terhubung ke TV ($cleanIp) via TCP Socket!")
                    }
                } else if (response != null && response.startsWith("PAIR_FAILED")) {
                    s.close()
                    withContext(Dispatchers.Main) {
                        _connectionState.value = ClientConnectionState(
                            isConnected = false,
                            errorMessage = "Kode pairing salah!"
                        )
                        onResult(false, "Kode pairing salah! Periksa 4-digit kode di layar TV.")
                    }
                    return@launch
                } else {
                    s.close()
                    tcpError = "Respons socket tidak sesuai"
                }
            } catch (e: Exception) {
                tcpError = e.message ?: "Timeout/Refused"
            }

            if (tcpSuccess) return@launch

            // 2. Fallback Otomatis: HTTP REST di port 23016 jika TCP Socket diblokir router
            try {
                val httpUrl = "http://$cleanIp:23016/api/remote?cmd=PAIR&payload=${URLEncoder.encode(cleanCode, "UTF-8")}"
                val connection = (URL(httpUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                    requestMethod = "GET"
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val latency = System.currentTimeMillis() - startTime
                    withContext(Dispatchers.Main) {
                        _connectionState.value = ClientConnectionState(
                            isConnected = true,
                            isPaired = true,
                            targetIp = cleanIp,
                            targetPort = 23016,
                            connectionMode = "HTTP LAN (Stabil)",
                            latencyMs = latency
                        )
                        onResult(true, "Terhubung ke TV ($cleanIp) via Saluran HTTP LAN!")
                    }
                    return@launch
                }
            } catch (httpEx: Exception) {
                // HTTP juga gagal
            }

            // Jika kedua cara gagal:
            withContext(Dispatchers.Main) {
                _connectionState.value = ClientConnectionState(
                    isConnected = false,
                    errorMessage = "Gagal terhubung ($tcpError)"
                )
                onResult(
                    false,
                    "Gagal terhubung ke $cleanIp. Pastikan HP dan TV di Wi-Fi yang sama dan aplikasi di TV terbuka."
                )
            }
        }
    }

    fun disconnect() {
        disableAirMouse()
        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        socket = null
        writer = null
        reader = null
        _connectionState.value = ClientConnectionState()
    }

    fun sendCommand(command: String, payload: String = "") {
        val cur = _connectionState.value
        if (!cur.isConnected) return

        scope.launch(Dispatchers.IO) {
            val fullMsg = if (payload.isEmpty()) command else "$command|$payload"

            // Jika socket aktif, gunakan socket (latensi 2ms)
            val w = writer
            if (w != null && socket?.isConnected == true && socket?.isClosed == false) {
                try {
                    w.println(fullMsg)
                    w.flush()
                    return@launch
                } catch (e: Exception) {
                    // Socket error, fallback ke HTTP di bawah
                }
            }

            // Fallback kirim via HTTP REST
            try {
                val encodedPayload = URLEncoder.encode(payload, "UTF-8")
                val urlStr = "http://${cur.targetIp}:23016/api/remote?cmd=$command&payload=$encodedPayload"
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 1500
                    readTimeout = 1500
                    requestMethod = "GET"
                }
                conn.responseCode // trigger request
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMouseMove(dx: Float, dy: Float) {
        val cur = _connectionState.value
        if (!cur.isConnected) return

        scope.launch(Dispatchers.IO) {
            val w = writer
            if (w != null && socket?.isConnected == true && socket?.isClosed == false) {
                try {
                    w.println("MOUSE_MOVE|$dx|$dy")
                    w.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // HTTP fallback for mouse click / move
                try {
                    val urlStr = "http://${cur.targetIp}:23016/api/remote?cmd=MOUSE_MOVE&payload=$dx,$dy"
                    (URL(urlStr).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 800
                        readTimeout = 800
                    }.responseCode
                } catch (_: Exception) {}
            }
        }
    }

    fun enableAirMouse(): Boolean {
        if (gyroSensor == null || sensorManager == null) return false
        val ok = sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        if (ok) {
            _connectionState.value = _connectionState.value.copy(isAirMouseActive = true)
        }
        return ok
    }

    fun disableAirMouse() {
        sensorManager?.unregisterListener(this)
        _connectionState.value = _connectionState.value.copy(isAirMouseActive = false)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_connectionState.value.isAirMouseActive) return
        val now = System.currentTimeMillis()
        if (now - lastSendTime < 35) return // ~30fps
        lastSendTime = now

        val dx: Float
        val dy: Float
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            dx = -event.values[2] * 25f
            dy = -event.values[0] * 25f
        } else {
            dx = -event.values[0] * 8f
            dy = event.values[1] * 8f
        }

        if (Math.abs(dx) > 0.5f || Math.abs(dy) > 0.5f) {
            sendMouseMove(dx, dy)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
