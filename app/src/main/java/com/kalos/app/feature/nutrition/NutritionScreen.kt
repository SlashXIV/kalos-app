package com.kalos.app.feature.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.domain.model.Food
import com.kalos.app.core.domain.model.MealEntry
import com.kalos.app.core.domain.model.MealItem
import com.kalos.app.core.domain.model.MealType
import com.kalos.app.core.ui.component.FitText
import com.kalos.app.core.ui.component.GraduatedDial
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.KalosNumberField
import com.kalos.app.core.ui.component.MeasureRow
import com.kalos.app.core.ui.component.MeasureTicket
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.TicketLine
import com.kalos.app.core.ui.component.ValueChip
import com.kalos.app.core.ui.motion.rememberHaptics
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import com.kalos.app.navigation.Screen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(
    navController: NavController,
    initialDate: String? = null,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val todayWorkout by viewModel.todayWorkout.collectAsStateWithLifecycle()
    val templates by viewModel.mealTemplates.collectAsStateWithLifecycle()

    LaunchedEffect(initialDate) {
        if (initialDate != null) viewModel.setDate(initialDate)
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val haptics = rememberHaptics()
    fun snack(text: String) { scope.launch { snackbarHostState.showSnackbar(text) } }

    var applyForMeal by remember { mutableStateOf<MealType?>(null) }
    var saveForMeal by remember { mutableStateOf<MealType?>(null) }

    applyForMeal?.let { mealType ->
        AlertDialog(
            onDismissRequest = { applyForMeal = null },
            title = { Text(stringResource(R.string.favorite_apply_title)) },
            text = {
                if (templates.isEmpty()) {
                    Text(stringResource(R.string.favorite_apply_empty))
                } else {
                    Column {
                        templates.forEach { t ->
                            ListItem(
                                headlineContent = { Text(t.name) },
                                supportingContent = {
                                    Text(
                                        pluralStringResource(
                                            R.plurals.favorite_summary, t.items.size, t.items.size,
                                            formatGroupedInt(t.totalKcal.roundToInt()),
                                        ),
                                        style = KalosTheme.type.metricSmall,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.applyTemplate(mealType, t.id)
                                    applyForMeal = null
                                    haptics.tick()
                                    snack(context.getString(R.string.favorite_added, t.name))
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { applyForMeal = null }) { Text(stringResource(R.string.action_close)) } },
        )
    }

    saveForMeal?.let { mealType ->
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { saveForMeal = null },
            title = { Text(stringResource(R.string.favorite_save_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.favorite_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        viewModel.saveMealAsFavorite(mealType, name)
                        saveForMeal = null
                        snack(context.getString(R.string.favorite_saved))
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { saveForMeal = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    val date = remember(state.date) { LocalDate.parse(state.date) }
    val today = LocalDate.now()
    val title = when (date) {
        today -> stringResource(R.string.today_title)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH))
    }
    val eyebrow = remember(date) { date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = title,
                    eyebrow = eyebrow,
                    navigation = if (initialDate != null) ({
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }) else null,
                ) {
                    IconButton(onClick = viewModel::goToPreviousDay) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.day_previous))
                    }
                    if (!state.isToday) {
                        IconButton(onClick = viewModel::goToNextDay) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.day_next))
                        }
                    }
                    if (initialDate == null) {
                        IconButton(onClick = { navController.navigate(Screen.NutritionHistory.route) }) {
                            Icon(Icons.Filled.BarChart, contentDescription = stringResource(R.string.action_history))
                        }
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (!state.isToday) DropdownMenuItem(
                            text = { Text(stringResource(R.string.day_back_to_today)) },
                            onClick = { menuExpanded = false; viewModel.goToToday() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.summary_copy)) },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                clipboardManager.setText(AnnotatedString(buildDailySummary(state)))
                                snack(context.getString(R.string.summary_copied))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.favorites_manage)) },
                            leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
                            onClick = { menuExpanded = false; navController.navigate(Screen.MealTemplates.route) },
                        )
                    }
                }
            }

            item(key = "summary") { DaySummary(state) }

            item(key = "hydration") {
                HydrationCard(
                    waterMl = state.waterMl,
                    waterGoalMl = state.waterGoalMl,
                    isGoalEditable = state.isToday,
                    onAdd = { haptics.tick(); viewModel.addWater(it) },
                    onSetGoal = viewModel::setWaterGoal,
                )
            }

            MealType.entries.forEach { mealType ->
                item(key = mealType.name) {
                    val entry = state.meals.firstOrNull { it.mealType == mealType }
                    val hasItems = entry?.items?.isNotEmpty() == true
                    MealTicket(
                        mealType = mealType,
                        entry = entry,
                        onAddFood = { navController.navigate(Screen.FoodSearch.route(mealType.name, state.date)) },
                        onDeleteItems = viewModel::deleteItems,
                        onApplyFavorite = { applyForMeal = mealType },
                        onSaveAsFavorite = if (hasItems) ({ saveForMeal = mealType }) else null,
                    )
                }
            }

            val workout = todayWorkout
            if (state.isToday && workout != null) item(key = "workout") {
                KalosCard {
                    InstrumentLabel(stringResource(R.string.today_workout))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(workout.template.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                pluralStringResource(
                                    R.plurals.workout_exercise_count, workout.template.exerciseCount,
                                    workout.template.exerciseCount, workout.template.estimatedDurationMin,
                                ),
                                style = KalosTheme.type.metricSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (workout.done) {
                            InstrumentLabel(stringResource(R.string.workout_done_today), color = KalosTheme.nutrients.goalReached)
                        } else {
                            KalosButton(
                                stringResource(R.string.workout_start_action),
                                onClick = { navController.navigate(Screen.ActiveWorkout.route(workout.template.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Le cadran du jour et les trois règles des macros. */
@Composable
private fun DaySummary(state: NutritionUiState) {
    val goal = state.goal.kcal
    val kcal = state.totalKcal.roundToInt()
    val rest = goal - kcal
    val over = goal > 0 && state.totalKcal > goal * 1.05f
    val status = if (rest >= 0) stringResource(R.string.kcal_remaining, formatGroupedInt(rest))
                 else stringResource(R.string.kcal_over, formatGroupedInt(abs(rest)))
    val nutrients = KalosTheme.nutrients
    KalosCard(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 16.dp)) {
        GraduatedDial(
            value = state.totalKcal,
            goal = goal.toFloat(),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            val sentence = stringResource(R.string.kcal_dial_sentence, formatGroupedInt(kcal), formatGroupedInt(goal), status)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clearAndSetSemantics { contentDescription = sentence },
            ) {
                InstrumentLabel(stringResource(R.string.kcal_label))
                FitText(formatGroupedInt(kcal), style = KalosTheme.type.metricHuge)
                Text(
                    stringResource(R.string.kcal_of, formatGroupedInt(goal)),
                    style = KalosTheme.type.metricSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    status,
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        over -> nutrients.overTarget
                        goal > 0 && state.totalKcal >= goal * 0.9f -> nutrients.goalReached
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
        val grams = stringResource(R.string.unit_grams_spoken)
        val g = stringResource(R.string.unit_g)
        listOf(
            Triple(R.string.nutrition_protein, state.totalProtein to state.goal.proteinG, nutrients.protein),
            Triple(R.string.nutrition_carbs, state.totalCarbs to state.goal.carbsG, nutrients.carbs),
            Triple(R.string.nutrition_fat, state.totalFat to state.goal.fatG, nutrients.fat),
        ).forEach { (label, values, color) ->
            MeasureRow(
                label = stringResource(label),
                value = values.first,
                goal = values.second.toFloat(),
                valueText = formatGroupedInt(values.first.roundToInt()),
                goalText = formatGroupedInt(values.second),
                unit = g,
                spokenUnit = grams,
                color = color,
            )
        }
    }
}

@Composable
private fun MealTicket(
    mealType: MealType,
    entry: MealEntry?,
    onAddFood: () -> Unit,
    onDeleteItems: (List<Long>) -> Unit,
    onApplyFavorite: () -> Unit,
    onSaveAsFavorite: (() -> Unit)?,
) {
    MeasureTicket {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(mealType.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (entry != null && entry.items.isNotEmpty()) {
                Text(
                    "${formatGroupedInt(entry.totalKcal.roundToInt())} ${stringResource(R.string.kcal_label)}",
                    style = KalosTheme.type.metricSmall,
                )
            }
            var menuExpanded by remember { mutableStateOf(false) }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.meal_options, mealType.label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.favorite_apply_title)) },
                    leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    onClick = { menuExpanded = false; onApplyFavorite() },
                )
                if (onSaveAsFavorite != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.favorite_save_title)) },
                        leadingIcon = { Icon(Icons.Filled.BookmarkAdd, contentDescription = null) },
                        onClick = { menuExpanded = false; onSaveAsFavorite() },
                    )
                }
            }
        }
        val lines = remember(entry?.items) { entry?.items?.let(::consolidate).orEmpty() }
        if (lines.isNotEmpty()) {
            lines.forEach { item ->
                val amount = formatGroupedInt(item.totalAmountG.roundToInt())
                val kcal = formatGroupedInt(item.totalKcal.roundToInt())
                TicketLine(
                    name = item.food.name,
                    amount = "$amount ${stringResource(R.string.unit_g)}",
                    value = kcal,
                    spoken = stringResource(R.string.meal_item_spoken, item.food.name, amount, kcal),
                ) {
                    IconButton(onClick = { onDeleteItems(item.itemIds) }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.meal_item_remove, item.food.name),
                            // Discret, pas rouge : une croix par ligne dominerait le journal.
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        } else {
            Text(
                stringResource(R.string.meal_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val addLabel = stringResource(R.string.meal_add_food, mealType.label)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onAddFood)
                .clearAndSetSemantics {
                    contentDescription = addLabel
                    role = Role.Button
                    onClick { onAddFood(); true }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.nutrition_add_food),
                style = KalosTheme.type.action,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ─── Eau ──────────────────────────────────────────────────────────────────────

@Composable
private fun HydrationCard(
    waterMl: Int,
    waterGoalMl: Int,
    isGoalEditable: Boolean,
    onAdd: (Int) -> Unit,
    onSetGoal: (Int) -> Unit,
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    val litres = { ml: Int -> "%.2f".format(Locale.FRENCH, ml / 1000f).trimEnd('0').trimEnd(',').ifEmpty { "0" } }

    KalosCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                MeasureRow(
                    label = stringResource(R.string.water_title),
                    value = waterMl.toFloat(),
                    goal = waterGoalMl.toFloat(),
                    valueText = litres(waterMl),
                    goalText = litres(waterGoalMl),
                    unit = stringResource(R.string.unit_l),
                    spokenUnit = stringResource(R.string.unit_litres_spoken),
                    color = KalosTheme.nutrients.water,
                    // Boire plus que l'objectif n'est pas un dépassement à signaler.
                    overLimit = Float.MAX_VALUE,
                )
            }
            if (isGoalEditable) {
                IconButton(onClick = { showGoalDialog = true }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.water_goal_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(250, 500, 750).forEach { ml ->
                ValueChip(
                    text = stringResource(R.string.water_quick_add, ml),
                    onClick = { onAdd(ml) },
                    contentDescription = pluralStringResource(R.plurals.water_quick_add_spoken, ml, ml),
                    modifier = Modifier.weight(1f),
                )
            }
            ValueChip(
                text = stringResource(R.string.water_other),
                onClick = { showCustomDialog = true },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showGoalDialog) {
        WaterAmountDialog(
            title = stringResource(R.string.water_goal_dialog),
            initialValue = waterGoalMl.toString(),
            onConfirm = { v ->
                v.toIntOrNull()?.let { onSetGoal(it) }
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false },
        )
    }

    if (showCustomDialog) {
        WaterAmountDialog(
            title = stringResource(R.string.water_custom_dialog),
            initialValue = "",
            hint = stringResource(R.string.water_custom_hint),
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
                KalosNumberField(
                    value = value,
                    onValueChange = { value = it },
                    suffix = { Text(stringResource(R.string.unit_ml)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hint != null) {
                    Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text(stringResource(R.string.action_validate)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

// texte-technique : résumé copié dans le presse-papiers, au format texte brut.
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
