package com.example.dilson.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create srs_entry table if it does not exist
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS srs_entry (
                  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  contentId INTEGER NOT NULL,
                  contentType TEXT NOT NULL,
                  box INTEGER NOT NULL,
                  lastReviewed INTEGER NOT NULL,
                  nextReview INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create daily_plan table if missing
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS daily_plan (
                  date INTEGER PRIMARY KEY NOT NULL,
                  targetWords INTEGER NOT NULL,
                  targetSentences INTEGER NOT NULL,
                  completedWords INTEGER NOT NULL,
                  completedSentences INTEGER NOT NULL,
                  streak INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create word and sentence tables if missing
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS word (
                  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  text TEXT NOT NULL,
                  transliteration TEXT,
                  meaning TEXT,
                  category TEXT,
                  hasDiacritics INTEGER NOT NULL
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS sentence (
                  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  text TEXT NOT NULL,
                  transliteration TEXT,
                  translation TEXT,
                  category TEXT,
                  hasDiacritics INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create point_transaction table if missing
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS point_transaction (
                  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  amount INTEGER NOT NULL,
                  reason TEXT,
                  timestamp INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
