package com.kalos.app.feature.workout.active

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.ExerciseStatus
import com.kalos.app.core.domain.model.ExerciseTrackingMode
import com.kalos.app.core.ui.util.formatElapsedSince
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(templateId) { viewModel.loadTemplate(templateId) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onErrorShown()
        }
    }

    // Navigate to a newly added exercise after the tab list has been laid out.
    // Doing this in the same state update that grows the exercises list causes
    // ScrollableTabRow's SubcomposeLayout to access tabPositions[newIndex] before the
    // new tab has been measured (still uses the previous frame's tabPositions → IOOB crash).
    LaunchedEffect(state.exercises.size) {
        val lastIdx = state.exercises.size - 1
        if (lastIdx > 0 && state.exercises.getOrNull(lastIdx)?.status == ExerciseStatus.ADDED) {
            viewModel.selectExercise(lastIdx)
        }
    }

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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            val tabIndex = state.currentExIndex.coerceIn(0, state.exercises.size - 1)

            // Tab row + add-exercise button — unified surface bar
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = tabIndex,
                        modifier = Modifier.weight(1f),
                        containerColor = Color.Transparent,
                        divider = {},
                    ) {
                        state.exercises.forEachIndexed { i, ep ->
                            val textColor = when (ep.status) {
                                ExerciseStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
                                ExerciseStatus.REPLACED -> MaterialTheme.colorScheme.tertiary
                                ExerciseStatus.ADDED -> MaterialTheme.colorScheme.primary
                                ExerciseStatus.PLANNED -> MaterialTheme.colorScheme.onSurface
                            }
                            Tab(
                                selected = tabIndex == i,
                                onClick = { viewModel.selectExercise(i) },
                                text = {
                                    Text(ep.templateExercise.exercise.name, maxLines = 1, color = textColor)
                                },
                            )
                        }
                    }
                    VerticalDivider(
                        modifier = Modifier.height(28.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    IconButton(
                        onClick = viewModel::openAddPicker,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.Add, "Ajouter un exercice",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            val exIdx = tabIndex
            val ep = state.exercises[exIdx]

            // Per-exercise action bar (replace / skip / undo)
            ExerciseActionBar(
                ep = ep,
                onReplace = { viewModel.openReplacePicker(exIdx) },
                onSkip = { viewModel.skipExercise(exIdx) },
                onUndoReplace = { viewModel.undoReplace(exIdx) },
            )

            if (ep.status == ExerciseStatus.SKIPPED) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Filled.SkipNext, null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Exercice passé",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = { viewModel.undoSkip(exIdx) }) {
                                Text("Annuler")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    item {
                        Text(ep.templateExercise.exercise.primaryMuscle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Historical load reference (PR + last session) — weight modes only.
                        val mode = ep.templateExercise.exercise.trackingMode
                        val showWeightRef = mode == ExerciseTrackingMode.REPS_WEIGHT ||
                            mode == ExerciseTrackingMode.DURATION_WEIGHT
                        val ref = state.exerciseReferences[ep.templateExercise.exercise.id]
                        if (showWeightRef && ref != null) {
                            val label = buildString {
                                append("PR ${formatRefWeight(ref.prKg)} kg")
                                // Skip "Dernière séance" when it just equals the PR (redundant).
                                ref.lastSessionTopKg?.let { last ->
                                    if (last != ref.prKg) append(" · Dernière séance ${formatRefWeight(last)} kg")
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

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
                            trackingMode = ep.templateExercise.exercise.trackingMode,
                            onRepsChange = { viewModel.onRepsChange(exIdx, setIdx, it) },
                            onWeightChange = { viewModel.onWeightChange(exIdx, setIdx, it) },
                            onDurationChange = { viewModel.onDurationChange(exIdx, setIdx, it) },
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
            }

            AnimatedVisibility(visible = state.isResting) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
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
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Repos : ${state.restSecsLeft}s",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        TextButton(onClick = viewModel::skipRest) {
                            Text("Passer")
                        }
                    }
                }
            }
        }
    }

    // Exercise picker bottom sheet
    if (state.exercisePickerExIndex >= 0) {
        ExercisePickerSheet(
            muscleFilter = state.exercisePickerMuscle,
            onExerciseSelected = viewModel::onExercisePicked,
            onDismiss = viewModel::dismissPicker,
            // Greyed-out "Déjà ajouté" for every exercise currently in the session
            // (including SKIPPED — the user already touched it once, no double-tap).
            excludedExerciseIds = state.exercises.map { it.templateExercise.exercise.id }.toSet(),
        )
    }

    // Confirm dialog: replace exercise that already has set data
    if (state.confirmReplaceExercise != null) {
        val currentName = state.exercises.getOrNull(state.confirmReplaceExIndex)
            ?.templateExercise?.exercise?.name ?: ""
        AlertDialog(
            onDismissRequest = viewModel::cancelReplace,
            title = { Text("Remplacer l'exercice ?") },
            text = { Text("Les séries déjà saisies pour « $currentName » seront perdues.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmReplace) {
                    Text("Remplacer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelReplace) { Text("Annuler") }
            },
        )
    }

    if (state.resumeAvailable) {
        val timeLabel = formatElapsedSince(state.resumeStartedAt)
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Séance en cours") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Une séance non terminée a été trouvée (${state.templateName}), démarrée $timeLabel.")
                    if (state.resumeIsStale) {
                        Text(
                            "Cette séance date de plus de 24h. Elle est peut-être obsolète.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::resumeDraft) { Text("Reprendre") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::discardDraftAndStart) {
                    Text("Nouvelle séance", color = MaterialTheme.colorScheme.error)
                }
            },
        )
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ExerciseActionBar(
    ep: ExerciseProgress,
    onReplace: () -> Unit,
    onSkip: () -> Unit,
    onUndoReplace: () -> Unit,
) {
    when (ep.status) {
        ExerciseStatus.PLANNED -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onReplace,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remplacer", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Passer", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        ExerciseStatus.REPLACED -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(
                    "Remplace : ${ep.originalExerciseName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                if (ep.originalTemplateExercise != null) {
                    TextButton(
                        onClick = onUndoReplace,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("Annuler le remplacement",
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        ExerciseStatus.ADDED -> {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(
                    "Hors programme",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        ExerciseStatus.SKIPPED -> { /* handled by the skipped card in the main body */ }
    }
}

@Composable
private fun SetRow(
    setNumber: Int,
    set: SetInput,
    trackingMode: ExerciseTrackingMode,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onToggleComplete: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    // TextFieldValue is kept locally so we can control selection (select-all on focus).
    // LaunchedEffect syncs back if the ViewModel changes the value externally (undo, reset…)
    // without interfering with the cursor during normal typing.
    var weightValue by remember { mutableStateOf(TextFieldValue(set.weight, TextRange(set.weight.length))) }
    var repsValue   by remember { mutableStateOf(TextFieldValue(set.reps,   TextRange(set.reps.length)))  }
    var durationValue by remember { mutableStateOf(TextFieldValue(set.duration, TextRange(set.duration.length))) }

    LaunchedEffect(set.weight) {
        if (weightValue.text != set.weight) weightValue = TextFieldValue(set.weight, TextRange(set.weight.length))
    }
    LaunchedEffect(set.reps) {
        if (repsValue.text != set.reps) repsValue = TextFieldValue(set.reps, TextRange(set.reps.length))
    }
    LaunchedEffect(set.duration) {
        if (durationValue.text != set.duration) durationValue = TextFieldValue(set.duration, TextRange(set.duration.length))
    }

    val showReps = trackingMode == ExerciseTrackingMode.REPS_WEIGHT
    val showWeight = trackingMode == ExerciseTrackingMode.REPS_WEIGHT ||
        trackingMode == ExerciseTrackingMode.DURATION_WEIGHT
    val showDuration = trackingMode == ExerciseTrackingMode.DURATION ||
        trackingMode == ExerciseTrackingMode.DURATION_WEIGHT

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
        if (showWeight) {
            OutlinedTextField(
                value = weightValue,
                onValueChange = { tv -> weightValue = tv; onWeightChange(tv.text) },
                modifier = Modifier
                    .weight(1.5f)
                    .onFocusChanged { fs ->
                        if (fs.isFocused)
                            weightValue = weightValue.copy(selection = TextRange(0, weightValue.text.length))
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                placeholder = { Text("0") },
            )
        }
        if (showReps) {
            OutlinedTextField(
                value = repsValue,
                onValueChange = { tv -> repsValue = tv; onRepsChange(tv.text) },
                modifier = Modifier
                    .weight(1.5f)
                    .onFocusChanged { fs ->
                        if (fs.isFocused)
                            repsValue = repsValue.copy(selection = TextRange(0, repsValue.text.length))
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                placeholder = { Text("0") },
            )
        }
        if (showDuration) {
            // For DURATION-only mode the field takes the full row; for DURATION_WEIGHT it shares
            // the row with the weight field at the same weight ratio.
            OutlinedTextField(
                value = durationValue,
                onValueChange = { tv -> durationValue = tv; onDurationChange(tv.text) },
                modifier = Modifier
                    .weight(if (showWeight) 1.5f else 3f)
                    .onFocusChanged { fs ->
                        if (fs.isFocused)
                            durationValue = durationValue.copy(selection = TextRange(0, durationValue.text.length))
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                placeholder = { Text("mm:ss") },
            )
        }
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

/** 100.0 → "100", 82.5 → "82.5". */
private fun formatRefWeight(kg: Float): String =
    if (kg == kg.toLong().toFloat()) kg.toLong().toString() else "%.1f".format(kg)
