package com.kalos.app.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.ProgramWorkout
import com.kalos.app.core.ui.component.CalorieProgressRing
import com.kalos.app.core.ui.component.MacroTrioRow
import com.kalos.app.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dateLabel = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
        ).replaceFirstChar { it.uppercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Greeting
        Column {
            Text(
                if (state.userName.isNotEmpty()) "Bonjour, ${state.userName} 👋" else "Bonjour 👋",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(dateLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Calorie summary card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Calories aujourd'hui", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CalorieProgressRing(
                        consumed = state.summary.totalKcal,
                        goal = state.summary.goalKcal,
                        size = 140.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CalorieStat("Consommées", "${state.summary.totalKcal.toInt()} kcal", MaterialTheme.colorScheme.onSurface)
                        CalorieStat("Objectif", "${state.summary.goalKcal} kcal", MaterialTheme.colorScheme.onSurfaceVariant)
                        val remaining = state.summary.remainingKcal
                        CalorieStat(
                            if (remaining >= 0) "Restantes" else "Dépassement",
                            "${Math.abs(remaining.toInt())} kcal",
                            if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        // Macros card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Macros", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                MacroTrioRow(
                    proteinConsumed = state.summary.totalProtein,
                    proteinGoal = state.summary.goalProtein,
                    carbsConsumed = state.summary.totalCarbs,
                    carbsGoal = state.summary.goalCarbs,
                    fatConsumed = state.summary.totalFat,
                    fatGoal = state.summary.goalFat,
                )
            }
        }

        // Body weight card
        if (state.lastWeightKg != null && state.lastWeightDate != null) {
            BodyWeightCard(
                weightKg = state.lastWeightKg,
                date = state.lastWeightDate,
                delta = state.weightDelta,
            )
        }

        // Quick actions
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = { navController.navigate(Screen.Nutrition.route) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(Icons.Outlined.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Journal")
            }
            FilledTonalButton(
                onClick = { navController.navigate(Screen.Workout.route) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Sport")
            }
        }

        // Programme actif — séance du jour
        when {
            state.todayProgramWorkout != null -> {
                TodayProgramWorkoutCard(
                    programWorkout = state.todayProgramWorkout!!,
                    programName = state.activeProgramName ?: "",
                    onStart = { templateId ->
                        navController.navigate(Screen.ActiveWorkout.route(templateId))
                    },
                )
            }
            state.activeProgramName != null -> {
                RestDayCard(
                    programName = state.activeProgramName!!,
                    nextWorkout = state.nextSessionWorkout,
                    nextDate = state.nextSessionDate,
                    onViewProgram = { navController.navigate(Screen.Programs.route) },
                )
            }
        }

        // Séances du jour déjà effectuées
        if (state.todayWorkouts.isNotEmpty()) {
            Text("Entraînements aujourd'hui", style = MaterialTheme.typography.titleSmall)
            state.todayWorkouts.forEach { log ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(log.templateName.ifEmpty { "Séance libre" }) },
                        supportingContent = {
                            Text("${log.exercises.size} exercice(s) • ${log.durationSecs / 60} min")
                        },
                        leadingContent = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TodayProgramWorkoutCard(
    programWorkout: ProgramWorkout,
    programName: String,
    onStart: (Long) -> Unit,
) {
    val template = programWorkout.template ?: return
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Séance prévue aujourd'hui",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(template.name, style = MaterialTheme.typography.titleMedium)
            if (programName.isNotBlank()) {
                Text(
                    programName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onStart(template.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Démarrer la séance")
            }
        }
    }
}

@Composable
private fun RestDayCard(
    programName: String,
    nextWorkout: ProgramWorkout?,
    nextDate: LocalDate?,
    onViewProgram: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("Repos aujourd'hui", style = MaterialTheme.typography.titleSmall)
                Text(
                    programName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (nextWorkout != null && nextDate != null) {
                    val dayName = nextDate.dayOfWeek
                        .getDisplayName(TextStyle.FULL, Locale.FRENCH)
                        .replaceFirstChar { it.uppercase() }
                    val sessionName = nextWorkout.template?.name
                    val label = if (sessionName != null) "$dayName — $sessionName" else dayName
                    Text(
                        "Prochaine séance : $label",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            TextButton(onClick = onViewProgram) { Text("Programme") }
        }
    }
}

@Composable
private fun BodyWeightCard(weightKg: Float, date: String, delta: Float?) {
    val dateLabel = remember(date) {
        val d = java.time.LocalDate.parse(date)
        val today = java.time.LocalDate.now()
        when (d) {
            today -> "Aujourd'hui"
            today.minusDays(1) -> "Hier"
            else -> d.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH))
        }
    }
    val weightLabel = if (weightKg == weightKg.toLong().toFloat()) "${weightKg.toLong()} kg" else "${"%.1f".format(weightKg)} kg"

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    Icons.Filled.MonitorWeight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Poids corporel", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(weightLabel, style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(dateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (delta != null) {
                    val sign = if (delta >= 0f) "+" else ""
                    val deltaLabel = if (delta == delta.toLong().toFloat()) "${sign}${delta.toLong()} kg" else "${sign}${"%.1f".format(delta)} kg"
                    Text(deltaLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CalorieStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
