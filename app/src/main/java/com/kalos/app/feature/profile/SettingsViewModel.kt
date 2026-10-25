package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.kalos.app.core.data.DietaryPreferencesStore
import com.kalos.app.core.data.ThemePreferenceStore
import com.kalos.app.core.domain.model.DietaryFilter
import com.kalos.app.core.ui.theme.AppearanceSettings
import com.kalos.app.core.ui.theme.Brightness
import com.kalos.app.core.ui.theme.MotionPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsStore: DietaryPreferencesStore,
    private val themeStore: ThemePreferenceStore,
) : ViewModel() {
    val filters: StateFlow<Set<DietaryFilter>> = prefsStore.filtersFlow
    fun toggle(filter: DietaryFilter, enabled: Boolean) = prefsStore.setFilter(filter, enabled)

    val appearance: StateFlow<AppearanceSettings> = themeStore.settings
    fun setPalette(id: String) = themeStore.setPalette(id)
    fun setBrightness(brightness: Brightness) = themeStore.setBrightness(brightness)
    fun setMotion(motion: MotionPreference) = themeStore.setMotion(motion)
    fun setVibrations(enabled: Boolean) = themeStore.setVibrations(enabled)
}
