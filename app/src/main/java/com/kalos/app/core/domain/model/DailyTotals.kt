package com.kalos.app.core.domain.model

/** Logged totals for one day (0 when nothing was logged). */
data class DailyTotals(
    val date: String,
    val totalKcal: Float,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
)
