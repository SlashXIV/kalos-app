package com.kalos.app.feature.nutrition.history

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.database.dao.DailySummaryRow
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text("Historique nutritionnel") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (state.summaries.isNotEmpty()) {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(buildNutritionTsv(state.summaries)))
                            scope.launch { snackbarHostState.showSnackbar("Copié") }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copier l'historique")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.summaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Aucun historique",
                    subtitle = "Commencez à logger vos repas pour voir votre historique",
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.summaries, key = { it.date }) { summary ->
                    DaySummaryCard(
                        summary = summary,
                        onClick = { navController.navigate(Screen.NutritionDay.route(summary.date)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySummaryCard(summary: DailySummaryRow, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                formatHistoryDate(summary.date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(summary.totalKcal ?: 0f).roundToInt()} kcal", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("P: ${(summary.totalProtein ?: 0f).roundToInt()}g", style = MaterialTheme.typography.bodySmall)
                Text("G: ${(summary.totalCarbs ?: 0f).roundToInt()}g", style = MaterialTheme.typography.bodySmall)
                Text("L: ${(summary.totalFat ?: 0f).roundToInt()}g", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun buildNutritionTsv(summaries: List<DailySummaryRow>): String {
    val recent = summaries.take(14)
    val sb = StringBuilder()
    sb.appendLine("Kalos — Historique nutritionnel (14 jours)")
    sb.appendLine("Date\tKcal\tProtéines (g)\tGlucides (g)\tLipides (g)")
    for (s in recent) {
        val kcal = (s.totalKcal ?: 0f).roundToInt()
        val p    = (s.totalProtein ?: 0f).roundToInt()
        val c    = (s.totalCarbs ?: 0f).roundToInt()
        val f    = (s.totalFat ?: 0f).roundToInt()
        sb.appendLine("${s.date}\t$kcal\t$p\t$c\t$f")
    }
    if (recent.isNotEmpty()) {
        val n = recent.size.toDouble()
        val avgKcal = (recent.sumOf { (it.totalKcal  ?: 0f).toDouble() } / n).toFloat().roundToInt()
        val avgP    = (recent.sumOf { (it.totalProtein ?: 0f).toDouble() } / n).toFloat().roundToInt()
        val avgC    = (recent.sumOf { (it.totalCarbs  ?: 0f).toDouble() } / n).toFloat().roundToInt()
        val avgF    = (recent.sumOf { (it.totalFat    ?: 0f).toDouble() } / n).toFloat().roundToInt()
        val totKcal = recent.sumOf { (it.totalKcal  ?: 0f).toDouble() }.toFloat().roundToInt()
        val totP    = recent.sumOf { (it.totalProtein ?: 0f).toDouble() }.toFloat().roundToInt()
        val totC    = recent.sumOf { (it.totalCarbs  ?: 0f).toDouble() }.toFloat().roundToInt()
        val totF    = recent.sumOf { (it.totalFat    ?: 0f).toDouble() }.toFloat().roundToInt()
        sb.appendLine("Moyenne\t$avgKcal\t$avgP\t$avgC\t$avgF")
        sb.append("Total\t$totKcal\t$totP\t$totC\t$totF")
    }
    return sb.toString()
}

private fun formatHistoryDate(dateStr: String): String {
    val d = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    return when (d) {
        today -> "Aujourd'hui"
        today.minusDays(1) -> "Hier"
        else -> {
            val pattern = if (d.year == today.year) "EEEE d MMMM" else "EEEE d MMMM yyyy"
            d.format(DateTimeFormatter.ofPattern(pattern, Locale.FRENCH))
                .replaceFirstChar { it.uppercase() }
        }
    }
}
