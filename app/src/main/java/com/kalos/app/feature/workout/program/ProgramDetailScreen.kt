package com.kalos.app.feature.workout.program

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.ProgramWorkout
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _program = MutableStateFlow<TrainingProgram?>(null)
    val program: StateFlow<TrainingProgram?> = _program

    fun load(id: Long) {
        viewModelScope.launch { _program.value = programRepository.getById(id) }
    }

    fun activate() {
        val id = _program.value?.id ?: return
        viewModelScope.launch {
            programRepository.activate(id)
            reminderScheduler.schedule()
            _program.value = _program.value?.copy(isActive = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    navController: NavController,
    programId: Long,
    viewModel: ProgramDetailViewModel = hiltViewModel(),
) {
    val program by viewModel.program.collectAsStateWithLifecycle()
    LaunchedEffect(programId) { viewModel.load(programId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(program?.name ?: "Programme") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (program?.isActive == false) {
                        TextButton(onClick = viewModel::activate) {
                            Text("Activer")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (program == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val p = program!!
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                if (p.description.isNotBlank()) {
                    Text(p.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("${p.durationWeeks} semaines") })
                    AssistChip(onClick = {}, label = { Text("${p.daysPerWeek} j/sem") })
                    if (p.isActive) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Actif") },
                            leadingIcon = { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp)) },
                        )
                    }
                }
            }

            if (p.workouts.isNotEmpty()) {
                item {
                    Text("Programme hebdomadaire", style = MaterialTheme.typography.titleSmall)
                }
                val days = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
                items(p.workouts.sortedWith(compareBy({ it.weekNumber }, { it.dayOfWeek }))) { pw ->
                    ProgramWorkoutRow(pw, days)
                }
            } else {
                item {
                    Text("Aucune séance planifiée.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ProgramWorkoutRow(pw: ProgramWorkout, days: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    days.getOrElse(pw.dayOfWeek - 1) { "?" }.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(pw.template?.name ?: "Séance libre",
                style = MaterialTheme.typography.bodyMedium)
            if (pw.weekNumber > 1) {
                Text("Semaine ${pw.weekNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(pw.template?.let { "${it.exercises.size} ex." } ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
