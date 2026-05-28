package com.kalos.app.feature.nutrition.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.model.MealItem
import com.kalos.app.core.domain.model.MealType
import com.kalos.app.core.domain.model.NutritionGoal
import com.kalos.app.core.domain.repository.FoodRepository
import com.kalos.app.core.domain.repository.MealRepository
import com.kalos.app.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class ServingMode { GRAMS, UNITS }

data class FoodSearchUiState(
    val query: String = "",
    val results: List<Food> = emptyList(),
    val recent: List<Food> = emptyList(),
    val favorites: List<Food> = emptyList(),
    val selectedFood: Food? = null,
    val amountG: String = "100",
    val servingMode: ServingMode = ServingMode.GRAMS,
    val servingCount: String = "1",
    val isLoading: Boolean = false,
    val addedSuccessfully: Boolean = false,
    val errorMessage: String? = null,
    val categoryFilter: String = "",
    val onlyCustom: Boolean = false,
    val categories: List<String> = emptyList(),
    // Daily context for nutritional preview
    val dailyKcal: Float = 0f,
    val dailyProtein: Float = 0f,
    val dailyCarbs: Float = 0f,
    val dailyFat: Float = 0f,
    val goalKcal: Float = 0f,
    val goalProtein: Float = 0f,
    val goalCarbs: Float = 0f,
    val goalFat: Float = 0f,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    private val mealRepository: MealRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val searchDate = savedStateHandle.get<String>("date") ?: LocalDate.now().toString()

    private val _state = MutableStateFlow(FoodSearchUiState())
    val state: StateFlow<FoodSearchUiState> = _state

    private val _query = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow("")
    private val _onlyCustom = MutableStateFlow(false)

    init {
        val startQuery = savedStateHandle.get<String>("query").orEmpty()
        if (startQuery.isNotEmpty()) {
            _state.update { it.copy(query = startQuery, isLoading = true) }
            _query.value = startQuery
        }

        viewModelScope.launch {
            combine(_query, _categoryFilter, _onlyCustom) { q, cat, custom -> Triple(q, cat, custom) }
                .debounce { (q, _, _) -> if (q.isEmpty()) 0L else 300L }
                .distinctUntilChanged()
                .flatMapLatest { (q, cat, custom) -> foodRepository.search(q, cat, custom) }
                .collect { results -> _state.update { it.copy(results = results, isLoading = false) } }
        }

        viewModelScope.launch {
            foodRepository.getRecent().collect { recent -> _state.update { it.copy(recent = recent) } }
        }

        viewModelScope.launch {
            foodRepository.getFavorites().collect { favs -> _state.update { it.copy(favorites = favs) } }
        }

        viewModelScope.launch {
            foodRepository.getDistinctCategories().collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }

        // Observe daily totals + goal for nutritional preview
        viewModelScope.launch {
            combine(
                mealRepository.getMealsForDate(searchDate),
                userRepository.observeGoal(),
            ) { meals, goal ->
                val safeGoal = goal ?: NutritionGoal()
                _state.update { s ->
                    s.copy(
                        dailyKcal = meals.sumOf { it.totalKcal.toDouble() }.toFloat(),
                        dailyProtein = meals.sumOf { it.totalProtein.toDouble() }.toFloat(),
                        dailyCarbs = meals.sumOf { it.totalCarbs.toDouble() }.toFloat(),
                        dailyFat = meals.sumOf { it.totalFat.toDouble() }.toFloat(),
                        goalKcal = safeGoal.kcal.toFloat(),
                        goalProtein = safeGoal.proteinG.toFloat(),
                        goalCarbs = safeGoal.carbsG.toFloat(),
                        goalFat = safeGoal.fatG.toFloat(),
                    )
                }
            }.collect()
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q, isLoading = q.isNotEmpty()) }
        _query.value = q
    }

    fun onCategorySelect(category: String) {
        val new = if (_categoryFilter.value == category) "" else category
        _categoryFilter.value = new
        _state.update { it.copy(categoryFilter = new) }
    }

    fun onCustomToggle() {
        val new = !_onlyCustom.value
        _onlyCustom.value = new
        _state.update { it.copy(onlyCustom = new) }
    }

    fun selectFood(food: Food) {
        val defaultMode = if (food.servingUnit != "g") ServingMode.UNITS else ServingMode.GRAMS
        _state.update {
            it.copy(
                selectedFood = food,
                amountG = food.defaultServingG.toInt().toString(),
                servingMode = defaultMode,
                servingCount = "1",
            )
        }
    }

    fun onAmountChange(v: String) { _state.update { it.copy(amountG = v) } }
    fun onServingCountChange(v: String) { _state.update { it.copy(servingCount = v) } }
    fun onServingModeChange(mode: ServingMode) { _state.update { it.copy(servingMode = mode) } }

    fun dismissSheet() { _state.update { it.copy(selectedFood = null, amountG = "100") } }

    fun addToMeal(mealType: String, date: String) {
        val food = _state.value.selectedFood ?: return
        val amount = when (_state.value.servingMode) {
            ServingMode.UNITS -> {
                val count = _state.value.servingCount.toFloatOrNull() ?: return
                count * food.defaultServingG
            }
            ServingMode.GRAMS -> _state.value.amountG.toFloatOrNull() ?: return
        }
        viewModelScope.launch {
            runCatching {
                val entryId = mealRepository.getOrCreateMealEntry(date, MealType.valueOf(mealType))
                mealRepository.addItemToMeal(
                    mealEntryId = entryId,
                    item = MealItem(
                        mealEntryId = entryId,
                        food = food,
                        amountG = amount,
                        kcal = food.kcalForAmount(amount),
                        proteinG = food.proteinForAmount(amount),
                        carbsG = food.carbsForAmount(amount),
                        fatG = food.fatForAmount(amount),
                    )
                )
            }
                .onSuccess {
                    _state.update { it.copy(selectedFood = null, amountG = "100", addedSuccessfully = true) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            errorMessage = e.message?.takeIf { msg -> msg.isNotBlank() }
                                ?: "Impossible d'ajouter l'aliment au repas",
                        )
                    }
                }
        }
    }

    /** Called by the screen after the success event was handled (e.g. popBackStack).
     *  Resets the flag so reusing the screen doesn't re-trigger the navigation. */
    fun onAddHandled() = _state.update { it.copy(addedSuccessfully = false) }

    fun onErrorShown() = _state.update { it.copy(errorMessage = null) }
}
