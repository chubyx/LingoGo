package com.example.lingogo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
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

    // --- NUEVO: LANZADOR PARA PEDIR PERMISO DE NOTIFICACIONES ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
            switchNotificaciones.isChecked = true
        } else {
            Toast.makeText(this, "Permiso denegado. No podrás recibir avisos.", Toast.LENGTH_LONG).show()
            switchNotificaciones.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ---------------------------------------------------------------
        // 1. FORZAR EL COLOR DEL "CURSO"
        // ---------------------------------------------------------------
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val cursoQueEstudio = prefs.getString("learning_language", "en") ?: "en"
        val themeId = LanguageManager.getThemeForLanguage(cursoQueEstudio)
        setTheme(themeId)
        // ---------------------------------------------------------------

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        settingsDataStore = SettingsDataStore(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Enlazar Vistas
        toolbar = findViewById(R.id.toolbarConfig)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.configuraci_n)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        spinnerIdioma = findViewById(R.id.spinnerIdioma)
        switchNotificaciones = findViewById(R.id.switchNotificaciones)
        radioTema = findViewById(R.id.radioTema)
        radioClaro = findViewById(R.id.radioClaro)
        radioOscuro = findViewById(R.id.radioOscuro)
        // btnSubirLeccion = findViewById(R.id.btnSubirLeccion) // Descomenta si tienes este botón en el XML

        // Cargar estado inicial (Tema y Permiso de Notificaciones)
        loadInitialState()

        // Configurar Spinner
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val currentUiLang = if (!appLocales.isEmpty) appLocales[0]?.language else "es"
        setupLanguageSpinner(currentUiLang ?: "es")

        // Configurar Listeners (Clics)
        setupListeners()
    }

    private fun loadInitialState() {
        // 1. Cargar Modo Oscuro/Claro
        runBlocking {
            val isDarkMode = settingsDataStore.isDarkMode.first()
            if (isDarkMode) radioOscuro.isChecked = true else radioClaro.isChecked = true
        }

        // 2. NUEVO: Verificar si ya tenemos permiso de notificaciones para marcar el switch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val tienePermiso = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            // Ponemos el switch según la realidad del sistema
            switchNotificaciones.isChecked = tienePermiso
        } else {
            // En Android 12 o inferior, el permiso es automático
            switchNotificaciones.isChecked = true
        }
    }

    private fun setupLanguageSpinner(idiomaUiActual: String) {
        val adapter = LanguageSpinnerAdapter(this, LanguageManager.availableLanguages)
        spinnerIdioma.adapter = adapter

        val index = LanguageManager.availableLanguages.indexOfFirst { it.id == idiomaUiActual }
        if (index >= 0) {
            spinnerIdioma.setSelection(index, false)
        }
        isLanguageSpinnerLoading = false
    }

    private fun setupListeners() {
        // Listener Cambio de Idioma (Texto)
        spinnerIdioma.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isLanguageSpinnerLoading) return

                val selectedLangCode = LanguageManager.availableLanguages[position].id
                val appLocales = AppCompatDelegate.getApplicationLocales()
                val currentCode = if (!appLocales.isEmpty) appLocales[0]?.language else "es"

                if (selectedLangCode != currentCode) {
                    cambiarSoloIdiomaTexto(selectedLangCode)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Listener Cambio de Tema (Oscuro/Claro)
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

        // --- NUEVO: Listener Switch Notificaciones ---
        switchNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Si el usuario ACTIVA el switch
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Verificamos si realmente tiene permiso
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        // Si no tiene, lanzamos la ventanita del sistema para pedirlo
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Toast.makeText(this, "Notificaciones activas", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Si el usuario DESACTIVA el switch
                // Nota: No podemos quitar el permiso del sistema por código, pero avisamos visualmente
                Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener Botón Admin (Si existe)
        btnSubirLeccion?.setOnClickListener {
            btnSubirLeccion?.isEnabled = false
            btnSubirLeccion?.text = "Subiendo..."
            subirIdiomasMasivos()
        }
    }

    private fun cambiarSoloIdiomaTexto(langCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        Toast.makeText(this, "Idioma de texto actualizado", Toast.LENGTH_SHORT).show()
    }

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

// --- CLASES DE DATOS (Incluidas para evitar errores de referencia) ---

data class LessonData(
    val id: String,
    val languageId: String,
    val title: String,
    val description: String,
    val questions: List<QuizQuestionModel>
)





object LessonContentProvider {
    fun getAllLessons(): List<LessonData> {
        // Ejemplo simple para que compile
        return listOf(
            LessonData("en_1", "en", "Simple Present", "Verb To Be", emptyList())
        )
    }
}