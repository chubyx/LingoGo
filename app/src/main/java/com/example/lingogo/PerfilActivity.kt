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
    private lateinit var storage: FirebaseStorage
    private lateinit var currentUserId: String

    // Selector de Fotos
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d(TAG, "Foto seleccionada: $uri")
            uploadProfileImageToStorage(uri)
        } else {
            Log.d(TAG, "No se seleccionó ninguna foto.")
        }
    }

    // --- VISTAS (Declaración de variables) ---
    // Modo Lectura
    private lateinit var tvNombreUsuario: TextView
    private lateinit var tvEmailUsuario: TextView
    private lateinit var tvIdiomas: TextView
    private lateinit var tvBio: TextView
    private lateinit var imgPerfil: ImageView
    private lateinit var cardInfoDisplay: CardView
    private lateinit var btnEditar: Button
    private lateinit var btnCambiarFoto: Button

    // Modo Edición (Aquí es donde te faltaban variables)
    private lateinit var cardInfoEdit: CardView
    private lateinit var etNombreEditar: EditText
    private lateinit var etIdiomasEditar: EditText
    private lateinit var etBioEditar: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button

    // Otros
    private lateinit var btnCerrarSesion: Button
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        toolbar = findViewById(R.id.toolbarPerfil)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tu Perfil"

        // --- ENLAZAR VISTAS (findViewById) ---

        // Lectura
        tvNombreUsuario = findViewById(R.id.tvNombreUsuarioPerfil)
        tvEmailUsuario = findViewById(R.id.tvEmailUsuarioPerfil)
        tvIdiomas = findViewById(R.id.tvIdiomasPerfil)
        tvBio = findViewById(R.id.tvBioPerfil)
        imgPerfil = findViewById(R.id.imgPerfil)
        cardInfoDisplay = findViewById(R.id.cardInfoDisplay)
        btnEditar = findViewById(R.id.btnEditarPerfil)
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto)

        // Edición (Aquí se asignan los IDs del XML a las variables)
        cardInfoEdit = findViewById(R.id.cardInfoEdit)
        etNombreEditar = findViewById(R.id.etNombreEditar)
        etIdiomasEditar = findViewById(R.id.etIdiomasEditar)
        etBioEditar = findViewById(R.id.etBioEditar)

        // OJO: Aquí conectamos las variables con los IDs correctos
        btnGuardar = findViewById(R.id.btnGuardarCambios)
        btnCancelar = findViewById(R.id.btnCancelarEdicion)

        btnCerrarSesion = findViewById(R.id.btnCerrarSesionPerfil)

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

        btnCambiarFoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnCerrarSesion.setOnClickListener { cerrarSesionCompleta() }
    }

    private fun cargarDatosUsuario() {
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)

                    if (user != null) {
                        // Llenar Display
                        tvNombreUsuario.text = user.nombre
                        tvEmailUsuario.text = user.email
                        tvIdiomas.text = if (user.idiomas.isNotEmpty()) user.idiomas else "No especificado"
                        tvBio.text = if (user.descripcion.isNotEmpty()) user.descripcion else "Sin descripción"

                        // Cargar Foto
                        if (user.fotoUrl.isNotEmpty()) {
                            Glide.with(this).load(user.fotoUrl).circleCrop().into(imgPerfil)
                        } else {
                            imgPerfil.setImageResource(R.drawable.ic_perfil_por_defecto)
                        }

                        // Pre-llenar campos de Edición
                        etNombreEditar.setText(user.nombre)
                        etIdiomasEditar.setText(user.idiomas)
                        etBioEditar.setText(user.descripcion)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar datos", e)
            }
    }

    private fun uploadProfileImageToStorage(imageUri: Uri) {
        val storageRef = storage.reference.child("profile_images/$currentUserId.jpg")
        Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show()

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { url ->
                    saveImageUrlToFirestore(url.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al subir foto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        db.collection("users").document(currentUserId).update("fotoUrl", imageUrl)
            .addOnSuccessListener {
                Toast.makeText(this, "Foto actualizada", Toast.LENGTH_SHORT).show()
                Glide.with(this).load(imageUrl).circleCrop().into(imgPerfil)
            }
    }

    private fun toggleEditMode(enModoEdicion: Boolean) {
        if (enModoEdicion) {
            cardInfoDisplay.visibility = View.GONE
            cardInfoEdit.visibility = View.VISIBLE
            btnCambiarFoto.isEnabled = false
        } else {
            cardInfoDisplay.visibility = View.VISIBLE
            cardInfoEdit.visibility = View.GONE
            btnCambiarFoto.isEnabled = true
        }
    }

    private fun guardarCambiosUsuario() {
        val nuevoNombre = etNombreEditar.text.toString().trim()
        val nuevosIdiomas = etIdiomasEditar.text.toString().trim()
        val nuevaBio = etBioEditar.text.toString().trim()

        if (nuevoNombre.isEmpty()) {
            etNombreEditar.error = "El nombre no puede estar vacío"
            return
        }

        // Mapa con TODOS los campos a actualizar
        val updates = hashMapOf<String, Any>(
            "nombre" to nuevoNombre,
            "idiomas" to nuevosIdiomas,
            "descripcion" to nuevaBio
        )

        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

        db.collection("users").document(currentUserId).update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()

                // Actualizar la vista Display manualmente para que sea inmediato
                tvNombreUsuario.text = nuevoNombre
                tvIdiomas.text = if (nuevosIdiomas.isNotEmpty()) nuevosIdiomas else "No especificado"
                tvBio.text = if (nuevaBio.isNotEmpty()) nuevaBio else "Sin descripción"

                toggleEditMode(false)
                btnGuardar.isEnabled = true
                btnGuardar.text = "Guardar"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                btnGuardar.isEnabled = true
                btnGuardar.text = "Guardar"
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