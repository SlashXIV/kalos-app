package com.kalos.app.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kalos.app.core.notification.NotificationHelper
import com.kalos.app.core.ui.component.KalosBottomNavBar
import com.kalos.app.feature.calendar.CalendarScreen
import com.kalos.app.feature.nutrition.NutritionScreen
import com.kalos.app.feature.nutrition.custom.CustomFoodScreen
import com.kalos.app.feature.nutrition.history.NutritionHistoryScreen
import com.kalos.app.feature.nutrition.myfoods.MyFoodsScreen
import com.kalos.app.feature.nutrition.templates.MealTemplateEditorScreen
import com.kalos.app.feature.nutrition.templates.MealTemplatesScreen
import com.kalos.app.feature.nutrition.scan.BarcodeScannerScreen
import com.kalos.app.feature.nutrition.search.FoodSearchScreen
import com.kalos.app.feature.profile.AppearanceScreen
import com.kalos.app.feature.profile.EditGoalsScreen
import com.kalos.app.feature.profile.EditProfileScreen
import com.kalos.app.feature.profile.NotificationsScreen
import com.kalos.app.feature.profile.ProfileScreen
import com.kalos.app.feature.profile.SettingsScreen
import com.kalos.app.feature.insights.InsightsScreen
import com.kalos.app.feature.profile.WeightLogScreen
import com.kalos.app.feature.workout.WorkoutScreen
import com.kalos.app.feature.workout.active.ActiveWorkoutScreen
import com.kalos.app.feature.workout.active.WorkoutSummaryScreen
import com.kalos.app.feature.workout.builder.WorkoutBuilderScreen
import com.kalos.app.feature.workout.catalog.ExerciseCatalogScreen
import com.kalos.app.feature.workout.catalog.ExerciseDetailScreen
import com.kalos.app.feature.workout.history.WorkoutHistoryScreen
import com.kalos.app.feature.workout.history.WorkoutLogDetailScreen
import com.kalos.app.feature.workout.program.ProgramDetailScreen
import com.kalos.app.feature.workout.program.ProgramEditorScreen
import com.kalos.app.feature.workout.program.ProgramsScreen

