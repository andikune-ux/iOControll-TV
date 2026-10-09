package dev.andikuneiocontroll.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room — menyimpan data TV yang sudah pernah di-pair/discovered.
 *
 * V2 (Update):
 * - Tambah field hasChromecast
 * - Tambah field firmwareVersion (opsional)
 */
@Entity(tableName = "tv_devices")
data class TvEntity(
    @PrimaryKey
    val id: String,
    val deviceId: String,
    val customName: String = "",
    val originalName: String = "",
    val brand: String = "UNKNOWN",
    val protocol: String = "UNKNOWN",
    val ipAddress: String,
    val port: Int = 0,
    val modelName: String = "",
    val pairingData: String = "",
    val isPaired: Boolean = false,
    val isFavorite: Boolean = false,
    val hasChromecast: Boolean = false,
    val firmwareVersion: String = "",
    val lastConnected: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = customName.ifBlank { originalName.ifBlank { ipAddress } }
}

enum class TvBrandEnum {
    ANDROID_TV,
    GOOGLE_TV,
    SAMSUNG,
    LG,
    ROKU,
    PHILIPS,
    VIZIO,
    FIRE_TV,
    APPLE_TV,
    UNKNOWN;

    companion object {
        fun fromString(value: String): TvBrandEnum {
            return try {
                valueOf(value.uppercase())
            } catch (_: Exception) {
                UNKNOWN
            }
        }
    }
}

enum class TvProtocolEnum {
    ANDROID_TV_V2,
    SAMSUNG_TIZEN,
    LG_WEBOS,
    ROKU_ECP,
    PHILIPS_JOINTSPACE,
    VIZIO_SMARTCAST,
    ADB_WIFI,
    UNKNOWN;

    companion object {
        fun fromString(value: String): TvProtocolEnum {
            return try {
                valueOf(value.uppercase())
            } catch (_: Exception) {
                UNKNOWN
            }
        }
    }
}
