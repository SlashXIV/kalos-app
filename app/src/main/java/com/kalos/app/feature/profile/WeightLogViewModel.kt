package com.kalos.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class WeightLogUiState(
    val entries: List<Pair<String, Float>> = emptyList(),
    val isSaving: Boolean = false,
) {
    val hasTodayEntry: Boolean
        get() = entries.firstOrNull()?.first == LocalDate.now().toString()
}

@HiltViewModel
class WeightLogViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)

    val uiState: StateFlow<WeightLogUiState> = combine(
        workoutRepository.getBodyWeightHistory(),
        _isSaving,
    ) { entries, isSaving ->
        WeightLogUiState(entries = entries, isSaving = isSaving)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeightLogUiState(),
    )

    fun logWeight(weightKg: Float) {
        viewModelScope.launch {
            _isSaving.value = true
            workoutRepository.logBodyWeight(LocalDate.now().toString(), weightKg)
            _isSaving.value = false
        }
    }
}
