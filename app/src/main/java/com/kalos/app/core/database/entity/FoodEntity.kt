package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nameNormalized: String = "",
    val brand: String = "",
    val category: String = "Divers",
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float = 0f,
    val sugarPer100g: Float = 0f,
    val defaultServingG: Float = 100f,
    val servingUnit: String = "g",
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = 0,
    val tags: String = "",  // comma-separated tag tokens, e.g. "pork,vegetarian"
    val isArchived: Boolean = false,
    // EAN/UPC barcode when the food was resolved via scan (Phase 1 groundwork).
    // Null for seed and manually-created foods. No index: the food table is small,
    // a linear lookup on scan is negligible.
    val barcode: String? = null,
)
