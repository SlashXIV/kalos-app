package com.kalos.app.feature.nutrition.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.FoodListItem
import com.kalos.app.core.ui.component.KalosNumberField
import com.kalos.app.core.ui.component.KalosSearchBar
import com.kalos.app.core.ui.util.color
import com.kalos.app.core.ui.util.foodSatietyLevel
import com.kalos.app.feature.nutrition.scan.SCANNED_BARCODE_KEY
import com.kalos.app.navigation.Screen
import kotlin.math.roundToInt

/** savedStateHandle key: id of the food picked in pick mode, read by the caller. */
const val PICKED_FOOD_ID_KEY = "picked_food_id"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    navController: NavController,
    mealType: String,
    date: String,
    pickForResult: Boolean = false,
    viewModel: FoodSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Pick mode (e.g. meal-template editor): tapping a food returns its id to the caller
    // via savedStateHandle and pops — no logging, no portion sheet.
    val onFoodClick: (Food) -> Unit = if (pickForResult) {
        { food ->
            navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_FOOD_ID_KEY, food.id)
            navController.popBackStack()
        }
    } else {
        { food -> viewModel.selectFood(food) }
    }

    LaunchedEffect(state.addedSuccessfully) {
        if (state.addedSuccessfully) {
            // Reset before popping so the screen isn't re-triggered if it's ever reused
            // (deep link, back navigation onto the same destination, etc.).
            viewModel.onAddHandled()
            navController.popBackStack()
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onErrorShown()
        }
    }

    // Barcode returned by the scanner via savedStateHandle → local lookup / manual create.
    val currentEntry = navController.currentBackStackEntry
    LaunchedEffect(currentEntry) {
        currentEntry?.savedStateHandle
            ?.getStateFlow<String?>(SCANNED_BARCODE_KEY, null)
            ?.collect { barcode ->
                if (barcode != null) {
                    viewModel.onBarcodeScanned(barcode)
                    currentEntry.savedStateHandle[SCANNED_BARCODE_KEY] = null
                }
            }
    }
    // Unknown barcode → open manual creation pre-filled with it (macros too if OFF resolved it).
    LaunchedEffect(state.createBarcode) {
        state.createBarcode?.let { bc ->
            viewModel.onCreateBarcodeHandled()
            navController.navigate(Screen.CustomFood.createWithBarcode(bc))
        }
    }

    if (state.isResolvingBarcode) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Recherche du produit") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Interrogation d'OpenFoodFacts…")
                }
            },
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (pickForResult) "Choisir un aliment" else "Ajouter un aliment") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.BarcodeScanner.route) }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scanner un code-barres")
                    }
                    IconButton(onClick = { navController.navigate(Screen.MyFoods.route) }) {
                        Icon(Icons.Filled.RestaurantMenu, contentDescription = "Mes aliments")
                    }
                    TextButton(onClick = { navController.navigate(Screen.CustomFood.create()) }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Créer")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            if (state.categories.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FoodFilterRow(
                    categories = state.categories,
                    selectedCategory = state.categoryFilter,
                    onlyCustom = state.onlyCustom,
                    onCategorySelect = viewModel::onCategorySelect,
                    onCustomToggle = viewModel::onCustomToggle,
                )
            }
            Spacer(Modifier.height(8.dp))

            val hasActiveFilter = state.categoryFilter.isNotEmpty() || state.onlyCustom
            // "Volume eating" sort — only where results are shown (search or filter active).
            if (state.query.isNotEmpty() || hasActiveFilter) {
                FilterChip(
                    selected = state.sortVolumeEating,
                    onClick = viewModel::onToggleVolumeSort,
                    label = { Text("Volume eating", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                Spacer(Modifier.height(8.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                if (state.query.isEmpty() && !hasActiveFilter) {
                    if (state.recent.isNotEmpty()) {
                        item { SectionHeader("Récents") }
                        items(state.recent) { food ->
                            FoodListItem(food = food, onClick = { onFoodClick(food) })
                            HorizontalDivider()
                        }
                    }
                    if (state.favorites.isNotEmpty()) {
                        item { SectionHeader("Favoris") }
                        items(state.favorites) { food ->
                            FoodListItem(food = food, onClick = { onFoodClick(food) })
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
                } else {  // query non-empty OR filter active
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
                        val displayed = if (state.sortVolumeEating) {
                            state.results.sortedBy { it.kcalPer100g }
                        } else {
                            state.results
                        }
                        items(displayed) { food ->
                            FoodListItem(food = food, onClick = { onFoodClick(food) })
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
                dailyKcal = state.dailyKcal,
                dailyProtein = state.dailyProtein,
                dailyCarbs = state.dailyCarbs,
                dailyFat = state.dailyFat,
                goalKcal = state.goalKcal,
                goalProtein = state.goalProtein,
                goalCarbs = state.goalCarbs,
                goalFat = state.goalFat,
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
    dailyKcal: Float = 0f,
    dailyProtein: Float = 0f,
    dailyCarbs: Float = 0f,
    dailyFat: Float = 0f,
    goalKcal: Float = 0f,
    goalProtein: Float = 0f,
    goalCarbs: Float = 0f,
    goalFat: Float = 0f,
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

        // Satiety (fullness-per-calorie) indicator — decision moment ("should I add this?").
        // Raw kcal/100 g shown alongside so the label never misleads on its own.
        val satietyLevel = foodSatietyLevel(food)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(satietyLevel.color()))
            Text(
                "${satietyLevel.label} · ${food.kcalPer100g.roundToInt()} kcal/100 g",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (hasUnitServing) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = servingMode == ServingMode.UNITS,
                    onClick = { onServingModeChange(ServingMode.UNITS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text(food.servingUnit.replaceFirstChar { it.uppercaseChar() }) }
                SegmentedButton(
                    selected = servingMode == ServingMode.GRAMS,
                    onClick = { onServingModeChange(ServingMode.GRAMS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("Grammes") }
            }
        }

        if (hasUnitServing && servingMode == ServingMode.UNITS) {
            KalosNumberField(
                value = servingCount,
                onValueChange = onServingCountChange,
                label = { Text("Nombre de ${food.servingUnit}") },
                suffix = { Text("× ${food.defaultServingG.toInt()}g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            KalosNumberField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Quantité") },
                suffix = { Text("g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        // This food's contribution
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MacroStat("Calories", "${kcal.roundToInt()} kcal")
            MacroStat("Protéines", "${protein.roundToInt()}g")
            MacroStat("Glucides", "${carbs.roundToInt()}g")
            MacroStat("Lipides", "${fat.roundToInt()}g")
        }

        // Projected totals preview (shown only when goals are set and there's a meaningful amount)
        if (goalKcal > 0f && amountFloat > 0f) {
            ProjectedNutritionStrip(
                projKcal = dailyKcal + kcal, projProtein = dailyProtein + protein,
                projCarbs = dailyCarbs + carbs, projFat = dailyFat + fat,
                goalKcal = goalKcal, goalProtein = goalProtein,
                goalCarbs = goalCarbs, goalFat = goalFat,
            )
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
private fun ProjectedNutritionStrip(
    projKcal: Float,
    projProtein: Float,
    projCarbs: Float,
    projFat: Float,
    goalKcal: Float,
    goalProtein: Float,
    goalCarbs: Float,
    goalFat: Float,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Après ajout",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ProjectedMacro(label = "kcal", projected = projKcal, goal = goalKcal)
                ProjectedMacro(label = "prot.", projected = projProtein, goal = goalProtein)
                ProjectedMacro(label = "gluc.", projected = projCarbs, goal = goalCarbs)
                ProjectedMacro(label = "lip.", projected = projFat, goal = goalFat)
            }
        }
    }
}

@Composable
private fun ProjectedMacro(label: String, projected: Float, goal: Float) {
    val ratio = if (goal > 0f) projected / goal else 0f
    val valueColor = when {
        ratio > 1.0f -> MaterialTheme.colorScheme.error
        ratio > 0.9f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${projected.roundToInt()}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor,
        )
        Text(
            "/ ${goal.roundToInt()} $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MacroStat(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FoodFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onlyCustom: Boolean,
    onCategorySelect: (String) -> Unit,
    onCustomToggle: () -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        item {
            FilterChip(
                selected = onlyCustom,
                onClick = onCustomToggle,
                label = { Text("Perso") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = onlyCustom,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelect(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCategory == category,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}
