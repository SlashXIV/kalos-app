package com.kalos.app.core.ui.motion

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/** Réglage « Vibrations », fourni par KalosTheme. */
val LocalVibrationsEnabled = staticCompositionLocalOf { true }

/**
 * Vibrations courtes, toutes au même endroit pour garder un vocabulaire cohérent.
 * Coupées en interne quand le réglage « Vibrations » est désactivé : aucun appelant n'a à y penser.
 * Passent par la vue, donc respectent aussi le retour tactile du système.
 */
class Haptics internal constructor(private val view: View, private val enabled: Boolean) {
    /** Petit événement : aliment ajouté, série validée, choix modifié. */
    fun tick() {
        if (enabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Événement accompli : objectif atteint, fin du repos, séance terminée. */
    fun confirm() {
        if (!enabled) return
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.LONG_PRESS,
        )
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val enabled = LocalVibrationsEnabled.current
    return remember(view, enabled) { Haptics(view, enabled) }
}
