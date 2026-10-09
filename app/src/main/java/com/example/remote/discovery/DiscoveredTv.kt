package dev.andikuneiocontroll.remote.discovery

/**
 * Data class hasil discovery TV.
 * Dipakai oleh semua method discovery (mDNS, SSDP, Roku).
 */
data class DiscoveredTv(
    /** ID unik dari protokol discovery */
    val deviceId: String,

    /** Nama asli dari TV (contoh: "Samsung Smart TV") */
    val name: String,

    /** IP Address */
    val ip: String,

    /** Port untuk koneksi (protokol spesifik) */
    val port: Int = 0,

    /** Brand TV: ANDROID_TV, SAMSUNG, LG, ROKU, dll */
    val brand: String = "UNKNOWN",

    /** Protokol koneksi: ANDROID_TV_V2, SAMSUNG_TIZEN, dll */
    val protocol: String = "UNKNOWN",

    /** Model TV (opsional) */
    val modelName: String = "",

    /** MAC Address (opsional) */
    val macAddress: String = "",

    /** Timestamp ditemukan */
    val discoveredAt: Long = System.currentTimeMillis()
) {
    /** Nama tampilan — pakai name, fallback ke ip */
    val displayName: String
        get() = name.ifBlank { ip }
}

/**
 * State scan discovery.
 */
data class DiscoveryState(
    val isScanning: Boolean = false,
    val scanStartedAt: Long = 0L,
    val scanDurationMs: Long = 5000L,
    val foundCount: Int = 0,
    val error: String = ""
)
