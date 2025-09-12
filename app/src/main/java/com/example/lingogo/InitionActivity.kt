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
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.lingogo.database.AppDatabase
import com.example.lingogo.database.FavoriteWord
import com.example.lingogo.database.SettingsDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Exception

class InitionActivity : AppCompatActivity() {

    // Vistas XML
    private var cardViewComunidad: CardView? = null
    private var cardViewConfig: CardView? = null
    private var cardGames: CardView? = null
    private var cardPerfil: CardView? = null
    private var cardPalabrasFavoritas: CardView? = null

    // Vistas Saludo
    private lateinit var tvBienvenida: TextView
    private lateinit var tvSubtitulo: TextView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var dbFirestore: FirebaseFirestore

    // Room (Guardar Palabras)
    private lateinit var dbRoom: AppDatabase
    private lateinit var etPalabraTemp: EditText
    private lateinit var btnGuardarTemp: Button

    private val TAG = "InitionActivity"

    // Launcher para refrescar la pantalla al volver de Config
    private val configActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "Regresando de Configuración. Re-dibujando la pantalla...")
            recreate()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // MyApplication.kt ya se encarga de cargar el tema
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        // Inicializar Room DB
        dbRoom = AppDatabase.getDatabase(applicationContext)

        // Inicializar Firebase
        initFirebase()

        // Enlazar Vistas
        tvBienvenida = findViewById(R.id.tvBienvenidaHeader)
        tvSubtitulo = findViewById(R.id.tvSubtituloHeader)

        cardViewComunidad = findViewById<CardView>(R.id.cardComunidad)
        cardViewConfig = findViewById<CardView>(R.id.cardConfiguracion)
        cardGames = findViewById<CardView>(R.id.cardGames)
        cardPerfil = findViewById<CardView>(R.id.cardPerfil)
        cardPalabrasFavoritas = findViewById<CardView>(R.id.cardPalabrasFavoritas)

        // Enlazar Vistas de Prueba
        etPalabraTemp = findViewById(R.id.etPalabraFavoritaTemp)
        btnGuardarTemp = findViewById(R.id.btnGuardarPalabraTemp)

        // Listeners de tus CardViews
        cardViewComunidad!!.setOnClickListener(View.OnClickListener {
            startActivity(Intent(this@InitionActivity, CommunityActivity::class.java))
        })

        // Listener de Configuración (usa el launcher)
        cardViewConfig!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@InitionActivity, ConfigActivity::class.java)
            configActivityLauncher.launch(intent)
        })

        cardGames!!.setOnClickListener(View.OnClickListener {
            startActivity(Intent(this@InitionActivity, GamesActivity::class.java))
        })
        cardPerfil!!.setOnClickListener(View.OnClickListener {
            startActivity(Intent(this@InitionActivity, PerfilActivity::class.java))
        })
        cardPalabrasFavoritas!!.setOnClickListener(View.OnClickListener {
            startActivity(Intent(this@InitionActivity, PalabrasFavoritasActivity::class.java))
        })

        // Listener del Botón Guardar
        btnGuardarTemp.setOnClickListener {
            Log.d("PRUEBA_GUARDAR", "¡El botón 'Guardar' SÍ responde al clic!")
            val textoPalabra = etPalabraTemp.text.toString().trim()
            if (textoPalabra.isNotEmpty()) {
                guardarPalabraFavorita(textoPalabra)
            } else {
                Log.d("PRUEBA_GUARDAR", "El texto estaba vacío. Mostrando Toast.")
                Toast.makeText(this, "Escribe una palabra", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para guardar en Room
    private fun guardarPalabraFavorita(palabra: String) {
        Log.d("PRUEBA_GUARDAR", "Función 'guardarPalabraFavorita' iniciada con: $palabra")
        lifecycleScope.launch {
            try {
                Log.d("PRUEBA_GUARDAR", "Intentando insertar '$palabra' en la base de datos...")
                val nuevaPalabra = FavoriteWord(word = palabra)
                dbRoom.favoriteWordDao().addFavorite(nuevaPalabra)

                Log.d("PRUEBA_GUARDAR", "¡ÉXITO! '$palabra' insertada. Mostrando Toast.")
                runOnUiThread {
                    Toast.makeText(this@InitionActivity, "¡'$palabra' guardada!", Toast.LENGTH_SHORT).show()
                    etPalabraTemp.text.clear()
                }
            } catch (e: Exception) {
                Log.e("PRUEBA_GUARDAR", "¡¡ERROR AL GUARDAR LA PALABRA EN ROOM!!", e)
                runOnUiThread {
                    Toast.makeText(this@InitionActivity, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Tus Funciones de Firebase
    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()
        dbFirestore = FirebaseFirestore.getInstance()
    }

    override fun onStart() {
        super.onStart()
        checkUserSession()
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Usuario logueado: ${currentUser.uid}")
            cargarDatosUsuario(currentUser.uid)
        } else {
            Log.d(TAG, "Usuario no logueado, volviendo a Login.")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun cargarDatosUsuario(userId: String) {
        val userRef = dbFirestore.collection("users").document(userId)
        userRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombre") ?: "Estudiante"
                    Log.d(TAG, "Datos de usuario cargados: $nombre")
                    // Usa la string con formato para la traducción
                    tvBienvenida.text = getString(R.string.inicio_bienvenida_usuario, nombre)
                    tvSubtitulo.text = getString(R.string.inicio_listo_aprender)
                } else {
                    Log.w(TAG, "El documento del usuario no existe en Firestore.")
                    tvBienvenida.text = getString(R.string.inicio_hola_estudiante)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al cargar datos de Firestore", exception)
                Toast.makeText(this, "Error al cargar tus datos.", Toast.LENGTH_SHORT).show()
                tvBienvenida.text = getString(R.string.inicio_hola_estudiante)
            }
    }
}