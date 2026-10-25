package com.kalos.app.feature.workout.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.util.formatKg
import com.kalos.app.core.domain.model.ExerciseStatus
import com.kalos.app.core.domain.model.LogExercise
import com.kalos.app.core.domain.model.WorkoutSet
import com.kalos.app.core.ui.component.EditWorkoutSetDialog
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import com.kalos.app.feature.workout.CompletedSetLine
import com.kalos.app.feature.workout.ExerciseSetsScale
import com.kalos.app.feature.workout.SessionStats
import com.kalos.app.feature.workout.logDateLong
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue


@Composable
fun WorkoutLogDetailScreen(
    navController: NavController,
    logId: Long,
    viewModel: WorkoutLogDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(logId) { viewModel.load(logId) }
    val context = LocalContext.current
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { res ->
            snackbarHostState.showSnackbar(context.getString(res))
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val log = state.log
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = log?.templateName?.ifBlank { null } ?: stringResource(R.string.training_free_workout),
                    eyebrow = log?.let { logDateLong(it.date) } ?: stringResource(R.string.training_log_detail),
                    small = true,
                    navigation = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                        }
                    },
                )
            }
            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                log == null -> item {
                    Text(
                        stringResource(R.string.training_log_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    item(key = "stats") { SessionStats(log, Modifier.padding(vertical = 8.dp)) }
                    if (log.exercises.isNotEmpty()) {
                        item(key = "label") { SectionLabel(stringResource(R.string.training_exercises)) }
                        items(log.exercises, key = { it.id }) { le ->
                            ExerciseDetailCard(
                                logExercise = le,
                                maxWeight = state.maxWeights[le.exercise.id],
                                onEditSet = { set -> viewModel.openEditDialog(set, le.exercise.id, le.exercise.trackingMode) },
                            )
                        }
                    }
                }
            }
        }
        state.pendingEdit?.let { pending ->
            EditWorkoutSetDialog(
                set = pending.set,
                trackingMode = pending.trackingMode,
                onDismiss = viewModel::dismissEditDialog,
                onConfirm = { reps, weight, durationSecs ->
                    viewModel.saveSetEdit(reps, weight, durationSecs)
                },
            )
        }
        if (state.isSavingEdit) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

// ─── Content ─────────────────────────────────────────────────────────────────

@Composable
private fun ExerciseDetailCard(
    logExercise: LogExercise,
    maxWeight: Float?,
    onEditSet: (WorkoutSet) -> Unit = {},
) {
    val completedSets = logExercise.sets.filter { it.isCompleted }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    KalosCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(logExercise.exercise.name, style = MaterialTheme.typography.titleMedium)
                if (logExercise.exercise.primaryMuscle.isNotBlank()) {
                    InstrumentLabel(logExercise.exercise.primaryMuscle)
                }
            }
            if (maxWeight != null && maxWeight > 0f) {
                Text(
                    stringResource(R.string.training_ref_pr, formatKg(maxWeight)),
                    style = KalosTheme.type.metricSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Statut : seulement hors du cas « prévu ». Toujours un libellé, jamais la couleur seule.
        when (logExercise.status) {
            ExerciseStatus.SKIPPED -> InstrumentLabel(stringResource(R.string.training_status_skipped))
            ExerciseStatus.REPLACED -> Column {
                InstrumentLabel(stringResource(R.string.training_status_replaced), color = MaterialTheme.colorScheme.primary)
                if (logExercise.replacedExerciseName.isNotBlank()) {
                    Text(
                        stringResource(R.string.training_instead_of, logExercise.replacedExerciseName),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
            }
            ExerciseStatus.ADDED -> InstrumentLabel(stringResource(R.string.training_off_program), color = MaterialTheme.colorScheme.primary)
            else -> {}
        }

        if (logExercise.status == ExerciseStatus.SKIPPED) return@KalosCard
        ExerciseSetsScale(logExercise)
        if (completedSets.isEmpty()) {
            Text(stringResource(R.string.training_no_completed_set), style = MaterialTheme.typography.bodySmall, color = muted)
        } else {
            completedSets.forEachIndexed { i, set ->
                CompletedSetLine(i + 1, set, logExercise.exercise.trackingMode) { onEditSet(set) }
            }
            if (logExercise.totalVolume > 0f) {
                Text(
                    stringResource(R.string.training_volume_line, formatGroupedInt(logExercise.totalVolume.roundToInt())),
                    style = KalosTheme.type.metricSmall,
                    color = muted,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}
