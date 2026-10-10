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
 * V3 fix:
 * - Dedup pakai stableKey (brand + name + mac), BUKAN deviceId/IP.
 * - Prioritaskan entri IPv4 saat merge.
 * - Filter akhir: hanya yang connectable (IPv4 only).
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
        combineJob = scope.launch {
            combine(
                mdnsDiscovery.discoveredTvs,
                ssdpDiscovery.discoveredTvs
            ) { mdns, ssdp ->
                val merged = mutableMapOf<String, DiscoveredTv>()

                fun put(tv: DiscoveredTv) {
                    val key = tv.stableKey
                    val existing = merged[key]
                    if (existing == null) {
                        merged[key] = tv
                    } else {
                        val newIsV4 = !tv.ip.contains(":")
                        val oldIsV4 = !existing.ip.contains(":")
                        val winner = when {
                            newIsV4 && !oldIsV4 -> tv
                            !newIsV4 && oldIsV4 -> existing
                            else -> if (tv.port > 0 && existing.port == 0) tv else existing
                        }
                        merged[key] = winner
                    }
                }

                mdns.forEach { put(it) }
                ssdp.forEach { put(it) }

                val filtered = TvFilter.filterOutSelf(context, merged.values.toList())
                TvFilter.filterOnlyTvs(filtered)
                    .filter { it.isConnectable }
            }.collect { list ->
                _discoveredTvs.value = list
            }
        }
    }

    fun startScan(durationMs: Long = 5000L) {
        if (_isScanning.value) return

        _isScanning.value = true
        _lastScanAt.value = System.currentTimeMillis()
        _discoveredTvs.value = emptyList()

        mdnsDiscovery.startScan(durationMs)
        ssdpDiscovery.startScan(durationMs)

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

    fun findTvByIp(ip: String): DiscoveredTv? {
        return _discoveredTvs.value.firstOrNull { it.ip == ip }
    }

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
