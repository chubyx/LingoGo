package com.lingogo.games

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// Data class para guardar la estructura de una pregunta
data class GrammarQuestion(
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)

class GrammarQuizViewModel : ViewModel() {

    // --- ¡¡NUEVA ESTRUCTURA DE PREGUNTAS!! ---
    // Map<Idioma, Map<Nivel, Lista_de_Preguntas>>
    private val allQuestions = mapOf(
        "English" to mapOf(
            "Easy" to listOf(
                GrammarQuestion("She ___ apples every day.", listOf("eat", "eats", "eating"), 1),
                GrammarQuestion("I am ___ to the park.", listOf("go", "goes", "going"), 2),
                GrammarQuestion("The book is ___ the table.", listOf("on", "in", "at"), 0)
            ),
            "Medium" to listOf(
                GrammarQuestion("He didn't ___ the movie.", listOf("see", "saw", "seen"), 0),
                GrammarQuestion("They are ___ than us.", listOf("tall", "taller", "tallest"), 1),
                GrammarQuestion("I have ___ been to Paris.", listOf("never", "ever", "already"), 0)
            ),
            "Hard" to listOf(
                GrammarQuestion("If I ___ you, I would study more.", listOf("was", "am", "were"), 2),
                GrammarQuestion("He insisted ___ paying the bill.", listOf("on", "to", "in"), 0)
            )
        ),
        "Spanish" to mapOf(
            "Easy" to listOf(
                GrammarQuestion("Yo ___ (comer) una manzana.", listOf("como", "comes", "come"), 0),
                GrammarQuestion("Ellos ___ (ser) amigos.", listOf("es", "eres", "son"), 2)
            ),
            "Medium" to listOf(
                GrammarQuestion("La casa es ___ (grande).", listOf("grande", "grandes", "granda"), 0),
                GrammarQuestion("Me ___ (gustar) los libros.", listOf("gusta", "gustan", "gusto"), 1)
            )
        )
    )
    // --- Fin de la Lista de Preguntas ---

    // --- ¡NUEVO! Orden de niveles ---
    private val levelOrder = listOf("Easy", "Medium", "Hard")
    private var currentLanguage = "English"
    private var currentLevel = "Easy"
    // --- FIN DE LO NUEVO ---

    private var questionList: List<GrammarQuestion> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0

    // --- LiveData para la UI ---
    private val _currentQuestion = MutableLiveData<GrammarQuestion>()
    val currentQuestion: LiveData<GrammarQuestion> = _currentQuestion

    private val _quizState = MutableLiveData<QuizState>()
    val quizState: LiveData<QuizState> = _quizState

    private val _scoreText = MutableLiveData<String>()
    val scoreText: LiveData<String> = _scoreText

    private val _progressText = MutableLiveData<String>()
    val progressText: LiveData<String> = _progressText

    // --- ¡ESTADOS DEL JUEGO ACTUALIZADOS! ---
    sealed class QuizState {
        object LOADING : QuizState()
        object QUESTION : QuizState() // Mostrando pregunta, esperando respuesta
        data class ANSWERED(val isCorrect: Boolean, val correctAnswerText: String) : QuizState()
        // Estado para cuando se termina un nivel (pero no todos)
        data class LEVEL_FINISHED(val levelName: String, val finalScore: Int, val totalQuestions: Int, val nextLevelName: String) : QuizState()
        // Estado para cuando se terminan TODOS los niveles de un idioma
        data class ALL_FINISHED(val finalScore: Int, val totalQuestions: Int) : QuizState()
    }

    // --- Lógica del Juego ---

    fun setupQuiz(language: String, level: String) {
        currentLanguage = language
        currentLevel = level
        questionList = allQuestions[currentLanguage]?.get(currentLevel) ?: emptyList()
        currentQuestionIndex = -1
        score = 0
        _quizState.value = QuizState.LOADING
        loadNextQuestion()
    }

    // --- ¡FUNCIÓN ACTUALIZADA CON LÓGICA DE NIVEL! ---
    fun loadNextQuestion() {
        currentQuestionIndex++

        // Comprobar si se acabó el quiz del nivel actual
        if (currentQuestionIndex >= questionList.size) {

            // --- ¡NUEVA LÓGICA DE PROGRESIÓN DE NIVEL! ---
            val currentLevelIndex = levelOrder.indexOf(currentLevel)

            // Comprobar si hay un siguiente nivel
            if (currentLevelIndex < levelOrder.size - 1) {
                // Hay un siguiente nivel
                val nextLevel = levelOrder[currentLevelIndex + 1]
                _quizState.postValue(QuizState.LEVEL_FINISHED(currentLevel, score, questionList.size, nextLevel))

                // Preparamos el siguiente nivel
                currentLevel = nextLevel
                questionList = allQuestions[currentLanguage]?.get(currentLevel) ?: emptyList()
                currentQuestionIndex = -1
                score = 0 // Reiniciar puntaje para el nuevo nivel

            } else {
                // Se terminaron todos los niveles
                _quizState.postValue(QuizState.ALL_FINISHED(score, questionList.size))
            }
            // --- FIN DE LÓGICA ---

        } else {
            // Cargar la siguiente pregunta
            _currentQuestion.postValue(questionList[currentQuestionIndex])
            _scoreText.postValue("Puntaje: $score")
            _progressText.postValue("[$currentLevel] ${currentQuestionIndex + 1} / ${questionList.size}")
            _quizState.postValue(QuizState.QUESTION)
        }
    }

    fun checkAnswer(selectedOptionIndex: Int) {
        val currentQuestion = _currentQuestion.value ?: return

        val correctAnswerText = currentQuestion.options[currentQuestion.correctAnswerIndex]

        if (selectedOptionIndex == currentQuestion.correctAnswerIndex) {
            // Respuesta Correcta
            score++
            _scoreText.postValue("Puntaje: $score")
            _quizState.postValue(QuizState.ANSWERED(isCorrect = true, correctAnswerText = correctAnswerText))
        } else {
            // Respuesta Incorrecta
            _quizState.postValue(QuizState.ANSWERED(isCorrect = false, correctAnswerText = correctAnswerText))
        }
    }

    // --- ¡NUEVA FUNCIÓN DE REINICIO! ---
    fun restartCurrentLevel() {
        // Reinicia el nivel actual
        currentQuestionIndex = -1
        score = 0
        loadNextQuestion()
    }

    // --- ¡¡NUEVA FUNCIÓN PARA REINICIAR TODO!! ---
    fun restartEntireQuiz() {
        // Llama a setupQuiz con el primer nivel del idioma actual
        setupQuiz(currentLanguage, levelOrder.firstOrNull() ?: "Easy")
    }
}

