package com.example.lingogo.games

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lingogo.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.util.Locale

class ListeningGameActivity : AppCompatActivity() {

    private val viewModel: ListeningGameViewModel by viewModels()

    private lateinit var tvHiddenPhrase: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var btnListen: MaterialButton
    private lateinit var btnRecord: ExtendedFloatingActionButton
    private lateinit var btnNext: MaterialButton

    private var speechRecognizer: SpeechRecognizer? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningInternal()
        } else {
            Toast.makeText(this, "Se necesita permiso de micrófono", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listening_game)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupRecognitionListener()

        tvHiddenPhrase = findViewById(R.id.tvHiddenPhrase)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnListen = findViewById(R.id.btnListen)
        btnRecord = findViewById(R.id.btnRecord)
        btnNext = findViewById(R.id.btnNext)

        btnListen.setOnClickListener {
            viewModel.playAudio()
        }

        btnRecord.setOnClickListener {
            // 1. Detenemos el audio si está sonando
            viewModel.stopAudio()
            // 2. Iniciamos la grabación
            checkPermissionAndListen()
        }

        btnNext.setOnClickListener {
            viewModel.nextRound()
            resetUI()
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.currentPhraseHidden.observe(this) { text ->
            tvHiddenPhrase.text = text
        }

        viewModel.feedback.observe(this) { text ->
            tvFeedback.text = text
        }

        viewModel.gameState.observe(this) { state ->
            if (state is ListeningGameViewModel.ListeningState.RESULT) {
                btnNext.visibility = View.VISIBLE

                // Restaurar estado de botones al terminar
                btnRecord.text = getString(R.string.listening_repetir)
                btnRecord.isEnabled = true
                btnRecord.icon = getDrawable(android.R.drawable.ic_btn_speak_now)
                btnListen.isEnabled = true
            }
        }
    }

    private fun checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListeningInternal()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListeningInternal() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        // UI Updates: Indicamos que estamos escuchando
        btnRecord.text = "Escuchando..."
        btnRecord.isEnabled = false

        // Deshabilitamos el botón de escuchar para evitar conflictos de audio
        btnListen.isEnabled = false

        speechRecognizer?.startListening(intent)
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                btnRecord.text = "Procesando..."
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No te entendí"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                    else -> "Error al escuchar"
                }
                Toast.makeText(this@ListeningGameActivity, message, Toast.LENGTH_SHORT).show()

                // Restaurar botones en caso de error
                btnRecord.text = getString(R.string.listening_repetir)
                btnRecord.isEnabled = true
                btnListen.isEnabled = true
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    viewModel.checkAnswer(matches[0])
                }
                // Nota: La reactivación de botones ocurre en el Observer cuando cambia el estado a RESULT
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun resetUI() {
        btnNext.visibility = View.GONE

        btnRecord.isEnabled = true
        btnRecord.text = getString(R.string.listening_repetir)

        btnListen.isEnabled = true

        tvFeedback.text = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}