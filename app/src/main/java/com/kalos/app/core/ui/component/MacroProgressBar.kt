package com.kalos.app.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kalos.app.R
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import kotlin.math.roundToInt

/** Les trois macros en règles graduées (protéines, glucides, lipides). */
@Composable
fun MacroTrioRow(
    proteinConsumed: Float, proteinGoal: Int,
    carbsConsumed: Float, carbsGoal: Int,
    fatConsumed: Float, fatGoal: Int,
    modifier: Modifier = Modifier,
) {
    val nutrients = KalosTheme.nutrients
    val unit = stringResource(R.string.unit_g)
    val spoken = stringResource(R.string.unit_grams_spoken)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            Triple(R.string.nutrition_protein, proteinConsumed to proteinGoal, nutrients.protein),
            Triple(R.string.nutrition_carbs, carbsConsumed to carbsGoal, nutrients.carbs),
            Triple(R.string.nutrition_fat, fatConsumed to fatGoal, nutrients.fat),
        ).forEach { (label, values, color) ->
            MeasureRow(
                label = stringResource(label),
                value = values.first,
                goal = values.second.toFloat(),
                valueText = formatGroupedInt(values.first.roundToInt()),
                goalText = formatGroupedInt(values.second),
                unit = unit,
                spokenUnit = spoken,
                color = color,
            )
        }
    }
}
