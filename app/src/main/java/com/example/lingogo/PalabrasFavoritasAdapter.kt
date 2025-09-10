package com.example.lingogo

import com.example.lingogo.database.FavoriteWord
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

// Usamos ListAdapter porque es más eficiente para listas que cambian
class PalabrasFavoritasAdapter(
    private val onDeleteClick: (FavoriteWord) -> Unit
) : ListAdapter<FavoriteWord, PalabrasFavoritasAdapter.PalabraViewHolder>(PalabraDiffCallback()) {

    // 1. ViewHolder: Mantiene las vistas de item_palabra_favorita.xml
    class PalabraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPalabra: TextView = itemView.findViewById(R.id.tvPalabra)
        private val btnBorrar: ImageButton = itemView.findViewById(R.id.btnBorrar)

        fun bind(palabra: FavoriteWord, onDeleteClick: (FavoriteWord) -> Unit) {
            tvPalabra.text = palabra.word
            btnBorrar.setOnClickListener { onDeleteClick(palabra) }
        }
    }

    // 2. onCreate: Infla el layout de la fila
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PalabraViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_palabra_favorita, parent, false)
        return PalabraViewHolder(view)
    }

    // 3. onBind: Conecta los datos con las vistas
    override fun onBindViewHolder(holder: PalabraViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick)
    }

    // 4. DiffCallback: Ayuda al adapter a saber qué cambió
    class PalabraDiffCallback : DiffUtil.ItemCallback<FavoriteWord>() {
        override fun areItemsTheSame(oldItem: FavoriteWord, newItem: FavoriteWord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FavoriteWord, newItem: FavoriteWord): Boolean {
            return oldItem == newItem
        }
    }
}