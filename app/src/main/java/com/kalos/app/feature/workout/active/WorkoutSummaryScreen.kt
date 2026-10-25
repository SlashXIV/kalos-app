package com.kalos.app.feature.workout.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.component.EditWorkoutSetDialog
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.feature.workout.CompletedSetLine
import com.kalos.app.feature.workout.ExerciseSetsScale
import com.kalos.app.feature.workout.SessionStats
import com.kalos.app.navigation.Screen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kalos.app.core.ui.motion.InterruptibleMotion
import com.kalos.app.core.ui.motion.rememberHaptics

@Composable
fun WorkoutSummaryScreen(
    navController: NavController,
    logId: Long,
    viewModel: WorkoutSummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val log = state.log
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(logId) { viewModel.loadLog(logId) }
    val context = LocalContext.current
    // Moment « séance terminée » : joué une fois, à l'ouverture du récapitulatif (pas après rotation).
    var played by rememberSaveable { mutableStateOf(false) }
    val animate = remember { !played }
    val haptics = rememberHaptics()
    LaunchedEffect(log != null) {
        if (log == null || played) return@LaunchedEffect
        haptics.confirm()
        played = true
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { res ->
            snackbarHostState.showSnackbar(context.getString(res))
            viewModel.onErrorShown()
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

    val backToWorkouts = { navController.popBackStack(Screen.Workout.route, false); Unit }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        InterruptibleMotion(resetKey = Unit) {
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
                        title = log?.templateName?.ifBlank { null } ?: stringResource(R.string.training_free_workout),
                        eyebrow = stringResource(R.string.training_summary_eyebrow),
                        rising = animate && log != null,
                        navigation = {
                            IconButton(onClick = backToWorkouts) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                            }
                        },
                    )
                }
                if (log == null) {
                    item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                    return@LazyColumn
                }

                item(key = "stats") { SessionStats(log, Modifier.padding(vertical = 8.dp), countUp = animate) }

                if (log.exercises.isNotEmpty()) {
                    item(key = "label") { SectionLabel(stringResource(R.string.training_exercises_detail)) }
                    itemsIndexed(log.exercises, key = { _, le -> le.id }) { exIdx, le ->
                        val completedSets = le.sets.filter { it.isCompleted }
                        KalosCard {
                            Text(le.exercise.name, style = MaterialTheme.typography.titleMedium)
                            ExerciseSetsScale(le, revealDelayMs = if (animate) REVEAL_START_MS + exIdx * REVEAL_STAGGER_MS else null)
                            if (completedSets.isEmpty()) {
                                Text(
                                    stringResource(R.string.training_no_completed_set),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                completedSets.forEachIndexed { i, s ->
                                    CompletedSetLine(i + 1, s, le.exercise.trackingMode) {
                                        viewModel.openEditDialog(s, le.exercise.id, le.exercise.trackingMode)
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "back") {
                    KalosButton(
                        stringResource(R.string.training_back_to_workouts),
                        onClick = backToWorkouts,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

private const val REVEAL_START_MS = 300
private const val REVEAL_STAGGER_MS = 250
