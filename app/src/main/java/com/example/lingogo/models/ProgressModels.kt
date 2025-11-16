package com.example.linggo.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa el objeto 'progress' completo dentro de un documento 'user'.
 */
data class UserProgress(
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    @ServerTimestamp val lastLessonDate: Date? = null,
    val lessonsProgress: Map<String, LessonProgress> = emptyMap()
)

/**
 * Representa el progreso de una única lección (ej: "L1_VERB_TO_BE").
 * Esto va DENTRO del mapa 'lessonsProgress'.
 */
data class LessonProgress(
    val status: String = "locked", // "locked", "unlocked", "completed"
    val stagesCompleted: Int = 0,
    val totalStages: Int = 3
) {
    // Constructor vacío requerido por Firestore para deserializar
    constructor() : this("locked", 0, 3)
}


// --- Modelos para el Contenido del Quiz (de la colección 'lessons') ---

/**
 * Representa un documento de la colección raíz /lessons.
 * (No incluye la sub-colección de quiz)
 */
data class Lesson(
    val id: String = "", // El ID del documento (ej: "L1_VERB_TO_BE")
    val title: String = "",
    val totalStages: Int = 3,
    val order: Int = 99
)

/**
 * Representa un documento de la sub-colección /lessons/{lessonId}/quiz.
 */
data class QuizQuestion(
    val id: String = "", // El ID del documento (ej: "Q1")
    val stage: Int = 0,
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String = ""
)