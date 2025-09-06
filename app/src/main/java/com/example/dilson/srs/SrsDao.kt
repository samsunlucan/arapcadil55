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
    @Query("SELECT s.id AS id, s.contentId AS contentId, s.contentType AS contentType, s.box AS box, s.lastReviewed AS lastReviewed, s.nextReview AS nextReview, c.id AS id, c.title AS title, c.text AS text, c.language AS language, c.transliteration AS transliteration FROM srs_entry s JOIN culture_content c ON s.contentId = c.id WHERE s.nextReview <= :time ORDER BY s.nextReview ASC")
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
