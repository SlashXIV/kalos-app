package com.kalos.app.core.domain.model

data class UserProfile(
    val id: Int = 1,
    val name: String = "",
    val age: Int = 25,
    val sex: Sex = Sex.MALE,
    val heightCm: Float = 175f,
    val weightKg: Float = 75f,
    val targetWeightKg: Float = 70f,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goal: FitnessGoal = FitnessGoal.MAINTAIN,
    val onboardingCompleted: Boolean = false,
)

enum class Sex { MALE, FEMALE }

enum class ActivityLevel(val label: String, val multiplier: Float) {
    SEDENTARY("Sédentaire", 1.2f),
    LIGHT("Légèrement actif", 1.375f),
    MODERATE("Modérément actif", 1.55f),
    ACTIVE("Très actif", 1.725f),
    VERY_ACTIVE("Extrêmement actif", 1.9f),
}

enum class FitnessGoal(val label: String, val kcalDelta: Int) {
    LOSE_AGGRESSIVE("Sèche agressive (−750 kcal)", -750),
    LOSE_FAST("Perte rapide (−500 kcal)", -500),
    LOSE_SLOW("Perte progressive (−250 kcal)", -250),
    MAINTAIN("Maintien", 0),
    GAIN_LEAN("Prise de masse légère (+250 kcal)", +250),
    GAIN_FAST("Prise de masse (+500 kcal)", +500),
}
