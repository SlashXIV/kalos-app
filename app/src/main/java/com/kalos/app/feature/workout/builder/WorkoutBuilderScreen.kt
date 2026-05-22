package com.kalos.app.feature.workout.builder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutBuilderScreen(
    navController: NavController,
    templateId: Long,
    viewModel: WorkoutBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEntry = navController.currentBackStackEntry

    LaunchedEffect(templateId) { viewModel.loadTemplate(templateId) }
    LaunchedEffect(state.savedId) { if (state.savedId != null) navController.popBackStack() }

    // Receive exercise selected from the catalog
    LaunchedEffect(currentEntry) {
        currentEntry?.savedStateHandle?.let { handle ->
            handle.getStateFlow<Long?>("added_exercise_id", null).collect { exerciseId ->
                if (exerciseId != null) {
                    viewModel.addExerciseById(exerciseId)
                    handle.remove<Long>("added_exercise_id")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (templateId > 0) "Modifier la séance" else "Nouvelle séance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save, enabled = state.name.isNotBlank() && !state.isSaving) {
                        Icon(Icons.Filled.Check, contentDescription = "Enregistrer")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Nom de la séance *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Exercices (${state.exercises.size})", style = MaterialTheme.typography.titleSmall)
                    FilledTonalButton(
                        onClick = { navController.navigate(Screen.ExerciseCatalog.route(templateId)) }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ajouter")
                    }
                }
            }

            if (state.exercises.isEmpty()) {
                item {
                    Text(
                        "Aucun exercice ajouté. Touchez « Ajouter » pour parcourir le catalogue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                itemsIndexed(state.exercises, key = { i, _ -> i }) { index, te ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${index + 1}.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(te.exercise.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${te.defaultSets} × ${te.defaultReps} rép — ${te.exercise.primaryMuscle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.removeExercise(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
