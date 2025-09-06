package com.example.dilson.srs

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dilson.advanced.CultureContent
import com.example.dilson.data.AppDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SrsDaoAndroidTest {

    private lateinit var db: AppDatabase
    private lateinit var srsDao: SrsDao

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        srsDao = db.srsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getDueWithContent_returnsRelatedContent() = runBlocking {
        val contentId = db.culturalContentDao().insert(
            CultureContent(
                title = "Test Başlık",
                text = "نص",
                language = "ar",
                transliteration = "nass"
            )
        ).toInt()

        val now = System.currentTimeMillis()
        srsDao.insert(
            SrsEntry(
                contentId = contentId,
                contentType = "culture",
                box = 1,
                lastReviewed = now - 1_000,
                nextReview = now - 500
            )
        )

        val result = srsDao.getDueWithContent(now).first()

        assertEquals(1, result.size)
        assertEquals("Test Başlık", result.first().content.title)
        assertEquals(contentId, result.first().srs.contentId)
    }
}

