package com.example.dilson.content

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val transliteration: String? = null,
    val meaning: String? = null,
    val category: String? = null,
    val hasDiacritics: Boolean = false
)

