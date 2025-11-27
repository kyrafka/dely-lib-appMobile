package com.example.mobileapp.presentation.ui.inventario

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.RetrofitClient
import com.example.mobileapp.data.remote.SessionStore
import com.example.mobileapp.data.repository.InventarioRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class InventarioFragment : Fragment(R.layout.fragment_inventario) {

    private val viewModel: InventarioViewModel by viewModels {
        InventarioViewModelFactory(InventarioRepository(RetrofitClient.inventarioApi, requireContext()))
    }

    private lateinit var adapter: InventarioAdapter
    private lateinit var rvInventario: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar que es EMPRESA
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val userRole = prefs.getString("USER_ROLE", "") ?: ""
        val isEmpresa = "EMPRESA".equals(userRole.trim(), ignoreCase = true)

        if (!isEmpresa) {
            Toast.makeText(requireContext(), "Solo las empresas pueden gestionar inventario", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return
        }

        initViews(view)
        setupRecyclerView()
        setupObservers()
        setupClickListeners(view)

        // Cargar inventario
        cargarInventario()
    }

    private fun initViews(view: View) {
        rvInventario = view.findViewById(R.id.rvInventario)
        emptyState = view.findViewById(R.id.emptyState)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        adapter = InventarioAdapter(
            onEditClick = { inventario ->
                // Navegar a pantalla de edición
                val fragment = AgregarInventarioFragment.newInstance(inventario.idInventario)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { inventario ->
                // Confirmar eliminación
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar inventario")
                    .setMessage("¿Deseas eliminar el inventario de '${inventario.tituloLibro}'?\n\nEsto eliminará precio y stock del libro.")
                    .setPositiveButton("Eliminar") { _, _ ->
                        eliminarInventario(inventario.idInventario)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )

        rvInventario.layoutManager = LinearLayoutManager(requireContext())
        rvInventario.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.inventarios.observe(viewLifecycleOwner) { inventarios ->
            adapter.submitList(inventarios)
            emptyState.visibility = if (inventarios.isEmpty()) View.VISIBLE else View.GONE
            rvInventario.visibility = if (inventarios.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    private fun setupClickListeners(view: View) {
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val fabAgregar = view.findViewById<FloatingActionButton>(R.id.fabAgregarInventario)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        fabAgregar.setOnClickListener {
            // Navegar a pantalla de agregar inventario
            val fragment = AgregarInventarioFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun cargarInventario() {
        val sessionId = SessionStore.sessionId ?: ""
        viewModel.cargarInventarios(sessionId)
    }

    private fun eliminarInventario(inventarioId: Long?) {
        inventarioId?.let { id ->
            val sessionId = SessionStore.sessionId ?: ""
            viewModel.eliminarInventario(sessionId, id)
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar cuando se vuelve del formulario
        cargarInventario()
    }

    companion object {
        fun newInstance(): InventarioFragment {
            return InventarioFragment()
        }
    }
}