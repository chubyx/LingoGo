package com.example.lingogo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adaptador para los mensajes de GRUPO.
 */
class GroupMessageAdapter(
    private val messageList: List<GroupMessage>,
    private val currentUserId: String,
    // --- ¡NUEVO! ---
    private val groupCreatorId: String, // ID del creador del grupo (Admin)
    private val onDeleteClick: (GroupMessage) -> Unit // Lambda de borrado
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val dateFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Constantes para los tipos de vista
    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    // ViewHolder para mensajes ENVIADOS
    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.tvMessageText)
        val messageTime: TextView = itemView.findViewById(R.id.tvMessageTime)
        val deleteButton: ImageView = itemView.findViewById(R.id.ivDeleteMessage)
    }

    // ViewHolder para mensajes RECIBIDOS (¡con nombre de autor!)
    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorName: TextView = itemView.findViewById(R.id.tvMessageAuthor)
        val messageText: TextView = itemView.findViewById(R.id.tvMessageText)
        val messageTime: TextView = itemView.findViewById(R.id.tvMessageTime)
        val deleteButton: ImageView = itemView.findViewById(R.id.ivDeleteMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return if (viewType == VIEW_TYPE_SENT) {
            // Infla el layout de ENVIADO
            val view = layoutInflater.inflate(R.layout.item_message_sent, parent, false)
            SentViewHolder(view)
        } else {
            // Infla el layout de RECIBIDO (el de grupo)
            val view = layoutInflater.inflate(R.layout.item_group_message_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]

        if (holder.itemViewType == VIEW_TYPE_SENT) {
            // --- Lógica para ViewHolder ENVIADO ---
            val sentHolder = holder as SentViewHolder
            sentHolder.messageText.text = message.text
            if (message.timestamp != null) {
                sentHolder.messageTime.text = dateFormatter.format(message.timestamp)
            } else {
                sentHolder.messageTime.text = "..."
            }

            // ¡NUEVO! Siempre mostramos el botón de borrar (es nuestro mensaje)
            sentHolder.deleteButton.visibility = View.VISIBLE
            sentHolder.deleteButton.setOnClickListener {
                onDeleteClick(message)
            }

        } else {
            // --- Lógica para ViewHolder RECIBIDO ---
            val receivedHolder = holder as ReceivedViewHolder
            receivedHolder.authorName.text = message.senderName // ¡Mostramos el nombre!
            receivedHolder.messageText.text = message.text
            if (message.timestamp != null) {
                receivedHolder.messageTime.text = dateFormatter.format(message.timestamp)
            } else {
                receivedHolder.messageTime.text = "..."
            }

            // --- ¡NUEVO! Lógica de Borrado de Admin ---
            // Solo mostramos el botón de borrar si somos el CREADOR del grupo
            if (currentUserId == groupCreatorId) {
                receivedHolder.deleteButton.visibility = View.VISIBLE
                receivedHolder.deleteButton.setOnClickListener {
                    onDeleteClick(message)
                }
            } else {
                receivedHolder.deleteButton.visibility = View.GONE
            }
        }
    }
}