package dev.andikuneiocontroll.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class TvRepository(context: Context) {

    private val TAG = "TvRepository"

    private val dao: TvDao? = try {
        TvDatabase.getInstance(context).tvDao()
    } catch (e: Exception) {
        Log.e(TAG, "Gagal init Room database: ${e.message}")
        null
    }

    // ==========================================
    // READ
    // ==========================================

    fun getAllTvs(): Flow<List<TvEntity>> {
        return try {
            dao?.getAllTvs() ?: flowOf(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "getAllTvs error: ${e.message}")
            flowOf(emptyList())
        }
    }

    fun getFavoriteTvs(): Flow<List<TvEntity>> {
        return try {
            dao?.getFavoriteTvs() ?: flowOf(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "getFavoriteTvs error: ${e.message}")
            flowOf(emptyList())
        }
    }

    suspend fun getTvById(id: String): TvEntity? {
        return try {
            dao?.getTvById(id)
        } catch (e: Exception) {
            Log.e(TAG, "getTvById error: ${e.message}")
            null
        }
    }

    suspend fun getTvByDeviceId(deviceId: String): TvEntity? {
        return try {
            dao?.getTvByDeviceId(deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "getTvByDeviceId error: ${e.message}")
            null
        }
    }

    suspend fun getLastConnectedTv(): TvEntity? {
        return try {
            dao?.getLastConnectedTv()
        } catch (e: Exception) {
            Log.e(TAG, "getLastConnectedTv error: ${e.message}")
            null
        }
    }

    suspend fun count(): Int {
        return try {
            dao?.count() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // ==========================================
    // WRITE
    // ==========================================

    suspend fun insertTv(tv: TvEntity) {
        try {
            dao?.insertTv(tv)
        } catch (e: Exception) {
            Log.e(TAG, "insertTv error: ${e.message}")
        }
    }

    suspend fun insertAll(tvs: List<TvEntity>) {
        try {
            dao?.insertAll(tvs)
        } catch (e: Exception) {
            Log.e(TAG, "insertAll error: ${e.message}")
        }
    }

    suspend fun updateTv(tv: TvEntity) {
        try {
            dao?.updateTv(tv)
        } catch (e: Exception) {
            Log.e(TAG, "updateTv error: ${e.message}")
        }
    }

    suspend fun deleteTv(tv: TvEntity) {
        try {
            dao?.deleteTv(tv)
        } catch (e: Exception) {
            Log.e(TAG, "deleteTv error: ${e.message}")
        }
    }

    suspend fun deleteTvById(id: String) {
        try {
            dao?.deleteTvById(id)
        } catch (e: Exception) {
            Log.e(TAG, "deleteTvById error: ${e.message}")
        }
    }

    suspend fun deleteAll() {
        try {
            dao?.deleteAll()
        } catch (e: Exception) {
            Log.e(TAG, "deleteAll error: ${e.message}")
        }
    }

    // ==========================================
    // UPDATE FIELD
    // ==========================================

    suspend fun updateLastConnected(id: String) {
        try {
            dao?.updateLastConnected(id, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "updateLastConnected error: ${e.message}")
        }
    }

    suspend fun updatePairingData(id: String, data: String) {
        try {
            dao?.updatePairingData(id, data)
        } catch (e: Exception) {
            Log.e(TAG, "updatePairingData error: ${e.message}")
        }
    }

    suspend fun updateCustomName(id: String, name: String) {
        try {
            dao?.updateCustomName(id, name)
        } catch (e: Exception) {
            Log.e(TAG, "updateCustomName error: ${e.message}")
        }
    }

    suspend fun toggleFavorite(id: String) {
        try {
            dao?.toggleFavorite(id)
        } catch (e: Exception) {
            Log.e(TAG, "toggleFavorite error: ${e.message}")
        }
    }

    // ==========================================
    // HELPER
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
    ): TvEntity? {
        return try {
            val existing = dao?.getTvByDeviceId(deviceId)

            if (existing != null) {
                val updated = existing.copy(
                    originalName = originalName.ifBlank { existing.originalName },
                    ipAddress = ipAddress,
                    port = port,
                    brand = brand,
                    protocol = protocol,
                    modelName = modelName.ifBlank { existing.modelName },
                    hasChromecast = hasChromecast || existing.hasChromecast
                )
                dao?.updateTv(updated)
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
                dao?.insertTv(newTv)
                newTv
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveOrUpdateFromDiscovery error: ${e.message}")
            null
        }
    }

    suspend fun getAutoConnectTv(): TvEntity? {
        return try {
            dao?.getLastConnectedTv()
        } catch (e: Exception) {
            Log.e(TAG, "getAutoConnectTv error: ${e.message}")
            null
        }
    }

    private fun generateId(deviceId: String): String {
        return "tv_${deviceId.hashCode().toString(16)}"
    }
}
