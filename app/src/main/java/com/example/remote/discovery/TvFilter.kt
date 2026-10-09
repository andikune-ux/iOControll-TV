package dev.andikuneiocontroll.remote.discovery

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

/**
 * TvFilter — filter HP sendiri dari hasil discovery.
 *
 * Kenapa perlu? Karena HP juga bisa nge-broadcast SSDP/mDNS,
 * jadi kita harus exclude HP sendiri dari daftar TV.
 */
object TvFilter {

    /**
     * Cek apakah DiscoveredTv ini adalah HP kita sendiri.
     */
    fun isThisDevice(context: Context, tv: DiscoveredTv): Boolean {
        // Cek 1: IP sama dengan IP HP?
        if (tv.ip == getLocalIpAddress(context)) return true

        // Cek 2: Nama mengandung model HP?
        val myDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}".lowercase()
        val tvName = tv.name.lowercase()

        if (tvName.contains(Build.MODEL.lowercase())) return true
        if (tvName.contains(Build.MANUFACTURER.lowercase()) &&
            tvName.contains(Build.MODEL.lowercase())) return true

        // Cek 3: MAC address sama dengan MAC HP?
        val myMac = getLocalMac(context)
        if (myMac.isNotEmpty() && tv.macAddress.isNotEmpty()) {
            if (tv.macAddress.equals(myMac, ignoreCase = true)) return true
        }

        // Cek 4: deviceId mengandung Android ID HP?
        val myAndroidId = getAndroidId(context)
        if (tv.deviceId.contains(myAndroidId)) return true

        return false
    }

    /**
     * Filter list DiscoveredTv, buang HP sendiri.
     */
    fun filterOutSelf(context: Context, tvs: List<DiscoveredTv>): List<DiscoveredTv> {
        return tvs.filter { !isThisDevice(context, it) }
    }

    /**
     * Filter tambahan: buang device yang bukan TV (misal: printer, router).
     */
    fun filterOnlyTvs(tvs: List<DiscoveredTv>): List<DiscoveredTv> {
        val validBrands = setOf(
            "ANDROID_TV", "GOOGLE_TV", "SAMSUNG", "LG", "ROKU",
            "PHILIPS", "VIZIO", "FIRE_TV", "SONY", "PANASONIC",
            "TCL", "HISENSE", "SHARP", "TOSHIBA"
        )
        return tvs.filter { it.brand.uppercase() in validBrands }
    }

    // ==========================================
    // UTIL
    // ==========================================

    private fun getLocalIpAddress(context: Context): String {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ipInt = wifiManager.connectionInfo.ipAddress
            if (ipInt != 0) {
                @Suppress("DEPRECATION")
                android.text.format.Formatter.formatIpAddress(ipInt)
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    @Suppress("DEPRECATION")
    private fun getLocalMac(context: Context): String {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.connectionInfo.macAddress ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun getAndroidId(context: Context): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}
