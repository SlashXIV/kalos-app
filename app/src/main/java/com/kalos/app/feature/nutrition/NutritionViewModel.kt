package com.kalos.app.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.data.DietaryPreferencesStore
import com.kalos.app.core.domain.model.*
import com.kalos.app.core.domain.model.DietaryFilter
import com.kalos.app.core.domain.repository.FoodRepository
import com.kalos.app.core.domain.repository.MealRepository
import com.kalos.app.core.domain.repository.UserRepository
import com.kalos.app.core.domain.usecase.FoodSuggestion
import com.kalos.app.core.domain.usecase.SuggestFoodsUseCase
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
    val suggestions: List<FoodSuggestion> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val userRepository: UserRepository,
    private val foodRepository: FoodRepository,
    private val suggestFoods: SuggestFoodsUseCase,
    private val dietaryPrefsStore: DietaryPreferencesStore,
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now().toString())
    val date: StateFlow<String> = _date

    val uiState: StateFlow<NutritionUiState> = combine(
        _date.flatMapLatest { mealRepository.getMealsForDate(it) },
        userRepository.observeGoal(),
        _date,
        foodRepository.getAll(),
        dietaryPrefsStore.filtersFlow,
    ) { meals: List<MealEntry>, goal: NutritionGoal?, currentDate: String, allFoods: List<Food>, filters: Set<DietaryFilter> ->
        val safeGoal = goal ?: NutritionGoal()
        val totalKcal = meals.sumOf { it.totalKcal.toDouble() }.toFloat()
        val totalProtein = meals.sumOf { it.totalProtein.toDouble() }.toFloat()
        val totalCarbs = meals.sumOf { it.totalCarbs.toDouble() }.toFloat()
        val totalFat = meals.sumOf { it.totalFat.toDouble() }.toFloat()
        val isToday = currentDate == LocalDate.now().toString()
        val suggestions = if (isToday && totalKcal > 0f) {
            suggestFoods(allFoods, safeGoal, totalKcal, totalProtein, totalCarbs, totalFat, filters)
        } else emptyList()
        NutritionUiState(
            date = currentDate,
            meals = meals,
            goal = safeGoal,
            totalKcal = totalKcal,
            totalProtein = totalProtein,
            totalCarbs = totalCarbs,
            totalFat = totalFat,
            suggestions = suggestions,
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

    fun deleteItem(itemId: Long) {
        viewModelScope.launch { mealRepository.removeItem(itemId) }
    }

    fun deleteItems(itemIds: List<Long>) {
        viewModelScope.launch { itemIds.forEach { mealRepository.removeItem(it) } }
    }
}
