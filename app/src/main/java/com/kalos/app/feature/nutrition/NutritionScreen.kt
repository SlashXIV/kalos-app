package com.kalos.app.feature.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.MealEntry
import com.kalos.app.core.domain.model.MealItem
import com.kalos.app.core.domain.model.MealType
import com.kalos.app.core.domain.usecase.FoodSuggestion
import com.kalos.app.core.ui.component.CalorieProgressRing
import com.kalos.app.core.ui.component.MacroTrioRow
import com.kalos.app.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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
                title = { Text("Nutrition", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.NutritionHistory.route) }) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Historique")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = paddingValues.calculateTopPadding() + 4.dp,
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Date navigation
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::goToPreviousDay) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Jour précédent")
                    }
                    TextButton(onClick = viewModel::goToToday) {
                        Text(
                            dateLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CalorieProgressRing(
                            consumed = state.totalKcal,
                            goal = state.goal.kcal,
                        )
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
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
                        onDeleteItem = viewModel::deleteItem,
                    )
                }
            }

            // Smart suggestions — only shown for today when at least one food logged
            if (state.suggestions.isNotEmpty()) {
                item(key = "suggestions") {
                    SuggestionsCard(
                        suggestions = state.suggestions,
                        onSuggestionClick = { suggestion ->
                            navController.navigate(
                                Screen.FoodSearch.routeWithQuery(
                                    MealType.SNACK.name,
                                    state.date,
                                    suggestion.food.name,
                                )
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
    onDeleteItem: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        mealType.label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (entry != null && entry.items.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${entry.totalKcal.toInt()} kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = onAddFood,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter", modifier = Modifier.size(18.dp))
                }
            }

            if (entry != null && entry.items.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(4.dp))
                entry.items.forEach { item ->
                    MealItemRow(item = item, onDelete = { onDeleteItem(item.id) })
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Rien ajouté",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun SuggestionsCard(
    suggestions: List<FoodSuggestion>,
    onSuggestionClick: (FoodSuggestion) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Suggestions",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Aliments qui complètent vos macros restantes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            suggestions.forEachIndexed { index, suggestion ->
                if (index > 0) HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                SuggestionRow(suggestion = suggestion, onClick = { onSuggestionClick(suggestion) })
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: FoodSuggestion, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                suggestion.food.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                "${suggestion.servingG.roundToInt()}g  ·  P ${suggestion.proteinG.roundToInt()}g  ·  G ${suggestion.carbsG.roundToInt()}g  ·  L ${suggestion.fatG.roundToInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${suggestion.kcal.roundToInt()} kcal",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(4.dp))
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Ajouter", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MealItemRow(item: MealItem, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.food.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${item.amountG.toInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "${item.kcal.toInt()} kcal",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Supprimer",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
