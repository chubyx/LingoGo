package com.example.lingogo

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.linggo.models.Lesson
import com.example.linggo.models.QuizQuestion
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class QuizActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Datos de la Lección
    private var lessonId: String? = null
    private var lessonTitle: String? = null
    private var languageId: String? = null // <--- Nuevo
    private var totalStages: Int = 3
    private var currentLessonOrder: Int = -1
    private var allQuestions = mutableListOf<QuizQuestion>()

    // Estado del Quiz
    private var currentStage = 1
    private var correctStages = 0
    private var currentQuestion: QuizQuestion? = null
    private var isClickable = true

    // Vistas
    private lateinit var toolbar: MaterialToolbar
    private lateinit var stageTitle: TextView
    private lateinit var questionText: TextView
    private lateinit var optionButtons: List<MaterialButton>

    // Colores
    private val colorCorrect by lazy { ColorStateList.valueOf(Color.parseColor("#4CAF50")) }
    private val colorIncorrect by lazy { ColorStateList.valueOf(Color.parseColor("#F44336")) }
    private val colorNeutral by lazy { ColorStateList(arrayOf(intArrayOf()), intArrayOf(getColor(com.google.android.material.R.color.design_default_color_primary))) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Obtener datos del Intent
        lessonId = intent.getStringExtra("LESSON_ID")
        lessonTitle = intent.getStringExtra("LESSON_TITLE")
        totalStages = intent.getIntExtra("TOTAL_STAGES", 3)
        currentLessonOrder = intent.getIntExtra("LESSON_ORDER", -1)
        languageId = intent.getStringExtra("LANGUAGE_ID") // <--- Recibimos el idioma

        if (lessonId == null || currentLessonOrder == -1) {
            Toast.makeText(this, "Error de datos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar Vistas
        toolbar = findViewById(R.id.toolbarQuiz)
        stageTitle = findViewById(R.id.textViewStageTitle)
        questionText = findViewById(R.id.textViewQuestion)
        optionButtons = listOf(
            findViewById(R.id.buttonOption1),
            findViewById(R.id.buttonOption2),
            findViewById(R.id.buttonOption3),
            findViewById(R.id.buttonOption4)
        )

        toolbar.title = lessonTitle
        toolbar.setNavigationOnClickListener { finish() }

        loadQuizQuestions()
    }

    private fun loadQuizQuestions() {
        db.collection("lessons").document(lessonId!!)
            .collection("quiz")
            .orderBy("stage")
            .get()
            .addOnSuccessListener { snapshot ->
                allQuestions = snapshot.documents.mapNotNull { it.toObject<QuizQuestion>() }.toMutableList()
                if (allQuestions.isNotEmpty()) {
                    showQuestionForCurrentStage()
                } else {
                    Toast.makeText(this, "Error: No hay preguntas", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showQuestionForCurrentStage() {
        currentQuestion = allQuestions.find { it.stage == currentStage }
        if (currentQuestion == null) {
            finishQuiz()
            return
        }

        isClickable = true
        stageTitle.text = "Etapa $currentStage / $totalStages"
        questionText.text = currentQuestion!!.questionText

        optionButtons.forEachIndexed { index, button ->
            if (index < currentQuestion!!.options.size) {
                button.text = currentQuestion!!.options[index]
                button.visibility = View.VISIBLE
                button.backgroundTintList = colorNeutral

                button.setOnClickListener { onOptionSelected(button) }
            } else {
                button.visibility = View.GONE
            }
        }
    }

    private fun onOptionSelected(selectedButton: MaterialButton) {
        if (!isClickable) return
        isClickable = false

        val selectedAnswer = selectedButton.text.toString()
        val correctAnswer = currentQuestion!!.correctAnswer

        if (selectedAnswer == correctAnswer) {
            correctStages++
            selectedButton.backgroundTintList = colorCorrect
        } else {
            selectedButton.backgroundTintList = colorIncorrect
            optionButtons.find { it.text == correctAnswer }?.backgroundTintList = colorCorrect
        }

        Handler(Looper.getMainLooper()).postDelayed({
            currentStage++
            if (currentStage > totalStages) finishQuiz()
            else showQuestionForCurrentStage()
        }, 1500)
    }

    private fun finishQuiz() {
        val userId = auth.currentUser?.uid ?: return

        val userDocRef = db.collection("users").document(userId)
        val pointsToAdd = correctStages * 200
        val lessonStatus = if (correctStages == totalStages) "completed" else "unlocked"

        val progressData = mapOf(
            "progress.totalPoints" to FieldValue.increment(pointsToAdd.toLong()),
            "progress.currentStreak" to FieldValue.increment(1),
            "progress.lessonsProgress.$lessonId.status" to lessonStatus,
            "progress.lessonsProgress.$lessonId.stagesCompleted" to correctStages,
            "progress.lessonsProgress.$lessonId.totalStages" to totalStages
        )

        userDocRef.update(progressData).addOnSuccessListener {
            if (lessonStatus == "completed") unlockNextLesson()
            else finish()
        }
    }

    private fun unlockNextLesson() {
        val userId = auth.currentUser?.uid ?: return
        val nextLessonOrder = currentLessonOrder + 1
        val currentLang = languageId ?: "en" // Por defecto inglés si falla

        // BUSCAMOS LA SIGUIENTE LECCIÓN *DEL MISMO IDIOMA*
        db.collection("lessons")
            .whereEqualTo("languageId", currentLang) // <--- Filtro importante
            .whereEqualTo("order", nextLessonOrder)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "¡Curso completado!", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val nextLessonDoc = snapshot.documents.first()
                val nextLessonId = nextLessonDoc.id
                val nextLesson = nextLessonDoc.toObject(Lesson::class.java) ?: return@addOnSuccessListener

                // Desbloqueamos la siguiente
                val userDocRef = db.collection("users").document(userId)
                val unlockData = mapOf(
                    "progress.lessonsProgress.$nextLessonId.status" to "unlocked",
                    "progress.lessonsProgress.$nextLessonId.stagesCompleted" to 0,
                    "progress.lessonsProgress.$nextLessonId.totalStages" to nextLesson.totalStages
                )

                userDocRef.update(unlockData).addOnSuccessListener {
                    Toast.makeText(this, "¡Siguiente lección desbloqueada!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }
}