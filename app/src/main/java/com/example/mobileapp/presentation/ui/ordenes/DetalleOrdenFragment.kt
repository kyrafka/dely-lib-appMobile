package com.example.mobileapp.presentation.ui.ordenes

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.RetrofitClient
import com.example.mobileapp.data.remote.SessionStore
import com.example.mobileapp.data.remote.model.compra.CompraDTO
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class DetalleOrdenFragment : Fragment(R.layout.fragment_detalle_orden) {

    private var compraId: Long = -1L
    private lateinit var adapter: DetalleOrdenAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var btnPagar: ExtendedFloatingActionButton
    private lateinit var btnVerSeguimiento: com.google.android.material.button.MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            compraId = it.getLong(ARG_COMPRA_ID, -1L)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        progressBar = view.findViewById(R.id.progressBar)
        btnPagar = view.findViewById(R.id.btnPagar)
        btnVerSeguimiento = view.findViewById(R.id.btnVerSeguimiento)
        val rvDetalles = view.findViewById<RecyclerView>(R.id.rvDetalles)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = DetalleOrdenAdapter()
        rvDetalles.layoutManager = LinearLayoutManager(requireContext())
        rvDetalles.adapter = adapter

        btnPagar.setOnClickListener {
            iniciarPago()
        }

        btnVerSeguimiento.setOnClickListener {
            navegarASeguimiento()
        }

        cargarDetallesOrden()
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.alpha = 0f
            progressBar.visibility = View.VISIBLE
            progressBar.animate().alpha(1f).setDuration(300).start()
        } else {
            progressBar.animate().alpha(0f).setDuration(300).withEndAction { progressBar.visibility = View.GONE }.start()
        }
    }

    private fun cargarDetallesOrden() {
        val sessionId = SessionStore.sessionId ?: ""
        if (sessionId.isBlank() || compraId == -1L) {
            Toast.makeText(requireContext(), "Error: Orden no válida", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        btnPagar.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Cargar info de la compra
                val compraResponse = RetrofitClient.compraApi.getCompraById(sessionId, compraId)
                if (!compraResponse.isSuccessful || compraResponse.body() == null) {
                    Toast.makeText(requireContext(), "Error al cargar orden", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val compra = compraResponse.body()!!
                actualizarInfoCompra(compra)

                // 2. Cargar detalles (items)
                val detallesResponse = RetrofitClient.detalleCompraApi.getDetallesByCompraId(sessionId, compraId)
                if (detallesResponse.isSuccessful && detallesResponse.body() != null) {
                    val detalles = detallesResponse.body()!!
                    
                    // 3. Cargar info de libros en paralelo
                    val itemsUI = detalles.map { detalle ->
                        async {
                            try {
                                val libroResponse = RetrofitClient.libroApi.findById(sessionId, detalle.idLibro)
                                val libro = if (libroResponse.isSuccessful) libroResponse.body() else null
                                DetalleItemUI(detalle, libro)
                            } catch (e: Exception) {
                                DetalleItemUI(detalle, null)
                            }
                        }
                    }.awaitAll()

                    adapter.submitList(itemsUI)
                    
                    // Calcular total si no viene en compra
                    val total = itemsUI.sumOf { it.detalle.subtotal }
                    view?.findViewById<TextView>(R.id.tvTotal)?.text = "$${String.format("%.2f", total)}"
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun actualizarInfoCompra(compra: CompraDTO) {
        val view = view ?: return
        view.findViewById<TextView>(R.id.tvOrdenId).text = "#${compra.idCompra}"
        
        val fecha = compra.fechaPago ?: compra.fechaCreacionEmpaquetado ?: compra.fechaEntrega ?: "Fecha desconocida"
        view.findViewById<TextView>(R.id.tvFecha).text = fecha
        view.findViewById<TextView>(R.id.tvDireccion).text = "${compra.direccionEnvio}, ${compra.distrito}"

        // Configurar Status Card
        val statusCard = view.findViewById<MaterialCardView>(R.id.statusCard)
        val ivStatusIcon = view.findViewById<ImageView>(R.id.ivStatusIcon)
        val tvStatusTitle = view.findViewById<TextView>(R.id.tvStatusTitle)
        val tvStatusMessage = view.findViewById<TextView>(R.id.tvStatusMessage)

        when (compra.estadoProcesoCompra) {
            "PENDIENTE" -> {
                statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
                statusCard.strokeColor = ContextCompat.getColor(requireContext(), R.color.primary)
                ivStatusIcon.setImageResource(R.drawable.ic_cart)
                ivStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
                tvStatusTitle.text = "Orden Pendiente"
                tvStatusMessage.text = "Tu orden está creada y esperando pago."
                
                btnPagar.visibility = View.VISIBLE
                btnVerSeguimiento.visibility = View.GONE
            }
            "PAGADO", "COMPLETADO", "EMPAQUETADO", "ENVIADO", "ENTREGADO" -> {
                statusCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                statusCard.strokeColor = Color.parseColor("#4CAF50")
                ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                ivStatusIcon.setColorFilter(Color.parseColor("#4CAF50"))
                tvStatusTitle.text = "Compra Aprobada"
                tvStatusMessage.text = "¡Gracias por tu compra! Tu pedido ha sido procesado exitosamente."
                
                btnPagar.visibility = View.GONE
                btnVerSeguimiento.visibility = View.VISIBLE
            }
            "CANCELADO" -> {
                statusCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                statusCard.strokeColor = Color.parseColor("#F44336")
                ivStatusIcon.setImageResource(R.drawable.ic_error)
                ivStatusIcon.setColorFilter(Color.parseColor("#F44336"))
                tvStatusTitle.text = "Orden Cancelada"
                tvStatusMessage.text = "Esta orden ha sido cancelada."
                
                btnPagar.visibility = View.GONE
                btnVerSeguimiento.visibility = View.VISIBLE
            }
            else -> {
                statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
                statusCard.strokeColor = ContextCompat.getColor(requireContext(), R.color.outline)
                tvStatusTitle.text = "Estado: ${compra.estadoProcesoCompra}"
                tvStatusMessage.text = "Detalles de tu orden."
                btnPagar.visibility = View.GONE
                btnVerSeguimiento.visibility = View.VISIBLE
            }
        }
    }

    private fun iniciarPago() {
        val sessionId = SessionStore.sessionId ?: ""
        showLoading(true)
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = mapOf("compraId" to compraId)
                val response = RetrofitClient.mercadoPagoApi.createPreference(sessionId, request)
                
                if (response.isSuccessful && response.body() != null) {
                    val mpResponse = response.body()!!
                    val initPoint = mpResponse.initPoint
                    
                    if (!initPoint.isNullOrBlank()) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(initPoint))
                        startActivity(intent)
                    } else {
                        Toast.makeText(requireContext(), "Error: No se recibió URL de pago", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error al iniciar pago: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun navegarASeguimiento() {
        val fragment = SeguimientoPedidoFragment.newInstance(compraId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val ARG_COMPRA_ID = "arg_compra_id"

        fun newInstance(compraId: Long): DetalleOrdenFragment {
            val fragment = DetalleOrdenFragment()
            val args = Bundle()
            args.putLong(ARG_COMPRA_ID, compraId)
            fragment.arguments = args
            return fragment
        }
    }
}
