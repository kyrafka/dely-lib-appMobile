package com.example.mobileapp.presentation.ui.libro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.model.resena.ResenaDTO

class ResenaAdapter : ListAdapter<ResenaDTO, ResenaAdapter.ResenaViewHolder>(ResenaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResenaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resena, parent, false)
        return ResenaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResenaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ResenaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreUsuario: TextView = itemView.findViewById(R.id.tvNombreUsuario)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        private val tvComentario: TextView = itemView.findViewById(R.id.tvComentario)

        fun bind(resena: ResenaDTO) {
            tvNombreUsuario.text = resena.nombreUsuario ?: "Usuario"
            ratingBar.rating = resena.calificacion.toFloat()
            tvFecha.text = resena.fechaCreacion ?: ""
            
            if (resena.comentario.isNullOrBlank()) {
                tvComentario.visibility = View.GONE
            } else {
                tvComentario.visibility = View.VISIBLE
                tvComentario.text = resena.comentario
            }
        }
    }

    class ResenaDiffCallback : DiffUtil.ItemCallback<ResenaDTO>() {
        override fun areItemsTheSame(oldItem: ResenaDTO, newItem: ResenaDTO): Boolean {
            return oldItem.idResena == newItem.idResena
        }

        override fun areContentsTheSame(oldItem: ResenaDTO, newItem: ResenaDTO): Boolean {
            return oldItem == newItem
        }
    }
}
