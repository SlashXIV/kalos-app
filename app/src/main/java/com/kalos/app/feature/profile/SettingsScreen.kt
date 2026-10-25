package com.kalos.app.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.DietaryFilter
import androidx.compose.ui.res.stringResource
import com.kalos.app.R
import com.kalos.app.core.ui.theme.Palettes
import com.kalos.app.navigation.Screen
import java.time.LocalDate

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    exportViewModel: ExportViewModel = hiltViewModel(),
    importViewModel: ImportViewModel = hiltViewModel(),
) {
    val activeFilters by viewModel.filters.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val exportState by exportViewModel.state.collectAsStateWithLifecycle()
    val importState by importViewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { exportViewModel.onUriSelected(it) } }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { importViewModel.onUriSelected(it) } }

    LaunchedEffect(exportState) {
        when (val s = exportState) {
            is ExportState.Success -> { snackbarHostState.showSnackbar(context.getString(R.string.me_settings_export_ok)); exportViewModel.resetState() }
            is ExportState.Error -> { snackbarHostState.showSnackbar(context.getString(R.string.me_settings_export_error, s.message)); exportViewModel.resetState() }
            else -> {}
        }
    }

    LaunchedEffect(importState) {
        when (val s = importState) {
            is ImportState.Success -> { snackbarHostState.showSnackbar(context.getString(R.string.me_settings_import_ok)); importViewModel.resetState() }
            is ImportState.Error -> { snackbarHostState.showSnackbar(context.getString(R.string.me_settings_import_error, s.message)); importViewModel.resetState() }
            else -> {}
        }
    }

    if (importState is ImportState.ConfirmRequired) {
        val backup = (importState as ImportState.ConfirmRequired).backup
        val exportDate = backup.exportedAt.substringBefore("T")
        AlertDialog(
            onDismissRequest = { importViewModel.onCancelImport() },
            title = { Text(stringResource(R.string.me_settings_import_confirm_title)) },
            text = { Text(stringResource(R.string.me_settings_import_confirm_text, exportDate)) },
            confirmButton = {
                TextButton(onClick = { importViewModel.onConfirmImport(backup) }) {
                    // Rouge : confirmation destructive (les données locales sont remplacées).
                    Text(stringResource(R.string.me_settings_import_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { importViewModel.onCancelImport() }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.me_settings_title),
                small = true,
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )

            KalosCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsItem(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.me_settings_notifications),
                    subtitle = stringResource(R.string.me_settings_notifications_sub),
                    onClick = { navController.navigate(Screen.Notifications.route) },
                )
                // ── Appearance ───────────────────────────────────────────────────
                SettingsItem(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.appearance_title),
                    subtitle = stringResource(R.string.appearance_subtitle, stringResource(Palettes.byId(appearance.paletteId).nameRes)),
                    onClick = { navController.navigate(Screen.Appearance.route) },
                )
            }

            SectionLabel(stringResource(R.string.me_settings_dietary), Modifier.padding(top = 8.dp))
            KalosCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                DietaryFilter.entries.forEach { filter ->
                    DietaryFilterRow(
                        filter = filter,
                        enabled = filter in activeFilters,
                        onToggle = { viewModel.toggle(filter, it) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.me_settings_halal_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionLabel(stringResource(R.string.me_settings_data), Modifier.padding(top = 8.dp))
            KalosCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsItem(
                    icon = Icons.Filled.FileDownload,
                    title = stringResource(R.string.me_settings_export),
                    subtitle = stringResource(if (exportState is ExportState.Writing) R.string.me_settings_exporting else R.string.me_settings_export_sub),
                    enabled = exportState !is ExportState.Writing,
                    onClick = { createDocumentLauncher.launch("kalos-backup-${LocalDate.now()}.json") },
                )
                SettingsItem(
                    icon = Icons.Filled.FileUpload,
                    title = stringResource(R.string.me_settings_import),
                    subtitle = stringResource(if (importState is ImportState.Importing) R.string.me_settings_importing else R.string.me_settings_import_sub),
                    enabled = importState !is ImportState.Importing,
                    onClick = { openDocumentLauncher.launch(arrayOf("application/json", "*/*")) },
                )
            }

            SectionLabel(stringResource(R.string.me_settings_about), Modifier.padding(top = 8.dp))
            KalosCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.me_settings_version),
                    subtitle = stringResource(
                        R.string.me_settings_version_value,
                        // Version lue dans le paquet installé : elle suit versionName sans texte à tenir à jour.
                        LocalContext.current.run { packageManager.getPackageInfo(packageName, 0).versionName.orEmpty() },
                    ),
                    enabled = false,
                )
            }
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
            .heightIn(min = 56.dp)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onToggle)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(filter.label, style = MaterialTheme.typography.bodyLarge)
            Text(filter.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}

/** Ligne de réglage : icône discrète, titre, sous-titre ; toute la ligne est la cible. */
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
