package com.kalos.app.feature.workout.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.KalosNumberField
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.navigation.Screen
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

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
                        KalosNumberField(
                            value = editSets,
                            onValueChange = { editSets = it },
                            label = { Text(stringResource(R.string.training_sets_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        KalosNumberField(
                            value = editReps,
                            onValueChange = { editReps = it },
                            label = { Text(stringResource(R.string.training_reps_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    }) { Text(stringResource(R.string.action_validate)) }
                },
                dismissButton = {
                    TextButton(onClick = { editingExerciseIndex = null }) { Text(stringResource(R.string.action_cancel)) }
                },
            )
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = stringResource(if (templateId > 0) R.string.training_edit_workout else R.string.training_new_workout),
                    small = true,
                    navigation = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                ) {
                    IconButton(onClick = viewModel::save, enabled = state.name.isNotBlank() && !state.isSaving) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save))
                    }
                }
            }
            item(key = "name") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.training_workout_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item(key = "description") {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text(stringResource(R.string.training_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }

            item(key = "exercisesHead") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(stringResource(R.string.training_exercises_count, state.exercises.size), Modifier.weight(1f))
                    KalosButton(
                        stringResource(R.string.training_add),
                        ghost = true,
                        onClick = {
                            // Propagate the current exercise ids so the catalog can hide duplicates.
                            // Read on the catalog side via previousBackStackEntry.savedStateHandle.
                            currentEntry?.savedStateHandle?.set(
                                "excluded_exercise_ids",
                                state.exercises.map { it.exercise.id }.toLongArray(),
                            )
                            navController.navigate(Screen.ExerciseCatalog.route(templateId))
                        },
                    )
                }
            }

            if (state.exercises.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.training_builder_empty),
                        subtitle = stringResource(R.string.training_builder_empty_hint),
                        icon = Icons.Filled.FitnessCenter,
                    )
                }
            } else {
                itemsIndexed(state.exercises, key = { i, _ -> i }) { index, te ->
                    val muted = MaterialTheme.colorScheme.onSurfaceVariant
                    KalosCard(contentPadding = PaddingValues(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 4.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "${index + 1}",
                                style = KalosTheme.type.metric,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(28.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(te.exercise.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(R.string.training_sets_reps, te.defaultSets, te.defaultReps) +
                                        if (te.exercise.primaryMuscle.isNotBlank()) " · " + te.exercise.primaryMuscle else "",
                                    style = KalosTheme.type.metricSmall,
                                    color = muted,
                                )
                            }
                        }
                        // Réordonner sans glisser-déposer ; cibles de 48 dp.
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { viewModel.moveExercise(index, -1) }, enabled = index > 0) {
                                Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.training_move_up, te.exercise.name))
                            }
                            IconButton(onClick = { viewModel.moveExercise(index, 1) }, enabled = index < state.exercises.size - 1) {
                                Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.training_move_down, te.exercise.name))
                            }
                            IconButton(onClick = {
                                editingExerciseIndex = index
                                editSets = te.defaultSets.toString()
                                editReps = te.defaultReps.toString()
                            }) {
                                Icon(Icons.Filled.Edit, stringResource(R.string.training_edit_named, te.exercise.name), tint = muted, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.removeExercise(index) }) {
                                // Discret par ligne ; le rouge est réservé aux confirmations.
                                Icon(Icons.Outlined.Delete, stringResource(R.string.training_delete_named, te.exercise.name), tint = muted, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item(key = "program") {
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
    val days = DayOfWeek.entries.map { d -> d.getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() } }
    val none = stringResource(R.string.training_none)

    KalosCard {
        SectionLabel(stringResource(R.string.training_link_program))

        if (programs.isEmpty()) {
            Text(
                stringResource(R.string.training_link_program_empty),
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
                    value = programs.find { it.id == selectedProgramId }?.name ?: none,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.training_program)) },
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
                        text = { Text(none) },
                        onClick = { onProgramSelected(null); programExpanded = false },
                    )
                    programs.forEach { program ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(program.name)
                                    if (program.isActive) {
                                        InstrumentLabel(stringResource(R.string.training_program_active), color = MaterialTheme.colorScheme.primary)
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
                        value = selectedDayOfWeek?.let { days.getOrNull(it - 1) } ?: stringResource(R.string.training_pick_day),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.training_day_of_week)) },
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
