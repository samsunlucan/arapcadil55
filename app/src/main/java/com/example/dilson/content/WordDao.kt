package com.example.dilson.content

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM word ORDER BY id DESC")
    fun getAll(): Flow<List<Word>>

    @Query("SELECT * FROM word WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Word?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: Word): Long

    @Update
    suspend fun update(word: Word)

    @Query("DELETE FROM word WHERE id = :id")
    suspend fun deleteById(id: Int)
}

