package com.example.lingogo

// Imports
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Button // Importante
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
import com.google.firebase.firestore.FirebaseFirestore // Importante
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

    // Botón de Admin (NUEVO)
    private lateinit var btnSubirLeccion: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore // Instancia para subir datos

    // DataStore
    private lateinit var settingsDataStore: SettingsDataStore

    private var isLanguageSpinnerLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        settingsDataStore = SettingsDataStore(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance() // Inicializar Firestore

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarConfig)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.configuraci_n)

        // Enlazar Vistas
        spinnerIdioma = findViewById(R.id.spinnerIdioma)
        switchNotificaciones = findViewById(R.id.switchNotificaciones)
        radioTema = findViewById(R.id.radioTema)
        radioClaro = findViewById(R.id.radioClaro)
        radioOscuro = findViewById(R.id.radioOscuro)

        // Enlazar Botón Admin (NUEVO - Asegúrate de haberlo puesto en el XML)
        //btnSubirLeccion = findViewById(R.id.btnSubirLeccion)

        // Cargar estado guardado
        loadInitialState()
        setupListeners()
    }

    private fun loadInitialState() {
        runBlocking {
            // ... (código del tema oscuro igual) ...

            // --- CÓDIGO ACTUALIZADO PARA CARGAR CUALQUIER IDIOMA ---
            val langCode = settingsDataStore.language.first()

            // Buscamos en qué posición de la lista está el idioma guardado
            val languageIndex = LanguageManager.availableLanguages.indexOfFirst { it.id == langCode }

            // Si lo encuentra (index >= 0), lo selecciona. Si no, selecciona el primero (0)
            spinnerIdioma.setSelection(if (languageIndex >= 0) languageIndex else 0)
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

        // Listener del Spinner
        spinnerIdioma.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isLanguageSpinnerLoading) {
                    return
                }

                // --- LÓGICA DINÁMICA MEJORADA ---
                // En lugar de un 'when' gigante, obtenemos el ID directamente de la lista
                val selectedLanguageCode = LanguageManager.availableLanguages[position].id

                Log.d(TAG, "Idioma seleccionado: $selectedLanguageCode")
                applyAndSaveLanguage(selectedLanguageCode)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Listener del Switch
        switchNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            // Lógica de notificaciones
        }

        // Listener del Tema
        radioTema.setOnCheckedChangeListener { group, checkedId ->
            radioTema.setOnCheckedChangeListener(null)
            val isDarkMode = (checkedId == R.id.radioOscuro)
            lifecycleScope.launch {
                saveTheme(isDarkMode)
                applyTheme(isDarkMode)
            }
        }

        // --- LISTENER DEL BOTÓN ADMIN (NUEVO) ---
        btnSubirLeccion.setOnClickListener {
            Toast.makeText(this, "Subiendo contenido masivo...", Toast.LENGTH_SHORT).show()
            subirIdiomasMasivos()
        }
    }

    // --- NUEVA LÓGICA MASIVA PARA 8 IDIOMAS ---

    private fun subirIdiomasMasivos() {
        val batch = db.batch()

        // Obtenemos la lista de lecciones para todos los idiomas
        val lessons = LessonContentProvider.getAllLessons()

        for (lessonData in lessons) {
            // 1. Referencia al documento de la lección (ej: "fr_1", "de_1")
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

            // 2. Agregar sus preguntas
            for (q in lessonData.questions) {
                val qRef = lessonRef.collection("quiz").document(q.id)
                batch.set(qRef, q) // q ya es un objeto, Firestore lo serializa
            }
        }

        // 3. Ejecutar TODO junto
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "¡8 Idiomas subidos correctamente!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // --- REEMPLAZA EL LISTENER DEL BOTÓN EN ONCREATE ---
    // En setupListeners():
    /*
    btnSubirLeccion.setOnClickListener {
        Toast.makeText(this, "Subiendo contenido masivo...", Toast.LENGTH_SHORT).show()
        subirIdiomasMasivos()
    }
    */

    // --- MÉTODOS AUXILIARES DE CONFIGURACIÓN ---

    private fun applyAndSaveLanguage(langCode: String) {
        lifecycleScope.launch {
            settingsDataStore.setLanguage(langCode)
            val appLocale = LocaleListCompat.forLanguageTags(langCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    private fun applyTheme(isDarkMode: Boolean) {
        val mode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private suspend fun saveTheme(isDarkMode: Boolean) {
        settingsDataStore.setDarkMode(isDarkMode)
    }

    private fun irALogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

// --- ESTRUCTURAS DE DATOS ---

data class LessonData(
    val id: String,
    val languageId: String,
    val title: String,
    val description: String,
    val questions: List<QuizQuestionModel>
)

// Reutilizamos tu modelo (asegúrate que coincida con el de QuizActivity)
// Si QuizQuestionModel ya existe en tu proyecto, borra esta definición para no duplicar.
/* data class QuizQuestionModel(
    val id: String = "",
    val stage: Int = 1,
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String = ""
)
*/

object LessonContentProvider {
    fun getAllLessons(): List<LessonData> {
        return listOf(
            // --- 1. INGLÉS (To Be) ---
            LessonData("en_1", "en", "Presente Simple", "Verbo To Be (Ser/Estar)", listOf(
                QuizQuestionModel("q1", 1, "I ___ a student.", listOf("is", "am", "are"), "am"),
                QuizQuestionModel("q2", 1, "She ___ happy.", listOf("are", "am", "is"), "is"),
                QuizQuestionModel("q3", 1, "They ___ friends.", listOf("is", "are", "am"), "are"),
                QuizQuestionModel("q4", 2, "I ___ not tired.", listOf("am", "is", "are"), "am"),
                QuizQuestionModel("q5", 2, "He ___ not here.", listOf("isn't", "aren't", "am not"), "isn't"),
                QuizQuestionModel("q6", 3, "___ you ready?", listOf("Is", "Are", "Am"), "Are")
            )),

            // --- 2. FRANCÉS (Être) ---
            LessonData("fr_1", "fr", "Les Bases", "El verbo Être (Ser/Estar)", listOf(
                QuizQuestionModel("q1", 1, "Je ___ content.", listOf("suis", "es", "est"), "suis"),
                QuizQuestionModel("q2", 1, "Tu ___ mon ami.", listOf("suis", "es", "est"), "es"),
                QuizQuestionModel("q3", 1, "Il ___ ici.", listOf("suis", "es", "est"), "est"),
                QuizQuestionModel("q4", 2, "Nous ___ étudiants.", listOf("sommes", "êtes", "sont"), "sommes"),
                QuizQuestionModel("q5", 2, "Vous ___ prêts?", listOf("sommes", "êtes", "sont"), "êtes"),
                QuizQuestionModel("q6", 3, "Ils ___ français.", listOf("sommes", "êtes", "sont"), "sont")
            )),

            // --- 3. ALEMÁN (Sein) ---
            LessonData("de_1", "de", "Grundlagen", "El verbo Sein (Ser)", listOf(
                QuizQuestionModel("q1", 1, "Ich ___ müde.", listOf("bin", "bist", "ist"), "bin"),
                QuizQuestionModel("q2", 1, "Du ___ mein Freund.", listOf("bin", "bist", "ist"), "bist"),
                QuizQuestionModel("q3", 1, "Er ___ hier.", listOf("bin", "bist", "ist"), "ist"),
                QuizQuestionModel("q4", 2, "Wir ___ glücklich.", listOf("sind", "seid", "ist"), "sind"),
                QuizQuestionModel("q5", 2, "Ihr ___ zu Hause.", listOf("sind", "seid", "ist"), "seid"),
                QuizQuestionModel("q6", 3, "Sie ___ nett.", listOf("sind", "seid", "ist"), "sind")
            )),

            // --- 4. ITALIANO (Essere) ---
            LessonData("it_1", "it", "Basi", "El verbo Essere (Ser)", listOf(
                QuizQuestionModel("q1", 1, "Io ___ italiano.", listOf("sono", "sei", "è"), "sono"),
                QuizQuestionModel("q2", 1, "Tu ___ alto.", listOf("sono", "sei", "è"), "sei"),
                QuizQuestionModel("q3", 1, "Lui ___ a casa.", listOf("sono", "sei", "è"), "è"),
                QuizQuestionModel("q4", 2, "Noi ___ amici.", listOf("siamo", "siete", "sono"), "siamo"),
                QuizQuestionModel("q5", 2, "Voi ___ pronti?", listOf("siamo", "siete", "sono"), "siete"),
                QuizQuestionModel("q6", 3, "Loro ___ qui.", listOf("siamo", "siete", "sono"), "sono")
            )),

            // --- 5. PORTUGUÉS (Ser/Estar) ---
            LessonData("pt_1", "pt", "Básico", "Verbo Ser y Estar", listOf(
                QuizQuestionModel("q1", 1, "Eu ___ brasileiro.", listOf("sou", "é", "são"), "sou"),
                QuizQuestionModel("q2", 1, "Ela ___ bonita.", listOf("sou", "é", "são"), "é"),
                QuizQuestionModel("q3", 1, "Nós ___ amigos.", listOf("somos", "são", "é"), "somos"),
                QuizQuestionModel("q4", 2, "Eu ___ cansado.", listOf("estou", "está", "estão"), "estou"),
                QuizQuestionModel("q5", 2, "Você ___ bem?", listOf("estou", "está", "estão"), "está"),
                QuizQuestionModel("q6", 3, "Eles ___ em casa.", listOf("estou", "está", "estão"), "estão")
            )),

            // --- 6. JAPONÉS (Desu) ---
            LessonData("ja_1", "ja", "Introducción", "El verbo auxiliar Desu", listOf(
                QuizQuestionModel("q1", 1, "Watashi wa Tanaka ___.", listOf("desu", "arimasu", "imasu"), "desu"),
                QuizQuestionModel("q2", 1, "Kore wa pen ___.", listOf("desu", "dewa", "ka"), "desu"),
                QuizQuestionModel("q3", 1, "Anata wa gakusei ___ ka?", listOf("desu", "masu", "nai"), "desu"),
                QuizQuestionModel("q4", 2, "Sakura ___ kirei desu.", listOf("wa", "ga", "wo"), "wa"),
                QuizQuestionModel("q5", 2, "Watashi ___ nihonjin desu.", listOf("wa", "no", "ni"), "wa"),
                QuizQuestionModel("q6", 3, "Genki ___?", listOf("desu ka", "desu", "ka"), "desu ka")
            )),

            // --- 7. CHINO (Shì - Ser) ---
            LessonData("zh_1", "zh", "Básico", "El verbo Shì (Ser)", listOf(
                QuizQuestionModel("q1", 1, "Wǒ ___ xuésheng (Soy estudiante).", listOf("shì", "yǒu", "zài"), "shì"),
                QuizQuestionModel("q2", 1, "Tā ___ wǒ péngyǒu (Él es mi amigo).", listOf("shì", "bù", "hǎo"), "shì"),
                QuizQuestionModel("q3", 1, "Nǐ ___ Zhōngguó rén ma? (¿Eres chino?)", listOf("shì", "ma", "hěn"), "shì"),
                QuizQuestionModel("q4", 2, "Wǒ ___ bù shì lǎoshī (No soy profesor).", listOf("shì", "bù", "hǎo"), "shì"),
                QuizQuestionModel("q5", 2, "Tāmen ___ shéi? (¿Quiénes son ellos?)", listOf("shì", "zài", "yǒu"), "shì"),
                QuizQuestionModel("q6", 3, "Nǐ hǎo ___?", listOf("ma", "shì", "bù"), "ma")
            )),

            // --- 8. RUSO (Básico) ---
            LessonData("ru_1", "ru", "Introducción", "Pronombres y Saludos", listOf(
                QuizQuestionModel("q1", 1, "___ - estudiante (Yo soy estudiante).", listOf("Ya (Я)", "Ty (Ты)", "On (Он)"), "Ya (Я)"),
                QuizQuestionModel("q2", 1, "Eto ___ dom (Esta es mi casa).", listOf("moy (мой)", "moya", "moe"), "moy (мой)"),
                QuizQuestionModel("q3", 1, "Kak ___ zovut? (¿Cómo te llamas?)", listOf("tebya", "menya", "vas"), "tebya"),
                QuizQuestionModel("q4", 2, "Spasibo (___)", listOf("Gracias", "Hola", "Adiós"), "Gracias"),
                QuizQuestionModel("q5", 2, "Privet (___)", listOf("Hola", "Gracias", "Por favor"), "Hola"),
                QuizQuestionModel("q6", 3, "Do svidaniya (___)", listOf("Adiós", "Hola", "Bien"), "Adiós")
            ))
        )
    }
}