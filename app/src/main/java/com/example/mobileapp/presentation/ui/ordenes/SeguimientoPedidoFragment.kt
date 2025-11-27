package com.example.mobileapp.presentation.ui.ordenes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapp.R
import com.example.mobileapp.data.model.EstadoPedido
import com.example.mobileapp.data.remote.RetrofitClient
import com.example.mobileapp.data.remote.SessionStore
import com.example.mobileapp.data.remote.model.compra.CompraDTO
import com.example.mobileapp.data.repository.CompraRepository
import com.example.mobileapp.data.repository.DetalleCompraRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class SeguimientoPedidoFragment : Fragment(R.layout.fragment_seguimiento_pedido) {

    private var compraId: Long = -1
    private lateinit var compraRepository: CompraRepository
    private lateinit var detalleRepository: DetalleCompraRepository
    private var currentCompra: CompraDTO? = null

    companion object {
        private const val ARG_COMPRA_ID = "compra_id"

        fun newInstance(compraId: Long): SeguimientoPedidoFragment {
            return SeguimientoPedidoFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_COMPRA_ID, compraId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compraId = arguments?.getLong(ARG_COMPRA_ID, -1) ?: -1
        compraRepository = CompraRepository(RetrofitClient.compraApi, RetrofitClient.mercadoPagoApi)
        detalleRepository = DetalleCompraRepository(RetrofitClient.detalleCompraApi)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar toolbar
        view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val isEmpresa = SessionStore.rol?.equals("EMPRESA", ignoreCase = true) == true

        // Mostrar/ocultar botones según el rol
        view.findViewById<View>(R.id.layoutBotonesEmpresa).visibility =
            if (isEmpresa) View.VISIBLE else View.GONE

        // Configurar botones
        view.findViewById<MaterialButton>(R.id.btnAvanzarEstado).setOnClickListener {
            avanzarEstado()
        }

        view.findViewById<MaterialButton>(R.id.btnRetrocederEstado).setOnClickListener {
            retrocederEstado()
        }

        view.findViewById<MaterialButton>(R.id.btnCancelarPedido).setOnClickListener {
            cancelarPedido()
        }

        // Cargar datos
        cargarDatosCompra()
    }

    private fun cargarDatosCompra() {
        val sessionId = SessionStore.sessionId ?: return

        lifecycleScope.launch {
            try {
                // Cargar compra
                val compraResponse = compraRepository.obtenerCompraPorId(sessionId, compraId)
                if (compraResponse.isSuccessful && compraResponse.body() != null) {
                    currentCompra = compraResponse.body()
                    actualizarUI(currentCompra!!)
                }

                // Cargar detalles
                val detallesResponse = detalleRepository.obtenerDetallesPorCompra(sessionId, compraId)
                if (detallesResponse.isSuccessful && detallesResponse.body() != null) {
                    mostrarDetalles(detallesResponse.body()!!)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarUI(compra: CompraDTO) {
        val view = view ?: return

        // Actualizar header
        view.findViewById<TextView>(R.id.tvOrdenNumero).text = "Orden #${compra.idCompra}"
        view.findViewById<TextView>(R.id.tvFechaCompra).text = "Realizada el ${compra.fechaPago ?: "Pendiente"}"

        // Mostrar nombre del cliente si es EMPRESA
        val isEmpresa = SessionStore.rol?.equals("EMPRESA", ignoreCase = true) == true
        if (isEmpresa) {
            view.findViewById<TextView>(R.id.tvClienteNombre).apply {
                visibility = View.VISIBLE
                val nombreCliente = compra.nombreUsuario ?: "Cliente ID: ${compra.idUsuario}"
                text = "👤 $nombreCliente"
            }
        }

        // Actualizar dirección
        val direccionCompleta = "${compra.direccionEnvio}, ${compra.calle}, ${compra.distrito}, ${compra.ciudad}"
        view.findViewById<TextView>(R.id.tvDireccionEnvio).text = direccionCompleta

        // Actualizar barra de progreso
        actualizarBarraProgreso(compra.estadoProcesoCompra)

        // Actualizar botón según estado
        actualizarBotonEstado(compra.estadoProcesoCompra)
    }

    private fun actualizarBarraProgreso(estadoStr: String) {
        val view = view ?: return
        val estado = EstadoPedido.fromString(estadoStr)

        val circleEmpaquetado = view.findViewById<View>(R.id.circleEmpaquetado)
        val lineEmpaquetado = view.findViewById<View>(R.id.lineEmpaquetado)
        val circleEnviado = view.findViewById<View>(R.id.circleEnviado)
        val lineEnviado = view.findViewById<View>(R.id.lineEnviado)
        val circleEntregado = view.findViewById<View>(R.id.circleEntregado)

        val activeColor = ContextCompat.getColor(requireContext(), R.color.green)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.gray_light)

        when (estado) {
            EstadoPedido.PENDIENTE -> {
                // Todo inactivo
                circleEmpaquetado.setBackgroundResource(R.drawable.circle_step_inactive)
                lineEmpaquetado.setBackgroundColor(inactiveColor)
                circleEnviado.setBackgroundResource(R.drawable.circle_step_inactive)
                lineEnviado.setBackgroundColor(inactiveColor)
                circleEntregado.setBackgroundResource(R.drawable.circle_step_inactive)
            }
            EstadoPedido.EMPAQUETADO -> {
                circleEmpaquetado.setBackgroundResource(R.drawable.circle_step_active)
                lineEmpaquetado.setBackgroundColor(inactiveColor)
                circleEnviado.setBackgroundResource(R.drawable.circle_step_inactive)
                lineEnviado.setBackgroundColor(inactiveColor)
                circleEntregado.setBackgroundResource(R.drawable.circle_step_inactive)
            }
            EstadoPedido.ENVIADO -> {
                circleEmpaquetado.setBackgroundResource(R.drawable.circle_step_active)
                lineEmpaquetado.setBackgroundColor(activeColor)
                circleEnviado.setBackgroundResource(R.drawable.circle_step_active)
                lineEnviado.setBackgroundColor(inactiveColor)
                circleEntregado.setBackgroundResource(R.drawable.circle_step_inactive)
            }
            EstadoPedido.ENTREGADO -> {
                circleEmpaquetado.setBackgroundResource(R.drawable.circle_step_active)
                lineEmpaquetado.setBackgroundColor(activeColor)
                circleEnviado.setBackgroundResource(R.drawable.circle_step_active)
                lineEnviado.setBackgroundColor(activeColor)
                circleEntregado.setBackgroundResource(R.drawable.circle_step_active)
            }
            EstadoPedido.CANCELADO -> {
                // Mostrar todo en rojo o gris
                circleEmpaquetado.setBackgroundResource(R.drawable.circle_step_inactive)
                lineEmpaquetado.setBackgroundColor(inactiveColor)
                circleEnviado.setBackgroundResource(R.drawable.circle_step_inactive)
                lineEnviado.setBackgroundColor(inactiveColor)
                circleEntregado.setBackgroundResource(R.drawable.circle_step_inactive)
            }
        }
    }

    private fun actualizarBotonEstado(estadoStr: String) {
        val view = view ?: return
        val btnAvanzar = view.findViewById<MaterialButton>(R.id.btnAvanzarEstado)
        val btnRetroceder = view.findViewById<MaterialButton>(R.id.btnRetrocederEstado)
        val estado = EstadoPedido.fromString(estadoStr)

        // Botón Avanzar
        val nextEstado = EstadoPedido.getNextEstado(estado)
        if (nextEstado != null) {
            btnAvanzar.isEnabled = true
            btnAvanzar.text = "Marcar como ${nextEstado.displayName}"
        } else {
            btnAvanzar.isEnabled = false
            btnAvanzar.text = "Pedido Completado"
        }

        // Botón Retroceder
        val prevEstado = EstadoPedido.getPreviousEstado(estado)
        if (prevEstado != null && estado != EstadoPedido.PENDIENTE) {
            btnRetroceder.isEnabled = true
            btnRetroceder.text = "Retroceder a ${prevEstado.displayName}"
        } else {
            btnRetroceder.isEnabled = false
            btnRetroceder.text = "No se puede retroceder"
        }
    }

    private fun avanzarEstado() {
        val compra = currentCompra ?: return
        val estadoActual = EstadoPedido.fromString(compra.estadoProcesoCompra)
        val nextEstado = EstadoPedido.getNextEstado(estadoActual) ?: return

        val sessionId = SessionStore.sessionId ?: return

        lifecycleScope.launch {
            try {
                val body = mapOf("estadoProcesoCompra" to nextEstado.name)
                val response = compraRepository.actualizarEstadoCompra(sessionId, compraId, body)

                if (response.isSuccessful && response.body() != null) {
                    currentCompra = response.body()
                    actualizarUI(currentCompra!!)
                    Toast.makeText(context, "Estado actualizado a ${nextEstado.displayName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al actualizar estado", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun retrocederEstado() {
        val compra = currentCompra ?: return
        val estadoActual = EstadoPedido.fromString(compra.estadoProcesoCompra)
        val prevEstado = EstadoPedido.getPreviousEstado(estadoActual) ?: return

        val sessionId = SessionStore.sessionId ?: return

        lifecycleScope.launch {
            try {
                val body = mapOf("estadoProcesoCompra" to prevEstado.name)
                val response = compraRepository.actualizarEstadoCompra(sessionId, compraId, body)

                if (response.isSuccessful && response.body() != null) {
                    currentCompra = response.body()
                    actualizarUI(currentCompra!!)
                    Toast.makeText(context, "Estado retrocedido a ${prevEstado.displayName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al retroceder estado", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelarPedido() {
        val sessionId = SessionStore.sessionId ?: return

        lifecycleScope.launch {
            try {
                val body = mapOf("estadoProcesoCompra" to EstadoPedido.CANCELADO.name)
                val response = compraRepository.actualizarEstadoCompra(sessionId, compraId, body)

                if (response.isSuccessful && response.body() != null) {
                    currentCompra = response.body()
                    actualizarUI(currentCompra!!)
                    Toast.makeText(context, "Pedido cancelado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al cancelar pedido", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDetalles(detalles: List<com.example.mobileapp.data.remote.model.compra.DetalleCompraDTO>) {
        val view = view ?: return
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvDetallesCompra)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Crear adapter simple para mostrar los detalles
        val adapter = DetalleCompraAdapter(detalles)
        recyclerView.adapter = adapter
    }
    
    // Adapter simple para mostrar los detalles de la compra
    private class DetalleCompraAdapter(
        private val detalles: List<com.example.mobileapp.data.remote.model.compra.DetalleCompraDTO>
    ) : RecyclerView.Adapter<DetalleCompraAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitulo: TextView = view.findViewById(R.id.tvProductoNombre)
            val tvCantidad: TextView = view.findViewById(R.id.tvCantidad)
            val tvPrecio: TextView = view.findViewById(R.id.tvPrecio)
            val tvSubtotal: TextView = view.findViewById(R.id.tvSubtotal)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_detalle_compra_simple, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val detalle = detalles[position]
            holder.tvTitulo.text = "Libro ID: ${detalle.idLibro}"
            holder.tvCantidad.text = "Cantidad: ${detalle.cantidad}"
            holder.tvPrecio.text = "Precio: $${String.format("%.2f", detalle.precioUnitario)}"
            holder.tvSubtotal.text = "Subtotal: $${String.format("%.2f", detalle.subtotal)}"
        }
        
        override fun getItemCount() = detalles.size
    }
}
