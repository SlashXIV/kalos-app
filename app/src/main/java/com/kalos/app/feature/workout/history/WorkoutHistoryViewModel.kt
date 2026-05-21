package com.kalos.app.feature.workout.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    val logs: StateFlow<List<WorkoutLog>> = workoutRepository.getLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
