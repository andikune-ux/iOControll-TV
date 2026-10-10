package dev.andikuneiocontroll

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dev.andikuneiocontroll.util.CrashHandler

/**
 * LauncherActivity — Entry point aplikasi.
 *
 * Cek crash log SEBELUM buka MainActivity:
 * - Ada crash → buka CrashViewerActivity (isolated, anti-crash)
 * - Tidak ada crash → buka MainActivity
 *
 * Activity ini SANGAAT SEDERHANA — hanya cek file.
 * Tidak ada Room, ViewModel, atau dependency apapun.
 */
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install crash handler
        try {
            CrashHandler.install(this)
        } catch (e: Exception) {
            // ignore
        }

        // Cek crash file
        val crashFile = try {
            CrashHandler.getLatestCrashFile(this)
        } catch (e: Exception) {
            null
        }

        val nextIntent = if (crashFile != null && crashFile.exists()) {
            // Ada crash → buka CrashViewerActivity
            Intent(this, CrashViewerActivity::class.java)
        } else {
            // Tidak ada crash → buka MainActivity
            Intent(this, MainActivity::class.java)
        }

        startActivity(nextIntent)
        finish()
    }
}
