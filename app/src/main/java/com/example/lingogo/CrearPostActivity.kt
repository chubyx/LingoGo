package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

class CrearPostActivity : AppCompatActivity() {

    private val TAG = "CrearPostActivity"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas
    private lateinit var toolbar: Toolbar
    private lateinit var etPostTexto: EditText
    private lateinit var btnPublicar: Button

    // Datos del usuario (para guardarlos en el post)
    private var autorNombre: String = "Usuario"
    private lateinit var autorId: String // Se llenará desde el Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_post)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- ¡CAMBIO CLAVE AQUÍ! ---
        // Obtenemos el ID del usuario desde el Intent que nos mandó CommunityActivity
        autorId = intent.getStringExtra("USER_ID") ?: "" // Se añade un valor por defecto

        // Si por alguna razón no recibimos el ID, cerramos esta pantalla.
        if (autorId.isEmpty()) { // Comprobamos si está vacío
            Log.e(TAG, "No se recibió USER_ID desde CommunityActivity. Cerrando.")
            Toast.makeText(this, "Error de sesión. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
            finish() // Cierra esta actividad
            return // Detiene la ejecución de onCreate
        }
        // --- FIN DEL CAMBIO ---

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarCrearPost)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Crear nueva publicación"

        // Enlazar Vistas
        etPostTexto = findViewById(R.id.etPostTexto)
        btnPublicar = findViewById(R.id.btnPublicarPost)

        // Configurar Listener
        btnPublicar.setOnClickListener {
            publicarPost()
        }

        // --- ¡CAMBIO! ---
        // Movemos esto aquí desde onStart()
        // Ahora que tenemos el autorId, cargamos su nombre
        cargarNombreUsuario()
    }

    override fun onStart() {
        super.onStart()
        // --- ¡CAMBIO! ---
        // Eliminamos la comprobación de auth.currentUser de aquí
        // para evitar la "condición de carrera".
        // La lógica se movió a onCreate().
    }

    /**
     * Busca el nombre del usuario en la colección "users" para guardarlo en el post.
     */
    private fun cargarNombreUsuario() {
        // Esta función ahora usa el autorId que recibimos del Intent.
        db.collection("users").document(autorId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    autorNombre = document.getString("nombre") ?: "Usuario Anónimo"
                    // Habilitar el botón de publicar solo cuando tenemos el nombre
                    btnPublicar.isEnabled = true
                } else {
                    Log.w(TAG, "No se encontró el documento del usuario, usando nombre por defecto")
                    autorNombre = "Usuario Anónimo"
                    btnPublicar.isEnabled = true // Habilitar igualmente
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar nombre de usuario", e)
                Toast.makeText(this, "Error al cargar tu nombre", Toast.LENGTH_SHORT).show()
                // Habilitar igualmente para que pueda postear
                btnPublicar.isEnabled = true
            }
    }

    /**
     * Toma el texto del EditText y lo sube a Firestore.
     */
    private fun publicarPost() {
        val textoPost = etPostTexto.text.toString().trim()

        if (textoPost.isEmpty()) {
            etPostTexto.error = "No puedes publicar un post vacío"
            return
        }

        // Deshabilitar el botón para evitar posts duplicados
        btnPublicar.isEnabled = false
        Toast.makeText(this, "Publicando...", Toast.LENGTH_SHORT).show()

        // Crear el mapa de datos para el nuevo Post
        val postMap = hashMapOf(
            "autorId" to autorId,
            "autorNombre" to autorNombre,
            "texto" to textoPost,
            "timestamp" to FieldValue.serverTimestamp() // Firestore pondrá la hora del servidor
        )

        // Añadir el documento a la colección "posts"
        db.collection("posts")
            .add(postMap)
            .addOnSuccessListener {
                Log.d(TAG, "Post publicado exitosamente!")
                Toast.makeText(this, "Publicado", Toast.LENGTH_SHORT).show()
                // Cerrar esta actividad y volver al foro
                finish()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al publicar post", e)
                Toast.makeText(this, "Error al publicar: ${e.message}", Toast.LENGTH_SHORT).show()
                // Volver a habilitar el botón si falla
                btnPublicar.isEnabled = true
            }
    }

    private fun irALogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Maneja el clic en la flecha "atrás" de la Toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}