@Composable
fun KalosNavGraph(
    deepLinkDestination: String? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Notification tap deep-link → jump to the relevant tab, once.
    LaunchedEffect(deepLinkDestination) {
        val route = when (deepLinkDestination) {
            NotificationHelper.DEST_WORKOUT -> Screen.Workout.route
            NotificationHelper.DEST_NUTRITION, NotificationHelper.DEST_WATER -> Screen.Nutrition.route
            else -> null
        }
        if (route != null) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onDeepLinkHandled()
        }
    }

    val mainRoutes = bottomNavItems.map { it.screen.route }
    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        mainRoutes.any { dest.route == it }
    } == true

    Scaffold(
        // Chaque écran gère ses propres marges système (barre d'état, clavier) : sans ça, elles
        // s'additionnent et le titre descend de la hauteur d'une barre d'état.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                KalosBottomNavBar(
                    items = bottomNavItems,
                    currentDestination = currentDestination,
                    onItemClick = { item ->
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Nutrition.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 4 } },
        ) {
            // Main tabs
            composable(Screen.Nutrition.route) { NutritionScreen(navController) }
            composable(Screen.Workout.route) { WorkoutScreen(navController) }
            composable(Screen.Calendar.route) { CalendarScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController) }

            // Nutrition sub-screens
            composable(
                route = Screen.FoodSearch.route,
                arguments = listOf(
                    navArgument("mealType") { type = NavType.StringType },
                    navArgument("date") { type = NavType.StringType; defaultValue = "" },
                    navArgument("query") { type = NavType.StringType; defaultValue = "" },
                    navArgument("pick") { type = NavType.BoolType; defaultValue = false },
                )
            ) { backStackEntry ->
                FoodSearchScreen(
                    navController = navController,
                    mealType = backStackEntry.arguments?.getString("mealType") ?: "BREAKFAST",
                    date = backStackEntry.arguments?.getString("date") ?: "",
                    pickForResult = backStackEntry.arguments?.getBoolean("pick") ?: false,
                )
            }
            composable(
                route = Screen.CustomFood.route,
                arguments = listOf(
                    navArgument("foodId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("barcode") { type = NavType.StringType; defaultValue = "" },
                )
            ) { backStackEntry ->
                CustomFoodScreen(
                    navController = navController,
                    foodId = backStackEntry.arguments?.getLong("foodId") ?: -1L,
                    barcode = backStackEntry.arguments?.getString("barcode").orEmpty(),
                )
            }
            composable(Screen.BarcodeScanner.route) { BarcodeScannerScreen(navController) }
            composable(Screen.MyFoods.route) { MyFoodsScreen(navController) }
            composable(Screen.MealTemplates.route) { MealTemplatesScreen(navController) }
            composable(
                route = Screen.MealTemplateEdit.route,
                arguments = listOf(
                    navArgument("templateId") { type = NavType.LongType; defaultValue = -1L },
                )
            ) { MealTemplateEditorScreen(navController) }
            composable(Screen.NutritionHistory.route) { NutritionHistoryScreen(navController) }
            composable(
                route = Screen.NutritionDay.route,
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                NutritionScreen(
                    navController = navController,
                    initialDate = backStackEntry.arguments?.getString("date"),
                )
            }

            // Workout sub-screens
            composable(
                route = Screen.ExerciseCatalog.route,
                arguments = listOf(navArgument("templateId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                ExerciseCatalogScreen(
                    navController = navController,
                    templateId = backStackEntry.arguments?.getLong("templateId") ?: -1L,
                )
            }
            composable(
                route = Screen.ExerciseDetail.route,
                arguments = listOf(
                    navArgument("exerciseId") { type = NavType.LongType },
                    navArgument("fromBuilder") { type = NavType.BoolType; defaultValue = false },
                )
            ) { backStackEntry ->
                ExerciseDetailScreen(
                    navController = navController,
                    exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: -1L,
                    fromBuilder = backStackEntry.arguments?.getBoolean("fromBuilder") ?: false,
                )
            }
            composable(
                route = Screen.WorkoutBuilder.route,
                arguments = listOf(navArgument("templateId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                WorkoutBuilderScreen(
                    navController = navController,
                    templateId = backStackEntry.arguments?.getLong("templateId") ?: -1L,
                )
            }
            composable(
                route = Screen.ActiveWorkout.route,
                arguments = listOf(navArgument("templateId") { type = NavType.LongType })
            ) { backStackEntry ->
                ActiveWorkoutScreen(
                    navController = navController,
                    templateId = backStackEntry.arguments?.getLong("templateId") ?: -1L,
                )
            }
            composable(
                route = Screen.WorkoutSummary.route,
                arguments = listOf(navArgument("logId") { type = NavType.LongType })
            ) { backStackEntry ->
                WorkoutSummaryScreen(
                    navController = navController,
                    logId = backStackEntry.arguments?.getLong("logId") ?: -1L,
                )
            }
            composable(Screen.Programs.route) { ProgramsScreen(navController) }
            composable(
                route = Screen.ProgramDetail.route,
                arguments = listOf(navArgument("programId") { type = NavType.LongType })
            ) { backStackEntry ->
                ProgramDetailScreen(
                    navController = navController,
                    programId = backStackEntry.arguments?.getLong("programId") ?: -1L,
                )
            }
            composable(
                route = Screen.ProgramEditor.route,
                arguments = listOf(navArgument("programId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                ProgramEditorScreen(
                    navController = navController,
                    programId = backStackEntry.arguments?.getLong("programId") ?: -1L,
                )
            }
            composable(Screen.WorkoutHistory.route) { WorkoutHistoryScreen(navController) }
            composable(
                route = Screen.WorkoutLogDetail.route,
                arguments = listOf(navArgument("logId") { type = NavType.LongType })
            ) { backStackEntry ->
                WorkoutLogDetailScreen(
                    navController = navController,
                    logId = backStackEntry.arguments?.getLong("logId") ?: -1L,
                )
            }

            // Profile sub-screens
            composable(Screen.EditProfile.route) { EditProfileScreen(navController) }
            composable(Screen.EditGoals.route) { EditGoalsScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            composable(Screen.Notifications.route) { NotificationsScreen(navController) }
            composable(Screen.WeightLog.route) { WeightLogScreen(navController) }
            composable(Screen.Insights.route) { InsightsScreen(navController) }
            composable(Screen.Appearance.route) { AppearanceScreen(navController) }
        }
        // Bord à bord : un bandeau de la couleur du fond derrière la barre d'état, pour que le
        // contenu qui défile ne passe pas sous l'heure et les icônes système.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.background),
        )
    }
}
