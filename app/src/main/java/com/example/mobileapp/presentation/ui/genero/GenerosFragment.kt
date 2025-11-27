package com.example.mobileapp.presentation.ui.genero

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog as AppAlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.SessionStore
import com.example.mobileapp.data.remote.RetrofitClient
import com.example.mobileapp.data.repository.GeneroRepository
import com.example.mobileapp.presentation.books.GeneroViewModel
import com.example.mobileapp.presentation.books.GeneroViewModelFactory
import com.example.mobileapp.presentation.logreg.LoginFragment
import com.example.mobileapp.presentation.ui.libro.LibroDetalleFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.mobileapp.presentation.ui.carrito.CarritoFragment
import com.example.mobileapp.presentation.ui.inventario.InventarioFragment
import kotlinx.coroutines.launch

class GenerosFragment : Fragment(R.layout.fragment_generos) {

    private val viewModel: GeneroViewModel by viewModels {
        GeneroViewModelFactory(GeneroRepository(RetrofitClient.generoApi, requireContext()))
    }

    private lateinit var adapter: GeneroAdapter
    private var generosCache: List<com.example.mobileapp.data.remote.model.genero.GeneroDTO> = emptyList()
    private var filtroGeneroId: Long? = null
    private lateinit var rv: RecyclerView
    private lateinit var tvCartBadge: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Header: mostrar nombre de usuario y acción de logout
        val tvBrand = view.findViewById<TextView>(R.id.tvBrand)
        val ivProfile = view.findViewById<ImageView>(R.id.ivProfile)
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val displayName = prefs.getString("USER_NAME", null)
        tvBrand.text = displayName ?: getString(R.string.app_name)

