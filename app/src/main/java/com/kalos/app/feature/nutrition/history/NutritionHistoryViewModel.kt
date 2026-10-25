package com.kalos.app.feature.nutrition.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalos.app.core.domain.model.DailyTotals
import com.kalos.app.core.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class HistoryUiState(
    val summaries: List<DailyTotals> = emptyList(),
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
        viewModelScope.launch {
            _earliestDate.value = mealRepository.getEarliestMealDate()
            // Après une pause ou un import, la fenêtre s'ouvre sur les derniers repas, jamais à vide.
            val latest = mealRepository.getLatestMealDate() ?: return@launch
            _daysWindow.update { maxOf(it, initialWindow(LocalDate.parse(latest), LocalDate.now())) }
        }
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

    internal companion object {
        /** Jours affichés au départ : 30, étendus pour inclure les 30 jours qui finissent au dernier repas. */
        fun initialWindow(latest: LocalDate, today: LocalDate): Int {
            val gap = ChronoUnit.DAYS.between(latest, today).toInt()
            return if (gap < INITIAL_DAYS) INITIAL_DAYS else gap + INITIAL_DAYS
        }

        const val INITIAL_DAYS = 30
        const val STEP_DAYS = 30
    }
}
