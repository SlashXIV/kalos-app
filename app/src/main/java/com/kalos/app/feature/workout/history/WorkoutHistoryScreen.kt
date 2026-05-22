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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.WorkoutLog
import com.kalos.app.core.ui.component.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    navController: NavController,
    viewModel: WorkoutHistoryViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        WorkoutHistoryContent(
            viewModel = viewModel,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun WorkoutHistoryTabContent(
    viewModel: WorkoutHistoryViewModel = hiltViewModel(),
) {
    WorkoutHistoryContent(viewModel = viewModel)
}

@Composable
private fun WorkoutHistoryContent(
    viewModel: WorkoutHistoryViewModel,
    modifier: Modifier = Modifier,
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    if (logs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Aucune séance enregistrée",
                subtitle = "Terminez votre première séance pour voir votre historique",
                icon = Icons.Filled.History,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(logs, key = { it.id }) { log ->
                WorkoutLogCard(log)
            }
        }
    }
}

@Composable
private fun WorkoutLogCard(log: WorkoutLog) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    log.templateName.ifBlank { "Séance libre" },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    log.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LogStat(Icons.Filled.Timer, formatDuration(log.durationSecs))
                LogStat(Icons.Filled.FitnessCenter, "${log.exercises.size} exercices")
                val completed = log.exercises.sumOf { le -> le.sets.count { it.isCompleted } }
                if (completed > 0) LogStat(Icons.Filled.CheckCircle, "$completed séries")
                if (log.totalVolumeKg > 0) LogStat(Icons.Filled.MonitorWeight, "%.0f kg".format(log.totalVolumeKg))
            }
        }
    }
}

@Composable
private fun LogStat(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(secs: Long): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    return if (h > 0) "${h}h%02d".format(m) else "${m}min"
}
