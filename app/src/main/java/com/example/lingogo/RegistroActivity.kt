package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistroActivity : AppCompatActivity() {

    // --- Declarar Firebase Auth y Firestore ---
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas (basadas en tu XML)
    private lateinit var etNombre: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var etDireccion: EditText
    private lateinit var spGenero: Spinner
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // --- ¡NUEVO! Inicializar Firebase ---
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Enlazar todas las vistas de tu XML
        etNombre = findViewById(R.id.etNombre)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etTelefono = findViewById(R.id.etTelefono)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        etDireccion = findViewById(R.id.etDireccion)
        spGenero = findViewById(R.id.spGenero) // (Asegúrate de poblar este Spinner con datos)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        tvLogin = findViewById(R.id.tvLogin)

        // Listener para el botón de Registrarse
        btnRegistrar.setOnClickListener {
            registerUser()
        }

        // Listener para el texto de "Ya tienes cuenta"
        tvLogin.setOnClickListener {
            // Volver a la pantalla de Login (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser() {
        // Obtener todos los datos de los campos
        val nombre = etNombre.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val fechaNacimiento = etFechaNacimiento.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()
        val genero = spGenero.selectedItem.toString() // (Asegúrate que el Spinner tenga datos)
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // --- Validaciones ---
        if (nombre.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Nombre, Usuario, Email y Contraseña son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        // --- 1. Crear el usuario en Firebase Authentication ---
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("RegistroActivity", "createUserWithEmail:success")
                    val userId = auth.currentUser?.uid ?: ""

                    if (userId.isEmpty()) {
                        Toast.makeText(this, "Error al obtener ID de usuario.", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    // --- 2. Guardar la información extra en Firestore ---

                    // Crear un mapa (diccionario) con los datos del usuario
                    val userData = hashMapOf(
                        "nombre" to nombre,
                        "username" to username,
                        "email" to email,
                        "telefono" to telefono,
                        "fechaNacimiento" to fechaNacimiento,
                        "direccion" to direccion,
                        "genero" to genero,
                        "uid" to userId,
                        "fechaRegistro" to System.currentTimeMillis() // Guardar la fecha de registro
                    )

                    // Guardar en la colección "users" con el ID del usuario
                    db.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d("RegistroActivity", "Datos guardados en Firestore exitosamente")
                            Toast.makeText(this, "Cuenta creada y datos guardados.", Toast.LENGTH_SHORT).show()

                            // 3. Navegar al Dashboard
                            navigateToDashboard()
                        }
                        .addOnFailureListener { e ->
                            Log.w("RegistroActivity", "Error al guardar datos en Firestore", e)
                            Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                } else {
                    // Si el registro falla (ej. email ya existe)
                    Log.w("RegistroActivity", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Cierra esta actividad (Registro)
    }
}
