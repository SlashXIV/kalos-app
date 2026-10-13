package com.kalos.app.feature.workout.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.domain.model.TrainingProgram
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.feature.workout.TRAINING_FAB_CLEARANCE
import com.kalos.app.navigation.Screen

@Composable
fun ProgramsScreen(
    navController: NavController,
    viewModel: ProgramViewModel = hiltViewModel(),
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.ProgramEditor.create()) }, shape = KalosShape.card) {
                Icon(Icons.Filled.Add, stringResource(R.string.training_new_program))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            ScreenHeader(
                title = stringResource(R.string.training_tab_programs),
                small = true,
                modifier = Modifier.padding(horizontal = 16.dp),
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
            ProgramsContent(
                navController = navController,
                viewModel = viewModel,
                bottomPadding = padding.calculateBottomPadding() + TRAINING_FAB_CLEARANCE,
            )
        }
    }
}

@Composable
fun ProgramsTabContent(
    navController: NavController,
    bottomPadding: Dp,
    viewModel: ProgramViewModel = hiltViewModel(),
) {
    ProgramsContent(navController = navController, viewModel = viewModel, bottomPadding = bottomPadding)
}

@Composable
private fun ProgramsContent(
    navController: NavController,
    viewModel: ProgramViewModel,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    var programToDelete by remember { mutableStateOf<TrainingProgram?>(null) }

    programToDelete?.let { program ->
        AlertDialog(
            onDismissRequest = { programToDelete = null },
            title = { Text(stringResource(R.string.training_delete_program_title)) },
            text = { Text(stringResource(R.string.training_delete_confirm_body, program.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(program.id)
                    programToDelete = null
                }) { Text(stringResource(R.string.training_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { programToDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (programs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.training_empty_programs),
                subtitle = stringResource(R.string.training_empty_programs_hint),
                icon = Icons.AutoMirrored.Filled.MenuBook,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(programs, key = { it.id }) { program ->
                ProgramCard(
                    program = program,
                    onActivate = { viewModel.activate(program.id) },
                    onDetail = { navController.navigate(Screen.ProgramDetail.route(program.id)) },
                    onEdit = if (program.isCustom) {
                        { navController.navigate(Screen.ProgramEditor.edit(program.id)) }
                    } else null,
                    // Tout programme inactif (fourni ou perso) peut être supprimé : nettoie les
                    // anciens programmes vides livrés avant la 3.12.
                    onDelete = if (!program.isActive) {
                        { programToDelete = program }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun ProgramCard(
    program: TrainingProgram,
    onActivate: () -> Unit,
    onDetail: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    KalosCard(contentPadding = PaddingValues(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (program.isActive) {
                    InstrumentLabel(stringResource(R.string.training_program_active), color = MaterialTheme.colorScheme.primary)
                }
                Text(program.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.training_program_rhythm, program.durationWeeks, program.durationWeeks, program.daysPerWeek) + " · " +
                        linkedWorkoutsText(program.workouts.size),
                    style = KalosTheme.type.metricSmall,
                    color = muted,
                )
                if (program.description.isNotBlank()) {
                    Text(program.description, style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 2)
                }
            }
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, stringResource(R.string.training_edit_named, program.name), tint = muted, modifier = Modifier.size(18.dp))
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, stringResource(R.string.training_delete_named, program.name), tint = muted, modifier = Modifier.size(18.dp))
                }
            }
        }
        val hasLinkedWorkouts = program.workouts.isNotEmpty()
        if (!hasLinkedWorkouts) {
            Text(
                stringResource(R.string.training_program_no_workouts_hint),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        Row(Modifier.padding(end = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KalosButton(stringResource(R.string.training_details), onClick = onDetail, ghost = true, modifier = Modifier.weight(1f))
            if (!program.isActive) {
                // Désactivé sans séance liée : le bouton ne promet pas un programme vide.
                KalosButton(
                    stringResource(R.string.training_activate),
                    onClick = onActivate,
                    enabled = hasLinkedWorkouts,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** « Aucune séance liée » / « 1 séance liée » / « 3 séances liées » (en français, 0 prend le singulier). */
@Composable
internal fun linkedWorkoutsText(n: Int): String =
    if (n == 0) stringResource(R.string.training_no_linked_workouts)
    else pluralStringResource(R.plurals.training_linked_workouts, n, n)
