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
    val editingFoodId: Long = 0,
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
    val containsPork: Boolean = false,
    val containsAlcohol: Boolean = false,
    val isVegetarian: Boolean = false,
    val isVegan: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val deletedSuccessfully: Boolean = false,
    val duplicateFood: Food? = null,
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
                        editingFoodId = food.id,
                        name = food.name, brand = food.brand, category = food.category,
                        kcal = food.kcalPer100g.toString(), protein = food.proteinPer100g.toString(),
                        carbs = food.carbsPer100g.toString(), fat = food.fatPer100g.toString(),
                        fiber = food.fiberPer100g.toString(), serving = food.defaultServingG.toString(),
                        unit = food.servingUnit,
                        containsPork = "pork" in food.tags,
                        containsAlcohol = "alcohol" in food.tags,
                        isVegetarian = "vegetarian" in food.tags,
                        isVegan = "vegan" in food.tags,
                        isEditing = true,
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
    fun onContainsPorkChange(v: Boolean) = _state.update { it.copy(containsPork = v) }
    fun onContainsAlcoholChange(v: Boolean) = _state.update { it.copy(containsAlcohol = v) }
    fun onIsVegetarianChange(v: Boolean) = _state.update {
        it.copy(isVegetarian = v, isVegan = if (!v) false else it.isVegan)
    }
    fun onIsVeganChange(v: Boolean) = _state.update {
        it.copy(isVegan = v, isVegetarian = if (v) true else it.isVegetarian)
    }

    val isValid: Boolean get() = with(_state.value) {
        name.isNotBlank() && kcal.parseFloat() != null &&
                protein.parseFloat() != null && carbs.parseFloat() != null &&
                fat.parseFloat() != null
    }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            // Duplicate check on create only
            if (!s.isEditing) {
                val duplicate = foodRepository.findDuplicate(s.name.trim())
                if (duplicate != null) {
                    _state.update { it.copy(isSaving = false, duplicateFood = duplicate) }
                    return@launch
                }
            }
            doSave()
        }
    }

    fun onConfirmSaveAnyway() {
        _state.update { it.copy(duplicateFood = null) }
        viewModelScope.launch { doSave() }
    }

    fun onDismissDuplicate() = _state.update { it.copy(duplicateFood = null, isSaving = false) }

    fun delete() {
        val foodId = _state.value.editingFoodId
        if (foodId <= 0) return
        viewModelScope.launch {
            foodRepository.archiveOrDelete(foodId)
            _state.update { it.copy(deletedSuccessfully = true) }
        }
    }

    private suspend fun doSave() {
        val s = _state.value
        val tags = buildList {
            if (s.containsPork) add("pork")
            if (s.containsAlcohol) add("alcohol")
            if (s.isVegetarian) add("vegetarian")
            if (s.isVegan) add("vegan")
        }
        foodRepository.save(
            Food(
                id = s.editingFoodId,
                name = s.name.trim(), brand = s.brand.trim(), category = s.category,
                kcalPer100g = s.kcal.parseFloat() ?: 0f,
                proteinPer100g = s.protein.parseFloat() ?: 0f,
                carbsPer100g = s.carbs.parseFloat() ?: 0f,
                fatPer100g = s.fat.parseFloat() ?: 0f,
                fiberPer100g = s.fiber.parseFloat() ?: 0f,
                defaultServingG = s.serving.parseFloat() ?: 100f,
                servingUnit = s.unit,
                isCustom = true,
                tags = tags,
            )
        )
        _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
    }
}

// Accepts both "7.5" and "7,5" (French decimal separator)
private fun String.parseFloat(): Float? = replace(',', '.').toFloatOrNull()
