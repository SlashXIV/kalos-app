package com.kalos.app.core.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kalos.app.core.ui.theme.KalosOrange
import kotlin.math.roundToInt

@Composable
fun CalorieProgressRing(
    consumed: Float,
    goal: Int,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 14.dp,
) {
    val progress = if (goal > 0) (consumed / goal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "calorieRing"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fillColor = KalosOrange

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)
            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round),
            )
            // Progress arc
            drawArc(
                color = fillColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = consumed.roundToInt().toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "/ $goal kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
