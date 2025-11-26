package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope

// Base de Datos
import com.example.lingogo.database.AppDatabase
import com.example.lingogo.database.DailyStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

// Juegos
import com.example.lingogo.games.ListeningGameActivity
import com.example.lingogo.games.WordMatchActivity
import com.example.lingogo.games.StoryBuilderActivity
import com.lingogo.games.GrammarQuizActivity
import com.lingogo.games.PronunciationChallengeActivity

class GamesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressPercent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)

        // Inicializar BD
        db = AppDatabase.getDatabase(applicationContext)

        // Enlazar Vistas
        progressBar = findViewById(R.id.progressBarDaily)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)

        setupGameCards()
        loadDailyProgress()
    }

    override fun onResume() {
        super.onResume()
        loadDailyProgress()
    }

    private fun loadDailyProgress() {
        lifecycleScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Consultar stats de hoy
            var stats = db.dailyStatsDao().getTodayStats(today)

            if (stats == null) {
                stats = DailyStats(date = today, gamesPlayed = 0, dailyGoal = 5)
                db.dailyStatsDao().insertOrUpdate(stats)
            }

            // Calcular porcentaje
            val progress = stats.gamesPlayed
            val max = stats.dailyGoal
            val percentage = if (max > 0) (progress * 100) / max else 0

            // UI
            progressBar.max = max
            progressBar.progress = progress
            tvProgressPercent.text = "$percentage%"
        }
    }

    private fun incrementProgress() {
        lifecycleScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val stats = db.dailyStatsDao().getTodayStats(today)

            if (stats == null) {
                db.dailyStatsDao().insertOrUpdate(DailyStats(date = today, gamesPlayed = 1))
            } else {
                db.dailyStatsDao().incrementProgress(today)
            }
            loadDailyProgress()
        }
    }

    private fun setupGameCards() {
        // 1. Conecta Palabras
        findViewById<CardView>(R.id.cardWordMatch).setOnClickListener {
            startActivity(Intent(this, WordMatchActivity::class.java))
        }

        // 2. Escucha y Repite
        findViewById<CardView>(R.id.cardListeningGame).setOnClickListener {
            startActivity(Intent(this, ListeningGameActivity::class.java))
        }

        // 3. Quiz Gramática
        findViewById<CardView>(R.id.cardGrammarQuiz).setOnClickListener {
            startActivity(Intent(this, GrammarQuizActivity::class.java))
        }

        // 4. Crea Historias
        findViewById<CardView>(R.id.cardStoryBuilder).setOnClickListener {
            startActivity(Intent(this, StoryBuilderActivity::class.java))
        }

        // 5. Desafío Pronunciación
        findViewById<CardView>(R.id.cardPronunciationGame).setOnClickListener {
            startActivity(Intent(this, PronunciationChallengeActivity::class.java))
        }

        // Botón Desafío Diario (Juego Aleatorio)
        val btnStartDaily = findViewById<Button>(R.id.btnStartDaily)
        btnStartDaily.setOnClickListener {

            // 1. Registrar avance
            incrementProgress()
            Toast.makeText(this, "¡Desafío iniciado! (+1)", Toast.LENGTH_SHORT).show()

            // 2. Lista de juegos disponibles
            val availableGames = listOf(
                WordMatchActivity::class.java,
                ListeningGameActivity::class.java,
                GrammarQuizActivity::class.java,
                StoryBuilderActivity::class.java,
                PronunciationChallengeActivity::class.java
            )

            // 3. Elegir uno al azar
            val randomGame = availableGames.random()

            // 4. Iniciar
            val intent = Intent(this, randomGame)
            // Flag opcional por si el juego necesita saberlo
            intent.putExtra("IS_DAILY_CHALLENGE", true)
            startActivity(intent)
        }
    }
}