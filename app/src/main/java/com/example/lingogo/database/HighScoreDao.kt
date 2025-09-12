package com.example.lingogo.database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HighScoreDao {

    // Inserta un nuevo puntaje. Si ya existe, lo reemplaza
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(highScore: HighScore)

    // Obtiene el puntaje más alto de todos
    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 1")
    suspend fun getHighestScore(): HighScore?

    // Obtiene todos los puntajes
    @Query("SELECT * FROM high_scores ORDER BY score DESC")
    suspend fun getAllScores(): List<HighScore>
}