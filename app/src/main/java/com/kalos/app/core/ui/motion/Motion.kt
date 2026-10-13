package com.kalos.app.core.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kalos.app.core.ui.theme.MotionPreference

/** Langage de mouvement de Kalos : celui d'un instrument de mesure. Peu de courbes, toujours les mêmes. */
object Motion {
    /** Pesée : l'aiguille ou la valeur se pose, avec un léger dépassement amorti. */
    val Weigh = CubicBezierEasing(0.22f, 0.9f, 0.3f, 1.12f)
    /** Entrées : départ vif, arrivée douce. */
    val Enter = CubicBezierEasing(0.25f, 0.8f, 0.25f, 1f)
    /** Transitions et balayages. */
    val Transition = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    const val WEIGH_MS = 620
    const val ENTER_MS = 420
    const val TRANSITION_MS = 480
    const val LETTER_STAGGER_MS = 40
    const val ITEM_STAGGER_MS = 50
}

enum class MotionLevel {
    FULL,     // Ambiance expressive : tout le mouvement
    REDUCED,  // Ambiance sobre : transitions brèves, aucun effet décoratif
    NONE;     // « Supprimer les animations » du système

    val decorative: Boolean get() = this == FULL
    val animates: Boolean get() = this != NONE
}

val LocalMotionLevel = staticCompositionLocalOf { MotionLevel.FULL }

/** Le réglage système d'accessibilité prime toujours sur l'ambiance choisie. */
fun motionLevelOf(systemAnimatorScale: Float, preference: MotionPreference): MotionLevel = when {
    systemAnimatorScale == 0f -> MotionLevel.NONE
    preference == MotionPreference.CALM -> MotionLevel.REDUCED
    else -> MotionLevel.FULL
}

/** Relu à chaque retour dans l'appli : le réglage système peut changer pendant qu'elle est en arrière-plan. */
@Composable
fun rememberMotionLevel(preference: MotionPreference): MotionLevel {
    val resolver = LocalContext.current.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    // Lu dès la première composition : aucune image animée avant le premier onResume.
    var scale by remember { mutableFloatStateOf(read()) }
    LifecycleResumeEffect(resolver) {
        scale = read()
        onPauseOrDispose { }
    }
    return motionLevelOf(scale, preference)
}
