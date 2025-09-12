package com.example.lingogo

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class que representa un Mensaje
 * en la subcolección "messages" de una sala de chat.
 */
data class Message(
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
    // No necesitamos el nombre del autor, solo el ID
    // (ya que el chat es 1-a-1)
)