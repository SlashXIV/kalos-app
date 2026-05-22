package com.kalos.app.feature.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.kalos.app.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dayDetail by viewModel.dayDetail.collectAsStateWithLifecycle()
    val insights by viewModel.insightsState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendrier", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Month navigation ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(Icons.Filled.ChevronLeft, "Mois précédent")
                }
                TextButton(onClick = viewModel::today) {
                    val monthName = state.month.month
                        .getDisplayName(TextStyle.FULL, Locale.FRENCH)
                        .replaceFirstChar { it.uppercase() }
                    Text(
                        "$monthName ${state.month.year}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(Icons.Filled.ChevronRight, "Mois suivant")
                }
            }

            // ── Day-of-week headers ───────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("L", "M", "M", "J", "V", "S", "D").forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Calendar grid ─────────────────────────────────────────────────
            CalendarGrid(
                state = state,
                selectedDate = selectedDate,
                onDateClick = viewModel::selectDate,
            )

            // ── Legend ────────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(color = MaterialTheme.colorScheme.primary, label = "Nutrition")
                LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Sport réalisé")
                LegendItem(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f), label = "Planifié")
            }

            // ── Day detail card ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = selectedDate != null,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                if (selectedDate != null) {
                    val planned = run {
                        val dow = LocalDate.parse(selectedDate!!).dayOfWeek.value
                        if (selectedDate in state.plannedWorkoutDates)
                            state.activeProgram?.workouts?.firstOrNull { it.dayOfWeek == dow }
                        else null
                    }
                    DayDetailCard(
                        selectedDate = selectedDate!!,
                        detail = dayDetail,
                        plannedWorkoutName = planned?.template?.name,
                        activeProgramName = if (planned != null) state.activeProgram?.name else null,
                        onNavigateToNutrition = {
                            navController.navigate(Screen.Nutrition.route)
                        },
                        isToday = selectedDate == LocalDate.now().toString(),
                    )
                }
            }

            HorizontalDivider()

            // ── Insights ──────────────────────────────────────────────────────
            InsightsSection(insights = insights)

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Calendar grid (non-lazy, works in scrollable Column) ─────────────────────

