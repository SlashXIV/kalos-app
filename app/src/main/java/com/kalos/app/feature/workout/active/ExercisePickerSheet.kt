package com.kalos.app.feature.workout.active

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.ui.component.KalosSearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    muscleFilter: String,
    onExerciseSelected: (Exercise) -> Unit,
    onDismiss: () -> Unit,
    excludedExerciseIds: Set<Long> = emptySet(),
    viewModel: ExercisePickerViewModel = hiltViewModel(),
) {
    LaunchedEffect(muscleFilter) { viewModel.setMuscleFilter(muscleFilter) }

    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            KalosSearchBar(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Rechercher un exercice…",
            )
            Spacer(Modifier.height(12.dp))

            val isSearching = query.isNotBlank()
            val displayList = if (isSearching) searchResults else suggestions

            if (!isSearching && displayList.isNotEmpty()) {
                Text(
                    "Même groupe musculaire",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(displayList, key = { it.id }) { ex ->
                    val isAlreadyAdded = ex.id in excludedExerciseIds
                    val dimmed = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ListItem(
                        headlineContent = {
                            Text(
                                ex.name,
                                color = if (isAlreadyAdded) dimmed
                                        else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        supportingContent = {
                            Text(
                                if (isAlreadyAdded) "Déjà ajouté"
                                else buildString {
                                    append(ex.primaryMuscle)
                                    if (ex.equipment.isNotBlank()) append(" · ${ex.equipment}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAlreadyAdded) dimmed
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = if (isAlreadyAdded) Modifier
                                   else Modifier.clickable { onExerciseSelected(ex) },
                    )
                    HorizontalDivider()
                }

                if (displayList.isEmpty() && isSearching) {
                    item {
                        Text(
                            "Aucun résultat pour « $query »",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
