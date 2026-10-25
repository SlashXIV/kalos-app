package com.kalos.app.core.ui.motion

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Un toucher fige les moments en cours sur leur état final : le contenu passe sans mouvement
 * jusqu'à ce que [resetKey] change (le moment suivant rejoue normalement). Le toucher n'est pas
 * consommé, boutons et défilement restent actifs.
 */
@Composable
fun InterruptibleMotion(resetKey: Any?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var interrupted by remember(resetKey) { mutableStateOf(false) }
    val level = if (interrupted) MotionLevel.NONE else LocalMotionLevel.current
    CompositionLocalProvider(LocalMotionLevel provides level) {
        Box(modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                interrupted = true
            }
        }) { content() }
    }
}

/** Annonce TalkBack polie : lue quand [text] change, jamais à chaque seconde. Vide, elle se tait. */
@Composable
fun LiveAnnouncement(text: String) {
    Box(Modifier.size(1.dp).semantics {
        liveRegion = LiveRegionMode.Polite
        contentDescription = text
    })
}
