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
    val creatorId: String = "", // ID del Creador/Admin

    // Usamos dos listas por compatibilidad si cambiaste el nombre del campo en la BD
    val members: List<String> = emptyList(),
    val participants: List<String> = emptyList(),

    @ServerTimestamp
    val lastActivity: Date? = null,
    val lastMessage: String = "Grupo creado."
)