@Composable
private fun CalendarGrid(
    state: CalendarUiState,
    selectedDate: String?,
    onDateClick: (String) -> Unit,
) {
    val month = state.month
    val firstDay = month.atDay(1)
    val dayOfWeekOffset = firstDay.dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()
    val today = LocalDate.now()

    val cells = remember(month) {
        buildList<Int?> {
            repeat(dayOfWeekOffset) { add(null) }
            for (d in 1..daysInMonth) add(d)
            while (size % 7 != 0) add(null)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (day == null) {
                            Box(modifier = Modifier.aspectRatio(1f))
                        } else {
                            val date = month.atDay(day)
                            val dateStr = date.toString()
                            DayCell(
                                day = day,
                                isToday = date == today,
                                isSelected = dateStr == selectedDate,
                                hasNutrition = dateStr in state.nutritionDates,
                                hasWorkout = dateStr in state.workoutDates,
                                hasPlanned = dateStr in state.plannedWorkoutDates,
                                isFuture = date.isAfter(today),
                                onClick = { onDateClick(dateStr) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasNutrition: Boolean,
    hasWorkout: Boolean,
    hasPlanned: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(
                when {
                    isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                    isToday -> Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    else -> Modifier
                }
            )
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            if (hasNutrition || hasWorkout || hasPlanned) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (hasNutrition) {
                        Box(
                            Modifier.size(4.dp).clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                    }
                    if (hasWorkout) {
                        Box(
                            Modifier.size(4.dp).clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.tertiary
                                )
                        )
                    } else if (hasPlanned) {
                        Box(
                            Modifier.size(4.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }
    }
}

// ─── Day detail card ──────────────────────────────────────────────────────────

@Composable
private fun DayDetailCard(
    selectedDate: String,
    detail: DayDetail?,
    plannedWorkoutName: String?,
    activeProgramName: String?,
    isToday: Boolean,
    onNavigateToNutrition: () -> Unit,
) {
    val dateLabel = remember(selectedDate) {
        val d = LocalDate.parse(selectedDate)
        val today = LocalDate.now()
        when (d) {
            today -> "Aujourd'hui"
            today.minusDays(1) -> "Hier"
            else -> d.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
                .replaceFirstChar { it.uppercase() }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                if (isToday) {
                    TextButton(
                        onClick = onNavigateToNutrition,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text("Voir nutrition", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            val hasAnything = (detail?.totalKcal ?: 0f) > 0f ||
                    (detail?.waterMl ?: 0) > 0 ||
                    detail?.workoutLogs?.isNotEmpty() == true ||
                    plannedWorkoutName != null

            if (!hasAnything) {
                Text(
                    "Rien d'enregistré ce jour-là.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // Nutrition
                if ((detail?.totalKcal ?: 0f) > 0f || (detail?.totalProtein ?: 0f) > 0f) {
                    DetailSection(icon = Icons.Filled.LocalFireDepartment, title = "Nutrition") {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            DetailPill(label = "Calories", value = "${detail!!.totalKcal.toInt()} kcal")
                            DetailPill(label = "Protéines", value = "${detail.totalProtein.toInt()} g")
                        }
                    }
                }

                // Hydratation
                if ((detail?.waterMl ?: 0) > 0) {
                    DetailSection(icon = Icons.Filled.WaterDrop, title = "Hydratation") {
                        DetailPill(label = "Eau bue", value = "${detail!!.waterMl} ml")
                    }
                }

                // Workouts réalisés
                if (detail?.workoutLogs?.isNotEmpty() == true) {
                    DetailSection(icon = Icons.Filled.FitnessCenter, title = "Séances réalisées") {
                        detail.workoutLogs.forEach { log ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.size(4.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                                Text(
                                    buildString {
                                        append(log.templateName.ifBlank { "Séance libre" })
                                        if (log.durationSecs > 0) append(" — ${log.durationSecs / 60} min")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                // Workout planifié
                if (plannedWorkoutName != null) {
                    DetailSection(icon = Icons.Filled.CalendarToday, title = "Séance planifiée") {
                        Text(
                            buildString {
                                append(plannedWorkoutName)
                                if (activeProgramName != null) append(" — $activeProgramName")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun DetailPill(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Insights ─────────────────────────────────────────────────────────────────

@Composable
private fun InsightsSection(insights: CalendarInsightsState) {
    Text(
        "Aperçu",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // Workout frequency — 4 weeks
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.FitnessCenter, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                Text("Fréquence d'entraînement", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            }
            Text(
                "Séances réalisées sur les 4 dernières semaines",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (insights.workoutsPerWeek.isNotEmpty()) {
                WorkoutFrequencyBars(weeksData = insights.workoutsPerWeek)
            }
        }
    }

    // Calorie average — 7 days
    if (insights.kcalGoal > 0f) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.LocalFireDepartment, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Moyenne calorique", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                }
                Text(
                    "7 derniers jours (jours enregistrés uniquement)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${insights.avgKcal7d.toInt()} kcal",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (insights.avgKcal7d > insights.kcalGoal * 1.1f)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "/ ${insights.kcalGoal.toInt()} kcal objectif",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { (insights.avgKcal7d / insights.kcalGoal).coerceIn(0f, 1.2f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small),
                    color = when {
                        insights.avgKcal7d == 0f -> MaterialTheme.colorScheme.surfaceVariant
                        insights.avgKcal7d > insights.kcalGoal * 1.1f -> MaterialTheme.colorScheme.error
                        insights.avgKcal7d < insights.kcalGoal * 0.8f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WorkoutFrequencyBars(weeksData: List<Int>) {
    val max = weeksData.maxOrNull()?.coerceAtLeast(1) ?: 1
    val weekLabels = listOf("S-3", "S-2", "S-1", "Cette sem.")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        weeksData.forEachIndexed { i, count ->
            val barHeight = ((count.toFloat() / max) * 44f).coerceAtLeast(if (count > 0) 6f else 2f).dp
            val isCurrentWeek = i == weeksData.size - 1
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.BottomCenter) {
                    if (count > 0) {
                        Text(
                            "$count",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isCurrentWeek) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(
                            if (isCurrentWeek) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f + 0.15f * i)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    weekLabels.getOrElse(i) { "S${i + 1}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

// ─── Legend ────────────────────────────────────────────────────────────────────

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
