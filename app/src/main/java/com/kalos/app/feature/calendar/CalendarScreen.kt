package com.kalos.app.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Calendrier") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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

            CalendarGrid(state = state)

            HorizontalDivider()

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LegendItem(color = MaterialTheme.colorScheme.primary, label = "Nutrition")
                LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Sport réalisé")
                LegendItem(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f), label = "Sport planifié")
            }
        }
    }
}

@Composable
private fun CalendarGrid(state: CalendarUiState) {
    val month = state.month
    val firstDay = month.atDay(1)
    val dayOfWeekOffset = firstDay.dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()
    val today = LocalDate.now()

    val cells = buildList {
        repeat(dayOfWeekOffset) { add(null) }
        for (d in 1..daysInMonth) add(d)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(cells) { day ->
            if (day == null) {
                Box(modifier = Modifier.aspectRatio(1f))
            } else {
                val date = month.atDay(day)
                val dateStr = date.toString()
                DayCell(
                    day = day,
                    isToday = date == today,
                    hasNutrition = dateStr in state.nutritionDates,
                    hasWorkout = dateStr in state.workoutDates,
                    hasPlanned = dateStr in state.plannedWorkoutDates,
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    hasNutrition: Boolean,
    hasWorkout: Boolean,
    hasPlanned: Boolean,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(
                if (isToday) Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
            if (hasNutrition || hasWorkout || hasPlanned) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (hasNutrition) {
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (hasWorkout) {
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                    } else if (hasPlanned) {
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
