package com.example.lingogo.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey
    val date: String,
    val gamesPlayed: Int = 0,
    val dailyGoal: Int = 5
)