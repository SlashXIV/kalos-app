package com.kalos.app.feature.insights

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.component.Dot
import com.kalos.app.core.ui.component.FitText
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.MeSeriesChart
import com.kalos.app.core.ui.component.MeasureRow
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.component.Swatch
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Zone d'objectif calorique, la même que pour « jours dans la cible » (InsightsViewModel). */
private const val ZONE_LOW = 0.90f
private const val ZONE_HIGH = 1.05f

@Composable
fun InsightsScreen(
    navController: NavController,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.me_insights_title),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
            PeriodSelector(state.period, viewModel::setPeriod)
            SummaryCard(headline = state.summaryHeadline, highlights = state.summaryHighlights)
            NutritionCard(state.nutrition)
            WeightCard(state.weight)
            TrainingCard(state.training)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(selected: InsightsPeriod, onSelect: (InsightsPeriod) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        InsightsPeriod.entries.forEachIndexed { i, period ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(i, InsightsPeriod.entries.size, KalosShape.control),
                icon = {},
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                FitText(
                    stringResource(if (period == InsightsPeriod.WEEK) R.string.me_period_week else R.string.me_period_month),
                    style = KalosTheme.type.metricSmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(headline: String, highlights: List<SummaryHighlight>) {
    if (headline.isBlank()) return
    KalosCard {
        InstrumentLabel(stringResource(R.string.me_insights_keep))
        Text(headline, style = MaterialTheme.typography.titleMedium)
        if (highlights.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                highlights.forEach { h ->
                    Row(
                        Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, KalosShape.chip)
                            .heightIn(min = 40.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(h.value, style = KalosTheme.type.metric)
                        Text(h.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NutritionCard(n: NutritionInsight) {
    KalosCard {
        SectionLabel(stringResource(R.string.me_insights_nutrition))
        if (n.daysWithData == 0) {
            EmptyLine(stringResource(R.string.me_insights_no_meals))
            return@KalosCard
        }
        val kcalUnit = stringResource(R.string.kcal_label)
        val kcalSpoken = stringResource(R.string.unit_kcal_spoken)
        val nutrients = KalosTheme.nutrients
        if (n.goalKcal > 0) {
            MeasureRow(
                label = stringResource(R.string.me_insights_avg_kcal),
                value = n.avgKcal.toFloat(), goal = n.goalKcal.toFloat(),
                valueText = formatGroupedInt(n.avgKcal), goalText = formatGroupedInt(n.goalKcal),
                unit = kcalUnit, spokenUnit = kcalSpoken, color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            StatBlock(stringResource(R.string.me_insights_avg_kcal), formatGroupedInt(n.avgKcal), kcalUnit, spokenUnit = kcalSpoken)
        }
        if (n.goalProtein > 0) {
            MeasureRow(
                label = stringResource(R.string.me_insights_avg_protein),
                value = n.avgProtein.toFloat(), goal = n.goalProtein.toFloat(),
                valueText = formatGroupedInt(n.avgProtein), goalText = formatGroupedInt(n.goalProtein),
                unit = stringResource(R.string.unit_g), spokenUnit = stringResource(R.string.unit_grams_spoken),
                color = nutrients.protein,
                // Dépasser l'objectif de protéines n'est pas un écart à signaler.
                overLimit = Float.MAX_VALUE,
            )
        }
        Text(
            pluralStringResource(R.plurals.me_insights_days_on_target, n.daysWithData, n.daysOnKcalTarget, n.daysWithData) +
                if (n.goalProtein > 0) "\n" + pluralStringResource(R.plurals.me_insights_days_protein, n.daysWithData, n.daysOnProteinTarget, n.daysWithData) else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (n.kcalPerDay.size >= 2) {
            val goal = n.goalKcal.toFloat()
            val readings = n.kcalPerDay.indices.map { i ->
                val k = n.kcalPerDay[i].roundToInt()
                buildList {
                    n.kcalDates.getOrNull(i)?.let { add(shortDate(it)) }
                    add(pluralStringResource(R.plurals.me_kcal_spoken, k, formatGroupedInt(k)))
                    if (goal > 0f) add(stringResource(R.string.me_pct_of_goal, (k / goal * 100).roundToInt()))
                }.joinToString(", ")
            }
            val overDays = if (goal > 0f) n.kcalPerDay.count { it > goal * ZONE_HIGH } else 0
            MeSeriesChart(
                values = n.kcalPerDay,
                reading = { readings[it] },
                summary = pluralStringResource(R.plurals.me_insights_kcal_chart, n.kcalPerDay.size, n.kcalPerDay.size),
                band = if (goal > 0f) goal * ZONE_LOW..goal * ZONE_HIGH else null,
                overAbove = if (goal > 0f) goal * ZONE_HIGH else Float.POSITIVE_INFINITY,
                ticks = if (goal > 0f) listOf(goal) else emptyList(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (goal > 0f) LegendItem(stringResource(R.string.me_insights_band), nutrients.goalReached.copy(alpha = 0.18f))
                if (overDays > 0) {
                    LegendItem(pluralStringResource(R.plurals.me_insights_over_days, overDays, overDays), nutrients.overTarget)
                }
            }
        }
    }
}

@Composable
private fun WeightCard(w: WeightInsight) {
    KalosCard {
        SectionLabel(stringResource(R.string.me_insights_weight))
        if (!w.hasEnoughData) {
            EmptyLine(stringResource(R.string.me_insights_weight_need))
            return@KalosCard
        }
        val kg = stringResource(R.string.kg)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatBlock(stringResource(R.string.me_insights_variation), signed(w.deltaKg), kg, Modifier.weight(1f), stringResource(R.string.me_unit_kg_spoken))
            StatBlock(
                stringResource(R.string.me_insights_pace), signed(w.perWeekKg),
                stringResource(R.string.me_unit_kg_per_week), Modifier.weight(1f), stringResource(R.string.me_unit_kg_per_week_spoken),
            )
        }
        val (label, color) = verdict(w.verdict)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dot(color)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TrainingCard(t: TrainingInsight) {
    KalosCard {
        SectionLabel(stringResource(R.string.me_insights_training))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val target = t.weeklyTarget
            if (target != null) {
                val value = stringResource(R.string.me_ratio, t.sessionsThisWeek, target)
                StatBlock(
                    stringResource(R.string.me_insights_this_week), value,
                    pluralStringResource(R.plurals.me_sessions_word, target),
                    spoken = stringResource(R.string.me_insights_this_week) + ", " +
                        pluralStringResource(R.plurals.me_sessions_of_target, target, t.sessionsThisWeek, target),
                    modifier = Modifier.weight(1f),
                )
            } else {
                StatBlock(
                    stringResource(R.string.me_insights_this_week), t.sessionsThisWeek.toString(),
                    pluralStringResource(R.plurals.me_sessions_word, t.sessionsThisWeek), modifier = Modifier.weight(1f),
                )
            }
            StatBlock(
                stringResource(R.string.me_insights_in_period), t.sessionsInPeriod.toString(),
                pluralStringResource(R.plurals.me_sessions_word, t.sessionsInPeriod), modifier = Modifier.weight(1f),
            )
        }
        if (t.sessionsPerWeek.any { it > 0 }) {
            val readings = t.sessionsPerWeek.indices.map { i ->
                val ago = t.sessionsPerWeek.lastIndex - i
                val week = if (ago == 0) stringResource(R.string.me_week_current) else pluralStringResource(R.plurals.me_weeks_ago, ago, ago)
                week + ", " + pluralStringResource(R.plurals.me_sessions, t.sessionsPerWeek[i], t.sessionsPerWeek[i])
            }
            val title = stringResource(R.string.me_insights_sessions_chart)
            InstrumentLabel(title)
            MeSeriesChart(
                values = t.sessionsPerWeek.map { it.toFloat() },
                reading = { readings[it] },
                summary = title,
                ticks = listOfNotNull(t.weeklyTarget?.toFloat()),
                height = 96.dp,
            )
        }
    }
}

/** Chiffre clé : libellé, valeur en chiffres tabulaires, unité ; une phrase pour TalkBack. */
@Composable
private fun StatBlock(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    spokenUnit: String = unit,
    spoken: String = stringResource(R.string.me_value_spoken, label, value, spokenUnit),
) {
    Column(modifier.clearAndSetSemantics { contentDescription = spoken }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FitText(value, style = KalosTheme.type.metricLarge)
            Text(unit, style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Swatch(color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun verdict(v: WeightVerdict): Pair<String, Color> = when (v) {
    WeightVerdict.ON_TRACK -> stringResource(R.string.me_verdict_on_track) to KalosTheme.nutrients.goalReached
    WeightVerdict.STALLED -> stringResource(R.string.me_verdict_stalled) to KalosTheme.nutrients.overTarget
    WeightVerdict.OPPOSITE -> stringResource(R.string.me_verdict_opposite) to KalosTheme.nutrients.overTarget
    WeightVerdict.NEUTRAL -> stringResource(R.string.me_verdict_neutral) to MaterialTheme.colorScheme.onSurfaceVariant
}

private fun signed(v: Float): String = (if (v >= 0f) "+" else "") + "%.1f".format(v)

private val SHORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)

private fun shortDate(date: String): String = LocalDate.parse(date).format(SHORT_DATE).replaceFirstChar { it.uppercase() }
