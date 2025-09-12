package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var inputLayout: LinearLayout // <-- Layout del input

    // --- ¡NUEVO! Vistas para "Unirse" ---
    private lateinit var layoutJoinGroup: LinearLayout
    private lateinit var tvJoinGroupName: TextView
    private lateinit var btnJoinGroup: Button

    // Datos
    private lateinit var currentUserId: String
    private lateinit var currentUserName: String
    private lateinit var groupId: String
    private var isMember: Boolean = false // <-- Para saber si ya es miembro

    private lateinit var messageAdapter: GroupMessageAdapter
    private val messageList = mutableListOf<GroupMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1. Obtener IDs
        val currentUser = auth.currentUser
        if (currentUser == null) {
            irALogin()
            return
        }
        currentUserId = currentUser.uid

        groupId = intent.getStringExtra("GROUP_ID") ?: ""
        val groupName = intent.getStringExtra("GROUP_NAME") ?: "Grupo"

        if (groupId.isEmpty()) {
            Log.e(TAG, "No se recibió GROUP_ID. Cerrando.")
            finish()
            return
        }

        // 3. Configurar Toolbar
        toolbar = findViewById(R.id.toolbarGroupChat)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = groupName

        // 4. Enlazar Vistas de Chat
        rvMessages = findViewById(R.id.rvGroupChatMessages)
        etMessage = findViewById(R.id.etGroupChatMessage)
        btnSend = findViewById(R.id.btnGroupSendMessage)
        inputLayout = findViewById(R.id.inputLayout) // <-- Layout del input

        // 5. Enlazar Vistas de "Unirse"
        // (Aquí es donde te da el error, pero los IDs SÍ están en el XML)
        layoutJoinGroup = findViewById(R.id.layoutJoinGroup)
        tvJoinGroupName = findViewById(R.id.tvJoinGroupName)
        btnJoinGroup = findViewById(R.id.btnJoinGroup)

        tvJoinGroupName.text = "Unirte a \"$groupName\""

        // 6. Configurar RecyclerView
        setupRecyclerView()

        // 7. Cargar nombre del usuario y revisar membresía
        loadCurrentUserNameAndCheckMembership()

        // 8. Listeners
        btnSend.setOnClickListener {
            sendMessage()
        }
        btnJoinGroup.setOnClickListener {
            unirseAlGrupo()
        }
    }

    /**
     * Carga el nombre del usuario actual Y comprueba si es miembro de este grupo.
     */
    private fun loadCurrentUserNameAndCheckMembership() {
        // Obtenemos el nombre del usuario (lo necesitamos para chatear)
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                currentUserName = userDoc.getString("nombre") ?: "Usuario Anónimo"
                btnSend.isEnabled = true
            }
            .addOnFailureListener {
                currentUserName = "Usuario Anónimo"
                btnSend.isEnabled = true
            }

        // Comprobamos si el usuario es miembro de este grupo
        db.collection("group_rooms").document(groupId).get()
            .addOnSuccessListener { groupDoc ->
                if (groupDoc != null && groupDoc.exists()) {
                    val participants = groupDoc.get("participants") as? List<String>

                    if (participants != null && participants.contains(currentUserId)) {
                        // ¡YA ES MIEMBRO!
                        isMember = true
                        mostrarChatUI(true)
                        loadMessages() // Cargar mensajes solo si es miembro
                    } else {
                        // NO ES MIEMBRO
                        isMember = false
                        mostrarChatUI(false) // Ocultar chat, mostrar botón "Unirse"
                    }
                } else {
                    Log.w(TAG, "El grupo no existe.")
                    Toast.makeText(this, "Este grupo ya no existe.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al verificar membresía", e)
                Toast.makeText(this, "Error al cargar grupo.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Muestra/oculta la UI de chat o la UI de "Unirse"
     */
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

    /**
     * ¡NUEVA FUNCIÓN! Añade al usuario actual a la lista de "participants" del grupo.
     */
    private fun unirseAlGrupo() {
        btnJoinGroup.isEnabled = false
        Toast.makeText(this, "Uniéndote al grupo...", Toast.LENGTH_SHORT).show()

        // (Las Reglas de Firestore se aseguran de que tengamos permiso para esto)
        db.collection("group_rooms").document(groupId)
            .update("participants", FieldValue.arrayUnion(currentUserId))
            .addOnSuccessListener {
                Log.d(TAG, "¡Usuario unido al grupo!")
                Toast.makeText(this, "¡Te has unido!", Toast.LENGTH_SHORT).show()

                // Ahora que es miembro, mostramos el chat
                isMember = true
                mostrarChatUI(true)
                loadMessages()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al unirse al grupo", e)
                Toast.makeText(this, "Error al unirte: ${e.message}", Toast.LENGTH_SHORT).show()
                btnJoinGroup.isEnabled = true
            }
    }

    private fun setupRecyclerView() {
        // (Sin cambios)
        messageAdapter = GroupMessageAdapter(messageList, currentUserId)
        rvMessages.adapter = messageAdapter
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
    }

    private fun loadMessages() {
        // (Sin cambios)
        db.collection("group_rooms").document(groupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { Log.w(TAG, "Error al escuchar mensajes de grupo", e); return@addSnapshotListener }
                if (snapshots != null) {
                    messageList.clear()
                    for (document in snapshots.documents) {
                        val message = document.toObject(GroupMessage::class.java)
                        if (message != null) { messageList.add(message) }
                    }
                    messageAdapter.notifyDataSetChanged()
                    rvMessages.scrollToPosition(messageList.size - 1)
                }
            }
    }

    private fun sendMessage() {
        // (Sin cambios)
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) { return }

        val messageMap = hashMapOf(
            "senderId" to currentUserId,
            "senderName" to currentUserName,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )
        etMessage.setText("")

        db.collection("group_rooms").document(groupId)
            .collection("messages")
            .add(messageMap)
            .addOnSuccessListener {
                Log.d(TAG, "Mensaje de grupo enviado!")
                updateGroupLastActivity(text)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al enviar mensaje de grupo", e)
                Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show()
                etMessage.setText(text)
            }
    }

    private fun updateGroupLastActivity(lastMessage: String) {
        // (Sin cambios)
        val roomData = hashMapOf<String, Any>(
            "lastActivity" to FieldValue.serverTimestamp(),
            "lastMessage" to lastMessage
        )
        db.collection("group_rooms").document(groupId)
            .set(roomData, SetOptions.merge())
    }

    private fun irALogin() {
        // (Sin cambios)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // (Sin cambios)
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}