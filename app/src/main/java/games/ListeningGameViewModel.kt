package com.example.lingogo.games

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Locale

class ListeningGameViewModel(application: Application) : AndroidViewModel(application) {

    // Frases de ejemplo (puedes ampliarlas o moverlas a recursos)
    private val phrases = listOf(
        "Where is the library?",
        "I would like a coffee",
        "What time is it?",
        "My name is John",
        "It is sunny today",
        "Can you help me?",
        "See you later"
    )

    private var currentPhraseIndex = -1
    private var textToSpeech: TextToSpeech? = null

    // Estados del juego
    sealed class ListeningState {
        object LOADING : ListeningState()
        object READY : ListeningState()
        object RESULT : ListeningState()
    }

    private val _gameState = MutableLiveData<ListeningState>()
    val gameState: LiveData<ListeningState> = _gameState

    private val _currentPhraseHidden = MutableLiveData<String>()
    val currentPhraseHidden: LiveData<String> = _currentPhraseHidden

    private val _feedback = MutableLiveData<String>()
    val feedback: LiveData<String> = _feedback

    init {
        _gameState.value = ListeningState.LOADING
        initTTS(application)
    }

    private fun initTTS(app: Application) {
        textToSpeech = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                nextRound()
            }
        }
    }

    fun nextRound() {
        currentPhraseIndex = (currentPhraseIndex + 1) % phrases.size
        val phrase = phrases[currentPhraseIndex]
        // Ocultamos la frase con asteriscos para dar una pista visual de la longitud
        _currentPhraseHidden.value = "* ".repeat(phrase.length / 2) + "?"
        _feedback.value = ""
        _gameState.value = ListeningState.READY
    }

    fun playAudio() {
        val phrase = phrases[currentPhraseIndex]
        textToSpeech?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // --- ¡NUEVA FUNCIÓN! Detiene el audio inmediatamente ---
    fun stopAudio() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
    }

    fun checkAnswer(spokenText: String) {
        val targetPhrase = phrases[currentPhraseIndex]

        // Normalización simple para comparar
        val cleanTarget = targetPhrase.lowercase().replace(Regex("[^a-z ]"), "")
        val cleanSpoken = spokenText.lowercase().replace(Regex("[^a-z ]"), "")
        val isCorrect = cleanTarget == cleanSpoken

        // Revelamos la frase real
        _currentPhraseHidden.value = targetPhrase

        if (isCorrect) {
            _feedback.value = "¡Correcto! 🎉"
        } else {
            _feedback.value = "Casi... Tú dijiste:\n\"$spokenText\""
        }
        _gameState.value = ListeningState.RESULT
    }

    override fun onCleared() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onCleared()
    }
}