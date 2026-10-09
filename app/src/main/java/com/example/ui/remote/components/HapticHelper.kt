package dev.andikuneiocontroll.ui.remote.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * HapticHelper — helper getar halus untuk tombol remote.
 *
 * 3 jenis getar:
 * - LIGHT: getar sangat halus (navigasi)
 * - MEDIUM: getar sedang (klik utama)
 * - HEAVY: getar kuat (power, exit)
 */
object HapticHelper {

    @Volatile
    var enabled: Boolean = true

    fun light(context: Context) = vibrate(context, 10L, 30)
    fun medium(context: Context) = vibrate(context, 25L, 60)
    fun heavy(context: Context) = vibrate(context, 50L, 100)

    private fun vibrate(context: Context, durationMs: Long, amplitude: Int) {
        if (!enabled) return

        try {
            val vibrator = getVibrator(context) ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(durationMs, amplitude)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }
}
