package com.example.dilson.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dilson.advanced.CulturalContentDao
import com.example.dilson.advanced.CultureContent
import com.example.dilson.srs.SrsEntry
import com.example.dilson.srs.SrsDao
import com.example.dilson.plan.DailyPlan
import com.example.dilson.plan.DailyPlanDao
import com.example.dilson.content.Word
import com.example.dilson.content.WordDao
import com.example.dilson.content.Sentence
import com.example.dilson.content.SentenceDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CultureContent::class, SrsEntry::class, DailyPlan::class, Word::class, Sentence::class, com.example.dilson.gamification.PointTransaction::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun culturalContentDao(): CulturalContentDao
    abstract fun srsDao(): SrsDao
    abstract fun dailyPlanDao(): DailyPlanDao
    abstract fun wordDao(): WordDao
    abstract fun sentenceDao(): SentenceDao
    abstract fun pointTransactionDao(): com.example.dilson.gamification.PointTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Build the database instance
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(*DatabaseMigrations.ALL)
                    .fallbackToDestructiveMigration()
                    .build()

                // Assign singleton
                INSTANCE = instance

                // Pre-populate asynchronously after instance is assigned
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = instance.culturalContentDao()
                        val srsDao = instance.srsDao()
                        val wordDao = instance.wordDao()
                        val sentenceDao = instance.sentenceDao()
                        val ptDao = instance.pointTransactionDao()

                        val id1 = dao.insert(CultureContent(title = "Merhaba (Arapça)", text = "مرحبا", language = "ar", transliteration = "marhaba")).toInt()
                        val id2 = dao.insert(CultureContent(title = "Teşekkürler", text = "شكرا", language = "ar", transliteration = "shukran")).toInt()

                        srsDao.insert(SrsEntry(contentId = id1, contentType = "culture", box = 1, lastReviewed = System.currentTimeMillis(), nextReview = System.currentTimeMillis()))
                        srsDao.insert(SrsEntry(contentId = id2, contentType = "culture", box = 1, lastReviewed = System.currentTimeMillis(), nextReview = System.currentTimeMillis()))

                        // sample words
                        wordDao.insert(Word(text = "كتاب", transliteration = "kitab", meaning = "kitap", category = "noun", hasDiacritics = false))
                        wordDao.insert(Word(text = "مدرسة", transliteration = "madrasa", meaning = "okul", category = "noun", hasDiacritics = false))

                        // sample sentences
                        sentenceDao.insert(Sentence(text = "أنا أدرس العربية.", transliteration = "Ana adrus al-ʿarabiyya.", translation = "Arapça öğreniyorum.", category = "basic", hasDiacritics = false))
                        sentenceDao.insert(Sentence(text = "كيف حالك؟", transliteration = "Kayfa haluka?", translation = "Nasılsın?", category = "basic", hasDiacritics = false))

                        // initial points
                        ptDao.insert(com.example.dilson.gamification.PointTransaction(amount = 50, reason = "Hoşgeldiniz bonusu"))
                    } catch (_: Exception) {
                        // ignore pre-populate failures in dev
                    }
                }

                instance
            }
        }
    }
}
