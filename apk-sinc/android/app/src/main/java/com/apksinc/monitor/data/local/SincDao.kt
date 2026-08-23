package com.apksinc.monitor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SincDao {

    @Query("SELECT * FROM habits ORDER BY title")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Transaction
    suspend fun replaceHabits(habits: List<HabitEntity>) {
        clearHabits()
        upsertHabits(habits)
    }
}
