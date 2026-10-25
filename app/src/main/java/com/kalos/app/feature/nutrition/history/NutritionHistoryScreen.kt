package com.kalos.app.feature.nutrition.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.util.kcalSpoken
import com.kalos.app.core.ui.util.gramsSpoken
import com.kalos.app.core.ui.util.dayLabel
import com.kalos.app.core.domain.model.DailyTotals
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.Swatch
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import com.kalos.app.navigation.Screen
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun NutritionHistoryScreen(
    navController: NavController,
    viewModel: NutritionHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val copied = stringResource(R.string.food_history_copied)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = stringResource(R.string.food_history_title),
                    small = true,
                    navigation = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                ) {
                    if (state.summaries.isNotEmpty()) {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(buildNutritionTsv(state.summaries)))
                            scope.launch { snackbarHostState.showSnackbar(copied) }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.food_history_copy))
                        }
                    }
                }
            }
            if (state.summaries.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.food_history_empty_title),
                        subtitle = stringResource(R.string.food_history_empty_subtitle),
                    )
                }
            }
            items(state.summaries, key = { it.date }) { summary ->
                DaySummaryCard(
                    summary = summary,
                    onClick = { navController.navigate(Screen.NutritionDay.route(summary.date)) },
                )
            }
            if (state.canLoadMore) {
                item(key = "load_more") {
                    KalosButton(
                        stringResource(R.string.food_history_load_more),
                        onClick = viewModel::loadMore,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ghost = true,
                    )
                }
            }
        }
    }
}

/** Un jour : date, kcal en grand, grammes des macros avec leur pastille. Une phrase TalkBack. */
@Composable
private fun DaySummaryCard(summary: DailyTotals, onClick: () -> Unit) {
    val date = formatHistoryDate(summary.date)
    val kcal = formatGroupedInt(summary.totalKcal.roundToInt())
    val p = formatGroupedInt(summary.totalProtein.roundToInt())
    val c = formatGroupedInt(summary.totalCarbs.roundToInt())
    val f = formatGroupedInt(summary.totalFat.roundToInt())
    val spoken = stringResource(
        R.string.food_history_day_spoken, date, kcalSpoken(summary.totalKcal.roundToInt()),
        gramsSpoken(summary.totalProtein.roundToInt()), gramsSpoken(summary.totalCarbs.roundToInt()), gramsSpoken(summary.totalFat.roundToInt()),
    )
    val nutrients = KalosTheme.nutrients
    KalosCard(onClick = onClick) {
        Column(
            Modifier.clearAndSetSemantics { contentDescription = spoken },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            InstrumentLabel(date)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(kcal, style = KalosTheme.type.metricLarge)
                Text(
                    stringResource(R.string.kcal_label),
                    style = KalosTheme.type.metricSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(
                    R.string.food_macro_p to p to nutrients.protein,
                    R.string.food_macro_c to c to nutrients.carbs,
                    R.string.food_macro_f to f to nutrients.fat,
                ).forEach { (labelValue, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Swatch(color)
                        Text(stringResource(labelValue.first, labelValue.second), style = KalosTheme.type.metricSmall)
                    }
                }
            }
        }
    }
}

// texte-technique : export TSV pour le presse-papiers, en-têtes de colonnes en texte brut.
private fun buildNutritionTsv(summaries: List<DailyTotals>): String {
    val recent = summaries.take(14)
    val sb = StringBuilder()
    sb.appendLine("Kalos — Historique nutritionnel (14 jours)")
    sb.appendLine("Date\tKcal\tProtéines (g)\tGlucides (g)\tLipides (g)")
    for (s in recent) {
        val kcal = s.totalKcal.roundToInt()
        val p    = s.totalProtein.roundToInt()
        val c    = s.totalCarbs.roundToInt()
        val f    = s.totalFat.roundToInt()
        sb.appendLine("${s.date}\t$kcal\t$p\t$c\t$f")
    }
    if (recent.isNotEmpty()) {
        val n = recent.size.toDouble()
        val avgKcal = (recent.sumOf { (it.totalKcal  ?: 0f).toDouble() } / n).toFloat().roundToInt()
        val avgP    = (recent.sumOf { it.totalProtein.toDouble() } / n).toFloat().roundToInt()
        val avgC    = (recent.sumOf { (it.totalCarbs  ?: 0f).toDouble() } / n).toFloat().roundToInt()
        val avgF    = (recent.sumOf { (it.totalFat    ?: 0f).toDouble() } / n).toFloat().roundToInt()
        val totKcal = recent.sumOf { (it.totalKcal  ?: 0f).toDouble() }.toFloat().roundToInt()
        val totP    = recent.sumOf { it.totalProtein.toDouble() }.toFloat().roundToInt()
        val totC    = recent.sumOf { (it.totalCarbs  ?: 0f).toDouble() }.toFloat().roundToInt()
        val totF    = recent.sumOf { (it.totalFat    ?: 0f).toDouble() }.toFloat().roundToInt()
        sb.appendLine("Moyenne\t$avgKcal\t$avgP\t$avgC\t$avgF")
        sb.append("Total\t$totKcal\t$totP\t$totC\t$totF")
    }
    return sb.toString()
}

@Composable
private fun formatHistoryDate(dateStr: String): String {
    val d = LocalDate.parse(dateStr)
    val pattern = if (d.year == LocalDate.now().year) "EEEE d MMMM" else "EEEE d MMMM yyyy"
    return dayLabel(d, pattern).replaceFirstChar { it.uppercase() }
}
