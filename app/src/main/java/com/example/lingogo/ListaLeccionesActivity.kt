package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linggo.models.Lesson
import com.example.linggo.models.LessonProgress
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ListaLeccionesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeccionAdapter

    private var currentLanguageId: String = "en" // Por defecto inglés
    private var allLessons = listOf<Lesson>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_lecciones)

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Vincular Vistas
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerViewLecciones)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()

        // 1. RECIBIR EL IDIOMA DESDE INITION ACTIVITY
        // Si no viene en el intent (ej: abriste la app directo aquí), usa "en" por defecto
        currentLanguageId = intent.getStringExtra("LANGUAGE_ID") ?: "en"

        // Configurar Título (ej: "Lecciones - Inglés")
        val langName = LanguageManager.getLanguageById(currentLanguageId)?.name ?: currentLanguageId.uppercase()
        toolbar.title = "Lecciones - $langName"

        loadData()
    }

    private fun setupRecyclerView() {
        adapter = LeccionAdapter { lesson, progress ->
            onLessonClicked(lesson, progress)
        }
        recyclerView.adapter = adapter
    }

    private fun loadData() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Error: No logueado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- PASO A: CARGAR EL PROGRESO DEL USUARIO PARA ESTE IDIOMA ---
        db.collection("users").document(userId)
            .collection("courses").document(currentLanguageId)
            .get()
            .addOnSuccessListener { document ->

                // Convertimos el documento al objeto DashboardData que creamos antes
                // (Si el usuario es nuevo en este idioma, el documento puede no existir, usamos datos vacíos)
                val dashboardData = document.toObject(DashboardData::class.java) ?: DashboardData()
                val rawProgressMap = dashboardData.lessonsProgress // Mapa crudo de Firebase

                // --- PASO B: CARGAR LAS LECCIONES ESTÁTICAS DE ESTE IDIOMA ---
                db.collection("lessons")
                    .whereEqualTo("languageId", currentLanguageId)
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener { snapshot ->

                        // Lista de objetos Lesson
                        allLessons = snapshot.documents.mapNotNull { d ->
                            d.toObject(Lesson::class.java)?.copy(id = d.id)
                        }

                        if (allLessons.isEmpty()) {
                            Toast.makeText(this, "Próximamente más lecciones de $currentLanguageId", Toast.LENGTH_LONG).show()
                        }

                        // --- PASO C: COMBINAR Y CALCULAR ESTADOS (Locked/Unlocked) ---
                        val finalProgressMap = calcularEstados(allLessons, rawProgressMap)

                        // Actualizar UI
                        progressBar.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter.submitList(allLessons, finalProgressMap)
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error cargando lecciones", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                // Si falla cargar el progreso, cargamos las lecciones igual (aparecerán bloqueadas)
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Esta función toma los datos crudos de Firebase y decide qué lecciones
     * están bloqueadas y cuáles desbloqueadas basándose en el orden.
     */
    private fun calcularEstados(
        lessons: List<Lesson>,
        rawMap: Map<String, LessonProgressDetail>
    ): Map<String, LessonProgress> {

        val resultMap = mutableMapOf<String, LessonProgress>()
        var isPreviousCompleted = true // La primera lección siempre empieza desbloqueada

        for (lesson in lessons) {
            val detail = rawMap[lesson.id]
            val stages = detail?.stagesCompleted ?: 0
            val total = if (detail != null && detail.totalStages > 0) detail.totalStages else lesson.totalStages

            // Lógica de estado
            var status = "locked"

            if (stages >= total && total > 0) {
                status = "completed"
            } else if (isPreviousCompleted) {
                // Si la anterior está lista, esta se desbloquea
                status = "unlocked"
            }

            // Guardamos en el mapa para el adaptador
            resultMap[lesson.id] = LessonProgress(
                status = status,
                stagesCompleted = stages,
                totalStages = lesson.totalStages
            )

            // Actualizamos la bandera para la siguiente vuelta del loop
            isPreviousCompleted = (status == "completed")
        }
        return resultMap
    }

    private fun onLessonClicked(lesson: Lesson, progress: LessonProgress) {
        // Doble verificación de seguridad
        if (progress.status == "locked") {
            Toast.makeText(this, "Completa la lección anterior primero", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra("LESSON_ID", lesson.id)
            putExtra("LESSON_TITLE", lesson.title)
            putExtra("TOTAL_STAGES", lesson.totalStages)
            putExtra("LESSON_ORDER", lesson.order)
            putExtra("LANGUAGE_ID", currentLanguageId) // Usamos el que ya tenemos
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos al volver (por si completó una lección)
        if (auth.currentUser != null) loadData()
    }
}