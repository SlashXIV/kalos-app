package com.kalos.app.feature.workout.program

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kalos.app.R
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.WorkoutTemplate
import kotlinx.coroutines.flow.*

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
            title = { Text(stringResource(R.string.training_delete_program_title)) },
            text = { Text(stringResource(R.string.training_delete_program_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(); showDeleteDialog = false }) {
                    Text(stringResource(R.string.training_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    val pickerDaySnapshot = pickerDay
    if (pickerDaySnapshot != null) {
        ModalBottomSheet(onDismissRequest = { pickerDay = null }, shape = KalosShape.sheet) {
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                KalosButton(
                    stringResource(R.string.action_save),
                    onClick = { viewModel.save() },
                    enabled = state.name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(
                title = stringResource(if (isNew) R.string.training_new_program else R.string.training_edit_program),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            ) {
                if (!isNew && state.isCustom) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            stringResource(R.string.training_delete_program_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.training_program_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = { Text(stringResource(R.string.training_description)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            val weeks = pluralStringResource(R.plurals.training_weeks, state.durationWeeks, state.durationWeeks)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.training_duration), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { viewModel.setDurationWeeks(state.durationWeeks - 1) },
                    enabled = state.durationWeeks > 1,
                ) {
                    Icon(Icons.Filled.Remove, stringResource(R.string.training_duration_less))
                }
                Text(
                    weeks,
                    style = KalosTheme.type.metric,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 96.dp),
                )
                IconButton(
                    onClick = { viewModel.setDurationWeeks(state.durationWeeks + 1) },
                    enabled = state.durationWeeks < 52,
                ) {
                    Icon(Icons.Filled.Add, stringResource(R.string.training_duration_more))
                }
            }

            SectionLabel(stringResource(R.string.training_program_days))

            KalosCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                (1..7).forEach { day ->
                    DayRow(
                        dayName = dayName(day),
                        template = state.assignments[day],
                        onClick = { pickerDay = day },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayRow(dayName: String, template: WorkoutTemplate?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InstrumentLabel(dayName, Modifier.weight(1f))
        Text(
            template?.name ?: stringResource(R.string.training_rest_day),
            style = MaterialTheme.typography.bodyMedium,
            color = if (template != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
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
        SectionLabel(
            stringResource(R.string.training_pick_workout),
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        PickerRow(stringResource(R.string.training_rest_day), null, selected = current == null, onClick = onClear)
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).selectableGroup()) {
            items(templates, key = { it.id }) { template ->
                val count = template.exerciseCount
                PickerRow(
                    template.name,
                    if (count > 0) pluralStringResource(R.plurals.training_exercise_count_only, count, count) else null,
                    selected = current?.id == template.id,
                    onClick = { onSelect(template) },
                )
            }
        }
    }
}

@Composable
private fun PickerRow(title: String, detail: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(detail, style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}

private fun dayName(day: Int): String =
    if (day in 1..7) DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.FRENCH) else ""
