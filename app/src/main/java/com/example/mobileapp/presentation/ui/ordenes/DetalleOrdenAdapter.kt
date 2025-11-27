package com.example.mobileapp.presentation.ui.ordenes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.SessionStore
import com.example.mobileapp.data.remote.model.LibroDTO
import com.example.mobileapp.data.remote.model.compra.DetalleCompraDTO

data class DetalleItemUI(
    val detalle: DetalleCompraDTO,
    val libro: LibroDTO? = null
)

class DetalleOrdenAdapter : ListAdapter<DetalleItemUI, DetalleOrdenAdapter.DetalleViewHolder>(DetalleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetalleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detalle_orden, parent, false)
        return DetalleViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetalleViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class DetalleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivLibro: ImageView = itemView.findViewById(R.id.ivLibro)
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        private val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        private val tvPrecioUnitario: TextView = itemView.findViewById(R.id.tvPrecioUnitario)
        private val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotal)

        fun bind(item: DetalleItemUI) {
            val detalle = item.detalle
            val libro = item.libro

            tvCantidad.text = "Cantidad: ${detalle.cantidad}"
            tvPrecioUnitario.text = "$${String.format("%.2f", detalle.precioUnitario)}"
            tvSubtotal.text = "$${String.format("%.2f", detalle.subtotal)}"

            if (libro != null) {
                tvTitulo.text = libro.titulo
                
                // Cargar imagen
                val sessionId = SessionStore.sessionId ?: ""
                val imageUrl = when {
                    !libro.imagenPortada.isNullOrBlank() && (libro.imagenPortada!!.startsWith("http") || libro.imagenPortada!!.startsWith("/")) -> {
                        if (libro.imagenPortada!!.startsWith("/")) "http://10.0.2.2:9090" + libro.imagenPortada else libro.imagenPortada
                    }
                    libro.idLibro != null -> "http://10.0.2.2:9090/api/v1/libros/${libro.idLibro}/imagen"
                    else -> null
                }

                val model = imageUrl?.let {
                    GlideUrl(
                        it,
                        LazyHeaders.Builder()
                            .addHeader("X-Session-Id", sessionId)
                            .build()
                    )
                }

                Glide.with(itemView.context)
                    .load(model)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(ivLibro)
            } else {
                tvTitulo.text = "Libro #${detalle.idLibro}"
                ivLibro.setImageResource(R.drawable.ic_placeholder)
            }
        }
    }

    class DetalleDiffCallback : DiffUtil.ItemCallback<DetalleItemUI>() {
        override fun areItemsTheSame(oldItem: DetalleItemUI, newItem: DetalleItemUI): Boolean {
            return oldItem.detalle.idDetalleCompra == newItem.detalle.idDetalleCompra
        }

        override fun areContentsTheSame(oldItem: DetalleItemUI, newItem: DetalleItemUI): Boolean {
            return oldItem == newItem
        }
    }
}
