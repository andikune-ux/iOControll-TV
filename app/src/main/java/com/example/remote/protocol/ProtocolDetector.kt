package dev.andikuneiocontroll.remote.protocol

import dev.andikuneiocontroll.remote.discovery.DiscoveredTv

/**
 * ProtocolDetector — deteksi protokol yang cocok berdasarkan hasil discovery.
 *
 * Dipakai untuk memilih client mana yang harus dipakai untuk connect ke TV.
 */
object ProtocolDetector {

    /**
     * Dapatkan protokol berdasarkan DiscoveredTv.
     * Prioritas: protocol field dari discovery → fallback ke brand.
     */
    fun detect(tv: DiscoveredTv): String {
        // Cek 1: protocol sudah ada dari discovery
        if (tv.protocol.isNotBlank() && tv.protocol != "UNKNOWN") {
            return tv.protocol.uppercase()
        }

        // Cek 2: fallback berdasarkan brand
        return when (tv.brand.uppercase()) {
            "ANDROID_TV", "GOOGLE_TV", "SONY", "TCL", "HISENSE", "SHARP", "TOSHIBA" ->
                "ANDROID_TV_V2"

            "SAMSUNG" -> "SAMSUNG_TIZEN"

            "LG" -> "LG_WEBOS"

            "ROKU" -> "ROKU_ECP"

            "PHILIPS" -> "PHILIPS_JOINTSPACE"

            "VIZIO" -> "VIZIO_SMARTCAST"

            "FIRE_TV" -> "ADB_WIFI"

            "APPLE_TV" -> "AIRPLAY"

            else -> "UNKNOWN"
        }
    }

    /**
     * Cek apakah protokol butuh pairing PIN.
     */
    fun needsPairing(protocol: String): Boolean {
        return when (protocol.uppercase()) {
            "ANDROID_TV_V2" -> true
            "SAMSUNG_TIZEN" -> true
            "LG_WEBOS" -> true
            "ADB_WIFI" -> false  // ADB pakai RSA key, bukan PIN
            "ROKU_ECP" -> false  // Roku langsung connect
            "PHILIPS_JOINTSPACE" -> false
            "VIZIO_SMARTCAST" -> true
            else -> false
        }
    }

    /**
     * Dapatkan port default per protokol.
     */
    fun defaultPort(protocol: String): Int {
        return when (protocol.uppercase()) {
            "ANDROID_TV_V2" -> 6467
            "SAMSUNG_TIZEN" -> 8001
            "LG_WEBOS" -> 3000
            "ROKU_ECP" -> 8060
            "PHILIPS_JOINTSPACE" -> 1925
            "VIZIO_SMARTCAST" -> 9000
            "ADB_WIFI" -> 5555
            "AIRPLAY" -> 7000
            else -> 0
        }
    }

    /**
     * Dapatkan label protokol untuk ditampilkan di UI.
     */
    fun protocolLabel(protocol: String): String {
        return when (protocol.uppercase()) {
            "ANDROID_TV_V2" -> "Android TV Remote v2"
            "SAMSUNG_TIZEN" -> "Samsung Tizen"
            "LG_WEBOS" -> "LG webOS"
            "ROKU_ECP" -> "Roku ECP"
            "PHILIPS_JOINTSPACE" -> "Philips JointSpace"
            "VIZIO_SMARTCAST" -> "Vizio SmartCast"
            "ADB_WIFI" -> "ADB Wi-Fi"
            "AIRPLAY" -> "Apple AirPlay"
            "GOOGLE_CAST" -> "Google Cast"
            else -> "Unknown"
        }
    }

    /**
     * Cek apakah protokol ini didukung oleh aplikasi.
     */
    fun isSupported(protocol: String): Boolean {
        return protocol.uppercase() in listOf(
            "ANDROID_TV_V2",
            "SAMSUNG_TIZEN",
            "LG_WEBOS",
            "ROKU_ECP",
            "PHILIPS_JOINTSPACE",
            "VIZIO_SMARTCAST"
        )
    }
}
