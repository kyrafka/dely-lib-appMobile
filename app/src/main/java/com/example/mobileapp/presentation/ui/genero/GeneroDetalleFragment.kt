package com.example.mobileapp.presentation.ui.genero

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.RetrofitClient
import com.example.mobileapp.data.remote.SessionStore
import com.example.mobileapp.data.remote.model.LibroDTO
import com.example.mobileapp.presentation.ui.libro.LibroAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class GeneroDetalleFragment : Fragment(R.layout.fragment_genero_detalle) {

    private var generoId: Long = -1L
    private var generoNombre: String = ""

    private lateinit var rv: RecyclerView
    private lateinit var adapter: LibroAdapter
    private lateinit var editorialContainer: ChipGroup
    private lateinit var deleteModeBanner: View
    private lateinit var btnMore: ImageButton

    private var librosOriginales: List<LibroDTO> = emptyList()
    private var filtroEditorial: String? = null
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            generoId = it.getLong(ARG_ID, -1L)
            generoNombre = it.getString(ARG_NOMBRE, "") ?: ""
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        val tvTituloGenero = view.findViewById<TextView>(R.id.tvTituloGenero)
        val etBuscar = view.findViewById<TextInputEditText>(R.id.etBuscarNombre)
        editorialContainer = view.findViewById(R.id.editorialContainer)
        rv = view.findViewById(R.id.rvLibros)
        deleteModeBanner = view.findViewById(R.id.deleteModeBanner)
        val btnCloseDeleteMode = view.findViewById<ImageButton>(R.id.btnCloseDeleteMode)
        btnMore = view.findViewById(R.id.btnMore)
        val tvInfoReglas = view.findViewById<TextView>(R.id.tvInfoReglas)

        tvTituloGenero.text = generoNombre

        // Ocultar botón de menú si no es empresa
        val roleFromStore = SessionStore.rol
        val roleFromPrefs = requireContext().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE).getString("USER_ROLE", null)
        val isEmpresa = "EMPRESA".equals((roleFromStore ?: roleFromPrefs)?.trim(), ignoreCase = true)
        btnMore.visibility = if (isEmpresa) View.VISIBLE else View.GONE

        val sessionId = SessionStore.sessionId ?: ""
        adapter = LibroAdapter(sessionId, { libro ->
            if (adapter.deleteMode) {
                // Si está en modo eliminación, el click no hace nada
            } else {
                // Navegar al detalle del libro
                libro.idLibro?.let { libroId ->
                    val fragment = com.example.mobileapp.presentation.ui.libro.LibroDetalleFragment.newInstance(libroId)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }, onDelete = { libro ->
            // Lógica de eliminación de libro (igual que antes)
            confirmarEliminacionLibro(libro, sessionId)
        }, onRemoveFromGenero = { libro ->
            // Lógica de remover de género (igual que antes)
            removerLibroDeGenero(libro, sessionId)
        })

        // Usar GridLayoutManager para mostrar libros en estantería (2 columnas)
        rv.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
        rv.adapter = adapter

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        cargarLibros(sessionId)

        etBuscar.addTextChangedListener { text ->
            searchQuery = text?.toString()?.trim().orEmpty()
            aplicarFiltros()
        }

        btnMore.setOnClickListener { showMenu(it, sessionId) }

        btnCloseDeleteMode.setOnClickListener {
            toggleDeleteMode(false)
        }
    }

    private fun showMenu(view: View, sessionId: String) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, if (adapter.deleteMode) "Desactivar eliminación" else "Activar eliminación")
        popup.menu.add(0, 2, 1, "Eliminar género")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    toggleDeleteMode(!adapter.deleteMode)
                    true
                }
                2 -> {
                    confirmarEliminarGenero(sessionId)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun toggleDeleteMode(enable: Boolean) {
        adapter.deleteMode = enable
        adapter.notifyDataSetChanged()
        
        if (enable) {
            deleteModeBanner.visibility = View.VISIBLE
            deleteModeBanner.alpha = 0f
            deleteModeBanner.animate().alpha(1f).setDuration(300).start()
        } else {
            deleteModeBanner.animate().alpha(0f).setDuration(300).withEndAction {
                deleteModeBanner.visibility = View.GONE
            }.start()
        }
    }

    private fun confirmarEliminarGenero(sessionId: String) {
        if (librosOriginales.isNotEmpty()) {
            Toast.makeText(requireContext(), "No puedes eliminar este género: tiene libros asociados", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar género")
            .setMessage("¿Seguro que deseas eliminar el género '$generoNombre'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                    try {
                        val resp = RetrofitClient.generoApi.deleteGenero(sessionId, generoId)
                        if (resp.isSuccessful) {
                            Toast.makeText(requireContext(), "Género eliminado", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        } else {
                            Toast.makeText(requireContext(), "Error al eliminar género: ${resp.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminacionLibro(libro: LibroDTO, sessionId: String) {
        val id = libro.idLibro ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar libro")
            .setMessage("Se quitarán TODAS las asociaciones de género de este libro y luego se eliminará. ¿Deseas continuar y eliminar '${libro.titulo}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarLibro(id, sessionId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarLibro(id: Long, sessionId: String) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            try {
                // Lógica compleja de eliminación de relaciones...
                // (Simplificada para brevedad, pero manteniendo la lógica original si es posible)
                // Por ahora usaremos la lógica directa de intentar borrar relaciones y luego el libro
                
                // 1. Borrar relaciones
                val relsResp = RetrofitClient.generoLibroApi.findGenerosByLibroId(sessionId, id)
                if (relsResp.isSuccessful) {
                    val rels = relsResp.body().orEmpty()
                    for (rel in rels) {
                        rel.idGeneroLibros?.let { relId ->
                            RetrofitClient.generoLibroApi.deleteById(sessionId, relId)
                        }
                    }
                }

                // 2. Borrar libro
                val resp = RetrofitClient.libroApi.deleteLibro(sessionId, id)
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "Libro eliminado", Toast.LENGTH_SHORT).show()
                    cargarLibros(sessionId)
                } else {
                    Toast.makeText(requireContext(), "No se pudo eliminar (código ${resp.code()})", Toast.LENGTH_LONG).show()
                    cargarLibros(sessionId)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removerLibroDeGenero(libro: LibroDTO, sessionId: String) {
        val id = libro.idLibro ?: return
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            try {
                val relsResp = RetrofitClient.generoLibroApi.findGenerosByLibroId(sessionId, id)
                if (relsResp.isSuccessful) {
                    val relacion = relsResp.body().orEmpty()
                        .firstOrNull { it.idGenero == generoId && it.estado?.equals("ACTIVO", true) == true }
                    
                    val relId = relacion?.idGeneroLibros
                    if (relId != null) {
                        val del = RetrofitClient.generoLibroApi.deleteById(sessionId, relId)
                        if (del.isSuccessful) {
                            Toast.makeText(requireContext(), "Libro removido del género", Toast.LENGTH_SHORT).show()
                            cargarLibros(sessionId)
                        } else {
                            Toast.makeText(requireContext(), "Error al remover", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "No se encontró relación activa", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarLibros(sessionId: String) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            try {
                val resp = RetrofitClient.generoApi.findLibrosByGenero(sessionId, generoId)
                if (resp.isSuccessful) {
                    librosOriginales = resp.body().orEmpty()
                    construirChipsEditorial(librosOriginales)
                    aplicarFiltros()
                    actualizarInfoReglas()
                } else {
                    Toast.makeText(requireContext(), "Error al cargar libros: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarInfoReglas() {
        val tvInfoReglas = view?.findViewById<TextView>(R.id.tvInfoReglas) ?: return
        val n = librosOriginales.size
        tvInfoReglas.text = when {
            n == 0 -> "No hay libros en este género aún"
            n == 1 -> "1 libro disponible"
            else -> "$n libros disponibles"
        }
    }

    private fun construirChipsEditorial(libros: List<LibroDTO>) {
        editorialContainer.removeAllViews()
        editorialContainer.isSingleSelection = true
        editorialContainer.isSelectionRequired = false

        val editoriales = libros.mapNotNull { it.editorial?.trim() }
            .filter { it.isNotEmpty() }
            .distinct().sorted()

        fun buildChip(text: String, isSelected: Boolean): Chip {
            return Chip(requireContext()).apply {
                this.text = text
                isCheckable = true
                isChecked = isSelected
                isClickable = true
                setOnClickListener {
                    if (text.equals("TODAS", ignoreCase = true)) {
                        editorialContainer.clearCheck()
                        filtroEditorial = null
                    } else {
                        filtroEditorial = text
                    }
                    aplicarFiltros()
                }
            }
        }

        editorialContainer.addView(buildChip("TODAS", filtroEditorial == null))
        editoriales.forEach { ed ->
            val selected = filtroEditorial?.equals(ed, ignoreCase = true) == true
            editorialContainer.addView(buildChip(ed, selected))
        }
    }

    private fun aplicarFiltros() {
        val filtrados = librosOriginales.filter { libro ->
            val pasaEditorial = filtroEditorial?.let { ed -> (libro.editorial ?: "").equals(ed, ignoreCase = true) } ?: true
            val pasaNombre = if (searchQuery.isEmpty()) true else (libro.titulo ?: "").contains(searchQuery, ignoreCase = true)
            pasaEditorial && pasaNombre
        }
        adapter.submit(filtrados)
    }

    companion object {
        private const val ARG_ID = "arg_genero_id"
        private const val ARG_NOMBRE = "arg_genero_nombre"
        fun newInstance(idGenero: Long, nombre: String): GeneroDetalleFragment {
            val f = GeneroDetalleFragment()
            f.arguments = Bundle().apply {
                putLong(ARG_ID, idGenero)
                putString(ARG_NOMBRE, nombre)
            }
            return f
        }
    }
}
