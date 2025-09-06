package com.example.dilson.content

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sentence")
data class Sentence(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val transliteration: String? = null,
    val translation: String? = null,
    val category: String? = null,
    val hasDiacritics: Boolean = false
)

