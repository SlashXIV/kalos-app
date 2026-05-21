package com.kalos.app.feature.nutrition.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.database.dao.DailySummaryRow
import com.kalos.app.core.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class HistoryUiState(
    val summaries: List<DailySummaryRow> = emptyList(),
)

@HiltViewModel
class NutritionHistoryViewModel @Inject constructor(
    private val mealRepository: MealRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = mealRepository
        .getDailySummaries(
            startDate = LocalDate.now().minusDays(60).toString(),
            endDate = LocalDate.now().toString(),
        )
        .map { rows ->
            HistoryUiState(summaries = rows.sortedByDescending { it.date })
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )
}
