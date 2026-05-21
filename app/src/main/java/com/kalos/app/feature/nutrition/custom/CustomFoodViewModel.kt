package com.kalos.app.feature.nutrition.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomFoodState(
    val name: String = "",
    val brand: String = "",
    val category: String = "Divers",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val fiber: String = "0",
    val serving: String = "100",
    val unit: String = "g",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class CustomFoodViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomFoodState())
    val state: StateFlow<CustomFoodState> = _state

    fun loadFood(foodId: Long) {
        if (foodId <= 0) return
        viewModelScope.launch {
            foodRepository.getById(foodId)?.let { food ->
                _state.update {
                    it.copy(
                        name = food.name, brand = food.brand, category = food.category,
                        kcal = food.kcalPer100g.toString(), protein = food.proteinPer100g.toString(),
                        carbs = food.carbsPer100g.toString(), fat = food.fatPer100g.toString(),
                        fiber = food.fiberPer100g.toString(), serving = food.defaultServingG.toString(),
                        unit = food.servingUnit, isEditing = true,
                    )
                }
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onBrandChange(v: String) = _state.update { it.copy(brand = v) }
    fun onKcalChange(v: String) = _state.update { it.copy(kcal = v) }
    fun onProteinChange(v: String) = _state.update { it.copy(protein = v) }
    fun onCarbsChange(v: String) = _state.update { it.copy(carbs = v) }
    fun onFatChange(v: String) = _state.update { it.copy(fat = v) }
    fun onFiberChange(v: String) = _state.update { it.copy(fiber = v) }
    fun onServingChange(v: String) = _state.update { it.copy(serving = v) }

    val isValid: Boolean get() = with(_state.value) {
        name.isNotBlank() && kcal.toFloatOrNull() != null &&
                protein.toFloatOrNull() != null && carbs.toFloatOrNull() != null &&
                fat.toFloatOrNull() != null
    }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            foodRepository.save(
                Food(
                    name = s.name.trim(), brand = s.brand.trim(), category = s.category,
                    kcalPer100g = s.kcal.toFloatOrNull() ?: 0f,
                    proteinPer100g = s.protein.toFloatOrNull() ?: 0f,
                    carbsPer100g = s.carbs.toFloatOrNull() ?: 0f,
                    fatPer100g = s.fat.toFloatOrNull() ?: 0f,
                    fiberPer100g = s.fiber.toFloatOrNull() ?: 0f,
                    defaultServingG = s.serving.toFloatOrNull() ?: 100f,
                    servingUnit = s.unit,
                    isCustom = true,
                )
            )
            _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}
