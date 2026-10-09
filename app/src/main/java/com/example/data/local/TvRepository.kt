package dev.andikuneiocontroll.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository — wrapper untuk akses TvDatabase.
 * Mengubah TvEntity <-> model domain (kalau perlu).
 */
class TvRepository(context: Context) {

    private val dao = TvDatabase.getInstance(context).tvDao()

    // ==========================================
    // READ
    // ==========================================

    fun getAllTvs(): Flow<List<TvEntity>> = dao.getAllTvs()

    fun getFavoriteTvs(): Flow<List<TvEntity>> = dao.getFavoriteTvs()

    suspend fun getTvById(id: String): TvEntity? = dao.getTvById(id)

    suspend fun getTvByDeviceId(deviceId: String): TvEntity? = dao.getTvByDeviceId(deviceId)

    suspend fun getLastConnectedTv(): TvEntity? = dao.getLastConnectedTv()

    suspend fun count(): Int = dao.count()

    // ==========================================
    // WRITE
    // ==========================================

    suspend fun insertTv(tv: TvEntity) = dao.insertTv(tv)

    suspend fun insertAll(tvs: List<TvEntity>) = dao.insertAll(tvs)

    suspend fun updateTv(tv: TvEntity) = dao.updateTv(tv)

    suspend fun deleteTv(tv: TvEntity) = dao.deleteTv(tv)

    suspend fun deleteTvById(id: String) = dao.deleteTvById(id)

    suspend fun deleteAll() = dao.deleteAll()

    // ==========================================
    // UPDATE FIELD
    // ==========================================

    suspend fun updateLastConnected(id: String) {
        dao.updateLastConnected(id, System.currentTimeMillis())
    }

    suspend fun updatePairingData(id: String, data: String) {
        dao.updatePairingData(id, data)
    }

    suspend fun updateCustomName(id: String, name: String) {
        dao.updateCustomName(id, name)
    }

    suspend fun toggleFavorite(id: String) {
        dao.toggleFavorite(id)
    }

    // ==========================================
    // HELPER — Simpan / Update dari hasil discovery
    // ==========================================

    /**
     * Simpan TV dari hasil discovery.
     * Kalau deviceId sudah ada, update info IP/port/name.
     * Kalau belum ada, insert baru.
     */
    suspend fun saveOrUpdateFromDiscovery(
        deviceId: String,
        originalName: String,
        ipAddress: String,
        port: Int,
        brand: String,
        protocol: String,
        modelName: String = ""
    ): TvEntity {
        val existing = dao.getTvByDeviceId(deviceId)

        return if (existing != null) {
            // Update info yang berubah (IP bisa berubah)
            val updated = existing.copy(
                originalName = originalName.ifBlank { existing.originalName },
                ipAddress = ipAddress,
                port = port,
                brand = brand,
                protocol = protocol,
                modelName = modelName.ifBlank { existing.modelName }
            )
            dao.updateTv(updated)
            updated
        } else {
            // Insert baru
            val newTv = TvEntity(
                id = generateId(deviceId),
                deviceId = deviceId,
                originalName = originalName,
                ipAddress = ipAddress,
                port = port,
                brand = brand,
                protocol = protocol,
                modelName = modelName,
                isPaired = false,
                lastConnected = 0L
            )
            dao.insertTv(newTv)
            newTv
        }
    }

    /**
     * Auto-connect ke TV terakhir yang connect.
     */
    suspend fun getAutoConnectTv(): TvEntity? {
        return dao.getLastConnectedTv()
    }

    // ==========================================
    // UTIL
    // ==========================================

    private fun generateId(deviceId: String): String {
        return "tv_${deviceId.hashCode().toString(16)}"
    }
}
