package dev.andikuneiocontroll.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TvDao {

    /** Ambil semua TV, urut terakhir connect */
    @Query("SELECT * FROM tv_devices ORDER BY isFavorite DESC, lastConnected DESC")
    fun getAllTvs(): Flow<List<TvEntity>>

    /** Ambil semua TV (non-Flow, untuk keperluan sekali pakai) */
    @Query("SELECT * FROM tv_devices ORDER BY isFavorite DESC, lastConnected DESC")
    suspend fun getAllTvsOnce(): List<TvEntity>

    /** Ambil TV berdasarkan ID */
    @Query("SELECT * FROM tv_devices WHERE id = :id LIMIT 1")
    suspend fun getTvById(id: String): TvEntity?

    /** Ambil TV berdasarkan deviceId (untuk cek duplikat) */
    @Query("SELECT * FROM tv_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getTvByDeviceId(deviceId: String): TvEntity?

    /** Ambil TV terakhir yang connect */
    @Query("SELECT * FROM tv_devices WHERE lastConnected > 0 ORDER BY lastConnected DESC LIMIT 1")
    suspend fun getLastConnectedTv(): TvEntity?

    /** Ambil TV favorit */
    @Query("SELECT * FROM tv_devices WHERE isFavorite = 1 ORDER BY lastConnected DESC")
    fun getFavoriteTvs(): Flow<List<TvEntity>>

    /** Insert TV baru */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTv(tv: TvEntity)

    /** Insert banyak TV */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tvs: List<TvEntity>)

    /** Update TV */
    @Update
    suspend fun updateTv(tv: TvEntity)

    /** Hapus TV */
    @Delete
    suspend fun deleteTv(tv: TvEntity)

    /** Hapus TV berdasarkan ID */
    @Query("DELETE FROM tv_devices WHERE id = :id")
    suspend fun deleteTvById(id: String)

    /** Hapus semua */
    @Query("DELETE FROM tv_devices")
    suspend fun deleteAll()

    /** Update lastConnected */
    @Query("UPDATE tv_devices SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: String, timestamp: Long)

    /** Update pairing data */
    @Query("UPDATE tv_devices SET pairingData = :data, isPaired = 1 WHERE id = :id")
    suspend fun updatePairingData(id: String, data: String)

    /** Update custom name */
    @Query("UPDATE tv_devices SET customName = :name WHERE id = :id")
    suspend fun updateCustomName(id: String, name: String)

    /** Toggle favorite */
    @Query("UPDATE tv_devices SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    /** Hitung total TV */
    @Query("SELECT COUNT(*) FROM tv_devices")
    suspend fun count(): Int
}
