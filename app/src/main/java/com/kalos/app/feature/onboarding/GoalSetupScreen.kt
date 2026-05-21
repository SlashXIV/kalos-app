package com.kalos.app.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kalos.app.core.domain.model.ActivityLevel
import com.kalos.app.core.domain.model.FitnessGoal
import com.kalos.app.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSetupScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Vos objectifs", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Définissez votre niveau d'activité et votre objectif pour obtenir un plan nutritionnel adapté.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Activity level
        Text("Niveau d'activité physique", style = MaterialTheme.typography.titleSmall)
        ActivityLevel.entries.forEach { level ->
            ElevatedCard(
                onClick = { viewModel.onActivityChange(level) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (state.activityLevel == level)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(level.label, style = MaterialTheme.typography.bodyLarge)
                    }
                    RadioButton(
                        selected = state.activityLevel == level,
                        onClick = { viewModel.onActivityChange(level) },
                    )
                }
            }
        }

        HorizontalDivider()

        // Goal
        Text("Objectif", style = MaterialTheme.typography.titleSmall)
        FitnessGoal.entries.forEach { goal ->
            ElevatedCard(
                onClick = { viewModel.onGoalChange(goal) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (state.goal == goal)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(goal.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = state.goal == goal,
                        onClick = { viewModel.onGoalChange(goal) },
                    )
                }
            }
        }

        Button(
            onClick = {
                viewModel.calculateResults()
                navController.navigate(Screen.OnboardingResult.route)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Calculer mes besoins")
        }
    }
}
