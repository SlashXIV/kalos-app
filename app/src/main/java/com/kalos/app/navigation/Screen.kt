package com.kalos.app.navigation

import androidx.annotation.StringRes
import com.kalos.app.R

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {

    // Main tabs
    object Nutrition : Screen("nutrition")
    object Workout : Screen("workout")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")

    // Nutrition sub-screens
    object FoodSearch : Screen("nutrition/food_search?mealType={mealType}&date={date}&query={query}&pick={pick}") {
        fun route(mealType: String, date: String) = "nutrition/food_search?mealType=$mealType&date=$date&query=&pick=false"
        fun routeWithQuery(mealType: String, date: String, query: String) =
            "nutrition/food_search?mealType=$mealType&date=$date&query=${android.net.Uri.encode(query)}&pick=false"
        /** Pick mode: tapping a food returns its id via savedStateHandle instead of logging it. */
        fun pick() = "nutrition/food_search?mealType=BREAKFAST&date=&query=&pick=true"
    }
    object CustomFood : Screen("nutrition/custom_food?foodId={foodId}&barcode={barcode}") {
        fun create() = "nutrition/custom_food?foodId=-1&barcode="
        fun edit(foodId: Long) = "nutrition/custom_food?foodId=$foodId&barcode="
        fun createWithBarcode(barcode: String) =
            "nutrition/custom_food?foodId=-1&barcode=${android.net.Uri.encode(barcode)}"
    }
    // Scanner returns the decoded barcode to the food-search screen via savedStateHandle,
    // then pops — it needs no arguments of its own.
    object BarcodeScanner : Screen("nutrition/scan")
    object MyFoods : Screen("nutrition/my_foods")
    object MealTemplates : Screen("nutrition/meal_templates")
    object MealTemplateEdit : Screen("nutrition/meal_template_edit?templateId={templateId}") {
        fun create() = "nutrition/meal_template_edit?templateId=-1"
        fun edit(templateId: Long) = "nutrition/meal_template_edit?templateId=$templateId"
    }
    object NutritionHistory : Screen("nutrition/history")
    object Insights : Screen("insights")
    object Appearance : Screen("appearance")
    object NutritionDay : Screen("nutrition/day/{date}") {
        fun route(date: String) = "nutrition/day/$date"
    }

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
    object ProgramEditor : Screen("workout/program_editor?programId={programId}") {
        fun create() = "workout/program_editor?programId=-1"
        fun edit(programId: Long) = "workout/program_editor?programId=$programId"
    }
    object WorkoutHistory : Screen("workout/history")
    object WorkoutLogDetail : Screen("workout/log/{logId}") {
        fun route(logId: Long) = "workout/log/$logId"
    }

    // Profile sub-screens
    object EditProfile : Screen("profile/edit")
    object EditGoals : Screen("profile/goals")
    object Settings : Screen("profile/settings")
    object Notifications : Screen("profile/notifications")
    object WeightLog : Screen("profile/weight")
}

data class BottomNavItem(
    val screen: Screen,
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Nutrition, R.string.nav_nutrition, Icons.Filled.Restaurant),
    BottomNavItem(Screen.Workout, R.string.nav_workout, Icons.Filled.FitnessCenter),
    BottomNavItem(Screen.Calendar, R.string.nav_calendar, Icons.Filled.CalendarMonth),
    BottomNavItem(Screen.Profile, R.string.nav_profile, Icons.Filled.Person),
)
