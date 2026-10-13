package com.kalos.app.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.domain.model.WorkoutTemplate
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.Graduation
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.WorkoutTemplateCard
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatElapsedSince
import com.kalos.app.feature.workout.history.WorkoutHistoryTabContent
import com.kalos.app.feature.workout.program.ProgramsTabContent
import com.kalos.app.navigation.Screen
import kotlinx.coroutines.launch

/** Marge sous la dernière carte : le FAB (56 dp + 16 dp de marge) ne recouvre plus son bouton. */
internal val TRAINING_FAB_CLEARANCE = 96.dp

@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.training_tab_workouts),
        stringResource(R.string.training_tab_programs),
        stringResource(R.string.training_tab_history),
    )
    var templateToDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var showAbandonConfirm by remember { mutableStateOf(false) }
    var showActivityDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showActivityDialog) {
        LogActivityDialog(
            onDismiss = { showActivityDialog = false },
            onSaved = {
                showActivityDialog = false
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.training_activity_saved)) }
            },
        )
    }

    if (showAbandonConfirm) {
        AlertDialog(
            onDismissRequest = { showAbandonConfirm = false },
            title = { Text(stringResource(R.string.training_abandon_title)) },
            text = { Text(stringResource(R.string.training_abandon_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showAbandonConfirm = false
                    viewModel.discardDraft()
                }) { Text(stringResource(R.string.training_abandon_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(
                    onClick = { navController.navigate(Screen.WorkoutBuilder.create()) },
                    shape = KalosShape.card,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.training_new_workout))
                }
                1 -> FloatingActionButton(
                    onClick = { navController.navigate(Screen.ProgramEditor.create()) },
                    shape = KalosShape.card,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.training_new_program))
                }
            }
        },
    ) { padding ->
        val bottom = padding.calculateBottomPadding()
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScreenHeader(title = stringResource(R.string.training_title)) {
                    IconButton(onClick = { showActivityDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = stringResource(R.string.training_log_activity))
                    }
                    IconButton(onClick = { navController.navigate(Screen.ExerciseCatalog.standalone()) }) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = stringResource(R.string.training_catalog))
                    }
                }
                state.draftBanner?.let { banner ->
                    ActiveWorkoutBanner(
                        banner = banner,
                        onResume = { navController.navigate(Screen.ActiveWorkout.route(banner.templateId)) },
                        onAbandon = { showAbandonConfirm = true },
                    )
                }
                TrainingTabs(tabs, selectedTab, onSelect = { selectedTab = it })
            }

            when (selectedTab) {
                0 -> WorkoutsTab(
                    templates = state.templates,
                    bottomPadding = bottom + TRAINING_FAB_CLEARANCE,
                    onStart = { navController.navigate(Screen.ActiveWorkout.route(it)) },
                    onEdit = { navController.navigate(Screen.WorkoutBuilder.edit(it)) },
                    onDelete = { template -> templateToDelete = template },
                )
                1 -> ProgramsTabContent(navController = navController, bottomPadding = bottom + TRAINING_FAB_CLEARANCE)
                2 -> WorkoutHistoryTabContent(navController = navController, bottomPadding = bottom + 24.dp)
            }
        }
    }

    templateToDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text(stringResource(R.string.training_delete_workout_title)) },
            text = { Text(stringResource(R.string.training_delete_confirm_body, template.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTemplate(template)
                        templateToDelete = null
                    },
                ) { Text(stringResource(R.string.training_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/**
 * Onglets d'instrument : libellés en capitales espacées, l'onglet actif est souligné d'une
 * graduation. Rôle « onglet » et état sélectionné pour TalkBack.
 */
@Composable
private fun TrainingTabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        titles.forEachIndexed { i, title ->
            val active = i == selected
            Column(
                Modifier
                    .heightIn(min = 48.dp)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(i) }),
                verticalArrangement = Arrangement.Bottom,
            ) {
                InstrumentLabel(
                    title,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                )
                if (active) {
                    Graduation(MaterialTheme.colorScheme.primary, Modifier.width(44.dp).height(8.dp))
                } else {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutBanner(
    banner: DraftBannerState,
    onResume: () -> Unit,
    onAbandon: () -> Unit,
) {
    val title = banner.templateName.ifBlank { stringResource(R.string.training_free_workout) }
    // Brouillon de plus de 24 h : ton neutre, la séance est très probablement abandonnée.
    val headline = stringResource(
        if (banner.isStale) R.string.training_banner_stale else R.string.training_banner_active,
        title,
    )
    KalosCard(onClick = onResume, contentPadding = PaddingValues(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                InstrumentLabel(
                    headline,
                    color = if (banner.isStale) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                )
                Text(
                    pluralStringResource(
                        R.plurals.training_banner_detail, banner.exerciseCount,
                        banner.exerciseCount, formatElapsedSince(banner.lastActiveAt),
                    ),
                    style = KalosTheme.type.metricSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onAbandon) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.training_abandon_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            TextButton(onClick = onResume) { Text(stringResource(R.string.training_resume)) }
        }
    }
}

@Composable
private fun WorkoutsTab(
    templates: List<WorkoutTemplate>,
    bottomPadding: Dp,
    onStart: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (WorkoutTemplate) -> Unit,
) {
    if (templates.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.training_empty_workouts),
                subtitle = stringResource(R.string.training_empty_workouts_hint),
                icon = Icons.Filled.FitnessCenter,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(templates, key = { it.id }) { template ->
                WorkoutTemplateCard(
                    template = template,
                    onStart = { onStart(template.id) },
                    onEdit = { onEdit(template.id) },
                    onDelete = { onDelete(template) },
                )
            }
        }
    }
}
