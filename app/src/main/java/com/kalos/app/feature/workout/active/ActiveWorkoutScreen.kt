package com.kalos.app.feature.workout.active

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.util.formatKg
import com.kalos.app.core.domain.model.ExerciseStatus
import com.kalos.app.core.domain.model.ExerciseTrackingMode
import com.kalos.app.core.ui.component.Graduation
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.motion.RollingDigits
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatElapsedSince
import com.kalos.app.feature.workout.formatClock
import com.kalos.app.navigation.Screen
import kotlin.math.cos
import kotlin.math.sin

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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = state.templateName.ifBlank { stringResource(R.string.training_in_progress) },
                eyebrow = stringResource(R.string.training_elapsed, formatClock(state.elapsedSecs)),
                small = true,
                modifier = Modifier.padding(horizontal = 16.dp),
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            ) {
                TextButton(onClick = { showFinishDialog = true }, enabled = !state.isSaving) {
                    Text(stringResource(R.string.training_finish), style = KalosTheme.type.action)
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.exercises.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.training_no_exercises), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> ActiveContent(state, viewModel, Modifier.weight(1f))
            }

            AnimatedVisibility(visible = state.isResting) {
                RestBar(secsLeft = state.restSecsLeft, totalSecs = state.restTotalSecs, onSkip = viewModel::skipRest)
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
            title = { Text(stringResource(R.string.training_replace_title)) },
            text = { Text(stringResource(R.string.training_replace_body, currentName)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmReplace) {
                    Text(stringResource(R.string.training_replace), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelReplace) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (state.resumeAvailable) {
        val timeLabel = formatElapsedSince(state.resumeStartedAt)
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.training_in_progress)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.training_resume_body, state.templateName, timeLabel))
                    if (state.resumeIsStale) {
                        Text(
                            stringResource(R.string.training_resume_stale),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::resumeDraft) { Text(stringResource(R.string.training_resume)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::discardDraftAndStart) {
                    Text(stringResource(R.string.training_new_workout), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text(stringResource(R.string.training_finish_title)) },
            text = { Text(stringResource(R.string.training_finish_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    viewModel.finish()
                }) { Text(stringResource(R.string.training_finish)) }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text(stringResource(R.string.training_continue)) }
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
private fun ActiveContent(state: ActiveWorkoutUiState, viewModel: ActiveWorkoutViewModel, modifier: Modifier) {
    val tabIndex = state.currentExIndex.coerceIn(0, state.exercises.size - 1)
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier) {
        // Onglets d'exercices + ajout d'un exercice.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                edgePadding = 16.dp,
                divider = {},
                indicator = { positions ->
                    positions.getOrNull(tabIndex)?.let { pos ->
                        Graduation(accent, Modifier.tabIndicatorOffset(pos).height(8.dp))
                    }
                },
            ) {
                state.exercises.forEachIndexed { i, ep ->
                    val textColor = when {
                        ep.status == ExerciseStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
                        tabIndex == i -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Tab(
                        selected = tabIndex == i,
                        onClick = { viewModel.selectExercise(i) },
                        text = {
                            Text(
                                ep.templateExercise.exercise.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = textColor,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                    )
                }
            }
            IconButton(onClick = viewModel::openAddPicker) {
                Icon(Icons.Filled.Add, stringResource(R.string.training_add_exercise), tint = accent)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        val exIdx = tabIndex
        val ep = state.exercises[exIdx]

        ExerciseActionBar(
            ep = ep,
            onReplace = { viewModel.openReplacePicker(exIdx) },
            onSkip = { viewModel.skipExercise(exIdx) },
            onUndoReplace = { viewModel.undoReplace(exIdx) },
        )

        if (ep.status == ExerciseStatus.SKIPPED) {
            KalosCard(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.training_exercise_skipped),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                KalosButton(stringResource(R.string.action_cancel), onClick = { viewModel.undoSkip(exIdx) }, ghost = true)
            }
            return@Column
        }

        val mode = ep.templateExercise.exercise.trackingMode
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "ref") {
                ExerciseReferenceBlock(ep, state)
            }
            item(key = "head") { SetTableHeader(mode) }

            itemsIndexed(ep.sets) { setIdx, si ->
                SetRow(
                    setNumber = setIdx + 1,
                    set = si,
                    trackingMode = mode,
                    onRepsChange = { viewModel.onRepsChange(exIdx, setIdx, it) },
                    onWeightChange = { viewModel.onWeightChange(exIdx, setIdx, it) },
                    onDurationChange = { viewModel.onDurationChange(exIdx, setIdx, it) },
                    onToggleComplete = { viewModel.toggleSetCompleted(exIdx, setIdx) },
                    onRemove = if (ep.sets.size > 1) {
                        { viewModel.removeSet(exIdx, setIdx) }
                    } else null,
                )
            }

            item(key = "add") {
                KalosButton(
                    stringResource(R.string.training_add_set),
                    onClick = { viewModel.addSet(exIdx) },
                    ghost = true,
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 6.dp),
                )
            }
        }
    }
}

/** Muscle principal et repère de charge (record + dernière séance), modes avec poids seulement. */
@Composable
private fun ExerciseReferenceBlock(ep: ExerciseProgress, state: ActiveWorkoutUiState) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (ep.templateExercise.exercise.primaryMuscle.isNotBlank()) {
            InstrumentLabel(ep.templateExercise.exercise.primaryMuscle)
        }
        val mode = ep.templateExercise.exercise.trackingMode
        val showWeightRef = mode == ExerciseTrackingMode.REPS_WEIGHT || mode == ExerciseTrackingMode.DURATION_WEIGHT
        val ref = state.exerciseReferences[ep.templateExercise.exercise.id]
        if (showWeightRef && ref != null) {
            val pr = stringResource(R.string.training_ref_pr, formatKg(ref.prKg))
            // « Dernière séance » masquée quand elle égale le record (redondant).
            val last = ref.lastSessionTopKg?.takeIf { it != ref.prKg }
            Text(
                if (last != null) pr + " · " + stringResource(R.string.training_ref_last, formatKg(last)) else pr,
                style = KalosTheme.type.metricSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            // Détail de la dernière séance : une ligne discrète, défilable si longue.
            if (ref.lastSessionSets.isNotEmpty()) {
                val detail = ref.lastSessionSets.joinToString(" · ") { s -> "${s.reps}×${formatKg(s.weightKg)}" }
                Text(
                    stringResource(R.string.training_ref_last_sets, detail),
                    style = KalosTheme.type.metricSmall,
                    color = muted,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun SetTableHeader(mode: ExerciseTrackingMode) {
    val showWeight = mode != ExerciseTrackingMode.DURATION
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InstrumentLabel(stringResource(R.string.training_col_number), Modifier.width(28.dp))
        if (showWeight) InstrumentLabel(stringResource(R.string.kg), Modifier.weight(1.5f))
        if (mode == ExerciseTrackingMode.REPS_WEIGHT) InstrumentLabel(stringResource(R.string.training_col_reps), Modifier.weight(1.5f))
        if (mode != ExerciseTrackingMode.REPS_WEIGHT) {
            InstrumentLabel(stringResource(R.string.training_col_duration), Modifier.weight(if (showWeight) 1.5f else 3f))
        }
        // Place des colonnes « valider » et « retirer ».
        Spacer(Modifier.width(104.dp))
    }
}

@Composable
private fun ExerciseActionBar(
    ep: ExerciseProgress,
    onReplace: () -> Unit,
    onSkip: () -> Unit,
    onUndoReplace: () -> Unit,
) {
    // Skipped exercises are handled by the dedicated card in the main body.
    if (ep.status == ExerciseStatus.SKIPPED) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Status context line — informative only, shown above the always-available actions.
        when (ep.status) {
            ExerciseStatus.REPLACED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.training_replaces, ep.originalExerciseName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    // Undo is only possible while the original is still in memory
                    // (lost after an app kill — the draft doesn't persist it).
                    if (ep.originalTemplateExercise != null) {
                        TextButton(onClick = onUndoReplace) { Text(stringResource(R.string.action_cancel)) }
                    }
                }
            }
            ExerciseStatus.ADDED -> InstrumentLabel(stringResource(R.string.training_off_program), color = MaterialTheme.colorScheme.primary)
            else -> {}
        }

        // Remplacer / Passer are ALWAYS available, whatever the current status.
        // Without this, a replaced (or added) exercise could never be re-replaced
        // after a misclick — and became fully frozen after an app kill.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KalosButton(stringResource(R.string.training_replace), onClick = onReplace, ghost = true, modifier = Modifier.weight(1f))
            KalosButton(stringResource(R.string.training_skip_exercise), onClick = onSkip, ghost = true, modifier = Modifier.weight(1f))
        }
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
    val metric = KalosTheme.type.metric
    val weightLabel = stringResource(R.string.training_field_weight, setNumber)
    val repsLabel = stringResource(R.string.training_field_reps, setNumber)
    val durationLabel = stringResource(R.string.training_field_duration, setNumber)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "$setNumber",
            style = metric,
            modifier = Modifier.width(28.dp),
            color = if (set.isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showWeight) {
            OutlinedTextField(
                value = weightValue,
                onValueChange = { tv -> weightValue = tv; onWeightChange(tv.text) },
                modifier = Modifier
                    .weight(1.5f)
                    .semantics { contentDescription = weightLabel }
                    .onFocusChanged { fs ->
                        if (fs.isFocused)
                            weightValue = weightValue.copy(selection = TextRange(0, weightValue.text.length))
                    },
                textStyle = metric,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                placeholder = { Text("0", style = metric) },
            )
        }
        if (showReps) {
            OutlinedTextField(
                value = repsValue,
                onValueChange = { tv -> repsValue = tv; onRepsChange(tv.text) },
                modifier = Modifier
                    .weight(1.5f)
                    .semantics { contentDescription = repsLabel }
                    .onFocusChanged { fs ->
                        if (fs.isFocused)
                            repsValue = repsValue.copy(selection = TextRange(0, repsValue.text.length))
                    },
                textStyle = metric,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                placeholder = { Text("0", style = metric) },
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
                    .semantics { contentDescription = durationLabel }
                    .onFocusChanged { fs ->
                        if (fs.isFocused)
                            durationValue = durationValue.copy(selection = TextRange(0, durationValue.text.length))
                    },
                textStyle = metric,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                placeholder = { Text("0:00", style = metric) },
            )
        }
        SetStamp(setNumber, set.isCompleted, onToggleComplete)
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Remove,
                    stringResource(R.string.training_remove_set, setNumber),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

/** Pastille ronde « tampon » : vide tant que la série n'est pas validée, pleine (objectif atteint) ensuite. */
@Composable
private fun SetStamp(setNumber: Int, done: Boolean, onToggle: () -> Unit) {
    val stamp = KalosTheme.nutrients.goalReached
    val label = stringResource(R.string.training_set_number, setNumber)
    Box(
        Modifier
            .size(48.dp)
            .toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .then(
                    if (done) Modifier.background(stamp)
                    else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Bandeau de repos : petit cadran gradué qui se vide et temps restant en chiffres qui roulent.
 * Pas de liveRegion : TalkBack ne lit le temps que si l'on se pose dessus, jamais chaque seconde.
 */
@Composable
private fun RestBar(secsLeft: Int, totalSecs: Int, onSkip: () -> Unit) {
    val total = maxOf(totalSecs, secsLeft)
    val label = stringResource(R.string.training_rest)
    val spoken = stringResource(R.string.training_rest_spoken, secsLeft / 60, secsLeft % 60)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.weight(1f).clearAndSetSemantics { contentDescription = spoken },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RestDial(fraction = if (total > 0) secsLeft / total.toFloat() else 0f)
            Column {
                InstrumentLabel(label)
                RollingDigits(formatClock(secsLeft), style = KalosTheme.type.metricLarge)
            }
        }
        TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.training_skip_rest), style = KalosTheme.type.action, textAlign = TextAlign.Center)
        }
    }
}

/** Cadran de 40 graduations : celles qui restent sont à l'accent, les écoulées s'éteignent. */
@Composable
private fun RestDial(fraction: Float) {
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.size(44.dp)) {
        val n = 40
        val c = Offset(size.width / 2, size.height / 2)
        val outer = size.minDimension / 2
        val w = 1.5.dp.toPx()
        val lit = (fraction.coerceIn(0f, 1f) * n).toInt()
        for (i in 0 until n) {
            val a = Math.toRadians(-90.0 + 360.0 * i / n)
            val inner = outer - (if (i % 5 == 0) 7 else 4).dp.toPx()
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            drawLine(if (i < lit) on else off, c + dir * inner, c + dir * outer, w, StrokeCap.Round)
        }
    }
}
