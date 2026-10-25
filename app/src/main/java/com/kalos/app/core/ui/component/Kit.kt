package com.kalos.app.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalos.app.core.ui.motion.pressScale
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.motion.RisingText

/** Carte d'instrument : surface pleine, peu d'arrondi, pas d'ombre. */
@Composable
fun KalosCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .clip(KalosShape.card)
        .background(MaterialTheme.colorScheme.surfaceContainer)
    Column(
        (if (onClick != null) base.clickable(onClick = onClick) else base).padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Étiquette capitale espacée, façon plaque d'instrument. */
@Composable
fun InstrumentLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), modifier = modifier, style = KalosTheme.type.label, color = color)
}

/** Titre de section : étiquette lue comme un titre par TalkBack. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) =
    InstrumentLabel(text, modifier.semantics { heading() })

/**
 * En-tête d'écran : sur-titre (date, contexte), titre d'affiche qui se réduit au lieu de couper,
 * actions à droite. Le titre est un seul nœud « titre » pour TalkBack.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    navigation: (@Composable () -> Unit)? = null,
    small: Boolean = false,
    rising: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (navigation != null) Box(Modifier.padding(bottom = 2.dp)) { navigation() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (eyebrow != null) InstrumentLabel(eyebrow)
            val style = if (small) KalosTheme.type.posterSmall else KalosTheme.type.poster
            val titleNode = Modifier.clearAndSetSemantics { contentDescription = title; heading() }
            // [rising] : le titre monte lettre à lettre (moment « séance terminée »).
            if (rising) Box(titleNode) {
                RisingText(title.uppercase(), style = style, color = MaterialTheme.colorScheme.onBackground)
            } else FitText(
                title.uppercase(),
                style = style,
                color = MaterialTheme.colorScheme.onBackground,
                minFontSize = 18.sp,
                modifier = titleNode,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Bouton principal : plein, à l'accent, enfoncement à la pression. */
@Composable
fun KalosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ghost: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val label: @Composable RowScope.() -> Unit = {
        FitText(text.uppercase(), style = KalosTheme.type.action)
    }
    val m = modifier.heightIn(min = 48.dp).pressScale(interaction)
    if (ghost) {
        OutlinedButton(
            onClick, m, enabled = enabled, shape = KalosShape.control, interactionSource = interaction,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            content = label,
        )
    } else {
        Button(onClick, m, enabled = enabled, shape = KalosShape.control, interactionSource = interaction, content = label)
    }
}

/** Puce de valeur rapide (+250 ml, 150 g) : chiffres tabulaires, cible tactile de 48 dp. */
@Composable
fun ValueChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentDescription: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(KalosShape.chip)
            .then(if (selected) Modifier.background(colors.onSurface) else Modifier.border(1.dp, colors.outlineVariant, KalosShape.chip))
            .clickable(onClick = onClick)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Quatre puces sur une ligne : elles se réduisent sur écran étroit plutôt que de rogner « ml ».
        FitText(text, style = KalosTheme.type.metricSmall, color = if (selected) colors.surface else colors.onSurface, minFontSize = 7.sp)
    }
}

/**
 * Ticket de pesée : un repas. Bord supérieur dentelé, lignes « nom ····· grammes  kcal ».
 */
@Composable
fun MeasureTicket(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val notch = MaterialTheme.colorScheme.background
    Column(
        modifier
            .fillMaxWidth()
            .clip(KalosShape.card)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .drawBehind {
                // Dents du ticket : demi-cercles de la couleur du fond posés sur le bord haut.
                val r = 4.dp.toPx()
                var x = r
                while (x < size.width) { drawCircle(notch, r, Offset(x, 0f)); x += r * 2.5f }
            }
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/** Ligne d'un ticket : nom suivi de pointillés jusqu'à la quantité, puis la valeur. */
@Composable
fun TicketLine(
    name: String,
    amount: String,
    value: String,
    spoken: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val leader = MaterialTheme.colorScheme.outline
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Row(modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
      // Une phrase pour TalkBack (« Riz, 200 grammes, 260 kilocalories ») ; l'action à droite reste à part.
      Row(Modifier.weight(1f).clearAndSetSemantics { contentDescription = spoken }, verticalAlignment = Alignment.CenterVertically) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            onTextLayout = { layout = it },
            modifier = Modifier
                .weight(1f)
                .drawBehind {
                    // Pointillés depuis la fin de la dernière ligne du nom jusqu'au bord.
                    val l = layout ?: return@drawBehind
                    val last = l.lineCount - 1
                    val start = l.getLineRight(last) + 6.dp.toPx()
                    val y = l.getLineBaseline(last)
                    if (start < size.width - 6.dp.toPx()) drawLine(
                        leader, Offset(start, y), Offset(size.width - 6.dp.toPx(), y), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 3.dp.toPx())),
                    )
                },
        )
        Text(amount, style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = KalosTheme.type.metricSmall, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 44.dp))
      }
        trailing()
    }
}

/** Pastille de couleur d'une série ou d'un nutriment, toujours à côté de son libellé. */
@Composable
fun Swatch(color: Color, modifier: Modifier = Modifier, shape: Shape = KalosShape.chip) {
    Box(modifier.size(9.dp).clip(shape).background(color))
}

/** Pastille ronde (séance faite / planifiée). */
@Composable
fun Dot(color: Color, filled: Boolean = true, modifier: Modifier = Modifier) {
    Box(
        modifier.size(7.dp).clip(CircleShape)
            .then(if (filled) Modifier.background(color) else Modifier.border(1.5.dp, color, CircleShape)),
    )
}

/** Simple surface pleine page qui suit le fond du thème (écrans sans Scaffold). */
@Composable
fun KalosSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Surface(modifier, color = MaterialTheme.colorScheme.background, content = content)
