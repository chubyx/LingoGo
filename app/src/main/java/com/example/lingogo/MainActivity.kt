package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.example.lingogo.database.SettingsDataStore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    // Vistas
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleSignIn: SignInButton
    private lateinit var tvIrRegistro: TextView
    private lateinit var btnChangeLanguage: ImageButton

    private lateinit var settingsDataStore: SettingsDataStore

    private val TAG = "MainActivity"

    // Lista de códigos de idioma (debe coincidir con R.array.idiomas)
    private val supportedLanguages = listOf("es", "en", "fr", "de", "pt", "ja")

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar
        settingsDataStore = SettingsDataStore(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configuración de Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Enlazar Vistas
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        tvIrRegistro = findViewById(R.id.tvIrRegistro)
        btnChangeLanguage = findViewById(R.id.btnChangeLanguage)

        // Listeners
        tvIrRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            loginConEmail()
        }

        btnGoogleSignIn.setOnClickListener {
            signInConGoogle()
        }

        // ¡CAMBIO! El listener ahora llama al diálogo
        btnChangeLanguage.setOnClickListener {
            showLanguagePicker()
        }
    }

    // ¡NUEVA FUNCIÓN! Muestra el diálogo de selección
    private fun showLanguagePicker() {
        // 1. Obtenemos los nombres de los idiomas (ej. "Español", "English")
        val languageNames = resources.getStringArray(R.array.idiomas)

        // 2. Lanzamos una corutina para ver cuál es el idioma actual
        lifecycleScope.launch {
            val currentLangCode = settingsDataStore.language.first()
            val currentIndex = supportedLanguages.indexOf(currentLangCode).coerceAtLeast(0)

            // 3. Creamos el diálogo
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(getString(R.string.language_picker_title))
                .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                    // 'which' es el índice (0=es, 1=en, 2=fr, etc.)
                    val selectedLangCode = supportedLanguages[which]

                    // Solo cambiamos si es un idioma diferente
                    if (currentLangCode != selectedLangCode) {
                        applyAndSaveLanguage(selectedLangCode)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    // Aplica y guarda el idioma seleccionado
    private fun applyAndSaveLanguage(langCode: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Guardando y aplicando nuevo idioma: $langCode")
                // 1. Guardar en DataStore
                settingsDataStore.setLanguage(langCode)

                // 2. Aplicar a la app (esto reiniciará la Activity)
                val appLocale = LocaleListCompat.forLanguageTags(langCode)
                AppCompatDelegate.setApplicationLocales(appLocale)

            } catch (e: Exception) {
                Log.e(TAG, "Error al aplicar idioma", e)
            }
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

    private fun signInConGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Falló el inicio con Google", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Inicio con Google cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    guardarUsuarioEnFirestore()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Error de autenticación: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun guardarUsuarioEnFirestore() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val nombre = user.displayName ?: "Usuario"
            val email = user.email ?: ""

            val userData = hashMapOf(
                "nombre" to nombre,
                "email" to email,
                "uid" to userId
            )

            db.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Datos de usuario (Google) guardados/actualizados en Firestore.")
                    navegarAlDashboard()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error al guardar datos de usuario (Google)", e)
                    navegarAlDashboard()
                }
        }
    }

    private fun navegarAlDashboard() {
        Toast.makeText(this, "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, InitionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Usuario ya logueado, navegando al Dashboard.")
            navegarAlDashboard()
        }
    }
}