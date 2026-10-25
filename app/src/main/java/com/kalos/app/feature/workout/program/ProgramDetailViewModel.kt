package com.kalos.app.feature.workout.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramNotifState(
    val enabled: Boolean = false,
    val dayOf: Boolean = true,
    val dayBefore: Boolean = false,
)

@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _program = MutableStateFlow<TrainingProgram?>(null)
    val program: StateFlow<TrainingProgram?> = _program

    private val _notif = MutableStateFlow(ProgramNotifState())
    val notif: StateFlow<ProgramNotifState> = _notif

    fun load(id: Long) {
        viewModelScope.launch {
            _program.value = programRepository.getById(id)
            _notif.value = ProgramNotifState(
                enabled = reminderScheduler.isProgramEnabled(id),
                dayOf = reminderScheduler.isProgramDayOf(id),
                dayBefore = reminderScheduler.isProgramDayBefore(id),
            )
        }
    }

    fun activate() {
        val id = _program.value?.id ?: return
        viewModelScope.launch {
            programRepository.activate(id)
            reminderScheduler.schedule()
            _program.value = _program.value?.copy(isActive = true)
        }
    }

    fun setNotifEnabled(v: Boolean) {
        val id = _program.value?.id ?: return
        reminderScheduler.setProgramEnabled(id, v)
        _notif.value = _notif.value.copy(enabled = v)
    }

    fun setNotifDayOf(v: Boolean) {
        val id = _program.value?.id ?: return
        reminderScheduler.setProgramDayOf(id, v)
        _notif.value = _notif.value.copy(dayOf = v)
    }

    fun setNotifDayBefore(v: Boolean) {
        val id = _program.value?.id ?: return
        reminderScheduler.setProgramDayBefore(id, v)
        _notif.value = _notif.value.copy(dayBefore = v)
    }
}
