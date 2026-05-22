package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class SettingsUiState(
    val reminderDayOf: Boolean = true,
    val reminderDayBefore: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            reminderDayOf = reminderScheduler.isDayOfEnabled(),
            reminderDayBefore = reminderScheduler.isDayBeforeEnabled(),
        )
    )
    val state: StateFlow<SettingsUiState> = _state

    fun setReminderDayOf(enabled: Boolean) {
        _state.value = _state.value.copy(reminderDayOf = enabled)
        reminderScheduler.setDayOf(enabled)
    }

    fun setReminderDayBefore(enabled: Boolean) {
        _state.value = _state.value.copy(reminderDayBefore = enabled)
        reminderScheduler.setDayBefore(enabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Rappels d'entraînement",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ToggleSettingsItem(
                icon = Icons.Filled.Notifications,
                title = "Rappel le jour même",
                subtitle = "Notification le matin des jours prévus au programme",
                checked = state.reminderDayOf,
                onCheckedChange = viewModel::setReminderDayOf,
            )
            ToggleSettingsItem(
                icon = Icons.Filled.NotificationsActive,
                title = "Rappel la veille",
                subtitle = "Notification la veille d'une séance planifiée",
                checked = state.reminderDayBefore,
                onCheckedChange = viewModel::setReminderDayBefore,
            )

            HorizontalDivider()

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
                subtitle = "Kalos 1.4.0",
                enabled = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToggleSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ElevatedCard(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
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
