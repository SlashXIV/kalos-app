package com.kalos.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.DailyNutritionSummary
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.model.ProgramWorkout
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.domain.repository.MealRepository
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val today: String = LocalDate.now().toString(),
    val summary: DailyNutritionSummary = DailyNutritionSummary(LocalDate.now().toString()),
    val todayWorkouts: List<WorkoutLog> = emptyList(),
    val recentWorkouts: List<WorkoutLog> = emptyList(),
    val todayProgramWorkout: ProgramWorkout? = null,
    val activeProgramName: String? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val programRepository: ProgramRepository,
) : ViewModel() {

    private val today = LocalDate.now().toString()

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.observeProfile(),
        userRepository.observeGoal(),
        mealRepository.getMealsForDate(today),
        workoutRepository.getLogsForDate(today),
        workoutRepository.getLogs(),
    ) { profile, goal, meals, todayLogs, allLogs ->
        val safeGoal = goal ?: NutritionGoal()
        val totalKcal = meals.sumOf { it.totalKcal.toDouble() }.toFloat()
        val totalProtein = meals.sumOf { it.totalProtein.toDouble() }.toFloat()
        val totalCarbs = meals.sumOf { it.totalCarbs.toDouble() }.toFloat()
        val totalFat = meals.sumOf { it.totalFat.toDouble() }.toFloat()
        HomeUiState(
            userName = profile?.name ?: "",
            today = today,
            summary = DailyNutritionSummary(
                date = today,
                totalKcal = totalKcal, totalProtein = totalProtein,
                totalCarbs = totalCarbs, totalFat = totalFat,
                goalKcal = safeGoal.kcal, goalProtein = safeGoal.proteinG,
                goalCarbs = safeGoal.carbsG, goalFat = safeGoal.fatG,
                meals = meals,
            ),
            todayWorkouts = todayLogs,
            recentWorkouts = allLogs.take(3),
            isLoading = false,
        )
    }.combine(programRepository.getActive()) { base, activeProgram ->
        val todayDow = LocalDate.now().dayOfWeek.value // 1=Lundi .. 7=Dimanche
        base.copy(
            todayProgramWorkout = activeProgram?.workouts?.firstOrNull { it.dayOfWeek == todayDow },
            activeProgramName = activeProgram?.name,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}
