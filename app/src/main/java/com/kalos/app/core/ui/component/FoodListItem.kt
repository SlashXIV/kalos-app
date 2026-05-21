package com.kalos.app.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kalos.app.core.domain.model.Food
import kotlin.math.roundToInt

@Composable
fun FoodListItem(
    food: Food,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                food.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                buildString {
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
        modifier = modifier.clickable(onClick = onClick),
    )
}
