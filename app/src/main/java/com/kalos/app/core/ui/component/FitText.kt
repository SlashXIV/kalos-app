package com.kalos.app.core.ui.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Texte sur une seule ligne qui se réduit au lieu de passer à la ligne ou de déborder :
 * petits écrans et grandes polices système compris. La taille du style est un plafond ;
 * en dessous de [minFontSize], le texte est rogné en dernier recours.
 *
 * Réduction par mise à l'échelle à la mise en page, et non `TextAutoSize` (qui rogne un mot
 * unique trop large, « REPREN ») ni `BoxWithConstraints` (qui casse les mesures intrinsèques
 * des boutons segmentés).
 */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    minFontSize: TextUnit = 10.sp,
) {
    val resolved = LocalTextStyle.current.merge(style)
    val minScale = if (resolved.fontSize.isSpecified && resolved.fontSize.value > 0f) {
        (minFontSize.value / resolved.fontSize.value).coerceAtMost(1f)
    } else {
        1f
    }
    Text(
        text = text,
        modifier = modifier.clipToBounds().shrinkToFitWidth(minScale),
        color = color,
        style = resolved,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}

/**
 * Réduit son contenu (une ligne) à la largeur disponible, sans descendre sous [minScale].
 * Pour un contenu qui n'est pas un simple texte (chiffres qui roulent).
 */
fun Modifier.shrinkToFitWidth(minScale: Float = 0f): Modifier = layout { measurable, constraints ->
    // Largeur naturelle sur une ligne : une mesure sans borne renverrait la largeur
    // maximale dès qu'un alignement de texte est actif.
    val natural = measurable.maxIntrinsicWidth(Constraints.Infinity)
    val placeable = measurable.measure(Constraints.fixedWidth(natural))
    val maxWidth = constraints.maxWidth
    val scale = if (constraints.hasBoundedWidth && natural > maxWidth) {
        (maxWidth.toFloat() / natural).coerceAtLeast(minScale)
    } else {
        1f
    }
    val width = (natural * scale).roundToInt().coerceIn(constraints.minWidth, maxWidth)
    val height = (placeable.height * scale).roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(width, height) {
        placeable.placeWithLayer(0, 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}
