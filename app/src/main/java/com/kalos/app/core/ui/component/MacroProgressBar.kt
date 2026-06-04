package com.kalos.app.core.ui.component

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kalos.app.core.ui.theme.ColorCarbs
import com.kalos.app.core.ui.theme.ColorFat
import com.kalos.app.core.ui.theme.ColorOverTarget
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
        animationSpec = tween(700, easing = EaseOutCubic),
        label = "macro_$label"
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // "162 / 160 g" inline — single fixation instead of value here + goal on its own
            // line below the bar. Consumed keeps the macro color; goal stays muted.
            // Over target: the consumed value switches to the amber warning tone.
            val isOver = goal > 0 && consumed.roundToInt() > goal
            Text(
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = if (isOver) ColorOverTarget else color,
                            fontWeight = FontWeight.SemiBold,
                        )
                    ) {
                        append("${consumed.roundToInt()}")
                    }
                    withStyle(
                        SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    ) {
                        append(" / $goal g")
                    }
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
        // Custom track + fill for better visual control
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

@Composable
fun MacroTrioRow(
    proteinConsumed: Float, proteinGoal: Int,
    carbsConsumed: Float, carbsGoal: Int,
    fatConsumed: Float, fatGoal: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MacroRow("Protéines", proteinConsumed, proteinGoal, ColorProtein)
        MacroRow("Glucides", carbsConsumed, carbsGoal, ColorCarbs)
        MacroRow("Lipides", fatConsumed, fatGoal, ColorFat)
    }
}
