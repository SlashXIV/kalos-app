package com.kalos.app.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kalos.app.R
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.SatietyLevel
import com.kalos.app.core.ui.util.color
import com.kalos.app.core.ui.util.foodSatietyLevel
import com.kalos.app.core.ui.util.formatGroupedInt
import kotlin.math.roundToInt

/**
 * Ligne d'aliment : nom, signal de satiété, valeurs pour 100 g en chiffres tabulaires et fine
 * barre de répartition énergétique des macros (les grammes restent écrits : la couleur n'est
 * jamais seule). TalkBack lit une phrase ; [trailingContent] reste un nœud à part.
 */
@Composable
fun FoodListItem(
    food: Food,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val level = foodSatietyLevel(food)
    val kcal = formatGroupedInt(food.kcalPer100g.roundToInt())
    val p = food.proteinPer100g.roundToInt().toString()
    val c = food.carbsPer100g.roundToInt().toString()
    val f = food.fatPer100g.roundToInt().toString()
    val customSpoken = stringResource(R.string.food_custom_spoken)
    val macrosSpoken = stringResource(R.string.food_item_spoken, kcal, p, c, f)
    // Satiété affichée seulement aux extrêmes utiles ; « Modéré » reste discret.
    val satiety = level.takeIf { it != SatietyLevel.MODERATE }?.label
    val spoken = listOfNotNull(
        food.name,
        customSpoken.takeIf { food.isCustom },
        food.brand.ifEmpty { null },
        satiety,
        macrosSpoken,
    ).joinToString(", ")

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(start = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f).clearAndSetSemantics { contentDescription = spoken },
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    food.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (food.isCustom) {
                    InstrumentLabel(
                        stringResource(R.string.food_custom_badge),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (satiety != null || food.brand.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (satiety != null) {
                        Dot(level.color())
                        Text(satiety, style = MaterialTheme.typography.labelMedium, color = level.color())
                    }
                    if (food.brand.isNotEmpty()) {
                        Text(
                            food.brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.food_per100_line, kcal, p, c, f),
                style = KalosTheme.type.metricSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            MacroSplitBar(food.proteinPer100g, food.carbsPer100g, food.fatPer100g)
        }
        trailingContent?.invoke()
    }
}

/**
 * Répartition de l'énergie des macros (4 / 4 / 9 kcal par gramme), dans l'ordre P, G, L du texte.
 * Un seul nœud dessiné par ligne : la liste de recherche en compte des centaines.
 */
@Composable
private fun MacroSplitBar(protein: Float, carbs: Float, fat: Float) {
    val n = KalosTheme.nutrients
    val total = protein * 4f + carbs * 4f + fat * 9f
    if (total <= 0f) return
    Spacer(
        Modifier.fillMaxWidth(0.6f).height(3.dp).drawBehind {
            val gap = 2.dp.toPx()
            val radius = CornerRadius(1.dp.toPx())
            var x = 0f
            val usable = size.width - gap * 2
            for ((energy, color) in arrayOf(protein * 4f to n.protein, carbs * 4f to n.carbs, fat * 9f to n.fat)) {
                if (energy <= 0f) continue
                val w = usable * energy / total
                drawRoundRect(color, Offset(x, 0f), Size(w, size.height), radius)
                x += w + gap
            }
        },
    )
}
