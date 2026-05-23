package com.kalos.app.feature.nutrition.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique nutritionnel") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
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