        ivProfile.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas cerrar sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    // Limpiar preferencias y SessionStore
                    prefs.edit().clear().apply()
                    SessionStore.sessionId = null
                    SessionStore.rol = null
                    // Ir al login
                    parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, com.example.mobileapp.presentation.logreg.LoginFragment())
                        .commit()
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Bottom bar icons
        val navHome = view.findViewById<View>(R.id.navHome)
        val navCart = view.findViewById<View>(R.id.navCart)
        val navOrders = view.findViewById<View>(R.id.navOrders)
        val navAdd = view.findViewById<View>(R.id.navAdd)
        val navInventory = view.findViewById<View>(R.id.navInventary)
        tvCartBadge = view.findViewById(R.id.tvCartBadge)
        // Recycler + progress + filtros
        rv = view.findViewById<RecyclerView>(R.id.rvGeneros)
        val progress = view.findViewById<ProgressBar>(R.id.progress)
        val filterContainer = view.findViewById<ChipGroup>(R.id.filterContainer)

        val sessionId = SessionStore.sessionId ?: ""
        adapter = GeneroAdapter(sessionId, { libro ->
            // ⭁ENAVEGAR AL DETALLE DEL LIBRO
            libro.idLibro?.let { libroId ->
                val detalleFragment = LibroDetalleFragment.newInstance(libroId)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, detalleFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }) { genero ->
            // Abrir pantalla detalle del género
            val frag = GeneroDetalleFragment.newInstance(genero.idGenero ?: -1L, genero.nombre ?: "")
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, frag)
                .addToBackStack(null)
                .commit()
        }
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        rv.adapter = adapter

        // Observers
        viewModel.generos.observe(viewLifecycleOwner) { generos ->
            generosCache = generos
            renderChips(filterContainer, generosCache)
            // Mostrar de inmediato las secciones visibles (aunque aún no lleguen los libros)
            val listaMostrar = filtroGeneroId?.let { id -> generosCache.filter { it.idGenero == id } } ?: generosCache
            adapter.submitGeneros(listaMostrar)
            // cargar libros de cada género
            val sessionIdObs = SessionStore.sessionId
            if (sessionIdObs.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Sesión no válida. Inicia sesión de nuevo.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, com.example.mobileapp.presentation.logreg.LoginFragment())
                    .commit()
                return@observe
            }
            generos.forEach { genero -> genero.idGenero?.let { id -> viewModel.cargarLibrosPorGenero(sessionIdObs, id) } }
        }
        viewModel.librosPorGenero.observe(viewLifecycleOwner) { map ->
            // enviar libros al adapter sin filtros globales
            map.forEach { (generoId, libros) -> adapter.submitLibros(generoId, libros) }
        }
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                progress.alpha = 0f
                progress.visibility = View.VISIBLE
                progress.animate().alpha(1f).setDuration(300).start()
            } else {
                progress.animate().alpha(0f).setDuration(300).withEndAction { progress.visibility = View.GONE }.start()
            }
        }
        viewModel.error.observe(viewLifecycleOwner) {
            if (it != null) Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        // Mostrar/ocultar "Agregar" según rol (insensible a mayúsculas/minúsculas y espacios)
        val role = (SessionStore.rol
            ?: requireContext()
                .getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("USER_ROLE", ""))?.trim()

        val isEmpresa = "EMPRESA".equals(role, ignoreCase = true)
        navAdd.visibility = if (isEmpresa) View.VISIBLE else View.GONE
        navInventory.visibility = if (isEmpresa) View.VISIBLE else View.GONE // 👈 NUEVO

        // Clicks bottom bar
        navHome.setOnClickListener {
            // Ya estás en home (géneros). Si usas más pantallas, podrías refrescar.
        }
        navCart.setOnClickListener {
            // Navegar al CarritoFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CarritoFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
        navOrders.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, com.example.mobileapp.presentation.ui.ordenes.OrdenesFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
        navAdd.setOnClickListener {
            // Abrir el formulario para EMPRESA
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, com.example.mobileapp.presentation.ui.libro.AddLibroFragment())
                .addToBackStack(null)
                .commit()
        }

        // Clicks bottom bar
        navInventory.setOnClickListener { // 👈 NUEVO
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, InventarioFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }


        // Cargar datos
        viewModel.cargarGeneros(SessionStore.sessionId ?: "")
    }

    override fun onResume() {
        super.onResume()
        // Refrescar al volver del formulario
        viewModel.cargarGeneros(SessionStore.sessionId ?: "")
        // Actualizar badge del carrito
        actualizarBadgeCarrito()
    }

    private fun actualizarBadgeCarrito() {
        val sessionId = SessionStore.sessionId ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.carritoApi.getMyCart(sessionId)
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    val totalItems = items.sumOf { it.cantidad }
                    
                    if (totalItems > 0) {
                        tvCartBadge.visibility = View.VISIBLE
                        tvCartBadge.text = if (totalItems > 99) "99+" else totalItems.toString()
                    } else {
                        tvCartBadge.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                // Silenciosamente fallar, no es crítico
            }
        }
    }

    private fun renderChips(container: ChipGroup, generos: List<com.example.mobileapp.data.remote.model.genero.GeneroDTO>) {
        container.removeAllViews()
        container.isSingleSelection = true
        container.isSelectionRequired = false

        fun buildChip(text: String, checked: Boolean, onChecked: () -> Unit): Chip {
            return Chip(requireContext()).apply {
                this.text = text
                isCheckable = true
                isChecked = checked
                isClickable = true
                setOnClickListener { onChecked() }
            }
        }

        // Chip "TODOS"
        container.addView(buildChip("TODOS", filtroGeneroId == null) {
            filtroGeneroId = null
            // actualizar lista visible
            android.transition.TransitionManager.beginDelayedTransition(rv)
            adapter.submitGeneros(generos)
        })

        // Chips por género
        generos.forEach { g ->
            val selected = (filtroGeneroId == g.idGenero)
            container.addView(buildChip(g.nombre, selected) {
                filtroGeneroId = g.idGenero
                val listaMostrar = generos.filter { it.idGenero == g.idGenero }
                // Animate the change
                android.transition.TransitionManager.beginDelayedTransition(rv)
                adapter.submitGeneros(listaMostrar)
            })
        }
    }
}
