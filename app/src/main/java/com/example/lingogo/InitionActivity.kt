package com.example.lingogo

import android.content.Context
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
import com.google.firebase.messaging.FirebaseMessaging // <--- NUEVO IMPORT
import kotlinx.coroutines.launch
import java.lang.Exception

class InitionActivity : AppCompatActivity() {

    private val TAG = "InitionActivity"

    // --- Vistas ---
    private lateinit var headerCard: CardView
    private lateinit var cardViewComunidad: CardView
    private lateinit var cardViewConfig: CardView
    private lateinit var cardGames: CardView
    private lateinit var cardPalabrasFavoritas: CardView
    private lateinit var cardEmpezarLeccion: CardView
    private lateinit var cardVoiceIoT: CardView

    private lateinit var tvBienvenidaHeader: TextView
    private lateinit var tvSubtituloHeader: TextView
    private lateinit var spinnerLanguages: Spinner

    // Bandera para evitar bucles infinitos al iniciar el spinner
    private var isUserInteracting = false

    private lateinit var txtStreakCount: TextView
    private lateinit var txtPointsCount: TextView
    private lateinit var progressBarDaily: ProgressBar

    private lateinit var tvLeccionTitulo: TextView
    private lateinit var tvLeccionDetalle: TextView
    private lateinit var tvLeccionProgreso: TextView

    // Prueba Room
    private lateinit var etPalabraTemp: EditText
    private lateinit var btnGuardarTemp: Button

    // --- Backend ---
    private lateinit var auth: FirebaseAuth
    private lateinit var dbFirestore: FirebaseFirestore
    private lateinit var dbRoom: AppDatabase

    // Launcher para volver de Configuración (por si cambian algo allí)
    private val configActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Al volver, recreamos por si cambiaron el modo oscuro o idioma
            recreate()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. APLICAR TEMA ANTES DE LA UI (CRUCIAL PARA EL CAMBIO DE COLOR)
        val prefs = getSharedPreferences("Ajustes", Context.MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"

        // Obtenemos el ID del tema (R.style.Theme_LingoGo_English, etc.) desde tu LanguageManager
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        // 2. Inicializar Backend
        auth = FirebaseAuth.getInstance()
        dbFirestore = FirebaseFirestore.getInstance()
        dbRoom = AppDatabase.getDatabase(applicationContext)

        // 3. Enlazar Vistas
        initViews()

        // 4. Configurar Componentes
        setupListeners()
        setupLanguageSpinner(idiomaGuardado)

        // 5. Cargar datos iniciales
        checkUserSession()

        // 6. Actualizar Token para Notificaciones (NUEVO)
        actualizarTokenFCM()
    }

    private fun initViews() {
        // Header
        headerCard = findViewById(R.id.headerCard)
        tvBienvenidaHeader = findViewById(R.id.tvBienvenidaHeader)
        tvSubtituloHeader = findViewById(R.id.tvSubtituloHeader)
        spinnerLanguages = findViewById(R.id.spinnerLanguages)

        // Dashboard
        txtStreakCount = findViewById(R.id.txtStreakCount)
        txtPointsCount = findViewById(R.id.txtPointsCount)
        progressBarDaily = findViewById(R.id.progressBarDaily)

        // Lección
        cardEmpezarLeccion = findViewById(R.id.cardEmpezarLeccion)
        tvLeccionTitulo = findViewById(R.id.tvLeccionTitulo)
        tvLeccionDetalle = findViewById(R.id.tvLeccionDetalle)
        tvLeccionProgreso = findViewById(R.id.tvLeccionProgreso)

        // Navegación
        cardViewComunidad = findViewById(R.id.cardComunidad)
        cardViewConfig = findViewById(R.id.cardConfiguracion)
        cardGames = findViewById(R.id.cardGames)
        cardPalabrasFavoritas = findViewById(R.id.cardPalabrasFavoritas)
        cardVoiceIoT = findViewById(R.id.cardVoiceIoT)

        // Room Test
        etPalabraTemp = findViewById(R.id.etPalabraFavoritaTemp)
        btnGuardarTemp = findViewById(R.id.btnGuardarPalabraTemp)
    }

    // ---------------------------------------------------------
    // LOGICA DEL SPINNER DE IDIOMAS Y CAMBIO DE COLOR
    // ---------------------------------------------------------

