package com.example.dilson.repository

import com.example.dilson.advanced.CulturalContentDao
import com.example.dilson.advanced.CultureContent
import kotlinx.coroutines.flow.Flow

class AdvancedFeaturesRepository(private val dao: CulturalContentDao) {
    fun getAll(): Flow<List<CultureContent>> = dao.getAll()

    suspend fun getById(id: Int): CultureContent? = dao.getById(id)

    suspend fun insert(item: CultureContent): Long = dao.insert(item)

    suspend fun deleteById(id: Int) = dao.deleteById(id)
}

