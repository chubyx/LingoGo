package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.lingogo.games.GrammarQuizActivity // <-- Import del Quiz
import com.lingogo.games.PronunciationChallengeActivity // <-- Import del de Pronunciación

class GamesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games) // Asegúrate que este sea tu XML

        // --- Conectar el juego de Pronunciación (YA LO TIENES) ---
        // (Buscamos el ID de tu XML: cardPronunciationGame)
        try {
            val pronunciationCard: CardView = findViewById(R.id.cardPronunciationGame)
            pronunciationCard.setOnClickListener {
                val intent = Intent(this, PronunciationChallengeActivity::class.java)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // Manejo de error por si el ID no se encuentra
            e.printStackTrace()
        }


        // --- ¡¡CONEXIÓN DEL JUEGO DE QUIZ!! ---
        // (Buscamos el ID de tu XML: cardGrammarQuiz)
        try {
            val grammarCard: CardView = findViewById(R.id.cardGrammarQuiz)
            grammarCard.setOnClickListener {
                val intent = Intent(this, GrammarQuizActivity::class.java)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // Manejo de error
            e.printStackTrace()
        }

        // --- ¡¡Añade los otros juegos aquí cuando los creemos!! ---

        // Ejemplo:
        /*
        try {
            val listenCard: CardView = findViewById(R.id.cardListeningGame)
            listenCard.setOnClickListener {
                // val intent = Intent(this, ListeningGameActivity::class.java)
                // startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        */

    }
}
