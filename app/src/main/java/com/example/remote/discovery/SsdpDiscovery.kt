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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

/**
 * SsdpDiscovery — deteksi TV via SSDP (Simple Service Discovery Protocol).
 *
 * SSDP menggunakan UDP multicast di 239.255.255.250:1900.
 * Mendukung:
 * - Samsung Smart TV (Tizen)
 * - LG Smart TV (webOS)
 * - Philips TV (JointSpace)
 * - Roku TV
 * - Sony, Panasonic, dll.
 *
 * Hasil dikumpulkan di MutableStateFlow<List<DiscoveredTv>>.
 */
class SsdpDiscovery(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var scanJob: Job? = null
    private val foundMap = mutableMapOf<String, DiscoveredTv>()

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
                val wifiLock = wifiManager.createMulticastLock("iocontroll_ssdp").apply {
                    setReferenceCounted(false)
                    acquire()
                }

                try {
                    val socket = DatagramSocket().apply {
                        soTimeout = 1000
                        broadcast = true
                        reuseAddress = true
                    }

                    // Kirim M-SEARCH 3x (interval 500ms) — pakai ST: ssdp:all
                    val startTime = System.currentTimeMillis()
                    var lastSendTime = 0L

                    while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                        val now = System.currentTimeMillis()

                        // Kirim M-SEARCH setiap 500ms
                        if (now - lastSendTime > 500) {
                            sendMSearch(socket, "ssdp:all")
                            lastSendTime = now
                        }

                        // Terima reply
                        try {
                            val buf = ByteArray(4096)
                            val packet = DatagramPacket(buf, buf.size)
                            socket.receive(packet)

                            val response = String(packet.data, 0, packet.length)
                            val senderIp = packet.address.hostAddress ?: continue

                            handleSsdpResponse(response, senderIp)

                        } catch (_: java.net.SocketTimeoutException) {
                            // timeout normal — lanjut
                        } catch (e: Exception) {
                            if (isActive) e.printStackTrace()
                        }
                    }

                    socket.close()

                } finally {
                    try {
                        if (wifiLock.isHeld) wifiLock.release()
                    } catch (_: Exception) {}
                }

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

    private fun sendMSearch(socket: DatagramSocket, searchTarget: String) {
        try {
            val message = buildString {
                append("M-SEARCH * HTTP/1.1\r\n")
                append("HOST: 239.255.255.250:1900\r\n")
                append("MAN: \"ssdp:discover\"\r\n")
                append("MX: 2\r\n")
                append("ST: $searchTarget\r\n")
                append("\r\n")
            }

            val group = InetAddress.getByName("239.255.255.250")
            val data = message.toByteArray()
            val packet = DatagramPacket(data, data.size, group, 1900)

            socket.send(packet)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun handleSsdpResponse(response: String, senderIp: String) {
        try {
            // Parse header
            val headers = parseHeaders(response)
            val location = headers["location"] ?: ""
            val server = headers["server"] ?: ""
            val st = headers["st"] ?: headers["nt"] ?: ""
            val usn = headers["usn"] ?: ""

            // Tentukan brand dari USN/ST/Server
            val (brand, protocol, port) = detectBrandAndProtocol(st, usn, server, location)

            if (brand == "UNKNOWN") return

            // Skip kalau bukan TV (misal: router, printer)
            if (!isTvBrand(brand)) return

            // Cari nama device — perlu fetch LOCATION (deskripsi XML)
            val name = extractNameFromUsn(usn) ?: "$brand TV"
            val deviceId = "${brand}_${senderIp}_${port}"

            // Filter HP sendiri
            if (senderIp == getLocalIpAddress()) return

            val tv = DiscoveredTv(
                deviceId = deviceId,
                name = name,
                ip = senderIp,
                port = port,
                brand = brand,
                protocol = protocol,
                modelName = "",
                macAddress = ""
            )

            foundMap[deviceId] = tv
            _discoveredTvs.value = foundMap.values.toList()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseHeaders(response: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val lines = response.split("\r\n", "\n")
        for (line in lines) {
            val idx = line.indexOf(":")
            if (idx > 0) {
                val key = line.substring(0, idx).trim().lowercase()
                val value = line.substring(idx + 1).trim()
                map[key] = value
            }
        }
        return map
    }

    private fun detectBrandAndProtocol(
        st: String,
        usn: String,
        server: String,
        location: String
    ): Triple<String, String, Int> {
        val combined = "$st $usn $server $location".lowercase()

        return when {
            // Samsung Tizen
            combined.contains("samsung") ||
                combined.contains("tizen") ||
                combined.contains("msf:2012:device") -> Triple("SAMSUNG", "SAMSUNG_TIZEN", 8001)

            // LG webOS
            combined.contains("lg") && (
                combined.contains("webos") ||
                combined.contains("ssap") ||
                combined.contains("lgelectronics")
            ) -> Triple("LG", "LG_WEBOS", 3000)

            // Philips JointSpace
            combined.contains("philips") ||
                combined.contains("jointspace") -> Triple("PHILIPS", "PHILIPS_JOINTSPACE", 1925)

            // Roku
            combined.contains("roku") ||
                combined.contains("ecp") -> Triple("ROKU", "ROKU_ECP", 8060)

            // Vizio SmartCast
            combined.contains("vizio") ||
                combined.contains("smartcast") -> Triple("VIZIO", "VIZIO_SMARTCAST", 9000)

            // Sony (Android TV)
            combined.contains("sony") && combined.contains("android") -> Triple(
                "ANDROID_TV", "ANDROID_TV_V2", 6467
            )

            // Android TV generic
            combined.contains("androidtv") ||
                combined.contains("androidtvremote") -> Triple("ANDROID_TV", "ANDROID_TV_V2", 6467)

            else -> Triple("UNKNOWN", "UNKNOWN", 0)
        }
    }

    private fun isTvBrand(brand: String): Boolean {
        return brand in listOf(
            "SAMSUNG", "LG", "PHILIPS", "ROKU", "VIZIO",
            "ANDROID_TV", "GOOGLE_TV", "SONY", "PANASONIC", "TCL", "HISENSE"
        )
    }

    private fun extractNameFromUsn(usn: String): String? {
        // USN format: "uuid:xxxxx::urn:schemas-upnp-org:device:tvdevice:1"
        // Kita pakai bagian terakhir sebagai hint
        val parts = usn.split("::")
        if (parts.size > 1) {
            val type = parts.last()
            val typeName = type.substringAfterLast(":").replaceFirstChar { it.uppercase() }
            return "$typeName TV"
        }
        return null
    }

    private fun getLocalIpAddress(): String? {
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
            null
        } catch (_: Exception) {
            null
        }
    }

    fun destroy() {
        stopScan()
    }
}
