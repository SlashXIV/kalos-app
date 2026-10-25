package com.kalos.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "body_weight", indices = [Index("date")])
data class BodyWeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,       // ISO date: "2024-01-15"
    val weightKg: Float,
    val note: String = "",
)
