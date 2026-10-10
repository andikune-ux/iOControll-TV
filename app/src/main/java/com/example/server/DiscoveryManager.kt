package dev.andikuneiocontroll.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
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
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class DiscoveryManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _discoveredPeers = MutableStateFlow<List<DevicePeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DevicePeer>> = _discoveredPeers

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var broadcastJob: Job? = null
    private var listenJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private val udpPort = 23018

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val serviceType = "_iocontroll._tcp."

    fun startDiscovery(localPort: Int) {
        stopDiscovery()

        // Acquire MulticastLock to allow Android device to receive UDP broadcast packets
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("iocontroll_discovery_lock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val localIp = WifiHttpServer.getDeviceIpAddress(context)
        val broadcastAddresses = getBroadcastAddresses()

        // 1. Broadcast presence via UDP to all subnet broadcast addresses every 2.5 seconds
        broadcastJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true

                while (isActive) {
                    try {
                        val message = "IOCONTROLL_ANNOUNCE|$deviceName|$localIp|$localPort"
                        val bytes = message.toByteArray()
                        for (bAddr in broadcastAddresses) {
                            try {
                                val packet = DatagramPacket(bytes, bytes.size, bAddr, udpPort)
                                socket.send(packet)
                            } catch (_: Exception) {}
                        }
                    } catch (e: Exception) {
                        // ignore packet error
                    }
                    delay(2500)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket?.close()
            }
        }

        // 2. Listen for UDP broadcasts from other devices
        listenJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(udpPort))
                }
                val buffer = ByteArray(1024)

                while (isActive) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val raw = String(packet.data, 0, packet.length).trim()
                        if (raw.startsWith("IOCONTROLL_ANNOUNCE")) {
                            val parts = raw.split("|")
                            if (parts.size >= 4) {
                                val peerName = parts[1]
                                val peerIp = parts[2]
                                val peerPort = parts[3].toIntOrNull() ?: 23016

                                if (peerIp != localIp && peerIp != "127.0.0.1") {
                                    addOrUpdatePeer(peerName, peerIp, peerPort)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket?.close()
            }
        }

        // 3. Register mDNS / NSD Service
        registerNsdService(deviceName, localPort)
    }

    private fun addOrUpdatePeer(peerName: String, peerIp: String, peerPort: Int) {
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

    private fun registerNsdService(name: String, port: Int) {
        try {
            nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "iOControll-$name"
                serviceType = this@DiscoveryManager.serviceType
                this.port = port
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {}
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            }

            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Fast Subnet Scan: Probes subnet IPs for TV remote & file server ports
    fun scanSubnetQuick(onComplete: (Int) -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            _isScanning.value = true
            val localIp = WifiHttpServer.getDeviceIpAddress(context)
            val parts = localIp.split(".")
            if (parts.size == 4) {
                val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                val myLastByte = parts[3].toIntOrNull() ?: -1

                var found = 0
                // Scan typical host range in batches of 25
                val jobs = mutableListOf<Job>()
                for (host in 1..254) {
                    if (host == myLastByte) continue
                    val testIp = "$prefix.$host"
                    val j = launch(Dispatchers.IO) {
                        if (isPortOpen(testIp, 23017, 300) || isPortOpen(testIp, 23016, 300)) {
                            addOrUpdatePeer("Android TV ($testIp)", testIp, 23017)
                            found++
                        }
                    }
                    jobs.add(j)
                }
                jobs.forEach { it.join() }
                withContext(Dispatchers.Main) {
                    _isScanning.value = false
                    onComplete(found)
                }
            } else {
                withContext(Dispatchers.Main) {
                    _isScanning.value = false
                    onComplete(0)
                }
            }
        }
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getBroadcastAddresses(): List<InetAddress> {
        val list = mutableListOf<InetAddress>()
        try {
            list.add(InetAddress.getByName("255.255.255.255"))
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val netIf = interfaces.nextElement()
                if (netIf.isLoopback || !netIf.isUp) continue
                for (ifAddr in netIf.interfaceAddresses) {
                    val broadcast = ifAddr.broadcast
                    if (broadcast != null && broadcast !in list) {
                        list.add(broadcast)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun stopDiscovery() {
        broadcastJob?.cancel()
        listenJob?.cancel()
        broadcastJob = null
        listenJob = null

        try {
            if (registrationListener != null && nsdManager != null) {
                nsdManager?.unregisterService(registrationListener)
                registrationListener = null
            }
        } catch (_: Exception) {}

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                multicastLock = null
            }
        } catch (_: Exception) {}
    }
}
