package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HistoryEntry::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE history_entries ADD COLUMN questionType TEXT NOT NULL DEFAULT 'MULTIPLE_CHOICE'"
                )
                db.execSQL(
                    "ALTER TABLE history_entries ADD COLUMN answerText TEXT"
                )
                // Preserve the meaning of historical failure rows. Successful rows
                // from schema v3 are all MC answers (1..5).
                db.execSQL(
                    "UPDATE history_entries SET questionType = 'ERROR' WHERE answerIndex < 1 OR answerIndex > 5"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screen_pilot_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
