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
// Añadimos dos nuevos parámetros:
// 1. currentUserId: Para saber quién está viendo la lista.
// 2. onDeleteClick: Una lambda para manejar el clic en el nuevo botón.
class PostAdapter(
    private val postList: List<Post>,
    private val currentUserId: String, // ID del usuario logueado
    private val onItemClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit // Lambda para el botón de borrar
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yy 'a las' HH:mm", Locale.getDefault())

    /**
     * ViewHolder: Mantiene las referencias a las vistas de un solo item (item_post.xml)
     */
    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAutor: TextView = itemView.findViewById(R.id.tvPostAutor)
        val tvTexto: TextView = itemView.findViewById(R.id.tvPostTexto)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvPostTimestamp)
        // --- ¡NUEVO! ---
        val ivDelete: ImageView = itemView.findViewById(R.id.ivDeletePost)

        // La función bind para el clic principal no cambia
        fun bind(post: Post, onItemClick: (Post) -> Unit) {
            itemView.setOnClickListener {
                onItemClick(post)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        holder.tvAutor.text = post.autorNombre
        holder.tvTexto.text = post.texto

        if (post.timestamp != null) {
            holder.tvTimestamp.text = dateFormatter.format(post.timestamp)
        } else {
            holder.tvTimestamp.text = ""
        }

        // --- ¡CAMBIO AQUÍ! ---
        // Lógica para mostrar/ocultar el botón de borrar
        if (post.autorId == currentUserId) {
            holder.ivDelete.visibility = View.VISIBLE
            holder.ivDelete.setOnClickListener {
                onDeleteClick(post) // Llama a la nueva lambda
            }
        } else {
            holder.ivDelete.visibility = View.GONE
        }

        // Vincular el clic al item principal
        holder.bind(post, onItemClick)
    }
}