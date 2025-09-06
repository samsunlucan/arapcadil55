package com.example.dilson.plan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyPlanDao {
    @Query("SELECT * FROM daily_plan WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: Long): DailyPlan?

    @Query("SELECT * FROM daily_plan ORDER BY date DESC LIMIT :limit")
    fun getRecentPlans(limit: Int): Flow<List<DailyPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: DailyPlan)

    @Update
    suspend fun update(plan: DailyPlan)

    @Query("DELETE FROM daily_plan WHERE date = :date")
    suspend fun deleteByDate(date: Long)
}

