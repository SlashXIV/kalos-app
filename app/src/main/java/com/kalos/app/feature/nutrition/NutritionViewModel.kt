package com.kalos.app.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.repository.MealRepository
import com.kalos.app.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class NutritionUiState(
    val date: String = LocalDate.now().toString(),
    val meals: List<MealEntry> = emptyList(),
    val goal: NutritionGoal = NutritionGoal(),
    val totalKcal: Float = 0f,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val isLoading: Boolean = true,
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now().toString())
    val date: StateFlow<String> = _date

    val uiState: StateFlow<NutritionUiState> = combine(
        _date.flatMapLatest { mealRepository.getMealsForDate(it) },
        userRepository.observeGoal(),
        _date,
    ) { meals, goal, currentDate ->
        val safeGoal = goal ?: NutritionGoal()
        NutritionUiState(
            date = currentDate,
            meals = meals,
            goal = safeGoal,
            totalKcal = meals.sumOf { it.totalKcal.toDouble() }.toFloat(),
            totalProtein = meals.sumOf { it.totalProtein.toDouble() }.toFloat(),
            totalCarbs = meals.sumOf { it.totalCarbs.toDouble() }.toFloat(),
            totalFat = meals.sumOf { it.totalFat.toDouble() }.toFloat(),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NutritionUiState(),
    )

    fun goToPreviousDay() { _date.update { LocalDate.parse(it).minusDays(1).toString() } }
    fun goToNextDay() { _date.update { LocalDate.parse(it).plusDays(1).toString() } }
    fun goToToday() { _date.value = LocalDate.now().toString() }
}
