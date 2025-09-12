package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// --- ¡NUEVO! Imports para colores ---
import androidx.core.content.ContextCompat
import android.content.res.ColorStateList
import android.graphics.Color
// --- Fin de Imports ---
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommunityActivity : AppCompatActivity() {

    private val TAG = "CommunityActivity"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentUserId: String = ""

    // Vistas (de tu nuevo XML)
    private lateinit var toolbar: Toolbar
    private lateinit var btnTabChats: Button
    private lateinit var btnTabGrupos: Button
    private lateinit var btnTabForo: Button
    private lateinit var fabCrearPost: FloatingActionButton

    // RecyclerView (para el Foro)
    private lateinit var rvPosts: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()

    // RecyclerView (para los Chats)
    private lateinit var rvChatRooms: RecyclerView
    private lateinit var chatRoomAdapter: ChatRoomAdapter
    private val chatRoomList = mutableListOf<ChatRoom>()

    // RecyclerView (para los Grupos)
    private lateinit var rvGroupRooms: RecyclerView
    private lateinit var groupAdapter: GroupAdapter
    private val groupList = mutableListOf<Group>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comunidad)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarCommunity)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Comunidad LingoGo"

        // Enlazar Vistas
        btnTabChats = findViewById(R.id.btnTabChats)
        btnTabGrupos = findViewById(R.id.btnTabGrupos)
        btnTabForo = findViewById(R.id.btnTabForo)
        fabCrearPost = findViewById(R.id.fabCrearPost)

        rvPosts = findViewById(R.id.rvPosts) // Para el Foro
        rvChatRooms = findViewById(R.id.rvChatRooms) // Para los Chats
        rvGroupRooms = findViewById(R.id.rvGroupRooms) // ¡NUEVO!

        setupTabListeners()

        fabCrearPost.setOnClickListener {
            // Verificamos el ID aquí para evitar la "condición de carrera"
            val userId = auth.currentUser?.uid
            if (userId.isNullOrEmpty()) {
                irALogin()
            } else {
                val intent = Intent(this, CrearPostActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Infla el menú (añade ítems a la barra de acción)
        menuInflater.inflate(R.menu.community_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Maneja los clics en los ítems del menú
        return when (item.itemId) {
            R.id.menu_buscar_usuarios -> {
                val userId = auth.currentUser?.uid
                if (userId.isNullOrEmpty()) {
                    irALogin()
                } else {
                    val intent = Intent(this, UserListActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                }
                true
            }
            R.id.menu_crear_grupo -> {
                val userId = auth.currentUser?.uid
                if (userId.isNullOrEmpty()) {
                    irALogin()
                } else {
                    val intent = Intent(this, CreateGroupActivity::class.java)
                    startActivity(intent)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        // Es importante verificar el usuario aquí,
        // ya que esta pantalla depende de quién esté logueado.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            irALogin()
        } else {
            currentUserId = currentUser.uid

            // Configurar AMBOS RecyclerViews
            setupPostRecyclerView()
            setupChatRoomRecyclerView()
            setupGroupRecyclerView() // ¡NUEVO!

            // Cargar los datos del Foro (la pestaña por defecto)
            mostrarPestañaForo()
        }
    }

    private fun setupPostRecyclerView() {
        postAdapter = PostAdapter(
            postList,
            currentUserId,
            { post ->
                // Acción de Clic en un Post: Abrir Detalle
                val intent = Intent(this, DetallePostActivity::class.java)
                intent.putExtra("POST_ID", post.id)
                startActivity(intent)
            },
            { post ->
                // Acción de Clic en Borrar Post
                showDeleteConfirmationDialog(post)
            }
        )
        rvPosts.adapter = postAdapter
        rvPosts.layoutManager = LinearLayoutManager(this)
    }

    private fun setupChatRoomRecyclerView() {
        chatRoomAdapter = ChatRoomAdapter(chatRoomList) { chatRoom ->
            // Al hacer clic en una sala, abrir ChatActivity
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("USER_ID_OTRO", chatRoom.otherUserId)
            intent.putExtra("USER_NOMBRE_OTRO", chatRoom.otherUserName)
            startActivity(intent)
        }
        rvChatRooms.adapter = chatRoomAdapter
        rvChatRooms.layoutManager = LinearLayoutManager(this)
    }

    private fun setupGroupRecyclerView() {
        groupAdapter = GroupAdapter(groupList) { group ->
            // Al hacer clic en un grupo, abrir GroupChatActivity
            val intent = Intent(this, GroupChatActivity::class.java)
            intent.putExtra("GROUP_ID", group.id)
            intent.putExtra("GROUP_NAME", group.nombre)
            startActivity(intent)
        }
        rvGroupRooms.adapter = groupAdapter
        rvGroupRooms.layoutManager = LinearLayoutManager(this)
    }

    private fun showDeleteConfirmationDialog(post: Post) {
        AlertDialog.Builder(this)
            .setTitle("Borrar Post")
            .setMessage("¿Estás seguro de que quieres borrar este post? Esta acción no se puede deshacer.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Sí, borrar") { _, _ ->
                deletePostFromFirestore(post)
            }
            .setNegativeButton("No, cancelar", null)
            .show()
    }

    private fun deletePostFromFirestore(post: Post) {
        db.collection("posts").document(post.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Post eliminado", Toast.LENGTH_SHORT).show()
                // El SnapshotListener actualizará la lista automáticamente.
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al borrar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupTabListeners() {
        btnTabChats.setOnClickListener {
            mostrarPestañaChats()
        }
        btnTabGrupos.setOnClickListener {
            mostrarPestañaGrupos()
        }
        btnTabForo.setOnClickListener {
            mostrarPestañaForo()
        }
    }

    // --- ¡FUNCIÓN MODIFICADA! ---
    private fun mostrarPestañaForo() {
        Log.d(TAG, "Mostrando pestaña Foro")
        rvChatRooms.visibility = View.GONE // Ocultar chats
        rvGroupRooms.visibility = View.GONE // Ocultar grupos
        rvPosts.visibility = View.VISIBLE // Mostrar posts
        fabCrearPost.visibility = View.VISIBLE // Mostrar botón de crear POST

        // --- ¡NUEVO! Cambiar colores de pestañas ---
        // (Color #FAD169 parseado)
        val colorClaro = ColorStateList.valueOf(Color.parseColor("#FAD169"))
        val colorNaranja = ContextCompat.getColorStateList(this, R.color.naranja)
        val colorTextoNaranja = ContextCompat.getColor(this, R.color.naranja)

        // Pestaña Foro (Seleccionada)
        btnTabForo.backgroundTintList = colorNaranja
        btnTabForo.setTextColor(Color.WHITE)

        // Pestaña Chats (No seleccionada)
        btnTabChats.backgroundTintList = colorClaro
        btnTabChats.setTextColor(colorTextoNaranja)

        // Pestaña Grupos (No seleccionada)
        btnTabGrupos.backgroundTintList = colorClaro
        btnTabGrupos.setTextColor(colorTextoNaranja)
        // --- FIN DEL CAMBIO ---

        cargarPostsForo()
    }

    // --- ¡FUNCIÓN MODIFICADA! ---
    private fun mostrarPestañaChats() {
        Log.d(TAG, "Mostrando pestaña Chats")
        rvPosts.visibility = View.GONE // Ocultar posts
        rvGroupRooms.visibility = View.GONE // Ocultar grupos
        rvChatRooms.visibility = View.VISIBLE // Mostrar chats
        fabCrearPost.visibility = View.GONE // Ocultar botón de crear post

        // --- ¡NUEVO! Cambiar colores de pestañas ---
        val colorClaro = ColorStateList.valueOf(Color.parseColor("#FAD169"))
        val colorNaranja = ContextCompat.getColorStateList(this, R.color.naranja)
        val colorTextoNaranja = ContextCompat.getColor(this, R.color.naranja)

        // Pestaña Chats (Seleccionada)
        btnTabChats.backgroundTintList = colorNaranja
        btnTabChats.setTextColor(Color.WHITE)

        // Pestaña Foro (No seleccionada)
        btnTabForo.backgroundTintList = colorClaro
        btnTabForo.setTextColor(colorTextoNaranja)

        // Pestaña Grupos (No seleccionada)
        btnTabGrupos.backgroundTintList = colorClaro
        btnTabGrupos.setTextColor(colorTextoNaranja)
        // --- FIN DEL CAMBIO ---

        cargarChatRooms()
    }

    // --- ¡FUNCIÓN MODIFICADA! ---
    private fun mostrarPestañaGrupos() {
        Log.d(TAG, "Mostrando pestaña Grupos")
        rvPosts.visibility = View.GONE // Ocultar posts
        rvChatRooms.visibility = View.GONE // Ocultar chats
        rvGroupRooms.visibility = View.VISIBLE // Mostrar grupos
        fabCrearPost.visibility = View.GONE // Ocultar botón de crear post

        // --- ¡NUEVO! Cambiar colores de pestañas ---
        val colorClaro = ColorStateList.valueOf(Color.parseColor("#FAD169"))
        val colorNaranja = ContextCompat.getColorStateList(this, R.color.naranja)
        val colorTextoNaranja = ContextCompat.getColor(this, R.color.naranja)

        // Pestaña Grupos (Seleccionada)
        btnTabGrupos.backgroundTintList = colorNaranja
        btnTabGrupos.setTextColor(Color.WHITE)

        // Pestaña Foro (No seleccionada)
        btnTabForo.backgroundTintList = colorClaro
        btnTabForo.setTextColor(colorTextoNaranja)

        // Pestaña Chats (No seleccionada)
        btnTabChats.backgroundTintList = colorClaro
        btnTabChats.setTextColor(colorTextoNaranja)
        // --- FIN DEL CAMBIO ---

        cargarGrupos()
    }

    private fun cargarPostsForo() {
        if (currentUserId.isEmpty()) return
        // Escucha en tiempo real (addSnapshotListener)
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { Log.w(TAG, "Error al escuchar posts", e); return@addSnapshotListener }
                if (snapshots != null) {
                    Log.d(TAG, "Posts del foro recibidos: ${snapshots.size()}");
                    postList.clear()
                    for (document in snapshots.documents) {
                        val post = document.toObject(Post::class.java)
                        if (post != null) {
                            post.id = document.id // Asignamos el ID del documento
                            postList.add(post)
                        }
                    }
                    postAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun cargarGrupos() {
        if (currentUserId.isEmpty()) return

        // Cargamos TODOS los grupos
        db.collection("group_rooms")
            .orderBy("lastActivity", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Error al escuchar grupos", e)
                    // (Si falla por índice, ver Logcat y crear el índice)
                    Toast.makeText(this, "Error al cargar grupos", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    Log.d(TAG, "Grupos encontrados: ${snapshots.size()}");
                    groupList.clear()
                    for (document in snapshots.documents) {
                        val group = document.toObject(Group::class.java)
                        if (group != null) {
                            group.id = document.id
                            groupList.add(group)
                        }
                    }
                    groupAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun cargarChatRooms() {
        if (currentUserId.isEmpty()) return

        // 1. Consultar las salas de chat donde soy participante
        db.collection("chat_rooms")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots == null) {
                    Log.d(TAG, "No se encontraron salas de chat."); return@addOnSuccessListener
                }

                chatRoomList.clear()
                Log.d(TAG, "Salas de chat encontradas: ${snapshots.size()}");

                // 2. Por cada sala, buscar los datos del OTRO usuario
                for (document in snapshots.documents) {
                    val participants = document.get("participants") as? List<String>
                    if (participants == null || participants.size < 2) continue

                    // Encuentra el ID del otro usuario
                    val otherUserId = participants.find { it != currentUserId }
                    if (otherUserId == null) continue

                    // 3. Buscar el perfil del otro usuario en la colección "users"
                    db.collection("users").document(otherUserId).get()
                        .addOnSuccessListener { userDoc ->

                            // 4. Construir el objeto ChatRoom con los datos
                            val chatRoom = ChatRoom(
                                id = document.id,
                                otherUserId = otherUserId,
                                lastActivity = document.getTimestamp("lastActivity")?.toDate(),
                                lastMessage = document.getString("lastMessage") ?: "..."
                            )

                            if (userDoc != null && userDoc.exists()) {
                                chatRoom.otherUserName = userDoc.getString("nombre") ?: "Usuario"
                                chatRoom.otherUserPhotoUrl = userDoc.getString("fotoUrl") ?: ""
                            } else {
                                chatRoom.otherUserName = "Usuario Desconocido"
                            }

                            chatRoomList.add(chatRoom)
                            // (Ordenar manualmente)
                            chatRoomList.sortByDescending { it.lastActivity }
                            chatRoomAdapter.notifyDataSetChanged()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar salas de chat", e)
                Toast.makeText(this, "Error al cargar chats", Toast.LENGTH_SHORT).show()
            }
    }

    private fun irALogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}