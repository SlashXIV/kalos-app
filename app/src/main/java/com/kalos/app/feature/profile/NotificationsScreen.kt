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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.kalos.app.R
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.component.ValueChip
import com.kalos.app.core.ui.theme.KalosTheme
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
            title = { Text(stringResource(R.string.me_notif_program_hour_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.me_hour_value, sliderHour.toInt()),
                        style = KalosTheme.type.metricLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally).clearAndSetSemantics { },
                    )
                    HourSlider(sliderHour, onChange = { sliderHour = it })
                    Text(
                        stringResource(R.string.me_notif_program_hour_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setNotifHour(sliderHour.toInt()); showHourDialog = false }) {
                    Text(stringResource(R.string.action_validate))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHourDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.me_settings_notifications),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )

            SectionLabel(stringResource(R.string.me_notif_smart), Modifier.padding(top = 4.dp))
            Text(
                stringResource(R.string.me_notif_smart_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SmartRemindersCard(state = state, viewModel = viewModel)

            SectionLabel(stringResource(R.string.me_notif_training), Modifier.padding(top = 8.dp))
            Text(
                stringResource(R.string.me_notif_training_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            KalosCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                val hour = stringResource(R.string.me_hour_value, state.notifHour)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable { showHourDialog = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.me_notif_send_hour), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(hour, style = KalosTheme.type.metric)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (state.programs.isEmpty()) {
                    Text(
                        stringResource(R.string.me_notif_no_program),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                    )
                } else {
                    state.programs.forEach { entry ->
                        ProgramReminderRow(
                            entry = entry,
                            onToggle = { viewModel.setProgramEnabled(entry.id, it) },
                            onDetail = { navController.navigate(Screen.ProgramDetail.route(entry.id)) },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.me_notif_program_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Curseur d'heure (6 h à 22 h) ; TalkBack lit l'heure, pas un pourcentage. */
@Composable
private fun HourSlider(value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val spoken = pluralStringResource(R.plurals.me_hour_spoken, value.toInt(), value.toInt())
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = 6f..22f,
        steps = 15,
        modifier = modifier.fillMaxWidth().semantics { stateDescription = spoken },
    )
}

@Composable
private fun ProgramReminderRow(
    entry: ProgramReminderEntry,
    onToggle: (Boolean) -> Unit,
    onDetail: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .toggleable(value = entry.reminderEnabled, role = Role.Switch, onValueChange = onToggle)
                .padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(entry.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
            if (entry.isActive) InstrumentLabel(stringResource(R.string.me_notif_active), color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            Switch(checked = entry.reminderEnabled, onCheckedChange = null)
        }
        IconButton(onClick = onDetail) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.me_notif_program_detail, entry.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SmartRemindersCard(state: NotificationsUiState, viewModel: NotificationsViewModel) {
    KalosCard(contentPadding = PaddingValues(vertical = 4.dp)) {
        SwitchRow(
            title = stringResource(R.string.me_notif_discipline),
            subtitle = stringResource(R.string.me_notif_discipline_sub),
            checked = state.smartEnabled,
            onCheckedChange = viewModel::setSmartEnabled,
            strong = true,
        )

        if (state.smartEnabled) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = stringResource(R.string.me_notif_nutrition),
                subtitle = stringResource(R.string.me_notif_nutrition_sub),
                checked = state.smartNutrition,
                onCheckedChange = viewModel::setSmartNutrition,
            )
            SwitchRow(
                title = stringResource(R.string.me_notif_activity),
                subtitle = stringResource(R.string.me_notif_activity_sub),
                checked = state.smartWorkout,
                onCheckedChange = viewModel::setSmartWorkout,
            )
            if (state.smartWorkout) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp),
                ) {
                    listOf(2, 3, 5).forEach { days ->
                        ValueChip(
                            text = stringResource(R.string.me_notif_days_short, days),
                            onClick = { viewModel.setInactivityDays(days) },
                            selected = state.inactivityDays == days,
                            contentDescription = pluralStringResource(R.plurals.me_notif_days_spoken, days, days),
                        )
                    }
                    Text(
                        stringResource(R.string.me_notif_without_session),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
            SwitchRow(
                title = stringResource(R.string.me_notif_hydration),
                subtitle = stringResource(R.string.me_notif_hydration_sub),
                checked = state.smartHydration,
                onCheckedChange = viewModel::setSmartHydration,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.me_notif_send_hour), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.me_hour_value, state.smartHour), style = KalosTheme.type.metric)
                }
                HourSlider(state.smartHour.toFloat(), onChange = { viewModel.setSmartHour(it.toInt()) })
                Text(
                    stringResource(R.string.me_notif_once_daily),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Interrupteur : toute la ligne bascule (rôle Switch), cible ≥ 56 dp. */
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    strong: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
