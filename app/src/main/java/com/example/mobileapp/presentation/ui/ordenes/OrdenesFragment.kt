package com.example.mobileapp.presentation.ui.ordenes

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.RetrofitClient
import com.example.mobileapp.data.remote.SessionStore
import kotlinx.coroutines.launch

class OrdenesFragment : Fragment(R.layout.fragment_ordenes) {

    private lateinit var adapter: OrdenAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var rvOrdenes: RecyclerView
    
    private var ordenesOriginales: List<com.example.mobileapp.data.remote.model.compra.CompraDTO> = emptyList()
    private var filtroActual: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmptyOrdenes)
        rvOrdenes = view.findViewById(R.id.rvOrdenes)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupRecyclerView()
        setupFiltros(view)
        cargarOrdenes()
    }

    private fun setupFiltros(view: View) {
        val chipTodos = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipTodos)
        val chipPendiente = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipPendiente)
        val chipPagado = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipPagado)
        val chipEmpaquetado = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipEmpaquetado)
        val chipEnviado = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipEnviado)
        val chipEntregado = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipEntregado)
        val chipCancelado = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipCancelado)

        chipTodos.setOnClickListener { aplicarFiltro(null) }
        chipPendiente.setOnClickListener { aplicarFiltro("PENDIENTE") }
        chipPagado.setOnClickListener { aplicarFiltro("PAGADO") }
        chipEmpaquetado.setOnClickListener { aplicarFiltro("EMPAQUETADO") }
        chipEnviado.setOnClickListener { aplicarFiltro("ENVIADO") }
        chipEntregado.setOnClickListener { aplicarFiltro("ENTREGADO") }
        chipCancelado.setOnClickListener { aplicarFiltro("CANCELADO") }
    }

    private fun aplicarFiltro(estado: String?) {
        filtroActual = estado
        val ordenesFiltradas = if (estado == null) {
            ordenesOriginales
        } else {
            ordenesOriginales.filter { it.estadoProcesoCompra.equals(estado, ignoreCase = true) }
        }
        
        if (ordenesFiltradas.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvOrdenes.visibility = View.GONE
            tvEmpty.text = if (estado == null) "No tienes órdenes aún" else "No hay órdenes con estado: $estado"
        } else {
            tvEmpty.visibility = View.GONE
            rvOrdenes.visibility = View.VISIBLE
            adapter.submitList(ordenesFiltradas)
        }
    }

    private fun setupRecyclerView() {
        adapter = OrdenAdapter { orden ->
            orden.idCompra?.let { id ->
                val fragment = DetalleOrdenFragment.newInstance(id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
        rvOrdenes.layoutManager = LinearLayoutManager(requireContext())
        rvOrdenes.adapter = adapter
    }

    private fun cargarOrdenes() {
        val sessionId = SessionStore.sessionId ?: ""
        if (sessionId.isBlank()) {
            Toast.makeText(requireContext(), "Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.alpha = 0f
        progressBar.visibility = View.VISIBLE
        progressBar.animate().alpha(1f).setDuration(300).start()
        
        tvEmpty.visibility = View.GONE
        rvOrdenes.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.compraApi.getMyCompras(sessionId)
                if (response.isSuccessful) {
                    val ordenes = response.body() ?: emptyList()
                    
                    // Ordenar por ID descendente (más reciente primero)
                    ordenesOriginales = ordenes.sortedByDescending { it.idCompra }
                    
                    if (ordenesOriginales.isEmpty()) {
                        tvEmpty.alpha = 0f
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.animate().alpha(1f).setDuration(300).start()
                        rvOrdenes.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvOrdenes.alpha = 0f
                        rvOrdenes.visibility = View.VISIBLE
                        rvOrdenes.animate().alpha(1f).setDuration(300).start()
                        
                        // Aplicar filtro actual (si hay)
                        aplicarFiltro(filtroActual)
                    }
                } else {
                    Toast.makeText(requireContext(), "Error al cargar órdenes: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.animate().alpha(0f).setDuration(300).withEndAction { progressBar.visibility = View.GONE }.start()
            }
        }
    }

    companion object {
        fun newInstance(): OrdenesFragment {
            return OrdenesFragment()
        }
    }
}
