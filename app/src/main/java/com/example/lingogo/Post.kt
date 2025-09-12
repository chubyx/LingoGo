package com.example.lingogo

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date


data class Post(

    @Exclude var id: String = "",

    val autorId: String = "",
    val autorNombre: String = "",
    val texto: String = "",
    @ServerTimestamp // Anotación para que Firebase ponga la hora del servidor
    val timestamp: Date? = null
)