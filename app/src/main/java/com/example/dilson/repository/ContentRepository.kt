package com.example.dilson.repository

import com.example.dilson.content.Sentence
import com.example.dilson.content.SentenceDao
import com.example.dilson.content.Word
import com.example.dilson.content.WordDao
import kotlinx.coroutines.flow.Flow

class ContentRepository(private val wordDao: WordDao, private val sentenceDao: SentenceDao) {
    fun getAllWords(): Flow<List<Word>> = wordDao.getAll()
    suspend fun getWordById(id: Int): Word? = wordDao.getById(id)
    suspend fun insertWord(word: Word): Long = wordDao.insert(word)
    suspend fun deleteWord(id: Int) = wordDao.deleteById(id)

    fun getAllSentences(): Flow<List<Sentence>> = sentenceDao.getAll()
    suspend fun getSentenceById(id: Int): Sentence? = sentenceDao.getById(id)
    suspend fun insertSentence(sentence: Sentence): Long = sentenceDao.insert(sentence)
    suspend fun deleteSentence(id: Int) = sentenceDao.deleteById(id)
}

