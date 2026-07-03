package com.kalos.app.core.data

import android.content.Context
import com.kalos.app.core.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the chosen theme mode (system / light / dark) in SharedPreferences. */
@Singleton
class ThemePreferenceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("kalos_theme", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private fun read(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_MODE, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _mode.value = mode
    }

    private companion object {
        const val KEY_MODE = "mode"
    }
}
