package com.kalos.app.feature.workout.program

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

data class ProgramNotifState(
    val enabled: Boolean = false,
    val dayOf: Boolean = true,
    val dayBefore: Boolean = false,
)

@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _program = MutableStateFlow<TrainingProgram?>(null)
    val program: StateFlow<TrainingProgram?> = _program

    private val _notif = MutableStateFlow(ProgramNotifState())
    val notif: StateFlow<ProgramNotifState> = _notif

    fun load(id: Long) {
        viewModelScope.launch {
            _program.value = programRepository.getById(id)
            _notif.value = ProgramNotifState(
                enabled = reminderScheduler.isProgramEnabled(id),
                dayOf = reminderScheduler.isProgramDayOf(id),
                dayBefore = reminderScheduler.isProgramDayBefore(id),
            )
        }
    }

    fun activate() {
        val id = _program.value?.id ?: return
        viewModelScope.launch {
            programRepository.activate(id)
            reminderScheduler.schedule()
            _program.value = _program.value?.copy(isActive = true)
        }
    }

    fun setNotifEnabled(v: Boolean) {
        val id = _program.value?.id ?: return
        reminderScheduler.setProgramEnabled(id, v)
        _notif.value = _notif.value.copy(enabled = v)
    }

    fun setNotifDayOf(v: Boolean) {
        val id = _program.value?.id ?: return
        reminderScheduler.setProgramDayOf(id, v)
        _notif.value = _notif.value.copy(dayOf = v)
    }

    fun setNotifDayBefore(v: Boolean) {
        val id = _program.value?.id ?: return
        reminderScheduler.setProgramDayBefore(id, v)
        _notif.value = _notif.value.copy(dayBefore = v)
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
    val notif by viewModel.notif.collectAsStateWithLifecycle()
    LaunchedEffect(programId) { viewModel.load(programId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(program?.name ?: "Programme") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
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

            item {
                NotifSettingsCard(
                    notif = notif,
                    onEnabled = viewModel::setNotifEnabled,
                    onDayOf = viewModel::setNotifDayOf,
                    onDayBefore = viewModel::setNotifDayBefore,
                )
            }
        }
    }
}

@Composable
private fun NotifSettingsCard(
    notif: ProgramNotifState,
    onEnabled: (Boolean) -> Unit,
    onDayOf: (Boolean) -> Unit,
    onDayBefore: (Boolean) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text("Rappels", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Switch(checked = notif.enabled, onCheckedChange = onEnabled)
            }

            if (notif.enabled) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NotifToggleRow(
                    label = "Le jour même",
                    sublabel = "Notification le matin des jours de séance",
                    checked = notif.dayOf,
                    onCheckedChange = onDayOf,
                )
                NotifToggleRow(
                    label = "La veille",
                    sublabel = "Rappel la veille d'une séance planifiée",
                    checked = notif.dayBefore,
                    onCheckedChange = onDayBefore,
                )
            }
        }
    }
}

@Composable
private fun NotifToggleRow(
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(sublabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
        Text(pw.template?.let { if (it.exerciseCount > 0) "${it.exerciseCount} ex." else "" } ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
