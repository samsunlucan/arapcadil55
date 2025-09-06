package com.example.dilson.quiz

import com.example.dilson.advanced.CultureContent

data class QuizQuestion(
    val correct: CultureContent,
    val options: List<CultureContent>
)

