package com.example.lingogo

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class que representa un Comentario en la subcolección "comentarios".
 */
data class Comentario(
    // --- ¡CAMBIO AQUÍ! ---
    // Añadimos el ID para poder borrarlo
    @Exclude var id: String = "",

    val autorId: String = "",
    val autorNombre: String = "",
    val texto: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)