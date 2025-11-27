package com.example.mobileapp.presentation.ui.genero

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.model.LibroDTO
import com.example.mobileapp.data.remote.model.genero.GeneroDTO
import com.example.mobileapp.presentation.ui.libro.LibroAdapter
import android.widget.ImageButton

class GeneroAdapter(
    private val sessionId: String,
    private val onLibroClick: (LibroDTO) -> Unit,
    private val onOpenGeneroDetail: (GeneroDTO) -> Unit
) : RecyclerView.Adapter<GeneroAdapter.GeneroVH>() {

    private val generos = mutableListOf<GeneroDTO>()
    private val librosPorGenero = mutableMapOf<Long, List<LibroDTO>>()

    fun submitGeneros(items: List<GeneroDTO>) {
        generos.clear()
        generos.addAll(items)
        notifyDataSetChanged()
    }

    fun submitLibros(generoId: Long, libros: List<LibroDTO>) {
        librosPorGenero[generoId] = libros
        val idx = generos.indexOfFirst { it.idGenero == generoId }
        if (idx >= 0) notifyItemChanged(idx) else notifyDataSetChanged()
    }

    inner class GeneroVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvGenero: TextView = view.findViewById(R.id.tvGenero)
        val rvLibros: RecyclerView = view.findViewById(R.id.rvLibrosGenero)
        val tvEmpty: View = view.findViewById(R.id.tvEmptyGenero)
        val btnDetalle: View = view.findViewById(R.id.btnGeneroDetalle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeneroVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_genero, parent, false)
        return GeneroVH(v)
    }

    override fun onBindViewHolder(holder: GeneroVH, pos: Int) {
        val genero = generos[pos]
        holder.tvGenero.text = genero.nombre
        holder.btnDetalle.setOnClickListener { onOpenGeneroDetail(genero) }

        val libros = genero.idGenero?.let { id -> librosPorGenero[id] } ?: emptyList()
        val adapter = LibroAdapter(sessionId, onLibroClick)

        holder.rvLibros.layoutManager = LinearLayoutManager(
            holder.itemView.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        holder.rvLibros.adapter = adapter
        adapter.submit(libros)
        holder.tvEmpty.visibility = if (libros.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = generos.size
}
