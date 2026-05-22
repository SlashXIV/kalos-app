package com.kalos.app.feature.nutrition.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.FoodListItem
import com.kalos.app.core.ui.component.KalosSearchBar
import com.kalos.app.navigation.Screen
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    navController: NavController,
    mealType: String,
    date: String,
    viewModel: FoodSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.addedSuccessfully) {
        if (state.addedSuccessfully) navController.popBackStack()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un aliment") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(onClick = { navController.navigate(Screen.CustomFood.create()) }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Créer")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            KalosSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Rechercher un aliment…",
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                if (state.query.isEmpty()) {
                    if (state.recent.isNotEmpty()) {
                        item { SectionHeader("Récents") }
                        items(state.recent) { food ->
                            FoodListItem(food = food, onClick = { viewModel.selectFood(food) })
                            HorizontalDivider()
                        }
                    }
                    if (state.favorites.isNotEmpty()) {
                        item { SectionHeader("Favoris") }
                        items(state.favorites) { food ->
                            FoodListItem(food = food, onClick = { viewModel.selectFood(food) })
                            HorizontalDivider()
                        }
                    }
                    if (state.recent.isEmpty() && state.favorites.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Recherchez un aliment",
                                subtitle = "Tapez le nom d'un aliment pour commencer",
                            )
                        }
                    }
                } else {
                    if (state.isLoading) {
                        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                    } else if (state.results.isEmpty()) {
                        item {
                            EmptyState(
                                title = "Aucun résultat",
                                subtitle = "Essayez un autre terme ou créez un aliment personnalisé",
                            )
                        }
                    } else {
                        items(state.results) { food ->
                            FoodListItem(food = food, onClick = { viewModel.selectFood(food) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // Food detail bottom sheet
    if (state.selectedFood != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSheet,
            sheetState = sheetState,
        ) {
            FoodDetailSheet(
                food = state.selectedFood!!,
                amount = state.amountG,
                onAmountChange = viewModel::onAmountChange,
                servingMode = state.servingMode,
                servingCount = state.servingCount,
                onServingCountChange = viewModel::onServingCountChange,
                onServingModeChange = viewModel::onServingModeChange,
                onAdd = { viewModel.addToMeal(mealType, date) },
                onDismiss = viewModel::dismissSheet,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodDetailSheet(
    food: Food,
    amount: String,
    onAmountChange: (String) -> Unit,
    servingMode: ServingMode,
    servingCount: String,
    onServingCountChange: (String) -> Unit,
    onServingModeChange: (ServingMode) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hasUnitServing = food.servingUnit != "g"

    val amountFloat: Float = if (hasUnitServing && servingMode == ServingMode.UNITS) {
        (servingCount.toFloatOrNull() ?: 0f) * food.defaultServingG
    } else {
        amount.toFloatOrNull() ?: 0f
    }

    val kcal = food.kcalForAmount(amountFloat)
    val protein = food.proteinForAmount(amountFloat)
    val carbs = food.carbsForAmount(amountFloat)
    val fat = food.fatForAmount(amountFloat)

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(food.name, style = MaterialTheme.typography.headlineSmall)
        if (food.brand.isNotEmpty()) {
            Text(food.brand, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Unit/gram toggle (only for foods with a non-gram serving unit)
        if (hasUnitServing) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = servingMode == ServingMode.UNITS,
                    onClick = { onServingModeChange(ServingMode.UNITS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(food.servingUnit.replaceFirstChar { it.uppercaseChar() }) }
                SegmentedButton(
                    selected = servingMode == ServingMode.GRAMS,
                    onClick = { onServingModeChange(ServingMode.GRAMS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Grammes") }
            }
        }

        if (hasUnitServing && servingMode == ServingMode.UNITS) {
            OutlinedTextField(
                value = servingCount,
                onValueChange = onServingCountChange,
                label = { Text("Nombre de ${food.servingUnit}") },
                suffix = { Text("× ${food.defaultServingG.toInt()}g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        } else {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Quantité") },
                suffix = { Text("g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MacroStat("Calories", "${kcal.roundToInt()} kcal")
            MacroStat("Protéines", "${protein.roundToInt()}g")
            MacroStat("Glucides", "${carbs.roundToInt()}g")
            MacroStat("Lipides", "${fat.roundToInt()}g")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Annuler") }
            Button(
                onClick = onAdd,
                modifier = Modifier.weight(1f),
                enabled = amountFloat > 0,
            ) {
                Text("Ajouter")
            }
        }
    }
}

@Composable
private fun MacroStat(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
