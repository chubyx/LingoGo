package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.lingogo.database.AppDatabase
import com.example.lingogo.database.FavoriteWord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.lang.Exception

// IMPORTANTE: Asegúrate de tener esta actividad importada
import com.example.lingogo.ListaLeccionesActivity

class InitionActivity : AppCompatActivity() {

    private val TAG = "InitionActivity"

    // Vistas de Navegación
    private lateinit var headerCard: CardView
    private lateinit var cardViewComunidad: CardView
    private lateinit var cardViewConfig: CardView
    private lateinit var cardGames: CardView
    private lateinit var cardPalabrasFavoritas: CardView
    private lateinit var cardEmpezarLeccion: CardView
    private lateinit var cardVoiceIoT: CardView

    // Vistas del Header
    private lateinit var tvBienvenidaHeader: TextView
    private lateinit var tvSubtituloHeader: TextView

    // Vistas de Prueba (Room)
    private lateinit var etPalabraTemp: EditText
    private lateinit var btnGuardarTemp: Button

    // Firebase y Room
    private lateinit var auth: FirebaseAuth
    private lateinit var dbFirestore: FirebaseFirestore
    private lateinit var dbRoom: AppDatabase

    private val configActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d(TAG, "Regresando de Configuración. Re-dibujando...")
            recreate()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        // Inicializar
        auth = FirebaseAuth.getInstance()
        dbFirestore = FirebaseFirestore.getInstance()
        dbRoom = AppDatabase.getDatabase(applicationContext)

        // Enlazar Vistas
        headerCard = findViewById(R.id.headerCard)
        tvBienvenidaHeader = findViewById(R.id.tvBienvenidaHeader)
        tvSubtituloHeader = findViewById(R.id.tvSubtituloHeader)

        cardViewComunidad = findViewById(R.id.cardComunidad)
        cardViewConfig = findViewById(R.id.cardConfiguracion)
        cardGames = findViewById(R.id.cardGames)
        cardPalabrasFavoritas = findViewById(R.id.cardPalabrasFavoritas)
        cardEmpezarLeccion = findViewById(R.id.cardEmpezarLeccion)
        cardVoiceIoT = findViewById(R.id.cardVoiceIoT)

        etPalabraTemp = findViewById(R.id.etPalabraFavoritaTemp)
        btnGuardarTemp = findViewById(R.id.btnGuardarPalabraTemp)

        // Configurar Listeners
        setupListeners()
    }

    private fun setupListeners() {
        // Header -> Perfil
        headerCard.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        // Navegación
        cardViewComunidad.setOnClickListener {
            startActivity(Intent(this, CommunityActivity::class.java))
        }
        cardGames.setOnClickListener {
            startActivity(Intent(this, GamesActivity::class.java))
        }
        cardPalabrasFavoritas.setOnClickListener {
            startActivity(Intent(this, PalabrasFavoritasActivity::class.java))
        }
        cardViewConfig.setOnClickListener {
            configActivityLauncher.launch(Intent(this, ConfigActivity::class.java))
        }

        // Listener IoT
        cardVoiceIoT.setOnClickListener {
            startActivity(Intent(this, VoiceControlActivity::class.java))
        }

        // --- CAMBIO PRINCIPAL AQUI ---
        // Antes solo mostraba un Toast, ahora navega a ListaLeccionesActivity
        cardEmpezarLeccion.setOnClickListener {
            val intent = Intent(this, ListaLeccionesActivity::class.java)
            startActivity(intent)
        }
        // -----------------------------

        // Guardar Palabra
        btnGuardarTemp.setOnClickListener {
            val textoPalabra = etPalabraTemp.text.toString().trim()
            if (textoPalabra.isNotEmpty()) {
                guardarPalabraFavorita(textoPalabra)
            } else {
                // Si tienes el string en strings.xml usa getString(R.string.error...),
                // si no, usa el texto directo para evitar errores:
                Toast.makeText(this, "Escribe una palabra", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarPalabraFavorita(palabra: String) {
        lifecycleScope.launch {
            try {
                val nuevaPalabra = FavoriteWord(word = palabra)
                dbRoom.favoriteWordDao().addFavorite(nuevaPalabra)
                runOnUiThread {
                    Toast.makeText(this@InitionActivity, "¡'$palabra' guardada!", Toast.LENGTH_SHORT).show()
                    etPalabraTemp.text.clear()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error Room", e)
                runOnUiThread {
                    Toast.makeText(this@InitionActivity, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkUserSession()
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            cargarDatosUsuario(currentUser.uid)
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun cargarDatosUsuario(userId: String) {
        dbFirestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombre") ?: "Estudiante"

                    // Asegúrate de tener este string en strings.xml con un placeholder (%s)
                    // O cámbialo a texto plano: "Hola, $nombre"
                    tvBienvenidaHeader.text = getString(R.string.inicio_bienvenida_usuario, nombre)
                    tvSubtituloHeader.text = getString(R.string.inicio_listo_aprender)
                } else {
                    tvBienvenidaHeader.text = getString(R.string.inicio_hola_estudiante)
                }
            }
            .addOnFailureListener {
                tvBienvenidaHeader.text = getString(R.string.inicio_hola_estudiante)
            }
    }
}