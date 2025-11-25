package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.lingogo.database.AppDatabase
import com.example.lingogo.database.FavoriteWord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.lang.Exception

class InitionActivity : AppCompatActivity() {

    private val TAG = "InitionActivity"

    // --- Vistas de Navegación ---
    private lateinit var headerCard: CardView
    private lateinit var cardViewComunidad: CardView
    private lateinit var cardViewConfig: CardView
    private lateinit var cardGames: CardView
    private lateinit var cardPalabrasFavoritas: CardView
    private lateinit var cardEmpezarLeccion: CardView
    private lateinit var cardVoiceIoT: CardView

    // --- Vistas del Header y Spinner ---
    private lateinit var tvBienvenidaHeader: TextView
    private lateinit var tvSubtituloHeader: TextView
    private lateinit var spinnerLanguages: Spinner
    private var isUserInteracting = false

    // --- Vistas del Dashboard (Progreso y Racha) ---
    private lateinit var txtStreakCount: TextView
    private lateinit var txtPointsCount: TextView
    private lateinit var progressBarDaily: ProgressBar

    // --- Vistas de la Tarjeta Lección ---
    private lateinit var tvLeccionTitulo: TextView
    private lateinit var tvLeccionDetalle: TextView
    private lateinit var tvLeccionProgreso: TextView

    // --- Vistas de Prueba (Room) ---
    private lateinit var etPalabraTemp: EditText
    private lateinit var btnGuardarTemp: Button

    // --- Firebase y Room ---
    private lateinit var auth: FirebaseAuth
    private lateinit var dbFirestore: FirebaseFirestore
    private lateinit var dbRoom: AppDatabase

    private val configActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d(TAG, "Regresando de Configuración. Re-dibujando...")
            recreate()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        // Inicializar Backend
        auth = FirebaseAuth.getInstance()
        dbFirestore = FirebaseFirestore.getInstance()
        dbRoom = AppDatabase.getDatabase(applicationContext)

        // --- ENLAZAR VISTAS (findViewById) ---

        // Header
        headerCard = findViewById(R.id.headerCard)
        tvBienvenidaHeader = findViewById(R.id.tvBienvenidaHeader)
        tvSubtituloHeader = findViewById(R.id.tvSubtituloHeader)
        spinnerLanguages = findViewById(R.id.spinnerLanguages)

        // Dashboard (NUEVO)
        txtStreakCount = findViewById(R.id.txtStreakCount)
        txtPointsCount = findViewById(R.id.txtPointsCount)
        progressBarDaily = findViewById(R.id.progressBarDaily)

        // Tarjeta Lección (NUEVO)
        cardEmpezarLeccion = findViewById(R.id.cardEmpezarLeccion)
        tvLeccionTitulo = findViewById(R.id.tvLeccionTitulo)
        tvLeccionDetalle = findViewById(R.id.tvLeccionDetalle)
        tvLeccionProgreso = findViewById(R.id.tvLeccionProgreso)

        // Tarjetas de Navegación
        cardViewComunidad = findViewById(R.id.cardComunidad)
        cardViewConfig = findViewById(R.id.cardConfiguracion)
        cardGames = findViewById(R.id.cardGames)
        cardPalabrasFavoritas = findViewById(R.id.cardPalabrasFavoritas)
        cardVoiceIoT = findViewById(R.id.cardVoiceIoT)

        // Prueba Room
        etPalabraTemp = findViewById(R.id.etPalabraFavoritaTemp)
        btnGuardarTemp = findViewById(R.id.btnGuardarPalabraTemp)

