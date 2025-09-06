package com.example.dilson.srs

import kotlinx.coroutines.flow.Flow

class SrsRepository(private val dao: SrsDao) {
    fun getDueWithContent(time: Long): Flow<List<SrsWithContent>> = dao.getDueWithContent(time)
    fun getDueEntries(time: Long): Flow<List<SrsEntry>> = dao.getDueEntries(time)

    suspend fun findByContent(contentId: Int, contentType: String): SrsEntry? = dao.findByContent(contentId, contentType)
    suspend fun insert(entry: SrsEntry): Long = dao.insert(entry)
    suspend fun update(entry: SrsEntry) = dao.update(entry)
    suspend fun deleteById(id: Int) = dao.deleteById(id)
}

