package com.kalos.app.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kalos.app.core.ui.motion.LocalMotionLevel
import com.kalos.app.core.ui.motion.Motion
import com.kalos.app.core.ui.theme.KalosTheme
import kotlin.math.cos
import kotlin.math.sin

/** L'objectif est placé aux 4/5 de l'échelle : on voit venir l'objectif et on voit le dépassement. */
private const val SCALE_HEADROOM = 1.25f

/**
 * Valeur animée « à la pesée » : l'aiguille se pose avec un léger amorti en ambiance expressive,
 * glisse brièvement en ambiance sobre, saute directement sans animations.
 */
@Composable
fun animateMeasure(target: Float, label: String): State<Float> {
    val level = LocalMotionLevel.current
    val spec = when {
        level.decorative -> tween<Float>(Motion.WEIGH_MS, easing = Motion.Weigh)
        level.animates -> tween(200, easing = Motion.Enter)
        else -> snap()
    }
    return animateFloatAsState(target, spec, label = label)
}

/** Simple rangée de graduations décoratives (aperçu de palette, soulignement d'onglet). */
@Composable
fun Graduation(color: Color, modifier: Modifier = Modifier, spacing: Dp = 5.dp) {
    Canvas(modifier) {
        val step = spacing.toPx()
        val count = (size.width / step).toInt()
        val w = 1.dp.toPx()
        for (i in 0..count) {
            val x = i * step + w / 2
            val h = if (i % 5 == 0) size.height else size.height * 0.55f
            drawLine(color, Offset(x, size.height), Offset(x, size.height - h), w)
        }
    }
}

/**
 * Règle graduée d'une quantité : graduations allumées jusqu'à la valeur, repère pointillé de
 * l'objectif, aiguille sous la règle. Purement visuelle : l'appelant fournit la phrase TalkBack
 * (voir [MeasureRow]). Les graduations sont calculées une fois par taille ; la valeur ne fait que
 * déplacer une découpe et l'aiguille (aucune allocation par image pendant l'animation).
 */
@Composable
fun GraduatedScale(
    value: Float,
    goal: Float,
    color: Color,
    modifier: Modifier = Modifier,
    ticks: Int = 25,
) {
    val tickColor = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val max = if (goal > 0f) goal * SCALE_HEADROOM else maxOf(value, 1f)
    val shown by animateMeasure(value, "règle")
    Box(
        modifier
            .fillMaxWidth()
            .height(22.dp)
            .drawWithCache {
                val pad = 4.dp.toPx()
                val base = size.height - 6.dp.toPx()
                val unit = (size.width - 2 * pad) / ticks
                val minor = ArrayList<Offset>(ticks * 2)
                val major = ArrayList<Offset>(ticks / 5 * 2 + 2)
                for (i in 0..ticks) {
                    val x = pad + i * unit
                    if (i % 5 == 0) major += listOf(Offset(x, base), Offset(x, base - 12.dp.toPx()))
                    else minor += listOf(Offset(x, base), Offset(x, base - 7.dp.toPx()))
                }
                val minorW = 1.3.dp.toPx()
                val majorW = 2.dp.toPx()
                val gx = pad + (goal / max).coerceIn(0f, 1f) * (size.width - 2 * pad)
                val dash = 2.dp.toPx()
                val needle = Path().apply {
                    moveTo(0f, 0f); lineTo(4.5.dp.toPx(), 5.5.dp.toPx()); lineTo(-4.5.dp.toPx(), 5.5.dp.toPx()); close()
                }
                fun drawTicks(c: Color, draw: androidx.compose.ui.graphics.drawscope.DrawScope) = with(draw) {
                    drawPoints(minor, PointMode.Lines, c, minorW, StrokeCap.Round)
                    drawPoints(major, PointMode.Lines, c, majorW, StrokeCap.Round)
                }
                onDrawBehind {
                    drawTicks(tickColor, this)
                    val vx = pad + (shown / max).coerceIn(0f, 1f) * (size.width - 2 * pad)
                    clipRect(right = vx + majorW) { drawTicks(color, this) }
                    if (goal > 0f) {
                        var y = 1f
                        while (y < base + 2) {
                            drawLine(ink, Offset(gx, y), Offset(gx, minOf(y + dash, base + 2)), 1.5.dp.toPx())
                            y += dash * 2
                        }
                    }
                    translate(vx, base + 1.dp.toPx()) { drawPath(needle, ink) }
                }
            },
    )
}

