package com.kalos.app.core.ui.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kalos.app.core.ui.theme.ColorOverTarget

/**
 * Calorie-density bucket, a volume-eating aid: for the same calories, lower density means
 * more food volume (more filling). Computed from kcal per 100 g — no new data needed.
 */
enum class FoodDensityLevel(val label: String) {
    LIGHT("Léger"),
    MODERATE("Modéré"),
    DENSE("Dense"),
}

fun foodDensityLevel(kcalPer100g: Float): FoodDensityLevel = when {
    kcalPer100g < 150f -> FoodDensityLevel.LIGHT
    kcalPer100g <= 350f -> FoodDensityLevel.MODERATE
    else -> FoodDensityLevel.DENSE
}

/**
 * Signal colours — never red (that stays reserved for destructive/error, and a red food would
 * read as "bad" rather than "dense"). Green = volume-friendly, amber = dense, neutral in between.
 */
@Composable
fun FoodDensityLevel.color(): Color = when (this) {
    FoodDensityLevel.LIGHT -> MaterialTheme.colorScheme.primary
    FoodDensityLevel.MODERATE -> MaterialTheme.colorScheme.onSurfaceVariant
    FoodDensityLevel.DENSE -> ColorOverTarget
}
