package com.example.dilson.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PointViewModel(private val repo: PointRepository) : ViewModel() {
    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total.asStateFlow()

    init {
        viewModelScope.launch {
            _total.value = repo.getTotalPoints()
            repo.getAllTransactions().collectLatest {
                _total.value = repo.getTotalPoints()
            }
        }
    }

    suspend fun addPoints(amount: Int, reason: String? = null) = repo.addPoints(amount, reason)

    class Factory(private val repo: PointRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PointViewModel::class.java)) {
                return PointViewModel(repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

