package dev.andikuneiocontroll.remote.discovery

/**
 * Data class hasil discovery TV.
 * Dipakai oleh semua method discovery (mDNS, SSDP, Roku).
 *
 * V3 (Update):
 * - Tambah helper isConnectable / isIpv6LinkLocal / stableKey
 * - Dedup key stabil (bukan pakai IP)
 */
data class DiscoveredTv(
    /** ID unik dari protokol discovery */
    val deviceId: String,

    /** Nama asli dari TV (contoh: "Samsung Smart TV") */
    val name: String,

    /** IP Address (IPv4 — wajib untuk connect) */
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

    /** True kalau TV support Chromecast built-in */
    val hasChromecast: Boolean = false,

    /** Timestamp ditemukan */
    val discoveredAt: Long = System.currentTimeMillis()
) {
    /** Nama tampilan — pakai name, fallback ke ip */
    val displayName: String
        get() = name.ifBlank { ip }

    /**
     * Kunci stabil untuk dedup — TIDAK pakai IP.
     * Prioritas: macAddress > deviceId tanpa IP > name
     */
    val stableKey: String
        get() {
            if (macAddress.isNotBlank()) return "mac:$macAddress"
            val cleaned = deviceId
                .replace(Regex("_?\\d+\\.\\d+\\.\\d+\\.\\d+_?"), "_")
                .replace(Regex("_?[0-9a-fA-F:]{3,}_?"), "_")
            return "id:$brand:${cleaned.ifBlank { name }}"
        }

    /** True kalau IP bisa dipakai untuk connect langsung */
    val isConnectable: Boolean
        get() = ip.isNotBlank() && !isIpv6LinkLocal(ip) && !isIpv6(ip)

    private fun isIpv6(host: String) = host.contains(":")

    private fun isIpv6LinkLocal(host: String): Boolean {
        val lower = host.lowercase()
        return lower.startsWith("fe80:") || lower.startsWith("fe80%")
    }
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
