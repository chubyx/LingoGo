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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CreateGroupActivity : AppCompatActivity() {

    private val TAG = "CreateGroupActivity"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas
    private lateinit var toolbar: Toolbar
    private lateinit var etGroupName: EditText
    private lateinit var btnCreateGroup: Button

    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Verificar usuario
        val currentUser = auth.currentUser
        if (currentUser == null) {
            irALogin()
            return
        }
        currentUserId = currentUser.uid

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarCreateGroup)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Crear Nuevo Grupo"

        // Enlazar Vistas
        etGroupName = findViewById(R.id.etGroupName)
        btnCreateGroup = findViewById(R.id.btnCreateGroup)

        // Listener
        btnCreateGroup.setOnClickListener {
            crearGrupo()
        }
    }

    private fun crearGrupo() {
        val groupName = etGroupName.text.toString().trim()
        if (groupName.isEmpty()) {
            etGroupName.error = "El nombre no puede estar vacío"
            return
        }

        btnCreateGroup.isEnabled = false
        Toast.makeText(this, "Creando grupo...", Toast.LENGTH_SHORT).show()

        // Datos del nuevo grupo
        val groupData = hashMapOf(
            "nombre" to groupName,
            "creadorId" to currentUserId,
            "participants" to listOf(currentUserId), // El creador es el primer participante
            "lastActivity" to FieldValue.serverTimestamp(),
            "lastMessage" to "Grupo creado."
        )

        // Crear el documento en la colección "group_rooms"
        db.collection("group_rooms")
            .add(groupData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Grupo creado con ID: ${documentReference.id}")
                Toast.makeText(this, "¡Grupo '$groupName' creado!", Toast.LENGTH_SHORT).show()

                // (Opcional) Abrir el chat del grupo recién creado
                val intent = Intent(this, GroupChatActivity::class.java)
                intent.putExtra("GROUP_ID", documentReference.id)
                intent.putExtra("GROUP_NAME", groupName)
                startActivity(intent)

                finish() // Cierra esta actividad
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al crear grupo", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnCreateGroup.isEnabled = true
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