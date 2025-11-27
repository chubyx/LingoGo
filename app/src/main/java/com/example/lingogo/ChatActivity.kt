package com.example.lingogo

import android.content.Intent
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
import java.io.IOException

class ChatActivity : AppCompatActivity() {

    private val TAG = "ChatActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var toolbar: Toolbar
    private lateinit var rvMessages: RecyclerView

    // Contenedores
    private lateinit var layoutInputMessage: LinearLayout
    private lateinit var layoutUserBlocked: LinearLayout
    private lateinit var tvBlockedMessage: TextView

    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    private lateinit var currentUserId: String
    private lateinit var otroUserId: String
    private lateinit var chatRoomId: String

    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    // Dos variables de control
    private var isUserBlocked = false   // Yo lo bloqueé a él
    private var imBlockedByOther = false // Él me bloqueó a mí

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            irALogin()
            return
        }
        currentUserId = currentUser.uid

        otroUserId = intent.getStringExtra("USER_ID_OTRO") ?: ""
        val otroUserNombre = intent.getStringExtra("USER_NOMBRE_OTRO") ?: "Chat"

        if (otroUserId.isEmpty()) {
            finish()
            return
        }

        chatRoomId = getChatRoomId(currentUserId, otroUserId)

        resetUnreadCount()

        // Enlazar Vistas
        toolbar = findViewById(R.id.toolbarChat)
        rvMessages = findViewById(R.id.rvChatMessages)
        etMessage = findViewById(R.id.etChatMessage)
        btnSend = findViewById(R.id.btnSendMessage)

        layoutInputMessage = findViewById(R.id.inputLayout)
        layoutUserBlocked = findViewById(R.id.layoutUserBlocked)
        tvBlockedMessage = findViewById(R.id.tvBlockedMessage)

        layoutUserBlocked.visibility = View.GONE

        // --- INICIAMOS LOS DOS CHEQUEOS ---
        checkBlockStatus()      // Revisa si YO lo bloqueé
        checkIfTheyBlockedMe()  // Revisa si ÉL me bloqueó

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = otroUserNombre

        // Opcional: Si quieres que al tocar el título también vaya al perfil
        toolbar.setOnClickListener {
            abrirPerfilUsuario()
        }

        setupRecyclerView()
        loadMessages()

        btnSend.setOnClickListener { sendMessage() }
    }

    private fun abrirPerfilUsuario() {
        val intent = Intent(this, UserProfileActivity::class.java)
        intent.putExtra("USER_ID", otroUserId)
        startActivity(intent)
    }

    // Chequea MI lista de bloqueados
    private fun checkBlockStatus() {
        db.collection("users").document(currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val blockedList = snapshot.get("blockedUsers") as? List<String> ?: emptyList()
                    isUserBlocked = blockedList.contains(otroUserId)
                    updateBlockUI()
                    invalidateOptionsMenu()
                }
            }
    }

    // --- NUEVO: Chequea SU lista de bloqueados ---
    private fun checkIfTheyBlockedMe() {
        db.collection("users").document(otroUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val theirBlockedList = snapshot.get("blockedUsers") as? List<String> ?: emptyList()
                    // Si mi ID está en su lista, significa que me bloqueó
                    imBlockedByOther = theirBlockedList.contains(currentUserId)
                    updateBlockUI()
                }
            }
    }

    // --- LÓGICA UNIFICADA DE BLOQUEO ---
    private fun updateBlockUI() {
        if (isUserBlocked) {
            // CASO 1: Yo lo bloqueé (Prioridad)
            layoutUserBlocked.visibility = View.VISIBLE
            tvBlockedMessage.text = "Has bloqueado a este usuario"
            layoutInputMessage.visibility = View.GONE
        } else if (imBlockedByOther) {
            // CASO 2: Él me bloqueó
            layoutUserBlocked.visibility = View.VISIBLE
            tvBlockedMessage.text = "No puedes responder a esta conversación"
            layoutInputMessage.visibility = View.GONE
        } else {
            // CASO 3: Nadie está bloqueado
            layoutUserBlocked.visibility = View.GONE
            layoutInputMessage.visibility = View.VISIBLE

            etMessage.isEnabled = true
            btnSend.isEnabled = true
        }
    }

    private fun toggleBlockStatus() {
        val userRef = db.collection("users").document(currentUserId)

        if (isUserBlocked) {
            userRef.update("blockedUsers", FieldValue.arrayRemove(otroUserId))
                .addOnSuccessListener {
                    Toast.makeText(this, "Usuario desbloqueado", Toast.LENGTH_SHORT).show()
                }
        } else {
            userRef.update("blockedUsers", FieldValue.arrayUnion(otroUserId))
                .addOnSuccessListener {
                    Toast.makeText(this, "Usuario bloqueado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    val data = hashMapOf("blockedUsers" to listOf(otroUserId))
                    userRef.set(data, SetOptions.merge())
                }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Opción 1: Ver Perfil
        menu?.add(0, 0, 0, "Ver Perfil")

        // Opción 2: Bloquear/Desbloquear
        val titulo = if (isUserBlocked) "Desbloquear usuario" else "Bloquear usuario"
        menu?.add(0, 1, 1, titulo)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
            0 -> { // ID 0 = Ver Perfil
                abrirPerfilUsuario()
                return true
            }
            1 -> { // ID 1 = Bloquear
                toggleBlockStatus()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getChatRoomId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList, currentUserId) { message ->
            showDeleteMessageDialog(message)
        }
        rvMessages.adapter = messageAdapter
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
    }

    private fun loadMessages() {
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
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
        if (text.isEmpty()) return

        btnSend.isEnabled = false
        etMessage.setText("")

        val messageMap = hashMapOf(
            "senderId" to currentUserId,
            "receiverId" to otroUserId,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .add(messageMap)
            .addOnSuccessListener {
                btnSend.isEnabled = true
            }
            .addOnFailureListener {
                btnSend.isEnabled = true
                Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show()
            }

        ensureChatRoomExists(text)
    }

    private fun ensureChatRoomExists(lastMessage: String) {
        val roomData = hashMapOf<String, Any>(
            "lastActivity" to FieldValue.serverTimestamp(),
            "participants" to listOf(currentUserId, otroUserId),
            "lastMessage" to lastMessage,
            "unreadCounts" to mapOf(otroUserId to FieldValue.increment(1))
        )

        db.collection("chat_rooms").document(chatRoomId)
            .set(roomData, SetOptions.merge())
            .addOnFailureListener {
                Log.w(TAG, "Error actualizando sala", it)
            }
    }

    private fun resetUnreadCount() {
        db.collection("chat_rooms").document(chatRoomId)
            .update("unreadCounts.$currentUserId", 0)
            .addOnFailureListener { e ->
                Log.w(TAG, "No se pudo resetear el contador", e)
            }
    }

    private fun showDeleteMessageDialog(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Borrar Mensaje")
            .setMessage("¿Eliminar?")
            .setPositiveButton("Sí") { _, _ -> deleteMessageFromFirestore(message) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteMessageFromFirestore(message: Message) {
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages").document(message.id)
            .delete()
            .addOnSuccessListener { updateLastMessageAfterDelete() }
    }

    private fun updateLastMessageAfterDelete() {
        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages").orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get()
            .addOnSuccessListener { snaps ->
                val txt = if (snaps != null && !snaps.isEmpty) snaps.documents[0].getString("text") ?: "..." else "Chat vacío"
                db.collection("chat_rooms").document(chatRoomId).update("lastMessage", txt)
            }
    }

    private fun irALogin() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}