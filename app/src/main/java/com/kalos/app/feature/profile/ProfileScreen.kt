package com.kalos.app.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.R
import com.kalos.app.core.ui.util.dayLabel
import com.kalos.app.core.ui.util.formatKg
import com.kalos.app.core.domain.model.Sex
import com.kalos.app.core.ui.component.FitText
import com.kalos.app.core.ui.component.InstrumentLabel
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.KalosCard
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.component.SectionLabel
import com.kalos.app.core.ui.component.Swatch
import com.kalos.app.core.ui.theme.KalosTheme
import com.kalos.app.core.ui.util.formatGroupedInt
import com.kalos.app.navigation.Screen
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    val kgUnit = stringResource(R.string.kg)
    val kgSpoken = stringResource(R.string.me_unit_kg_spoken)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val name = profile?.name?.trim().orEmpty().ifBlank { stringResource(R.string.me_profile_my_profile) }
            ScreenHeader(
                title = name,
                eyebrow = if (profile != null) {
                    pluralStringResource(R.plurals.me_profile_age, profile.age, profile.age) + " · " +
                        stringResource(if (profile.sex == Sex.MALE) R.string.me_sex_male else R.string.me_sex_female)
                } else stringResource(R.string.nav_profile),
            ) {
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.me_settings_title))
                }
            }

            KalosCard {
                SectionLabel(stringResource(R.string.me_profile_physical))
                if (profile != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileStat(stringResource(R.string.me_profile_weight), formatKg(profile.weightKg), kgUnit, kgSpoken, Modifier.weight(1f))
                        ProfileStat(
                            stringResource(R.string.me_profile_height), profile.heightCm.toInt().toString(),
                            stringResource(R.string.cm), stringResource(R.string.me_unit_cm_spoken), Modifier.weight(1f),
                        )
                        ProfileStat(stringResource(R.string.me_profile_target), formatKg(profile.targetWeightKg), kgUnit, kgSpoken, Modifier.weight(1f))
                    }
                    if (state.tdee > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            stringResource(R.string.me_profile_tdee, formatGroupedInt(state.tdee.toInt()), profile.activityLevel.label),
                            style = KalosTheme.type.metricSmall,
                        )
                        Text(
                            stringResource(R.string.me_profile_reference_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Resynchronisation en un geste quand la dernière pesée s'écarte d'au moins 1 kg
                    // du poids de référence utilisé pour le TDEE.
                    val lastWeight = state.lastWeightKg
                    if (lastWeight != null && abs(lastWeight - profile.weightKg) >= 1f) {
                        KalosButton(
                            stringResource(R.string.me_profile_use_last_weight, "${formatKg(lastWeight)} $kgUnit"),
                            onClick = viewModel::updateReferenceWeightFromLog,
                            ghost = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.me_profile_no_profile),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            KalosCard {
                SectionLabel(stringResource(R.string.me_profile_goals))
                val goal = state.goal
                if (goal != null) {
                    val nutrients = KalosTheme.nutrients
                    val g = stringResource(R.string.unit_g)
                    val grams = stringResource(R.string.unit_grams_spoken)
                    GoalLine(stringResource(R.string.nutrition_calories), formatGroupedInt(goal.kcal), stringResource(R.string.kcal_label), stringResource(R.string.unit_kcal_spoken), MaterialTheme.colorScheme.onSurface)
                    GoalLine(stringResource(R.string.nutrition_protein), formatGroupedInt(goal.proteinG), g, grams, nutrients.protein)
                    GoalLine(stringResource(R.string.nutrition_carbs), formatGroupedInt(goal.carbsG), g, grams, nutrients.carbs)
                    GoalLine(stringResource(R.string.nutrition_fat), formatGroupedInt(goal.fatG), g, grams, nutrients.fat)
                } else {
                    Text(
                        stringResource(R.string.me_profile_no_goal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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

            KalosCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                ProfileNavItem(Icons.Filled.Insights, stringResource(R.string.me_profile_nav_insights)) {
                    navController.navigate(Screen.Insights.route)
                }
                ProfileNavItem(Icons.Filled.Edit, stringResource(R.string.me_profile_nav_edit)) {
                    navController.navigate(Screen.EditProfile.route)
                }
                ProfileNavItem(Icons.Filled.TrackChanges, stringResource(R.string.me_profile_nav_goals)) {
                    navController.navigate(Screen.EditGoals.route)
                }
                ProfileNavItem(Icons.Filled.Settings, stringResource(R.string.me_settings_title)) {
                    navController.navigate(Screen.Settings.route)
                }
            }
        }
    }
}

@Composable
private fun BodyWeightCard(weightKg: Float, date: String, delta: Float?, onClick: () -> Unit) {
    val d = LocalDate.parse(date)
    val dateLabel = dayLabel(d, "d MMM")
    val kg = stringResource(R.string.kg)
    val deltaLabel = delta?.let { (if (it >= 0f) "+" else "") + formatKg(it) }
    val title = stringResource(R.string.me_profile_body_weight)
    val sentence = stringResource(R.string.me_value_spoken, title, formatKg(weightKg), stringResource(R.string.me_unit_kg_spoken)) +
        ", " + dateLabel + if (deltaLabel != null) ", " + stringResource(R.string.me_weight_delta_spoken, deltaLabel) else ""

    KalosCard(onClick = onClick, modifier = Modifier.clearAndSetSemantics {
        contentDescription = sentence
        onClick { onClick(); true }
        role = Role.Button
    }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                InstrumentLabel(title)
                Text("${formatKg(weightKg)} $kg", style = KalosTheme.type.metric)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(dateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (deltaLabel != null) {
                    Text("$deltaLabel $kg", style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, unit: String, spokenUnit: String, modifier: Modifier = Modifier) {
    val sentence = stringResource(R.string.me_value_spoken, label, value, spokenUnit)
    Column(modifier.clearAndSetSemantics { contentDescription = sentence }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        InstrumentLabel(label)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            FitText(value, style = KalosTheme.type.metricLarge, modifier = Modifier.weight(1f, fill = false))
            Text(unit, style = KalosTheme.type.metricSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

/** Objectif quotidien : pastille, libellé, valeur en chiffres tabulaires ; une phrase pour TalkBack. */
@Composable
private fun GoalLine(label: String, value: String, unit: String, spokenUnit: String, color: Color) {
    val sentence = stringResource(R.string.me_value_spoken, label, value, spokenUnit)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 28.dp).clearAndSetSemantics { contentDescription = sentence },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Swatch(color)
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text("$value $unit", style = KalosTheme.type.metric)
    }
}

@Composable
private fun ProfileNavItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
