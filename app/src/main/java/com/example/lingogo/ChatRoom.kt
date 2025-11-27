package com.example.lingogo

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class que representa una SALA DE CHAT en la colección "chat_rooms".
 * Usaremos esto para la lista de "Chats".
 *
 * ¡Importante! Esta clase es un modelo "ViewModel". No representa
 * exactamente el documento de Firestore, sino que la usamos
 * en la app para *mostrar* la información de la sala (incluyendo
 * los datos del *otro* usuario).
 */
data class ChatRoom(
    @Exclude var id: String = "", // El ID de la sala (ej: uid1_uid2)

    // Datos del OTRO participante
    var otherUserId: String = "",
    var otherUserName: String = "",
    var otherUserPhotoUrl: String = "",

    // Datos del último mensaje
    var lastMessage: String = "Inicia la conversación...",
    @ServerTimestamp
    var lastActivity: Date? = null,

    // --- NUEVO: Mapa para contar mensajes no leídos ---
    // Clave = ID del usuario, Valor = Cantidad de mensajes sin leer
    var unreadCounts: Map<String, Long> = emptyMap()
)