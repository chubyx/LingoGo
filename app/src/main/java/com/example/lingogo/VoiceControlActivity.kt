package com.example.lingogo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
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

    private val TAG = "VoiceControlActivity"

    private lateinit var tvStatus: TextView
    private lateinit var fabMic: FloatingActionButton
    private lateinit var dbRealtime: DatabaseReference

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentLanguageId: String = "es" // Idioma actual de la app

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
        // 1. Aplicar Tema y obtener idioma
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        currentLanguageId = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(currentLanguageId)
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_control)

        // 2. Inicializar Firebase Realtime Database
        dbRealtime = FirebaseDatabase.getInstance().getReference("iot_comandos")

        // 3. Vistas
        tvStatus = findViewById(R.id.tvStatus)
        fabMic = findViewById(R.id.fabMic)

        // 4. Configurar Reconocimiento
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
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

            // ¡IMPORTANTE! Forzamos el idioma del reconocedor al idioma de la app
            val locale = getLocaleForAppLanguage(currentLanguageId)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toString())
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, locale.toString())
        }

        tvStatus.text = getString(R.string.voz_iot_escuchando) // "Escuchando..."
        fabMic.isEnabled = false

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar escucha", e)
            fabMic.isEnabled = true
        }
    }

    // Función auxiliar para obtener el Locale correcto según tu LanguageManager
    private fun getLocaleForAppLanguage(langId: String): Locale {
        return when (langId) {
            "en" -> Locale.ENGLISH
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "it" -> Locale.ITALIAN
            "pt" -> Locale("pt", "BR") // Portugués Brasil
            "ja" -> Locale.JAPANESE
            "zh" -> Locale.CHINESE
            "ru" -> Locale("ru", "RU")
            else -> Locale("es", "ES") // Default Español
        }
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
                    SpeechRecognizer.ERROR_NO_MATCH -> getString(R.string.voz_iot_error) // "No te entendí"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Faltan permisos"
                    else -> "Error ($error)"
                }
                tvStatus.text = mensaje
                fabMic.isEnabled = true
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0].lowercase()
                    processCommand(command)
                } else {
                    fabMic.isEnabled = true
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // --- LÓGICA MULTI-IDIOMA DE COMANDOS ---

    private fun processCommand(command: String) {
        val cmd = command.lowercase()
        tvStatus.text = getString(R.string.voz_iot_enviado, cmd) // "Enviado: [texto]"

        // 1. Definimos listas de palabras clave para identificar OBJETOS y ACCIONES

        // Objetos: LUZ
        val wordsLight = listOf(
            "luz", "foco", "bombilla", // Español
            "light", "lamp", // Inglés
            "lumière", "lampe", // Francés
            "licht", "lampe", // Alemán
            "luce", "lampada", // Italiano
            "luz", "lâmpada", // Portugués
            "denki", "hikari", "電気", // Japonés (Fonético y Kanji)
            "deng", "guang", "灯", // Chino
            "svet", "свет", "lampu" // Ruso
        )

        // Objetos: PUERTA
        val wordsDoor = listOf(
            "puerta", // Español
            "door", // Inglés
            "porte", // Francés
            "tür", // Alemán
            "porta", // Italiano y Portugués
            "doa", "tobira", "ドア", // Japonés
            "men", "门", // Chino
            "dver", "дверь" // Ruso
        )

        // Acción: ENCENDER / ACTIVAR (ON)
        val wordsOn = listOf(
            "encender", "prender", "activar", "on", // Español / Inglés
            "turn on", "switch on",
            "allumer", // Francés
            "an", "einschalten", // Alemán (licht an)
            "accendere", // Italiano
            "ligar", // Portugués
            "tsukete", "on", "つけて", // Japonés
            "kai", "da kai", "开", // Chino
            "vklyuchit", "включить" // Ruso
        )

        // Acción: APAGAR / DESACTIVAR (OFF)
        val wordsOff = listOf(
            "apagar", "desactivar", "off", // Español / Inglés
            "turn off", "switch off",
            "éteindre", // Francés
            "aus", "ausschalten", // Alemán
            "spegnere", // Italiano
            "desligar", // Portugués
            "keshite", "off", "消して", // Japonés
            "guan", "关", // Chino
            "vyklyuchit", "выключить" // Ruso
        )

        // Acción: ABRIR (OPEN)
        val wordsOpen = listOf(
            "abrir", "abre", // Español / Port / Ita
            "open", // Inglés
            "ouvrir", // Francés
            "öffnen", "auf", // Alemán
            "aprire", // Italiano
            "akete", "hirake", "開けて", // Japonés
            "kai", "da kai", "开", "打开", // Chino
            "otkryt", "otkroy", "открыть" // Ruso
        )

        // Acción: CERRAR (CLOSE)
        val wordsClose = listOf(
            "cerrar", "cierra", // Español
            "close", "shut", // Inglés
            "fermer", // Francés
            "schließen", "zu", // Alemán
            "chiudere", // Italiano
            "fechar", // Portugués
            "shimete", "tojite", "閉めて", // Japonés
            "guan", "关", // Chino
            "zakryt", "zakroy", "закрыть" // Ruso
        )

        // 2. Lógica de coincidencia
        // Verificamos si el comando contiene (OBJETO + ACCIÓN)

        var actionFound = false

        // --- CASO: LUZ ---
        if (containsAny(cmd, wordsLight)) {
            if (containsAny(cmd, wordsOn)) {
                enviarA_Firebase("ON")
                actionFound = true
            } else if (containsAny(cmd, wordsOff)) {
                enviarA_Firebase("OFF")
                actionFound = true
            }
        }

        // --- CASO: PUERTA ---
        if (!actionFound && containsAny(cmd, wordsDoor)) {
            if (containsAny(cmd, wordsOpen)) {
                enviarA_Firebase("OPEN")
                actionFound = true
            } else if (containsAny(cmd, wordsClose)) {
                enviarA_Firebase("CLOSE")
                actionFound = true
            }
        }

        // --- FEEDBACK ---
        if (!actionFound) {
            Toast.makeText(this, "Comando no reconocido en este idioma", Toast.LENGTH_SHORT).show()
        }

        // Reactivar botón
        fabMic.isEnabled = true
    }

    // Helper para buscar si alguna palabra de la lista está en el comando
    private fun containsAny(command: String, keywords: List<String>): Boolean {
        for (word in keywords) {
            if (command.contains(word)) return true
        }
        return false
    }

    private fun enviarA_Firebase(accion: String) {
        // Estructura: iot_comandos -> { "accion": "ON" }
        dbRealtime.child("accion").setValue(accion)
            .addOnSuccessListener {
                Log.d(TAG, "Comando enviado: $accion")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar comando", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}