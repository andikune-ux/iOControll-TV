package dev.andikuneiocontroll.remote.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.speech.RecognizerIntent
import android.view.inputmethod.InputMethodManager
import dev.andikuneiocontroll.remote.protocol.TvCommand

/**
 * VoiceController — kirim perintah suara ke TV.
 * Menggunakan Google Voice via Intent Recognition.
 */
class VoiceController(private val context: Context) {

    /**
     * Create voice intent untuk di-launch via ActivityResultLauncher.
     */
    fun createVoiceIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan perintah…")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    /**
     * Ambil teks hasil voice recognition dari Intent result.
     */
    fun extractTextFromResult(data: Intent?): String {
        if (data == null) return ""
        val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        return results?.firstOrNull() ?: ""
    }

    /**
     * Kirim teks ke TV (input text).
     */
    suspend fun sendTextToTv(controller: RemoteController, text: String): Boolean {
        if (text.isBlank()) return false
        return controller.sendCommand(TvCommand.INPUT_TEXT, text)
    }
}

/**
 * KeyboardController — input text ke TV via IME HP.
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
     * Kirim backspace.
     */
    suspend fun sendBackspace(controller: RemoteController): Boolean {
        return controller.sendCommand(TvCommand.KEY_DELETE)
    }

    /**
     * Kirim enter.
     */
    suspend fun sendEnter(controller: RemoteController): Boolean {
        return controller.sendCommand(TvCommand.KEY_ENTER)
    }

    /**
     * Sembunyikan keyboard.
     */
    fun hideKeyboard(activity: Activity?) {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            activity?.currentFocus?.let {
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
        } catch (_: Exception) {}
    }
}

/**
 * MouseController — kontrol kursor TV.
 */
class MouseController(private val context: Context) {

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
     * Klik.
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
     * Get MediaProjectionManager untuk request screen capture.
     */
    fun getMediaProjectionManager(): MediaProjectionManager? {
        return try {
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Create screen capture intent.
     */
    fun createScreenCaptureIntent(): Intent? {
        return try {
            getMediaProjectionManager()?.createScreenCaptureIntent()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Start cast (placeholder — implementasi penuh butuh Chromecast SDK).
     */
    fun startCast() {
        isCasting = true
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
     * Daftar shortcut default.
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
