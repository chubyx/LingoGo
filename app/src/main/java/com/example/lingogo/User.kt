package com.example.lingogo

data class User(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val fotoUrl: String = "",
    val fcmToken: String = "",
    // --- NUEVOS CAMPOS ---
    val idiomas: String = "No especificado", // Ej: "Español, Inglés"
    val descripcion: String = "¡Hola! Estoy aprendiendo idiomas en Lingogo."
)