package com.example.lingogo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class PerfilActivity : AppCompatActivity() {

    private val TAG = "PerfilActivity"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage // ¡NUEVO!
    private lateinit var currentUserId: String

    // --- ¡NUEVO! Launcher para el Selector de Fotos ---
    // Este es el nuevo método para seleccionar fotos sin pedir permisos
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d(TAG, "Foto seleccionada: $uri")
            // La foto fue seleccionada, ahora la subimos
            uploadProfileImageToStorage(uri)
        } else {
            Log.d(TAG, "No se seleccionó ninguna foto.")
        }
    }

    // Vistas
    private lateinit var tvNombreUsuario: TextView
    private lateinit var tvEmailUsuario: TextView
    private lateinit var cardInfoDisplay: CardView
    private lateinit var btnEditar: Button
    private lateinit var etNombreEditar: EditText
    private lateinit var cardInfoEdit: CardView
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var toolbar: Toolbar
    private lateinit var imgPerfil: ImageView // ¡NUEVO!
    private lateinit var btnCambiarFoto: Button // ¡NUEVO!

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance() // ¡NUEVO!

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarPerfil)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tu Perfil"

        // Enlazar vistas (Display)
        tvNombreUsuario = findViewById(R.id.tvNombreUsuarioPerfil)
        tvEmailUsuario = findViewById(R.id.tvEmailUsuarioPerfil)
        cardInfoDisplay = findViewById(R.id.cardInfoDisplay)
        btnEditar = findViewById(R.id.btnEditarPerfil)

        // Enlazar vistas (Edición)
        etNombreEditar = findViewById(R.id.etNombreEditar)
        cardInfoEdit = findViewById(R.id.cardInfoEdit)
        btnGuardar = findViewById(R.id.btnGuardarCambios)
        btnCancelar = findViewById(R.id.btnCancelarEdicion)

        // Vistas de Foto y Sesión
        btnCerrarSesion = findViewById(R.id.btnCerrarSesionPerfil)
        imgPerfil = findViewById(R.id.imgPerfil) // ¡NUEVO!
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto) // ¡NUEVO!

        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            irALogin()
        } else {
            currentUserId = currentUser.uid
            cargarDatosUsuario()
        }
    }

    private fun setupListeners() {
        btnEditar.setOnClickListener { toggleEditMode(true) }
        btnCancelar.setOnClickListener { toggleEditMode(false) }
        btnGuardar.setOnClickListener { guardarCambiosUsuario() }
        btnCerrarSesion.setOnClickListener { cerrarSesionCompleta() }

        // --- ¡NUEVO! Listener para cambiar foto ---
        btnCambiarFoto.setOnClickListener {
            Log.d(TAG, "Botón 'Cambiar foto' presionado.")
            // Inicia el selector de fotos moderno
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    /**
     * Carga los datos (nombre, email y foto) desde Firestore.
     */
    private fun cargarDatosUsuario() {
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombre") ?: "Sin nombre"
                    val email = document.getString("email") ?: "Sin email"
                    val fotoUrl = document.getString("fotoUrl") // ¡NUEVO!

                    // Llenar vistas de Display
                    tvNombreUsuario.text = nombre
                    tvEmailUsuario.text = email

                    // --- ¡NUEVO! Cargar la foto usando Glide ---
                    if (fotoUrl != null && fotoUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(fotoUrl)
                            .circleCrop() // Opcional: para hacer la foto redonda
                            .into(imgPerfil)
                    } else {
                        // Si no tiene foto, mostrar un ícono por defecto
                        imgPerfil.setImageResource(R.drawable.ic_perfil_por_defecto) // (Necesitas crear este drawable)
                    }

                    // Pre-llenar vistas de Edición
                    etNombreEditar.setText(nombre)

                } else {
                    Log.w(TAG, "No se encontró el documento del usuario")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar datos", e)
            }
    }

    /**
     * Sube la imagen seleccionada a Firebase Storage.
     */
    private fun uploadProfileImageToStorage(imageUri: Uri) {
        // 1. Crear la referencia en Storage
        // Guardaremos la foto en "profile_images/USER_ID.jpg"
        val storageRef = storage.reference.child("profile_images/$currentUserId.jpg")

        Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show()

        // 2. Subir el archivo
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                Log.d(TAG, "Foto subida exitosamente a Storage.")
                // 3. Obtener la URL de descarga
                storageRef.downloadUrl.addOnSuccessListener { url ->
                    Log.d(TAG, "URL de descarga obtenida: $url")
                    // 4. Guardar la URL en Firestore
                    saveImageUrlToFirestore(url.toString())
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al subir foto a Storage", e)
                Toast.makeText(this, "Error al subir la foto", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Guarda la URL de la foto en el documento del usuario en Firestore.
     */
    private fun saveImageUrlToFirestore(imageUrl: String) {
        val updates = hashMapOf<String, Any>(
            "fotoUrl" to imageUrl
        )

        db.collection("users").document(currentUserId).update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "URL de la foto guardada en Firestore.")
                Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                // Volver a cargar la imagen en la UI con Glide
                Glide.with(this).load(imageUrl).circleCrop().into(imgPerfil)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar URL en Firestore", e)
                Toast.makeText(this, "Error al guardar la foto", Toast.LENGTH_SHORT).show()
            }
    }


    private fun toggleEditMode(enModoEdicion: Boolean) {
        if (enModoEdicion) {
            cardInfoDisplay.visibility = View.GONE
            cardInfoEdit.visibility = View.VISIBLE
        } else {
            cardInfoDisplay.visibility = View.VISIBLE
            cardInfoEdit.visibility = View.GONE
        }
    }

    private fun guardarCambiosUsuario() {
        val nuevoNombre = etNombreEditar.text.toString().trim()
        if (nuevoNombre.isEmpty()) {
            etNombreEditar.error = "El nombre no puede estar vacío"
            return
        }
        val updates = hashMapOf<String, Any>("nombre" to nuevoNombre)

        db.collection("users").document(currentUserId).update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Datos actualizados en Firestore")
                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                tvNombreUsuario.text = nuevoNombre
                toggleEditMode(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar cambios", e)
            }
    }

    private fun cerrarSesionCompleta() {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d(TAG, "Sesión de Google cerrada")
            irALogin()
        }
    }

    private fun irALogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}