package dev.andikuneiocontroll.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * DeviceTypeHelper — Deteksi tipe device: HP atau TV.
 *
 * Cara deteksi:
 * 1. Cek UiModeManager.currentModeType == UI_MODE_TYPE_TELEVISION
 * 2. Cek PackageManager.hasSystemFeature(FEATURE_LEANBACK) — Android TV
 * 3. Cek PackageManager.hasSystemFeature(FEATURE_TELEVISION) — Google TV
 *
 * Return:
 * - isTv() → true kalau TV, false kalau HP/tablet
 * - isPhone() → kebalikan isTv()
 */
object DeviceTypeHelper {

    fun isTv(context: Context): Boolean {
        return try {
            // Cek 1: UiModeManager
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
                return true
            }

            // Cek 2: Feature Leanback (Android TV)
            val pm = context.packageManager
            if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
                return true
            }

            // Cek 3: Feature Television (Google TV)
            if (pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
                return true
            }

            false
        } catch (e: Exception) {
            false
        }
    }

    fun isPhone(context: Context): Boolean = !isTv(context)

    /**
     * Get orientasi yang cocok untuk device ini.
     * - TV → Landscape
     * - HP → Portrait
     */
    fun getPreferredOrientation(context: Context): Int {
        return if (isTv(context)) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
