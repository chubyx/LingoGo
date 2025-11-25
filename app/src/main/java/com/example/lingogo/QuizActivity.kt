package com.example.lingogo

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class QuizActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Vistas
    private lateinit var tvQuestionText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnOption1: Button
    private lateinit var btnOption2: Button
    private lateinit var btnOption3: Button
    private lateinit var btnCheck: Button

    // Datos del Intent
    private var lessonId: String = ""
    private var languageId: String = "en"
    private var totalStages: Int = 1

    // Lógica del Juego
    private var currentStageToPlay = 1
    private var questionsList = mutableListOf<QuizQuestionModel>()
    private var currentQuestionIndex = 0
    private var selectedAnswer = ""
    private var correctAnswer = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 1. Recibir datos
        lessonId = intent.getStringExtra("LESSON_ID") ?: ""
        languageId = intent.getStringExtra("LANGUAGE_ID") ?: "en"
        totalStages = intent.getIntExtra("TOTAL_STAGES", 1)

        // 2. Vincular Vistas
        tvQuestionText = findViewById(R.id.tvQuestionText)
        progressBar = findViewById(R.id.quizProgressBar)
        btnOption1 = findViewById(R.id.btnOption1)
        btnOption2 = findViewById(R.id.btnOption2)
        btnOption3 = findViewById(R.id.btnOption3)
        btnCheck = findViewById(R.id.btnCheck)

        setupButtons()

        // 3. Determinar qué etapa jugar y cargar preguntas
        determinarEtapaYCargar()
    }

    private fun determinarEtapaYCargar() {
        val userId = auth.currentUser?.uid ?: return

        // Consultamos el progreso actual del usuario para esta lección
        db.collection("users").document(userId)
            .collection("courses").document(languageId)
            .get()
            .addOnSuccessListener { document ->
                // Buscamos dentro del mapa lessonsProgress -> lessonId -> stagesCompleted
                val lessonsMap = document.get("lessonsProgress") as? Map<String, Map<String, Any>>
                val myLessonData = lessonsMap?.get(lessonId)

                // Si stagesCompleted es 0, jugamos la etapa 1. Si es 1, jugamos la 2.
                val stagesCompleted = (myLessonData?.get("stagesCompleted") as? Long)?.toInt() ?: 0

                currentStageToPlay = stagesCompleted + 1

                if (currentStageToPlay > totalStages) {
                    Toast.makeText(this, "¡Ya completaste esta lección! Modo repaso.", Toast.LENGTH_SHORT).show()
                    currentStageToPlay = totalStages // O cargar aleatorio
                }

                cargarPreguntas(currentStageToPlay)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar progreso", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun cargarPreguntas(stage: Int) {
        tvQuestionText.text = "Cargando etapa $stage..."

        // FILTRO IMPORTANTE: .whereEqualTo("stage", stage)
        db.collection("lessons").document(lessonId)
            .collection("quiz")
            .whereEqualTo("stage", stage)
            .get()
            .addOnSuccessListener { snapshot ->
                questionsList.clear()
                for (doc in snapshot) {
                    val q = doc.toObject(QuizQuestionModel::class.java)
                    questionsList.add(q)
                }

                if (questionsList.isNotEmpty()) {
                    mostrarPregunta()
                } else {
                    tvQuestionText.text = "No hay preguntas para esta etapa."
                }
            }
    }

    private fun mostrarPregunta() {
        if (currentQuestionIndex >= questionsList.size) {
            terminarLeccion()
            return
        }

        val q = questionsList[currentQuestionIndex]
        tvQuestionText.text = q.questionText
        correctAnswer = q.correctAnswer

        // Resetear botones
        resetButtonStyles()
        btnCheck.isEnabled = false
        btnCheck.text = "Comprobar"
        btnCheck.setBackgroundColor(Color.parseColor("#E0E0E0")) // Gris desactivado
        selectedAnswer = ""

        // Asignar opciones (Asegúrate de que haya 3 opciones en Firebase o maneja el index)
        if (q.options.size >= 3) {
            btnOption1.text = q.options[0]
            btnOption2.text = q.options[1]
            btnOption3.text = q.options[2]
        }

        // Actualizar barra de progreso
        val progreso = ((currentQuestionIndex.toFloat() / questionsList.size.toFloat()) * 100).toInt()
        progressBar.progress = progreso
    }

    private fun setupButtons() {
        val listener = View.OnClickListener { view ->
            val button = view as Button
            selectedAnswer = button.text.toString()

            // Visualmente marcar seleccionado
            resetButtonStyles()
            button.setBackgroundColor(Color.parseColor("#D1C4E9")) // Morado claro selección
            button.setTextColor(Color.BLACK)

            // Activar botón comprobar
            btnCheck.isEnabled = true
            btnCheck.setBackgroundColor(Color.parseColor("#58CC02")) // Verde activo
        }

        btnOption1.setOnClickListener(listener)
        btnOption2.setOnClickListener(listener)
        btnOption3.setOnClickListener(listener)

        btnCheck.setOnClickListener {
            verificarRespuesta()
        }
    }

    private fun verificarRespuesta() {
        if (btnCheck.text == "Continuar") {
            // Pasar a la siguiente pregunta
            currentQuestionIndex++
            mostrarPregunta()
            return
        }

        if (selectedAnswer == correctAnswer) {
            // CORRECTO
            Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()
            btnCheck.text = "Continuar"
            btnCheck.setBackgroundColor(Color.parseColor("#58CC02"))

            // Pintar la opción correcta de verde
            when(selectedAnswer) {
                btnOption1.text -> btnOption1.setBackgroundColor(Color.GREEN)
                btnOption2.text -> btnOption2.setBackgroundColor(Color.GREEN)
                btnOption3.text -> btnOption3.setBackgroundColor(Color.GREEN)
            }
        } else {
            // INCORRECTO
            Toast.makeText(this, "Incorrecto. Era: $correctAnswer", Toast.LENGTH_SHORT).show()
            btnCheck.text = "Continuar"
            btnCheck.setBackgroundColor(Color.RED)
        }
    }

    private fun terminarLeccion() {
        progressBar.progress = 100
        tvQuestionText.text = "¡Lección Completada!"
        btnOption1.visibility = View.GONE
        btnOption2.visibility = View.GONE
        btnOption3.visibility = View.GONE
        btnCheck.visibility = View.GONE

        // --- GUARDAR PROGRESO EN FIREBASE ---
        // 1. Sumar puntos al Dashboard
        ProgressManager.actualizarProgreso(languageId, 15) {
            // Callback: Puntos sumados exitosamente
        }

        // 2. Marcar esta etapa como completada
        guardarEtapaCompletada()
    }

    // Esta función estaba cortada, aquí está completa
    private fun guardarEtapaCompletada() {
        val userId = auth.currentUser?.uid ?: return

        val docRef = db.collection("users").document(userId)
            .collection("courses").document(languageId)

        // Preparamos los datos anidados
        // Esto crea: lessonsProgress -> {ID_LECCION} -> {stagesCompleted: 1}
        val dataToSave = hashMapOf(
            "lessonsProgress" to hashMapOf(
                lessonId to hashMapOf(
                    "stagesCompleted" to currentStageToPlay,
                    "totalStages" to totalStages
                )
            ),
            // Aseguramos que currentLessonId apunte a esta lección
            "currentLessonId" to lessonId
        )

        // USAMOS SET con MERGE: Esto es la clave.
        // Si no existe, lo crea. Si existe, solo actualiza estos campos sin borrar los puntos.
        docRef.set(dataToSave, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "¡Progreso Guardado!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error guardando etapa: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    // Esta función faltaba por completo
    private fun resetButtonStyles() {
        val defaultColor = Color.parseColor("#FFFFFF") // Blanco
        btnOption1.setBackgroundColor(defaultColor)
        btnOption2.setBackgroundColor(defaultColor)
        btnOption3.setBackgroundColor(defaultColor)

        btnOption1.setTextColor(Color.BLACK)
        btnOption2.setTextColor(Color.BLACK)
        btnOption3.setTextColor(Color.BLACK)
    }
}

// ESTA CLASE FALTABA AL FINAL DEL ARCHIVO
data class QuizQuestionModel(
    val id: String = "",
    val stage: Int = 1,          // <--- Movemos el Stage a la 2da posición
    val questionText: String = "", // <--- La pregunta pasa a la 3ra posición
    val options: List<String> = emptyList(),
    val correctAnswer: String = ""
)