package com.lingogo.games

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar // <-- ¡NUEVO IMPORT!
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale
import androidx.activity.viewModels
import com.example.lingogo.R // <-- Asegúrate que este sea tu paquete de R

class PronunciationChallengeActivity : AppCompatActivity() {

    private val viewModel: PronunciationChallengeViewModel by viewModels()

    private lateinit var phraseTextView: TextView
    private lateinit var speakButton: Button
    private lateinit var recordButton: Button
    private lateinit var feedbackTextView: TextView

    // --- ¡NUEVO! Referencias para la barra de progreso ---
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    // --- FIN DE LO NUEVO ---

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var speechRecognizerIntent: Intent

    // Para solicitar el permiso de grabación (esto se mantiene igual)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permiso concedido. Presiona 'Grabar' de nuevo.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso de micrófono denegado.", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pronunciation_challenge)

        phraseTextView = findViewById(R.id.phraseTextView)
        speakButton = findViewById(R.id.speakButton)
        recordButton = findViewById(R.id.recordButton)
        feedbackTextView = findViewById(R.id.feedbackTextView)


        progressText = findViewById(R.id.progressText)
        progressBar = findViewById(R.id.progressBar)

        viewModel.setLanguageAndLevel("English", "Easy")


        setupSpeechRecognizer()

        // --- Observadores ---

        // Observar los cambios de estado del ViewModel
        viewModel.gameState.observe(this) { state ->
            when (state) {
                is PronunciationChallengeViewModel.GameState.LOADING -> {
                    phraseTextView.text = "Cargando..."
                    speakButton.isEnabled = false
                    recordButton.isEnabled = false
                }
                is PronunciationChallengeViewModel.GameState.READY -> {
                    phraseTextView.text = "Presiona 'Hablar' para escuchar la frase."
                    speakButton.text = "Hablar"
                    speakButton.isEnabled = true
                    recordButton.isEnabled = false // No puede grabar hasta que escuche la frase
                    feedbackTextView.text = ""
                }
                is PronunciationChallengeViewModel.GameState.SPEAKING -> {
                    phraseTextView.text = viewModel.currentPhrase.value
                    speakButton.text = "Hablando..."
                    speakButton.isEnabled = false
                    recordButton.isEnabled = false
                }
                is PronunciationChallengeViewModel.GameState.READY_TO_RECORD -> {
                    speakButton.text = "Escuchar de Nuevo"
                    speakButton.isEnabled = true
                    recordButton.isEnabled = true
                    feedbackTextView.text = "Ahora, repite la frase. Presiona 'Grabar'."
                }
                is PronunciationChallengeViewModel.GameState.RECORDING -> {
                    recordButton.text = "Grabando..."
                    recordButton.isEnabled = true
                    speakButton.isEnabled = false
                }
                is PronunciationChallengeViewModel.GameState.ANALYZING -> {
                    feedbackTextView.text = "Analizando tu pronunciación..."
                    speakButton.isEnabled = false
                    recordButton.isEnabled = false
                }
                is PronunciationChallengeViewModel.GameState.RESULT_SHOWN -> {
                    feedbackTextView.text = viewModel.resultFeedback.value
                    speakButton.text = "Siguiente Frase"
                    speakButton.isEnabled = true
                    recordButton.isEnabled = false
                }
                is PronunciationChallengeViewModel.GameState.ERROR -> {
                    // Ya no mostramos el error de idioma aquí,
                    // lo manejamos con un Toast para que no bloquee la UI
                    if (state.message.contains("Idioma no soportado")) {
                        Toast.makeText(this, "Voz no encontrada. Usando síntesis de red.", Toast.LENGTH_SHORT).show()
                        // No bloqueamos los botones
                    } else {
                        phraseTextView.text = "Error: ${state.message}"
                        speakButton.isEnabled = false
                        recordButton.isEnabled = false
                        Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    }
                }
                else -> {
                    // Buena práctica
                }
            }
        }

