package com.kalos.app.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.domain.repository.MealRepository
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.repository.WaterRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val nutritionDates: Set<String> = emptySet(),
    val workoutDates: Set<String> = emptySet(),
    val plannedWorkoutDates: Set<String> = emptySet(),
    val activeProgram: TrainingProgram? = null,
)

data class DayDetail(
    val date: String,
    val totalKcal: Float = 0f,
    val totalProtein: Float = 0f,
    val waterMl: Int = 0,
    val workoutLogs: List<WorkoutLog> = emptyList(),
)

data class CalendarInsightsState(
    val avgKcal7d: Float = 0f,
    val kcalGoal: Float = 0f,
    val workoutsPerWeek: List<Int> = emptyList(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val workoutRepository: WorkoutRepository,
    private val programRepository: ProgramRepository,
    private val waterRepository: WaterRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow<String?>(null)

    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    val uiState: StateFlow<CalendarUiState> = combine(
        _month,
        mealRepository.getLoggedDates(),
        workoutRepository.getTrainedDates(),
        programRepository.getActive(),
    ) { month, mealDates, workoutDates, activeProgram ->
        CalendarUiState(
            month = month,
            nutritionDates = mealDates.toSet(),
            workoutDates = workoutDates.toSet(),
            plannedWorkoutDates = plannedDatesForMonth(activeProgram, month),
            activeProgram = activeProgram,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    val dayDetail: StateFlow<DayDetail?> = _selectedDate.flatMapLatest { date ->
        if (date == null) return@flatMapLatest flowOf(null)
        combine(
            mealRepository.getDailySummaries(date, date),
            workoutRepository.getLogsForDate(date),
            waterRepository.observeWaterForDate(date),
        ) { summaries, logs, waterMl ->
            val s = summaries.firstOrNull()
            DayDetail(
                date = date,
                totalKcal = s?.totalKcal ?: 0f,
                totalProtein = s?.totalProtein ?: 0f,
                waterMl = waterMl,
                workoutLogs = logs,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val today = LocalDate.now().toString()
    private val sevenDaysAgo = LocalDate.now().minusDays(6).toString()

    val insightsState: StateFlow<CalendarInsightsState> = combine(
        mealRepository.getDailySummaries(sevenDaysAgo, today),
        userRepository.observeGoal(),
        workoutRepository.getTrainedDates(),
    ) { summaries, goal, trainedDates ->
        val daysWithData = summaries.count { (it.totalKcal ?: 0f) > 0f }
        val avgKcal = if (daysWithData == 0) 0f
                      else summaries.sumOf { (it.totalKcal ?: 0f).toDouble() }.toFloat() / daysWithData
        val kcalGoal = goal?.kcal?.toFloat() ?: 0f
        val now = LocalDate.now()
        val workoutsPerWeek = (3 downTo 0).map { weekOffset ->
            val weekEnd = now.minusDays((weekOffset * 7).toLong())
            val weekStart = weekEnd.minusDays(6)
            trainedDates.count { dateStr ->
                val d = LocalDate.parse(dateStr)
                !d.isBefore(weekStart) && !d.isAfter(weekEnd)
            }
        }
        CalendarInsightsState(
            avgKcal7d = avgKcal,
            kcalGoal = kcalGoal,
            workoutsPerWeek = workoutsPerWeek,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarInsightsState())

    fun previousMonth() = _month.update { it.minusMonths(1) }
    fun nextMonth() = _month.update { it.plusMonths(1) }
    fun today() { _month.value = YearMonth.now() }

    fun selectDate(date: String) {
        _selectedDate.value = if (_selectedDate.value == date) null else date
    }

    private fun plannedDatesForMonth(program: TrainingProgram?, month: YearMonth): Set<String> {
        if (program == null || program.workouts.isEmpty()) return emptySet()
        val scheduledDays = program.workouts.map { it.dayOfWeek }.toSet()
        return (1..month.lengthOfMonth())
            .asSequence()
            .map { month.atDay(it) }
            .filter { it.dayOfWeek.value in scheduledDays }
            .map { it.toString() }
            .toSet()
    }
}
