package com.kalos.app.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.repository.MealRepository
import com.kalos.app.core.domain.repository.MealTemplateRepository
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val isToday: Boolean = true,
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2000,
) {
    val waterProgress: Float get() = if (waterGoalMl > 0) (waterMl.toFloat() / waterGoalMl).coerceIn(0f, 1f) else 0f
    val isWaterGoalReached: Boolean get() = waterMl >= waterGoalMl
    val waterDisplayTotal: String get() = if (waterMl >= 1000) "${"%.1f".format(waterMl / 1000f)} L" else "$waterMl ml"
    val waterDisplayGoal: String get() = if (waterGoalMl >= 1000) "${"%.1f".format(waterGoalMl / 1000f)} L" else "$waterGoalMl ml"
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val userRepository: UserRepository,
    private val waterRepository: WaterRepository,
    private val mealTemplateRepository: MealTemplateRepository,
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now().toString())
    val date: StateFlow<String> = _date

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
        viewModelScope.launch { itemIds.forEach { mealRepository.removeItem(it) } }
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
        if (goalMl < 100) return
        waterRepository.setGoalMl(goalMl)
        _waterGoalMl.value = goalMl
    }
}
