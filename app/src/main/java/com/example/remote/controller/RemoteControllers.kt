package dev.andikuneiocontroll.remote.controller

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.inputmethod.InputMethodManager
import dev.andikuneiocontroll.remote.protocol.TvCommand

/**
 * VoiceController — kirim perintah suara ke TV.
 * Menggunakan Google Voice via ADB (untuk Android TV).
 */
class VoiceController(private val context: Context) {

    /**
     * Start voice input dari HP.
     * Return Intent yang harus di-launch via ActivityResultLauncher.
     */
    fun createVoiceIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan perintah…")
        }
    }

    /**
     * Kirim text hasil voice ke TV via ADB.
     */
    suspend fun sendVoiceText(controller: RemoteController, text: String): Boolean {
        // Pakai ADB command untuk search
        return controller.sendCommand("LAUNCH_APP", "com.google.android.katniss")
    }
}

/**
 * KeyboardController — input text ke TV.
 * Pakai IME HP.
 */
class KeyboardController(private val context: Context) {

    /**
     * Kirim text ke TV.
     */
    suspend fun sendText(controller: RemoteController, text: String): Boolean {
        if (text.isBlank()) return false
        return controller.sendCommand(TvCommand.INPUT_TEXT, text)
    }

    /**
     * Hapus karakter.
     */
    suspend fun sendBackspace(controller: RemoteController): Boolean {
        return controller.sendCommand(TvCommand.KEY_DELETE)
    }

    /**
     * Enter.
     */
    suspend fun sendEnter(controller: RemoteController): Boolean {
        return controller.sendCommand(TvCommand.KEY_ENTER)
    }

    /**
     * Sembunyikan keyboard.
     */
    fun hideKeyboard() {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        } catch (_: Exception) {}
    }
}

/**
 * MouseController — kontrol kursor TV.
 * - Touchpad mode: drag = gerak kursor
 * - Air mouse mode: gyroscope
 */
class MouseController(private val context: Context) {

    // Sensitivitas (0-100)
    @Volatile
    var sensitivity: Int = 50

    /**
     * Kirim gerakan kursor.
     */
    suspend fun move(controller: RemoteController, dx: Float, dy: Float): Boolean {
        if (dx == 0f && dy == 0f) return false
        val factor = sensitivity / 50f
        val scaledDx = dx * factor
        val scaledDy = dy * factor
        return controller.sendCommand(TvCommand.MOUSE_MOVE, "$scaledDx|$scaledDy")
    }

    /**
     * Klik kiri.
     */
    suspend fun click(controller: RemoteController): Boolean {
        return controller.sendCommand(TvCommand.MOUSE_CLICK)
    }

    /**
     * Klik kanan.
     */
    suspend fun rightClick(controller: RemoteController): Boolean {
        return controller.sendCommand(TvCommand.MOUSE_RIGHT_CLICK)
    }

    /**
     * Scroll.
     */
    suspend fun scroll(controller: RemoteController, amount: Float): Boolean {
        return controller.sendCommand(TvCommand.MOUSE_SCROLL, amount.toString())
    }
}

/**
 * ScreenCastController — cast layar HP ke TV.
 * Menggunakan MediaProjection + Chromecast.
 */
class ScreenCastController(private val context: Context) {

    @Volatile
    var isCasting: Boolean = false
        private set

    /**
     * Start screen cast.
     * Untuk sekarang hanya return Intent placeholder.
     */
    fun createCastIntent(): Intent? {
        // Placeholder — implementasi penuh butuh MediaProjection
        return null
    }

    /**
     * Stop cast.
     */
    fun stopCast() {
        isCasting = false
    }
}

/**
 * ShortcutManager — custom shortcut (app quick launch).
 */
class ShortcutManager(private val context: Context) {

    /**
     * Launch app di TV berdasarkan package name.
     */
    suspend fun launchApp(controller: RemoteController, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return controller.sendCommand(TvCommand.LAUNCH_APP, packageName)
    }

    /**
     * Daftar shortcut default (app populer di Android TV).
     */
    fun getDefaultShortcuts(): List<AppShortcut> {
        return listOf(
            AppShortcut("YouTube", "com.google.android.youtube.tv"),
            AppShortcut("Netflix", "com.netflix.ninja"),
            AppShortcut("Prime Video", "com.amazon.amazonvideo.livingroom"),
            AppShortcut("Disney+", "com.disney.disneyplus"),
            AppShortcut("Spotify", "com.spotify.tv.android"),
            AppShortcut("VLC", "org.videolan.vlc"),
            AppShortcut("Plex", "com.plexapp.android"),
            AppShortcut("Chrome", "com.android.chrome"),
            AppShortcut("Play Store", "com.android.vending"),
            AppShortcut("Settings", "com.android.tv.settings")
        )
    }
}

data class AppShortcut(
    val name: String,
    val packageName: String
)
