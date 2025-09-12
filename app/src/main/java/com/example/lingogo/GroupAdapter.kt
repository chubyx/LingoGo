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
 * Adaptador para la lista de salas de GRUPO (la pestaña "Grupos")
 */
class GroupAdapter(
    private val groupList: List<Group>,
    private val onItemClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    private val dateFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // (Reutilizamos los IDs de item_chat_room.xml)
        val ivFoto: ImageView = itemView.findViewById(R.id.ivChatRoomPhoto)
        val tvNombre: TextView = itemView.findViewById(R.id.tvChatRoomName)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvChatRoomLastMessage)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvChatRoomTimestamp)

        fun bind(group: Group, onItemClick: (Group) -> Unit) {
            itemView.setOnClickListener {
                onItemClick(group)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        // ¡Reutilizamos el layout de item_chat_room!
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_room, parent, false)
        return GroupViewHolder(view)
    }

    override fun getItemCount(): Int {
        return groupList.size
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groupList[position]

        // Llenar los datos
        holder.tvNombre.text = group.nombre // Nombre del GRUPO
        holder.tvLastMessage.text = group.lastMessage

        // (Aquí podríamos poner una foto de grupo, pero por ahora usamos el ícono por defecto)
        holder.ivFoto.setImageResource(R.drawable.ic_group_por_defecto) // (Necesitas crear este drawable)

        // Formatear la fecha
        if (group.lastActivity != null) {
            holder.tvTimestamp.text = dateFormatter.format(group.lastActivity!!)
        } else {
            holder.tvTimestamp.text = ""
        }

        // Asignar el clic
        holder.bind(group, onItemClick)
    }
}