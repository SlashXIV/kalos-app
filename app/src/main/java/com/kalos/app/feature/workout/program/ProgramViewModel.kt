package com.kalos.app.feature.workout.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val programs: StateFlow<List<TrainingProgram>> = programRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun activate(programId: Long) {
        viewModelScope.launch {
            programRepository.activate(programId)
            reminderScheduler.schedule()
        }
    }
}
