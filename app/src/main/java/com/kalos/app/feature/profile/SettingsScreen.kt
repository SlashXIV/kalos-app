package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.data.DietaryPreferencesStore
import com.kalos.app.core.domain.model.DietaryFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsStore: DietaryPreferencesStore,
) : ViewModel() {
    val filters: StateFlow<Set<DietaryFilter>> = prefsStore.filtersFlow
    fun toggle(filter: DietaryFilter, enabled: Boolean) = prefsStore.setFilter(filter, enabled)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val activeFilters by viewModel.filters.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Dietary preferences ──────────────────────────────────────
            Text(
                "Préférences alimentaires",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    DietaryFilter.entries.forEach { filter ->
                        DietaryFilterRow(
                            filter = filter,
                            enabled = filter in activeFilters,
                            onToggle = { viewModel.toggle(filter, it) },
                        )
                        if (filter != DietaryFilter.entries.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }

            // Honest disclaimer about halal / religious filtering
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Pour un mode de vie halal : activez « Sans porc » et « Sans alcool ». " +
                            "L'application ne peut pas certifier la conformité de l'abattage — " +
                            "seuls les ingrédients clairement identifiables sont filtrés.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            // ── Reminders ────────────────────────────────────────────────
            Text(
                "Rappels d'entraînement",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Configuration par programme", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Activez et configurez les rappels depuis la fiche de chaque programme",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider()

            // ── General ──────────────────────────────────────────────────
            Text(
                "Général",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingsItem(
                icon = Icons.Filled.FileDownload,
                title = "Exporter mes données",
                subtitle = "Bientôt disponible",
                enabled = false,
            )

            HorizontalDivider()

            Text(
                "À propos",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsItem(
                icon = Icons.Filled.Info,
                title = "Version",
                subtitle = "Kalos 1.5.1",
                enabled = false,
            )
        }
    }
}

@Composable
private fun DietaryFilterRow(
    filter: DietaryFilter,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                filter.label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                filter.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    ElevatedCard(
        onClick = { if (enabled) onClick?.invoke() },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                icon, null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
