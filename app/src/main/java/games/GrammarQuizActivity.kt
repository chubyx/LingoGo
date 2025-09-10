package com.lingogo.games

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lingogo.R // <-- Asegúrate que este sea tu paquete de R

class GrammarQuizActivity : AppCompatActivity() {

    private val viewModel: GrammarQuizViewModel by viewModels()

    // Referencias de la UI
    private lateinit var scoreTextView: TextView
    private lateinit var progressTextView: TextView
    private lateinit var questionTextView: TextView
    private lateinit var feedbackTextView: TextView
    private lateinit var nextButton: Button
    private lateinit var restartButton: Button // <-- ¡NUEVO BOTÓN!
    private val optionButtons = mutableListOf<Button>()

    // Colores
    private var colorCorrect: Int = 0
    private var colorIncorrect: Int = 0
    private var colorNeutral: Int = 0
    private var colorNeutralText: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grammar_quiz)

        // Inicializar vistas
        scoreTextView = findViewById(R.id.scoreTextView)
        progressTextView = findViewById(R.id.progressTextView)
        questionTextView = findViewById(R.id.questionTextView)
        feedbackTextView = findViewById(R.id.feedbackTextView)
        nextButton = findViewById(R.id.nextButton)
        restartButton = findViewById(R.id.restartButton) // <-- ¡NUEVO!
        optionButtons.add(findViewById(R.id.optionButton1))
        optionButtons.add(findViewById(R.id.optionButton2))
        optionButtons.add(findViewById(R.id.optionButton3))

        // Cargar colores
        colorCorrect = Color.parseColor("#4CAF50") // Verde
        colorIncorrect = Color.parseColor("#F44336") // Rojo
        colorNeutral = Color.parseColor("#EDE7F6") // Color neutro de tus botones
        colorNeutralText = Color.parseColor("#5E35B1") // Texto neutro

        // Configurar Listeners de botones
        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                viewModel.checkAnswer(index)
            }
        }

        nextButton.setOnClickListener {
            viewModel.loadNextQuestion()
        }

        // --- ¡¡EL LISTENER SE MOVERÁ!! ---
        // El listener del 'restartButton' ahora se asigna dinámicamente
        // en 'showLevelFinished' y 'showAllFinished'
        // --- FIN DE LA NOTA ---

        // Observar el ViewModel
        observeViewModel()

        // --- ¡ACTUALIZADO! ---
        // Iniciar el Quiz (Hardcodeado a Inglés, Nivel Easy por ahora)
        // En el futuro, puedes pasar esto desde la 'GamesActivity'
        viewModel.setupQuiz("English", "Easy")
    }

    private fun observeViewModel() {

        // Observar la pregunta actual
        viewModel.currentQuestion.observe(this) { question ->
            questionTextView.text = question.questionText
            // Asignar texto a los botones de opción
            optionButtons.forEachIndexed { index, button ->
                if (index < question.options.size) {
                    button.text = question.options[index]
                    button.visibility = View.VISIBLE
                } else {
                    button.visibility = View.GONE
                }
            }
        }

        // --- ¡OBSERVADOR ACTUALIZADO CON NUEVOS ESTADOS! ---
        viewModel.quizState.observe(this) { state ->
            when (state) {
                is GrammarQuizViewModel.QuizState.LOADING -> {
                    resetUIForNewQuestion()
                    feedbackTextView.text = "Cargando..."
                }
                is GrammarQuizViewModel.QuizState.QUESTION -> {
                    resetUIForNewQuestion()
                }
                is GrammarQuizViewModel.QuizState.ANSWERED -> {
                    showAnswerResult(state.isCorrect, state.correctAnswerText)
                }
                is GrammarQuizViewModel.QuizState.LEVEL_FINISHED -> {
                    showLevelFinished(state.levelName, state.finalScore, state.totalQuestions, state.nextLevelName)
                }
                is GrammarQuizViewModel.QuizState.ALL_FINISHED -> {
                    showAllFinished(state.finalScore, state.totalQuestions)
                }
            }
        }
        // --- FIN DE LA ACTUALIZACIÓN ---

        // Observar el puntaje y progreso
        viewModel.scoreText.observe(this) { scoreTextView.text = it }
        viewModel.progressText.observe(this) { progressTextView.text = it }
    }

    private fun resetUIForNewQuestion() {
        feedbackTextView.text = ""
        nextButton.visibility = View.INVISIBLE
        // --- ¡¡AQUÍ ESTÁ LA LÍNEA QUE FALTABA!! ---
        // Reiniciamos el texto del botón
        nextButton.text = "Siguiente"
        // --- FIN DE LA CORRECCIÓN ---
        restartButton.visibility = View.INVISIBLE // <-- Ocultar reinicio
        optionButtons.forEach { button ->
            button.isEnabled = true
            button.setBackgroundColor(colorNeutral)
            button.setTextColor(colorNeutralText)
            button.visibility = View.VISIBLE
        }
    }

    private fun showAnswerResult(isCorrect: Boolean, correctAnswerText: String) {
        // Desactivar botones de opción
        optionButtons.forEach { it.isEnabled = false }

        // Mostrar feedback
        if (isCorrect) {
            feedbackTextView.text = "¡Correcto!"
            feedbackTextView.setTextColor(colorCorrect)
        } else {
            feedbackTextView.text = "Incorrecto. La respuesta era: $correctAnswerText"
            feedbackTextView.setTextColor(colorIncorrect)
        }

        // Mostrar el botón "Siguiente" después de un breve retraso
        Handler(Looper.getMainLooper()).postDelayed({
            nextButton.visibility = View.VISIBLE
        }, 1000) // 1 segundo de retraso
    }

    // --- ¡NUEVA FUNCIÓN DE UI! ---
    private fun showLevelFinished(levelName: String, score: Int, total: Int, nextLevelName: String) {
        questionTextView.text = "¡Nivel $levelName Completo!"
        feedbackTextView.text = "Tu puntaje: $score / $total"
        feedbackTextView.setTextColor(Color.BLACK)

        nextButton.visibility = View.VISIBLE
        nextButton.text = "Empezar Nivel $nextLevelName" // El botón 'Siguiente' ahora carga el próximo nivel

        restartButton.visibility = View.VISIBLE // Mostrar botón de reinicio
        restartButton.text = "Reintentar Nivel $levelName"
        // --- ¡¡NUEVO LISTENER!! ---
        restartButton.setOnClickListener {
            viewModel.restartCurrentLevel()
        }
        // --- FIN DE LO NUEVO ---

        optionButtons.forEach { it.visibility = View.GONE }
    }

    // --- ¡NUEVA FUNCIÓN DE UI! ---
    private fun showAllFinished(score: Int, total: Int) {
        questionTextView.text = "¡Quiz Terminado!"
        feedbackTextView.text = "Tu puntaje final es: $score / $total"
        feedbackTextView.setTextColor(Color.BLACK)

        nextButton.visibility = View.GONE // Ocultar botón 'Siguiente'

        restartButton.visibility = View.VISIBLE // Mostrar botón de reinicio

        // --- ¡¡LÓGICA ACTUALIZADA!! ---
        restartButton.text = "Reiniciar Idioma Completo"
        restartButton.setOnClickListener {
            viewModel.restartEntireQuiz()
        }
        // --- FIN DE LA ACTUALIZACIÓN ---

        optionButtons.forEach { it.visibility = View.GONE }
    }
}

