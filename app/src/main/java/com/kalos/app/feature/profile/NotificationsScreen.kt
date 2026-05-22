package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.domain.repository.ProgramRepository
import com.kalos.app.core.notification.IntelligentReminderScheduler
import com.kalos.app.core.notification.ReminderScheduler
import com.kalos.app.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgramReminderEntry(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val reminderEnabled: Boolean,
)

data class NotificationsUiState(
    val programs: List<ProgramReminderEntry> = emptyList(),
    val notifHour: Int = 8,
    val smartEnabled: Boolean = false,
    val smartNutrition: Boolean = true,
    val smartWorkout: Boolean = true,
    val smartHydration: Boolean = false,
    val inactivityDays: Int = 3,
    val smartHour: Int = 20,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val reminderScheduler: ReminderScheduler,
    private val smartScheduler: IntelligentReminderScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(
        NotificationsUiState(
            notifHour = reminderScheduler.getNotifHour(),
            smartEnabled = smartScheduler.isEnabled(),
            smartNutrition = smartScheduler.isNutritionEnabled(),
            smartWorkout = smartScheduler.isWorkoutEnabled(),
            smartHydration = smartScheduler.isHydrationEnabled(),
            inactivityDays = smartScheduler.getInactivityDays(),
            smartHour = smartScheduler.getHour(),
        )
    )
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            programRepository.getAll().collect { programs ->
                _state.update { s ->
                    s.copy(programs = programs.map { p ->
                        ProgramReminderEntry(
                            id = p.id,
                            name = p.name,
                            isActive = p.isActive,
                            reminderEnabled = reminderScheduler.isProgramEnabled(p.id),
                        )
                    })
                }
            }
        }
    }

    fun setNotifHour(hour: Int) {
        reminderScheduler.setNotifHour(hour)
        _state.update { it.copy(notifHour = hour) }
    }

    fun setProgramEnabled(id: Long, v: Boolean) {
        reminderScheduler.setProgramEnabled(id, v)
        _state.update { s ->
            s.copy(programs = s.programs.map { if (it.id == id) it.copy(reminderEnabled = v) else it })
        }
    }

    fun setSmartEnabled(v: Boolean) { smartScheduler.setEnabled(v); _state.update { it.copy(smartEnabled = v) } }
    fun setSmartNutrition(v: Boolean) { smartScheduler.setNutritionEnabled(v); _state.update { it.copy(smartNutrition = v) } }
    fun setSmartWorkout(v: Boolean) { smartScheduler.setWorkoutEnabled(v); _state.update { it.copy(smartWorkout = v) } }
    fun setSmartHydration(v: Boolean) { smartScheduler.setHydrationEnabled(v); _state.update { it.copy(smartHydration = v) } }
    fun setInactivityDays(days: Int) { smartScheduler.setInactivityDays(days); _state.update { it.copy(inactivityDays = days) } }
    fun setSmartHour(hour: Int) { smartScheduler.setHour(hour); _state.update { it.copy(smartHour = hour) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showHourDialog by remember { mutableStateOf(false) }

    if (showHourDialog) {
        var sliderHour by remember { mutableFloatStateOf(state.notifHour.toFloat()) }
        AlertDialog(
            onDismissRequest = { showHourDialog = false },
            title = { Text("Heure des rappels programme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "${sliderHour.toInt()}h00",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    Slider(value = sliderHour, onValueChange = { sliderHour = it }, valueRange = 6f..22f, steps = 15)
                    Text(
                        "S'applique à toutes les séances planifiées",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setNotifHour(sliderHour.toInt()); showHourDialog = false }) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHourDialog = false }) { Text("Annuler") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Smart reminders ──────────────────────────────────────────────
            Text(
                "Rappels intelligents",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Rappels quotidiens basés sur vos habitudes — nutrition, sport, hydratation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SmartRemindersCard(state = state, viewModel = viewModel)

            HorizontalDivider()

            // ── Programme reminders ──────────────────────────────────────────
            Text(
                "Rappels d'entraînement",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Notifications liées à vos programmes de sport planifiés.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Hour picker
            ElevatedCard(
                onClick = { showHourDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Heure d'envoi", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${state.notifHour}h00",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Programs list
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (state.programs.isEmpty()) {
                        Text(
                            "Aucun programme créé",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        state.programs.forEachIndexed { index, entry ->
                            ProgramReminderRow(
                                entry = entry,
                                onToggle = { viewModel.setProgramEnabled(entry.id, it) },
                                onDetail = { navController.navigate(Screen.ProgramDetail.route(entry.id)) },
                            )
                            if (index < state.programs.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        "Pour configurer le rappel jour J ou la veille d'une séance, " +
                            "ouvrez la fiche du programme concerné.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramReminderRow(
    entry: ProgramReminderEntry,
    onToggle: (Boolean) -> Unit,
    onDetail: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                )
                if (entry.isActive) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            "Actif",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
        TextButton(
            onClick = onDetail,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text("Détail", style = MaterialTheme.typography.labelSmall)
        }
        Switch(
            checked = entry.reminderEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}

@Composable
private fun SmartRemindersCard(state: NotificationsUiState, viewModel: NotificationsViewModel) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Master switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = if (state.smartEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Column {
                        Text(
                            "Rappels de discipline",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Text(
                            "Nutrition, sport, hydratation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = state.smartEnabled,
                    onCheckedChange = viewModel::setSmartEnabled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }

            if (state.smartEnabled) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(12.dp))

                SmartToggleRow(
                    title = "Nutrition",
                    subtitle = "Si aucun repas enregistré aujourd'hui",
                    checked = state.smartNutrition,
                    onCheckedChange = viewModel::setSmartNutrition,
                )
                Spacer(Modifier.height(10.dp))

                SmartToggleRow(
                    title = "Activité physique",
                    subtitle = "Si inactif depuis trop longtemps",
                    checked = state.smartWorkout,
                    onCheckedChange = viewModel::setSmartWorkout,
                )

                if (state.smartWorkout) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        listOf(2, 3, 5).forEach { days ->
                            FilterChip(
                                selected = state.inactivityDays == days,
                                onClick = { viewModel.setInactivityDays(days) },
                                label = { Text("$days j", style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                        Text(
                            "sans séance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                SmartToggleRow(
                    title = "Hydratation",
                    subtitle = "Si < 50 % de l'objectif eau atteint",
                    checked = state.smartHydration,
                    onCheckedChange = viewModel::setSmartHydration,
                )

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Heure d'envoi", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${state.smartHour}h00",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = state.smartHour.toFloat(),
                    onValueChange = { viewModel.setSmartHour(it.toInt()) },
                    valueRange = 6f..22f,
                    steps = 15,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Envoyés une fois par jour à l'heure choisie",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SmartToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}
