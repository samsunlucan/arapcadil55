package com.example.dilson

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dilson.content.Word
import com.example.dilson.content.Sentence
import com.example.dilson.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseInstrumentedTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadWord() = runBlocking {
        val word = Word(text = "سلام", transliteration = "salam", meaning = "selam", category = "greeting", hasDiacritics = false)
        val id = db.wordDao().insert(word)
        val loaded = db.wordDao().getById(id.toInt())
        assertEquals("سلام", loaded?.text)
        assertEquals("selam", loaded?.meaning)
    }

    @Test
    fun writeAndReadSentence() = runBlocking {
        val sentence = Sentence(text = "مرحبا بك", transliteration = "marhaba bik", translation = "Hoş geldiniz", category = "greeting", hasDiacritics = false)
        val id = db.sentenceDao().insert(sentence)
        val loaded = db.sentenceDao().getById(id.toInt())
        assertEquals("مرحبا بك", loaded?.text)
        assertEquals("Hoş geldiniz", loaded?.translation)
    }
}

