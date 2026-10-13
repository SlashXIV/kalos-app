package com.kalos.app.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Couleurs propres à une palette : l'accent et son conteneur. Tout le reste est commun. */
internal data class Accents(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
)

/**
 * Neutres communs à toutes les palettes : ce sont eux qui garantissent la lisibilité
 * (encre ≈ 16:1 sur le fond). Une palette ne fait que colorer.
 * Les cartes sont plus claires (clair) ou plus élevées (sombre) que le fond.
 */
internal fun lightScheme(a: Accents): ColorScheme = lightColorScheme(
    primary = a.primary, onPrimary = a.onPrimary,
    primaryContainer = a.primaryContainer, onPrimaryContainer = a.onPrimaryContainer,
    secondary = a.primary, onSecondary = a.onPrimary,
    secondaryContainer = a.primaryContainer, onSecondaryContainer = a.onPrimaryContainer,
    tertiary = a.primary, onTertiary = a.onPrimary,
    tertiaryContainer = a.primaryContainer, onTertiaryContainer = a.onPrimaryContainer,
    background = Color(0xFFF4F5F3), onBackground = Color(0xFF14171A),
    surface = Color(0xFFF4F5F3), onSurface = Color(0xFF14171A),
    surfaceVariant = Color(0xFFECEEEB), onSurfaceVariant = Color(0xFF545A60),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFE4E6E3), surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFECEEEB),
    inverseSurface = Color(0xFF2A2E32), inverseOnSurface = Color(0xFFF1F2EF), inversePrimary = a.primaryContainer,
    outline = Color(0xFF767C76), outlineVariant = Color(0xFFC9CDC7),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    scrim = Color.Black,
)

internal fun darkScheme(a: Accents): ColorScheme = darkColorScheme(
    primary = a.primary, onPrimary = a.onPrimary,
    primaryContainer = a.primaryContainer, onPrimaryContainer = a.onPrimaryContainer,
    secondary = a.primary, onSecondary = a.onPrimary,
    secondaryContainer = a.primaryContainer, onSecondaryContainer = a.onPrimaryContainer,
    tertiary = a.primary, onTertiary = a.onPrimary,
    tertiaryContainer = a.primaryContainer, onTertiaryContainer = a.onPrimaryContainer,
    background = Color(0xFF0F1113), onBackground = Color(0xFFECEEEA),
    surface = Color(0xFF0F1113), onSurface = Color(0xFFECEEEA),
    surfaceVariant = Color(0xFF1F2327), onSurfaceVariant = Color(0xFFA2A8AD),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFF0F1113), surfaceBright = Color(0xFF2A2E33),
    surfaceContainerLowest = Color(0xFF0B0D0E), surfaceContainerLow = Color(0xFF171A1D),
    surfaceContainer = Color(0xFF171A1D), surfaceContainerHigh = Color(0xFF1D2024),
    surfaceContainerHighest = Color(0xFF23272B),
    inverseSurface = Color(0xFFECEEEA), inverseOnSurface = Color(0xFF1A1D20), inversePrimary = a.primaryContainer,
    outline = Color(0xFF737A80), outlineVariant = Color(0xFF383E44),
    error = Color(0xFFFF8A80), onError = Color(0xFF3B0907),
    errorContainer = Color(0xFF5C1611), onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black,
)
