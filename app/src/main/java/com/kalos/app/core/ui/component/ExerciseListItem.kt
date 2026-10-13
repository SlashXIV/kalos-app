package com.kalos.app.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kalos.app.R
import com.kalos.app.core.domain.model.Exercise

/** Ligne du catalogue : nom, « muscle · matériel · niveau », action d'information ou de favori. */
@Composable
fun ExerciseListItem(
    exercise: Exercise,
    onClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    onFavoriteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(exercise.primaryMuscle, exercise.equipment, exercise.level.label).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        when {
            onInfoClick != null -> IconButton(onClick = onInfoClick) {
                Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.training_details_named, exercise.name), tint = muted)
            }
            onFavoriteClick != null -> IconButton(onClick = onFavoriteClick) {
                Icon(
                    if (exercise.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(
                        if (exercise.isFavorite) R.string.training_favorite_remove else R.string.training_favorite_add,
                        exercise.name,
                    ),
                    tint = if (exercise.isFavorite) MaterialTheme.colorScheme.primary else muted,
                )
            }
        }
    }
}
