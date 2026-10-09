package dev.andikuneiocontroll.remote

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
    val targetIp: String = "",
    val targetPort: Int = 23017,
    val targetName: String = "",
    val errorMessage: String = "",
    val isAirMouseActive: Boolean = false
)

/**
 * RemoteClient — Client di HP.
 *
 * V1.00.001 (Updated):
 * - Hapus pairing code (auto-accept, sesuai Zank Remote)
 * - Tambah auto-discovery via UDP Broadcast (RemoteDiscovery)
 * - Connect langsung pakai IP:port dari hasil discovery
 */
class RemoteClient(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private val _connectionState = MutableStateFlow(ClientConnectionState())
    val connectionState: StateFlow<ClientConnectionState> = _connectionState

    // Auto-discovery
    val discovery = RemoteDiscovery(context, scope)

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastSendTime = 0L
    private var readerJob: Job? = null

    // ==========================================================
    // AUTO-DISCOVERY
    // ==========================================================

    /**
     * Mulai scan TV di WiFi yang sama. Hasil muncul di [discovery.discoveredTvs].
     * Scan otomatis berhenti setelah ~3 detik.
     */
    fun startAutoScan() {
        discovery.startScan()
    }

    fun stopAutoScan() {
        discovery.stopScan()
    }

    /**
     * Connect langsung ke TV hasil discovery (tanpa pairing).
     */
    fun connectToTv(tv: DiscoveredTv, onResult: (Boolean, String) -> Unit) {
        connect(tv.ip, tv.remotePort, tv.name, onResult)
    }

    // ==========================================================
    // CONNECT / DISCONNECT
    // ==========================================================

    fun connect(
        targetIp: String,
        port: Int = 23017,
        targetName: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        disconnect()

        scope.launch(Dispatchers.IO) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(targetIp, port), 4000)
                val w = PrintWriter(s.getOutputStream(), true)
                val r = BufferedReader(InputStreamReader(s.getInputStream()))

                // Baca balasan pertama (auto-accept di sisi TV)
                val response = r.readLine()

                if (response == "CONNECT_OK") {
                    socket = s
                    writer = w
                    reader = r

                    withContext(Dispatchers.Main) {
                        _connectionState.value = ClientConnectionState(
                            isConnected = true,
                            targetIp = targetIp,
                            targetPort = port,
                            targetName = targetName
                        )
                        onResult(true, "Terhubung ke ${targetName.ifBlank { targetIp }}")
                    }

                    // Loop baca balasan dari server (biar socket tidak EOF)
                    readerJob = scope.launch(Dispatchers.IO) {
                        try {
                            while (true) {
                                val line = r.readLine() ?: break
                                // Balasan "OK" diabaikan
                            }
                        } catch (_: Exception) {}
                        // Kalau server putus
                        withContext(Dispatchers.Main) {
                            if (_connectionState.value.isConnected) {
                                _connectionState.value = ClientConnectionState()
                            }
                        }
                    }
                } else {
                    s.close()
                    withContext(Dispatchers.Main) {
                        _connectionState.value = ClientConnectionState(
                            errorMessage = "TV menolak koneksi"
                        )
                        onResult(false, "TV menolak koneksi")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _connectionState.value = ClientConnectionState(
                        errorMessage = "Gagal terhubung: ${e.message}"
                    )
                    onResult(false, "Koneksi gagal: ${e.message}")
                }
            }
        }
    }

    fun disconnect() {
        disableAirMouse()
        readerJob?.cancel()
        readerJob = null
        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        writer = null
        reader = null
        _connectionState.value = ClientConnectionState()
    }

    // ==========================================================
    // COMMAND
    // ==========================================================

    fun sendCommand(command: String, payload: String = "") {
        val w = writer ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val fullMsg = if (payload.isEmpty()) command else "$command|$payload"
                w.println(fullMsg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMouseMove(dx: Float, dy: Float) {
        val w = writer ?: return
        scope.launch(Dispatchers.IO) {
            try {
                w.println("MOUSE_MOVE|$dx|$dy")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================================
    // AIR MOUSE
    // ==========================================================

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
        if (now - lastSendTime < 35) return // throttle ~30fps
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
