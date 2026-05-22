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
        }
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(programs, key = { it.id }) { program ->
                ProgramCard(
                    program = program,
                    onActivate = { viewModel.activate(program.id) },
                    onDetail = { navController.navigate(Screen.ProgramDetail.route(program.id)) },
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
                if (program.isActive) {
                    Badge { Text("Actif") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${program.durationWeeks} sem.") })
                AssistChip(onClick = {}, label = { Text("${program.daysPerWeek} j/sem") })
                AssistChip(onClick = {}, label = { Text("${program.workouts.size} séances") })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDetail) { Text("Détails") }
                if (!program.isActive) {
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onActivate) { Text("Activer") }
                }
            }
        }
    }
}
