package com.example.lingogo

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class para un mensaje de GRUPO.
 * Es igual a 'Message', pero incluye el 'senderName'
 * para saber quién lo envió.
 */
data class GroupMessage(
    val senderId: String = "",
    val senderName: String = "...", // ¡Importante!
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)