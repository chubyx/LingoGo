package com.lingogo.games

import android.app.Application
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Locale
import android.os.Bundle

class PronunciationChallengeViewModel(application: Application) : AndroidViewModel(application) {

    // --- ¡¡GRAN CAMBIO ESTRUCTURAL!! ---
    // Ahora las frases están anidadas por Idioma y Nivel
    // Map<Idioma, Map<Nivel, Lista_de_Frases>>
    private val phrasesByLanguageAndLevel = mapOf(
        "English" to mapOf(
            "Easy" to listOf(
                "Hello",
                "How are you?",
                "Good morning",
                "What is your name?",
                "Yes",
                "No",
                "Thank you"
            ),
            "Medium" to listOf(
                "What time is it?",
                "Where is the nearest restaurant?",
                "I am learning to speak English",
                "Can you help me with this?",
                "Practice makes perfect"
            ),
            "Hard" to listOf(
                "The quick brown fox jumps over the lazy dog",
                "She sells seashells by the seashore",
                "I appreciate your comprehensive explanation",
                "Please, could you repeat that more slowly?"
            )
        ),
        "Spanish" to mapOf(
            "Easy" to listOf(
                "Hola",
                "¿Cómo estás?",
                "Buenos días",
                "¿Cómo te llamas?",
                "Sí",
                "No",
                "Gracias"
            ),
            "Medium" to listOf(
                "¿Qué hora es?",
                "¿Dónde está el restaurante más cercano?",
                "Estoy aprendiendo a hablar español",
                "¿Puedes ayudarme con esto?"
            ),
            "Hard" to listOf(
                "El rápido zorro marrón salta sobre el perro perezoso",
                "Tres tristes tigres, tragaban trigo en un trigal",
                "Agradezco tu exhaustiva explicación"
            )
        )
    )
    // --- FIN DEL CAMBIO ESTRUCTURAL ---

    // --- ¡NUEVO! Define el orden de progresión ---
    private val levelOrder = listOf("Easy", "Medium", "Hard")

    // --- ¡NUEVO! Mapa de idiomas a Locales de TTS ---
    private val languageToLocaleMap = mapOf(
        "English" to Locale.US,
        "Spanish" to Locale.forLanguageTag("es-ES")
        // Puedes añadir más: "French" to Locale.FRANCE, "German" to Locale.GERMANY, etc.
    )

    // --- Estado del Nivel ---
    private var currentLanguage = "English" // Ahora podemos cambiar el idioma
    private var currentUserLevel = "Easy"
    private val completedPhrases = mutableSetOf<String>()

    // --- LiveData para el Progreso ---
    data class ProgressState(val completed: Int, val total: Int)
    private val _progressState = MutableLiveData<ProgressState>()
    val progressState: LiveData<ProgressState> = _progressState

    // --- Evento para Nivel Completo ---
    private val _levelCompleteEvent = MutableLiveData<String?>()
    val levelCompleteEvent: LiveData<String?> = _levelCompleteEvent

    // --- LiveData existentes ---
    private val _currentPhrase = MutableLiveData<String>()
    val currentPhrase: LiveData<String> = _currentPhrase

    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _resultFeedback = MutableLiveData<String>()
    val resultFeedback: LiveData<String> = _resultFeedback

    private val _challengeLocale = MutableLiveData<Locale>(Locale.US)
    val challengeLocale: LiveData<Locale> = _challengeLocale

    private lateinit var textToSpeech: TextToSpeech

    init {
        _gameState.value = GameState.LOADING
        initializeTextToSpeech(application)
        updateProgress()
    }

