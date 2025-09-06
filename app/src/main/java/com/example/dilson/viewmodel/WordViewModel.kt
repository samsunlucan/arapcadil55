package com.example.dilson.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dilson.content.Word
import com.example.dilson.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WordViewModel(private val repo: ContentRepository) : ViewModel() {
    private val _words = MutableStateFlow<List<Word>>(emptyList())
    val words: StateFlow<List<Word>> = _words.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllWords().collectLatest { list -> _words.value = list }
        }
    }

    fun insert(word: Word) {
        viewModelScope.launch { repo.insertWord(word) }
    }

    fun delete(id: Int) {
        viewModelScope.launch { repo.deleteWord(id) }
    }

    class Factory(private val repo: ContentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WordViewModel::class.java)) {
                return WordViewModel(repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

