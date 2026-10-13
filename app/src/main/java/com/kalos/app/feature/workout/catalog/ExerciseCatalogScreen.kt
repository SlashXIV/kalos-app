package com.kalos.app.feature.workout.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.component.EmptyState
import com.kalos.app.core.ui.component.ExerciseListItem
import com.kalos.app.core.ui.component.KalosSearchBar
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.theme.KalosShape
import com.kalos.app.navigation.Screen

// Valeurs de filtre = valeurs stockées dans le catalogue (comparées telles quelles par le ViewModel).
private val MUSCLE_FILTERS = listOf("Pectoraux", "Grand dorsal", "Épaules", "Biceps", "Triceps", "Quadriceps", "Ischio-jambiers", "Fessiers", "Abdominaux", "Mollets", "Cardio", "Trapèzes", "Érecteurs")
private val TYPE_FILTERS = listOf("Musculation", "Poids du corps", "Cardio", "HIIT", "Mobilité")

@Composable
fun ExerciseCatalogScreen(
    navController: NavController,
    templateId: Long,
    viewModel: ExerciseCatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Detect builder context from back stack — works for both new (templateId=-1) and edit
    val inBuilderContext = remember {
        navController.previousBackStackEntry?.destination?.route
            ?.startsWith("workout/builder") == true
    }

    // Builder context: hide exercises already in the template (passed via savedStateHandle).
    // Standalone catalog: no key written → empty set → no filtering.
    val excludedExerciseIds: Set<Long> = remember {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<LongArray>("excluded_exercise_ids")
            ?.toSet()
            ?: emptySet()
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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            ScreenHeader(
                title = stringResource(if (inBuilderContext) R.string.training_add_exercise else R.string.training_catalog),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            KalosSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.training_search_exercise),
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CatalogChip(stringResource(R.string.training_favorites), state.onlyFavorites, viewModel::onFavoritesToggle, favorite = true)
                MUSCLE_FILTERS.forEach { muscle ->
                    CatalogChip(muscle, state.selectedMuscle == muscle, { viewModel.onMuscleChange(muscle) })
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TYPE_FILTERS.forEach { type ->
                    CatalogChip(type, state.selectedType == type, { viewModel.onTypeChange(type) })
                }
            }
            Spacer(Modifier.height(8.dp))

            // Hide exercises already added to the current template (builder context only —
            // standalone catalog passes an empty exclusion set).
            val visibleExercises = state.exercises.filter { it.id !in excludedExerciseIds }

            if (visibleExercises.isEmpty()) {
                EmptyState(
                    title = stringResource(
                        if (excludedExerciseIds.isNotEmpty() && state.exercises.isNotEmpty()) R.string.training_catalog_all_added
                        else R.string.training_catalog_empty,
                    ),
                    subtitle = stringResource(R.string.training_catalog_empty_hint),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(visibleExercises, key = { it.id }) { exercise ->
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
                            onFavoriteClick = if (!inBuilderContext) {
                                { viewModel.toggleFavorite(exercise) }
                            } else null,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogChip(text: String, selected: Boolean, onClick: () -> Unit, favorite: Boolean = false) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelMedium) },
        shape = KalosShape.chip,
        leadingIcon = if (favorite && selected) {
            { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.surface,
            selectedLeadingIconColor = MaterialTheme.colorScheme.surface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}
