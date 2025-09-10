package com.example.lingogo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class VoiceControlActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var fabMic: FloatingActionButton
    private lateinit var dbRealtime: DatabaseReference

    private var speechRecognizer: SpeechRecognizer? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningInternal()
        } else {
            Toast.makeText(this, "Se necesita permiso para escuchar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_control)

        dbRealtime = FirebaseDatabase.getInstance().getReference("iot_comandos")

        tvStatus = findViewById(R.id.tvStatus)
        fabMic = findViewById(R.id.fabMic)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupRecognitionListener()

        fabMic.setOnClickListener {
            checkPermissionAndListen()
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        tvStatus.text = "Escuchando..."
        fabMic.isEnabled = false

        speechRecognizer?.startListening(intent)
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                tvStatus.text = "Procesando..."
            }

            override fun onError(error: Int) {
                val mensaje = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No te entendí"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de conexión"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Faltan permisos"
                    else -> "Error al escuchar"
                }
                tvStatus.text = mensaje
                fabMic.isEnabled = true
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0].lowercase()
                    processCommand(command)
                }
                fabMic.isEnabled = true
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun processCommand(command: String) {
        tvStatus.text = getString(R.string.voz_iot_enviado, command)

        when {
            (command.contains("encender") && command.contains("luz")) ||
                    (command.contains("turn on") && command.contains("light")) -> {
                enviarA_Firebase("ON")
            }

            (command.contains("apagar") && command.contains("luz")) ||
                    (command.contains("turn off") && command.contains("light")) -> {
                enviarA_Firebase("OFF")
            }

            (command.contains("abrir") && command.contains("puerta")) ||
                    (command.contains("open") && command.contains("door")) -> {
                enviarA_Firebase("OPEN")
            }

            (command.contains("cerrar") && command.contains("puerta")) ||
                    (command.contains("close") && command.contains("door")) -> {
                enviarA_Firebase("CLOSE")
            }

            else -> {
                Toast.makeText(this, "Comando no reconocido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarA_Firebase(accion: String) {
        dbRealtime.child("accion").setValue(accion)
            .addOnSuccessListener {
                Toast.makeText(this, "Enviado: $accion", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}