package com.kalos.app.feature.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.Food
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    navController: NavController,
    initialDate: String? = null,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialDate) {
        if (initialDate != null) viewModel.setDate(initialDate)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

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
                title = {
                    Text(
                        if (!state.isToday) dateLabel else "Nutrition",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    if (initialDate != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                },
                actions = {
                    if (initialDate == null) {
                        IconButton(onClick = { navController.navigate(Screen.NutritionHistory.route) }) {
                            Icon(Icons.Filled.BarChart, contentDescription = "Historique")
                        }
                    }
                    // Copy lives in an overflow menu with an explicit label — a bare copy
                    // glyph next to the chart icon didn't communicate what gets copied.
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Plus d'options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copier le résumé du jour") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                clipboardManager.setText(AnnotatedString(buildDailySummary(state)))
                                scope.launch { snackbarHostState.showSnackbar("Résumé nutritionnel copié") }
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            // Hydration card
            item(key = "hydration") {
                HydrationCard(
                    waterMl = state.waterMl,
                    waterGoalMl = state.waterGoalMl,
                    progress = state.waterProgress,
                    isGoalReached = state.isWaterGoalReached,
                    displayTotal = state.waterDisplayTotal,
                    displayGoal = state.waterDisplayGoal,
                    isToday = state.isToday,
                    dateLabel = dateLabel,
                    isGoalEditable = state.isToday,
                    onAdd = viewModel::addWater,
                    onSetGoal = viewModel::setWaterGoal,
                )
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
                        onDeleteItems = viewModel::deleteItems,
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
    onDeleteItems: (List<Long>) -> Unit,
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
                consolidate(entry.items).forEach { consolidated ->
                    MealItemRow(
                        item = consolidated,
                        onDelete = { onDeleteItems(consolidated.itemIds) },
                    )
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

// ─── Hydration ────────────────────────────────────────────────────────────────

@Composable
private fun HydrationCard(
    waterMl: Int,
    waterGoalMl: Int,
    progress: Float,
    isGoalReached: Boolean,
    displayTotal: String,
    displayGoal: String,
    isToday: Boolean,
    dateLabel: String,
    isGoalEditable: Boolean,
    onAdd: (Int) -> Unit,
    onSetGoal: (Int) -> Unit,
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Filled.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isGoalReached) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    )
                    Text(
                        "Hydratation",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    if (isGoalReached) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Objectif atteint",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (!isToday) {
                        Text(
                            "· $dateLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isGoalEditable) {
                    IconButton(
                        onClick = { showGoalDialog = true },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Modifier l'objectif",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        displayTotal,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isGoalReached) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "/ $displayGoal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Quick-add buttons — target the currently displayed date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(250, 500, 750).forEach { ml ->
                    OutlinedButton(
                        onClick = { onAdd(ml) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    ) {
                        Text("+${ml}ml", style = MaterialTheme.typography.labelMedium)
                    }
                }
                // Same outlined treatment as the quick-add buttons: "Autre" is the least
                // frequent action, it must not be the most prominent one.
                OutlinedButton(
                    onClick = { showCustomDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                ) {
                    Text("Autre", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showGoalDialog) {
        WaterAmountDialog(
            title = "Objectif quotidien",
            initialValue = waterGoalMl.toString(),
            suffix = "ml",
            onConfirm = { v ->
                v.toIntOrNull()?.let { onSetGoal(it) }
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false },
        )
    }

    if (showCustomDialog) {
        WaterAmountDialog(
            title = "Quantité personnalisée",
            initialValue = "",
            suffix = "ml",
            hint = "Valeur négative pour corriger (ex : -250)",
            onConfirm = { v ->
                v.toIntOrNull()?.let { if (it != 0) onAdd(it) }
                showCustomDialog = false
            },
            onDismiss = { showCustomDialog = false },
        )
    }
}

@Composable
private fun WaterAmountDialog(
    title: String,
    initialValue: String,
    suffix: String,
    hint: String? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    suffix = { Text(suffix) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hint != null) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

// ─── Suggestions ──────────────────────────────────────────────────────────────

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

private fun buildDailySummary(state: NutritionUiState): String {
    val sb = StringBuilder()

    sb.appendLine("Date : ${state.date}")
    sb.appendLine()

    sb.appendLine("Calories : ${state.totalKcal.toInt()} / ${state.goal.kcal} kcal")
    sb.appendLine("Restant : ${(state.goal.kcal - state.totalKcal).toInt()} kcal")
    sb.appendLine()

    sb.appendLine("Protéines : ${state.totalProtein.toInt()} / ${state.goal.proteinG} g")
    sb.appendLine("Restant : ${(state.goal.proteinG - state.totalProtein).toInt()} g")
    sb.appendLine()

    sb.appendLine("Glucides : ${state.totalCarbs.toInt()} / ${state.goal.carbsG} g")
    sb.appendLine("Restant : ${(state.goal.carbsG - state.totalCarbs).toInt()} g")
    sb.appendLine()

    sb.appendLine("Lipides : ${state.totalFat.toInt()} / ${state.goal.fatG} g")
    sb.appendLine("Restant : ${(state.goal.fatG - state.totalFat).toInt()} g")
    sb.appendLine()

    sb.append("Hydratation : ${state.waterMl} / ${state.waterGoalMl} ml")
    if (state.isWaterGoalReached) {
        sb.appendLine("  ✓ Objectif atteint")
    } else {
        sb.appendLine()
        sb.appendLine("Restant : ${state.waterGoalMl - state.waterMl} ml")
    }

    val nonEmpty = state.meals.filter { it.items.isNotEmpty() }
    if (nonEmpty.isNotEmpty()) {
        nonEmpty.forEach { meal ->
            sb.appendLine()
            sb.appendLine("${meal.mealType.label} :")
            consolidate(meal.items).forEach { item ->
                sb.appendLine("- ${item.food.name} — ${item.totalAmountG.toInt()} g — ${item.totalKcal.toInt()} kcal")
            }
        }
    }

    return sb.toString().trimEnd()
}

private data class ConsolidatedItem(
    val food: Food,
    val totalAmountG: Float,
    val totalKcal: Float,
    val itemIds: List<Long>,
)

private fun consolidate(items: List<MealItem>): List<ConsolidatedItem> =
    items.groupBy { it.food.id }.values.map { group ->
        val first = group.first()
        ConsolidatedItem(
            food = first.food,
            totalAmountG = group.sumOf { it.amountG.toDouble() }.toFloat(),
            totalKcal = group.sumOf { it.kcal.toDouble() }.toFloat(),
            itemIds = group.map { it.id },
        )
    }

@Composable
private fun MealItemRow(item: ConsolidatedItem, onDelete: () -> Unit) {
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
                "${item.totalAmountG.toInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "${item.totalKcal.toInt()} kcal",
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
                // Muted, not red: with one delete affordance per row, red would dominate the
                // whole journal. Red stays reserved for confirmation dialogs.
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
