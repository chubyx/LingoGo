package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserListActivity : AppCompatActivity() {

    private val TAG = "UserListActivity"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas
    private lateinit var toolbar: Toolbar
    private lateinit var rvUsers: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>()

    // ¡CAMBIO! Ya no dependemos de auth.currentUser
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- ¡CAMBIO CLAVE! ---
        // Obtenemos el ID del usuario desde el Intent
        currentUserId = intent.getStringExtra("USER_ID") ?: ""

        // Verificar si el ID se pasó correctamente
        if (currentUserId.isEmpty()) {
            Log.e(TAG, "No se recibió USER_ID desde CommunityActivity. Cerrando.")
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            irALogin() // Ir a login si no hay ID
            return // Detener la ejecución
        }
        // --- FIN DEL CAMBIO ---


        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarUserList)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Buscar Usuarios"

        // Enlazar Vistas
        rvUsers = findViewById(R.id.rvUserList)

        setupRecyclerView()
        cargarUsuarios()
    }

    private fun setupRecyclerView() {
        // (Sin cambios aquí)
        userAdapter = UserAdapter(userList) { user ->
            Log.d(TAG, "Abriendo chat con: ${user.nombre}")
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("USER_ID_OTRO", user.uid)
            intent.putExtra("USER_NOMBRE_OTRO", user.nombre)
            startActivity(intent)
        }
        rvUsers.adapter = userAdapter
        rvUsers.layoutManager = LinearLayoutManager(this)
    }

    private fun cargarUsuarios() {
        // (Sin cambios aquí)
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots != null) {
                    userList.clear()
                    for (document in snapshots.documents) {
                        val user = document.toObject(User::class.java)
                        if (user != null) {
                            if (user.uid != currentUserId) {
                                userList.add(user)
                            }
                        }
                    }
                    userAdapter.notifyDataSetChanged()
                    Log.d(TAG, "Usuarios cargados: ${userList.size}")
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al cargar la lista de usuarios", e)
                Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
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