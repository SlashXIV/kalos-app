package com.kalos.app.feature.nutrition.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.database.dao.DailySummaryRow
import com.kalos.app.core.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HistoryUiState(
    val summaries: List<DailySummaryRow> = emptyList(),
    val canLoadMore: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionHistoryViewModel @Inject constructor(
    private val mealRepository: MealRepository,
) : ViewModel() {

    private val _daysWindow = MutableStateFlow(INITIAL_DAYS)
    private val _earliestDate = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { _earliestDate.value = mealRepository.getEarliestMealDate() }
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        _daysWindow.flatMapLatest { days ->
            mealRepository.getDailySummaries(
                startDate = LocalDate.now().minusDays((days - 1).toLong()).toString(),
                endDate = LocalDate.now().toString(),
            )
        },
        _daysWindow,
        _earliestDate,
    ) { rows, days, earliest ->
        val windowStart = LocalDate.now().minusDays((days - 1).toLong())
        val canLoadMore = earliest != null && LocalDate.parse(earliest).isBefore(windowStart)
        HistoryUiState(
            summaries = rows.sortedByDescending { it.date },
            canLoadMore = canLoadMore,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun loadMore() {
        _daysWindow.update { it + STEP_DAYS }
    }

    private companion object {
        const val INITIAL_DAYS = 30
        const val STEP_DAYS = 30
    }
}
