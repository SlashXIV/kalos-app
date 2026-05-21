package com.kalos.app.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.ui.component.MacroTrioRow
import com.kalos.app.core.ui.theme.ColorCalories
import com.kalos.app.navigation.Screen
import kotlin.math.roundToInt

@Composable
fun OnboardingResultScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Vos besoins journaliers", style = MaterialTheme.typography.headlineMedium)

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Métabolisme de base (BMR)", style = MaterialTheme.typography.bodyMedium)
                    Text("${state.bmr.roundToInt()} kcal", style = MaterialTheme.typography.bodyMedium)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dépense totale (TDEE)", style = MaterialTheme.typography.bodyMedium)
                    Text("${state.tdee.roundToInt()} kcal", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Objectif calorique", style = MaterialTheme.typography.titleMedium, color = ColorCalories)
                    Text("${state.calculatedKcal} kcal", style = MaterialTheme.typography.titleMedium, color = ColorCalories)
                }
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Répartition des macros", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                MacroTrioRow(
                    proteinConsumed = state.calculatedProtein.toFloat(),
                    proteinGoal = state.calculatedProtein,
                    carbsConsumed = state.calculatedCarbs.toFloat(),
                    carbsGoal = state.calculatedCarbs,
                    fatConsumed = state.calculatedFat.toFloat(),
                    fatGoal = state.calculatedFat,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Protéines: ${state.calculatedProtein}g  •  Glucides: ${state.calculatedCarbs}g  •  Lipides: ${state.calculatedFat}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            "Ces valeurs sont des estimations basées sur la formule Mifflin-St Jeor. Vous pourrez les ajuster à tout moment dans votre profil.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.saveAndComplete {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !state.isSaving,
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Commencer Kalos")
            }
        }
    }
}