        // Observar la frase actual
        viewModel.currentPhrase.observe(this) { phrase ->
            phraseTextView.text = phrase
        }

        // --- ¡NUEVO! Observar el progreso ---
        viewModel.progressState.observe(this) { progress ->
            progressText.text = "${progress.completed} / ${progress.total}"
            progressBar.max = progress.total
            progressBar.progress = progress.completed
        }

        // --- ¡NUEVO! Observar eventos de Nivel Completo ---
        viewModel.levelCompleteEvent.observe(this) { message ->
            if (message != null) {
                feedbackTextView.text = message
                speakButton.text = "Siguiente Nivel"
                if (message.contains("Completaste todos")) {
                    speakButton.text = "Reiniciar Idioma"
                }
            }
        }

        // --- Listeners de botones ---
        speakButton.setOnClickListener {
            val currentState = viewModel.gameState.value

            // Si el botón dice "Escuchar de Nuevo"
            if (currentState is PronunciationChallengeViewModel.GameState.READY_TO_RECORD) {
                viewModel.repeatCurrentPhrase()
            } else {
                // Si dice "Hablar", "Siguiente Frase", "Siguiente Nivel", etc.
                viewModel.startNextChallenge()
            }
        }

        recordButton.setOnClickListener {
            checkPermissionAndStartSpeechRecognition()
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Reconocimiento de voz no disponible", Toast.LENGTH_LONG).show()
            recordButton.isEnabled = false
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                viewModel.setGameStateRecording()
                recordButton.text = "Grabando... (Habla ahora)"
            }

            override fun onBeginningOfSpeech() { /* Opcional */ }
            override fun onEndOfSpeech() {
                recordButton.text = "Analizando..."
                viewModel.setGameStateReadyToRecord()
            }

            override fun onError(error: Int) {
                val errorMessage = when(error) {
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz. Intenta de nuevo."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se entendió. Intenta de nuevo."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocedor está ocupado."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Faltan permisos de micrófono."
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red. Revisa tu conexión."
                    SpeechRecognizer.ERROR_CLIENT -> "Error del cliente de reconocimiento."
                    else -> "Error de reconocimiento. Intenta de nuevo."
                }
                Log.e("SpeechRecognizer", "Error: $errorMessage (código: $error)")
                Toast.makeText(this@PronunciationChallengeActivity, errorMessage, Toast.LENGTH_SHORT).show()
                viewModel.setGameStateReadyToRecord()
                recordButton.text = "Grabar Pronunciación"
            }

            override fun onResults(results: Bundle?) {
                val spokenText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)

                if (!spokenText.isNullOrEmpty()) {
                    viewModel.onSpeechResult(spokenText)
                } else {
                    Toast.makeText(this@PronunciationChallengeActivity, "No se reconoció ninguna voz.", Toast.LENGTH_SHORT).show()
                    viewModel.setGameStateReadyToRecord()
                }
                recordButton.text = "Grabar Pronunciación"
            }

            override fun onRmsChanged(rmsdB: Float) { /* Opcional: para visualizador de voz */ }
            override fun onBufferReceived(buffer: ByteArray?) { /* No usado */ }
            override fun onPartialResults(partialResults: Bundle?) { /* No usado */ }
            override fun onEvent(eventType: Int, params: Bundle?) { /* No usado */ }
        })
    }


    private fun checkPermissionAndStartSpeechRecognition() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {

                // ¡IMPORTANTE! Actualizar el idioma del reconocedor
                // para que coincida con el idioma del desafío (TTS)
                val currentLocale = viewModel.challengeLocale.value ?: Locale.US
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLocale.toLanguageTag())
                // --- FIN DE LA ACTUALIZACIÓN ---

                speechRecognizer?.startListening(speechRecognizerIntent)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(this, "Necesitamos el permiso de micrófono para grabar tu voz.", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
    }
}

