package com.kalos.app.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.repository.MealRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val nutritionDates: Set<String> = emptySet(),
    val workoutDates: Set<String> = emptySet(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        _month,
        mealRepository.getLoggedDates(),
        workoutRepository.getTrainedDates(),
    ) { month, mealDates, workoutDates ->
        CalendarUiState(
            month = month,
            nutritionDates = mealDates.toSet(),
            workoutDates = workoutDates.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun previousMonth() = _month.update { it.minusMonths(1) }
    fun nextMonth() = _month.update { it.plusMonths(1) }
    fun today() { _month.value = YearMonth.now() }
}
