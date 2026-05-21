package com.kalos.app.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.kalos.app.core.domain.model.Exercise

@Composable
fun ExerciseListItem(
    exercise: Exercise,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                "${exercise.primaryMuscle} • ${exercise.equipment} • ${exercise.level.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = trailingContent,
        modifier = modifier.clickable(onClick = onClick),
    )
}
