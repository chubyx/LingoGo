package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilActivity : AppCompatActivity() {

    private val TAG = "PerfilActivity"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUserId: String

    // Vistas (Display)
    private lateinit var tvNombreUsuario: TextView
    private lateinit var tvEmailUsuario: TextView
    private lateinit var cardInfoDisplay: CardView
    private lateinit var btnEditar: Button

    // Vistas (Edición)
    private lateinit var etNombreEditar: EditText
    // private lateinit var etDescripcionEditar: EditText // (Para el futuro)
    private lateinit var cardInfoEdit: CardView
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button

    // Vistas (Generales)
    private lateinit var btnCerrarSesion: Button
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configurar Toolbar (la barra superior con la flecha de "atrás")
        toolbar = findViewById(R.id.toolbarPerfil)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tu Perfil"

        // Enlazar vistas de Display
        tvNombreUsuario = findViewById(R.id.tvNombreUsuarioPerfil)
        tvEmailUsuario = findViewById(R.id.tvEmailUsuarioPerfil)
        cardInfoDisplay = findViewById(R.id.cardInfoDisplay)
        btnEditar = findViewById(R.id.btnEditarPerfil)

        // Enlazar vistas de Edición
        etNombreEditar = findViewById(R.id.etNombreEditar)
        cardInfoEdit = findViewById(R.id.cardInfoEdit)
        btnGuardar = findViewById(R.id.btnGuardarCambios)
        btnCancelar = findViewById(R.id.btnCancelarEdicion)

        // Botón de Cerrar Sesión
        btnCerrarSesion = findViewById(R.id.btnCerrarSesionPerfil)

        setupListeners()
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
            cargarDatosUsuario()
        }
    }

    private fun setupListeners() {
        // Botón Editar: Muestra la tarjeta de edición
        btnEditar.setOnClickListener {
            toggleEditMode(true)
        }

        // Botón Cancelar: Oculta la tarjeta de edición
        btnCancelar.setOnClickListener {
            toggleEditMode(false)
        }

        // Botón Guardar: Llama a la función de guardar
        btnGuardar.setOnClickListener {
            guardarCambiosUsuario()
        }

        // Botón Cerrar Sesión
        btnCerrarSesion.setOnClickListener {
            cerrarSesionCompleta()
        }
    }

    /**
     * Carga los datos del usuario actual desde Firestore y los muestra en las vistas.
     */
    private fun cargarDatosUsuario() {
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombre") ?: "Sin nombre"
                    val email = document.getString("email") ?: "Sin email"

                    // Llenar vistas de Display
                    tvNombreUsuario.text = nombre
                    tvEmailUsuario.text = email

                    // Pre-llenar vistas de Edición (para que al editar ya esté el texto)
                    etNombreEditar.setText(nombre)

                } else {
                    Log.w(TAG, "No se encontró el documento del usuario")
                    Toast.makeText(this, "No se encontraron tus datos.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar datos", e)
                Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Muestra u oculta las tarjetas de edición/display.
     * @param enModoEdicion True para mostrar la tarjeta de edición, false para mostrar la de display.
     */
    private fun toggleEditMode(enModoEdicion: Boolean) {
        if (enModoEdicion) {
            cardInfoDisplay.visibility = View.GONE
            cardInfoEdit.visibility = View.VISIBLE
        } else {
            cardInfoDisplay.visibility = View.VISIBLE
            cardInfoEdit.visibility = View.GONE
        }
    }

    /**
     * Obtiene los datos de los EditText y los actualiza en Firestore.
     */
    private fun guardarCambiosUsuario() {
        val nuevoNombre = etNombreEditar.text.toString().trim()

        if (nuevoNombre.isEmpty()) {
            etNombreEditar.error = "El nombre no puede estar vacío"
            return
        }

        // Crear mapa solo con los campos a actualizar
        val updates = hashMapOf<String, Any>(
            "nombre" to nuevoNombre
            // "descripcion" to nuevaDescripcion // (Para el futuro)
        )

        // Actualizar el documento
        db.collection("users").document(currentUserId).update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Datos actualizados en Firestore")
                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()

                // Actualizar la vista de display con el nuevo nombre
                tvNombreUsuario.text = nuevoNombre

                // Salir del modo edición
                toggleEditMode(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar cambios", e)
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Cierra la sesión tanto en Firebase Auth como en Google Sign-In (muy importante).
     */
    private fun cerrarSesionCompleta() {
        // 1. Cerrar sesión de Firebase
        auth.signOut()

        // 2. Cerrar sesión de Google (si no, la próxima vez se logueará automáticamente)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d(TAG, "Sesión de Google cerrada")
            // 3. Enviar al Login
            irALogin()
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
            onBackPressedDispatcher.onBackPressed() // Simula el botón "atrás"
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
