package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.example.lingogo.database.SettingsDataStore
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ConfigActivity : AppCompatActivity() {

    // Vistas
    private lateinit var toolbar: Toolbar
    private lateinit var spinnerIdioma: Spinner
    private lateinit var switchNotificaciones: SwitchMaterial
    private lateinit var radioTema: RadioGroup
    private lateinit var radioClaro: RadioButton
    private lateinit var radioOscuro: RadioButton
    private var btnSubirLeccion: Button? = null

    // Firebase y DataStore
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var settingsDataStore: SettingsDataStore

    private var isLanguageSpinnerLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // ---------------------------------------------------------------
        // 1. EL TRUCO: FORZAR EL COLOR DEL "CURSO" (NO DEL IDIOMA UI)
        // ---------------------------------------------------------------
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)

        // Leemos la variable "learning_language" (que se guarda en InitionActivity)
        // Esta variable es la que decide si la app es Azul, Roja, Verde, etc.
        val cursoQueEstudio = prefs.getString("learning_language", "en") ?: "en"

        // Aplicamos el tema basado en el CURSO, no en el idioma de la interfaz
        val themeId = LanguageManager.getThemeForLanguage(cursoQueEstudio)
        setTheme(themeId)
        // ---------------------------------------------------------------

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        settingsDataStore = SettingsDataStore(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        toolbar = findViewById(R.id.toolbarConfig)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.configuraci_n)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        spinnerIdioma = findViewById(R.id.spinnerIdioma)
        switchNotificaciones = findViewById(R.id.switchNotificaciones)
        radioTema = findViewById(R.id.radioTema)
        radioClaro = findViewById(R.id.radioClaro)
        radioOscuro = findViewById(R.id.radioOscuro)
        //btnSubirLeccion = findViewById(R.id.btnSubirLeccion)

        loadInitialState()

        // Configuramos el Spinner para mostrar el idioma ACTUAL DE LOS TEXTOS
        // (No el del curso)
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val currentUiLang = if (!appLocales.isEmpty) appLocales[0]?.language else "es"
        setupLanguageSpinner(currentUiLang ?: "es")

        setupListeners()
    }

    private fun loadInitialState() {
        runBlocking {
            val isDarkMode = settingsDataStore.isDarkMode.first()
            if (isDarkMode) radioOscuro.isChecked = true else radioClaro.isChecked = true
        }
    }

    private fun setupLanguageSpinner(idiomaUiActual: String) {
        val adapter = LanguageSpinnerAdapter(this, LanguageManager.availableLanguages)
        spinnerIdioma.adapter = adapter

        // Seleccionamos en el spinner el idioma que tienes configurado para leer
        val index = LanguageManager.availableLanguages.indexOfFirst { it.id == idiomaUiActual }
        if (index >= 0) {
            spinnerIdioma.setSelection(index, false)
        }
        isLanguageSpinnerLoading = false
    }

    private fun setupListeners() {
        // Listener para CAMBIAR SOLO EL TEXTO
        spinnerIdioma.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isLanguageSpinnerLoading) return

                val selectedLangCode = LanguageManager.availableLanguages[position].id

                // Verificamos cuál es el idioma de texto actual
                val appLocales = AppCompatDelegate.getApplicationLocales()
                val currentCode = if (!appLocales.isEmpty) appLocales[0]?.language else "es"

                // Si seleccionas un idioma distinto, cambiamos los textos
                if (selectedLangCode != currentCode) {
                    cambiarSoloIdiomaTexto(selectedLangCode)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        radioTema.setOnCheckedChangeListener { _, checkedId ->
            if (!isLanguageSpinnerLoading) {
                val isDarkMode = (checkedId == R.id.radioOscuro)
                lifecycleScope.launch {
                    settingsDataStore.setDarkMode(isDarkMode)
                    val mode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
        }

        btnSubirLeccion?.setOnClickListener {
            btnSubirLeccion?.isEnabled = false
            btnSubirLeccion?.text = "Subiendo..."
            subirIdiomasMasivos()
        }
    }

    private fun cambiarSoloIdiomaTexto(langCode: String) {
        // 1. ESTO CAMBIA LOS STRINGS.XML (Textos)
        // Al ejecutarse, la actividad se reinicia sola.
        val appLocale = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)

        // Nota: NO guardamos nada en "learning_language", así que el color no se toca.
        Toast.makeText(this, "Idioma de texto actualizado", Toast.LENGTH_SHORT).show()
    }

    // --- LÓGICA DE ADMIN ---
    private fun subirIdiomasMasivos() {
        val batch = db.batch()
        val lessons = LessonContentProvider.getAllLessons()
        for (lessonData in lessons) {
            val lessonRef = db.collection("lessons").document(lessonData.id)
            val lessonMap = hashMapOf(
                "id" to lessonData.id,
                "title" to lessonData.title,
                "description" to lessonData.description,
                "languageId" to lessonData.languageId,
                "order" to 1,
                "totalStages" to 3
            )
            batch.set(lessonRef, lessonMap)
            for (q in lessonData.questions) {
                val qRef = lessonRef.collection("quiz").document(q.id)
                batch.set(qRef, q)
            }
        }
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Contenido actualizado", Toast.LENGTH_SHORT).show()
                btnSubirLeccion?.isEnabled = true
                btnSubirLeccion?.text = "Actualizar Contenido"
            }
            .addOnFailureListener {
                btnSubirLeccion?.isEnabled = true
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

// --- DATA CLASSES NECESARIAS PARA QUE NO DE ERROR ---
data class LessonData(
    val id: String,
    val languageId: String,
    val title: String,
    val description: String,
    val questions: List<QuizQuestionModel>
)

object LessonContentProvider {
    fun getAllLessons(): List<LessonData> {
        // Tu lista de lecciones (Inglés, Francés, etc.) va aquí...
        // He puesto una versión resumida para que compile, usa la tuya completa si la tienes.
        return listOf(
            LessonData("en_1", "en", "Simple Present", "Verb To Be", listOf(
                QuizQuestionModel("q1", 1, "I ___ a student.", listOf("is", "am", "are"), "am")
            ))
        )
    }
}