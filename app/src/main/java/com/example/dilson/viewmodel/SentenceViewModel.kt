package com.example.dilson.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dilson.content.Sentence
import com.example.dilson.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SentenceViewModel(private val repo: ContentRepository) : ViewModel() {
    private val _sentences = MutableStateFlow<List<Sentence>>(emptyList())
    val sentences: StateFlow<List<Sentence>> = _sentences.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllSentences().collectLatest { list -> _sentences.value = list }
        }
    }

    fun insert(sentence: Sentence) {
        viewModelScope.launch { repo.insertSentence(sentence) }
    }

    fun delete(id: Int) {
        viewModelScope.launch { repo.deleteSentence(id) }
    }

    class Factory(private val repo: ContentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SentenceViewModel::class.java)) {
                return SentenceViewModel(repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

