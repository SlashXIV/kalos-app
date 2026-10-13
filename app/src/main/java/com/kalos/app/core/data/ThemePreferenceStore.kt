package com.kalos.app.core.data

import android.content.Context
import android.content.SharedPreferences
import com.kalos.app.core.ui.theme.AppearanceSettings
import com.kalos.app.core.ui.theme.Brightness
import com.kalos.app.core.ui.theme.MotionPreference
import com.kalos.app.core.ui.theme.migrateLegacyTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Persiste les réglages d'apparence (palette, luminosité, ambiance, vibrations) en SharedPreferences. */
@Singleton
class ThemePreferenceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read(prefs))
    val settings: StateFlow<AppearanceSettings> = _settings.asStateFlow()

    fun setPalette(id: String) = update { it.copy(paletteId = id) }
    fun setBrightness(brightness: Brightness) = update { it.copy(brightness = brightness) }
    fun setMotion(motion: MotionPreference) = update { it.copy(motion = motion) }
    fun setVibrations(enabled: Boolean) = update { it.copy(vibrations = enabled) }

    private fun update(change: (AppearanceSettings) -> AppearanceSettings) {
        val next = change(_settings.value)
        write(prefs, next)
        _settings.value = next
    }

    companion object {
        const val PREFS = "kalos_theme"
        private const val KEY_LEGACY_MODE = "mode"
        private const val KEY_PALETTE = "palette"
        private const val KEY_BRIGHTNESS = "brightness"
        private const val KEY_MOTION = "ambiance"
        private const val KEY_VIBRATIONS = "vibrations"

        /** Lecture synchrone, aussi utilisée par MainActivity avant l'injection (fond de fenêtre). */
        fun read(prefs: SharedPreferences): AppearanceSettings {
            val palette = prefs.getString(KEY_PALETTE, null)
            if (palette == null) {
                // Première ouverture après la 3.x : convertir l'ancien thème une fois pour toutes.
                val (id, brightness) = migrateLegacyTheme(prefs.getString(KEY_LEGACY_MODE, null) ?: "SYSTEM")
                return AppearanceSettings(paletteId = id, brightness = brightness).also { write(prefs, it) }
            }
            return AppearanceSettings(
                paletteId = palette,
                brightness = enumOr(prefs.getString(KEY_BRIGHTNESS, null), Brightness.SYSTEM),
                motion = enumOr(prefs.getString(KEY_MOTION, null), MotionPreference.EXPRESSIVE),
                vibrations = prefs.getBoolean(KEY_VIBRATIONS, true),
            )
        }

        private fun write(prefs: SharedPreferences, s: AppearanceSettings) {
            prefs.edit()
                .putString(KEY_PALETTE, s.paletteId)
                .putString(KEY_BRIGHTNESS, s.brightness.name)
                .putString(KEY_MOTION, s.motion.name)
                .putBoolean(KEY_VIBRATIONS, s.vibrations)
                .remove(KEY_LEGACY_MODE)
                .apply()
        }

        private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: default
    }
}
