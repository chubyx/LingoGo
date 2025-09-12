package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InitionActivity : AppCompatActivity() {

    private val TAG = "InitionActivity"

    // Vistas del Dashboard
    private lateinit var cardViewComunidad: CardView
    private lateinit var cardViewConfig: CardView
    private lateinit var cardGames: CardView
    private lateinit var cardPerfil: CardView
    // (Añadí estas para los Toasts)
    private lateinit var cardEmpezarLeccion: CardView
    private lateinit var cardProgreso: CardView

    // Vistas del Header (¡NUEVO!)
    private lateinit var tvBienvenidaHeader: TextView
    private lateinit var tvSubtituloHeader: TextView
    private lateinit var ivHeaderProfilePic: ImageView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Enlazar Vistas (Cards de navegación)
        cardViewComunidad = findViewById(R.id.cardComunidad)
        cardViewConfig = findViewById(R.id.cardConfiguracion)
        cardGames = findViewById(R.id.cardGames)
        cardPerfil = findViewById(R.id.cardPerfil)
        cardEmpezarLeccion = findViewById(R.id.cardEmpezarLeccion)
        cardProgreso = findViewById(R.id.cardProgreso)

        // Enlazar Vistas (Header) (¡NUEVO!)
        tvBienvenidaHeader = findViewById(R.id.tvBienvenidaHeader)
        tvSubtituloHeader = findViewById(R.id.tvSubtituloHeader)
        ivHeaderProfilePic = findViewById(R.id.ivHeaderProfilePic)

        // Configurar Listeners para los CardViews
        setupCardListeners()

        // (El botón de cerrar sesión de prueba que estaba aquí se movió a PerfilActivity)
    }

    override fun onStart() {
        super.onStart()
        // Comprobar si el usuario está logueado
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Si no hay usuario, volver al Login
            irALogin()
        } else {
            // Si hay usuario, cargar sus datos
            currentUserId = currentUser.uid
            Log.d(TAG, "Usuario logueado: $currentUserId")
            cargarDatosUsuario()
        }
    }

    /**
     * Carga los datos del usuario (nombre y foto) desde Firestore
     * y los muestra en el header.
     */
    private fun cargarDatosUsuario() {
        if (currentUserId == null) return

        db.collection("users").document(currentUserId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    Log.d(TAG, "Datos de usuario encontrados.")
                    val nombre = document.getString("nombre") ?: "Estudiante"
                    val fotoUrl = document.getString("fotoUrl") // ¡NUEVO!

                    // Actualizar el saludo
                    tvBienvenidaHeader.text = "¡Hola, $nombre!"
                    tvSubtituloHeader.text = "¡Listo para aprender!"

                    // --- ¡NUEVO! Cargar la foto de perfil con Glide ---
                    if (fotoUrl != null && fotoUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(fotoUrl)
                            .circleCrop() // Hacerla redonda
                            .into(ivHeaderProfilePic)
                    } else {
                        // Si no tiene foto, mostrar un ícono por defecto
                        ivHeaderProfilePic.setImageResource(R.drawable.ic_perfil_por_defecto) // (Necesitas crear este drawable)
                    }

                } else {
                    Log.w(TAG, "No se encontró el documento del usuario en Firestore.")
                    tvBienvenidaHeader.text = "¡Hola, Estudiante!"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar datos de Firestore", e)
                Toast.makeText(this, "Error al cargar sus datos", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Configura todos los OnClickListeners para las tarjetas del dashboard.
     */
    private fun setupCardListeners() {
        cardViewComunidad.setOnClickListener {
            // (Esto ya debería estar abriendo CommunityActivity)
            val intent = Intent(this, CommunityActivity::class.java)
            startActivity(intent)
        }

        cardViewConfig.setOnClickListener {
            // (Esto ya debería estar abriendo ConfigActivity)
            val intent = Intent(this, ConfigActivity::class.java)
            startActivity(intent)
        }

        cardPerfil.setOnClickListener {
            // (Esto ya debería estar abriendo PerfilActivity)
            val intent = Intent(this, PerfilActivity::class.java)
            startActivity(intent)
        }

        // --- Módulos de tus compañeros (con Toasts) ---
        cardGames.setOnClickListener {
            Toast.makeText(this, "Módulo de Juegos (Aún no implementado)", Toast.LENGTH_SHORT).show()
            // val intent = Intent(this, GamesActivity::class.java)
            // startActivity(intent)
        }

        cardEmpezarLeccion.setOnClickListener {
            Toast.makeText(this, "Módulo de Lecciones (Aún no implementado)", Toast.LENGTH_SHORT).show()
            // val intent = Intent(this, LeccionActivity::class.java)
            // startActivity(intent)
        }

        cardProgreso.setOnClickListener {
            Toast.makeText(this, "Módulo de Progreso (Aún no implementado)", Toast.LENGTH_SHORT).show()
            // val intent = Intent(this, ProgresoActivity::class.java)
            // startActivity(intent)
        }
    }

    private fun irALogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}