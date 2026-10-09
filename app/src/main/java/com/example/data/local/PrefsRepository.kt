package dev.andikuneiocontroll.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * PrefsRepository — DataStore untuk pengaturan aplikasi.
 *
 * Menyimpan:
 * - Flag onboarding sudah dilihat
 * - Haptic feedback on/off
 * - Sound effect on/off
 * - Ukuran tombol (SMALL/MEDIUM/LARGE)
 * - Sensitivitas air mouse
 * - Sensitivitas mouse
 * - TV terakhir yang connect
 * - Auto-connect on/off
 */
class PrefsRepository(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "iocontroll_prefs")

        // Keys
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_BUTTON_SIZE = stringPreferencesKey("button_size")
        private val KEY_AIR_MOUSE_SENSITIVITY = intPreferencesKey("air_mouse_sensitivity")
        private val KEY_MOUSE_SENSITIVITY = intPreferencesKey("mouse_sensitivity")
        private val KEY_LAST_TV_ID = stringPreferencesKey("last_tv_id")
        private val KEY_AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        private val KEY_THEME = stringPreferencesKey("theme")
    }

    // ==========================================
    // ONBOARDING
    // ==========================================

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    // ==========================================
    // HAPTIC
    // ==========================================

    val isHapticEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAPTIC_ENABLED] ?: true
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC_ENABLED] = enabled }
    }

    // ==========================================
    // SOUND
    // ==========================================

    val isSoundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOUND_ENABLED] ?: false
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    // ==========================================
    // BUTTON SIZE
    // ==========================================

    val buttonSize: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BUTTON_SIZE] ?: "MEDIUM"
    }

    suspend fun setButtonSize(size: String) {
        context.dataStore.edit { it[KEY_BUTTON_SIZE] = size }
    }

    // ==========================================
    // SENSITIVITY
    // ==========================================

    val airMouseSensitivity: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_AIR_MOUSE_SENSITIVITY] ?: 50
    }

    suspend fun setAirMouseSensitivity(value: Int) {
        context.dataStore.edit { it[KEY_AIR_MOUSE_SENSITIVITY] = value.coerceIn(0, 100) }
    }

    val mouseSensitivity: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_MOUSE_SENSITIVITY] ?: 40
    }

    suspend fun setMouseSensitivity(value: Int) {
        context.dataStore.edit { it[KEY_MOUSE_SENSITIVITY] = value.coerceIn(0, 100) }
    }

    // ==========================================
    // AUTO CONNECT
    // ==========================================

    val autoConnect: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_CONNECT] ?: true
    }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CONNECT] = enabled }
    }

    val lastTvId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_TV_ID] ?: ""
    }

    suspend fun setLastTvId(id: String) {
        context.dataStore.edit { it[KEY_LAST_TV_ID] = id }
    }

    // ==========================================
    // THEME
    // ==========================================

    val theme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "DARK"
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    // ==========================================
    // UTIL — Get sekali pakai
    // ==========================================

    suspend fun getOnboardingDoneOnce(): Boolean {
        return context.dataStore.data.first()[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun getHapticEnabledOnce(): Boolean {
        return context.dataStore.data.first()[KEY_HAPTIC_ENABLED] ?: true
    }

    suspend fun getAutoConnectOnce(): Boolean {
        return context.dataStore.data.first()[KEY_AUTO_CONNECT] ?: true
    }

    suspend fun getLastTvIdOnce(): String {
        return context.dataStore.data.first()[KEY_LAST_TV_ID] ?: ""
    }
}
