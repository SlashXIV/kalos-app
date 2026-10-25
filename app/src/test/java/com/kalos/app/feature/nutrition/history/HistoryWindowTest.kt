package com.kalos.app.feature.nutrition.history

import com.kalos.app.feature.nutrition.history.NutritionHistoryViewModel.Companion.initialWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HistoryWindowTest {
    private val today = LocalDate.of(2026, 10, 25)

    @Test
    fun recentMealsKeepTheDefaultWindow() {
        assertEquals(30, initialWindow(today, today))
        assertEquals(30, initialWindow(today.minusDays(29), today))
    }

    @Test
    fun afterABreakTheWindowReachesTheLastMeal() {
        // Dernier repas le 21 août : 65 jours d'écart, la fenêtre couvre les 30 jours qui finissent ce jour-là.
        val days = initialWindow(LocalDate.of(2026, 8, 21), today)
        assertEquals(95, days)
        assertEquals(LocalDate.of(2026, 7, 23), today.minusDays((days - 1).toLong()))
    }
}
