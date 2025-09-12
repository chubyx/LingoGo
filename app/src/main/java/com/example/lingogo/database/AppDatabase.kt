package com.example.lingogo.database
import com.example.lingogo.database.FavoriteWord
import com.example.lingogo.database.HighScore
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [HighScore::class, FavoriteWord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun highScoreDao(): HighScoreDao
    abstract fun favoriteWordDao(): FavoriteWordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lingogo_database" // Nombre del archivo de la base de datos
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}