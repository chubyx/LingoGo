package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InitionActivity : AppCompatActivity() {

    // Vistas de tu XML
    private var cardViewComunidad: CardView? = null
    private var cardViewConfig: CardView? = null
    private var cardGames: CardView? = null
    private var cardPerfil: CardView? = null

    // --- ¡NUEVO! Vistas para el saludo ---
    private lateinit var tvBienvenida: TextView
    private lateinit var tvSubtitulo: TextView

    // --- ¡NUEVO! Firebase ---
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val TAG = "InitionActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio) // Usando tu XML actualizado

        // --- ¡NUEVO! Inicializar Firebase ---
        initFirebase()

        // --- ¡NUEVO! Enlazar vistas de saludo ---
        // (Usando los IDs de activity_inicio.xml)
        tvBienvenida = findViewById(R.id.tvBienvenidaHeader)
        tvSubtitulo = findViewById(R.id.tvSubtituloHeader)

        // --- Tu código original para los CardView ---
        cardViewComunidad = findViewById<CardView>(R.id.cardComunidad)
        cardViewConfig = findViewById<CardView>(R.id.cardConfiguracion)
        cardGames = findViewById<CardView>(R.id.cardGames)
        cardPerfil = findViewById<CardView>(R.id.cardPerfil)

        //Acción para presionar
        cardViewComunidad!!.setOnClickListener(View.OnClickListener { view: View? ->
             val intent = Intent(this@InitionActivity, CommunityActivity::class.java)
             startActivity(intent)

        })

        cardViewConfig!!.setOnClickListener(View.OnClickListener { view: View? ->
             val intent = Intent(this@InitionActivity, ConfigActivity::class.java)
             startActivity(intent)

        })

        cardGames!!.setOnClickListener(View.OnClickListener { view: View? ->
            val intent = Intent(this@InitionActivity, GamesActivity::class.java)
            startActivity(intent)

        })
        cardPerfil!!.setOnClickListener(View.OnClickListener { view: View? ->
             val intent = Intent(this@InitionActivity, PerfilActivity::class.java)
             startActivity(intent)

        })
    }

    // --- ¡NUEVO! Inicializa Auth y Firestore ---
    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    // --- ¡NUEVO! Revisa la sesión al iniciar la actividad ---
    override fun onStart() {
        super.onStart()
        checkUserSession()
    }

    // --- ¡NUEVO! Comprueba si el usuario está logueado ---
    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // El usuario está logueado, cargar sus datos
            Log.d(TAG, "Usuario logueado: ${currentUser.uid}")
            cargarDatosUsuario(currentUser.uid)
        } else {
            // El usuario no está logueado, enviarlo a MainActivity (Login)
            Log.d(TAG, "Usuario no logueado, volviendo a Login.")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // --- ¡NUEVO! Carga los datos del usuario desde Firestore ---
    private fun cargarDatosUsuario(userId: String) {
        // Busca en la colección "users" el documento con el ID del usuario
        val userRef = db.collection("users").document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // El documento existe, obtener el nombre
                    val nombre = document.getString("nombre") ?: "Estudiante"
                    Log.d(TAG, "Datos de usuario cargados: $nombre")

                    // Actualizar los TextViews
                    tvBienvenida.text = "¡Bienvenido, $nombre!"
                    tvSubtitulo.text = getString(R.string.inicio_listo_aprender)

                } else {
                    // El documento no existe (raro si el login guardó bien los datos)
                    Log.w(TAG, "El documento del usuario no existe en Firestore.")
                    tvBienvenida.text = getString(R.string.inicio_hola_estudiante)
                }
            }
            .addOnFailureListener { exception ->
                // Error al intentar obtener los datos
                Log.e(TAG, "Error al cargar datos de Firestore", exception)
                Toast.makeText(this, "Error al cargar tus datos.", Toast.LENGTH_SHORT).show()
                tvBienvenida.text = getString(R.string.inicio_hola_estudiante)
            }
    }
}