        // --- CONFIGURACIÓN ---
        setupListeners()
        setupLanguageSpinner()
    }

    // ---------------------------------------------------------
    // LOGICA DEL DASHBOARD (PROGRESO)
    // ---------------------------------------------------------

    private fun cargarDashboard(languageId: String) {
        val userId = auth.currentUser?.uid ?: return

        // Escuchamos cambios en tiempo real en: users/{uid}/courses/{languageId}
        dbFirestore.collection("users").document(userId)
            .collection("courses").document(languageId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Error al cargar dashboard", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // Convertimos el documento a objeto
                    val data = snapshot.toObject(DashboardData::class.java)
                    if (data != null) {
                        actualizarVistasDashboard(data)
                        actualizarTarjetaLeccion(data)
                    }
                } else {
                    // Si no existe progreso para este idioma, mostrar ceros
                    tvLeccionTitulo.text = "¡Comienza el curso!"
                    tvLeccionDetalle.text = "Lección 1: Introducción"
                    tvLeccionProgreso.text = "0% completado"

                    txtPointsCount.text = "⭐ 0"
                    txtStreakCount.text = "🔥 0"
                    progressBarDaily.progress = 0
                }
            }
    }

    private fun actualizarVistasDashboard(data: DashboardData) {
        txtStreakCount.text = "🔥 ${data.streak}"
        txtPointsCount.text = "⭐ ${data.points}"
        progressBarDaily.progress = data.dailyProgress
    }

    private fun actualizarTarjetaLeccion(data: DashboardData) {
        val lessonId = data.currentLessonId

        // 1. Validar que tengamos un ID válido
        if (lessonId.isEmpty()) {
            tvLeccionTitulo.text = "¡Curso completado!" // O mensaje por defecto
            tvLeccionDetalle.text = "No hay lección activa"
            tvLeccionProgreso.text = ""
            return
        }

        // 2. Calcular porcentaje (Esto sí funcionaba)
        val progressDetail = data.lessonsProgress[lessonId] ?: LessonProgressDetail()
        val porcentaje = if (progressDetail.totalStages > 0) {
            (progressDetail.stagesCompleted.toFloat() / progressDetail.totalStages.toFloat()) * 100
        } else {
            0f
        }
        tvLeccionProgreso.text = "Progreso: ${porcentaje.toInt()}% completado"

        // 3. Buscar el Título en Firebase (AQUÍ ESTÁ EL ERROR VISUAL)
        Log.d(TAG, "Buscando lección con ID: '$lessonId' en la colección 'lessons'") // <-- CHIVATO EN LOGCAT

        dbFirestore.collection("lessons").document(lessonId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val titulo = doc.getString("title") ?: "Sin título"
                    val descripcion = doc.getString("description") ?: ""

                    tvLeccionTitulo.text = "Continuar"
                    tvLeccionDetalle.text = "$titulo: $descripcion"
                } else {
                    // Si el documento NO existe en la colección 'lessons'
                    Log.e(TAG, "ERROR: El documento '$lessonId' no existe en 'lessons'.")
                    tvLeccionTitulo.text = "Error de datos"
                    tvLeccionDetalle.text = "No se encontró la lección '$lessonId'"
                }
            }
            .addOnFailureListener { e ->
                // Si falló la conexión o permisos
                Log.e(TAG, "ERROR al descargar lección", e)
                tvLeccionTitulo.text = "Error de conexión"
                tvLeccionDetalle.text = "Intenta nuevamente"
            }
    }

    // ---------------------------------------------------------
    // LOGICA DEL SPINNER DE IDIOMAS
    // ---------------------------------------------------------

    private fun setupLanguageSpinner() {
        val adapter = LanguageSpinnerAdapter(this, LanguageManager.availableLanguages)
        spinnerLanguages.adapter = adapter

        spinnerLanguages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (isUserInteracting) {
                    val selectedLang = LanguageManager.availableLanguages[position]
                    cambiarIdioma(selectedLang.id)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerLanguages.setOnTouchListener { v, event ->
            isUserInteracting = true
            v.performClick()
            false
        }
    }

    private fun cambiarIdioma(nuevoIdiomaId: String) {
        val userId = auth.currentUser?.uid ?: return

        // 1. Guardar preferencia en Firebase
        dbFirestore.collection("users").document(userId)
            .update("activeLanguageId", nuevoIdiomaId)
            .addOnSuccessListener {
                val nombreIdioma = LanguageManager.getLanguageById(nuevoIdiomaId)?.name
                Toast.makeText(this, "Cambiado a $nombreIdioma", Toast.LENGTH_SHORT).show()
            }

        // 2. Cargar el dashboard del nuevo idioma inmediatamente
        cargarDashboard(nuevoIdiomaId)
    }

    // ---------------------------------------------------------
    // CARGA DE DATOS INICIAL
    // ---------------------------------------------------------

    override fun onStart() {
        super.onStart()
        checkUserSession()
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            cargarDatosUsuario(currentUser.uid)
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun cargarDatosUsuario(userId: String) {
        dbFirestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombre") ?: "Estudiante"

                    // Configurar idioma guardado
                    val activeLangId = document.getString("activeLanguageId") ?: "en"
                    val langIndex = LanguageManager.availableLanguages.indexOfFirst { it.id == activeLangId }

                    if (langIndex >= 0) {
                        spinnerLanguages.setSelection(langIndex)
                    }

                    // Textos
                    tvBienvenidaHeader.text = getString(R.string.inicio_bienvenida_usuario, nombre)
                    tvSubtituloHeader.text = getString(R.string.inicio_listo_aprender)

                    // **IMPORTANTE**: Cargar el dashboard inicial con el idioma detectado
                    cargarDashboard(activeLangId)

                } else {
                    tvBienvenidaHeader.text = getString(R.string.inicio_hola_estudiante)
                }
            }
    }

    // ---------------------------------------------------------
    // LISTENERS DE NAVEGACIÓN
    // ---------------------------------------------------------

    private fun setupListeners() {
        headerCard.setOnClickListener { startActivity(Intent(this, PerfilActivity::class.java)) }
        cardViewComunidad.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    CommunityActivity::class.java
                )
            )
        }
        cardGames.setOnClickListener { startActivity(Intent(this, GamesActivity::class.java)) }
        cardPalabrasFavoritas.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    PalabrasFavoritasActivity::class.java
                )
            )
        }
        cardViewConfig.setOnClickListener {
            configActivityLauncher.launch(
                Intent(
                    this,
                    ConfigActivity::class.java
                )
            )
        }
        cardVoiceIoT.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    VoiceControlActivity::class.java
                )
            )
        }

        cardEmpezarLeccion.setOnClickListener {
            val intent = Intent(this, ListaLeccionesActivity::class.java)

            // Obtener el idioma seleccionado en el Spinner (ej: "en" o "de")
            val languageOption = spinnerLanguages.selectedItem as LanguageOption
            intent.putExtra("LANGUAGE_ID", languageOption.id)

            // Opcional: También podrías enviar el título de la lección actual si quisieras
            // intent.putExtra("LESSON_ID", currentLessonId)

            startActivity(intent)
        }

        // Prueba Room

        btnGuardarTemp.setOnClickListener {
            val textoPalabra = etPalabraTemp.text.toString().trim()
            if (textoPalabra.isNotEmpty()) {
                guardarPalabraFavorita(textoPalabra) // Volvemos a tu función original
            } else {
                Toast.makeText(this, "Escribe una palabra", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarPalabraFavorita(palabra: String) {
        lifecycleScope.launch {
            try {
                dbRoom.favoriteWordDao().addFavorite(FavoriteWord(word = palabra))
                runOnUiThread {
                    Toast.makeText(this@InitionActivity, "¡'$palabra' guardada!", Toast.LENGTH_SHORT).show()
                    etPalabraTemp.text.clear()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error Room", e)
            }
        }
    }
}

// ---------------------------------------------------------
// MODELOS DE DATOS (Ponlos aquí o en un archivo aparte)
// ---------------------------------------------------------

data class DashboardData(
    val points: Int = 0,
    val streak: Int = 0,
    val dailyProgress: Int = 0,
    val currentLessonId: String = "",
    val lessonsProgress: Map<String, LessonProgressDetail> = emptyMap()
)

data class LessonProgressDetail(
    val stagesCompleted: Int = 0,
    val totalStages: Int = 1
) {
    // Constructor vacío para Firebase
    constructor() : this(0, 1)
}