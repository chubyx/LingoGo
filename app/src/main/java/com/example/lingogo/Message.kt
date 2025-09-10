package com.example.lingogo

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class que representa un Mensaje
 * en la subcolección "messages" de una sala de chat.
 */
data class Message(
    // --- ¡CAMBIO AQUÍ! ---
    @Exclude var id: String = "",

    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)