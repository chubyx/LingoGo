package com.example.lingogo.games
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

// Data class simple para un par de palabras
data class WordPair(
    val id: Int, // Para identificar si coinciden
    val textA: String, // Ej: "Hello"
    val textB: String  // Ej: "Hola"
)

class WordMatchViewModel(application: Application) : AndroidViewModel(application) {

    // --- DATOS (Idioma -> Nivel -> Lista) ---
    private val allPairs = mapOf(
        "English" to mapOf(
            "Easy" to listOf(
                WordPair(1, "Hello", "Hola"),
                WordPair(2, "Cat", "Gato"),
                WordPair(3, "Dog", "Perro"),
                WordPair(4, "Red", "Rojo"),
                WordPair(5, "Blue", "Azul")
            ),
            "Medium" to listOf(
                WordPair(6, "Run", "Correr"),
                WordPair(7, "Eat", "Comer"),
                WordPair(8, "Sleep", "Dormir"),
                WordPair(9, "House", "Casa"),
                WordPair(10, "Water", "Agua")
            ),
            "Hard" to listOf(
                WordPair(11, "Algorithm", "Algoritmo"),
                WordPair(12, "Development", "Desarrollo"),
                WordPair(13, "Database", "Base de datos"),
                WordPair(14, "Network", "Red"),
                WordPair(15, "Software", "Software")
            )
        )
        // Puedes agregar "Spanish" (para aprender inglés desde español) aquí
    )

    private val levelOrder = listOf("Easy", "Medium", "Hard")
    private var currentLanguage = "English"
    private var currentLevel = "Easy"

    // --- LIVE DATA ---
    // Listas separadas para mostrar en la UI
    private val _leftColumn = MutableLiveData<List<WordPair>>()
    val leftColumn: LiveData<List<WordPair>> = _leftColumn

    private val _rightColumn = MutableLiveData<List<WordPair>>()
    val rightColumn: LiveData<List<WordPair>> = _rightColumn

    // Estado del juego
    sealed class MatchState {
        object LOADING : MatchState()
        object PLAYING : MatchState()
        data class MATCHED(val id: Int) : MatchState() // ¡Correcto!
        object MISMATCH : MatchState() // ¡Error!
        data class LEVEL_COMPLETE(val level: String, val nextLevel: String?) : MatchState()
        object ALL_COMPLETE : MatchState()
    }

    private val _gameState = MutableLiveData<MatchState>()
    val gameState: LiveData<MatchState> = _gameState

    private val _progressText = MutableLiveData<String>()
    val progressText: LiveData<String> = _progressText

    // Lógica de selección
    private var selectedIdLeft: Int? = null
    private var selectedIdRight: Int? = null
    private var pairsMatchedCount = 0
    private var totalPairsInLevel = 0

    // --- INICIO ---
    fun setupGame(language: String, level: String) {
        currentLanguage = language
        currentLevel = level
        loadLevel()
    }

    private fun loadLevel() {
        _gameState.value = MatchState.LOADING

        val pairs = allPairs[currentLanguage]?.get(currentLevel) ?: emptyList()
        totalPairsInLevel = pairs.size
        pairsMatchedCount = 0

        // Actualizar texto de progreso
        updateProgress()

        // Columna izquierda: Orden original
        _leftColumn.value = pairs

        // Columna derecha: ¡BARAJADA! (Shuffled)
        _rightColumn.value = pairs.shuffled()

        // Resetear selecciones
        selectedIdLeft = null
        selectedIdRight = null

        _gameState.value = MatchState.PLAYING
    }

    // --- LÓGICA DEL JUEGO ---

    fun selectItemLeft(id: Int) {
        if (_gameState.value is MatchState.MATCHED) return // Evitar clicks rápidos
        selectedIdLeft = id
        checkMatch()
    }

    fun selectItemRight(id: Int) {
        if (_gameState.value is MatchState.MATCHED) return
        selectedIdRight = id
        checkMatch()
    }

    private fun checkMatch() {
        // Solo chequeamos si ambos lados tienen una selección
        if (selectedIdLeft != null && selectedIdRight != null) {
            if (selectedIdLeft == selectedIdRight) {
                // ¡COINCIDENCIA!
                pairsMatchedCount++
                updateProgress()

                // Avisar a la UI que hubo match (para borrar botones o ponerlos verdes)
                _gameState.value = MatchState.MATCHED(selectedIdLeft!!)

                // Verificar si terminó el nivel
                if (pairsMatchedCount == totalPairsInLevel) {
                    handleLevelComplete()
                } else {
                    // Volver a jugando después de un momento (handled in UI usually, but logic resets here)
                    resetSelections()
                }
            } else {
                // ¡ERROR!
                _gameState.value = MatchState.MISMATCH
                resetSelections()
            }
        }
    }

    private fun resetSelections() {
        selectedIdLeft = null
        selectedIdRight = null
    }

    private fun updateProgress() {
        _progressText.value = "Nivel $currentLevel: $pairsMatchedCount / $totalPairsInLevel"
    }

    private fun handleLevelComplete() {
        val currentIndex = levelOrder.indexOf(currentLevel)
        if (currentIndex < levelOrder.size - 1) {
            val nextLevel = levelOrder[currentIndex + 1]
            _gameState.value = MatchState.LEVEL_COMPLETE(currentLevel, nextLevel)
        } else {
            _gameState.value = MatchState.ALL_COMPLETE
        }
    }

    // Función para avanzar de nivel (llamada desde el botón "Siguiente" del diálogo)
    fun loadNextLevel() {
        val currentIndex = levelOrder.indexOf(currentLevel)
        if (currentIndex < levelOrder.size - 1) {
            currentLevel = levelOrder[currentIndex + 1]
            loadLevel()
        }
    }

    fun restartGame() {
        setupGame(currentLanguage, "Easy")
    }
    fun resetGameState() {
        _gameState.value = MatchState.PLAYING
    }
}