    private fun initializeTextToSpeech(application: Application) {
        textToSpeech = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Obtenemos el Locale del idioma actual
                val targetLocale = languageToLocaleMap[currentLanguage] ?: Locale.US
                findAndSetVoice(targetLocale)
            } else {
                Log.e("TTS_Init", "¡Falló la inicialización del motor TextToSpeech! Código de estado: $status")
                _gameState.postValue(GameState.ERROR("Error al inicializar TextToSpeech"))
            }
        }

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _gameState.postValue(GameState.SPEAKING) }
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "SPEAK_PHRASE") { _gameState.postValue(GameState.READY_TO_RECORD) }
            }
            override fun onError(utteranceId: String?) { _gameState.postValue(GameState.ERROR("Error en TTS")) }
        })
    }

    // --- ¡¡FUNCIÓN RENOMBRADA Y MEJORADA!! ---
    // Ahora busca CUALQUIER idioma, no solo Inglés
    private fun findAndSetVoice(targetLocale: Locale) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.e("TTS_Setup", "La búsqueda de voces no está soportada en esta API. Intentando método antiguo.")
            setChallengeLocale(targetLocale)
            return
        }

        try {
            val allVoices = textToSpeech.voices
            if (allVoices == null || allVoices.isEmpty()) {
                Log.e("TTS_Setup", "El motor de TTS no reportó ninguna voz. Usando método antiguo.")
                setChallengeLocale(targetLocale)
                return
            }

            // 1. Buscamos la voz "ideal" (ej. Español de España, offline)
            var bestVoice = allVoices.find {
                it.locale == targetLocale && !it.isNetworkConnectionRequired
            }

            // 2. Si no, buscamos CUALQUIER voz en ese idioma (ej. Español de México, offline)
            if (bestVoice == null) {
                bestVoice = allVoices.find {
                    it.locale.language == targetLocale.language && !it.isNetworkConnectionRequired
                }
            }

            // 3. Si no, buscamos CUALQUIER voz en ese idioma (incluso online)
            if (bestVoice == null) {
                bestVoice = allVoices.find {
                    it.locale.language == targetLocale.language
                }
            }

            // 4. Comprobamos si encontramos algo
            if (bestVoice != null) {
                textToSpeech.voice = bestVoice
                _challengeLocale.postValue(bestVoice.locale)
                _gameState.postValue(GameState.READY)
                Log.i("TTS_Setup", "¡Voz encontrada y establecida exitosamente! Voz: ${bestVoice.name}, Locale: ${bestVoice.locale}")
            } else {
                // Modo optimista: asumimos que la red funcionará
                Log.w("TTS_Setup", "No se encontró una voz pre-instalada para [${targetLocale.language}]. Se intentará usar la síntesis de red.")
                textToSpeech.language = targetLocale
                _gameState.postValue(GameState.READY)
            }

        } catch (e: Exception) {
            Log.e("TTS_Setup", "Excepción al obtener lista de voces. Usando método antiguo.", e)
            setChallengeLocale(targetLocale)
        }
    }
    // --- FIN DE LA FUNCIÓN MEJORADA ---

    // Esta función ahora es solo un "plan B"
    fun setChallengeLocale(locale: Locale) {
        _challengeLocale.value = locale
        if (!::textToSpeech.isInitialized) {
            Log.w("TTS_Setup", "setChallengeLocale llamado pero TTS no está inicializado.")
            return
        }
        val result = textToSpeech.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("TTS_Setup", "Método 'setChallengeLocale' (Plan B) falló para ${locale.toLanguageTag()}. Confiando en la red.")
            _gameState.postValue(GameState.READY) // Modo optimista
        } else {
            Log.i("TTS_Setup", "Método 'setChallengeLocale' (Plan B) funcionó. Locale: ${locale.toLanguageTag()}")
            _gameState.postValue(GameState.READY)
        }
    }

    // --- ¡NUEVO! Función pública para configurar el juego ---
    /**
     * Configura el idioma y el nivel para el desafío.
     * Debe llamarse desde la Activity ANTES de empezar el juego.
     */
    fun setLanguageAndLevel(language: String, level: String) {
        currentLanguage = language
        currentUserLevel = level

        // Cargar el Locale correcto
        val targetLocale = languageToLocaleMap[currentLanguage] ?: Locale.US // Default a US

        // Limpiar el progreso
        completedPhrases.clear()
        updateProgress()

        // Configurar el motor de TTS para el nuevo idioma
        findAndSetVoice(targetLocale)

        // Ponemos el juego en estado LISTO (READY)
        // _gameState.postValue(GameState.READY) // findAndSetVoice ya hace esto
    }


    fun startNextChallenge() {
        // Obtenemos el mapa de niveles para el idioma actual
        val phraseMapForLanguage = phrasesByLanguageAndLevel[currentLanguage]
            ?: phrasesByLanguageAndLevel["English"]!! // Default a Inglés si hay error

        // Obtenemos la lista de frases para el nivel actual
        val phraseListForLevel = phraseMapForLanguage[currentUserLevel]
            ?: phraseMapForLanguage["Easy"]!! // Default a Easy si hay error

        val availablePhrases = phraseListForLevel.filter { it !in completedPhrases }

        // --- ¡¡LÓGICA DE PROGRESIÓN DE NIVEL!! ---
        if (availablePhrases.isEmpty()) {

            // 1. Encontrar el índice del nivel actual
            val currentLevelIndex = levelOrder.indexOf(currentUserLevel)

            // 2. Comprobar si hay un siguiente nivel
            if (currentLevelIndex < levelOrder.size - 1) {
                // ¡Hay un siguiente nivel!
                val nextLevel = levelOrder[currentLevelIndex + 1]
                _levelCompleteEvent.postValue("¡Nivel $currentUserLevel Completo!\nEmpezando Nivel $nextLevel.")
                currentUserLevel = nextLevel // Avanzamos al siguiente nivel

                // Reiniciamos el progreso (solo para las frases del nivel anterior)
                resetProgressForLevel(phraseListForLevel)

                // Empezamos el desafío con el nuevo nivel (se llamará a updateProgress)
                startNextChallenge()

            } else {
                // ¡Se completaron todos los niveles de este idioma!
                _levelCompleteEvent.postValue("¡Felicidades! ¡Completaste todos los niveles de $currentLanguage!")
                // Reiniciamos el progreso del último nivel
                resetProgressForLevel(phraseListForLevel)
                // Opcional: Volver al nivel "Easy"
                // currentUserLevel = levelOrder[0]
                // updateProgress()
            }
            // --- FIN DE LA LÓGICA DE PROGRESIÓN ---

        } else {
            // Todavía hay frases
            var newPhrase = availablePhrases.random()
            while (newPhrase == _currentPhrase.value && availablePhrases.size > 1) {
                newPhrase = availablePhrases.random()
            }
            _currentPhrase.value = newPhrase
            speakPhrase(newPhrase)
            updateProgress() // Actualizamos la barra de progreso
        }
    }


    private fun speakPhrase(phrase: String) {
        textToSpeech.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "SPEAK_PHRASE")
    }

    fun repeatCurrentPhrase() {
        _currentPhrase.value?.let { speakPhrase(it) }
    }

    fun onSpeechResult(userSpeech: String) {
        _gameState.value = GameState.ANALYZING
        val originalPhrase = _currentPhrase.value ?: ""
        val score = calculateSimilarity(originalPhrase, userSpeech)
        val feedback = "Tu pronunciación: \"$userSpeech\"\n" +
                "Original: \"$originalPhrase\"\n" +
                "Similitud: ${"%.2f".format(score * 100)}%"
        _resultFeedback.postValue(feedback)

        if (score == 1.0) {
            markPhraseAsCompleted(originalPhrase)
            updateProgress()
        }
        _gameState.postValue(GameState.RESULT_SHOWN)
    }

    private fun calculateSimilarity(original: String, spoken: String): Double {
        val punctuation = Regex("[.,?!]")
        // --- ¡CAMBIO! Usamos el Locale del desafío para toLowerCase ---
        val locale = _challengeLocale.value ?: Locale.US
        val cleanOriginal = original.toLowerCase(locale).replace(punctuation, "")
        val cleanSpoken = spoken.toLowerCase(locale).replace(punctuation, "")
        // --- FIN DEL CAMBIO ---

        if (cleanOriginal == cleanSpoken) {
            return 1.0
        }
        val originalWords = cleanOriginal.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val spokenWords = cleanSpoken.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (originalWords.isEmpty()) return if (spokenWords.isEmpty()) 1.0 else 0.0
        if (spokenWords.isEmpty()) return 0.0
        var correctWords = 0
        for (i in 0 until minOf(originalWords.size, spokenWords.size)) {
            if (originalWords[i] == spokenWords[i]) { correctWords++ }
        }
        return correctWords.toDouble() / maxOf(originalWords.size, spokenWords.size).toDouble()
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    fun setGameStateRecording() { _gameState.value = GameState.RECORDING }
    fun setGameStateReadyToRecord() { _gameState.value = GameState.READY_TO_RECORD }

    private fun markPhraseAsCompleted(phrase: String) {
        if (phrase.isNotEmpty()) { completedPhrases.add(phrase) }
    }

    private fun resetProgressForLevel(phrasesInLevel: List<String>) {
        completedPhrases.removeAll(phrasesInLevel)
    }

    private fun updateProgress() {
        val phraseMapForLanguage = phrasesByLanguageAndLevel[currentLanguage] ?: emptyMap()
        val total = phraseMapForLanguage[currentUserLevel]?.size ?: 0

        val completed = (phraseMapForLanguage[currentUserLevel] ?: emptyList())
            .count { it in completedPhrases }

        _progressState.postValue(ProgressState(completed, total))
    }

    sealed class GameState {
        object LOADING : GameState(); object READY : GameState()
        object SPEAKING : GameState(); object READY_TO_RECORD : GameState()
        object RECORDING : GameState(); object ANALYZING : GameState()
        object RESULT_SHOWN : GameState(); data class ERROR(val message: String) : GameState()
    }
}



