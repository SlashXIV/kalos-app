package com.kalos.app.feature.workout.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.ExerciseListItem
import com.kalos.app.core.ui.component.KalosSearchBar
import com.kalos.app.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCatalogScreen(
    navController: NavController,
    templateId: Long,
    viewModel: ExerciseCatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val muscleFilters = listOf("Pectoraux","Grand dorsal","Épaules","Biceps","Triceps","Quadriceps","Ischio-jambiers","Fessiers","Abdominaux","Mollets","Cardio","Trapèzes","Érecteurs")
    val typeFilters = listOf("Musculation","Poids du corps","Cardio","HIIT","Mobilité")

    // Detect builder context from back stack — works for both new (templateId=-1) and edit
    val inBuilderContext = remember {
        navController.previousBackStackEntry?.destination?.route
            ?.startsWith("workout/builder") == true
    }

    // When ExerciseDetail (opened from builder) passes back an exercise ID, propagate to builder
    LaunchedEffect(Unit) {
        val handle = navController.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        handle.getStateFlow<Long?>("added_exercise_id", null).collect { exerciseId ->
            if (exerciseId != null && inBuilderContext) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("added_exercise_id", exerciseId)
                handle.remove<Long>("added_exercise_id")
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (inBuilderContext) "Ajouter un exercice" else "Catalogue d'exercices")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            KalosSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Rechercher un exercice…",
            )
            Spacer(Modifier.height(8.dp))

            // Muscle filter chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                muscleFilters.forEach { muscle ->
                    FilterChip(
                        selected = state.selectedMuscle == muscle,
                        onClick = { viewModel.onMuscleChange(muscle) },
                        label = { Text(muscle, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            // Type filter chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                typeFilters.forEach { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (state.exercises.isEmpty()) {
                EmptyState(title = "Aucun exercice trouvé", subtitle = "Modifiez vos critères de recherche")
            } else {
                LazyColumn {
                    items(state.exercises, key = { it.id }) { exercise ->
                        ExerciseListItem(
                            exercise = exercise,
                            onClick = {
                                if (inBuilderContext) {
                                    // Tap principal = ajouter directement à la séance
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("added_exercise_id", exercise.id)
                                    navController.popBackStack()
                                } else {
                                    navController.navigate(Screen.ExerciseDetail.route(exercise.id))
                                }
                            },
                            onInfoClick = if (inBuilderContext) {
                                // Bouton ⓘ = ouvrir la fiche détail avec CTA "Ajouter"
                                { navController.navigate(Screen.ExerciseDetail.routeFromBuilder(exercise.id)) }
                            } else null,
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
