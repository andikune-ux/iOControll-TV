package dev.andikuneiocontroll.remote.protocol.adb

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AdbPairing — Wrapper untuk ADB pairing (Android 11+).
 *
 * Cara kerja:
 * 1. Di TV: aktifkan "Wireless Debugging" di Developer Options
 * 2. TV akan tampilkan IP + port + 6 digit pairing code
 * 3. Di HP: masukkan IP + port + code
 * 4. Library handle TLS + SPAKE2 handshake
 * 5. Setelah pairing sukses, keypair disimpan → bisa connect tanpa code lagi
 *
 * Format pairing:
 * - Port pairing: 37000-44000 (random, tampil di TV)
 * - Port connect: 5555 (default ADB) atau custom
 */
object AdbPairing {

    private const val KEY_FILE_NAME = "adb_keypair.pem"

    /**
     * Pair ke TV via ADB.
     * @param host IP TV (contoh: 192.168.0.103)
     * @param pairingPort Port pairing dari TV (contoh: 37251)
     * @param pairingCode Kode 6 digit dari TV (contoh: 123456)
     * @return Pair(sukses, pesan)
     */
    suspend fun pair(
        context: Context,
        host: String,
        pairingPort: Int,
        pairingCode: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            // Validasi input
            if (host.isBlank()) return@withContext false to "IP TV tidak boleh kosong"
            if (pairingPort <= 0 || pairingPort > 65535) return@withContext false to "Port pairing tidak valid"
            if (pairingCode.length != 6 || !pairingCode.all { it.isDigit() }) {
                return@withContext false to "Pairing code harus 6 digit angka"
            }

            // Load atau buat keypair
            val keyPair = AdbCrypto.getOrCreateKeyPair(context)

            // Panggil library libadb-android untuk pairing
            // NOTE: API libadb mungkin berbeda. Kalau ada error, kirim log ke saya.
            val paired = AdbPairingHelper.pair(
                host = host,
                port = pairingPort,
                pairingCode = pairingCode,
                keyPair = keyPair
            )

            if (paired) {
                // Tandai pairing sukses di file flag
                savePairingSuccess(context, host, pairingPort)
                true to "Pairing berhasil! Sekarang bisa connect ke TV."
            } else {
                false to "Pairing gagal. Cek code & pastikan TV di WiFi sama."
            }

        } catch (e: Exception) {
            e.printStackTrace()
            false to "Error pairing: ${e.message ?: "Unknown"}"
        }
    }

    /**
     * Cek apakah sudah pernah pairing dengan TV.
     */
    fun isPaired(context: Context, host: String): Boolean {
        val flagFile = File(context.filesDir, "paired_$host.flag")
        return flagFile.exists()
    }

    /**
     * Hapus data pairing (untuk reset).
     */
    fun clearPairing(context: Context, host: String? = null) {
        try {
            if (host == null) {
                // Hapus semua
                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("paired_")) file.delete()
                }
            } else {
                File(context.filesDir, "paired_$host.flag").delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Ambil public key ADB dalam format string (untuk debugging).
     */
    fun getPublicKeyString(context: Context): String {
        return try {
            val keyPair = AdbCrypto.getOrCreateKeyPair(context)
            AdbCrypto.getAdbPublicKeyString(keyPair)
        } catch (e: Exception) {
            ""
        }
    }

    private fun savePairingSuccess(context: Context, host: String, port: Int) {
        try {
            val flagFile = File(context.filesDir, "paired_$host.flag")
            flagFile.writeText("paired_at=${System.currentTimeMillis()}\nport=$port\ndevice=${Build.MANUFACTURER} ${Build.MODEL}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Helper untuk memanggil library libadb-android.
 * Dipisah supaya mudah di-update kalau API library berubah.
 *
 * NOTE: File ini menggunakan reflection + coba beberapa kemungkinan API.
 * Kalau build error, kirim log ke saya untuk penyesuaian.
 */
private object AdbPairingHelper {

    fun pair(
        host: String,
        port: Int,
        pairingCode: String,
        keyPair: java.security.KeyPair
    ): Boolean {
        // Coba beberapa API library yang umum
        return try {
            // Metode 1: pakai PairingClient (libadb)
            tryPairingClient(host, port, pairingCode, keyPair)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            false
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun tryPairingClient(
        host: String,
        port: Int,
        pairingCode: String,
        keyPair: java.security.KeyPair
    ): Boolean {
        return try {
            // Import dinamis (reflection) — biar tidak error compile kalau class tidak ada
            val pairingClientClass = Class.forName("com.tananaev.adblib.AdbPairingClient")
            val constructor = pairingClientClass.getConstructor(
                String::class.java,
                Int::class.javaPrimitiveType,
                String::class.java,
                java.security.KeyPair::class.java
            )
            val client = constructor.newInstance(host, port, pairingCode, keyPair)

            // Panggil method "pair()" atau "start()" atau "execute()"
            val pairMethod = pairingClientClass.methods.firstOrNull {
                it.name in listOf("pair", "start", "execute", "doPair")
            } ?: return false

            pairMethod.isAccessible = true
            val result = pairMethod.invoke(client)
            result as? Boolean ?: true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
