package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // ¡NUEVO!
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class DetallePostActivity : AppCompatActivity() {

    private val TAG = "DetallePostActivity"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas
    private lateinit var toolbar: Toolbar
    private lateinit var tvPostAutor: TextView
    private lateinit var tvPostTimestamp: TextView
    private lateinit var tvPostTexto: TextView
    private lateinit var rvComentarios: RecyclerView
    private lateinit var etComentario: EditText
    private lateinit var btnEnviarComentario: Button

    // Datos
    private lateinit var postId: String
    private lateinit var autorIdActual: String
    private var autorNombreActual: String = "Usuario"
    private lateinit var comentarioAdapter: ComentarioAdapter
    private val comentarioList = mutableListOf<Comentario>()

    private val dateFormatter = SimpleDateFormat("dd/MM/yy 'a las' HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_post)

        postId = intent.getStringExtra("POST_ID") ?: ""
        if (postId.isEmpty()) {
            Log.e(TAG, "No se recibió POST_ID. Cerrando actividad.")
            Toast.makeText(this, "Error al cargar el post.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            irALogin()
            return
        }
        autorIdActual = currentUser.uid

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarDetallePost)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detalle del Post"

        // Enlazar Vistas
        tvPostAutor = findViewById(R.id.tvDetallePostAutor)
        tvPostTimestamp = findViewById(R.id.tvDetallePostTimestamp)
        tvPostTexto = findViewById(R.id.tvDetallePostTexto)
        rvComentarios = findViewById(R.id.rvComentarios)
        etComentario = findViewById(R.id.etEscribirComentario)
        btnEnviarComentario = findViewById(R.id.btnEnviarComentario)

        // --- ¡CAMBIO AQUÍ! ---
        // SetupRecyclerView ahora se llama aquí, ya que autorIdActual está listo
        setupRecyclerView()

        // Cargar datos
        cargarNombreUsuarioActual()
        cargarPostOriginal()
        cargarComentarios()

        // Listeners
        btnEnviarComentario.setOnClickListener {
            enviarComentario()
        }
    }

    private fun cargarNombreUsuarioActual() {
        // (Sin cambios aquí)
        db.collection("users").document(autorIdActual).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    autorNombreActual = document.getString("nombre") ?: "Usuario Anónimo"
                } else {
                    autorNombreActual = "Usuario Anónimo"
                }
                btnEnviarComentario.isEnabled = true
            }
            .addOnFailureListener {
                autorNombreActual = "Usuario Anónimo"
                btnEnviarComentario.isEnabled = true
            }
    }

    private fun cargarPostOriginal() {
        // (Sin cambios aquí)
        db.collection("posts").document(postId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val post = document.toObject(Post::class.java)
                    if (post != null) {
                        tvPostAutor.text = post.autorNombre
                        tvPostTexto.text = post.texto
                        if (post.timestamp != null) {
                            tvPostTimestamp.text = dateFormatter.format(post.timestamp)
                        } else {
                            tvPostTimestamp.text = ""
                        }
                    }
                } else {
                    Log.w(TAG, "No se encontró el post con ID: $postId")
                    Toast.makeText(this, "No se encontró el post.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar el post", e)
                Toast.makeText(this, "Error al cargar el post.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Configura el RecyclerView para la lista de comentarios.
     */
    private fun setupRecyclerView() {
        // --- ¡CAMBIO AQUÍ! ---
        // Pasamos el ID del usuario y la nueva lambda de borrado
        comentarioAdapter = ComentarioAdapter(
            comentarioList,
            autorIdActual, // 1. Pasamos el ID del usuario
            { comentario -> // 2. ¡NUEVA! Lambda para clic en borrar
                showDeleteCommentDialog(comentario)
            }
        )
        rvComentarios.adapter = comentarioAdapter
        rvComentarios.layoutManager = LinearLayoutManager(this)
    }

    /**
     * Escucha en tiempo real los comentarios de la subcolección "comentarios".
     */
    private fun cargarComentarios() {
        db.collection("posts").document(postId).collection("comentarios")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Error al escuchar comentarios", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    comentarioList.clear()
                    for (document in snapshots.documents) {
                        val comentario = document.toObject(Comentario::class.java)
                        if (comentario != null) {
                            // --- ¡CAMBIO AQUÍ! ---
                            // Guardamos el ID del documento en el objeto Comentario
                            comentario.id = document.id
                            comentarioList.add(comentario)
                        }
                    }
                    comentarioAdapter.notifyDataSetChanged()
                    if (comentarioList.isNotEmpty()) {
                        rvComentarios.scrollToPosition(comentarioList.size - 1)
                    }
                }
            }
    }

    /**
     * Guarda un nuevo comentario en la subcolección de este post.
     */
    private fun enviarComentario() {
        // (Sin cambios aquí)
        val textoComentario = etComentario.text.toString().trim()
        if (textoComentario.isEmpty()) {
            return
        }
        btnEnviarComentario.isEnabled = false

        val comentarioMap = hashMapOf(
            "autorId" to autorIdActual,
            "autorNombre" to autorNombreActual,
            "texto" to textoComentario,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("posts").document(postId).collection("comentarios")
            .add(comentarioMap)
            .addOnSuccessListener {
                Log.d(TAG, "Comentario enviado!")
                etComentario.setText("")
                btnEnviarComentario.isEnabled = true
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al enviar comentario", e)
                Toast.makeText(this, "Error al enviar comentario", Toast.LENGTH_SHORT).show()
                btnEnviarComentario.isEnabled = true
            }
    }

    // --- ¡NUEVA FUNCIÓN! ---
    /**
     * Muestra un diálogo de alerta para confirmar el borrado del comentario.
     */
    private fun showDeleteCommentDialog(comentario: Comentario) {
        AlertDialog.Builder(this)
            .setTitle("Borrar Comentario")
            .setMessage("¿Estás seguro de que quieres borrar este comentario?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Sí, borrar") { _, _ ->
                deleteCommentFromFirestore(comentario)
            }
            .setNegativeButton("No", null)
            .show()
    }

    // --- ¡NUEVA FUNCIÓN! ---
    /**
     * Borra el documento del comentario de Firestore.
     */
    private fun deleteCommentFromFirestore(comentario: Comentario) {
        // (Nota: Las reglas de Firestore se aseguran de que solo el autor pueda hacer esto)
        db.collection("posts").document(postId)
            .collection("comentarios").document(comentario.id) // Usamos el ID del comentario
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Comentario borrado exitosamente")
                Toast.makeText(this, "Comentario eliminado", Toast.LENGTH_SHORT).show()
                // El SnapshotListener actualizará la lista automáticamente.
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al borrar comentario", e)
                Toast.makeText(this, "Error al borrar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun irALogin() {
        // (Sin cambios aquí)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // (Sin cambios aquí)
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}