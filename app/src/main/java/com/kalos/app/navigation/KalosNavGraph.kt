package com.kalos.app.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.kalos.app.feature.onboarding.*
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
                route = "nutrition/food_search?mealType={mealType}&date={date}&query={query}&pick={pick}",
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
                route = "nutrition/custom_food?foodId={foodId}&barcode={barcode}",
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
                route = "nutrition/meal_template_edit?templateId={templateId}",
                arguments = listOf(
                    navArgument("templateId") { type = NavType.LongType; defaultValue = -1L },
                )
            ) { MealTemplateEditorScreen(navController) }
            composable(Screen.NutritionHistory.route) { NutritionHistoryScreen(navController) }
            composable(
                route = "nutrition/day/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                NutritionScreen(
                    navController = navController,
                    initialDate = backStackEntry.arguments?.getString("date"),
                )
            }

            // Workout sub-screens
            composable(
                route = "workout/catalog?templateId={templateId}",
                arguments = listOf(navArgument("templateId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                ExerciseCatalogScreen(
                    navController = navController,
                    templateId = backStackEntry.arguments?.getLong("templateId") ?: -1L,
                )
            }
            composable(
                route = "workout/exercise/{exerciseId}?fromBuilder={fromBuilder}",
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
                route = "workout/builder?templateId={templateId}",
                arguments = listOf(navArgument("templateId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                WorkoutBuilderScreen(
                    navController = navController,
                    templateId = backStackEntry.arguments?.getLong("templateId") ?: -1L,
                )
            }
            composable(
                route = "workout/active/{templateId}",
                arguments = listOf(navArgument("templateId") { type = NavType.LongType })
            ) { backStackEntry ->
                ActiveWorkoutScreen(
                    navController = navController,
                    templateId = backStackEntry.arguments?.getLong("templateId") ?: -1L,
                )
            }
            composable(
                route = "workout/summary/{logId}",
                arguments = listOf(navArgument("logId") { type = NavType.LongType })
            ) { backStackEntry ->
                WorkoutSummaryScreen(
                    navController = navController,
                    logId = backStackEntry.arguments?.getLong("logId") ?: -1L,
                )
            }
            composable(Screen.Programs.route) { ProgramsScreen(navController) }
            composable(
                route = "workout/program/{programId}",
                arguments = listOf(navArgument("programId") { type = NavType.LongType })
            ) { backStackEntry ->
                ProgramDetailScreen(
                    navController = navController,
                    programId = backStackEntry.arguments?.getLong("programId") ?: -1L,
                )
            }
            composable(
                route = "workout/program_editor?programId={programId}",
                arguments = listOf(navArgument("programId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                ProgramEditorScreen(
                    navController = navController,
                    programId = backStackEntry.arguments?.getLong("programId") ?: -1L,
                )
            }
            composable(Screen.WorkoutHistory.route) { WorkoutHistoryScreen(navController) }
            composable(
                route = "workout/log/{logId}",
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
    }
}
