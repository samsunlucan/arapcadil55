package com.example.dilson.srs

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "srs_entry")
data class SrsEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contentId: Int, // references a word/culture/content id
    val contentType: String = "culture", // e.g. "word", "sentence", "culture"
    val box: Int = 1,
    val lastReviewed: Long = 0L,
    val nextReview: Long = 0L
)

