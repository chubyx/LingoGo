package com.example.lingogo

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class CommunityActivity : AppCompatActivity() {

    private val TAG = "CommunityActivity"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentUserId: String = ""

    // Vistas
    private lateinit var toolbar: Toolbar
    private lateinit var btnTabChats: MaterialButton
    private lateinit var btnTabGrupos: MaterialButton
    private lateinit var btnTabForo: MaterialButton
    private lateinit var fabCrearPost: FloatingActionButton

    // RecyclerViews
    private lateinit var rvPosts: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()

    private lateinit var rvChatRooms: RecyclerView
    private lateinit var chatRoomAdapter: ChatRoomAdapter
    private val chatRoomList = mutableListOf<ChatRoom>()

    private lateinit var rvGroupRooms: RecyclerView
    private lateinit var groupAdapter: GroupAdapter
    private val groupList = mutableListOf<Group>()

    // Listeners
    private var postsListener: ListenerRegistration? = null
    private var groupsListener: ListenerRegistration? = null
    private var chatRoomsListener: ListenerRegistration? = null

    // Pestaña actual
    private var currentTabId: Int = R.id.btnTabForo

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comunidad)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        toolbar = findViewById(R.id.toolbarCommunity)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.inicio_comunidad)

        btnTabChats = findViewById(R.id.btnTabChats)
        btnTabGrupos = findViewById(R.id.btnTabGrupos)
        btnTabForo = findViewById(R.id.btnTabForo)
        fabCrearPost = findViewById(R.id.fabCrearPost)

        rvPosts = findViewById(R.id.rvPosts)
        rvChatRooms = findViewById(R.id.rvChatRooms)
        rvGroupRooms = findViewById(R.id.rvGroupRooms)

        setupTabListeners()

        fabCrearPost.setOnClickListener {
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

    private fun getThemeColor(@AttrRes attrResId: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrResId, typedValue, true)
        return typedValue.data
    }

    private fun updateTabsColors(activeBtn: MaterialButton) {
        val colorPrimary = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val colorOnPrimary = getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)

        val buttons = listOf(btnTabChats, btnTabGrupos, btnTabForo)

        for (btn in buttons) {
            if (btn == activeBtn) {
                btn.backgroundTintList = ColorStateList.valueOf(colorPrimary)
                btn.setTextColor(colorOnPrimary)
                btn.strokeWidth = 0
            } else {
                btn.backgroundTintList = ColorStateList.valueOf(colorSurface)
                btn.setTextColor(colorPrimary)
                btn.setStrokeColor(ColorStateList.valueOf(colorPrimary))
                btn.strokeWidth = 3
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.community_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_buscar_usuarios -> {
                val userId = auth.currentUser?.uid
                if (userId.isNullOrEmpty()) irALogin()
                else {
                    val intent = Intent(this, UserListActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                }
                true
            }
            R.id.menu_crear_grupo -> {
                val userId = auth.currentUser?.uid
                if (userId.isNullOrEmpty()) irALogin()
                else startActivity(Intent(this, CreateGroupActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            irALogin()
        } else {
            currentUserId = currentUser.uid
            setupPostRecyclerView()
            setupChatRoomRecyclerView()
            setupGroupRecyclerView()

            when (currentTabId) {
                R.id.btnTabChats -> mostrarPestañaChats()
                R.id.btnTabGrupos -> mostrarPestañaGrupos()
                else -> mostrarPestañaForo()
            }
        }
    }

    private fun setupPostRecyclerView() {
        postAdapter = PostAdapter(
            postList,
            currentUserId,
            { post ->
                val intent = Intent(this, DetallePostActivity::class.java)
                intent.putExtra("POST_ID", post.id)
                startActivity(intent)
            },
            { post -> showDeleteConfirmationDialog(post) }
        )
        rvPosts.adapter = postAdapter
        rvPosts.layoutManager = LinearLayoutManager(this)
    }

    private fun setupChatRoomRecyclerView() {
        chatRoomAdapter = ChatRoomAdapter(chatRoomList) { chatRoom ->
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
            .setMessage("¿Estás seguro de que quieres borrar este post?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Sí, borrar") { _, _ -> deletePostFromFirestore(post) }
            .setNegativeButton("No, cancelar", null)
            .show()
    }

    private fun deletePostFromFirestore(post: Post) {
        db.collection("posts").document(post.id).delete()
            .addOnSuccessListener { Toast.makeText(this, "Post eliminado", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun setupTabListeners() {
        btnTabChats.setOnClickListener { mostrarPestañaChats() }
        btnTabGrupos.setOnClickListener { mostrarPestañaGrupos() }
        btnTabForo.setOnClickListener { mostrarPestañaForo() }
    }

    private fun mostrarPestañaForo() {
        Log.d(TAG, "Mostrando pestaña Foro")
        rvChatRooms.visibility = View.GONE
        rvGroupRooms.visibility = View.GONE
        rvPosts.visibility = View.VISIBLE
        fabCrearPost.visibility = View.VISIBLE

        chatRoomsListener?.remove()
        groupsListener?.remove()

        currentTabId = R.id.btnTabForo
        updateTabsColors(btnTabForo)
        cargarPostsForo()
    }

    private fun mostrarPestañaChats() {
        Log.d(TAG, "Mostrando pestaña Chats")
        rvPosts.visibility = View.GONE
        rvGroupRooms.visibility = View.GONE
        rvChatRooms.visibility = View.VISIBLE
        fabCrearPost.visibility = View.GONE

        postsListener?.remove()
        groupsListener?.remove()

        currentTabId = R.id.btnTabChats
        updateTabsColors(btnTabChats)
        cargarChatRooms()
    }

    private fun mostrarPestañaGrupos() {
        Log.d(TAG, "Mostrando pestaña Grupos")
        rvPosts.visibility = View.GONE
        rvChatRooms.visibility = View.GONE
        rvGroupRooms.visibility = View.VISIBLE
        fabCrearPost.visibility = View.GONE

        postsListener?.remove()
        chatRoomsListener?.remove()

        currentTabId = R.id.btnTabGrupos
        updateTabsColors(btnTabGrupos)
        cargarGrupos()
    }

    private fun cargarPostsForo() {
        if (currentUserId.isEmpty()) return
        postsListener = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { Log.w(TAG, "Error posts", e); return@addSnapshotListener }
                if (snapshots != null) {
                    postList.clear()
                    for (doc in snapshots.documents) {
                        val post = doc.toObject(Post::class.java)
                        if (post != null) {
                            post.id = doc.id
                            postList.add(post)
                        }
                    }
                    postAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun cargarGrupos() {
        if (currentUserId.isEmpty()) return
        groupsListener = db.collection("group_rooms")
            .orderBy("lastActivity", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { Log.w(TAG, "Error grupos", e); return@addSnapshotListener }
                if (snapshots != null) {
                    groupList.clear()
                    for (doc in snapshots.documents) {
                        val group = doc.toObject(Group::class.java)
                        if (group != null) {
                            group.id = doc.id
                            groupList.add(group)
                        }
                    }
                    groupAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun cargarChatRooms() {
        if (currentUserId.isEmpty()) return

        chatRoomsListener = db.collection("chat_rooms")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val tempChatList = mutableListOf<ChatRoom>()
                    var usersProcessed = 0
                    val totalDocs = snapshots.size()

                    if (totalDocs == 0) {
                        chatRoomList.clear()
                        chatRoomAdapter.notifyDataSetChanged()
                        return@addSnapshotListener
                    }

                    for (document in snapshots.documents) {
                        val participants = document.get("participants") as? List<String>
                        if (participants == null || participants.size < 2) {
                            usersProcessed++
                            continue
                        }

                        val otherUserId = participants.find { it != currentUserId } ?: continue

                        // --- LECTURA SEGURA DEL MAPA ---
                        // Leemos como Map<String, Any> para evitar errores de casteo
                        val rawUnread = document.get("unreadCounts") as? Map<String, Any> ?: emptyMap()
                        // Convertimos los valores a Long de forma segura
                        val unreadCountsMap = rawUnread.mapValues {
                            (it.value as? Number)?.toLong() ?: 0L
                        }

                        db.collection("users").document(otherUserId).get()
                            .addOnSuccessListener { userDoc ->
                                val chatRoom = ChatRoom(
                                    id = document.id,
                                    otherUserId = otherUserId,
                                    lastActivity = document.getTimestamp("lastActivity")?.toDate(),
                                    lastMessage = document.getString("lastMessage") ?: "...",
                                    unreadCounts = unreadCountsMap
                                )

                                if (userDoc != null && userDoc.exists()) {
                                    chatRoom.otherUserName = userDoc.getString("nombre") ?: "Usuario"
                                    chatRoom.otherUserPhotoUrl = userDoc.getString("fotoUrl") ?: ""
                                } else {
                                    chatRoom.otherUserName = "Usuario Desconocido"
                                }

                                tempChatList.add(chatRoom)
                                usersProcessed++

                                if (usersProcessed == totalDocs) {
                                    chatRoomList.clear()
                                    chatRoomList.addAll(tempChatList)
                                    chatRoomList.sortByDescending { it.lastActivity }
                                    chatRoomAdapter.notifyDataSetChanged()
                                }
                            }
                    }
                }
            }
    }

    private fun irALogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}