package com.example.lingogo

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class para un mensaje de GRUPO.
 * Es igual a 'Message', pero incluye el 'senderName'
 * para saber quién lo envió.
 */
data class GroupMessage(
    // --- ¡CAMBIO AQUÍ! ---
    @Exclude var id: String = "",

    val senderId: String = "",
    val senderName: String = "...",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)