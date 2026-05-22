package com.kalos.app.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaterUiState(
    val todayMl: Int = 0,
    val goalMl: Int = 2000,
) {
    val progress: Float get() = if (goalMl > 0) (todayMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f
    val isGoalReached: Boolean get() = todayMl >= goalMl
    val displayTotal: String get() = if (todayMl >= 1000) "${"%.1f".format(todayMl / 1000f)} L" else "$todayMl ml"
    val displayGoal: String get() = if (goalMl >= 1000) "${"%.1f".format(goalMl / 1000f)} L" else "$goalMl ml"
}

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val waterRepository: WaterRepository,
) : ViewModel() {

    private val _goalMl = MutableStateFlow(waterRepository.getGoalMl())

    val uiState: StateFlow<WaterUiState> = combine(
        waterRepository.observeTodayIntake(),
        _goalMl,
    ) { todayMl, goalMl ->
        WaterUiState(todayMl = todayMl, goalMl = goalMl)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WaterUiState(goalMl = waterRepository.getGoalMl()),
    )

    fun addWater(amountMl: Int) {
        viewModelScope.launch { waterRepository.addWater(amountMl) }
    }

    fun setGoal(goalMl: Int) {
        if (goalMl < 100) return
        waterRepository.setGoalMl(goalMl)
        _goalMl.value = goalMl
    }
}
