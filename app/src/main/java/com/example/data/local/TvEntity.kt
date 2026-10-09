package dev.andikuneiocontroll.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room — menyimpan data TV yang sudah pernah di-pair/discovered.
 *
 * @property id           ID unik (hash dari deviceId atau MAC)
 * @property deviceId     ID dari protokol discovery (MD5/IP)
 * @property customName   Nama yang diberikan user (contoh: "TV Ruang Tamu")
 * @property originalName Nama asli dari TV (contoh: "Samsung Smart TV")
 * @property brand        Brand TV: ANDROID_TV, SAMSUNG, LG, ROKU, PHILIPS, VIZIO, UNKNOWN
 * @property protocol     Protokol koneksi: ANDROID_TV_V2, SAMSUNG_TIZEN, LG_WEBOS, ROKU_ECP, ADB
 * @property ipAddress    IP Address TV
 * @property port         Port koneksi
 * @property modelName    Model TV (opsional)
 * @property pairingData  Data pairing (cert/token) — JSON string
 * @property isPaired     True kalau sudah di-pair
 * @property isFavorite   True kalau user tandai favorit
 * @property lastConnected Timestamp terakhir connect (millis)
 * @property createdAt    Timestamp pertama kali disimpan
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
    val lastConnected: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Nama tampilan: pakai customName kalau ada, kalau tidak pakai originalName */
    val displayName: String
        get() = customName.ifBlank { originalName.ifBlank { ipAddress } }
}

/**
 * Enum brand TV.
 */
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

/**
 * Enum protokol koneksi.
 */
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
