package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // ¡NUEVO!
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommunityActivity : AppCompatActivity() {

    private val TAG = "CommunityActivity"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas (de tu nuevo XML)
    private lateinit var toolbar: Toolbar
    private lateinit var btnTabChats: Button
    private lateinit var btnTabGrupos: Button
    private lateinit var btnTabForo: Button
    private lateinit var rvPosts: RecyclerView
    private lateinit var fabCrearPost: FloatingActionButton

    // RecyclerView (para el Foro)
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()

    // --- ¡NUEVO! ---
    // Guardamos el ID del usuario actual aquí
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comunidad)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarCommunity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Comunidad LingoGo"

        // Enlazar Vistas
        btnTabChats = findViewById(R.id.btnTabChats)
        btnTabGrupos = findViewById(R.id.btnTabGrupos)
        btnTabForo = findViewById(R.id.btnTabForo)
        rvPosts = findViewById(R.id.rvPosts)
        fabCrearPost = findViewById(R.id.fabCrearPost)

        // Configurar RecyclerView (para el Foro)
        // NOTA: setupRecyclerView se llama ahora en onStart,
        // después de que tengamos el currentUserId

        // Configurar Listeners
        setupTabListeners()

        fabCrearPost.setOnClickListener {
            val user = auth.currentUser
            if (user == null) {
                Log.e(TAG, "No hay usuario logueado. Forzando login.")
                irALogin()
            } else {
                Log.d(TAG, "Usuario verificado (${user.uid}). Abriendo CrearPostActivity.")
                val intent = Intent(this, CrearPostActivity::class.java)
                intent.putExtra("USER_ID", user.uid)
                startActivity(intent)
            }
        }

        // Por defecto, mostramos el Foro
        mostrarPestañaForo()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            irALogin()
        } else {
            // --- ¡CAMBIO AQUÍ! ---
            // Guardamos el ID del usuario y *ahora* configuramos el RecyclerView,
            // porque el adaptador lo necesita.
            currentUserId = currentUser.uid
            setupRecyclerView() // Mover esto aquí
            cargarPostsForo()
        }
    }

    private fun setupRecyclerView() {
        // --- ¡CAMBIO AQUÍ! ---
        // Pasamos el ID del usuario y la nueva lambda de borrado
        postAdapter = PostAdapter(
            postList,
            currentUserId, // 1. Pasamos el ID del usuario
            { post -> // 2. Lambda para clic normal (abrir detalle)
                Log.d(TAG, "Clic en post: ${post.id}")
                val intent = Intent(this, DetallePostActivity::class.java)
                intent.putExtra("POST_ID", post.id)
                startActivity(intent)
            },
            { post -> // 3. ¡NUEVA! Lambda para clic en borrar
                showDeleteConfirmationDialog(post)
            }
        )
        rvPosts.adapter = postAdapter
        rvPosts.layoutManager = LinearLayoutManager(this)
    }

    // --- ¡NUEVA FUNCIÓN! ---
    /**
     * Muestra un diálogo de alerta para confirmar el borrado del post.
     */
    private fun showDeleteConfirmationDialog(post: Post) {
        AlertDialog.Builder(this)
            .setTitle("Borrar Post")
            .setMessage("¿Estás seguro de que quieres borrar este post? Esta acción no se puede deshacer.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Sí, borrar") { _, _ ->
                // El usuario confirmó, llamamos a la función de borrado
                deletePostFromFirestore(post)
            }
            .setNegativeButton("No, cancelar", null) // No hace nada al presionar "No"
            .show()
    }

    // --- ¡NUEVA FUNCIÓN! ---
    /**
     * Borra el documento del post de Firestore.
     */
    private fun deletePostFromFirestore(post: Post) {
        // (Nota: Las reglas de Firestore se aseguran de que solo el autor pueda hacer esto)
        db.collection("posts").document(post.id).delete()
            .addOnSuccessListener {
                Log.d(TAG, "Post borrado exitosamente")
                Toast.makeText(this, "Post eliminado", Toast.LENGTH_SHORT).show()
                // El SnapshotListener actualizará la lista automáticamente.
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al borrar post", e)
                Toast.makeText(this, "Error al borrar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupTabListeners() {
        // (Sin cambios aquí)
        btnTabChats.setOnClickListener {
            Toast.makeText(this, "Módulo de Chats (Aún no implementado)", Toast.LENGTH_SHORT).show()
        }
        btnTabGrupos.setOnClickListener {
            Toast.makeText(this, "Módulo de Grupos (Aún no implementado)", Toast.LENGTH_SHORT).show()
        }
        btnTabForo.setOnClickListener {
            mostrarPestañaForo()
        }
    }

    private fun mostrarPestañaForo() {
        // (Sin cambios aquí)
        Log.d(TAG, "Mostrando pestaña Foro")
        rvPosts.visibility = View.VISIBLE
        cargarPostsForo()
    }

    private fun cargarPostsForo() {
        // (Sin cambios aquí, excepto por el 'post.id' que ya teníamos)
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Error al escuchar posts", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    Log.d(TAG, "Posts del foro recibidos: ${snapshots.size()}");
                    postList.clear()
                    for (document in snapshots.documents) {
                        val post = document.toObject(Post::class.java)
                        if (post != null) {
                            post.id = document.id
                            postList.add(post)
                        }
                    }
                    postAdapter.notifyDataSetChanged()
                }
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