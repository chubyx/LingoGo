package com.example.lingogo

// Imports
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.example.lingogo.database.SettingsDataStore // Asegúrate que la ruta sea correcta
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ConfigActivity : AppCompatActivity() {

    private val TAG = "ConfigActivity"

    // Vistas
    private lateinit var toolbar: Toolbar
    private lateinit var spinnerIdioma: Spinner
    private lateinit var switchNotificaciones: SwitchMaterial
    private lateinit var radioTema: RadioGroup
    private lateinit var radioClaro: RadioButton
    private lateinit var radioOscuro: RadioButton

    // Firebase
    private lateinit var auth: FirebaseAuth

    // DataStore
    private lateinit var settingsDataStore: SettingsDataStore

    private var isLanguageSpinnerLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        settingsDataStore = SettingsDataStore(this)
        auth = FirebaseAuth.getInstance()

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarConfig)
        setSupportActionBar(toolbar)
        // Usamos la string para que se traduzca
        supportActionBar?.title = getString(R.string.configuraci_n)

        // Enlazar Vistas
        spinnerIdioma = findViewById(R.id.spinnerIdioma)
        switchNotificaciones = findViewById(R.id.switchNotificaciones)
        radioTema = findViewById(R.id.radioTema)
        radioClaro = findViewById(R.id.radioClaro)
        radioOscuro = findViewById(R.id.radioOscuro)

        // Cargar estado guardado
        loadInitialState()

        setupListeners()
    }

    private fun loadInitialState() {
        runBlocking {
            // Cargar Tema
            val isDarkMode = settingsDataStore.isDarkMode.first()
            if (isDarkMode) {
                radioOscuro.isChecked = true
            } else {
                radioClaro.isChecked = true
            }

            // --- ¡CÓDIGO ACTUALIZADO PARA 6 IDIOMAS! ---
            // Cargar Idioma
            val langCode = settingsDataStore.language.first()
            val languageIndex = when (langCode) {
                "en" -> 1 // Inglés
                "fr" -> 2 // Francés
                "de" -> 3 // Alemán
                "pt" -> 4 // Portugués
                "ja" -> 5 // Japonés
                else -> 0 // "es" (Español)
            }
            spinnerIdioma.setSelection(languageIndex)
        }

        isLanguageSpinnerLoading = false
    }


    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            irALogin()
        }
    }

    private fun setupListeners() {

        // --- ¡LISTENER DEL SPINNER ACTUALIZADO PARA 6 IDIOMAS! ---
        spinnerIdioma.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (isLanguageSpinnerLoading) {
                    return
                }

                // Convertimos la posición del spinner al código de idioma
                val selectedLanguageCode = when (position) {
                    1 -> "en" // Inglés
                    2 -> "fr" // Francés
                    3 -> "de" // Alemán
                    4 -> "pt" // Portugués
                    5 -> "ja" // Japonés
                    else -> "es" // Español
                }

                Log.d(TAG, "Idioma seleccionado: $selectedLanguageCode")
                applyAndSaveLanguage(selectedLanguageCode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        switchNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            // ... (tu lógica de notificaciones)
        }

        // Listener del Tema (sin cambios, ya funciona)
        radioTema.setOnCheckedChangeListener { group, checkedId ->

            radioTema.setOnCheckedChangeListener(null) // Evitar bucle

            val isDarkMode = (checkedId == R.id.radioOscuro)

            Log.d(TAG, "Cambiando tema a: ${if (isDarkMode) "Oscuro" else "Claro"}")

            lifecycleScope.launch {
                saveTheme(isDarkMode) // Primero guardamos
                applyTheme(isDarkMode) // Luego aplicamos
            }
        }
    }

    // Aplica y guarda el idioma
    private fun applyAndSaveLanguage(langCode: String) {
        lifecycleScope.launch {
            // 1. Guardar la preferencia
            settingsDataStore.setLanguage(langCode)
            Log.d(TAG, "Preferencia de idioma guardada: $langCode")

            // 2. Aplicar el idioma a la app
            val appLocale = LocaleListCompat.forLanguageTags(langCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    // Función para aplicar el tema
    private fun applyTheme(isDarkMode: Boolean) {
        val mode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    // Guarda la preferencia de tema
    private suspend fun saveTheme(isDarkMode: Boolean) {
        settingsDataStore.setDarkMode(isDarkMode)
        Log.d(TAG, "Preferencia de tema guardada.")
    }

    // Navega a la MainActivity (Login)
    private fun irALogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Maneja el clic en la flecha "atrás" de la Toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}