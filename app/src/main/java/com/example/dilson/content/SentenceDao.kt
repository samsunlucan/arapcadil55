package com.example.dilson.content

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceDao {
    @Query("SELECT * FROM sentence ORDER BY id DESC")
    fun getAll(): Flow<List<Sentence>>

    @Query("SELECT * FROM sentence WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Sentence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sentence: Sentence): Long

    @Update
    suspend fun update(sentence: Sentence)

    @Query("DELETE FROM sentence WHERE id = :id")
    suspend fun deleteById(id: Int)
}

