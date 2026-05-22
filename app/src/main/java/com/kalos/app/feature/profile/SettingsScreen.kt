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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.data.DietaryPreferencesStore
import com.kalos.app.core.domain.model.DietaryFilter
import com.kalos.app.core.notification.IntelligentReminderScheduler
import com.kalos.app.core.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsStore: DietaryPreferencesStore,
    private val reminderScheduler: ReminderScheduler,
    private val smartScheduler: IntelligentReminderScheduler,
) : ViewModel() {
    val filters: StateFlow<Set<DietaryFilter>> = prefsStore.filtersFlow
    fun toggle(filter: DietaryFilter, enabled: Boolean) = prefsStore.setFilter(filter, enabled)

    // Program reminder hour
    private val _notifHour = MutableStateFlow(reminderScheduler.getNotifHour())
    val notifHour: StateFlow<Int> = _notifHour.asStateFlow()

    fun setNotifHour(hour: Int) {
        reminderScheduler.setNotifHour(hour)
        _notifHour.value = hour
    }

    // Smart reminders
    private val _smartEnabled = MutableStateFlow(smartScheduler.isEnabled())
    val smartEnabled: StateFlow<Boolean> = _smartEnabled.asStateFlow()

    private val _smartNutrition = MutableStateFlow(smartScheduler.isNutritionEnabled())
    val smartNutrition: StateFlow<Boolean> = _smartNutrition.asStateFlow()

    private val _smartWorkout = MutableStateFlow(smartScheduler.isWorkoutEnabled())
    val smartWorkout: StateFlow<Boolean> = _smartWorkout.asStateFlow()

    private val _smartHydration = MutableStateFlow(smartScheduler.isHydrationEnabled())
    val smartHydration: StateFlow<Boolean> = _smartHydration.asStateFlow()

    private val _inactivityDays = MutableStateFlow(smartScheduler.getInactivityDays())
    val inactivityDays: StateFlow<Int> = _inactivityDays.asStateFlow()

    private val _smartHour = MutableStateFlow(smartScheduler.getHour())
    val smartHour: StateFlow<Int> = _smartHour.asStateFlow()

    fun setSmartEnabled(v: Boolean) { smartScheduler.setEnabled(v); _smartEnabled.value = v }
    fun setSmartNutrition(v: Boolean) { smartScheduler.setNutritionEnabled(v); _smartNutrition.value = v }
    fun setSmartWorkout(v: Boolean) { smartScheduler.setWorkoutEnabled(v); _smartWorkout.value = v }
    fun setSmartHydration(v: Boolean) { smartScheduler.setHydrationEnabled(v); _smartHydration.value = v }
    fun setInactivityDays(days: Int) { smartScheduler.setInactivityDays(days); _inactivityDays.value = days }
    fun setSmartHour(hour: Int) { smartScheduler.setHour(hour); _smartHour.value = hour }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    exportViewModel: ExportViewModel = hiltViewModel(),
    importViewModel: ImportViewModel = hiltViewModel(),
) {
    val activeFilters by viewModel.filters.collectAsStateWithLifecycle()
    val exportState by exportViewModel.state.collectAsStateWithLifecycle()
    val importState by importViewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Export launcher ───────────────────────────────────────────────────────
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { exportViewModel.onUriSelected(it) } }

    // ── Import launcher ───────────────────────────────────────────────────────
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { importViewModel.onUriSelected(it) } }

    // ── Snackbar side-effects ─────────────────────────────────────────────────
    LaunchedEffect(exportState) {
        when (val s = exportState) {
            is ExportState.Success -> {
                snackbarHostState.showSnackbar("Sauvegarde enregistrée avec succès")
                exportViewModel.resetState()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar("Export : ${s.message}")
                exportViewModel.resetState()
            }
            else -> {}
        }
    }

    LaunchedEffect(importState) {
        when (val s = importState) {
            is ImportState.Success -> {
                snackbarHostState.showSnackbar("Données importées avec succès")
                importViewModel.resetState()
            }
            is ImportState.Error -> {
                snackbarHostState.showSnackbar("Import : ${s.message}")
                importViewModel.resetState()
            }
            else -> {}
        }
    }

    // ── Import confirmation dialog ────────────────────────────────────────────
    if (importState is ImportState.ConfirmRequired) {
        val backup = (importState as ImportState.ConfirmRequired).backup
        val exportDate = backup.exportedAt.substringBefore("T")
        AlertDialog(
            onDismissRequest = { importViewModel.onCancelImport() },
            title = { Text("Confirmer l'import") },
            text = {
                Text(
                    "Cette action remplacera vos données locales actuelles " +
                    "(profil, journal nutrition, entraînements, hydratation…) " +
                    "par celles du fichier sélectionné.\n\n" +
                    "Fichier exporté le $exportDate.\n\n" +
                    "Les données actuelles seront perdues. Continuer ?",
                )
            },
            confirmButton = {
                TextButton(onClick = { importViewModel.onConfirmImport(backup) }) {
                    Text("Importer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { importViewModel.onCancelImport() }) {
                    Text("Annuler")
                }
            },
        )
    }

    val notifHour by viewModel.notifHour.collectAsStateWithLifecycle()
    var showHourDialog by remember { mutableStateOf(false) }

    if (showHourDialog) {
        var sliderHour by remember { mutableFloatStateOf(notifHour.toFloat()) }
        AlertDialog(
            onDismissRequest = { showHourDialog = false },
            title = { Text("Heure des rappels programme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "${sliderHour.toInt()}h00",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    Slider(
                        value = sliderHour,
                        onValueChange = { sliderHour = it },
                        valueRange = 6f..22f,
                        steps = 15,
                    )
                    Text(
                        "S'applique aux rappels de séances planifiées",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setNotifHour(sliderHour.toInt())
                    showHourDialog = false
                }) { Text("Valider") }
            },
            dismissButton = {
                TextButton(onClick = { showHourDialog = false }) { Text("Annuler") }
            },
        )
    }

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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Smart reminders ──────────────────────────────────────────────
            Text(
                "Rappels intelligents",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SmartRemindersCard(viewModel = viewModel)

            HorizontalDivider()

            // ── Dietary preferences ──────────────────────────────────────────
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

            // ── Program reminders ────────────────────────────────────────────
            Text(
                "Rappels d'entraînement",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingsItem(
                icon = Icons.Filled.Notifications,
                title = "Heure des rappels programme",
                subtitle = "${notifHour}h00 — activez les rappels depuis la fiche programme",
                onClick = { showHourDialog = true },
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Activation par programme", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Activez et configurez les rappels depuis la fiche de chaque programme d'entraînement",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider()

            // ── Data management ──────────────────────────────────────────────
            Text(
                "Données",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingsItem(
                icon = Icons.Filled.FileDownload,
                title = "Exporter mes données",
                subtitle = if (exportState is ExportState.Writing) "Export en cours…"
                           else "Sauvegarde JSON locale",
                enabled = exportState !is ExportState.Writing,
                onClick = {
                    createDocumentLauncher.launch("kalos-backup-${LocalDate.now()}.json")
                },
            )

            SettingsItem(
                icon = Icons.Filled.FileUpload,
                title = "Importer mes données",
                subtitle = when (importState) {
                    is ImportState.Importing -> "Import en cours…"
                    else -> "Restaurer depuis un fichier de sauvegarde"
                },
                enabled = importState !is ImportState.Importing,
                onClick = {
                    openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                },
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
                subtitle = "Kalos 2.2.0",
                enabled = false,
            )
        }
    }
}

// ─── Smart reminders card ─────────────────────────────────────────────────────

@Composable
private fun SmartRemindersCard(viewModel: SettingsViewModel) {
    val enabled by viewModel.smartEnabled.collectAsStateWithLifecycle()
    val nutritionEnabled by viewModel.smartNutrition.collectAsStateWithLifecycle()
    val workoutEnabled by viewModel.smartWorkout.collectAsStateWithLifecycle()
    val hydrationEnabled by viewModel.smartHydration.collectAsStateWithLifecycle()
    val inactivityDays by viewModel.inactivityDays.collectAsStateWithLifecycle()
    val smartHour by viewModel.smartHour.collectAsStateWithLifecycle()

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            // Master switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Column {
                        Text(
                            "Rappels de discipline",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Text(
                            "Nutrition, sport, hydratation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = viewModel::setSmartEnabled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }

            if (enabled) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(12.dp))

                // Nutrition toggle
                SmartToggleRow(
                    title = "Nutrition",
                    subtitle = "Si aucun repas enregistré aujourd'hui",
                    checked = nutritionEnabled,
                    onCheckedChange = viewModel::setSmartNutrition,
                )

                Spacer(Modifier.height(10.dp))

                // Workout toggle + inactivity threshold
                SmartToggleRow(
                    title = "Activité physique",
                    subtitle = "Si inactif depuis trop longtemps",
                    checked = workoutEnabled,
                    onCheckedChange = viewModel::setSmartWorkout,
                )

                if (workoutEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        listOf(2, 3, 5).forEach { days ->
                            FilterChip(
                                selected = inactivityDays == days,
                                onClick = { viewModel.setInactivityDays(days) },
                                label = {
                                    Text(
                                        "$days j",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                        Text(
                            "sans séance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Hydration toggle
                SmartToggleRow(
                    title = "Hydratation",
                    subtitle = "Si < 50 % de l'objectif eau atteint",
                    checked = hydrationEnabled,
                    onCheckedChange = viewModel::setSmartHydration,
                )

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(12.dp))

                // Hour picker inline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Heure des rappels",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "${smartHour}h00",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = smartHour.toFloat(),
                    onValueChange = { viewModel.setSmartHour(it.toInt()) },
                    valueRange = 6f..22f,
                    steps = 15,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Les rappels sont envoyés une fois par jour à l'heure choisie",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SmartToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
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
