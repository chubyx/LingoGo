package com.example.lingogo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
class UserAdapter(
    private val userList: List<User>,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvUserName)
        val ivFoto: ImageView = itemView.findViewById(R.id.ivUserPhoto)
        // (Podrías añadir un TextView para el email o descripción)

        fun bind(user: User, onItemClick: (User) -> Unit) {
            itemView.setOnClickListener {
                onItemClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.tvNombre.text = user.nombre

        // Cargar la foto de perfil (si no, muestra el ícono por defecto)
        if (user.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.fotoUrl)
                .circleCrop()
                .into(holder.ivFoto)
        } else {
            // (Asegúrate de tener este drawable que creamos)
            holder.ivFoto.setImageResource(R.drawable.ic_perfil_por_defecto)
        }

        holder.bind(user, onItemClick)
    }
}