package com.kalos.app.feature.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.ui.theme.ColorOverTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    navController: NavController,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bilan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Period selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                InsightsPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = state.period == period,
                        onClick = { viewModel.setPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(index, InsightsPeriod.entries.size),
                    ) { Text(period.label) }
                }
            }

            SummaryCard(text = state.summary)
            NutritionCard(state.nutrition)
            WeightCard(state.weight)
            TrainingCard(state.training)

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SummaryCard(text: String) {
    if (text.isBlank()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "À retenir",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            }
            content()
        }
    }
}

@Composable
private fun NutritionCard(n: NutritionInsight) {
    SectionCard(Icons.Filled.Restaurant, "Nutrition") {
        if (n.daysWithData == 0) {
            EmptyLine("Aucun repas logué sur la période.")
            return@SectionCard
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatBlock(
                label = "Calories / j (moy.)",
                value = "${n.avgKcal}",
                sub = if (n.goalKcal > 0) "/ ${n.goalKcal} kcal" else "kcal",
                valueColor = if (n.isKcalOver) ColorOverTarget else MaterialTheme.colorScheme.primary,
            )
            StatBlock(
                label = "Protéines / j (moy.)",
                value = "${n.avgProtein}",
                sub = if (n.goalProtein > 0) "/ ${n.goalProtein} g" else "g",
                valueColor = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            "Calories dans la cible : ${n.daysOnKcalTarget}/${n.daysWithData} jours · Protéines atteintes : ${n.daysOnProteinTarget}/${n.daysWithData} jours",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (n.kcalPerDay.size >= 2) {
            BarChart(
                values = n.kcalPerDay,
                goal = n.goalKcal.toFloat(),
                barColor = MaterialTheme.colorScheme.primary,
                overColor = ColorOverTarget,
                gridColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
            )
        }
    }
}

@Composable
private fun WeightCard(w: WeightInsight) {
    SectionCard(Icons.Filled.MonitorWeight, "Poids") {
        if (!w.hasEnoughData) {
            EmptyLine("Au moins deux pesées sur la période sont nécessaires pour voir la tendance.")
            return@SectionCard
        }
        val sign = if (w.deltaKg >= 0f) "+" else ""
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatBlock(
                label = "Variation",
                value = "$sign${"%.1f".format(w.deltaKg)}",
                sub = "kg",
                valueColor = verdictColor(w.verdict),
            )
            StatBlock(
                label = "Rythme",
                value = "${if (w.perWeekKg >= 0f) "+" else ""}${"%.1f".format(w.perWeekKg)}",
                sub = "kg / sem.",
                valueColor = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            verdictLabel(w.verdict),
            style = MaterialTheme.typography.bodySmall,
            color = verdictColor(w.verdict),
        )
    }
}

@Composable
private fun TrainingCard(t: TrainingInsight) {
    SectionCard(Icons.Filled.FitnessCenter, "Entraînement") {
        val thisWeek = if (t.weeklyTarget != null) "${t.sessionsThisWeek}/${t.weeklyTarget}" else "${t.sessionsThisWeek}"
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatBlock(
                label = "Cette semaine",
                value = thisWeek,
                sub = "séances",
                valueColor = if (t.weeklyTarget != null && t.sessionsThisWeek >= t.weeklyTarget)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            StatBlock(
                label = "Sur la période",
                value = "${t.sessionsInPeriod}",
                sub = "séances",
                valueColor = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (t.sessionsPerWeek.any { it > 0 }) {
            Text(
                "Séances / semaine (4 dernières)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BarChart(
                values = t.sessionsPerWeek.map { it.toFloat() },
                goal = t.weeklyTarget?.toFloat() ?: 0f,
                barColor = MaterialTheme.colorScheme.primary,
                overColor = MaterialTheme.colorScheme.primary,
                gridColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, sub: String, valueColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = valueColor)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Compact bar chart. Bars above [goal] (when goal > 0) use [overColor]. */
@Composable
private fun BarChart(
    values: List<Float>,
    goal: Float,
    barColor: Color,
    overColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxV = (values.maxOrNull() ?: 0f).coerceAtLeast(goal).coerceAtLeast(1f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val n = values.size
        if (n == 0) return@Canvas
        val gap = 4.dp.toPx()
        val barW = ((w - gap * (n - 1)) / n).coerceAtLeast(1f)

        // Goal line
        if (goal > 0f) {
            val gy = h * (1f - goal / maxV)
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, gy),
                end = androidx.compose.ui.geometry.Offset(w, gy),
                strokeWidth = 1.dp.toPx(),
            )
        }

        values.forEachIndexed { i, v ->
            val barH = (v / maxV) * h
            val x = i * (barW + gap)
            val over = goal > 0f && v > goal * 1.05f
            drawRect(
                color = if (over) overColor else barColor.copy(alpha = 0.85f),
                topLeft = androidx.compose.ui.geometry.Offset(x, h - barH),
                size = androidx.compose.ui.geometry.Size(barW, barH),
            )
        }
    }
}

@Composable
private fun verdictColor(v: WeightVerdict): Color = when (v) {
    WeightVerdict.ON_TRACK -> MaterialTheme.colorScheme.primary
    WeightVerdict.STALLED -> ColorOverTarget
    WeightVerdict.OPPOSITE -> MaterialTheme.colorScheme.error
    WeightVerdict.NEUTRAL -> MaterialTheme.colorScheme.onSurface
}

private fun verdictLabel(v: WeightVerdict): String = when (v) {
    WeightVerdict.ON_TRACK -> "En ligne avec ton objectif."
    WeightVerdict.STALLED -> "Stagnation sur la période."
    WeightVerdict.OPPOSITE -> "Direction opposée à ton objectif."
    WeightVerdict.NEUTRAL -> "Écart par rapport à ton maintien."
}
