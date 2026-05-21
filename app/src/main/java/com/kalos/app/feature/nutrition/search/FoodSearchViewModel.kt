package com.kalos.app.feature.nutrition.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.model.MealItem
import com.kalos.app.core.domain.model.MealType
import com.kalos.app.core.domain.repository.FoodRepository
import com.kalos.app.core.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoodSearchUiState(
    val query: String = "",
    val results: List<Food> = emptyList(),
    val recent: List<Food> = emptyList(),
    val favorites: List<Food> = emptyList(),
    val selectedFood: Food? = null,
    val amountG: String = "100",
    val isLoading: Boolean = false,
    val addedSuccessfully: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val mealRepository: MealRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FoodSearchUiState())
    val state: StateFlow<FoodSearchUiState> = _state

    private val _query = MutableStateFlow("")

    init {
        // Debounced search
        viewModelScope.launch {
            _query
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { q -> foodRepository.search(q) }
                .collect { results -> _state.update { it.copy(results = results, isLoading = false) } }
        }
        // Load recent
        viewModelScope.launch {
            foodRepository.getRecent().collect { recent -> _state.update { it.copy(recent = recent) } }
        }
        // Load favorites
        viewModelScope.launch {
            foodRepository.getFavorites().collect { favs -> _state.update { it.copy(favorites = favs) } }
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q, isLoading = q.isNotEmpty()) }
        _query.value = q
    }

    fun selectFood(food: Food) {
        _state.update { it.copy(selectedFood = food, amountG = food.defaultServingG.toInt().toString()) }
    }

    fun onAmountChange(v: String) { _state.update { it.copy(amountG = v) } }

    fun dismissSheet() { _state.update { it.copy(selectedFood = null, amountG = "100") } }

    fun addToMeal(mealType: String, date: String) {
        val food = _state.value.selectedFood ?: return
        val amount = _state.value.amountG.toFloatOrNull() ?: return
        viewModelScope.launch {
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
            _state.update { it.copy(selectedFood = null, amountG = "100", addedSuccessfully = true) }
        }
    }
}
