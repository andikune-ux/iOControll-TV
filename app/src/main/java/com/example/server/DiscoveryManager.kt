package dev.andikuneiocontroll.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import dev.andikuneiocontroll.model.DevicePeer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DiscoveryManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _discoveredPeers = MutableStateFlow<List<DevicePeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DevicePeer>> = _discoveredPeers

    private var broadcastJob: Job? = null
    private var listenJob: Job? = null
    private val udpPort = 23018

    fun startDiscovery(localPort: Int) {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val localIp = WifiHttpServer.getDeviceIpAddress(context)

        // Broadcast presence via UDP every 3 seconds
        broadcastJob = scope.launch(Dispatchers.IO) {
            val socket = DatagramSocket()
            socket.broadcast = true
            while (isActive) {
                try {
                    val message = "IOCONTROLL_ANNOUNCE|$deviceName|$localIp|$localPort"
                    val bytes = message.toByteArray()
                    val packet = DatagramPacket(
                        bytes,
                        bytes.size,
                        InetAddress.getByName("255.255.255.255"),
                        udpPort
                    )
                    socket.send(packet)
                } catch (e: Exception) {
                    // Ignore broadcast errors
                }
                delay(3000)
            }
            socket.close()
        }

        // Listen for other devices' UDP broadcasts
        listenJob = scope.launch(Dispatchers.IO) {
            try {
                val socket = DatagramSocket(udpPort)
                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val raw = String(packet.data, 0, packet.length)
                    if (raw.startsWith("IOCONTROLL_ANNOUNCE")) {
                        val parts = raw.split("|")
                        if (parts.size >= 4) {
                            val peerName = parts[1]
                            val peerIp = parts[2]
                            val peerPort = parts[3].toIntOrNull() ?: 23016

                            // Don't add ourselves
                            if (peerIp != localIp) {
                                val current = _discoveredPeers.value.toMutableList()
                                val existingIndex = current.indexOfFirst { it.ip == peerIp }
                                val peer = DevicePeer(
                                    id = "$peerIp:$peerPort",
                                    name = peerName,
                                    ip = peerIp,
                                    port = peerPort,
                                    isOnline = true
                                )
                                if (existingIndex >= 0) {
                                    current[existingIndex] = peer
                                } else {
                                    current.add(peer)
                                }
                                _discoveredPeers.value = current
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // socket closed or error
            }
        }
    }

    fun stopDiscovery() {
        broadcastJob?.cancel()
        listenJob?.cancel()
        broadcastJob = null
        listenJob = null
    }
}
