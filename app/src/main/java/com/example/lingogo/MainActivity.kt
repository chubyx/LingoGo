package com.example.lingogo


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions // ¡Importante!

class MainActivity : AppCompatActivity() {

    // --- Declarar Firebase Auth y Firestore ---
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore // <-- ¡NUEVO!

    // --- Declarar Google Sign In Client ---
    private lateinit var googleSignInClient: GoogleSignInClient

    // Vistas (basadas en tu activity_login.xml)
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleSignIn: SignInButton // Botón de Google
    private lateinit var tvIrRegistro: TextView

    // Constante para el Log
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Asegúrate que este sea tu XML de Login

        // --- 1. Inicializar Firebase Auth y Firestore ---
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance() // <-- ¡NUEVO!

        // --- 2. Configurar Google Sign-In ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // --- 3. Enlazar Vistas ---
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn) // ID de tu XML
        tvIrRegistro = findViewById(R.id.tvIrRegistro)

        // --- 4. Listeners de los Botones ---

        // Listener para ir a Registro
        tvIrRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        // Listener para Login con Email
        btnLogin.setOnClickListener {
            loginConEmail()
        }

        // Listener para el botón de Google
        btnGoogleSignIn.setOnClickListener {
            signInConGoogle()
        }
    }

    private fun loginConEmail() {
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email y contraseña no pueden estar vacíos", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    navegarAlDashboard()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Fallo en la autenticación.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // --- 5. Iniciar el proceso de Google Sign-In ---
    private fun signInConGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        // Usamos el nuevo Activity Result Launcher
        googleSignInLauncher.launch(signInIntent)
    }

    // --- 6. Registrar el "launcher" para obtener el resultado de Google ---
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Google Sign-In fue exitoso, ahora autenticamos con Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign-In falló
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Falló el inicio con Google", Toast.LENGTH_SHORT).show()
            }
        } else {
            // El usuario cerró la ventana de Google
            Toast.makeText(this, "Inicio con Google cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 7. Autenticar la cuenta de Google con Firebase ---
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in exitoso
                    Log.d(TAG, "signInWithCredential:success")

                    // --- ¡NUEVO! Guardar/Actualizar datos en Firestore ---
                    guardarUsuarioEnFirestore()

                } else {
                    // Error
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Error de autenticación: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // --- ¡NUEVA FUNCIÓN! ---
    private fun guardarUsuarioEnFirestore() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val nombre = user.displayName ?: "Usuario"
            val email = user.email ?: ""

            // Creamos un mapa solo con los datos básicos
            val userData = hashMapOf(
                "nombre" to nombre,
                "email" to email,
                "uid" to userId

            )


            db.collection("users").document(userId)
                .set(userData, SetOptions.merge()) // <-- SetOptions.merge() es clave
                .addOnSuccessListener {
                    Log.d(TAG, "Datos de usuario (Google) guardados/actualizados en Firestore.")
                    // Ahora que los datos están guardados, navegamos
                    navegarAlDashboard()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error al guardar datos de usuario (Google)", e)
                    // Navegamos igualmente, aunque no se hayan guardado los datos
                    navegarAlDashboard()
                }
        }
    }


    // --- 8. Navegar al Dashboard/Inicio ---
    private fun navegarAlDashboard() {


        Toast.makeText(this, "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, InitionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Cierra la actividad de Login
    }

    override fun onStart() {
        super.onStart()
        // Chequear si el usuario ya inició sesión
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // (Opcional) Si ya está logueado, mandarlo directo al dashboard
            Log.d(TAG, "Usuario ya logueado, navegando al Dashboard.")
            navegarAlDashboard()
        }
    }
}

