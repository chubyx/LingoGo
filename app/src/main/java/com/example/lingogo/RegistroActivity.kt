package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter // ¡Importante!
import android.widget.AutoCompleteTextView // ¡Importante!
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistroActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas
    private lateinit var etNombre: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var etDireccion: EditText
    private lateinit var spGenero: AutoCompleteTextView // ¡CAMBIO!
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Enlazar vistas
        etNombre = findViewById(R.id.etNombre)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etTelefono = findViewById(R.id.etTelefono)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        etDireccion = findViewById(R.id.etDireccion)
        spGenero = findViewById(R.id.spGenero) // ¡CAMBIO!
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        tvLogin = findViewById(R.id.tvLogin)

        // --- ¡NUEVO! Poblar el Spinner de Género ---
        val generos = resources.getStringArray(R.array.generos)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, generos)
        spGenero.setAdapter(adapter)

        // Listeners
        btnRegistrar.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser() {
        // Obtener datos
        val nombre = etNombre.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val fechaNacimiento = etFechaNacimiento.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()
        val genero = spGenero.text.toString() // ¡CAMBIO!
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Validaciones (usando las nuevas strings)
        if (nombre.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.registro_error_obligatorios), Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, getString(R.string.registro_error_contrasena_corta), Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.registro_error_contrasenas_no_coinciden), Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Crear usuario en Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("RegistroActivity", "createUserWithEmail:success")
                    val userId = auth.currentUser?.uid ?: ""

                    if (userId.isEmpty()) {
                        Toast.makeText(this, getString(R.string.registro_error_id), Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    // 2. Guardar datos en Firestore
                    val userData = hashMapOf(
                        "nombre" to nombre,
                        "username" to username,
                        "email" to email,
                        "telefono" to telefono,
                        "fechaNacimiento" to fechaNacimiento,
                        "direccion" to direccion,
                        "genero" to genero,
                        "uid" to userId,
                        "fechaRegistro" to System.currentTimeMillis()
                    )

                    db.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d("RegistroActivity", "Datos guardados en Firestore exitosamente")
                            Toast.makeText(this, getString(R.string.registro_exito), Toast.LENGTH_SHORT).show()
                            navigateToDashboard()
                        }
                        .addOnFailureListener { e ->
                            Log.w("RegistroActivity", "Error al guardar datos en Firestore", e)
                            Toast.makeText(this, getString(R.string.registro_error_firestore, e.message), Toast.LENGTH_LONG).show()
                        }

                } else {
                    Log.w("RegistroActivity", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this, getString(R.string.registro_error_auth, task.exception?.message), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}