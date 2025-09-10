package com.example.lingogo.database
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "high_scores")
data class HighScore(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val playerName: String,
    val score: Int
)