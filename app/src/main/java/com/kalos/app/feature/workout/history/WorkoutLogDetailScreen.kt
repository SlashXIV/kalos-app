package com.kalos.app.feature.workout.history

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.ExerciseStatus
import com.kalos.app.core.domain.model.LogExercise
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.domain.model.WorkoutSet
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.core.ui.component.EditWorkoutSetDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class PendingSetEdit(val set: WorkoutSet, val exerciseId: Long)

data class WorkoutLogDetailUiState(
    val log: WorkoutLog? = null,
    val maxWeights: Map<Long, Float?> = emptyMap(),
    val isLoading: Boolean = true,
    val pendingEdit: PendingSetEdit? = null,
    val isSavingEdit: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class WorkoutLogDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutLogDetailUiState())
    val uiState: StateFlow<WorkoutLogDetailUiState> = _uiState.asStateFlow()

    fun load(logId: Long) {
        viewModelScope.launch {
            val log = workoutRepository.getLog(logId)
            if (log != null) {
                val maxWeights = workoutRepository.getMaxWeightsForExercises(
                    log.exercises.map { it.exercise.id }
                )
                _uiState.value = WorkoutLogDetailUiState(
                    log = log,
                    maxWeights = maxWeights,
                    isLoading = false,
                )
            } else {
                _uiState.value = WorkoutLogDetailUiState(isLoading = false)
            }
        }
    }

    fun openEditDialog(set: WorkoutSet, exerciseId: Long) {
        _uiState.update { it.copy(pendingEdit = PendingSetEdit(set, exerciseId)) }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(pendingEdit = null) }
    }

    fun saveSetEdit(newReps: Int, newWeightKg: Float) {
        val pending = _uiState.value.pendingEdit ?: return
        val log = _uiState.value.log ?: return
        _uiState.update { it.copy(pendingEdit = null, isSavingEdit = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val finalLog = workoutRepository.editSet(
                    logId = log.id,
                    exerciseId = pending.exerciseId,
                    set = pending.set.copy(reps = newReps, weightKg = newWeightKg),
                )
                val maxWeights = finalLog?.let {
                    workoutRepository.getMaxWeightsForExercises(it.exercises.map { le -> le.exercise.id })
                } ?: emptyMap()
                finalLog to maxWeights
            }
                .onSuccess { (finalLog, maxWeights) ->
                    _uiState.update {
                        it.copy(log = finalLog, maxWeights = maxWeights, isSavingEdit = false)
                    }
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

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLogDetailScreen(
    navController: NavController,
    logId: Long,
    viewModel: WorkoutLogDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(logId) { viewModel.load(logId) }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.log?.let { "Séance du ${formatLogDateShort(it.date)}" } ?: "Détail de séance")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.log == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Séance introuvable",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LogDetailContent(
                log = state.log!!,
                maxWeights = state.maxWeights,
                onEditSet = viewModel::openEditDialog,
                modifier = Modifier.padding(padding),
            )
        }
        state.pendingEdit?.let { pending ->
            EditWorkoutSetDialog(
                set = pending.set,
                onDismiss = viewModel::dismissEditDialog,
                onConfirm = { reps, weight -> viewModel.saveSetEdit(reps, weight) },
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
private fun LogDetailContent(
    log: WorkoutLog,
    maxWeights: Map<Long, Float?>,
    onEditSet: (WorkoutSet, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SessionHeaderCard(log) }

        if (log.exercises.isNotEmpty()) {
            item {
                Text(
                    "Exercices",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(log.exercises, key = { it.id }) { le ->
                ExerciseDetailCard(
                    logExercise = le,
                    maxWeight = maxWeights[le.exercise.id],
                    onEditSet = { set -> onEditSet(set, le.exercise.id) },
                )
            }
        }
    }
}

@Composable
private fun SessionHeaderCard(log: WorkoutLog) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                log.templateName.ifBlank { "Séance libre" },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                formatLogDateLong(log.date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                if (log.durationSecs > 0) {
                    DetailStat(Icons.Filled.Timer, formatDuration(log.durationSecs))
                }
                val plural = { n: Int, s: String -> "$n $s${if (n > 1) "s" else ""}" }
                DetailStat(Icons.Filled.FitnessCenter, plural(log.exercises.size, "exercice"))
                val completedSets = log.exercises.sumOf { le -> le.sets.count { it.isCompleted } }
                if (completedSets > 0) {
                    DetailStat(Icons.Filled.CheckCircle, plural(completedSets, "série"))
                }
                if (log.totalVolumeKg > 0f) {
                    DetailStat(Icons.Filled.MonitorWeight, "%.0f kg".format(log.totalVolumeKg))
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailCard(
    logExercise: LogExercise,
    maxWeight: Float?,
    onEditSet: (WorkoutSet) -> Unit = {},
) {
    val completedSets = logExercise.sets.filter { it.isCompleted }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(logExercise.exercise.name, style = MaterialTheme.typography.titleSmall)
                    if (logExercise.exercise.primaryMuscle.isNotBlank()) {
                        Text(
                            logExercise.exercise.primaryMuscle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (maxWeight != null && maxWeight > 0f) {
                    Text(
                        "PR : ${"%.1f".format(maxWeight)} kg",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Status badge — only shown for non-PLANNED statuses
            if (logExercise.status != ExerciseStatus.PLANNED) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (logExercise.status) {
                        ExerciseStatus.SKIPPED -> StatusBadge(
                            "Passé",
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ExerciseStatus.REPLACED -> {
                            StatusBadge(
                                "Remplacé",
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            if (logExercise.replacedExerciseName.isNotBlank()) {
                                Text(
                                    "à la place de : ${logExercise.replacedExerciseName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        ExerciseStatus.ADDED -> StatusBadge(
                            "Hors programme",
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        else -> {}
                    }
                }
            }

            if (logExercise.status == ExerciseStatus.SKIPPED) {
                // No sets to display for a skipped exercise
            } else if (completedSets.isEmpty()) {
                Text(
                    "Aucune série complétée",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                completedSets.forEachIndexed { i, set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditSet(set) }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Série ${i + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                formatSet(set.reps, set.weightKg),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            )
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Modifier",
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            )
                        }
                    }
                }
                if (logExercise.totalVolume > 0f) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            "Volume : ${"%.0f".format(logExercise.totalVolume)} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, containerColor: Color, contentColor: Color) {
    Surface(color = containerColor, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun DetailStat(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatDuration(secs: Long): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    return if (h > 0) "${h}h%02d".format(m) else "${m}min"
}

private fun formatSet(reps: Int, weightKg: Float): String = when {
    reps > 0 && weightKg > 0f -> "$reps × ${"%.1f".format(weightKg)} kg"
    reps > 0 -> "$reps rép."
    weightKg > 0f -> "${"%.1f".format(weightKg)} kg"
    else -> "—"
}

fun formatLogDateShort(dateStr: String): String {
    val d = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    return when (d) {
        today -> "aujourd'hui"
        today.minusDays(1) -> "hier"
        else -> {
            val pattern = if (d.year == today.year) "d MMM" else "d MMM yyyy"
            d.format(DateTimeFormatter.ofPattern(pattern, Locale.FRENCH))
        }
    }
}

fun formatLogDateLong(dateStr: String): String {
    val d = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    return when (d) {
        today -> "Aujourd'hui"
        today.minusDays(1) -> "Hier"
        else -> d.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH))
            .replaceFirstChar { it.uppercase() }
    }
}
