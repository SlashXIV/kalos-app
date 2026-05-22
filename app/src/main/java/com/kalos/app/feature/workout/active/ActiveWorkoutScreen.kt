package com.kalos.app.feature.workout.active

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    templateId: Long,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(templateId) { viewModel.loadTemplate(templateId) }
    LaunchedEffect(state.savedLogId) {
        if (state.savedLogId != null) {
            navController.navigate(Screen.WorkoutSummary.route(state.savedLogId!!)) {
                popUpTo(Screen.Workout.route)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.templateName.ifBlank { "Séance en cours" },
                            style = MaterialTheme.typography.titleMedium)
                        Text(formatElapsed(state.elapsedSecs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showFinishDialog = true },
                        enabled = !state.isSaving,
                    ) {
                        Text("Terminer", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.exercises.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aucun exercice dans cette séance.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = state.currentExIndex.coerceIn(0, state.exercises.size - 1)) {
                state.exercises.forEachIndexed { i, ep ->
                    Tab(
                        selected = state.currentExIndex == i,
                        onClick = { viewModel.selectExercise(i) },
                        text = { Text(ep.templateExercise.exercise.name, maxLines = 1) },
                    )
                }
            }

            val exIdx = state.currentExIndex.coerceIn(0, state.exercises.size - 1)
            val ep = state.exercises[exIdx]

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                item {
                    Text(ep.templateExercise.exercise.primaryMuscle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("N°", modifier = Modifier.width(24.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Poids (kg)", modifier = Modifier.weight(1.5f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Reps", modifier = Modifier.weight(1.5f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                }

                itemsIndexed(ep.sets) { setIdx, si ->
                    SetRow(
                        setNumber = setIdx + 1,
                        set = si,
                        onRepsChange = { viewModel.onRepsChange(exIdx, setIdx, it) },
                        onWeightChange = { viewModel.onWeightChange(exIdx, setIdx, it) },
                        onToggleComplete = { viewModel.toggleSetCompleted(exIdx, setIdx) },
                        onRemove = if (ep.sets.size > 1) {
                            { viewModel.removeSet(exIdx, setIdx) }
                        } else null,
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.addSet(exIdx) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ajouter une série")
                    }
                }
            }

            AnimatedVisibility(visible = state.isResting) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.Timer, null, modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Repos : ${state.restSecsLeft}s",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        TextButton(onClick = viewModel::skipRest) {
                            Text("Passer")
                        }
                    }
                }
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Terminer la séance ?") },
            text = { Text("Les séries enregistrées seront sauvegardées dans votre historique.") },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    viewModel.finish()
                }) { Text("Terminer") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Continuer") }
            },
        )
    }

    if (state.isSaving) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun SetRow(
    setNumber: Int,
    set: SetInput,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onToggleComplete: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "$setNumber",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(24.dp),
            color = if (set.isCompleted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = set.weight,
            onValueChange = onWeightChange,
            modifier = Modifier.weight(1.5f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            placeholder = { Text("0") },
        )
        OutlinedTextField(
            value = set.reps,
            onValueChange = onRepsChange,
            modifier = Modifier.weight(1.5f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            placeholder = { Text("0") },
        )
        IconButton(onClick = onToggleComplete) {
            Icon(
                if (set.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (set.isCompleted) "Complétée" else "Marquer complète",
                tint = if (set.isCompleted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Remove, "Supprimer la série",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error)
            }
        } else {
            Spacer(modifier = Modifier.width(36.dp))
        }
    }
}

private fun formatElapsed(secs: Int): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
