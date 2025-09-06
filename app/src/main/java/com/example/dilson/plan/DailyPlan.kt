package com.example.dilson.plan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_plan")
data class DailyPlan(
    @PrimaryKey val date: Long, // start of day epoch millis (unique per day)
    val targetWords: Int = 35,
    val targetSentences: Int = 15,
    val completedWords: Int = 0,
    val completedSentences: Int = 0,
    val streak: Int = 0
)

