package com.kalos.app.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    surfaceTint = md_theme_dark_surfaceTint,
    surfaceDim = md_theme_dark_surfaceDim,
    surfaceBright = md_theme_dark_surfaceBright,
    surfaceContainerLowest = md_theme_dark_surfaceContainerLowest,
    surfaceContainerLow = md_theme_dark_surfaceContainerLow,
    surfaceContainer = md_theme_dark_surfaceContainer,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_dark_surfaceContainerHighest,
    outline = md_theme_dark_outline,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
)

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    surfaceTint = md_theme_light_surfaceTint,
    surfaceDim = md_theme_light_surfaceDim,
    surfaceBright = md_theme_light_surfaceBright,
    surfaceContainerLowest = md_theme_light_surfaceContainerLowest,
    surfaceContainerLow = md_theme_light_surfaceContainerLow,
    surfaceContainer = md_theme_light_surfaceContainer,
    surfaceContainerHigh = md_theme_light_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_light_surfaceContainerHighest,
    outline = md_theme_light_outline,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
)

/** Whether the resolved theme is dark (drives system-bar icon contrast & window background). */
fun ThemeMode.isDark(systemInDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemInDark
    ThemeMode.LIGHT, ThemeMode.PASTEL -> false
    ThemeMode.DARK, ThemeMode.BERRY, ThemeMode.AURORA, ThemeMode.MONOCHROME,
    ThemeMode.AQUA, ThemeMode.OCEAN, ThemeMode.SUNSET -> true
}

/** Resolves the Material 3 colour scheme for a theme (SYSTEM follows the device setting). */
fun colorSchemeFor(mode: ThemeMode, systemInDark: Boolean): ColorScheme = when (mode) {
    ThemeMode.SYSTEM -> if (systemInDark) DarkColorScheme else LightColorScheme
    ThemeMode.LIGHT -> LightColorScheme
    ThemeMode.DARK -> DarkColorScheme
    ThemeMode.PASTEL -> PastelScheme
    ThemeMode.BERRY -> BerryScheme
    ThemeMode.AURORA -> AuroraScheme
    ThemeMode.MONOCHROME -> MonochromeScheme
    ThemeMode.AQUA -> AquaScheme
    ThemeMode.OCEAN -> OceanScheme
    ThemeMode.SUNSET -> SunsetScheme
}

@Composable
fun KalosTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemInDark = isSystemInDarkTheme()
    val darkTheme = mode.isDark(systemInDark)
    val colorScheme = colorSchemeFor(mode, systemInDark)

    // Keep the status/navigation bar icon contrast aligned with the resolved theme,
    // not the device setting (edge-to-edge draws behind the bars).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KalosTypography,
        shapes = KalosShapes,
        content = content,
    )
}
