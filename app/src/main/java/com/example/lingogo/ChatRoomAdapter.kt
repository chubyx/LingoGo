package com.example.lingogo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adaptador para la lista de salas de chat (la pestaña "Chats")
 */
class ChatRoomAdapter(
    private val chatRoomList: List<ChatRoom>,
    private val onItemClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder>() {

    private val dateFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFoto: ImageView = itemView.findViewById(R.id.ivChatRoomPhoto)
        val tvNombre: TextView = itemView.findViewById(R.id.tvChatRoomName)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvChatRoomLastMessage)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvChatRoomTimestamp)

        fun bind(chatRoom: ChatRoom, onItemClick: (ChatRoom) -> Unit) {
            itemView.setOnClickListener {
                onItemClick(chatRoom)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_room, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun getItemCount(): Int {
        return chatRoomList.size
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        val chatRoom = chatRoomList[position]

        // Llenar los datos
        holder.tvNombre.text = chatRoom.otherUserName
        holder.tvLastMessage.text = chatRoom.lastMessage // (Por ahora estático)

        // Cargar la foto (si no, muestra el ícono por defecto)
        if (chatRoom.otherUserPhotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(chatRoom.otherUserPhotoUrl)
                .circleCrop()
                .into(holder.ivFoto)
        } else {
            holder.ivFoto.setImageResource(R.drawable.ic_perfil_por_defecto)
        }

        // Formatear la fecha
        if (chatRoom.lastActivity != null) {
            holder.tvTimestamp.text = dateFormatter.format(chatRoom.lastActivity!!)
        } else {
            holder.tvTimestamp.text = ""
        }

        // Asignar el clic
        holder.bind(chatRoom, onItemClick)
    }
}