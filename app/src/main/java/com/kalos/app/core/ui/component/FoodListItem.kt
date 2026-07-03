package com.kalos.app.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.ui.util.SatietyLevel
import com.kalos.app.core.ui.util.color
import com.kalos.app.core.ui.util.foodSatietyLevel
import kotlin.math.roundToInt

@Composable
fun FoodListItem(
    food: Food,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    food.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (food.isCustom) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            "Perso",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        },
        supportingContent = {
            // Satiety (fullness-per-calorie) prefix, coloured — shown only for the actionable
            // extremes (Rassasiant / Peu rassasiant); "Modéré" stays quiet to avoid noise.
            val level = foodSatietyLevel(food)
            val satietyColor = level.color()
            Text(
                buildAnnotatedString {
                    if (level != SatietyLevel.MODERATE) {
                        withStyle(SpanStyle(color = satietyColor, fontWeight = FontWeight.SemiBold)) {
                            append("${level.label} · ")
                        }
                    }
                    if (food.brand.isNotEmpty()) append("${food.brand} • ")
                    append("${food.kcalPer100g.roundToInt()} kcal")
                    append(" • P: ${food.proteinPer100g.roundToInt()}g")
                    append(" • G: ${food.carbsPer100g.roundToInt()}g")
                    append(" • L: ${food.fatPer100g.roundToInt()}g")
                    append(" / 100g")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        },
        trailingContent = trailingContent,
        modifier = modifier.clickable(onClick = onClick),
    )
}
