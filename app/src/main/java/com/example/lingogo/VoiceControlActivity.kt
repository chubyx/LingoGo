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
    private var currentLanguageId: String = "es" 

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
        // 1. Aplicar Tema y obtener idioma guardado
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        currentLanguageId = prefs.getString("idioma_seleccionado", "es") ?: "es"
        
        val themeId = LanguageManager.getThemeForLanguage(currentLanguageId)
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_control)

        // 2. Inicializar Firebase
        dbRealtime = FirebaseDatabase.getInstance().getReference("iot_comandos")

        // 3. Vistas
        tvStatus = findViewById(R.id.tvStatus)
        fabMic = findViewById(R.id.fabMic)

        // 4. Configurar el Recognizer
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

            // --- CORRECCIÓN CLAVE ---
            // 1. Obtenemos el Locale completo (idioma + país)
            val locale = getLocaleForAppLanguage(currentLanguageId)
            
            // 2. Usamos toLanguageTag() para formato estándar (ej: "ja-JP" en vez de "ja")
            // Esto asegura que Google reconozca el idioma específico y no use el default.
            val languageTag = locale.toLanguageTag() 

            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageTag)
        }

        tvStatus.text = getString(R.string.voz_iot_escuchando) 
        fabMic.isEnabled = false

        try {
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Escuchando en idioma: ${intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar escucha", e)
            fabMic.isEnabled = true
        }
    }

    // --- CORRECCIÓN DE IDIOMAS ---
    // Agregamos el país específico para evitar ambigüedades (especialmente en JA, ZH, EN)
    private fun getLocaleForAppLanguage(langId: String): Locale {
        return when (langId) {
            "en" -> Locale("en", "US") // Inglés USA
            "fr" -> Locale("fr", "FR") // Francia
            "de" -> Locale("de", "DE") // Alemania
            "it" -> Locale("it", "IT") // Italia
            "pt" -> Locale("pt", "BR") // Brasil
            "ja" -> Locale("ja", "JP") // Japón (Crítico para que funcione el Japonés)
            "zh" -> Locale("zh", "CN") // China
            "ru" -> Locale("ru", "RU") // Rusia
            else -> Locale("es", "ES") // España/Latam por defecto
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
                // Manejo básico de errores para no trabar la UI
                val mensaje = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> getString(R.string.voz_iot_error) 
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
                    // Convertimos a minúsculas para comparar fácil
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

    // --- LÓGICA DE COMANDOS ---
    private fun processCommand(command: String) {
        val cmd = command.lowercase()
        tvStatus.text = getString(R.string.voz_iot_enviado, cmd) 

        // Listas de palabras clave
        // Nota: En japonés el reconocedor devuelve Kanji/Hiragana, no Romaji.

        // Objeto: LUZ
        val wordsLight = listOf(
            "luz", "foco", "bombilla", 
            "light", "lamp", 
            "lumière", "lampe", 
            "licht", "lampe", 
            "luce", "lampada", 
            "luz", "lâmpada", 
            "denki", "hikari", "電気", // Incluye Kanji
            "deng", "guang", "灯", 
            "svet", "свет", "lampu" 
        )

        // Objeto: PUERTA
        val wordsDoor = listOf(
            "puerta", "door", 
            "porte", "tür", 
            "porta", 
            "doa", "tobira", "ドア", 
            "men", "门", 
            "dver", "дверь" 
        )

        // Acción: ON
        val wordsOn = listOf(
            "encender", "prender", "activar", "on", 
            "turn on", "switch on",
            "allumer", "an", "einschalten", 
            "accendere", "ligar", 
            "tsukete", "on", "つけて", 
            "kai", "da kai", "开", 
            "vklyuchit", "включить" 
        )

        // Acción: OFF
        val wordsOff = listOf(
            "apagar", "desactivar", "off", 
            "turn off", "switch off",
            "éteindre", "aus", "ausschalten", 
            "spegnere", "desligar", 
            "keshite", "off", "消して", 
            "guan", "关", 
            "vyklyuchit", "выключить" 
        )

        // Acción: ABRIR
        val wordsOpen = listOf(
            "abrir", "abre", "open", 
            "ouvrir", "öffnen", "auf", 
            "aprire", 
            "akete", "hirake", "開けて", 
            "kai", "da kai", "开", "打开", 
            "otkryt", "otkroy", "открыть" 
        )

        // Acción: CERRAR
        val wordsClose = listOf(
            "cerrar", "cierra", "close", "shut", 
            "fermer", "schließen", "zu", 
            "chiudere", "fechar", 
            "shimete", "tojite", "閉めて", 
            "guan", "关", 
            "zakryt", "zakroy", "закрыть" 
        )

        var actionFound = false

        // Lógica: Si contiene (OBJETO) y (ACCIÓN) -> Enviar Firebase

        // 1. Caso Luz
        if (containsAny(cmd, wordsLight)) {
            if (containsAny(cmd, wordsOn)) {
                enviarA_Firebase("ON")
                actionFound = true
            } else if (containsAny(cmd, wordsOff)) {
                enviarA_Firebase("OFF")
                actionFound = true
            }
        }

        // 2. Caso Puerta
        if (!actionFound && containsAny(cmd, wordsDoor)) {
            if (containsAny(cmd, wordsOpen)) {
                enviarA_Firebase("OPEN")
                actionFound = true
            } else if (containsAny(cmd, wordsClose)) {
                enviarA_Firebase("CLOSE")
                actionFound = true
            }
        }

        if (!actionFound) {
            Toast.makeText(this, "Comando no reconocido", Toast.LENGTH_SHORT).show()
        }

        fabMic.isEnabled = true
    }

    private fun containsAny(command: String, keywords: List<String>): Boolean {
        for (word in keywords) {
            if (command.contains(word)) return true
        }
        return false
    }

    private fun enviarA_Firebase(accion: String) {
        dbRealtime.child("accion").setValue(accion)
            .addOnSuccessListener {
                Log.d(TAG, "Comando enviado: $accion")
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
