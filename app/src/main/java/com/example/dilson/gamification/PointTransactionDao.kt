package com.example.dilson.gamification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PointTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: PointTransaction): Long

    @Query("SELECT * FROM point_transaction ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PointTransaction>>

    @Query("SELECT SUM(amount) FROM point_transaction")
    suspend fun getTotalPoints(): Int?

    @Query("DELETE FROM point_transaction WHERE id = :id")
    suspend fun deleteById(id: Int)
}

