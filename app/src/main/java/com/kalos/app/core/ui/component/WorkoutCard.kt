package com.kalos.app.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kalos.app.core.domain.model.WorkoutTemplate

@Composable
fun WorkoutTemplateCard(
    template: WorkoutTemplate,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(template.name, style = MaterialTheme.typography.titleMedium)
                    if (template.description.isNotEmpty()) {
                        Text(
                            template.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${template.exercises.size} exercice(s) • ~${template.estimatedDurationMin} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Modifier") }
                Button(onClick = onStart, modifier = Modifier.weight(1f)) { Text("Démarrer") }
            }
        }
    }
}
