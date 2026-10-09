package dev.andikuneiocontroll.remote.discovery

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * TvDiscoveryManager — orkestrator semua jenis discovery.
 *
 * Menggabungkan:
 * - MdnsDiscovery (Android TV, Google TV, Apple TV)
 * - SsdpDiscovery (Samsung, LG, Philips, Roku, Vizio)
 *
 * Output: daftar DiscoveredTv yang sudah di-filter (tanpa HP sendiri).
 */
class TvDiscoveryManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    val mdnsDiscovery = MdnsDiscovery(context, scope)
    val ssdpDiscovery = SsdpDiscovery(context, scope)

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastScanAt = MutableStateFlow(0L)
    val lastScanAt: StateFlow<Long> = _lastScanAt.asStateFlow()

    private var combineJob: Job? = null
    private var timeoutJob: Job? = null

    init {
        // Gabungkan hasil dari kedua discovery
        combineJob = scope.launch {
            combine(
                mdnsDiscovery.discoveredTvs,
                ssdpDiscovery.discoveredTvs
            ) { mdns, ssdp ->
                // Gabungkan, buang duplikat berdasarkan deviceId
                val merged = mutableMapOf<String, DiscoveredTv>()
                mdns.forEach { merged[it.deviceId] = it }
                ssdp.forEach { merged[it.deviceId] = it }

                // Filter HP sendiri + filter hanya TV
                val filtered = TvFilter.filterOutSelf(context, merged.values.toList())
                TvFilter.filterOnlyTvs(filtered)
            }.collect { list ->
                _discoveredTvs.value = list
            }
        }
    }

    /**
     * Mulai scan (mDNS + SSDP bersamaan).
     * @param durationMs Durasi total scan (default 5 detik).
     */
    fun startScan(durationMs: Long = 5000L) {
        if (_isScanning.value) return

        _isScanning.value = true
        _lastScanAt.value = System.currentTimeMillis()
        _discoveredTvs.value = emptyList()

        // Jalankan kedua scan bersamaan
        mdnsDiscovery.startScan(durationMs)
        ssdpDiscovery.startScan(durationMs)

        // Timer untuk stop otomatis
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(durationMs + 500)
            withContext(Dispatchers.Main) {
                _isScanning.value = false
            }
        }
    }

    fun stopScan() {
        mdnsDiscovery.stopScan()
        ssdpDiscovery.stopScan()
        timeoutJob?.cancel()
        timeoutJob = null
        _isScanning.value = false
    }

    fun clearResults() {
        _discoveredTvs.value = emptyList()
    }

    /**
     * Cari TV berdasarkan IP.
     */
    fun findTvByIp(ip: String): DiscoveredTv? {
        return _discoveredTvs.value.firstOrNull { it.ip == ip }
    }

    /**
     * Cari TV berdasarkan deviceId.
     */
    fun findTvByDeviceId(deviceId: String): DiscoveredTv? {
        return _discoveredTvs.value.firstOrNull { it.deviceId == deviceId }
    }

    fun destroy() {
        stopScan()
        combineJob?.cancel()
        mdnsDiscovery.destroy()
        ssdpDiscovery.destroy()
    }
}
