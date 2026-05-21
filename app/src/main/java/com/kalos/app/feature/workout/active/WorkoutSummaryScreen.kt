package com.kalos.app.feature.workout.active

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.domain.repository.WorkoutRepository
import com.kalos.app.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _log = MutableStateFlow<WorkoutLog?>(null)
    val log: StateFlow<WorkoutLog?> = _log

    fun loadLog(id: Long) {
        viewModelScope.launch { _log.value = workoutRepository.getLog(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    navController: NavController,
    logId: Long,
    viewModel: WorkoutSummaryViewModel = hiltViewModel(),
) {
    val log by viewModel.log.collectAsStateWithLifecycle()
    LaunchedEffect(logId) { viewModel.loadLog(logId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Résumé de séance") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack(Screen.Workout.route, false)
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (log == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val l = log!!
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(l.templateName.ifBlank { "Séance libre" },
                            style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Série ${i + 1} :",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${s.reps} × ${s.weightKg} kg",
                                            style = MaterialTheme.typography.bodySmall)
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
