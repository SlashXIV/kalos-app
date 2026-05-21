package com.kalos.app.feature.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.MealEntry
import com.kalos.app.core.domain.model.MealType
import com.kalos.app.core.ui.component.CalorieProgressRing
import com.kalos.app.core.ui.component.MacroTrioRow
import com.kalos.app.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    navController: NavController,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val dateLabel = remember(state.date) {
        val d = LocalDate.parse(state.date)
        val today = LocalDate.now()
        when (d) {
            today -> "Aujourd'hui"
            today.minusDays(1) -> "Hier"
            else -> d.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH))
                .replaceFirstChar { it.uppercase() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.NutritionHistory.route) }) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Historique")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Date navigation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::goToPreviousDay) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Jour précédent")
                    }
                    TextButton(onClick = viewModel::goToToday) {
                        Text(dateLabel, style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(
                        onClick = viewModel::goToNextDay,
                        enabled = state.date < LocalDate.now().toString(),
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Jour suivant")
                    }
                }
            }

            // Summary card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CalorieProgressRing(
                            consumed = state.totalKcal,
                            goal = state.goal.kcal,
                            size = 120.dp,
                            strokeWidth = 12.dp,
                        )
                        Spacer(Modifier.height(16.dp))
                        MacroTrioRow(
                            proteinConsumed = state.totalProtein, proteinGoal = state.goal.proteinG,
                            carbsConsumed = state.totalCarbs, carbsGoal = state.goal.carbsG,
                            fatConsumed = state.totalFat, fatGoal = state.goal.fatG,
                        )
                    }
                }
            }

            // Meal sections
            MealType.entries.forEach { mealType ->
                item(key = mealType.name) {
                    MealSection(
                        mealType = mealType,
                        entry = state.meals.firstOrNull { it.mealType == mealType },
                        onAddFood = {
                            navController.navigate(
                                Screen.FoodSearch.route(mealType.name, state.date)
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MealSection(
    mealType: MealType,
    entry: MealEntry?,
    onAddFood: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(mealType.label, style = MaterialTheme.typography.titleSmall)
                    if (entry != null && entry.items.isNotEmpty()) {
                        Text(
                            "${entry.totalKcal.toInt()} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                FilledTonalIconButton(onClick = onAddFood, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter", modifier = Modifier.size(18.dp))
                }
            }

            if (entry != null && entry.items.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                entry.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${item.food.name} — ${item.amountG.toInt()}g",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${item.kcal.toInt()} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
