package com.kalos.app.core.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.kalos.app.core.ui.motion.LocalMotionLevel
import com.kalos.app.core.ui.motion.LocalVibrationsEnabled
import com.kalos.app.core.ui.motion.Motion
import com.kalos.app.core.ui.motion.MotionLevel

/**
 * Thème de Kalos. Pas de couleurs dynamiques (Material You) : les contrastes sont choisis et
 * vérifiés par test, pas hérités du fond d'écran. Changer de palette ou de luminosité FOND les
 * couleurs quand le mouvement est permis.
 */
@Composable
fun KalosTheme(
    palette: KalosPalette = Palettes.default,
    darkTheme: Boolean,
    motion: MotionLevel = MotionLevel.FULL,
    vibrations: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = animateScheme(if (darkTheme) palette.dark else palette.light, animate = motion.animates)

    // Contraste des icônes des barres système aligné sur le thème résolu (bord à bord).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalNutrientColors provides if (darkTheme) NutrientColors.Dark else NutrientColors.Light,
        LocalKalosType provides DefaultKalosType,
        LocalMotionLevel provides motion,
        LocalVibrationsEnabled provides vibrations,
    ) {
        MaterialTheme(colorScheme = scheme, typography = KalosMaterialTypography, shapes = KalosShapes, content = content)
    }
}

object KalosTheme {
    val nutrients: NutrientColors
        @Composable @ReadOnlyComposable get() = LocalNutrientColors.current
    val type: KalosType
        @Composable @ReadOnlyComposable get() = LocalKalosType.current
}

@Composable
private fun animateScheme(target: ColorScheme, animate: Boolean): ColorScheme {
    @Composable
    fun Color.animated(label: String): Color {
        if (!animate) return this
        val value by animateColorAsState(this, tween(Motion.TRANSITION_MS, easing = Motion.Transition), label = label)
        return value
    }
    return target.copy(
        primary = target.primary.animated("primary"),
        onPrimary = target.onPrimary.animated("onPrimary"),
        primaryContainer = target.primaryContainer.animated("primaryContainer"),
        onPrimaryContainer = target.onPrimaryContainer.animated("onPrimaryContainer"),
        secondary = target.secondary.animated("secondary"),
        onSecondary = target.onSecondary.animated("onSecondary"),
        secondaryContainer = target.secondaryContainer.animated("secondaryContainer"),
        onSecondaryContainer = target.onSecondaryContainer.animated("onSecondaryContainer"),
        tertiary = target.tertiary.animated("tertiary"),
        background = target.background.animated("background"),
        onBackground = target.onBackground.animated("onBackground"),
        surface = target.surface.animated("surface"),
        onSurface = target.onSurface.animated("onSurface"),
        surfaceVariant = target.surfaceVariant.animated("surfaceVariant"),
        onSurfaceVariant = target.onSurfaceVariant.animated("onSurfaceVariant"),
        surfaceContainerLow = target.surfaceContainerLow.animated("surfaceContainerLow"),
        surfaceContainer = target.surfaceContainer.animated("surfaceContainer"),
        surfaceContainerHigh = target.surfaceContainerHigh.animated("surfaceContainerHigh"),
        surfaceContainerHighest = target.surfaceContainerHighest.animated("surfaceContainerHighest"),
        outline = target.outline.animated("outline"),
        outlineVariant = target.outlineVariant.animated("outlineVariant"),
    )
}
