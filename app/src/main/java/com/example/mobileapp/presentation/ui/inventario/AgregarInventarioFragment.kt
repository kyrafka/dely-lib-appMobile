package com.example.mobileapp.presentation.ui.inventario

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.RetrofitClient
import com.example.mobileapp.data.remote.SessionStore
import com.example.mobileapp.data.remote.model.LibroDTO
import com.example.mobileapp.data.remote.model.inventario.InventarioDTO
import com.example.mobileapp.data.repository.InventarioRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AgregarInventarioFragment : Fragment(R.layout.fragment_agregar_inventario) {

    private val viewModel: InventarioViewModel by viewModels {
        InventarioViewModelFactory(InventarioRepository(RetrofitClient.inventarioApi, requireContext()))
    }

    private var inventarioId: Long? = null
    private var isEditMode = false
    private var librosDisponibles: List<LibroDTO> = emptyList()

    private lateinit var spinnerLibro: Spinner
    private lateinit var etPrecio: TextInputEditText
    private lateinit var etStock: TextInputEditText
    private lateinit var btnGuardar: MaterialButton
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            inventarioId = it.getLong(ARG_INVENTARIO_ID, -1L).takeIf { id -> id != -1L }
            isEditMode = inventarioId != null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupObservers()
        setupClickListeners(view)
        cargarLibros()

        if (isEditMode) {
            toolbar.title = "Editar Inventario"
            btnGuardar.text = "Actualizar Inventario"
            cargarDatosInventario()
        } else {
            toolbar.title = "Agregar Inventario"
            btnGuardar.text = "Guardar Inventario"
        }
    }

    private fun initViews(view: View) {
        spinnerLibro = view.findViewById(R.id.spinnerLibro)
        etPrecio = view.findViewById(R.id.etPrecio)
        etStock = view.findViewById(R.id.etStock)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        toolbar = view.findViewById(R.id.toolbar)
    }

    private fun setupObservers() {
        viewModel.operationSuccess.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            btnGuardar.isEnabled = !isLoading
            btnGuardar.text = if (isLoading) "Procesando..." else if (isEditMode) "Actualizar Inventario" else "Guardar Inventario"
        }
    }

    private fun setupClickListeners(view: View) {
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnGuardar.setOnClickListener {
            guardarInventario()
        }
    }

    private fun cargarLibros() {
        lifecycleScope.launch {
            try {
                val sessionId = SessionStore.sessionId ?: ""
                val response = RetrofitClient.libroApi.findAll(sessionId)

                if (response.isSuccessful) {
                    librosDisponibles = response.body() ?: emptyList()
                    configurarSpinnerLibros()
                } else {
                    Toast.makeText(requireContext(), "Error al cargar libros", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarSpinnerLibros() {
        val librosParaSpinner = if (isEditMode) {
            // En modo edición, mostrar todos los libros
            librosDisponibles
        } else {
            // En modo creación, filtrar libros que ya tienen inventario
            filtrarLibrosSinInventario()
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            librosParaSpinner.map { "${it.titulo} - ${it.nombreCompletoAutor}" }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerLibro.adapter = adapter
        spinnerLibro.isEnabled = !isEditMode // Deshabilitado en modo edición
    }

    private fun filtrarLibrosSinInventario(): List<LibroDTO> {
        // Aquí podrías hacer una consulta para verificar qué libros ya tienen inventario
        // Por simplicidad, retornamos todos por ahora
        return librosDisponibles
    }

    private fun cargarDatosInventario() {
        inventarioId?.let { id ->
            lifecycleScope.launch {
                try {
                    val sessionId = SessionStore.sessionId ?: ""
                    val response = RetrofitClient.inventarioApi.findById(sessionId, id)

                    if (response.isSuccessful) {
                        val inventario = response.body()!!

                        etPrecio.setText(inventario.precio.toString())
                        etStock.setText(inventario.cantidadStock.toString())

                        // Seleccionar el libro correspondiente en el spinner
                        val libroIndex = librosDisponibles.indexOfFirst { it.idLibro == inventario.idLibro }
                        if (libroIndex >= 0) {
                            spinnerLibro.setSelection(libroIndex)
                        }

                    } else {
                        Toast.makeText(requireContext(), "Error al cargar inventario", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun guardarInventario() {
        // Validaciones
        val precioText = etPrecio.text.toString().trim()
        val stockText = etStock.text.toString().trim()

        if (precioText.isEmpty()) {
            etPrecio.error = "El precio es obligatorio"
            return
        }

        if (stockText.isEmpty()) {
            etStock.error = "El stock es obligatorio"
            return
        }

        val precio = precioText.toDoubleOrNull()
        val stock = stockText.toIntOrNull()

        if (precio == null || precio <= 0) {
            etPrecio.error = "Precio debe ser mayor a 0"
            return
        }

        if (stock == null || stock < 0) {
            etStock.error = "Stock no puede ser negativo"
            return
        }

        if (spinnerLibro.selectedItemPosition == -1 || librosDisponibles.isEmpty()) {
            Toast.makeText(requireContext(), "Debe seleccionar un libro", Toast.LENGTH_SHORT).show()
            return
        }

        val libroSeleccionado = librosDisponibles[spinnerLibro.selectedItemPosition]
        val sessionId = SessionStore.sessionId ?: ""

        val inventarioDTO = InventarioDTO(
            idInventario = inventarioId,
            idLibro = libroSeleccionado.idLibro!!,
            precio = precio,
            cantidadStock = stock
        )

        if (isEditMode) {
            viewModel.actualizarInventario(sessionId, inventarioId!!, inventarioDTO)
        } else {
            viewModel.crearInventario(sessionId, inventarioDTO)
        }
    }

    companion object {
        private const val ARG_INVENTARIO_ID = "inventario_id"

        fun newInstance(inventarioId: Long? = null): AgregarInventarioFragment {
            val fragment = AgregarInventarioFragment()
            val args = Bundle()
            inventarioId?.let {
                args.putLong(ARG_INVENTARIO_ID, it)
            }
            fragment.arguments = args
            return fragment
        }
    }
}