/**
 * Cadran gradué de la journée (270°, 60 graduations) : les graduations passent à l'encre jusqu'à la
 * valeur, à la couleur « au-dessus » après la zone d'objectif (90–105 %), aiguille à l'accent.
 * [lit] (0..1 de l'échelle) allume les graduations en vert : le moment « objectif atteint ».
 */
@Composable
fun GraduatedDial(
    value: Float,
    goal: Float,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    lit: Float = 0f,
    center: @Composable BoxScope.() -> Unit,
) {
    val tickColor = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val needleColor = MaterialTheme.colorScheme.primary
    val nutrients = KalosTheme.nutrients
    val max = if (goal > 0f) goal * SCALE_HEADROOM else maxOf(value, 1f)
    val shown by animateMeasure(value, "cadran")
    Box(
        modifier
            .size(size)
            .drawWithCache {
                val n = 60
                val c = Offset(this.size.width / 2, this.size.height / 2)
                val r = this.size.minDimension / 2 - 8.dp.toPx()
                fun angle(f: Float) = Math.toRadians(135.0 + 270.0 * f.coerceIn(0f, 1f))
                val outer = Array(n + 1) { i -> polar(c, r - 4.dp.toPx(), angle(i / n.toFloat())) }
                val inner = Array(n + 1) { i -> polar(c, r - 4.dp.toPx() - (if (i % 5 == 0) 16 else 9).dp.toPx(), angle(i / n.toFloat())) }
                val zoneLo = goal * 0.9f / max
                val zoneHi = goal * 1.05f / max
                val zr = r + 3.dp.toPx()
                val zoneStart = (135f + 270f * zoneLo)
                val zoneSweep = 270f * (zoneHi - zoneLo)
                val majorW = 2.2.dp.toPx()
                val minorW = 1.4.dp.toPx()
                val needleW = 3.2.dp.toPx()
                // L'aiguille couvre l'anneau des graduations sans entrer dans le centre (chiffres).
                val needleOut = r + 2.dp.toPx()
                val needleIn = r - 26.dp.toPx()
                onDrawBehind {
                    if (goal > 0f) drawArc(
                        nutrients.goalReached, zoneStart, zoneSweep, false,
                        topLeft = Offset(c.x - zr, c.y - zr), size = Size(zr * 2, zr * 2),
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                    )
                    val f = shown / max
                    for (i in 0..n) {
                        val t = i / n.toFloat()
                        val col = when {
                            lit > 0f && t <= lit -> nutrients.goalReached
                            t <= f + 1e-4f -> if (t > zoneHi) nutrients.overTarget else ink
                            else -> tickColor
                        }
                        drawLine(col, outer[i], inner[i], if (i % 5 == 0) majorW else minorW, StrokeCap.Round)
                    }
                    val a = angle(f)
                    drawLine(needleColor, polar(c, needleIn, a), polar(c, needleOut, a), needleW, StrokeCap.Round)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Le texte central tient dans le cercle intérieur : réduit (largeur et hauteur) plutôt
        // que de déborder sur les graduations quand la police système est agrandie.
        Box(Modifier.size(size * 0.56f).shrinkToFit(), contentAlignment = Alignment.Center, content = center)
    }
}

/** Réduit uniformément son contenu pour qu'il tienne dans les contraintes, sans jamais l'agrandir. */
private fun Modifier.shrinkToFit(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints())
    val scale = minOf(
        1f,
        constraints.maxWidth.toFloat() / placeable.width.coerceAtLeast(1),
        constraints.maxHeight.toFloat() / placeable.height.coerceAtLeast(1),
    )
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.placeWithLayer(
            (constraints.maxWidth - placeable.width) / 2,
            (constraints.maxHeight - placeable.height) / 2,
        ) { scaleX = scale; scaleY = scale }
    }
}

private fun polar(c: Offset, r: Float, a: Double) = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
