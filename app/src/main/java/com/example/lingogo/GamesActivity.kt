package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.lingogo.games.GrammarQuizActivity
import com.lingogo.games.PronunciationChallengeActivity

class GamesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)

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



    }
}
