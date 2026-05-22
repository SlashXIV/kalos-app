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
import com.kalos.app.navigation.Screen

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
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { navController.navigate(Screen.WorkoutBuilder.create()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nouvelle séance")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(title) },
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
                2 -> WorkoutHistoryTabContent()
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
