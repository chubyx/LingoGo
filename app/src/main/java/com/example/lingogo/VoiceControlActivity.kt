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

    // El objeto para escuchar sin pop-up
    private var speechRecognizer: SpeechRecognizer? = null

    // Launcher para pedir permiso de micrófono (Obligatorio para este método)
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

        // Inicializamos el reconocedor
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupRecognitionListener()

        fabMic.setOnClickListener {
            checkPermissionAndListen()
        }
    }

    private fun checkPermissionAndListen() {
        // Verificamos si tenemos permiso de grabación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListeningInternal()
        } else {
            // Si no, lo pedimos
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListeningInternal() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        // Feedback visual: Cambiamos el texto para que el usuario sepa que debe hablar
        tvStatus.text = "Escuchando... (Habla ahora)"
        fabMic.isEnabled = false // Desactivar botón para evitar doble clic

        speechRecognizer?.startListening(intent)
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // El micrófono está abierto
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {} // Aquí podrías animar el botón con el volumen de voz
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
                fabMic.isEnabled = true // Reactivar botón
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0].lowercase()
                    processCommand(command)
                }
                fabMic.isEnabled = true // Reactivar botón
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun processCommand(command: String) {
        tvStatus.text = getString(R.string.voz_iot_enviado, command)

        when {
            command.contains("encender") && command.contains("luz") -> enviarA_Firebase("LUZ", "ON")
            command.contains("apagar") && command.contains("luz") -> enviarA_Firebase("LUZ", "OFF")
            command.contains("abrir") && command.contains("puerta") -> enviarA_Firebase("PUERTA", "OPEN")
            command.contains("cerrar") && command.contains("puerta") -> enviarA_Firebase("PUERTA", "CLOSE")
            // Inglés
            command.contains("turn on") || command.contains("light on") -> enviarA_Firebase("LUZ", "ON")
            command.contains("turn off") || command.contains("light off") -> enviarA_Firebase("LUZ", "OFF")
            else -> {
                enviarA_Firebase("DESCONOCIDO", command)
                Toast.makeText(this, "Comando no reconocido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarA_Firebase(dispositivo: String, accion: String) {
        val datos = mapOf(
            "dispositivo" to dispositivo,
            "accion" to accion,
            "timestamp" to System.currentTimeMillis()
        )

        dbRealtime.setValue(datos)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Enviado!", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}