package com.example.remote

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

data class ClientConnectionState(
    val isConnected: Boolean = false,
    val isPaired: Boolean = false,
    val targetIp: String = "",
    val targetPort: Int = 23017,
    val errorMessage: String = "",
    val isAirMouseActive: Boolean = false
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

    fun connect(targetIp: String, port: Int = 23017, pairingCode: String, onResult: (Boolean, String) -> Unit) {
        disconnect()

        scope.launch(Dispatchers.IO) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(targetIp, port), 4000)
                val w = PrintWriter(s.getOutputStream(), true)
                val r = BufferedReader(InputStreamReader(s.getInputStream()))

                // Send pairing code
                w.println("PAIR|$pairingCode")
                val response = r.readLine()

                if (response == "PAIR_OK") {
                    socket = s
                    writer = w
                    reader = r
                    withContext(Dispatchers.Main) {
                        _connectionState.value = ClientConnectionState(
                            isConnected = true,
                            isPaired = true,
                            targetIp = targetIp,
                            targetPort = port
                        )
                        onResult(true, "Terhubung ke TV ($targetIp)")
                    }
                } else {
                    s.close()
                    withContext(Dispatchers.Main) {
                        _connectionState.value = ClientConnectionState(
                            isConnected = false,
                            errorMessage = "Kode pairing salah atau ditolak"
                        )
                        onResult(false, "Kode pairing salah!")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _connectionState.value = ClientConnectionState(
                        isConnected = false,
                        errorMessage = "Gagal terhubung: ${e.message}"
                    )
                    onResult(false, "Koneksi gagal: ${e.message}")
                }
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
        if (writer == null) return
        scope.launch(Dispatchers.IO) {
            try {
                val fullMsg = if (payload.isEmpty()) command else "$command|$payload"
                writer?.println(fullMsg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMouseMove(dx: Float, dy: Float) {
        if (writer == null) return
        scope.launch(Dispatchers.IO) {
            try {
                writer?.println("MOUSE_MOVE|$dx|$dy")
            } catch (e: Exception) {
                e.printStackTrace()
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
        if (now - lastSendTime < 35) return // Throttle ~30fps
        lastSendTime = now

        val dx: Float
        val dy: Float
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // Gyroscope values in rad/s
            dx = -event.values[2] * 25f
            dy = -event.values[0] * 25f
        } else {
            // Accelerometer fallback
            dx = -event.values[0] * 8f
            dy = event.values[1] * 8f
        }

        if (Math.abs(dx) > 0.5f || Math.abs(dy) > 0.5f) {
            sendMouseMove(dx, dy)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
