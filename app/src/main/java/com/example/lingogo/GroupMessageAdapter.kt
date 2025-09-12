package com.example.lingogo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adaptador para los mensajes de GRUPO.
 * Es casi idéntico a MessageAdapter, pero usa
 * item_group_message_received.xml para mostrar el nombre del autor.
 */
class GroupMessageAdapter(
    private val messageList: List<GroupMessage>,
    private val currentUserId: String
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
    }

    // ViewHolder para mensajes RECIBIDOS (¡con nombre de autor!)
    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorName: TextView = itemView.findViewById(R.id.tvMessageAuthor)
        val messageText: TextView = itemView.findViewById(R.id.tvMessageText)
        val messageTime: TextView = itemView.findViewById(R.id.tvMessageTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return if (viewType == VIEW_TYPE_SENT) {
            // Reutilizamos el layout de chat 1-a-1
            val view = layoutInflater.inflate(R.layout.item_message_sent, parent, false)
            SentViewHolder(view)
        } else {
            // Usamos el NUEVO layout para grupos
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
            // Es un ViewHolder de tipo ENVIADO
            val sentHolder = holder as SentViewHolder
            sentHolder.messageText.text = message.text
            if (message.timestamp != null) {
                sentHolder.messageTime.text = dateFormatter.format(message.timestamp)
            } else {
                sentHolder.messageTime.text = "..."
            }
        } else {
            // Es un ViewHolder de tipo RECIBIDO
            val receivedHolder = holder as ReceivedViewHolder
            receivedHolder.authorName.text = message.senderName // ¡Mostramos el nombre!
            receivedHolder.messageText.text = message.text
            if (message.timestamp != null) {
                receivedHolder.messageTime.text = dateFormatter.format(message.timestamp)
            } else {
                receivedHolder.messageTime.text = "..."
            }
        }
    }
}