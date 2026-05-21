package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// One food item logged inside a meal entry, with actual amounts consumed
@Entity(
    tableName = "meal_entry_item",
    foreignKeys = [
        ForeignKey(
            entity = MealEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("mealEntryId"), Index("foodId")],
)
data class MealEntryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealEntryId: Long,
    val foodId: Long,
    val amountG: Float,
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
)
