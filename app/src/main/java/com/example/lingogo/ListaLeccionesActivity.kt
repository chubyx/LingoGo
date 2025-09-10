package com.example.lingogo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lingogo.LeccionAdapter
import com.example.linggo.models.Lesson
import com.example.linggo.models.LessonProgress
import com.example.linggo.models.UserProgress
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

class ListaLeccionesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeccionAdapter

    private var allLessons = listOf<Lesson>()
    private var userProgress: UserProgress? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_lecciones)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerViewLecciones)

        // Configuración del RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = LeccionAdapter { lesson, progress ->
            onLessonClicked(lesson, progress)
        }
        recyclerView.adapter = adapter
    }

    // --- DETECTIVE DE IDIOMA ---
    private fun getTargetLanguageCode(): String {
        // 1. Leemos las preferencias.
        // Si usas PreferenceManager en tu Config, cambia esto a PreferenceManager.getDefaultSharedPreferences(this)
        val sharedPreferences = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

        // Obtenemos el string guardado ("German", "Alemán", etc.)
        val selection = sharedPreferences.getString("app_language", "")?.lowercase() ?: ""

        // 2. Traducimos la selección a código ISO ("de", "en", "fr")
        return when {
            // ALEMÁN
            selection.contains("german") || selection.contains("alem") || selection.contains("deut") -> "de"
            // FRANCÉS
            selection.contains("french") || selection.contains("franc") || selection.contains("fran") -> "fr"
            // INGLÉS
            selection.contains("english") || selection.contains("ingl") -> "en"
            // JAPONÉS
            selection.contains("japanese") || selection.contains("japon") || selection.contains("nihon") -> "ja"
            // PORTUGUÉS
            selection.contains("portuguese") || selection.contains("portugu") || selection.contains("brasil") -> "pt"
            // ESPAÑOL (Por si acaso)
            selection.contains("spanish") || selection.contains("españ") -> "es"

            // Fallback: Si no encuentra nada, usa el idioma del teléfono o Inglés
            else -> {
                val deviceLang = Locale.getDefault().language
                if (deviceLang in listOf("en", "de", "fr", "ja", "pt")) deviceLang else "en"
            }
        }
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

        // 1. Determinamos qué idioma cargar
        val targetCode = getTargetLanguageCode()

        // Título de la barra (Ej: "Lecciones (DE)")
        val titleBase = getString(R.string.inicio_continuar_leccion)
        toolbar.title = "$titleBase (${targetCode.uppercase()})"

        // 2. Obtenemos el progreso
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            userProgress = doc.get("progress", UserProgress::class.java) ?: UserProgress()

            // 3. CONSULTA A FIREBASE CON EL FILTRO DE IDIOMA
            db.collection("lessons")
                .whereEqualTo("languageId", targetCode) // <--- FILTRO MÁGICO
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    allLessons = snapshot.documents.map { d ->
                        d.toObject(Lesson::class.java)!!.copy(id = d.id)
                    }

                    if (allLessons.isEmpty()) {
                        Toast.makeText(this, "No hay curso disponible para: $targetCode", Toast.LENGTH_LONG).show()
                    }

                    updateRecyclerView()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error cargando lecciones", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateRecyclerView() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        val progressMap = userProgress?.lessonsProgress ?: emptyMap()
        adapter.submitList(allLessons, progressMap)
    }

    private fun onLessonClicked(lesson: Lesson, progress: LessonProgress) {
        if (progress.status == "locked") {
            Toast.makeText(this, "Completa la lección anterior primero", Toast.LENGTH_SHORT).show()
            return
        }

        // Pasamos todos los datos necesarios al Quiz, incluido el IDIOMA y el ORDEN
        val intent = Intent(this, QuizActivity::class.java)
        intent.putExtra("LESSON_ID", lesson.id)
        intent.putExtra("LESSON_TITLE", lesson.title)
        intent.putExtra("TOTAL_STAGES", lesson.totalStages)
        intent.putExtra("LESSON_ORDER", lesson.order)
        intent.putExtra("LANGUAGE_ID", lesson.languageId) // <--- IMPORTANTE para saber cuál desbloquear después
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (userProgress != null) loadData()
    }
}