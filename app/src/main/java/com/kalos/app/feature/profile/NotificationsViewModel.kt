package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.notification.IntelligentReminderScheduler
import com.kalos.app.core.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramReminderEntry(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val reminderEnabled: Boolean,
)

data class NotificationsUiState(
    val programs: List<ProgramReminderEntry> = emptyList(),
    val notifHour: Int = 8,
    val smartEnabled: Boolean = false,
    val smartNutrition: Boolean = true,
    val smartWorkout: Boolean = true,
    val smartHydration: Boolean = false,
    val inactivityDays: Int = 3,
    val smartHour: Int = 20,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val reminderScheduler: ReminderScheduler,
    private val smartScheduler: IntelligentReminderScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(
        NotificationsUiState(
            notifHour = reminderScheduler.getNotifHour(),
            smartEnabled = smartScheduler.isEnabled(),
            smartNutrition = smartScheduler.isNutritionEnabled(),
            smartWorkout = smartScheduler.isWorkoutEnabled(),
            smartHydration = smartScheduler.isHydrationEnabled(),
            inactivityDays = smartScheduler.getInactivityDays(),
            smartHour = smartScheduler.getHour(),
        )
    )
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            programRepository.getAll().collect { programs ->
                _state.update { s ->
                    s.copy(programs = programs.map { p ->
                        ProgramReminderEntry(
                            id = p.id,
                            name = p.name,
                            isActive = p.isActive,
                            reminderEnabled = reminderScheduler.isProgramEnabled(p.id),
                        )
                    })
                }
            }
        }
    }

    fun setNotifHour(hour: Int) {
        reminderScheduler.setNotifHour(hour)
        _state.update { it.copy(notifHour = hour) }
    }

    fun setProgramEnabled(id: Long, v: Boolean) {
        reminderScheduler.setProgramEnabled(id, v)
        _state.update { s ->
            s.copy(programs = s.programs.map { if (it.id == id) it.copy(reminderEnabled = v) else it })
        }
    }

    fun setSmartEnabled(v: Boolean) { smartScheduler.setEnabled(v); _state.update { it.copy(smartEnabled = v) } }
    fun setSmartNutrition(v: Boolean) { smartScheduler.setNutritionEnabled(v); _state.update { it.copy(smartNutrition = v) } }
    fun setSmartWorkout(v: Boolean) { smartScheduler.setWorkoutEnabled(v); _state.update { it.copy(smartWorkout = v) } }
    fun setSmartHydration(v: Boolean) { smartScheduler.setHydrationEnabled(v); _state.update { it.copy(smartHydration = v) } }
    fun setInactivityDays(days: Int) { smartScheduler.setInactivityDays(days); _state.update { it.copy(inactivityDays = days) } }
    fun setSmartHour(hour: Int) { smartScheduler.setHour(hour); _state.update { it.copy(smartHour = hour) } }
}
