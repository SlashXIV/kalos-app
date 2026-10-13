package com.kalos.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Couleurs porteuses de sens, hors du ColorScheme de Material : les protéines restent violettes
 * quelle que soit la palette. Choisies au validateur de palettes (distinctes en protanopie et
 * deutéranopie, ≥ 3:1 sur le fond) ; `PaletteContrastTest` empêche de régresser.
 * Elles accompagnent toujours un libellé : la couleur n'est jamais seule.
 */
@Immutable
data class NutrientColors(
    val protein: Color,
    val carbs: Color,
    val fat: Color,
    val water: Color,
    /** État « objectif atteint » : toujours accompagné d'un libellé. */
    val goalReached: Color,
    /** État « au-dessus de l'objectif » : toujours accompagné d'un libellé. */
    val overTarget: Color,
) {
    companion object {
        val Light = NutrientColors(
            protein = Color(0xFF5B47D1), carbs = Color(0xFFB8790F), fat = Color(0xFFC23A66),
            water = Color(0xFF0A7FA0), goalReached = Color(0xFF0B7A57), overTarget = Color(0xFFB3540E),
        )
        val Dark = NutrientColors(
            protein = Color(0xFF8A78EE), carbs = Color(0xFFBA841C), fat = Color(0xFFD45A80),
            water = Color(0xFF1F9AB6), goalReached = Color(0xFF3CCB98), overTarget = Color(0xFFF0954A),
        )
    }
}

internal val LocalNutrientColors = staticCompositionLocalOf { NutrientColors.Dark }
