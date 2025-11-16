package com.example.lingogo


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.linggo.models.Lesson
import com.example.linggo.models.LessonProgress
import com.example.linggo.models.UserProgress
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.lingogo.LeccionAdapter// Necesitaremos crear este adaptador

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

        toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        // El adaptador necesita una lambda para manejar el clic en una lección
        adapter = LeccionAdapter { lesson, lessonProgress ->
            onLessonClicked(lesson, lessonProgress)
        }
        recyclerView.adapter = adapter
    }

    private fun loadData() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 1. Obtener el progreso del usuario
        val userProgressDoc = db.collection("users").document(userId)
        userProgressDoc.get().addOnSuccessListener { doc ->
            userProgress = doc.get("progress", UserProgress::class.java) ?: UserProgress()

            // 2. Obtener la lista de todas las lecciones
            db.collection("lessons")
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    allLessons = snapshot.documents.map { d ->
                        d.toObject(Lesson::class.java)!!.copy(id = d.id)
                    }

                    // 3. Combinar datos y actualizar la UI
                    updateRecyclerView()
                }
                .addOnFailureListener { e -> handleError(e) }
        }
            .addOnFailureListener { e -> handleError(e) }
    }

    private fun updateRecyclerView() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        val progressMap = userProgress?.lessonsProgress ?: emptyMap()

        // Pasamos al adaptador la lista de todas las lecciones y el mapa de progreso
        adapter.submitList(allLessons, progressMap)
    }

    private fun onLessonClicked(lesson: Lesson, progress: LessonProgress) {
        if (progress.status == "locked") {
            Toast.makeText(this, "Completa la lección anterior primero", Toast.LENGTH_SHORT).show()
            return
        }

        // Iniciar la QuizActivity
        val intent = Intent(this, QuizActivity::class.java)
        intent.putExtra("LESSON_ID", lesson.id)
        intent.putExtra("LESSON_TITLE", lesson.title)
        intent.putExtra("TOTAL_STAGES", lesson.totalStages)
        intent.putExtra("LESSON_ORDER", lesson.order)
        startActivity(intent)
    }

    private fun handleError(e: Exception) {
        progressBar.visibility = View.GONE
        Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando volvemos del Quiz, para mostrar el progreso actualizado
        if (userProgress != null) {
            loadData()
        }
    }
}