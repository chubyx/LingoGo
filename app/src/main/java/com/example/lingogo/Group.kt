package com.example.lingogo

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class que representa una SALA DE GRUPO
 * en la colección "group_rooms".
 */
data class Group(
    @Exclude var id: String = "",
    val nombre: String = "",
    val creadorId: String = "",
    val participants: List<String> = listOf(), // Lista de IDs de miembros

    @ServerTimestamp
    val lastActivity: Date? = null,
    val lastMessage: String = "Grupo creado."
)