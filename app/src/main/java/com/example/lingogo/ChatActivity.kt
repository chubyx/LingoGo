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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions // ¡Importante!

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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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

    /**
     * Crea un ID único y consistente para la sala de chat 1-a-1
     * ordenando los IDs de usuario alfabéticamente.
     */
    private fun getChatRoomId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList, currentUserId)
        rvMessages.adapter = messageAdapter
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Hace que la lista empiece desde abajo
        }
    }

    /**
     * Escucha en tiempo real los mensajes de la subcolección "messages".
     */
    private fun loadMessages() {
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Error al escuchar mensajes", e)
                    // (Si falla aquí, es probable que la sala aún no exista,
                    // se creará con el primer mensaje)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    messageList.clear()
                    for (document in snapshots.documents) {
                        val message = document.toObject(Message::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                    // Mover el scroll al último mensaje
                    rvMessages.scrollToPosition(messageList.size - 1)
                }
            }
    }

    /**
     * Envía un nuevo mensaje a Firestore.
     */
    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) {
            return
        }

        // Deshabilitar botón y limpiar input
        btnSend.isEnabled = false
        etMessage.setText("")

        // --- ¡LÓGICA INVERTIDA! ---
        // 1. Primero, nos aseguramos de que la sala de chat exista
        //    (con la lista de participantes).
        ensureChatRoomExists {

            // 2. Una vez que la sala existe (o se ha creado),
            //    enviamos el mensaje.

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
                    btnSend.isEnabled = true // Reactivar botón
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error al enviar mensaje", e)
                    Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show()
                    etMessage.setText(text) // Devolver texto si falla
                    btnSend.isEnabled = true // Reactivar botón
                }
        }
    }

    /**
     * ¡FUNCIÓN ACTUALIZADA!
     * Crea/actualiza la sala de chat en Firestore.
     * Llama a onComplete() cuando termina.
     */
    private fun ensureChatRoomExists(onComplete: () -> Unit) {
        val roomData = hashMapOf(
            "lastActivity" to FieldValue.serverTimestamp(),
            "participants" to listOf(currentUserId, otroUserId) // La lista clave
        )

        // Usamos .set con merge para crear la sala si no existe,
        // o solo actualizar el timestamp si ya existe.
        db.collection("chat_rooms").document(chatRoomId)
            .set(roomData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Sala de chat asegurada/creada.")
                onComplete() // Llama a la función lambda (que enviará el mensaje)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al asegurar la sala de chat", e)
                Toast.makeText(this, "Error al crear la sala", Toast.LENGTH_SHORT).show()
                btnSend.isEnabled = true // Reactivar si falla
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