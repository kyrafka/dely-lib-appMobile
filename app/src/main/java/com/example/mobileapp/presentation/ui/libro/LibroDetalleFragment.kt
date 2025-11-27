package com.example.mobileapp.presentation.ui.libro

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.RetrofitClient
import com.example.mobileapp.data.remote.SessionStore
import com.example.mobileapp.data.remote.model.LibroDTO
import com.example.mobileapp.data.remote.model.carrito.CarritoDTO
import com.example.mobileapp.data.repository.CarritoRepository
import com.example.mobileapp.data.repository.InventarioRepository
import com.example.mobileapp.data.repository.LibroRepository
import kotlinx.coroutines.launch

class LibroDetalleFragment : Fragment(R.layout.fragment_libro_detalle) {

    private var libroId: Long = -1L
    private lateinit var libro: LibroDTO
    private lateinit var carritoRepository: CarritoRepository
    private lateinit var libroRepository: LibroRepository
    private lateinit var inventarioRepository: InventarioRepository

    // Variables para inventario
    private var precioLibro: Double? = null
    private var stockLibro: Int? = null
    private var inventarioDisponible: Boolean = false
    private var currentQuantity: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            libroId = it.getLong(ARG_LIBRO_ID, -1L)
        }
        carritoRepository = CarritoRepository(RetrofitClient.carritoApi)
        libroRepository = LibroRepository(RetrofitClient.libroApi, requireContext())
        inventarioRepository = InventarioRepository(RetrofitClient.inventarioApi, requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        val bottomCard = view.findViewById<View>(R.id.bottomCard)
        val btnAgregarCarrito = view.findViewById<Button>(R.id.btnAgregarCarrito)
        
        // Configurar botón de volver
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        // Elementos del selector de cantidad
        val tvQuantity = view.findViewById<TextView>(R.id.tvQuantity)
        val btnDecrease = view.findViewById<ImageButton>(R.id.btnDecrease)
        val btnIncrease = view.findViewById<ImageButton>(R.id.btnIncrease)

        // Elementos para mostrar precio/stock
        val tvPrecio = view.findViewById<TextView>(R.id.tvPrecio)
        val tvStock = view.findViewById<TextView>(R.id.tvStock)

        // Inicializar cantidad
        tvQuantity.text = currentQuantity.toString()

        // Listeners para cantidad
        btnDecrease.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                tvQuantity.text = currentQuantity.toString()
            }
        }

        btnIncrease.setOnClickListener {
            val maxStock = stockLibro ?: 1
            if (currentQuantity < maxStock) {
                currentQuantity++
                tvQuantity.text = currentQuantity.toString()
            } else {
                Toast.makeText(requireContext(), "Stock máximo alcanzado", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Verificar rol para mostrar/ocultar botón carrito
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val userRole = if (SessionStore.isOfflineMode) "EMPRESA" else (prefs.getString("USER_ROLE", "") ?: "")
        val isCliente = "CLIENTE".equals(userRole.trim(), ignoreCase = true)

        bottomCard.visibility = if (isCliente) View.VISIBLE else View.GONE

        // Cargar libro e inventario
        cargarLibroCompleto(libroId, tvPrecio, tvStock, btnAgregarCarrito)

        // Acción agregar al carrito
        btnAgregarCarrito.setOnClickListener {
            agregarAlCarrito()
        }
    }

    private fun showLoading(show: Boolean) {
        val progressBar = view?.findViewById<View>(R.id.progressBar) ?: return
        if (show) {
            progressBar.alpha = 0f
            progressBar.visibility = View.VISIBLE
            progressBar.animate().alpha(1f).setDuration(300).start()
        } else {
            progressBar.animate().alpha(0f).setDuration(300).withEndAction { progressBar.visibility = View.GONE }.start()
        }
    }

    private fun cargarLibroCompleto(id: Long, tvPrecio: TextView, tvStock: TextView, btnCarrito: Button) {
        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sessionId = SessionStore.sessionId ?: ""

                // 1. Cargar datos del libro
                val libroResponse = libroRepository.findById(sessionId, id)
                if (libroResponse.isSuccessful && libroResponse.body() != null) {
                    libro = libroResponse.body()!!
                    mostrarDetallesLibro()
                } else {
                    Toast.makeText(requireContext(), "Error al cargar libro: ${libroResponse.code()}", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@launch
                }

                // 2. Cargar inventario del libro
                try {
                    val inventarioResponse = inventarioRepository.findByLibroId(sessionId, id)
                    if (inventarioResponse.isSuccessful && inventarioResponse.body() != null) {
                        val inventario = inventarioResponse.body()!!
                        precioLibro = inventario.precio
                        stockLibro = inventario.cantidadStock
                        inventarioDisponible = true

                        // Mostrar precio y stock
                        tvPrecio.text = "Precio: $${String.format("%.2f", precioLibro ?: 0.0)}"
                        tvStock.text = "Stock: ${stockLibro ?: 0} disponibles"

                        // Habilitar/deshabilitar botón según stock
                        val hayStock = (stockLibro ?: 0) > 0
                        btnCarrito.isEnabled = hayStock && inventarioDisponible
                        btnCarrito.text = if (hayStock) "Agregar al Carrito" else "Sin stock"
                        btnCarrito.alpha = if (hayStock) 1.0f else 0.5f

                        tvPrecio.visibility = View.VISIBLE
                        tvStock.visibility = View.VISIBLE
                    } else {
                        // No hay inventario para este libro
                        inventarioDisponible = false
                        tvPrecio.text = "Precio: No disponible"
                        tvStock.text = "Stock: No disponible"
                        btnCarrito.isEnabled = false
                        btnCarrito.text = "No disponible"
                        btnCarrito.alpha = 0.5f

                        tvPrecio.visibility = View.VISIBLE
                        tvStock.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    // Error al cargar inventario
                    tvPrecio.text = "Precio: Error al cargar"
                    tvStock.text = "Stock: Error al cargar"
                    btnCarrito.isEnabled = false
                    btnCarrito.alpha = 0.5f

                    tvPrecio.visibility = View.VISIBLE
                    tvStock.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun mostrarDetallesLibro() {
        val view = requireView()
        val sessionId = SessionStore.sessionId ?: ""

        // Imagen
        val ivPortada = view.findViewById<ImageView>(R.id.ivPortada)
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

        Glide.with(this)
            .load(model)
            .placeholder(R.drawable.ic_placeholder)
            .into(ivPortada)

        // Información básica
        view.findViewById<TextView>(R.id.tvTitulo).text = libro.titulo
        view.findViewById<TextView>(R.id.tvAutor).text = libro.nombreCompletoAutor ?: "Autor desconocido"
        view.findViewById<TextView>(R.id.tvSinopsis).text = libro.sinopsis ?: "Sin sinopsis disponible"

        // Información adicional
        view.findViewById<TextView>(R.id.tvEditorial).text = "Editorial: ${libro.editorial ?: "No especificada"}"
        view.findViewById<TextView>(R.id.tvIsbn).text = "ISBN: ${libro.isbn ?: "No disponible"}"
        view.findViewById<TextView>(R.id.tvFechaLanzamiento).text = "Fecha: ${libro.fechaLanzamiento ?: "No especificada"}"
        view.findViewById<TextView>(R.id.tvIdioma).text = "Idioma: ${libro.idioma ?: "No especificado"}"
        view.findViewById<TextView>(R.id.tvNumPaginas).text = "Páginas: ${libro.numPaginas ?: 0}"
        view.findViewById<TextView>(R.id.tvEdicion).text = "Edición: ${libro.edicion ?: "No especificada"}"

        // Puntuación
        val rating = (libro.puntuacionPromedio ?: 0.0).coerceIn(0.0, 5.0)
        val filled = rating.toInt()
        val half = (rating - filled) >= 0.5
        val stars = StringBuilder().apply {
            repeat(filled) { append('★') }
            if (half) append('★')
            val remaining = 5 - length
            repeat(remaining) { append('☆') }
        }.toString()
        view.findViewById<TextView>(R.id.tvEstrellas).text = "$stars (${String.format("%.1f", rating)})"
    }

    private fun agregarAlCarrito() {
        // Validar stock antes de agregar
        if (!inventarioDisponible) {
            Toast.makeText(requireContext(), "Este libro no está disponible", Toast.LENGTH_SHORT).show()
            return
        }

        if ((stockLibro ?: 0) <= 0) {
            Toast.makeText(requireContext(), "Sin stock disponible", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sessionId = SessionStore.sessionId ?: ""
                val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val userId = prefs.getString("USER_ID", "")?.toLongOrNull()

                if (userId == null) {
                    Toast.makeText(requireContext(), "Error: Usuario no válido", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Crear item de carrito con la cantidad seleccionada
                val carritoItem = CarritoDTO(
                    idUsuario = userId,
                    idLibro = libro.idLibro!!,
                    cantidad = currentQuantity,
                    precioUnitario = precioLibro
                )

                val response = carritoRepository.addToCart(sessionId, carritoItem)

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Libro agregado al carrito por $${String.format("%.2f", (precioLibro ?: 0.0) * currentQuantity)}", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    Toast.makeText(requireContext(), "Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val ARG_LIBRO_ID = "arg_libro_id"

        fun newInstance(libroId: Long): LibroDetalleFragment {
            val fragment = LibroDetalleFragment()
            val args = Bundle()
            args.putLong(ARG_LIBRO_ID, libroId)
            fragment.arguments = args
            return fragment
        }
    }
}