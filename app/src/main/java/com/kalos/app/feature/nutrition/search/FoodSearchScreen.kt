package com.kalos.app.feature.nutrition.search

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.ui.component.KalosFilterChip
import com.kalos.app.core.ui.component.Dot
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.FoodListItem
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosNumberField
import com.kalos.app.core.ui.component.KalosSearchBar
import com.kalos.app.core.ui.component.MeasureRow
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.component.Swatch
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.color
import com.kalos.app.core.ui.util.foodSatietyLevel
import com.kalos.app.core.ui.util.formatGroupedInt
import com.kalos.app.feature.nutrition.scan.SCANNED_BARCODE_KEY
import com.kalos.app.navigation.Screen
import kotlin.math.roundToInt
import com.kalos.app.core.ui.util.parseDecimal

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
            title = { Text(stringResource(R.string.food_resolving_title)) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.food_resolving_body))
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        // En-tête et recherche fixes au-dessus des résultats : le champ garde le focus quand la liste défile.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            ScreenHeader(
                title = stringResource(if (pickForResult) R.string.food_search_title_pick else R.string.food_search_title_add),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            ) {
                IconButton(onClick = { navController.navigate(Screen.BarcodeScanner.route) }) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.food_scan_action))
                }
                IconButton(onClick = { navController.navigate(Screen.MyFoods.route) }) {
                    Icon(Icons.Filled.RestaurantMenu, contentDescription = stringResource(R.string.food_my_foods))
                }
                IconButton(onClick = { navController.navigate(Screen.CustomFood.create()) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.food_create_action))
                }
            }
            Spacer(Modifier.height(8.dp))
            KalosSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.food_search_placeholder),
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

            val hasActiveFilter = state.categoryFilter.isNotEmpty() || state.onlyCustom
            // "Volume eating" sort — only where results are shown (search or filter active).
            if (state.query.isNotEmpty() || hasActiveFilter) {
                KalosFilterChip(
                    selected = state.sortVolumeEating,
                    onClick = viewModel::onToggleVolumeSort,
                    text = stringResource(R.string.food_volume_eating),
                )
            }
            Spacer(Modifier.height(4.dp))
            val displayed = remember(state.results, state.sortVolumeEating) {
                if (state.sortVolumeEating) state.results.sortedBy { it.kcalPer100g } else state.results
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                if (state.query.isEmpty() && !hasActiveFilter) {
                    if (state.recent.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.food_section_recent)) }
                        items(state.recent, key = { "recent-${it.id}" }) { food ->
                            FoodListItem(food = food, onClick = { onFoodClick(food) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    if (state.favorites.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.food_section_favorites)) }
                        items(state.favorites, key = { "fav-${it.id}" }) { food ->
                            FoodListItem(food = food, onClick = { onFoodClick(food) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    if (state.recent.isEmpty() && state.favorites.isEmpty()) {
                        item {
                            EmptyState(
                                title = stringResource(R.string.food_search_empty_title),
                                subtitle = stringResource(R.string.food_search_empty_subtitle),
                            )
                        }
                    }
                } else {  // query non-empty OR filter active
                    if (state.isLoading) {
                        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                    } else if (state.results.isEmpty()) {
                        item {
                            EmptyState(
                                title = stringResource(R.string.food_no_result),
                                subtitle = stringResource(R.string.food_no_result_subtitle),
                            )
                        }
                    } else {
                        items(displayed, key = { "res-${it.id}" }) { food ->
                            FoodListItem(food = food, onClick = { onFoodClick(food) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }

    // Food detail bottom sheet. The sheet state is created here (inside the conditional)
    // so each open gets a fresh state — reusing a hoisted state across open/dismiss cycles
    // left it stuck "hidden", so the sheet stopped re-opening until the screen was recreated.
    if (state.selectedFood != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSheet,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
    SectionLabel(title, Modifier.padding(top = 12.dp, bottom = 4.dp))
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
        (servingCount.parseDecimal() ?: 0f) * food.defaultServingG
    } else {
        amount.parseDecimal() ?: 0f
    }

    val kcal = food.kcalForAmount(amountFloat)
    val protein = food.proteinForAmount(amountFloat)
    val carbs = food.carbsForAmount(amountFloat)
    val fat = food.fatForAmount(amountFloat)
    val nutrients = KalosTheme.nutrients
    val g = stringResource(R.string.unit_g)
    val grams = stringResource(R.string.unit_grams_spoken)

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (food.brand.isNotEmpty()) InstrumentLabel(food.brand)
            Text(
                food.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }

        // Satiety (fullness-per-calorie) indicator — decision moment ("should I add this?").
        // Raw kcal/100 g shown alongside so the label never misleads on its own.
        val satietyLevel = foodSatietyLevel(food)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Dot(satietyLevel.color())
            Text(
                stringResource(R.string.food_satiety_line, satietyLevel.label, formatGroupedInt(food.kcalPer100g.roundToInt())),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (hasUnitServing) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = servingMode == ServingMode.UNITS,
                    onClick = { onServingModeChange(ServingMode.UNITS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2, baseShape = KalosShape.control),
                ) { Text(food.servingUnit.replaceFirstChar { it.uppercaseChar() }) }
                SegmentedButton(
                    selected = servingMode == ServingMode.GRAMS,
                    onClick = { onServingModeChange(ServingMode.GRAMS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2, baseShape = KalosShape.control),
                ) { Text(stringResource(R.string.food_unit_grams_mode)) }
            }
        }

        if (hasUnitServing && servingMode == ServingMode.UNITS) {
            KalosNumberField(
                value = servingCount,
                onValueChange = onServingCountChange,
                label = { Text(stringResource(R.string.food_serving_count, food.servingUnit)) },
                suffix = { Text(stringResource(R.string.food_serving_suffix, food.defaultServingG.roundToInt().toString())) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            KalosNumberField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.food_quantity)) },
                suffix = { Text(g) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        // This food's contribution, in tabular digits.
        InstrumentLabel(stringResource(R.string.food_portion_label))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MacroStat(
                stringResource(R.string.nutrition_calories), formatGroupedInt(kcal.roundToInt()),
                stringResource(R.string.kcal_label), stringResource(R.string.unit_kcal_spoken), null,
            )
            MacroStat(stringResource(R.string.nutrition_protein), protein.roundToInt().toString(), g, grams, nutrients.protein)
            MacroStat(stringResource(R.string.nutrition_carbs), carbs.roundToInt().toString(), g, grams, nutrients.carbs)
            MacroStat(stringResource(R.string.nutrition_fat), fat.roundToInt().toString(), g, grams, nutrients.fat)
        }

        // Projected totals preview (shown only when goals are set and there's a meaningful amount)
        if (goalKcal > 0f && amountFloat > 0f) {
            SectionLabel(stringResource(R.string.food_after_add))
            MeasureRow(
                label = stringResource(R.string.nutrition_calories),
                value = dailyKcal + kcal, goal = goalKcal,
                valueText = formatGroupedInt((dailyKcal + kcal).roundToInt()), goalText = formatGroupedInt(goalKcal.roundToInt()),
                unit = stringResource(R.string.kcal_label), spokenUnit = stringResource(R.string.unit_kcal_spoken),
                color = MaterialTheme.colorScheme.primary,
            )
            listOf(
                Triple(R.string.nutrition_protein, (dailyProtein + protein) to goalProtein, nutrients.protein),
                Triple(R.string.nutrition_carbs, (dailyCarbs + carbs) to goalCarbs, nutrients.carbs),
                Triple(R.string.nutrition_fat, (dailyFat + fat) to goalFat, nutrients.fat),
            ).forEach { (label, values, color) ->
                MeasureRow(
                    label = stringResource(label),
                    value = values.first, goal = values.second,
                    valueText = formatGroupedInt(values.first.roundToInt()), goalText = formatGroupedInt(values.second.roundToInt()),
                    unit = g, spokenUnit = grams, color = color,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KalosButton(stringResource(R.string.action_cancel), onClick = onDismiss, modifier = Modifier.weight(1f), ghost = true)
            KalosButton(stringResource(R.string.food_add), onClick = onAdd, modifier = Modifier.weight(1f), enabled = amountFloat > 0)
        }
    }
}

/** Une valeur de la portion : chiffres tabulaires, libellé (avec sa pastille de macro), une phrase TalkBack. */
@Composable
private fun MacroStat(label: String, value: String, unit: String, spokenUnit: String, color: Color?) {
    val spoken = stringResource(R.string.food_stat_spoken, label, value, spokenUnit)
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
    ) {
        Text("$value $unit", style = KalosTheme.type.metric)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (color != null) Swatch(color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            KalosFilterChip(text = stringResource(R.string.food_filter_custom), selected = onlyCustom, onClick = onCustomToggle)
        }
        items(categories) { category ->
            KalosFilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelect(category) },
                text = category,
            )
        }
    }
}
