package com.example.dilson.gamification

import kotlinx.coroutines.flow.Flow

class PointRepository(private val dao: PointTransactionDao) {
    suspend fun addPoints(amount: Int, reason: String? = null): Long {
        return dao.insert(PointTransaction(amount = amount, reason = reason))
    }

    fun getAllTransactions(): Flow<List<PointTransaction>> = dao.getAll()

    suspend fun getTotalPoints(): Int = dao.getTotalPoints() ?: 0

    suspend fun deleteTransaction(id: Int) = dao.deleteById(id)
}

