package com.example.lingogo.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyStatsDao {

    @Query("SELECT * FROM daily_stats WHERE date = :todayDate")
    suspend fun getTodayStats(todayDate: String): DailyStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: DailyStats)

    @Query("UPDATE daily_stats SET gamesPlayed = gamesPlayed + 1 WHERE date = :todayDate")
    suspend fun incrementProgress(todayDate: String)
}