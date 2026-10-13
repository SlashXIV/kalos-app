package com.kalos.app.feature.workout.active

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalos.app.R
import com.kalos.app.core.domain.model.Exercise
import com.kalos.app.core.ui.component.KalosSearchBar
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.theme.KalosShape

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
        shape = KalosShape.sheet,
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
                placeholder = stringResource(R.string.training_search_exercise),
            )
            Spacer(Modifier.height(12.dp))

            val isSearching = query.isNotBlank()
            val displayList = if (isSearching) searchResults else suggestions

            if (!isSearching && displayList.isNotEmpty()) {
                SectionLabel(stringResource(R.string.training_same_muscle), Modifier.padding(bottom = 4.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(displayList, key = { it.id }) { ex ->
                    val isAlreadyAdded = ex.id in excludedExerciseIds
                    val dimmed = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable(enabled = !isAlreadyAdded, role = Role.Button) { onExerciseSelected(ex) }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            ex.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isAlreadyAdded) dimmed else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (isAlreadyAdded) stringResource(R.string.training_already_added)
                            else listOf(ex.primaryMuscle, ex.equipment).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAlreadyAdded) dimmed else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                if (displayList.isEmpty() && isSearching) {
                    item {
                        Text(
                            stringResource(R.string.training_no_result, query),
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
