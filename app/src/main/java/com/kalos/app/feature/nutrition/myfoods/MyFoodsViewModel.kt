package com.kalos.app.feature.nutrition.myfoods

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyFoodsUiState(
    val foods: List<Food> = emptyList(),
    val query: String = "",
)

@HiltViewModel
class MyFoodsViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _allFoods = foodRepository.getCustomFoods()

    val state: StateFlow<MyFoodsUiState> = combine(_allFoods, _query) { foods, query ->
        val filtered = if (query.isBlank()) foods
        else foods.filter { it.name.contains(query, ignoreCase = true) || it.brand.contains(query, ignoreCase = true) }
        MyFoodsUiState(foods = filtered, query = query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyFoodsUiState())

    fun onQueryChange(v: String) { _query.value = v }

    fun delete(food: Food) {
        viewModelScope.launch { foodRepository.archiveOrDelete(food.id) }
    }
}
