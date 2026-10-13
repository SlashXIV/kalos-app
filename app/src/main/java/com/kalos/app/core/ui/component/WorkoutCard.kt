package com.kalos.app.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kalos.app.R
import com.kalos.app.core.domain.model.WorkoutTemplate
import com.kalos.app.core.ui.theme.KalosTheme

/** Carte d'une séance : nom, « 5 exercices · ~60 min », Démarrer / Modifier, suppression discrète. */
@Composable
fun WorkoutTemplateCard(
    template: WorkoutTemplate,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    KalosCard(modifier, contentPadding = PaddingValues(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(template.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(
                        R.plurals.workout_exercise_count, template.exercises.size,
                        template.exercises.size, template.estimatedDurationMin,
                    ),
                    style = KalosTheme.type.metricSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (template.description.isNotEmpty()) {
                    Text(
                        template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.training_delete_named, template.name),
                        // Discret, pas rouge : le rouge est réservé au dialogue de confirmation.
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Row(Modifier.padding(end = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KalosButton(stringResource(R.string.training_start), onClick = onStart, modifier = Modifier.weight(1f))
            KalosButton(stringResource(R.string.training_edit), onClick = onEdit, ghost = true, modifier = Modifier.weight(1f))
        }
    }
}
