package com.kalos.app.core.ui.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.ui.theme.ColorOverTarget

/**
 * "Fullness per calorie" signal for volume eating: how filling a food is relative to its
 * calories. Combines energy density (dominant) with satiating macros (protein + fibre).
 *
 * Rule-based and transparent — not an opaque composite score:
 *  - LOW  (Peu rassasiant)  : high density (> 350 kcal/100 g) → calorie-dense dominates,
 *                             even if nutritious (nuts, cheese, oils).
 *  - HIGH (Rassasiant)      : low density (< 150) OR moderate density but rich in
 *                             protein + fibre (≥ 12 g/100 g) → chicken, lentils, vegetables.
 *  - MODERATE               : everything else.
 *
 * Honest limitation: a dense-but-nutritious food reads "Peu rassasiant" because it's low on
 * fullness *per calorie*. The detail sheet shows raw kcal/100 g so the label never misleads alone.
 */
enum class SatietyLevel(val label: String) {
    HIGH("Rassasiant"),
    MODERATE("Modéré"),
    LOW("Peu rassasiant"),
}

private const val DENSE_THRESHOLD = 350f
private const val LIGHT_THRESHOLD = 150f
private const val RICH_MACROS_THRESHOLD = 12f  // protein + fibre per 100 g

fun foodSatietyLevel(food: Food): SatietyLevel {
    val density = food.kcalPer100g
    val satiatingMacros = food.proteinPer100g + food.fiberPer100g
    return when {
        density > DENSE_THRESHOLD -> SatietyLevel.LOW
        density < LIGHT_THRESHOLD -> SatietyLevel.HIGH
        satiatingMacros >= RICH_MACROS_THRESHOLD -> SatietyLevel.HIGH
        else -> SatietyLevel.MODERATE
    }
}

/**
 * Signal colours — never red (reserved for destructive/error; a red food would read as "bad").
 * Green = filling per calorie, amber = calorie-dense, neutral in between.
 */
@Composable
fun SatietyLevel.color(): Color = when (this) {
    SatietyLevel.HIGH -> MaterialTheme.colorScheme.primary
    SatietyLevel.MODERATE -> MaterialTheme.colorScheme.onSurfaceVariant
    SatietyLevel.LOW -> ColorOverTarget
}
