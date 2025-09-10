package com.example.lingogo.database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FavoriteWordDao {

    @Insert
    suspend fun addFavorite(word: FavoriteWord)

    @Query("DELETE FROM favorite_words WHERE word = :wordText")
    suspend fun removeFavorite(wordText: String)

    @Query("SELECT * FROM favorite_words")
    suspend fun getAllFavorites(): List<FavoriteWord>
}