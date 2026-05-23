package com.kalos.app.feature.workout.history

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.LogExercise
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class WorkoutLogDetailUiState(
    val log: WorkoutLog? = null,
    val maxWeights: Map<Long, Float?> = emptyMap(),
    val isLoading: Boolean = true,
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
                val maxWeights = log.exercises.associate { le ->
                    le.exercise.id to workoutRepository.getMaxWeight(le.exercise.id)
                }
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
    LaunchedEffect(logId) { viewModel.load(logId) }

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
        }
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
                modifier = Modifier.padding(padding),
            )
        }
    }
}

// ─── Content ─────────────────────────────────────────────────────────────────

@Composable
private fun LogDetailContent(
    log: WorkoutLog,
    maxWeights: Map<Long, Float?>,
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

            if (completedSets.isEmpty()) {
                Text(
                    "Aucune série complétée",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                completedSets.forEachIndexed { i, set ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Série ${i + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            formatSet(set.reps, set.weightKg),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        )
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
