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

enum class ActivityLevel(val label: String, val multiplier: Float, val description: String) {
    SEDENTARY(
        "Sédentaire", 1.2f,
        "Peu ou pas d'exercice — travail de bureau, déplacements en voiture",
    ),
    LIGHT(
        "Légèrement actif", 1.375f,
        "Exercice léger 1–3 fois/semaine (marche, vélo, stretching)",
    ),
    MODERATE(
        "Modérément actif", 1.55f,
        "Sport 3–5 fois/semaine — gym, running, natation",
    ),
    ACTIVE(
        "Très actif", 1.725f,
        "Entraînement intense 6–7 fois/semaine ou travail physique",
    ),
    VERY_ACTIVE(
        "Extrêmement actif", 1.9f,
        "Sport intensif quotidien et travail physique exigeant — athlètes",
    ),
}

enum class FitnessGoal(
    val label: String,
    val kcalDelta: Int,
    /** Protein target in g per kg of body weight */
    val proteinPerKg: Float,
    /** Fat target in g per kg of body weight */
    val fatPerKg: Float,
) {
    // Higher protein during deficits to preserve muscle; lower fat leaves room for carbs
    LOSE_AGGRESSIVE("Sèche agressive (−750 kcal)", -750, 2.2f, 0.8f),
    LOSE_FAST("Perte rapide (−500 kcal)",          -500, 2.0f, 0.9f),
    LOSE_SLOW("Perte progressive (−250 kcal)",     -250, 1.9f, 1.0f),
    MAINTAIN("Maintien",                              0, 1.8f, 1.0f),
    GAIN_LEAN("Prise de masse légère (+250 kcal)", +250, 1.8f, 1.1f),
    GAIN_FAST("Prise de masse (+500 kcal)",        +500, 1.8f, 1.2f),
}
