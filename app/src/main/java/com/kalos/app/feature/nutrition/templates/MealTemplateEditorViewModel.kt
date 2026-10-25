package com.kalos.app.feature.nutrition.templates

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.repository.FoodRepository
import com.kalos.app.core.domain.repository.MealTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorItem(val food: Food, val amountG: Float) {
    val kcal: Float get() = food.kcalForAmount(amountG)
}

data class MealTemplateEditorUiState(
    val id: Long = -1L,
    val name: String = "",
    val items: List<EditorItem> = emptyList(),
    val isLoading: Boolean = true,
    val saved: Boolean = false,
) {
    val isNew: Boolean get() = id <= 0L
    val canSave: Boolean get() = name.isNotBlank() && items.isNotEmpty()
    val totalKcal: Float get() = items.sumOf { it.kcal.toDouble() }.toFloat()
}

@HiltViewModel
class MealTemplateEditorViewModel @Inject constructor(
    private val repository: MealTemplateRepository,
    private val foodRepository: FoodRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialId: Long = savedStateHandle["templateId"] ?: -1L

    private val _state = MutableStateFlow(MealTemplateEditorUiState(id = initialId))
    val state: StateFlow<MealTemplateEditorUiState> = _state.asStateFlow()

    init {
        if (initialId > 0L) {
            viewModelScope.launch {
                val template = repository.getTemplate(initialId)
                _state.update { s ->
                    if (template == null) s.copy(isLoading = false)
                    else s.copy(
                        name = template.name,
                        items = template.items.map { EditorItem(it.food, it.amountG) },
                        isLoading = false,
                    )
                }
            }
        } else {
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value) }
    }

    fun onAmountChange(index: Int, grams: Float) {
        _state.update { s ->
            s.copy(items = s.items.mapIndexed { i, item -> if (i == index) item.copy(amountG = grams) else item })
        }
    }

    fun removeItem(index: Int) {
        _state.update { s -> s.copy(items = s.items.filterIndexed { i, _ -> i != index }) }
    }

    fun addFood(foodId: Long) {
        viewModelScope.launch {
            val food = foodRepository.getById(foodId) ?: return@launch
            _state.update { s ->
                // Already present → leave it (user adjusts grams inline) rather than duplicate.
                if (s.items.any { it.food.id == foodId }) s
                else {
                    val defaultG = food.defaultServingG.takeIf { it > 0f } ?: 100f
                    s.copy(items = s.items + EditorItem(food, defaultG))
                }
            }
        }
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        val idArg = if (s.id > 0L) s.id else 0L
        viewModelScope.launch {
            val newId = repository.saveTemplate(idArg, s.name.trim(), s.items.map { it.food.id to it.amountG })
            _state.update { it.copy(id = newId, saved = true) }
        }
    }
}
