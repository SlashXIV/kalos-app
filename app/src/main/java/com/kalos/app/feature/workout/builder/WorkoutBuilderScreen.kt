package com.kalos.app.feature.workout.builder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.TrainingProgram
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

    var editingExerciseIndex by remember { mutableStateOf<Int?>(null) }
    var editSets by remember { mutableStateOf("") }
    var editReps by remember { mutableStateOf("") }

    LaunchedEffect(templateId) { viewModel.loadTemplate(templateId) }
    LaunchedEffect(state.savedId) { if (state.savedId != null) navController.popBackStack() }

    LaunchedEffect(currentEntry) {
        currentEntry?.savedStateHandle?.let { handle ->
            handle.getStateFlow<Long?>("added_exercise_id", null).collect { exerciseId ->
                if (exerciseId != null) {
                    viewModel.addExerciseById(exerciseId)
                    handle["added_exercise_id"] = null
                }
            }
        }
    }

    editingExerciseIndex?.let { idx ->
        if (idx < state.exercises.size) {
            val te = state.exercises[idx]
            AlertDialog(
                onDismissRequest = { editingExerciseIndex = null },
                title = { Text(te.exercise.name, style = MaterialTheme.typography.titleMedium) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editSets,
                            onValueChange = { editSets = it },
                            label = { Text("Séries") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = editReps,
                            onValueChange = { editReps = it },
                            label = { Text("Répétitions") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateExerciseParams(
                            idx,
                            editSets.toIntOrNull() ?: te.defaultSets,
                            editReps.toIntOrNull() ?: te.defaultReps,
                        )
                        editingExerciseIndex = null
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { editingExerciseIndex = null }) { Text("Annuler") }
                },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (templateId > 0) "Modifier la séance" else "Nouvelle séance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
                    OutlinedButton(
                        onClick = {
                            // Propagate the current exercise ids so the catalog can hide duplicates.
                            // Read on the catalog side via previousBackStackEntry.savedStateHandle.
                            currentEntry?.savedStateHandle?.set(
                                "excluded_exercise_ids",
                                state.exercises.map { it.exercise.id }.toLongArray(),
                            )
                            navController.navigate(Screen.ExerciseCatalog.route(templateId))
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ajouter")
                    }
                }
            }

            if (state.exercises.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Text(
                            "Aucun exercice ajouté",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Touchez « Ajouter » pour parcourir le catalogue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            } else {
                itemsIndexed(state.exercises, key = { i, _ -> i }) { index, te ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${index + 1}.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(28.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(te.exercise.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${te.defaultSets} × ${te.defaultReps} rép — ${te.exercise.primaryMuscle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                editingExerciseIndex = index
                                editSets = te.defaultSets.toString()
                                editReps = te.defaultReps.toString()
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.removeExercise(index) }) {
                                // Muted per-row delete; red is reserved for confirmation steps.
                                Icon(Icons.Filled.Delete, contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            item {
                ProgramLinkSection(
                    programs = state.availablePrograms,
                    selectedProgramId = state.selectedProgramId,
                    selectedDayOfWeek = state.selectedDayOfWeek,
                    onProgramSelected = viewModel::onProgramSelected,
                    onDaySelected = viewModel::onDaySelected,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramLinkSection(
    programs: List<TrainingProgram>,
    selectedProgramId: Long?,
    selectedDayOfWeek: Int?,
    onProgramSelected: (Long?) -> Unit,
    onDaySelected: (Int?) -> Unit,
) {
    val days = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.CalendarToday,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text("Rattacher à un programme", style = MaterialTheme.typography.titleSmall)
            }

            if (programs.isEmpty()) {
                Text(
                    "Aucun programme disponible — créez-en un dans l'onglet Programmes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                var programExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = programExpanded,
                    onExpandedChange = { programExpanded = it },
                ) {
                    OutlinedTextField(
                        value = programs.find { it.id == selectedProgramId }?.name ?: "Aucun",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Programme") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = programExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = programExpanded,
                        onDismissRequest = { programExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Aucun") },
                            onClick = { onProgramSelected(null); programExpanded = false },
                        )
                        programs.forEach { program ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(program.name)
                                        if (program.isActive) {
                                            Text(
                                                "Actif",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                },
                                onClick = { onProgramSelected(program.id); programExpanded = false },
                            )
                        }
                    }
                }

                if (selectedProgramId != null) {
                    var dayExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = dayExpanded,
                        onExpandedChange = { dayExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedDayOfWeek?.let { days.getOrElse(it - 1) { "?" } } ?: "Choisir le jour",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jour de la semaine") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false },
                        ) {
                            days.forEachIndexed { idx, day ->
                                DropdownMenuItem(
                                    text = { Text(day) },
                                    onClick = { onDaySelected(idx + 1); dayExpanded = false },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
