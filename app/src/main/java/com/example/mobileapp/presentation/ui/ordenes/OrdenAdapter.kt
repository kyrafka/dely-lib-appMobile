package com.example.mobileapp.presentation.ui.ordenes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.model.compra.CompraDTO
import com.google.android.material.chip.Chip

class OrdenAdapter(
    private val onOrdenClick: (CompraDTO) -> Unit
) : ListAdapter<CompraDTO, OrdenAdapter.OrdenViewHolder>(OrdenDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdenViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_orden, parent, false)
        return OrdenViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrdenViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class OrdenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrdenId: TextView = itemView.findViewById(R.id.tvOrdenId)
        private val chipEstado: Chip = itemView.findViewById(R.id.chipEstado)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        private val tvCliente: TextView = itemView.findViewById(R.id.tvCliente)
        private val tvDireccion: TextView = itemView.findViewById(R.id.tvDireccion)

        fun bind(orden: CompraDTO) {
            tvOrdenId.text = "Orden #${orden.idCompra}"
            chipEstado.text = orden.estadoProcesoCompra
            
            // Fecha: preferimos fechaPago, sino fechaCreacionEmpaquetado, sino fechaEntrega
            val fecha = orden.fechaPago ?: orden.fechaCreacionEmpaquetado ?: orden.fechaEntrega ?: "Fecha desconocida"
            tvFecha.text = "Fecha: $fecha"
            
            // Mostrar cliente solo si es EMPRESA
            val prefs = itemView.context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            val userRole = prefs.getString("USER_ROLE", "") ?: ""
            val isEmpresa = "EMPRESA".equals(userRole.trim(), ignoreCase = true)
            
            if (isEmpresa) {
                tvCliente.visibility = View.VISIBLE
                val nombreCliente = orden.nombreUsuario ?: "Cliente ID: ${orden.idUsuario}"
                tvCliente.text = "👤 $nombreCliente"
            } else {
                tvCliente.visibility = View.GONE
            }
            
            tvDireccion.text = "Envío a: ${orden.direccionEnvio}, ${orden.distrito}"

            itemView.setOnClickListener {
                onOrdenClick(orden)
            }
        }
    }

    class OrdenDiffCallback : DiffUtil.ItemCallback<CompraDTO>() {
        override fun areItemsTheSame(oldItem: CompraDTO, newItem: CompraDTO): Boolean {
            return oldItem.idCompra == newItem.idCompra
        }

        override fun areContentsTheSame(oldItem: CompraDTO, newItem: CompraDTO): Boolean {
            return oldItem == newItem
        }
    }
}
