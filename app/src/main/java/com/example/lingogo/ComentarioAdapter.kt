package com.example.lingogo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

// --- ¡CAMBIO AQUÍ! ---
// Añadimos currentUserId y onDeleteClick
class ComentarioAdapter(
    private val comentarioList: List<Comentario>,
    private val currentUserId: String,
    private val onDeleteClick: (Comentario) -> Unit
) : RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    class ComentarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAutor: TextView = itemView.findViewById(R.id.tvComentarioAutor)
        val tvTexto: TextView = itemView.findViewById(R.id.tvComentarioTexto)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvComentarioTimestamp)
        // --- ¡NUEVO! ---
        val ivDelete: ImageView = itemView.findViewById(R.id.ivDeleteComentario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comentario, parent, false)
        return ComentarioViewHolder(view)
    }

    override fun getItemCount(): Int {
        return comentarioList.size
    }

    override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {
        val comentario = comentarioList[position]

        holder.tvAutor.text = comentario.autorNombre
        holder.tvTexto.text = comentario.texto

        if (comentario.timestamp != null) {
            holder.tvTimestamp.text = dateFormatter.format(comentario.timestamp)
        } else {
            holder.tvTimestamp.text = "..."
        }

        // --- ¡CAMBIO AQUÍ! ---
        // Lógica para mostrar/ocultar el botón de borrar
        if (comentario.autorId == currentUserId) {
            holder.ivDelete.visibility = View.VISIBLE
            holder.ivDelete.setOnClickListener {
                onDeleteClick(comentario)
            }
        } else {
            holder.ivDelete.visibility = View.GONE
        }
    }
}