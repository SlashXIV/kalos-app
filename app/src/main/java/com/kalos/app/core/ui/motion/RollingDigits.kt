package com.kalos.app.core.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import com.kalos.app.core.ui.component.shrinkToFitWidth

/**
 * Chiffres qui roulent : seul le caractère qui change descend en place (300 ms,
 * courbe d'entrée), les autres ne bougent pas. Prévu pour une police tabulaire :
 * la ligne garde sa largeur d'une seconde à l'autre. Se réduit à la largeur
 * disponible, sans animation hors ambiance expressive.
 */
@Composable
fun RollingDigits(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minScale: Float = 0.5f,
) {
    val decorative = LocalMotionLevel.current.decorative
    Row(modifier.clipToBounds().shrinkToFitWidth(minScale).clearAndSetSemantics { contentDescription = text }) {
        // Clé : position depuis la droite, pour que « 9:59 » → « 10:00 » ne décale pas les unités.
        text.forEachIndexed { index, char ->
            key(text.length - index) {
                RollingChar(char, style, color, decorative)
            }
        }
    }
}

@Composable
private fun RollingChar(char: Char, style: TextStyle, color: Color, decorative: Boolean) {
    val progress = remember { Animatable(1f) }
    // Première apparition : le chiffre est déjà là, il ne roule qu'à ses changements.
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(char) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (!decorative) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(ROLL_MS, easing = Motion.Enter))
    }
    Text(
        char.toString(),
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.graphicsLayer {
            translationY = -(1f - progress.value) * size.height * 0.35f
            alpha = progress.value
        },
    )
}

private const val ROLL_MS = 300
