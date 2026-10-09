package dev.andikuneiocontroll.remote.discovery

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

class MdnsDiscovery(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var jmDNS: JmDNS? = null
    private var scanJob: Job? = null
    private val foundMap = mutableMapOf<String, DiscoveredTv>()

    private val serviceTypes = listOf(
        "_androidtvremote2._tcp.local.",
        "_googlecast._tcp.local.",
        "_airplay._tcp.local."
    )

    fun startScan(durationMs: Long = 5000L) {
        if (_isScanning.value) return
        foundMap.clear()
        _discoveredTvs.value = emptyList()

        scanJob = scope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val wifiManager = context.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager

                @Suppress("DEPRECATION")
                val wifiLock = wifiManager.createMulticastLock("iocontroll_mdns").apply {
                    setReferenceCounted(false)
                    acquire()
                }

                try {
                    val localIp = getLocalIpAddress()
                    if (localIp == null) {
                        withContext(Dispatchers.Main) { _isScanning.value = false }
                        return@launch
                    }

                    val host = InetAddress.getByName(localIp)
                    jmDNS = JmDNS.create(host, "iOControll-${System.currentTimeMillis()}")

                    serviceTypes.forEach { type ->
                        jmDNS?.addServiceListener(type, object : ServiceListener {
                            override fun serviceAdded(event: ServiceEvent) {
                                jmDNS?.requestServiceInfo(event.type, event.name, 3000)
                            }

                            override fun serviceRemoved(event: ServiceEvent) {}

                            override fun serviceResolved(event: ServiceEvent) {
                                handleServiceResolved(event)
                            }
                        })
                    }

                    delay(durationMs)

                } finally {
                    try {
                        if (wifiLock.isHeld) wifiLock.release()
                    } catch (_: Exception) {}
                    try { jmDNS?.close() } catch (_: Exception) {}
                    jmDNS = null
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) { _isScanning.value = false }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
        try { jmDNS?.close() } catch (_: Exception) {}
        jmDNS = null
    }

    private fun handleServiceResolved(event: ServiceEvent) {
        try {
            val info: ServiceInfo = event.info ?: return
            val addresses = info.inetAddresses ?: return
            if (addresses.isEmpty()) return

            val ip = addresses.firstOrNull()?.hostAddress ?: return
            if (ip.isEmpty() || ip == "0.0.0.0") return

            val name = info.name ?: "Unknown TV"
            val port = info.port
            val type = event.type ?: ""

            // Deteksi Chromecast
            val isChromecast = type.contains("googlecast", ignoreCase = true)

            val (brand, protocol, defaultPort) = when {
                type.contains("androidtvremote2", ignoreCase = true) -> Triple(
                    "ANDROID_TV", "ANDROID_TV_V2", 6467
                )
                type.contains("googlecast", ignoreCase = true) -> Triple(
                    "GOOGLE_TV", "GOOGLE_CAST", 8009
                )
                type.contains("airplay", ignoreCase = true) -> Triple(
                    "APPLE_TV", "AIRPLAY", 7000
                )
                else -> Triple("UNKNOWN", "UNKNOWN", port)
            }

            val deviceId = "${brand}_${ip}_${port}"
            val finalPort = if (port > 0) port else defaultPort

            val tv = DiscoveredTv(
                deviceId = deviceId,
                name = name,
                ip = ip,
                port = finalPort,
                brand = brand,
                protocol = protocol,
                modelName = info.getPropertyString("model") ?: "",
                macAddress = info.getPropertyString("mac") ?: "",
                hasChromecast = isChromecast
            )

            if (isThisDevice(name, ip)) return

            foundMap[deviceId] = tv
            _discoveredTvs.value = foundMap.values.toList()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isThisDevice(name: String, ip: String): Boolean {
        return try {
            val myIp = getLocalIpAddress() ?: return false
            val myName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
            ip == myIp || name.contains(myName, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    private fun getLocalIpAddress(): String? {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ipInt = wifiManager.connectionInfo.ipAddress
            if (ipInt != 0) {
                @Suppress("DEPRECATION")
                android.text.format.Formatter.formatIpAddress(ipInt)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun destroy() {
        stopScan()
    }
}
