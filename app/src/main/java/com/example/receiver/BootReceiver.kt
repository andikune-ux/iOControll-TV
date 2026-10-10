package dev.andikuneiocontroll.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BootReceiver — Terima broadcast BOOT_COMPLETED.
 *
 * Dipakai untuk auto-start server WiFi saat device reboot
 * (kalau user mengaktifkan auto-start).
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "Device booted — cek auto-start server")
                try {
                    // Auto-start logika (bisa dikembangkan nanti)
                    // Saat ini hanya log — tidak ada aksi
                } catch (e: Exception) {
                    Log.e(TAG, "Error on boot: ${e.message}")
                }
            }
        }
    }
}
