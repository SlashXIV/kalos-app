package com.kalos.app.feature.workout.active

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.ExerciseTrackingMode
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.domain.model.WorkoutSet
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.core.ui.component.EditWorkoutSetDialog
import com.kalos.app.core.ui.util.formatSecsAsDuration
import com.kalos.app.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SummaryPendingEdit(
    val set: WorkoutSet,
    val exerciseId: Long,
    val trackingMode: ExerciseTrackingMode,
)

data class WorkoutSummaryUiState(
    val log: WorkoutLog? = null,
    val pendingEdit: SummaryPendingEdit? = null,
    val isSavingEdit: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState

    fun loadLog(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(log = workoutRepository.getLog(id)) }
        }
    }

    fun openEditDialog(set: WorkoutSet, exerciseId: Long, trackingMode: ExerciseTrackingMode) {
        _uiState.update { it.copy(pendingEdit = SummaryPendingEdit(set, exerciseId, trackingMode)) }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(pendingEdit = null) }
    }

    fun saveSetEdit(newReps: Int, newWeightKg: Float, newDurationSecs: Int) {
        val pending = _uiState.value.pendingEdit ?: return
        val log = _uiState.value.log ?: return
        _uiState.update { it.copy(pendingEdit = null, isSavingEdit = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                workoutRepository.editSet(
                    logId = log.id,
                    exerciseId = pending.exerciseId,
                    set = pending.set.copy(
                        reps = newReps,
                        weightKg = newWeightKg,
                        durationSecs = newDurationSecs,
                    ),
                )
            }
                .onSuccess { finalLog ->
                    _uiState.update { it.copy(log = finalLog, isSavingEdit = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSavingEdit = false,
                            errorMessage = e.message?.takeIf { msg -> msg.isNotBlank() }
                                ?: "Échec de la modification de la série",
                        )
                    }
                }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Résumé de séance") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack(Screen.Workout.route, false)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (log == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val l = log
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(l.templateName.ifBlank { "Séance libre" },
                            style = MaterialTheme.typography.titleLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SummaryStat(Icons.Filled.Timer, formatDuration(l.durationSecs))
                            SummaryStat(Icons.Filled.FitnessCenter, "${l.exercises.size} exercices")
                            if (l.totalVolumeKg > 0) {
                                SummaryStat(Icons.Filled.MonitorWeight, "%.0f kg".format(l.totalVolumeKg))
                            }
                        }
                    }
                }
            }

            if (l.exercises.isNotEmpty()) {
                item {
                    Text("Détail des exercices", style = MaterialTheme.typography.titleSmall)
                }
                items(l.exercises) { le ->
                    val completedSets = le.sets.filter { it.isCompleted }
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(le.exercise.name, style = MaterialTheme.typography.titleSmall)
                            if (completedSets.isEmpty()) {
                                Text("Aucune série complétée",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                completedSets.forEachIndexed { i, s ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.openEditDialog(s, le.exercise.id, le.exercise.trackingMode) }
                                            .padding(vertical = 1.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Série ${i + 1} :",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(formatSetSummary(s, le.exercise.trackingMode),
                                                style = MaterialTheme.typography.bodySmall)
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Modifier",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { navController.popBackStack(Screen.Workout.route, false) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Retour aux séances") }
            }
        }
    }
}

@Composable
private fun SummaryStat(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatDuration(secs: Long): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    return if (h > 0) "${h}h%02d".format(m) else "${m}min"
}

private fun formatSetSummary(s: WorkoutSet, mode: ExerciseTrackingMode): String = when (mode) {
    ExerciseTrackingMode.REPS_WEIGHT ->
        "${s.reps} × ${s.weightKg} kg"
    ExerciseTrackingMode.DURATION ->
        formatSecsAsDuration(s.durationSecs).ifEmpty { "—" }
    ExerciseTrackingMode.DURATION_WEIGHT ->
        "${formatSecsAsDuration(s.durationSecs).ifEmpty { "—" }} @ ${s.weightKg} kg"
}
