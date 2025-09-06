package com.example.dilson.srs

import androidx.room.Embedded
import androidx.room.Relation
import com.example.dilson.advanced.CultureContent

data class SrsWithContent(
    @Embedded val srs: SrsEntry,
    @Relation(parentColumn = "contentId", entityColumn = "id")
    val content: CultureContent
)

