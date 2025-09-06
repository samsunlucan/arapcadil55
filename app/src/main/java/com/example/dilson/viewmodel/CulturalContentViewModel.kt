package com.example.dilson.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dilson.advanced.CultureContent
import com.example.dilson.repository.AdvancedFeaturesRepository
import com.example.dilson.srs.SrsEntry
import com.example.dilson.srs.SrsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CulturalContentViewModel(private val repo: AdvancedFeaturesRepository, private val srsRepo: SrsRepository? = null) : ViewModel() {
    private val _insertResult = MutableStateFlow<Long?>(null)
    val insertResult: StateFlow<Long?> = _insertResult

    val allItems: Flow<List<CultureContent>> = repo.getAll()

    fun insert(title: String?, text: String, language: String, transliteration: String?) {
        viewModelScope.launch {
            val id = repo.insert(CultureContent(title = title, text = text, language = language, transliteration = transliteration))
            _insertResult.value = id
            // Ensure SRS entry created for new content
            if (id != null && srsRepo != null) {
                val now = System.currentTimeMillis()
                val entry = SrsEntry(contentId = id.toInt(), contentType = "culture", box = 1, lastReviewed = now, nextReview = now)
                srsRepo.insert(entry)
            }
        }
    }

    fun deleteById(id: Int) {
        viewModelScope.launch {
            repo.deleteById(id)
        }
    }

    class Factory(private val repo: AdvancedFeaturesRepository, private val srsRepo: SrsRepository? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CulturalContentViewModel::class.java)) {
                return CulturalContentViewModel(repo, srsRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
