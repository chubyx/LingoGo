package com.example.lingogo

/**
 * Data class que representa un Usuario en la colección "users".
 * Esta SÍ la leemos completa, a diferencia del Post o Comentario.
 */
data class User(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val fotoUrl: String = ""
    // (Añadir más campos si los tienes: telefono, descripcion, etc.)
)