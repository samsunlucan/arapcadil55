package com.example.dilson.advanced

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CulturalContentDao {
    @Query("SELECT * FROM culture_content ORDER BY id DESC")
    fun getAll(): Flow<List<CultureContent>>

    @Query("SELECT * FROM culture_content WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): CultureContent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CultureContent): Long

    @Query("DELETE FROM culture_content WHERE id = :id")
    suspend fun deleteById(id: Int)
}

