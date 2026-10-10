package dev.andikuneiocontroll

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import dev.andikuneiocontroll.util.CrashHandler
import dev.andikuneiocontroll.util.DeviceTypeHelper

/**
 * LauncherActivity — Entry point aplikasi.
 *
 * - Deteksi TV / HP → set orientasi
 * - Cek crash log sebelum buka MainActivity
 */
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install crash handler
        try {
            CrashHandler.install(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Set orientasi berdasarkan device type
        requestedOrientation = try {
            DeviceTypeHelper.getPreferredOrientation(this)
        } catch (e: Exception) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Cek crash file
        val crashFile = try {
            CrashHandler.getLatestCrashFile(this)
        } catch (e: Exception) {
            null
        }

        val nextIntent = if (crashFile != null && crashFile.exists()) {
            Intent(this, CrashViewerActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }

        startActivity(nextIntent)
        finish()
    }
}
