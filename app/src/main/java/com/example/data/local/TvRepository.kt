package dev.andikuneiocontroll.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow

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
    // HELPER — Save/Update dari discovery
    // ==========================================

    suspend fun saveOrUpdateFromDiscovery(
        deviceId: String,
        originalName: String,
        ipAddress: String,
        port: Int,
        brand: String,
        protocol: String,
        modelName: String = "",
        hasChromecast: Boolean = false
    ): TvEntity {
        val existing = dao.getTvByDeviceId(deviceId)

        return if (existing != null) {
            val updated = existing.copy(
                originalName = originalName.ifBlank { existing.originalName },
                ipAddress = ipAddress,
                port = port,
                brand = brand,
                protocol = protocol,
                modelName = modelName.ifBlank { existing.modelName },
                hasChromecast = hasChromecast || existing.hasChromecast
            )
            dao.updateTv(updated)
            updated
        } else {
            val newTv = TvEntity(
                id = generateId(deviceId),
                deviceId = deviceId,
                originalName = originalName,
                ipAddress = ipAddress,
                port = port,
                brand = brand,
                protocol = protocol,
                modelName = modelName,
                hasChromecast = hasChromecast,
                isPaired = false,
                lastConnected = 0L
            )
            dao.insertTv(newTv)
            newTv
        }
    }

    suspend fun getAutoConnectTv(): TvEntity? {
        return dao.getLastConnectedTv()
    }

    private fun generateId(deviceId: String): String {
        return "tv_${deviceId.hashCode().toString(16)}"
    }
}
