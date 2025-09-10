package com.example.lingogo.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Ensure all entities are imported and listed here
@Database(entities = [FavoriteWord::class, HighScore::class, DailyStats::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteWordDao(): FavoriteWordDao
    abstract fun highScoreDao(): HighScoreDao
    abstract fun dailyStatsDao(): DailyStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lingogo_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}