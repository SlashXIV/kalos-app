package com.kalos.app.core.ui.component

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kalos.app.core.ui.theme.ColorCarbs
import com.kalos.app.core.ui.theme.ColorFat
import com.kalos.app.core.ui.theme.ColorProtein
import kotlin.math.roundToInt

@Composable
fun MacroRow(
    label: String,
    consumed: Float,
    goal: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val progress = if (goal > 0) (consumed / goal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "macro_$label"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${consumed.roundToInt()} / ${goal}g",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun MacroTrioRow(
    proteinConsumed: Float, proteinGoal: Int,
    carbsConsumed: Float, carbsGoal: Int,
    fatConsumed: Float, fatGoal: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroRow("Protéines", proteinConsumed, proteinGoal, ColorProtein)
        MacroRow("Glucides", carbsConsumed, carbsGoal, ColorCarbs)
        MacroRow("Lipides", fatConsumed, fatGoal, ColorFat)
    }
}
