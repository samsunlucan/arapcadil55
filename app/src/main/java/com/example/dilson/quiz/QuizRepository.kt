package com.example.dilson.quiz

import com.example.dilson.advanced.CulturalContentDao
import com.example.dilson.advanced.CultureContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlin.random.Random

class QuizRepository(private val dao: CulturalContentDao) {
    suspend fun generateQuestions(count: Int = 10, optionsPerQuestion: Int = 4): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val all = dao.getAll().first()
        if (all.isEmpty()) return@withContext emptyList()

        val questions = mutableListOf<QuizQuestion>()
        val pool = all.toMutableList()

        repeat(minOf(count, pool.size)) {
            val correct = pool.random()
            // build options: include correct + random others
            val others = all.filter { it.id != correct.id }.shuffled().take(optionsPerQuestion - 1).toMutableList()
            val options = (others + correct).shuffled(Random(System.nanoTime()))
            questions.add(QuizQuestion(correct = correct, options = options))
        }
        questions
    }
}

