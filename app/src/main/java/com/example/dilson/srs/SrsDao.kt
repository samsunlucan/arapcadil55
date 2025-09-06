package com.example.dilson.srs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SrsDao {
    @Query("SELECT * FROM srs_entry WHERE nextReview <= :time ORDER BY nextReview ASC")
    fun getDueEntries(time: Long): Flow<List<SrsEntry>>

    @Transaction
    @Query("SELECT * FROM srs_entry WHERE nextReview <= :time ORDER BY nextReview ASC")
    fun getDueWithContent(time: Long): Flow<List<SrsWithContent>>

    @Query("SELECT * FROM srs_entry WHERE contentId = :contentId AND contentType = :contentType LIMIT 1")
    suspend fun findByContent(contentId: Int, contentType: String): SrsEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SrsEntry): Long

    @Update
    suspend fun update(entry: SrsEntry)

    @Query("DELETE FROM srs_entry WHERE id = :id")
    suspend fun deleteById(id: Int)
}
