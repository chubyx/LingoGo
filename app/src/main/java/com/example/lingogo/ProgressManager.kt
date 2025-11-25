package com.example.lingogo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date

object ProgressManager {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Llama a esta función cuando el usuario termine una lección.
     * @param languageId El idioma actual (ej: "en")
     * @param puntosGanados Cuántos puntos ganó en esta lección (ej: 10 o 15)
     */
    fun actualizarProgreso(languageId: String, puntosGanados: Int, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(userId).collection("courses").document(languageId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)

            var newTotalPoints = 0L
            var xpToday = 0L
            var streak = 0L
            var dailyGoal = 50L // Meta por defecto

            if (snapshot.exists()) {
                // --- CASO 1: EL USUARIO YA TIENE DATOS ---
                val currentPoints = snapshot.getLong("points") ?: 0
                dailyGoal = snapshot.getLong("dailyGoal") ?: 50
                val storedXpToday = snapshot.getLong("xpToday") ?: 0
                val lastDateTimestamp = snapshot.getTimestamp("lastPracticeDate")
                val storedStreak = snapshot.getLong("streak") ?: 0

                // Lógica de fechas
                val lastDate = lastDateTimestamp?.toDate()
                if (lastDate == null || !isSameDay(lastDate, Date())) {
                    // Nuevo día
                    xpToday = 0
                    if (isYesterday(lastDate)) streak = storedStreak + 1
                    else if (!isSameDay(lastDate, Date())) streak = 1
                } else {
                    // Mismo día
                    xpToday = storedXpToday
                    streak = storedStreak
                }

                xpToday += puntosGanados
                newTotalPoints = currentPoints + puntosGanados

                // Actualizamos
                transaction.update(docRef,
                    "points", newTotalPoints,
                    "xpToday", xpToday,
                    "streak", streak,
                    "dailyProgress", calculateProgress(xpToday, dailyGoal),
                    "lastPracticeDate", FieldValue.serverTimestamp()
                )

            } else {
                // --- CASO 2: PRIMERA VEZ (CREAR DOCUMENTO) ---
                newTotalPoints = puntosGanados.toLong()
                xpToday = puntosGanados.toLong()
                streak = 1

                val newData = hashMapOf(
                    "points" to newTotalPoints,
                    "xpToday" to xpToday,
                    "streak" to streak,
                    "dailyGoal" to dailyGoal,
                    "dailyProgress" to calculateProgress(xpToday, dailyGoal),
                    "lastPracticeDate" to FieldValue.serverTimestamp(),
                    "currentLessonId" to "${languageId}_1" // Ej: en_1 por defecto
                )

                // Usamos SET en lugar de UPDATE para crear
                transaction.set(docRef, newData)
            }

        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            // Log para que veas si falla
            System.out.println("ERROR ProgressManager: ${e.message}")
        }
    }

    private fun calculateProgress(xp: Long, goal: Long): Int {
        var progress = (xp.toDouble() / goal.toDouble()) * 100
        if (progress > 100) progress = 100.0
        return progress.toInt()
    }

    // Funciones auxiliares para comparar fechas
    private fun isSameDay(date1: Date?, date2: Date): Boolean {
        if (date1 == null) return false
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(date1: Date?): Boolean {
        if (date1 == null) return false
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val calCheck = Calendar.getInstance().apply { time = date1 }
        return yesterday.get(Calendar.YEAR) == calCheck.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == calCheck.get(Calendar.DAY_OF_YEAR)
    }
}