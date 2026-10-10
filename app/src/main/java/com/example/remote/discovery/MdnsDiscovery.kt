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
import java.net.Inet4Address
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

/**
 * MdnsDiscovery — deteksi TV via mDNS/NSD.
 *
 * V3 fix:
 * - Pilih IPv4 eksplisit (bukan firstOrNull()) supaya tidak salah
 *   ambil IPv6 link-local (fe80::) yang tidak bisa di-connect.
 * - Dedup by stable key (brand + name + mac) — BUKAN pakai IP.
 *   1 TV = 1 entri, walau di-resolve berkali-kali.
 */
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

            // FIX: pilih IPv4 saja
            val ipv4 = addresses.filterIsInstance<Inet4Address>().firstOrNull()
            val ip: String = ipv4?.hostAddress
                ?: addresses
                    .firstOrNull { addr ->
                        val h = addr.hostAddress ?: return@firstOrNull false
                        !h.startsWith("fe80:", ignoreCase = true) && !h.contains(":")
                    }
                    ?.hostAddress
                ?: return

            if (ip.isBlank() || ip == "0.0.0.0") return

            val name = info.name ?: "Unknown TV"
            val port = info.port
            val type = event.type ?: ""

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

            val finalPort = if (port > 0) port else defaultPort

            if (isThisDevice(name, ip)) return

            // FIX DEDUP: stableKey (brand + name + mac)
            val mac = info.getPropertyString("mac") ?: ""
            val stableKey = buildString {
                append("mdns:")
                append(brand).append(":")
                append(name.lowercase().trim())
                if (mac.isNotBlank()) append(":").append(mac)
            }

            val tv = DiscoveredTv(
                deviceId = stableKey,
                name = name,
                ip = ip,
                port = finalPort,
                brand = brand,
                protocol = protocol,
                modelName = info.getPropertyString("model") ?: "",
                macAddress = mac,
                hasChromecast = isChromecast
            )

            val existing = foundMap[stableKey]
            if (existing != null) {
                val newIsV4 = !ip.contains(":")
                val oldIsV4 = !existing.ip.contains(":")
                val finalTv = if (newIsV4 && !oldIsV4) tv else existing
                foundMap[stableKey] = finalTv
            } else {
                foundMap[stableKey] = tv
            }

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
