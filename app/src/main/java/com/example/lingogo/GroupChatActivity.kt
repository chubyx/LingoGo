package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem // <-- ¡Importante!
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
    private lateinit var inputLayout: LinearLayout
    private lateinit var layoutJoinGroup: LinearLayout
    private lateinit var tvJoinGroupName: TextView
    private lateinit var btnJoinGroup: Button

    // Datos
    private lateinit var currentUserId: String
    private lateinit var currentUserName: String
    private lateinit var groupId: String
    private var isMember: Boolean = false
    private var groupCreatorId: String = ""

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
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Muestra la flecha
        supportActionBar?.title = groupName

        // 4. Enlazar Vistas de Chat
        rvMessages = findViewById(R.id.rvGroupChatMessages)
        etMessage = findViewById(R.id.etGroupChatMessage)
        btnSend = findViewById(R.id.btnGroupSendMessage)
        inputLayout = findViewById(R.id.inputLayout)

        // 5. Enlazar Vistas de "Unirse"
        layoutJoinGroup = findViewById(R.id.layoutJoinGroup)
        tvJoinGroupName = findViewById(R.id.tvJoinGroupName)
        btnJoinGroup = findViewById(R.id.btnJoinGroup)
        tvJoinGroupName.text = "Unirte a \"$groupName\""

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
                    groupCreatorId = groupDoc.getString("creadorId") ?: ""

                    val participants = groupDoc.get("participants") as? List<String>

                    if (participants != null && participants.contains(currentUserId)) {
                        isMember = true
                        mostrarChatUI(true)
                        setupRecyclerView()
                        loadMessages()
                    } else {
                        isMember = false
                        mostrarChatUI(false)
                    }
                } else {
                    Toast.makeText(this, "Este grupo ya no existe.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al verificar membresía", e)
                Toast.makeText(this, "Error al cargar grupo.", Toast.LENGTH_SHORT).show()
            }
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
        Toast.makeText(this, "Uniéndote al grupo...", Toast.LENGTH_SHORT).show()

        // --- ¡CAMBIO! ---
        // Al unirse, actualizamos 'participants' Y 'lastActivity'
        val updates = mapOf(
            "participants" to FieldValue.arrayUnion(currentUserId),
            "lastActivity" to FieldValue.serverTimestamp()
        )

        db.collection("group_rooms").document(groupId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "¡Usuario unido al grupo!")
                Toast.makeText(this, "¡Te has unido!", Toast.LENGTH_SHORT).show()

                isMember = true
                mostrarChatUI(true)
                setupRecyclerView()
                loadMessages()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al unirse al grupo", e)
                Toast.makeText(this, "Error al unirte: ${e.message}", Toast.LENGTH_SHORT).show()
                btnJoinGroup.isEnabled = true
            }
    }

    private fun setupRecyclerView() {
        messageAdapter = GroupMessageAdapter(
            messageList,
            currentUserId,
            groupCreatorId,
            { message ->
                showDeleteGroupMessageDialog(message)
            }
        )
        rvMessages.adapter = messageAdapter
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
    }

    private fun loadMessages() {
        db.collection("group_rooms").document(groupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { Log.w(TAG, "Error al escuchar mensajes de grupo", e); return@addSnapshotListener }
                if (snapshots != null) {
                    messageList.clear()
                    for (document in snapshots.documents) {
                        val message = document.toObject(GroupMessage::class.java)
                        if (message != null) {
                            message.id = document.id
                            messageList.add(message)
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                    rvMessages.scrollToPosition(messageList.size - 1)
                }
            }
    }

    private fun sendMessage() {
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
                // ¡CAMBIO! Ahora llamamos a la función genérica
                updateGroupLastActivity(text)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al enviar mensaje de grupo", e)
                etMessage.setText(text)
            }
    }

    // ¡CAMBIO! Esta función ahora actualiza CADA VEZ que se envía un mensaje
    private fun updateGroupLastActivity(lastMessage: String) {
        val roomData = hashMapOf<String, Any>(
            "lastActivity" to FieldValue.serverTimestamp(),
            "lastMessage" to lastMessage
        )
        // Usamos set con merge en lugar de update, por si acaso
        db.collection("group_rooms").document(groupId)
            .set(roomData, SetOptions.merge())
    }

    private fun showDeleteGroupMessageDialog(message: GroupMessage) {
        AlertDialog.Builder(this)
            .setTitle("Borrar Mensaje")
            .setMessage("¿Estás seguro de que quieres borrar este mensaje?")
            .setPositiveButton("Sí, borrar") { _, _ ->
                deleteGroupMessageFromFirestore(message)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteGroupMessageFromFirestore(message: GroupMessage) {
        db.collection("group_rooms").document(groupId)
            .collection("messages").document(message.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Mensaje de grupo borrado exitosamente")
                Toast.makeText(this, "Mensaje eliminado", Toast.LENGTH_SHORT).show()
                // --- ¡NUEVO! Actualizar el lastMessage después de borrar ---
                updateLastMessageAfterDelete_Group()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al borrar mensaje de grupo", e)
                Toast.makeText(this, "Error al borrar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- ¡NUEVA FUNCIÓN! ---
    /**
     * Busca el *nuevo* último mensaje y actualiza la sala de grupo.
     */
    private fun updateLastMessageAfterDelete_Group() {
        // 1. Buscar el nuevo último mensaje
        db.collection("group_rooms").document(groupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING) // El más nuevo primero
            .limit(1) // Solo queremos uno
            .get()
            .addOnSuccessListener { messageSnapshots ->
                val newLastMessageText: String
                val newLastActivity: Any

                if (messageSnapshots != null && !messageSnapshots.isEmpty) {
                    // 2a. Si quedan mensajes, usamos ese
                    val newLastMessage = messageSnapshots.documents[0].toObject(GroupMessage::class.java)
                    newLastMessageText = newLastMessage?.text ?: "..."
                    newLastActivity = newLastMessage?.timestamp ?: FieldValue.serverTimestamp()
                } else {
                    // 2b. Si no quedan mensajes, limpiamos el campo
                    newLastMessageText = "Grupo vacío"
                    newLastActivity = FieldValue.serverTimestamp()
                }

                // 3. Actualizar la sala de grupo
                val roomUpdates = hashMapOf<String, Any>(
                    "lastMessage" to newLastMessageText,
                    "lastActivity" to newLastActivity
                )

                db.collection("group_rooms").document(groupId)
                    .update(roomUpdates)
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error al actualizar lastMessage de grupo después de borrar", e)
                    }
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