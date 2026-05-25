package com.kalos.app.feature.workout.program

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.ProgramWorkout
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.domain.model.WorkoutTemplate
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramEditorState(
    val name: String = "",
    val description: String = "",
    val durationWeeks: Int = 4,
    val assignments: Map<Int, WorkoutTemplate?> = (1..7).associateWith { null },
    val templates: List<WorkoutTemplate> = emptyList(),
    val isCustom: Boolean = true,
    val isSaved: Boolean = false,
)

@HiltViewModel
class ProgramEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val programRepository: ProgramRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val programId: Long = savedStateHandle["programId"] ?: -1L

    private val _state = MutableStateFlow(ProgramEditorState())
    val state: StateFlow<ProgramEditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            workoutRepository.getTemplates().collect { templates ->
                _state.update { it.copy(templates = templates) }
            }
        }
        if (programId > 0L) {
            viewModelScope.launch {
                programRepository.getById(programId)?.let { program ->
                    _state.update {
                        it.copy(
                            name = program.name,
                            description = program.description,
                            durationWeeks = program.durationWeeks,
                            isCustom = program.isCustom,
                            assignments = (1..7).associateWith { day ->
                                program.workouts.firstOrNull { pw -> pw.dayOfWeek == day }?.template
                            },
                        )
                    }
                }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun setDurationWeeks(v: Int) = _state.update { it.copy(durationWeeks = v.coerceIn(1, 52)) }

    fun assign(day: Int, template: WorkoutTemplate?) = _state.update {
        it.copy(assignments = it.assignments.toMutableMap().also { m -> m[day] = template })
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        viewModelScope.launch {
            val workouts = s.assignments.entries
                .filter { it.value != null }
                .map { (day, template) ->
                    ProgramWorkout(
                        programId = if (programId > 0L) programId else 0L,
                        template = template,
                        dayOfWeek = day,
                    )
                }
            programRepository.save(
                TrainingProgram(
                    id = if (programId > 0L) programId else 0L,
                    name = s.name.trim(),
                    description = s.description.trim(),
                    durationWeeks = s.durationWeeks,
                    daysPerWeek = workouts.size,
                    isCustom = true,
                    workouts = workouts,
                )
            )
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        if (programId <= 0L) return
        viewModelScope.launch {
            val s = _state.value
            programRepository.delete(TrainingProgram(id = programId, name = s.name, isCustom = true))
            _state.update { it.copy(isSaved = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramEditorScreen(
    navController: NavController,
    programId: Long,
    viewModel: ProgramEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) navController.popBackStack()
    }

    var pickerDay by remember { mutableStateOf<Int?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer le programme") },
            text = { Text("Cette action est irréversible. Le programme sera définitivement supprimé.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(); showDeleteDialog = false }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            },
        )
    }

    val pickerDaySnapshot = pickerDay
    if (pickerDaySnapshot != null) {
        ModalBottomSheet(onDismissRequest = { pickerDay = null }) {
            TemplatePicker(
                templates = state.templates,
                current = state.assignments[pickerDaySnapshot],
                onSelect = { template -> viewModel.assign(pickerDaySnapshot, template); pickerDay = null },
                onClear = { viewModel.assign(pickerDaySnapshot, null); pickerDay = null },
            )
        }
    }

    val isNew = programId <= 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Nouveau programme" else "Modifier le programme") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (!isNew && state.isCustom) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                "Supprimer",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Button(
                    onClick = { viewModel.save() },
                    enabled = state.name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("Enregistrer")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Nom du programme *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Durée", style = MaterialTheme.typography.bodyLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = { viewModel.setDurationWeeks(state.durationWeeks - 1) },
                        enabled = state.durationWeeks > 1,
                    ) {
                        Icon(Icons.Filled.Remove, "-")
                    }
                    Text(
                        "${state.durationWeeks} sem.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.widthIn(min = 60.dp),
                    )
                    IconButton(
                        onClick = { viewModel.setDurationWeeks(state.durationWeeks + 1) },
                        enabled = state.durationWeeks < 52,
                    ) {
                        Icon(Icons.Filled.Add, "+")
                    }
                }
            }

            HorizontalDivider()

            Text(
                "Séances par jour",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    (1..7).forEach { day ->
                        DayRow(
                            dayName = dayName(day),
                            template = state.assignments[day],
                            onClick = { pickerDay = day },
                        )
                        if (day < 7) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DayRow(dayName: String, template: WorkoutTemplate?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(dayName, style = MaterialTheme.typography.bodyLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (template != null) {
                Text(
                    template.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    "Repos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun TemplatePicker(
    templates: List<WorkoutTemplate>,
    current: WorkoutTemplate?,
    onSelect: (WorkoutTemplate) -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
    ) {
        Text(
            "Choisir une séance",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Repos") },
            leadingContent = {
                Icon(
                    Icons.Filled.Hotel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = if (current == null) {
                { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
            } else null,
            modifier = Modifier.clickable(onClick = onClear),
        )
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
            items(templates, key = { it.id }) { template ->
                ListItem(
                    headlineContent = { Text(template.name) },
                    supportingContent = {
                        val count = template.exerciseCount
                        if (count > 0) Text("$count exercice${if (count > 1) "s" else ""}")
                    },
                    trailingContent = if (current?.id == template.id) {
                        { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    modifier = Modifier.clickable { onSelect(template) },
                )
                HorizontalDivider()
            }
        }
    }
}

private fun dayName(day: Int): String = when (day) {
    1 -> "Lundi"
    2 -> "Mardi"
    3 -> "Mercredi"
    4 -> "Jeudi"
    5 -> "Vendredi"
    6 -> "Samedi"
    7 -> "Dimanche"
    else -> ""
}
