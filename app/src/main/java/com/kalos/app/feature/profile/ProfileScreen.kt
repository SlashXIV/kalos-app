package com.kalos.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.Sex
import com.kalos.app.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Filled.Settings, "Paramètres")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Avatar + info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Person, null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Column {
                    Text(
                        profile?.name?.ifBlank { "Mon profil" } ?: "Mon profil",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (profile != null) {
                        Text(
                            buildString {
                                append("${profile.age} ans")
                                append(" · ")
                                append(if (profile.sex == Sex.MALE) "Homme" else "Femme")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Physical stats
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Données physiques", style = MaterialTheme.typography.titleSmall)
                    if (profile != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            ProfileStat("Poids", "${profile.weightKg} kg")
                            ProfileStat("Taille", "${profile.heightCm.toInt()} cm")
                            ProfileStat("Objectif", "${profile.targetWeightKg} kg")
                        }
                        if (state.tdee > 0) {
                            HorizontalDivider()
                            Text(
                                "TDEE estimé : ${state.tdee.toInt()} kcal/j — ${profile.activityLevel.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text("Aucun profil configuré. Touchez « Modifier » pour commencer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Nutrition goals
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Objectifs nutritionnels", style = MaterialTheme.typography.titleSmall)
                    val goal = state.goal
                    if (goal != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            ProfileStat("Calories", "${goal.kcal} kcal")
                            ProfileStat("Protéines", "${goal.proteinG} g")
                            ProfileStat("Glucides", "${goal.carbsG} g")
                            ProfileStat("Lipides", "${goal.fatG} g")
                        }
                    } else {
                        Text("Aucun objectif défini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Body weight card
            val lastWeightKg = state.lastWeightKg
            val lastWeightDate = state.lastWeightDate
            if (lastWeightKg != null && lastWeightDate != null) {
                BodyWeightCard(
                    weightKg = lastWeightKg,
                    date = lastWeightDate,
                    delta = state.weightDelta,
                    onClick = { navController.navigate(Screen.WeightLog.route) },
                )
            }

            // Navigation items
            ProfileNavItem(Icons.Filled.Edit, "Modifier le profil") {
                navController.navigate(Screen.EditProfile.route)
            }
            ProfileNavItem(Icons.Filled.TrackChanges, "Modifier les objectifs") {
                navController.navigate(Screen.EditGoals.route)
            }
            ProfileNavItem(Icons.Filled.Settings, "Paramètres") {
                navController.navigate(Screen.Settings.route)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyWeightCard(weightKg: Float, date: String, delta: Float?, onClick: () -> Unit) {
    val dateLabel = remember(date) {
        val d = LocalDate.parse(date)
        val today = LocalDate.now()
        when (d) {
            today -> "Aujourd'hui"
            today.minusDays(1) -> "Hier"
            else -> d.format(DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH))
        }
    }
    val weightLabel = if (weightKg == weightKg.toLong().toFloat()) "${weightKg.toLong()} kg"
                      else "${"%.1f".format(weightKg)} kg"

    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Poids corporel", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(weightLabel, style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(dateLabel, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (delta != null) {
                    val sign = if (delta >= 0f) "+" else ""
                    val deltaLabel = if (delta == delta.toLong().toFloat()) "${sign}${delta.toLong()} kg"
                                     else "${sign}${"%.1f".format(delta)} kg"
                    Text(deltaLabel, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileNavItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
