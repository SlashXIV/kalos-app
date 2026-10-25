package com.kalos.app.core.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import kotlin.math.abs
import kotlin.math.roundToInt

/** Forme du graphique d'une série. */
enum class MeChartStyle { Bars, Line }

/**
 * Graphique d'une seule série (calendrier, bilan, profil) : barres fines ancrées à la ligne de base,
 * ou ligne de 2 dp avec point final étiqueté. Un seul axe vertical (repères [ticks] en filets).
 *
 * Curseur : au doigt (toucher ou glisser), aux flèches du clavier, et réglable par TalkBack comme
 * une barre de progression (`setProgress`) ; la lecture du point courant ([reading]) est l'état lu
 * et s'affiche sous le graphique. Géométrie calculée une fois par taille et par données
 * (`drawWithCache`) : déplacer le curseur ne fait que redessiner, sans allocation.
 *
 * [band] : zone ombrée (ex. objectif 90–105 %), à nommer par l'appelant dans une légende.
 * Les valeurs au-dessus de [overAbove] passent en `overTarget` ; l'appelant le dit aussi en texte.
 */
@Composable
fun MeSeriesChart(
    values: List<Float>,
    reading: (Int) -> String,
    summary: String,
    modifier: Modifier = Modifier,
    style: MeChartStyle = MeChartStyle.Bars,
    color: Color = MaterialTheme.colorScheme.onSurface,
    xFractions: List<Float>? = null,
    band: ClosedFloatingPointRange<Float>? = null,
    overAbove: Float = Float.POSITIVE_INFINITY,
    ticks: List<Float> = emptyList(),
    tickLabel: (Float) -> String = { formatGroupedInt(it.roundToInt()) },
    endLabel: String? = null,
    height: Dp = 140.dp,
) {
    val n = values.size
    if (n == 0) return
    val last = n - 1
    var index by remember(n) { mutableIntStateOf(last) }
    var focused by remember { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
    val ink = colors.onSurface
    val grid = colors.outlineVariant
    val ring = colors.surfaceContainer
    val bandColor = KalosTheme.nutrients.goalReached.copy(alpha = 0.18f)
    val over = KalosTheme.nutrients.overTarget
    val tickStyle = KalosTheme.type.metricSmall.copy(fontSize = 10.sp, color = colors.onSurfaceVariant)
    val endStyle = KalosTheme.type.metricSmall.copy(color = ink)
    val measurer = rememberTextMeasurer()
    val bars = style == MeChartStyle.Bars
    val fractions = xFractions ?: List(n) { i ->
        when {
            bars -> (i + 0.5f) / n
            n == 1 -> 0.5f
            else -> i / last.toFloat()
        }
    }
    // Zone de dessin connue des gestes (écrite par le cache, lue au toucher).
    val plot = remember { FloatArray(2) }
    fun pick(x: Float) {
        val f = (x - plot[0]) / plot[1].coerceAtLeast(1f)
        var best = 0
        for (i in 1..last) if (abs(fractions[i] - f) < abs(fractions[best] - f)) best = i
        index = best
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .then(if (focused) Modifier.border(1.dp, ink, KalosShape.chip) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .onKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (it.key) {
                        Key.DirectionLeft -> { index = (index - 1).coerceAtLeast(0); true }
                        Key.DirectionRight -> { index = (index + 1).coerceAtMost(last); true }
                        else -> false
                    }
                }
                .focusable()
                .pointerInput(fractions) { detectTapGestures { pick(it.x) } }
                .pointerInput(fractions) {
                    detectHorizontalDragGestures(onDragStart = { pick(it.x) }) { change, _ -> pick(change.position.x) }
                }
                .semantics {
                    contentDescription = summary
                    stateDescription = reading(index)
                    progressBarRangeInfo = ProgressBarRangeInfo(index.toFloat(), 0f..last.toFloat(), (n - 2).coerceAtLeast(0))
                    setProgress { target -> index = target.roundToInt().coerceIn(0, last); true }
                }
                .drawWithCache {
                    val tickLayouts = ticks.map { measurer.measure(tickLabel(it), tickStyle) }
                    val gutter = if (ticks.isEmpty()) 0f else tickLayouts.maxOf { it.size.width } + 6.dp.toPx()
                    val endLayout = endLabel?.let { measurer.measure(it, endStyle) }
                    val dotR = 4.dp.toPx()
                    val right = size.width - if (bars) 0f else dotR + 2.dp.toPx()
                    val plotW = right - gutter
                    plot[0] = gutter; plot[1] = plotW
                    val top = 6.dp.toPx() + (endLayout?.size?.height ?: 0) + if (bars) 0f else dotR
                    val bottom = size.height - if (bars) 1.dp.toPx() else dotR + 2.dp.toPx()

                    val all = values + ticks + listOfNotNull(band?.start, band?.endInclusive)
                    val lo: Float
                    val hi: Float
                    if (bars) {
                        lo = 0f; hi = all.max().coerceAtLeast(1f) * 1.08f
                    } else {
                        val span = (all.max() - all.min()).coerceAtLeast(1f)
                        lo = all.min() - span * 0.08f; hi = all.max() + span * 0.08f
                    }
                    fun y(v: Float) = bottom - (v - lo) / (hi - lo) * (bottom - top)
                    val xs = FloatArray(n) { gutter + fractions[it] * plotW }
                    val ys = FloatArray(n) { y(values[it]) }

                    val inkPath = Path()
                    val overPath = Path()
                    if (bars) {
                        val barW = (plotW / n * 0.55f).coerceIn(2.dp.toPx(), 10.dp.toPx())
                        val r = CornerRadius(minOf(barW / 2, 3.dp.toPx()))
                        for (i in 0..last) {
                            val rect = Rect(xs[i] - barW / 2, ys[i], xs[i] + barW / 2, bottom)
                            (if (values[i] > overAbove) overPath else inkPath)
                                .addRoundRect(RoundRect(rect, topLeft = r, topRight = r))
                        }
                    } else {
                        inkPath.moveTo(xs[0], ys[0])
                        for (i in 1..last) inkPath.lineTo(xs[i], ys[i])
                    }
                    val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val hair = 1.dp.toPx()
                    val tickYs = FloatArray(ticks.size) { y(ticks[it]) }
                    val bandTop = band?.let { y(it.endInclusive) } ?: 0f
                    val bandBottom = band?.let { y(it.start) } ?: 0f

                    onDrawBehind {
                        if (band != null) drawRect(bandColor, Offset(gutter, bandTop), Size(plotW, bandBottom - bandTop))
                        for (t in tickYs.indices) {
                            drawLine(grid, Offset(gutter, tickYs[t]), Offset(size.width, tickYs[t]), hair)
                            val l = tickLayouts[t]
                            drawText(l, topLeft = Offset(0f, (tickYs[t] - l.size.height / 2f).coerceIn(0f, size.height - l.size.height)))
                        }
                        if (bars) drawLine(grid, Offset(gutter, bottom), Offset(size.width, bottom), hair)
                        // Curseur : filet vertical sous les marques.
                        val cx = xs[index]
                        drawLine(ink, Offset(cx, top - 4.dp.toPx()), Offset(cx, bottom), hair)
                        if (bars) {
                            drawPath(inkPath, color)
                            drawPath(overPath, over)
                        } else {
                            drawPath(inkPath, color, style = stroke)
                            drawCircle(ring, dotR + 2.dp.toPx(), Offset(xs[last], ys[last]))
                            drawCircle(color, dotR, Offset(xs[last], ys[last]))
                            if (index != last) {
                                drawCircle(ring, dotR + 2.dp.toPx(), Offset(cx, ys[index]))
                                drawCircle(ink, dotR, Offset(cx, ys[index]))
                            }
                            if (endLayout != null) {
                                // À gauche du point : le filet du curseur, posé sur le dernier point par défaut, ne le barre pas.
                                // Au-dessus, sauf si la ligne arrive d'en haut (chute) : alors à hauteur du point.
                                val h = endLayout.size.height
                                val falling = last > 0 && ys[last] - ys[last - 1] > h
                                val lx = (xs[last] - dotR - 4.dp.toPx() - endLayout.size.width).coerceAtLeast(gutter)
                                val ly = (if (falling) ys[last] - h / 2f else ys[last] - dotR - 4.dp.toPx() - h)
                                    .coerceIn(0f, size.height - h)
                                drawText(endLayout, topLeft = Offset(lx, ly))
                            }
                        }
                    }
                },
        )
        // Lecture du point courant ; TalkBack la lit déjà comme état du graphique. Lue dans son
        // propre composable : déplacer le curseur ne recompose pas (ni ne reconstruit) le graphique.
        ChartReading({ reading(index) }, colors.onSurfaceVariant)
    }
}

@Composable
private fun ChartReading(text: () -> String, color: androidx.compose.ui.graphics.Color) {
    Text(text(), style = KalosTheme.type.metricSmall, color = color, modifier = Modifier.clearAndSetSemantics { })
}
