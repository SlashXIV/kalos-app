package com.kalos.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Onboarding
    object Welcome : Screen("onboarding/welcome")
    object ProfileSetup : Screen("onboarding/profile")
    object GoalSetup : Screen("onboarding/goal")
    object OnboardingResult : Screen("onboarding/result")

    // Main tabs
    object Home : Screen("home")
    object Nutrition : Screen("nutrition")
    object Workout : Screen("workout")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")

    // Nutrition sub-screens
    object FoodSearch : Screen("nutrition/food_search?mealType={mealType}&date={date}&query={query}") {
        fun route(mealType: String, date: String) = "nutrition/food_search?mealType=$mealType&date=$date&query="
        fun routeWithQuery(mealType: String, date: String, query: String) =
            "nutrition/food_search?mealType=$mealType&date=$date&query=${android.net.Uri.encode(query)}"
    }
    object CustomFood : Screen("nutrition/custom_food?foodId={foodId}") {
        fun create() = "nutrition/custom_food?foodId=-1"
        fun edit(foodId: Long) = "nutrition/custom_food?foodId=$foodId"
    }
    object NutritionHistory : Screen("nutrition/history")

    // Workout sub-screens
    object ExerciseCatalog : Screen("workout/catalog?templateId={templateId}") {
        fun route(templateId: Long) = "workout/catalog?templateId=$templateId"
        fun standalone() = "workout/catalog?templateId=-1"
    }
    object ExerciseDetail : Screen("workout/exercise/{exerciseId}") {
        fun route(exerciseId: Long) = "workout/exercise/$exerciseId"
        fun routeFromBuilder(exerciseId: Long) = "workout/exercise/$exerciseId?fromBuilder=true"
    }
    object WorkoutBuilder : Screen("workout/builder?templateId={templateId}") {
        fun create() = "workout/builder?templateId=-1"
        fun edit(templateId: Long) = "workout/builder?templateId=$templateId"
    }
    object ActiveWorkout : Screen("workout/active/{templateId}") {
        fun route(templateId: Long) = "workout/active/$templateId"
    }
    object WorkoutSummary : Screen("workout/summary/{logId}") {
        fun route(logId: Long) = "workout/summary/$logId"
    }
    object Programs : Screen("workout/programs")
    object ProgramDetail : Screen("workout/program/{programId}") {
        fun route(programId: Long) = "workout/program/$programId"
    }
    object WorkoutHistory : Screen("workout/history")

    // Profile sub-screens
    object EditProfile : Screen("profile/edit")
    object EditGoals : Screen("profile/goals")
    object Settings : Screen("profile/settings")
    object Notifications : Screen("profile/notifications")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Nutrition, "Nutrition", Icons.Filled.Restaurant),
    BottomNavItem(Screen.Workout, "Sport", Icons.Filled.FitnessCenter),
    BottomNavItem(Screen.Calendar, "Calendrier", Icons.Filled.CalendarMonth),
    BottomNavItem(Screen.Profile, "Profil", Icons.Filled.Person),
)
