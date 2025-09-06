package com.example.dilson.advanced

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "culture_content")
data class CultureContent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String? = null, // başlıksız olabilir
    val text: String,
    val language: String = "ar",
    val transliteration: String? = null
)

