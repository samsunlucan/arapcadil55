package com.example.dilson.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuizViewModel(private val repo: QuizRepository) : ViewModel() {
    private val _questions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val questions: StateFlow<List<QuizQuestion>> = _questions.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    fun loadQuestions(count: Int = 10, options: Int = 4) {
        viewModelScope.launch {
            val qs = repo.generateQuestions(count, options)
            _questions.value = qs
            _currentIndex.value = 0
            _score.value = 0
            _completed.value = qs.isEmpty()
        }
    }

    fun answerSelected(optionIndex: Int) {
        val qs = _questions.value
        val idx = _currentIndex.value
        if (idx >= qs.size) return
        val q = qs[idx]
        val selected = q.options[optionIndex]
        if (selected.id == q.correct.id) {
            _score.value = _score.value + 1
        }
        if (idx + 1 >= qs.size) {
            _completed.value = true
        } else {
            _currentIndex.value = idx + 1
        }
    }

    class Factory(private val repo: QuizRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
                return QuizViewModel(repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

