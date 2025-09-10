package com.example.lingogo.database
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_words")
data class FavoriteWord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val word: String
)