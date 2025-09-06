package com.example.dilson.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dilson.srs.SrsRepository
import com.example.dilson.srs.SrsWithContent
import com.example.dilson.gamification.PointRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SrsViewModel(private val repo: SrsRepository, private val pointRepo: PointRepository? = null) : ViewModel() {
    // expose due items as StateFlow
    val now: Long
        get() = System.currentTimeMillis()

    private val intervals = mapOf(
        1 to 1L * 24 * 60 * 60 * 1000, // 1 day
        2 to 3L * 24 * 60 * 60 * 1000, // 3 days
        3 to 7L * 24 * 60 * 60 * 1000, // 7 days
        4 to 14L * 24 * 60 * 60 * 1000, // 14 days
        5 to 30L * 24 * 60 * 60 * 1000 // 30 days
    )

    val dueItems: StateFlow<List<SrsWithContent>> = repo.getDueWithContent(now)
        .map { it }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun reviewKnown(srs: SrsWithContent) {
        viewModelScope.launch {
            val currentBox = srs.srs.box.coerceAtLeast(1)
            val nextBox = (currentBox + 1).coerceAtMost(5)
            val nextReview = scheduleNext(nextBox)
            val updated = srs.srs.copy(box = nextBox, lastReviewed = now, nextReview = nextReview)
            repo.update(updated)
            try { pointRepo?.addPoints(5, "SRS: Biliyorum") } catch (_: Exception) {}
        }
    }

    fun reviewUnsure(srs: SrsWithContent) {
        viewModelScope.launch {
            val currentBox = srs.srs.box.coerceAtLeast(1)
            val nextBox = currentBox // keep same
            val nextReview = scheduleNext(nextBox)
            val updated = srs.srs.copy(box = nextBox, lastReviewed = now, nextReview = nextReview)
            repo.update(updated)
            try { pointRepo?.addPoints(2, "SRS: Emin Değilim") } catch (_: Exception) {}
        }
    }

    fun reviewUnknown(srs: SrsWithContent) {
        viewModelScope.launch {
            val nextBox = 1
            val nextReview = scheduleNext(nextBox)
            val updated = srs.srs.copy(box = nextBox, lastReviewed = now, nextReview = nextReview)
            repo.update(updated)
            try { pointRepo?.addPoints(1, "SRS: Bilmiyorum") } catch (_: Exception) {}
        }
    }

    suspend fun ensureSrsForContent(contentId: Int, contentType: String = "culture") {
        val existing = repo.findByContent(contentId, contentType)
        if (existing == null) {
            val now = System.currentTimeMillis()
            val entry = com.example.dilson.srs.SrsEntry(contentId = contentId, contentType = contentType, box = 1, lastReviewed = now, nextReview = now)
            repo.insert(entry)
        }
    }

    private fun scheduleNext(box: Int): Long {
        val interval = intervals[box] ?: intervals[1]!!
        return System.currentTimeMillis() + interval
    }

    class Factory(private val repo: SrsRepository, private val pointRepo: PointRepository? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SrsViewModel::class.java)) {
                return SrsViewModel(repo, pointRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
