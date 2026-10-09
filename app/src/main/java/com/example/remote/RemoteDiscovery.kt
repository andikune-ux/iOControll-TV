package dev.andikuneiocontroll.remote

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

/**
 * Data class untuk TV/perangkat yang ditemukan via UDP Broadcast.
 */
data class DiscoveredTv(
    val deviceId: String,
    val name: String,
    val ip: String,
    val remotePort: Int,
    val lastSeen: Long = System.currentTimeMillis()
)

/**
 * RemoteDiscovery — Auto-discovery perangkat TV via UDP Broadcast.
 *
 * Cara kerja (mirip Zank Remote):
 * - TV: buka UDP listener di port [DISCOVERY_PORT]. Saat ada broadcast "IO_DISCOVER", balas
 *   dengan data device (nama + IP + port remote).
 * - HP: kirim broadcast "IO_DISCOVER" ke 255.255.255.255:[DISCOVERY_PORT].
 *   Kumpulkan semua reply selama [SCAN_DURATION_MS] ms.
 *
 * Tidak perlu pairing code — semua device di WiFi yang sama langsung terdeteksi.
 */
class RemoteDiscovery(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isServing = MutableStateFlow(false)
    val isServing: StateFlow<Boolean> = _isServing

    private var listenerJob: Job? = null
    private var scanJob: Job? = null
    private var listenerSocket: DatagramSocket? = null

    // ==========================================================
    // TV SIDE — Listener (mendengarkan broadcast dari HP)
    // ==========================================================

    /**
     * Mulai listener di TV. Akan menjawab broadcast dari HP.
     * @param remotePort Port TCP remote (untuk kontrol) — biasanya 23017.
     */
    fun startListener(remotePort: Int = 23017) {
        if (_isServing.value) return

        listenerJob = scope.launch(Dispatchers.IO) {
            try {
                val socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    soTimeout = 0
                    bind(InetSocketAddress(DISCOVERY_PORT))
                }
                listenerSocket = socket
                _isServing.value = true

                val buffer = ByteArray(1024)
                while (isActive && !socket.isClosed) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val msg = String(packet.data, 0, packet.length).trim()

                        if (msg == MSG_DISCOVER) {
                            val reply = buildReplyJson(remotePort)
                            val replyBytes = reply.toByteArray()
                            val replyPacket = DatagramPacket(
                                replyBytes, replyBytes.size,
                                packet.address, packet.port
                            )
                            socket.send(replyPacket)
                        }
                    } catch (e: Exception) {
                        if (!socket.isClosed) e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isServing.value = false
            }
        }
    }

    fun stopListener() {
        try {
            listenerSocket?.close()
        } catch (_: Exception) {}
        listenerSocket = null
        listenerJob?.cancel()
        listenerJob = null
        _isServing.value = false
    }

    private fun buildReplyJson(remotePort: Int): String {
        val deviceId = getDeviceId()
        val name = "${Build.MANUFACTURER} ${Build.MODEL}"
        val ip = getLocalIpAddress()
        return "{\"deviceId\":\"$deviceId\",\"name\":\"$name\",\"port\":$remotePort,\"ip\":\"$ip\"}"
    }

    // ==========================================================
    // HP SIDE — Scanner (broadcast + kumpulkan reply)
    // ==========================================================

    /**
     * Mulai scan. Akan broadcast dan tunggu reply selama [SCAN_DURATION_MS].
     */
    fun startScan() {
        if (_isScanning.value) return
        _discoveredTvs.value = emptyList()

        scanJob = scope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val socket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 500
                }

                val msgBytes = MSG_DISCOVER.toByteArray()
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(msgBytes, msgBytes.size, broadcastAddr, DISCOVERY_PORT)

                val startTime = System.currentTimeMillis()
                val found = mutableMapOf<String, DiscoveredTv>()

                while (isActive && (System.currentTimeMillis() - startTime) < SCAN_DURATION_MS) {
                    try {
                        socket.send(packet)

                        val buf = ByteArray(1024)
                        val replyPacket = DatagramPacket(buf, buf.size)
                        socket.receive(replyPacket)

                        val replyStr = String(replyPacket.data, 0, replyPacket.length).trim()
                        val tv = parseReplyJson(replyStr, replyPacket.address.hostAddress ?: "")
                        if (tv != null) {
                            found[tv.deviceId] = tv
                            withContext(Dispatchers.Main) {
                                _discoveredTvs.value = found.values.toList()
                            }
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        // timeout normal, lanjut broadcast lagi
                    } catch (e: Exception) {
                        if (isActive) e.printStackTrace()
                    }
                }

                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    _isScanning.value = false
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
    }

    fun clearResults() {
        _discoveredTvs.value = emptyList()
    }

    private fun parseReplyJson(json: String, senderIp: String): DiscoveredTv? {
        return try {
            val deviceId = extractJsonString(json, "deviceId") ?: return null
            val name = extractJsonString(json, "name") ?: "TV"
            val port = extractJsonInt(json, "port") ?: 23017
            val ip = extractJsonString(json, "ip") ?: senderIp
            DiscoveredTv(
                deviceId = deviceId,
                name = name,
                ip = ip,
                remotePort = port
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.getOrNull(1)
    }

    private fun extractJsonInt(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        return pattern.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    // ==========================================================
    // UTILITY
    // ==========================================================

    private fun getDeviceId(): String {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val mac = wifiManager.connectionInfo.macAddress ?: "unknown"
            "${Build.MANUFACTURER}_${Build.MODEL}_${mac.replace(":", "")}"
        } catch (_: Exception) {
            "${Build.MANUFACTURER}_${Build.MODEL}_default"
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    val host = addr.hostAddress ?: continue
                    if (host.contains(".") && !host.contains(":")) {
                        return host
                    }
                }
            }
            "0.0.0.0"
        } catch (_: Exception) {
            "0.0.0.0"
        }
    }

    fun destroy() {
        stopScan()
        stopListener()
    }

    companion object {
        const val DISCOVERY_PORT = 23018
        const val MSG_DISCOVER = "IO_DISCOVER"
        const val SCAN_DURATION_MS = 3000L
    }
}
