package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val age: Int = 25,
    val sex: String = "male", // "male" | "female"
    val heightCm: Float = 175f,
    val weightKg: Float = 75f,
    val targetWeightKg: Float = 70f,
    val activityLevel: String = "moderate", // sedentary|light|moderate|active|very_active
    val goal: String = "maintain", // lose_slow|lose_fast|maintain|gain_lean|gain_fast
    val createdAt: Long = System.currentTimeMillis(),
    val onboardingCompleted: Boolean = false,
)
