package com.kalos.app.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.repository.MealRepository
import com.kalos.app.core.domain.repository.MealTemplateRepository
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.ui.util.todayFlow
import com.kalos.app.core.data.MomentsStore

data class NutritionUiState(
    val date: String = LocalDate.now().toString(),
    val meals: List<MealEntry> = emptyList(),
    val goal: NutritionGoal = NutritionGoal(),
    val totalKcal: Float = 0f,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val isLoading: Boolean = true,
    val isToday: Boolean = true,
    val waterMl: Int = 0,
    val waterGoalMl: Int = NutritionGoal.DEFAULT_WATER_ML,
) {
    val waterProgress: Float get() = if (waterGoalMl > 0) (waterMl.toFloat() / waterGoalMl).coerceIn(0f, 1f) else 0f
    val isWaterGoalReached: Boolean get() = waterMl >= waterGoalMl
    val waterDisplayTotal: String get() = if (waterMl >= 1000) "${"%.1f".format(waterMl / 1000f)} L" else "$waterMl ml"
    val waterDisplayGoal: String get() = if (waterGoalMl >= 1000) "${"%.1f".format(waterGoalMl / 1000f)} L" else "$waterGoalMl ml"
}

data class TodayWorkout(val template: WorkoutTemplate, val done: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val userRepository: UserRepository,
    private val waterRepository: WaterRepository,
    private val mealTemplateRepository: MealTemplateRepository,
    programRepository: ProgramRepository,
    workoutRepository: WorkoutRepository,
    private val momentsStore: MomentsStore,
) : ViewModel() {

    /** Vrai une seule fois par jour : le moment « objectif atteint » se joue alors. */
    fun claimGoalMoment(date: String): Boolean = momentsStore.claimGoalMoment(date)

    /** Séance du programme actif prévue aujourd'hui (même règle que le calendrier), et si elle est faite. */
    val todayWorkout: StateFlow<TodayWorkout?> = combine(
        programRepository.getActive(),
        todayFlow().flatMapLatest { day -> workoutRepository.getLogsForDate(day.toString()).map { day to it } },
    ) { program, (day, logs) ->
        program?.workoutOn(day.dayOfWeek.value)?.template
            ?.let { TodayWorkout(it, done = logs.isNotEmpty()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _date = MutableStateFlow(LocalDate.now().toString())
    val date: StateFlow<String> = _date

    init {
        // Viewing today when the day changes (midnight, or back after a night): follow it.
        viewModelScope.launch {
            var previous = LocalDate.now()
            todayFlow().collect { today ->
                if (_date.value == previous.toString()) _date.value = today.toString()
                previous = today
            }
        }
    }

    val mealTemplates: StateFlow<List<MealTemplate>> = mealTemplateRepository.getTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _waterGoalMl = MutableStateFlow(waterRepository.getGoalMl())

    private val baseState: Flow<NutritionUiState> = combine(
        _date.flatMapLatest { mealRepository.getMealsForDate(it) },
        userRepository.observeGoal(),
        _date,
    ) { meals: List<MealEntry>, goal: NutritionGoal?, currentDate: String ->
        val safeGoal = goal ?: NutritionGoal()
        val totalKcal = meals.sumOf { it.totalKcal.toDouble() }.toFloat()
        val totalProtein = meals.sumOf { it.totalProtein.toDouble() }.toFloat()
        val totalCarbs = meals.sumOf { it.totalCarbs.toDouble() }.toFloat()
        val totalFat = meals.sumOf { it.totalFat.toDouble() }.toFloat()
        val isToday = currentDate == LocalDate.now().toString()
        NutritionUiState(
            date = currentDate,
            meals = meals,
            goal = safeGoal,
            totalKcal = totalKcal,
            totalProtein = totalProtein,
            totalCarbs = totalCarbs,
            totalFat = totalFat,
            isLoading = false,
            isToday = isToday,
        )
    }

    val uiState: StateFlow<NutritionUiState> = combine(
        baseState,
        _date.flatMapLatest { waterRepository.observeWaterForDate(it) },
        _waterGoalMl,
    ) { state, waterMl, waterGoalMl ->
        state.copy(waterMl = waterMl, waterGoalMl = waterGoalMl)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NutritionUiState(),
    )

    fun goToPreviousDay() { _date.update { LocalDate.parse(it).minusDays(1).toString() } }
    fun goToNextDay() { _date.update { LocalDate.parse(it).plusDays(1).toString() } }
    fun goToToday() { _date.value = LocalDate.now().toString() }
    fun setDate(date: String) { _date.value = date }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch { mealRepository.removeItem(itemId) }
    }

    fun deleteItems(itemIds: List<Long>) {
        viewModelScope.launch { mealRepository.removeItems(itemIds) }
    }

    /** Applies a favourite meal to the given meal type on the current date (appends items). */
    fun applyTemplate(mealType: MealType, templateId: Long) {
        viewModelScope.launch { mealTemplateRepository.applyToMeal(templateId, _date.value, mealType) }
    }

    /** Saves the current content of a meal section as a reusable favourite. */
    fun saveMealAsFavorite(mealType: MealType, name: String) {
        if (name.isBlank()) return
        val meal = uiState.value.meals.firstOrNull { it.mealType == mealType } ?: return
        // Merge duplicate foods so the favourite stays clean.
        val items = meal.items
            .groupBy { it.food.id }
            .map { (foodId, group) -> foodId to group.sumOf { it.amountG.toDouble() }.toFloat() }
        if (items.isEmpty()) return
        viewModelScope.launch { mealTemplateRepository.saveTemplate(0, name.trim(), items) }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { mealTemplateRepository.deleteTemplate(id) }
    }

    fun addWater(amountMl: Int) {
        val date = _date.value
        viewModelScope.launch { waterRepository.addWater(amountMl, date) }
    }

    fun setWaterGoal(goalMl: Int) {
        if (goalMl < NutritionGoal.MIN_WATER_GOAL_ML) return
        waterRepository.setGoalMl(goalMl)
        _waterGoalMl.value = goalMl
    }
}
