package com.kalos.app.core.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.kalos.app.core.ui.component.FitText
import kotlinx.coroutines.delay

/**
 * Entrée « montée » : fondu et glissement de 12 dp vers le haut, décalée selon
 * [index]. Jouée une seule fois, à la première composition.
 */
fun Modifier.riseIn(index: Int = 0, baseDelayMs: Int = 0): Modifier = composed {
    val level = LocalMotionLevel.current
    if (!level.animates) return@composed this
    val progress = remember { Animatable(0f) }
    val offsetPx = with(LocalDensity.current) { 12.dp.toPx() }
    LaunchedEffect(Unit) {
        val stagger = if (level.decorative) Motion.ITEM_STAGGER_MS * index.coerceAtMost(8) else 0
        delay((baseDelayMs + stagger).toLong())
        progress.animateTo(1f, tween(if (level.decorative) Motion.ENTER_MS else 200, easing = Motion.Enter))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * offsetPx
    }
}

/** Pression élastique : 0,97 en appui, retour avec un léger rebond. */
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val decorative = LocalMotionLevel.current.decorative
    val scale by animateFloatAsState(
        targetValue = if (pressed && decorative) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "pression", // texte-technique : nom d'animation, jamais affiché
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Texte dont les lettres montent une à une derrière un masque (titre d'affiche).
 * [accentFrom] : à partir de cet indice, les lettres prennent [accent].
 */
@Composable
fun RisingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    accent: Color = Color.Unspecified,
    accentFrom: Int = Int.MAX_VALUE,
    startDelayMs: Int = 150,
) {
    // Titre d'affiche : il ne passe jamais à la ligne ni ne déborde, il se réduit.
    // Un seul nœud pour TalkBack : animées, les lettres sont autant de Text, qui
    // seraient épelées une à une.
    BoxWithConstraints(modifier.clearAndSetSemantics { contentDescription = text; heading() }) {
        val measurer = rememberTextMeasurer()
        val maxWidthPx = constraints.maxWidth
        val fitted = remember(text, style, maxWidthPx) {
            // Animées, les lettres sont posées une à une, sans crénage entre elles : leur
            // somme dépasse la largeur de la phrase, et la fin du titre était rognée.
            fun widthOf(part: String) = measurer.measure(part, style, maxLines = 1, softWrap = false).size.width
            val width = maxOf(widthOf(text), text.sumOf { widthOf(it.toString()) })
            if (maxWidthPx == Constraints.Infinity || width <= maxWidthPx) style
            else style.copy(fontSize = style.fontSize * (maxWidthPx.toFloat() / width) * 0.98f)
        }
        RisingLetters(text, fitted, color, accent, accentFrom, startDelayMs)
    }
}

@Composable
private fun RisingLetters(
    text: String,
    style: TextStyle,
    color: Color,
    accent: Color,
    accentFrom: Int,
    startDelayMs: Int,
) {
    val level = LocalMotionLevel.current
    if (!level.decorative) {
        // Sans mouvement, l'accent reste : il fait partie du titre, pas de l'animation.
        val styled = buildAnnotatedString {
            append(text)
            if (accentFrom < text.length) addStyle(SpanStyle(color = accent), accentFrom, text.length)
        }
        Text(styled, style = style, color = color, maxLines = 1, softWrap = false)
        return
    }
    Row(Modifier.clipToBounds()) {
        text.forEachIndexed { i, ch ->
            val progress = remember(text) { Animatable(0f) }
            LaunchedEffect(text) {
                delay((startDelayMs + i * Motion.LETTER_STAGGER_MS).toLong())
                progress.animateTo(1f, tween(650, easing = Motion.Enter))
            }
            Text(
                ch.toString(),
                style = style,
                color = if (i >= accentFrom) accent else color,
                softWrap = false,
                modifier = Modifier.graphicsLayer {
                    alpha = progress.value
                    translationY = (1f - progress.value) * size.height * 0.8f
                },
            )
        }
    }
}

/** Nombre qui compte jusqu'à sa valeur ([format] reçoit la valeur courante). */
@Composable
fun CountUp(
    value: Double,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    durationMs: Int = 900,
) {
    val level = LocalMotionLevel.current
    val animated = remember { Animatable(if (level.decorative) 0f else value.toFloat()) }
    LaunchedEffect(value) {
        if (level.decorative) animated.animateTo(value.toFloat(), tween(durationMs, easing = Motion.Enter))
        else animated.snapTo(value.toFloat())
    }
    // Mouvement coupé en cours de route (séquence interrompue) : la valeur finale, tout de suite.
    val shown = if (LocalMotionLevel.current.decorative) animated.value.toDouble() else value
    FitText(format(shown), modifier, style = style, color = color)
}
