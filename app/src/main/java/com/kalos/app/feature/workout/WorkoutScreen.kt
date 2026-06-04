package com.kalos.app.feature.workout

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.WorkoutTemplate
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.WorkoutTemplateCard
import com.kalos.app.feature.workout.history.WorkoutHistoryTabContent
import com.kalos.app.feature.workout.program.ProgramsTabContent
import com.kalos.app.core.ui.util.formatElapsedSince
import com.kalos.app.navigation.Screen
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Séances", "Programmes", "Historique")
    var templateToDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var showAbandonConfirm by remember { mutableStateOf(false) }

    if (showAbandonConfirm) {
        AlertDialog(
            onDismissRequest = { showAbandonConfirm = false },
            title = { Text("Abandonner la séance ?") },
            text = { Text("Les séries saisies dans cette séance interrompue seront définitivement perdues.") },
            confirmButton = {
                TextButton(onClick = {
                    showAbandonConfirm = false
                    viewModel.discardDraft()
                }) { Text("Abandonner", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonConfirm = false }) { Text("Annuler") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sport") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.ExerciseCatalog.standalone()) }) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = "Catalogue d'exercices")
                    }
                }
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(onClick = { navController.navigate(Screen.WorkoutBuilder.create()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nouvelle séance")
                }
                1 -> FloatingActionButton(onClick = { navController.navigate(Screen.ProgramEditor.create()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nouveau programme")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            state.draftBanner?.let { banner ->
                ActiveWorkoutBanner(
                    banner = banner,
                    onResume = {
                        navController.navigate(Screen.ActiveWorkout.route(banner.templateId))
                    },
                    onAbandon = if (banner.isStale) {
                        { showAbandonConfirm = true }
                    } else null,
                )
            }
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(title) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (selectedTab) {
                0 -> WorkoutsTab(
                    templates = state.templates,
                    onStart = { navController.navigate(Screen.ActiveWorkout.route(it)) },
                    onEdit = { navController.navigate(Screen.WorkoutBuilder.edit(it)) },
                    onDelete = { template -> templateToDelete = template },
                )
                1 -> ProgramsTabContent(navController = navController)
                2 -> WorkoutHistoryTabContent(navController = navController)
            }
        }
    }

    templateToDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("Supprimer la séance") },
            text = { Text("Voulez-vous vraiment supprimer « ${template.name} » ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTemplate(template)
                        templateToDelete = null
                    }
                ) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun ActiveWorkoutBanner(
    banner: DraftBannerState,
    onResume: () -> Unit,
    onAbandon: (() -> Unit)? = null,
) {
    val elapsedLabel = formatElapsedSince(banner.startedAt)
    val title = banner.templateName.ifBlank { "Séance libre" }
    val plural = if (banner.exerciseCount > 1) "exercices" else "exercice"

    // Stale draft (> 24h): neutral tone instead of the engaging green — the session is
    // almost certainly abandoned, the banner should inform, not invite.
    val containerColor = if (banner.isStale) MaterialTheme.colorScheme.surfaceVariant
                         else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (banner.isStale) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onPrimaryContainer
    val headline = if (banner.isStale) "Séance interrompue · $title"
                   else "Séance en cours · $title"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onResume),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Timer,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    headline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
                Text(
                    "${banner.exerciseCount} $plural · $elapsedLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f),
                )
            }
            if (onAbandon != null) {
                TextButton(
                    onClick = onAbandon,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Abandonner")
                }
            }
            TextButton(
                onClick = onResume,
                colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
            ) {
                Text("Reprendre")
            }
        }
    }
}

@Composable
private fun WorkoutsTab(
    templates: List<WorkoutTemplate>,
    onStart: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (WorkoutTemplate) -> Unit,
) {
    if (templates.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Aucune séance",
                subtitle = "Créez votre première séance d'entraînement en touchant +",
                icon = Icons.Filled.FitnessCenter,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
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
