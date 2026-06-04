package com.kalos.app.feature.workout.program

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
    navController: NavController,
    viewModel: ProgramViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Programmes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.ProgramEditor.create()) }) {
                Icon(Icons.Filled.Add, "Nouveau programme")
            }
        },
    ) { padding ->
        ProgramsContent(
            navController = navController,
            viewModel = viewModel,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun ProgramsTabContent(
    navController: NavController,
    viewModel: ProgramViewModel = hiltViewModel(),
) {
    ProgramsContent(navController = navController, viewModel = viewModel)
}

@Composable
private fun ProgramsContent(
    navController: NavController,
    viewModel: ProgramViewModel,
    modifier: Modifier = Modifier,
) {
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    if (programs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Aucun programme",
                subtitle = "Les programmes d'entraînement apparaîtront ici",
                icon = Icons.AutoMirrored.Filled.MenuBook,
            )
        }
    } else {
        LazyColumn(
            // bottom = 88dp keeps the last card's "Activer" button clear of the FAB
            // (56dp FAB + 16dp margin + breathing room).
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(programs, key = { it.id }) { program ->
                ProgramCard(
                    program = program,
                    onActivate = { viewModel.activate(program.id) },
                    onDetail = { navController.navigate(Screen.ProgramDetail.route(program.id)) },
                    onEdit = if (program.isCustom) {
                        { navController.navigate(Screen.ProgramEditor.edit(program.id)) }
                    } else null,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramCard(
    program: TrainingProgram,
    onActivate: () -> Unit,
    onDetail: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(program.name, style = MaterialTheme.typography.titleMedium)
                    if (program.description.isNotBlank()) {
                        Text(
                            program.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (program.isActive) {
                        Badge { Text("Actif") }
                    }
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Filled.Edit,
                                "Modifier",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${program.durationWeeks} sem.") })
                AssistChip(onClick = {}, label = { Text("${program.daysPerWeek} j/sem") })
                AssistChip(onClick = {}, label = {
                    Text(
                        when (val n = program.workouts.size) {
                            0 -> "Aucune séance liée"
                            1 -> "1 séance liée"
                            else -> "$n séances liées"
                        }
                    )
                })
            }
            val hasLinkedWorkouts = program.workouts.isNotEmpty()
            if (!hasLinkedWorkouts) {
                Text(
                    "Aucune séance liée — activer ce programme n'aurait aucun effet. " +
                        "Liez des séances depuis l'éditeur de séance (« Rattacher à un programme »).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDetail) { Text("Détails") }
                if (!program.isActive) {
                    Spacer(Modifier.width(8.dp))
                    // Disabled when empty: the CTA must not promise an activation that
                    // would produce an empty program.
                    Button(onClick = onActivate, enabled = hasLinkedWorkouts) { Text("Activer") }
                }
            }
        }
    }
}
