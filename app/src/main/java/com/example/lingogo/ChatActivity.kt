package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem // <-- ¡Importante!
import android.widget.Button
import android.widget.EditText
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

class ChatActivity : AppCompatActivity() {

    private val TAG = "ChatActivity"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Vistas
    private lateinit var toolbar: Toolbar
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    // Datos
    private lateinit var currentUserId: String
    private lateinit var otroUserId: String
    private lateinit var chatRoomId: String

    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1. Obtener IDs
        val currentUser = auth.currentUser
        if (currentUser == null) {
            irALogin()
            return
        }
        currentUserId = currentUser.uid

        otroUserId = intent.getStringExtra("USER_ID_OTRO") ?: ""
        val otroUserNombre = intent.getStringExtra("USER_NOMBRE_OTRO") ?: "Chat"

        if (otroUserId.isEmpty()) {
            Log.e(TAG, "No se recibió USER_ID_OTRO. Cerrando.")
            finish()
            return
        }

        // 2. Determinar el ID de la Sala de Chat
        chatRoomId = getChatRoomId(currentUserId, otroUserId)
        Log.d(TAG, "ID de la sala de chat: $chatRoomId")

        // 3. Configurar Toolbar
        toolbar = findViewById(R.id.toolbarChat)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Muestra la flecha
        supportActionBar?.title = otroUserNombre

        // 4. Enlazar Vistas
        rvMessages = findViewById(R.id.rvChatMessages)
        etMessage = findViewById(R.id.etChatMessage)
        btnSend = findViewById(R.id.btnSendMessage)

        // 5. Configurar RecyclerView
        setupRecyclerView()

        // 6. Cargar Mensajes
        loadMessages()

        // 7. Listener del botón Enviar
        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun getChatRoomId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList, currentUserId) { message ->
            // Acción de Clic en Borrar Mensaje
            showDeleteMessageDialog(message)
        }
        rvMessages.adapter = messageAdapter
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
    }

    private fun loadMessages() {
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { Log.w(TAG, "Error al escuchar mensajes", e); return@addSnapshotListener }

                if (snapshots != null) {
                    messageList.clear()
                    for (document in snapshots.documents) {
                        val message = document.toObject(Message::class.java)
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

        btnSend.isEnabled = false
        etMessage.setText("")

        // 1. Asegurar que la sala exista Y ACTUALIZAR el lastMessage
        ensureChatRoomExists(text) {
            // 2. Enviar el mensaje
            val messageMap = hashMapOf(
                "senderId" to currentUserId,
                "text" to text,
                "timestamp" to FieldValue.serverTimestamp()
            )
            db.collection("chat_rooms").document(chatRoomId)
                .collection("messages")
                .add(messageMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Mensaje enviado!")
                    btnSend.isEnabled = true
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error al enviar mensaje", e)
                    Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show()
                    etMessage.setText(text)
                    btnSend.isEnabled = true
                }
        }
    }

    // ¡CAMBIO! Esta función ahora actualiza el lastMessage CADA VEZ que envías uno
    private fun ensureChatRoomExists(lastMessage: String, onComplete: () -> Unit) {
        val roomData = hashMapOf<String, Any>(
            "lastActivity" to FieldValue.serverTimestamp(),
            "participants" to listOf(currentUserId, otroUserId),
            "lastMessage" to lastMessage // Actualizamos el lastMessage aquí
        )
        db.collection("chat_rooms").document(chatRoomId)
            .set(roomData, SetOptions.merge()) // Usamos merge para crear o actualizar
            .addOnSuccessListener {
                Log.d(TAG, "Sala de chat asegurada/creada.")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al asegurar la sala de chat", e)
                Toast.makeText(this, "Error al crear la sala", Toast.LENGTH_SHORT).show()
                btnSend.isEnabled = true
            }
    }

    private fun showDeleteMessageDialog(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Borrar Mensaje")
            .setMessage("¿Estás seguro de que quieres borrar este mensaje?")
            .setPositiveButton("Sí, borrar") { _, _ ->
                deleteMessageFromFirestore(message)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteMessageFromFirestore(message: Message) {
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages").document(message.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Mensaje borrado exitosamente")
                Toast.makeText(this, "Mensaje eliminado", Toast.LENGTH_SHORT).show()
                // --- ¡NUEVO! Actualizar el lastMessage después de borrar ---
                updateLastMessageAfterDelete()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al borrar mensaje", e)
                Toast.makeText(this, "Error al borrar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- ¡NUEVA FUNCIÓN! ---
    /**
     * Busca el *nuevo* último mensaje y actualiza la sala de chat.
     */
    private fun updateLastMessageAfterDelete() {
        // 1. Buscar el nuevo último mensaje (el más reciente)
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING) // El más nuevo primero
            .limit(1) // Solo queremos uno
            .get()
            .addOnSuccessListener { messageSnapshots ->
                val newLastMessageText: String
                val newLastActivity: Any

                if (messageSnapshots != null && !messageSnapshots.isEmpty) {
                    // 2a. Si quedan mensajes, usamos ese
                    val newLastMessage = messageSnapshots.documents[0].toObject(Message::class.java)
                    newLastMessageText = newLastMessage?.text ?: "..."
                    newLastActivity = newLastMessage?.timestamp ?: FieldValue.serverTimestamp()
                } else {
                    // 2b. Si no quedan mensajes, limpiamos el campo
                    newLastMessageText = "Chat vacío"
                    newLastActivity = FieldValue.serverTimestamp()
                }

                // 3. Actualizar la sala de chat
                val roomUpdates = hashMapOf<String, Any>(
                    "lastMessage" to newLastMessageText,
                    "lastActivity" to newLastActivity
                )

                db.collection("chat_rooms").document(chatRoomId)
                    .update(roomUpdates)
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error al actualizar lastMessage después de borrar", e)
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