    private fun setupLanguageSpinner(idiomaActualId: String) {
        val adapter = LanguageSpinnerAdapter(this, LanguageManager.availableLanguages)
        spinnerLanguages.adapter = adapter

        // Pre-seleccionar el idioma guardado en SharedPreferences para que coincida con el color
        val index = LanguageManager.availableLanguages.indexOfFirst { it.id == idiomaActualId }
        if (index >= 0) {
            spinnerLanguages.setSelection(index, false) // false evita disparar el listener al iniciar
        }

        spinnerLanguages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Solo cambiamos si el usuario tocó el spinner (evita bucles al iniciar)
                if (isUserInteracting) {
                    val selectedLang = LanguageManager.availableLanguages[position]

                    // Verificamos si realmente cambió el idioma para no recargar innecesariamente
                    val prefs = getSharedPreferences("Ajustes", Context.MODE_PRIVATE)
                    val currentStored = prefs.getString("idioma_seleccionado", "es")

                    if (selectedLang.id != currentStored) {
                        cambiarIdioma(selectedLang.id)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Detectar interacción real del usuario
        spinnerLanguages.setOnTouchListener { v, event ->
            isUserInteracting = true
            v.performClick()
            false
        }
    }

    private fun cambiarIdioma(nuevoIdiomaId: String) {
        val userId = auth.currentUser?.uid ?: return

        // 1. Guardar en SharedPreferences (Para el color del Tema)
        val prefs = getSharedPreferences("Ajustes", Context.MODE_PRIVATE)
        prefs.edit().putString("idioma_seleccionado", nuevoIdiomaId).apply()

        // 2. Guardar en Firebase (Para sincronizar datos)
        dbFirestore.collection("users").document(userId)
            .update("activeLanguageId", nuevoIdiomaId)
            .addOnSuccessListener {
                // 3. RECREAR LA ACTIVIDAD PARA APLICAR EL NUEVO COLOR
                recreate()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar idioma", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------------------------------------------------
    // DASHBOARD Y FIREBASE
    // ---------------------------------------------------------

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
        // Obtenemos el idioma actual de SharedPreferences para cargar el dashboard inmediato
        val prefs = getSharedPreferences("Ajustes", Context.MODE_PRIVATE)
        val idiomaActual = prefs.getString("idioma_seleccionado", "es") ?: "es"

        // Cargamos dashboard inmediatamente con lo que tenemos localmente
        cargarDashboard(idiomaActual)

        // Obtenemos datos del usuario (Nombre, etc.)
        dbFirestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombre") ?: "Estudiante"
                    tvBienvenidaHeader.text = getString(R.string.inicio_bienvenida_usuario, nombre)

                    // Nota: Ya no seteamos el spinner aquí para evitar conflictos con el Theme.
                    // El spinner se controla via SharedPreferences en setupLanguageSpinner.
                }
            }
    }

    private fun cargarDashboard(languageId: String) {
        val userId = auth.currentUser?.uid ?: return

        dbFirestore.collection("users").document(userId)
            .collection("courses").document(languageId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Error al cargar dashboard", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.toObject(DashboardData::class.java)
                    if (data != null) {
                        actualizarVistasDashboard(data)
                        actualizarTarjetaLeccion(data)
                    }
                } else {
                    // Estado vacío (curso nuevo)
                    tvLeccionTitulo.text = getString(R.string.inicio_continuar_leccion)
                    tvLeccionDetalle.text = "Lección 1"
                    tvLeccionProgreso.text = "0%"
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

        if (lessonId.isEmpty()) {
            tvLeccionTitulo.text = "Curso finalizado"
            tvLeccionDetalle.text = "¡Felicidades!"
            tvLeccionProgreso.text = "100%"
            return
        }

        val progressDetail = data.lessonsProgress[lessonId] ?: LessonProgressDetail()
        val porcentaje = if (progressDetail.totalStages > 0) {
            (progressDetail.stagesCompleted.toFloat() / progressDetail.totalStages.toFloat()) * 100
        } else { 0f }

        tvLeccionProgreso.text = "Progreso: ${porcentaje.toInt()}%"

        dbFirestore.collection("lessons").document(lessonId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val titulo = doc.getString("title") ?: "Lección"
                    tvLeccionTitulo.text = getString(R.string.inicio_continuar_leccion)
                    tvLeccionDetalle.text = titulo
                }
            }
    }

    // ---------------------------------------------------------
    // LISTENERS DE NAVEGACIÓN
    // ---------------------------------------------------------

    private fun setupListeners() {
        headerCard.setOnClickListener { startActivity(Intent(this, PerfilActivity::class.java)) }
        cardViewComunidad.setOnClickListener { startActivity(Intent(this, CommunityActivity::class.java)) }
        cardGames.setOnClickListener { startActivity(Intent(this, GamesActivity::class.java)) }
        cardPalabrasFavoritas.setOnClickListener { startActivity(Intent(this, PalabrasFavoritasActivity::class.java)) }

        cardViewConfig.setOnClickListener {
            configActivityLauncher.launch(Intent(this, ConfigActivity::class.java))
        }

        cardVoiceIoT.setOnClickListener { startActivity(Intent(this, VoiceControlActivity::class.java)) }

        cardEmpezarLeccion.setOnClickListener {
            val intent = Intent(this, ListaLeccionesActivity::class.java)
            // Pasamos el ID del idioma seleccionado actualmente
            val languageOption = spinnerLanguages.selectedItem as LanguageOption
            intent.putExtra("LANGUAGE_ID", languageOption.id)
            startActivity(intent)
        }

        // Room
        btnGuardarTemp.setOnClickListener {
            val texto = etPalabraTemp.text.toString().trim()
            if (texto.isNotEmpty()) guardarPalabraFavorita(texto)
        }
    }

    private fun guardarPalabraFavorita(palabra: String) {
        lifecycleScope.launch {
            try {
                dbRoom.favoriteWordDao().addFavorite(FavoriteWord(word = palabra))
                runOnUiThread {
                    Toast.makeText(this@InitionActivity, "Guardado", Toast.LENGTH_SHORT).show()
                    etPalabraTemp.text.clear()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error Room", e)
            }
        }
    }

    // ---------------------------------------------------------
    // NOTIFICACIONES PUSH (FCM) - NUEVA FUNCIONALIDAD
    // ---------------------------------------------------------
    private fun actualizarTokenFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Error obteniendo el token FCM", task.exception)
                return@addOnCompleteListener
            }

            // 1. Obtener el nuevo token
            val token = task.result

            // 2. Guardarlo en el usuario actual
            val userId = auth.currentUser?.uid
            if (userId != null) {
                dbFirestore.collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al guardar el token en Firestore", e)
                    }
            }
        }
    }
}

// Modelos
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
    constructor() : this(0, 1)
}