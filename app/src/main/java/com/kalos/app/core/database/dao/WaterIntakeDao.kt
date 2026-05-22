package com.kalos.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kalos.app.core.database.entity.WaterIntakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterIntakeDao {

    @Query("SELECT totalMl FROM water_intake WHERE date = :date")
    fun observeForDate(date: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: WaterIntakeEntity)

    @Query("UPDATE water_intake SET totalMl = totalMl + :amount WHERE date = :date")
    suspend fun increment(date: String, amount: Int)

    @Query("UPDATE water_intake SET totalMl = MAX(0, totalMl - :amount) WHERE date = :date")
    suspend fun decrement(date: String, amount: Int)
}
