package com.example.lingogo

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class GroupChatActivity : AppCompatActivity() {

    private val TAG = "GroupChatActivity"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas
    private lateinit var toolbar: Toolbar
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    // Vistas para Unirse
    private lateinit var inputLayout: LinearLayout
    private lateinit var layoutJoinGroup: LinearLayout
    private lateinit var tvJoinGroupName: TextView
    private lateinit var btnJoinGroup: Button

    // Datos
    private lateinit var currentUserId: String
    private lateinit var currentUserName: String
    private lateinit var groupId: String
    private var isMember: Boolean = false

    // Variables de Admin
    private var groupCreatorId: String = ""
    private var myGroupMembers = mutableListOf<String>()

    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }
        currentUserId = currentUser.uid

        groupId = intent.getStringExtra("GROUP_ID") ?: ""
        val groupName = intent.getStringExtra("GROUP_NAME") ?: "Grupo"

        if (groupId.isEmpty()) {
            finish()
            return
        }

        // Enlazar Vistas
        toolbar = findViewById(R.id.toolbarGroupChat)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = groupName

        rvMessages = findViewById(R.id.rvGroupChatMessages)
        etMessage = findViewById(R.id.etGroupChatMessage)
        btnSend = findViewById(R.id.btnGroupSendMessage)

        inputLayout = findViewById(R.id.inputLayout)
        layoutJoinGroup = findViewById(R.id.layoutJoinGroup)
        tvJoinGroupName = findViewById(R.id.tvJoinGroupName)
        btnJoinGroup = findViewById(R.id.btnJoinGroup)

        tvJoinGroupName.text = "Unirte a \"$groupName\""

        loadCurrentUserNameAndCheckMembership()

        btnSend.setOnClickListener { sendMessage() }
        btnJoinGroup.setOnClickListener { unirseAlGrupo() }
    }

    private fun loadCurrentUserNameAndCheckMembership() {
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                currentUserName = userDoc.getString("nombre") ?: "Usuario"
            }

        db.collection("group_rooms").document(groupId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val group = snapshot.toObject(Group::class.java)
                    if (group != null) {
                        // --- CORRECCIÓN DE ADMIN (Truco de seguridad) ---
                        // Intentamos leer 'creatorId', si está vacío, probamos con 'creadorId'
                        var adminId = group.creatorId
                        if (adminId.isEmpty()) {
                            adminId = snapshot.getString("creadorId") ?: ""
                        }
                        groupCreatorId = adminId

                        // Depuración: Avisar si soy Admin
                        if (currentUserId == groupCreatorId) {
                            // Log.d(TAG, "¡Eres el ADMIN de este grupo!")
                        }

                        // Leemos miembros
                        val allMembers = mutableSetOf<String>()
                        allMembers.addAll(group.members)
                        allMembers.addAll(group.participants)
                        myGroupMembers = allMembers.toMutableList()

                        val amIMember = myGroupMembers.contains(currentUserId) || currentUserId == groupCreatorId

                        if (amIMember) {
                            if (!isMember) {
                                isMember = true
                                mostrarChatUI(true)
                                setupRecyclerView()
                                loadMessages()

                                if (currentUserId == groupCreatorId && !myGroupMembers.contains(currentUserId)) {
                                    unirseAlGrupoSilenciosamente()
                                }
                            }
                        } else {
                            if (isMember) {
                                Toast.makeText(this, "Has sido eliminado del grupo.", Toast.LENGTH_LONG).show()
                                finish()
                            } else {
                                isMember = false
                                mostrarChatUI(false)
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "El grupo ya no existe.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    private fun unirseAlGrupoSilenciosamente() {
        val updates = mapOf(
            "members" to FieldValue.arrayUnion(currentUserId),
            "participants" to FieldValue.arrayUnion(currentUserId)
        )
        db.collection("group_rooms").document(groupId).update(updates)
    }

    private fun mostrarChatUI(mostrarChat: Boolean) {
        if (mostrarChat) {
            rvMessages.visibility = View.VISIBLE
            inputLayout.visibility = View.VISIBLE
            layoutJoinGroup.visibility = View.GONE
        } else {
            rvMessages.visibility = View.GONE
            inputLayout.visibility = View.GONE
            layoutJoinGroup.visibility = View.VISIBLE
        }
    }

    private fun unirseAlGrupo() {
        btnJoinGroup.isEnabled = false
        Toast.makeText(this, "Uniéndote...", Toast.LENGTH_SHORT).show()

        val updates = mapOf(
            "members" to FieldValue.arrayUnion(currentUserId),
            "participants" to FieldValue.arrayUnion(currentUserId),
            "lastActivity" to FieldValue.serverTimestamp()
        )

        db.collection("group_rooms").document(groupId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Te has unido!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al unirse", Toast.LENGTH_SHORT).show()
                btnJoinGroup.isEnabled = true
            }
    }

    // --- LÓGICA DE ADMIN MEJORADA ---

    private fun mostrarIntegrantes() {
        if (myGroupMembers.isEmpty()) return

        val idsToFetch = myGroupMembers.take(10)
        val namesList = mutableListOf<String>()
        val idsList = mutableListOf<String>()

        db.collection("users").whereIn("uid", idsToFetch)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val name = doc.getString("nombre") ?: "Usuario"
                    val uid = doc.getString("uid") ?: ""

                    // Si es Admin, le ponemos una marquita visual
                    val displayName = if (uid == groupCreatorId) "$name (👑 Admin)" else name

                    namesList.add(displayName)
                    idsList.add(uid)
                }
                mostrarDialogoLista(namesList, idsList)
            }
    }

    private fun mostrarDialogoLista(names: List<String>, ids: List<String>) {
        val amIAdmin = (currentUserId == groupCreatorId)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Integrantes del Grupo")

        // Mostramos la lista simple
        builder.setItems(names.toTypedArray()) { _, which ->
            val selectedUserId = ids[which]
            val selectedUserName = names[which]

            // Si SOY ADMIN y toqué a OTRA persona, mostramos opciones
            if (amIAdmin && selectedUserId != currentUserId) {
                mostrarOpcionesDeAdmin(selectedUserId, selectedUserName)
            } else {
                // Si no soy admin, solo saludamos (o no hacemos nada)
                // Toast.makeText(this, selectedUserName, Toast.LENGTH_SHORT).show()
            }
        }
        builder.setPositiveButton("Cerrar", null)
        builder.show()
    }

    // --- NUEVO DIÁLOGO INTERMEDIO ---
    // Esto hace que sea obvio que puedes expulsar
    private fun mostrarOpcionesDeAdmin(userId: String, userName: String) {
        val opciones = arrayOf("Ver Perfil", "Expulsar del Grupo ❌")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Opciones para $userName")
        builder.setItems(opciones) { _, which ->
            when (which) {
                0 -> { // Ver Perfil (Opcional, si quieres implementarlo)
                    // val intent = Intent(this, UserProfileActivity::class.java)
                    // intent.putExtra("USER_ID", userId)
                    // startActivity(intent)
                }
                1 -> { // Expulsar
                    confirmarExpulsion(userId, userName)
                }
            }
        }
        builder.show()
    }

    private fun confirmarExpulsion(userIdToDelete: String, userName: String) {
        AlertDialog.Builder(this)
            .setTitle("Expulsar Miembro")
            .setMessage("¿Estás seguro de que quieres sacar a $userName?")
            .setPositiveButton("Sí, Sacar") { _, _ ->
                sacarUsuarioDelGrupo(userIdToDelete)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun sacarUsuarioDelGrupo(userIdToDelete: String) {
        val updates = mapOf(
            "members" to FieldValue.arrayRemove(userIdToDelete),
            "participants" to FieldValue.arrayRemove(userIdToDelete)
        )

        db.collection("group_rooms").document(groupId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Usuario expulsado.", Toast.LENGTH_SHORT).show()
                enviarMensajeSistema("El administrador eliminó a un miembro.")
            }
    }

    private fun enviarMensajeSistema(texto: String) {
        val messageMap = hashMapOf(
            "senderId" to "SYSTEM",
            "text" to texto,
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("group_rooms").document(groupId)
            .collection("messages").add(messageMap)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (isMember) {
            menu?.add(0, 1, 0, "Ver Integrantes")
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            mostrarIntegrantes()
            return true
        }
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // --- CHAT ---
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList, currentUserId) {}
        rvMessages.adapter = messageAdapter
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
    }

    private fun loadMessages() {
        db.collection("group_rooms").document(groupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (snapshots != null) {
                    messageList.clear()
                    for (doc in snapshots.documents) {
                        val msg = doc.toObject(Message::class.java)
                        if (msg != null) {
                            msg.id = doc.id
                            messageList.add(msg)
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                    rvMessages.scrollToPosition(messageList.size - 1)
                }
            }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        etMessage.setText("")

        val messageMap = hashMapOf(
            "senderId" to currentUserId,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("group_rooms").document(groupId)
            .collection("messages").add(messageMap)

        db.collection("group_rooms").document(groupId)
            .update(
                mapOf(
                    "lastActivity" to FieldValue.serverTimestamp(),
                    "lastMessage" to text
                )
            )
    }
}