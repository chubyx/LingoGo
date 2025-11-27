package com.example.lingogo

import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var userId: String = ""

    private lateinit var ivProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvLanguages: TextView
    private lateinit var tvBio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        db = FirebaseFirestore.getInstance()

        // 1. Recibir el ID del usuario que queremos ver
        userId = intent.getStringExtra("USER_ID") ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Enlazar Vistas
        val toolbar = findViewById<Toolbar>(R.id.toolbarProfile)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Perfil"

        ivProfile = findViewById(R.id.ivUserProfile)
        tvName = findViewById(R.id.tvUserName)
        tvEmail = findViewById(R.id.tvUserEmail)
        tvLanguages = findViewById(R.id.tvUserLanguages)
        tvBio = findViewById(R.id.tvUserBio)

        // 3. Cargar datos
        cargarDatosUsuario()
    }

    private fun cargarDatosUsuario() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)

                    if (user != null) {
                        tvName.text = user.nombre
                        tvEmail.text = user.email

                        // Mostrar Idiomas (o mensaje por defecto)
                        if (user.idiomas.isNotEmpty()) {
                            tvLanguages.text = user.idiomas
                        } else {
                            tvLanguages.text = "No especificado"
                        }

                        // Mostrar Bio
                        if (user.descripcion.isNotEmpty()) {
                            tvBio.text = user.descripcion
                        } else {
                            tvBio.text = "Sin descripción."
                        }

                        // Cargar Foto
                        if (user.fotoUrl.isNotEmpty()) {
                            Glide.with(this).load(user.fotoUrl).circleCrop().into(ivProfile)
                        } else {
                            ivProfile.setImageResource(R.drawable.ic_perfil_por_defecto)
                        }

                        supportActionBar?.title = user.nombre
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}