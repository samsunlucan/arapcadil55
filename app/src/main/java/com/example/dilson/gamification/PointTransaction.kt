package com.example.dilson.gamification

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "point_transaction")
data class PointTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Int,
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

