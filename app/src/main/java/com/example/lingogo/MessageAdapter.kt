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
 * Este adaptador es más complejo porque maneja DOS tipos de vistas (layouts):
 * 1. VIEW_TYPE_SENT: Mensajes que YO envié (se alinean a la derecha).
 * 2. VIEW_TYPE_RECEIVED: Mensajes que ELLOS enviaron (se alinean a la izquierda).
 */
class MessageAdapter(
    private val messageList: List<Message>,
    private val currentUserId: String,
    // --- ¡NUEVO! ---
    private val onDeleteClick: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val dateFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Constantes para los tipos de vista
    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    /**
     * ViewHolder base.
     * Ambas vistas (enviada y recibida) solo tienen un TextView para el texto
     * y uno para la hora (opcional).
     */
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.tvMessageText)
        val messageTime: TextView = itemView.findViewById(R.id.tvMessageTime)
        // --- ¡NUEVO! ---
        // (El layout recibido no tiene este botón, así que puede ser nulo)
        val deleteButton: ImageView? = itemView.findViewById(R.id.ivDeleteMessage)
    }

    /**
     * Esta función decide qué layout "inflar" (dibujar) basado
     * en quién es el emisor (senderId) del mensaje.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)
        val view: View

        if (viewType == VIEW_TYPE_SENT) {
            // Si el tipo es "enviado", infla el layout 'item_message_sent'
            view = layoutInflater.inflate(R.layout.item_message_sent, parent, false)
        } else {
            // Si el tipo es "recibido", infla el layout 'item_message_received'
            view = layoutInflater.inflate(R.layout.item_message_received, parent, false)
        }
        return MessageViewHolder(view)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    /**
     * Esta función le dice al adaptador qué tipo de vista (layout)
     * usar para cada posición en la lista.
     */
    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]

        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    /**
     * Vincula los datos del objeto Message con las vistas (TextViews).
     */
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.messageText.text = message.text

        if (message.timestamp != null) {
            holder.messageTime.text = dateFormatter.format(message.timestamp)
        } else {
            holder.messageTime.text = "..."
        }

        // --- ¡NUEVO! Lógica de Borrado ---
        // Solo mostramos el botón de borrar si es un mensaje ENVIADO
        if (holder.itemViewType == VIEW_TYPE_SENT) {
            holder.deleteButton?.visibility = View.VISIBLE
            holder.deleteButton?.setOnClickListener {
                onDeleteClick(message)
            }
        } else {
            // (El layout recibido no tiene botón, pero lo ocultamos por si acaso)
            holder.deleteButton?.visibility = View.GONE
        }
    }
}