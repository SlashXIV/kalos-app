package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: String = "[]",  // JSON array of strings
    val equipment: String = "Aucun",
    val level: String = "BEGINNER",       // ExerciseLevel name (French labels before v18)
    val type: String = "STRENGTH",        // ExerciseType name (French labels before v18)
    val trackingMode: String = "REPS_WEIGHT",  // REPS_WEIGHT | DURATION | DURATION_WEIGHT
    val description: String = "",
    val instructions: String = "",
    val imageUrl: String = "",
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val seedId: String? = null,
    val nameNormalized: String = "",
)
