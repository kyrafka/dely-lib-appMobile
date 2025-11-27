package com.example.mobileapp.presentation.ui.checkout

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.RetrofitClient
import com.example.mobileapp.data.remote.SessionStore
import com.example.mobileapp.data.remote.model.compra.CompraDTO
import com.example.mobileapp.data.remote.model.compra.DetalleCompraDTO
import com.example.mobileapp.data.repository.CarritoRepository
import com.example.mobileapp.data.repository.CompraRepository
import com.example.mobileapp.data.repository.DetalleCompraRepository
import com.example.mobileapp.presentation.ui.carrito.CarritoItemCompleto
import com.example.mobileapp.presentation.ui.carrito.CarritoViewModel
import com.example.mobileapp.presentation.ui.carrito.CarritoViewModelFactory
import com.example.mobileapp.presentation.ui.libro.PagoConfirmacionFragment
import kotlinx.coroutines.launch

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    private val carritoViewModel: CarritoViewModel by viewModels {
        CarritoViewModelFactory(CarritoRepository(RetrofitClient.carritoApi))
    }

    private val compraRepository =
        CompraRepository(RetrofitClient.compraApi, RetrofitClient.mercadoPagoApi)
    private val detalleCompraRepository = DetalleCompraRepository(RetrofitClient.detalleCompraApi)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        val btnSeleccionarUbicacion = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSeleccionarUbicacion)
        val etDireccion = view.findViewById<EditText>(R.id.etDireccion)
        val etDistrito = view.findViewById<EditText>(R.id.etDistrito)
        val etCalle = view.findViewById<EditText>(R.id.etCalle)
        val etCiudad = view.findViewById<EditText>(R.id.etCiudad)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotal)
        val btnPagar = view.findViewById<Button>(R.id.btnPagar)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Listener para abrir selector de ubicación
        btnSeleccionarUbicacion.setOnClickListener {
            val fragment = LocationPickerFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Recibir resultado del selector de ubicación
        parentFragmentManager.setFragmentResultListener("location_result", viewLifecycleOwner) { _, bundle ->
            val address = bundle.getString("address", "")
            
            // Parsear la dirección y llenar los campos automáticamente
            if (address.isNotEmpty()) {
                val parts = address.split(",").map { it.trim() }
                when {
                    parts.size >= 4 -> {
                        etDireccion.setText(parts[0])
                        etCalle.setText(parts[1])
                        etDistrito.setText(parts[2])
                        etCiudad.setText(parts[3])
                    }
                    parts.size == 3 -> {
                        etDireccion.setText(parts[0])
                        etDistrito.setText(parts[1])
                        etCiudad.setText(parts[2])
                    }
                    parts.size == 2 -> {
                        etDireccion.setText(parts[0])
                        etCiudad.setText(parts[1])
                    }
                    else -> {
                        etDireccion.setText(address)
                    }
                }
                
                Toast.makeText(requireContext(), "Ubicación seleccionada ✓", Toast.LENGTH_SHORT).show()
            }
        }

        // 👈 OBTENER userId REAL desde SharedPreferences
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val userIdString = prefs.getString("USER_ID", null)
        val userId = userIdString?.toLongOrNull()

        if (userId == null) {
            Toast.makeText(
                requireContext(),
                "Error: Usuario no válido. Inicia sesión nuevamente.",
                Toast.LENGTH_LONG
            ).show()
            parentFragmentManager.popBackStack()
            return
        }

        // Cargar carrito completo
        val sessionId = SessionStore.sessionId ?: ""
        carritoViewModel.cargarCarrito(sessionId)

        // Observar carrito completo (con datos de libros)
        carritoViewModel.carritoCompleto.observe(viewLifecycleOwner) { items ->
            val total = items.sumOf { it.precioUnitario * it.cantidad }
            tvTotal.text = "Total: $${String.format("%.2f", total)}"
        }

        carritoViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        carritoViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        btnPagar.setOnClickListener {
            val direccion = etDireccion.text.toString().trim()
            val distrito = etDistrito.text.toString().trim()
            val calle = etCalle.text.toString().trim()
            val ciudad = etCiudad.text.toString().trim()

            if (direccion.isEmpty() || distrito.isEmpty() || calle.isEmpty() || ciudad.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Por favor completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val items = carritoViewModel.carritoCompleto.value
            if (items.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "El carrito está vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            procesarPagoCompleto(
                sessionId,
                userId,
                direccion,
                distrito,
                calle,
                ciudad,
                items,
                progressBar,
                btnPagar
            )
        }
    }

    private fun procesarPagoCompleto(
        sessionId: String,
        userId: Long, // 👈 PARÁMETRO REAL
        direccion: String,
        distrito: String,
        calle: String,
        ciudad: String,
        items: List<CarritoItemCompleto>,
        progressBar: ProgressBar,
        btnPagar: Button
    ) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                btnPagar.isEnabled = false
                btnPagar.text = "Procesando..."

                // PASO 1: Crear compra CON userId REAL
                val compraDTO = CompraDTO(
                    idUsuario = userId, // 👈 AQUÍ ESTÁ LA CORRECCIÓN
                    direccionEnvio = direccion,
                    distrito = distrito,
                    calle = calle,
                    ciudad = ciudad,
                    estadoProcesoCompra = "PENDIENTE"
                )

                val compraResponse = compraRepository.crearCompra(sessionId, compraDTO)
                if (!compraResponse.isSuccessful) {
                    val errorBody = compraResponse.errorBody()?.string()
                    mostrarError("Error al crear compra: ${compraResponse.code()} - $errorBody")
                    return@launch
                }

                val compraCreada = compraResponse.body()!!
                val compraId = compraCreada.idCompra!!

                // NOTA: Los detalles de compra se crean automáticamente en el backend
                // desde el carrito del usuario, no es necesario crearlos aquí

                // PASO 2: Crear preferencia de pago en Mercado Pago
                val preferenciaResponse = compraRepository.crearPreferenciaPago(sessionId, compraId)
                if (!preferenciaResponse.isSuccessful) {
                    val errorBody = preferenciaResponse.errorBody()?.string()
                    mostrarError("Error al crear preferencia de pago: ${preferenciaResponse.code()} - $errorBody")
                    return@launch
                }

                val preference = preferenciaResponse.body()!!

                // PASO 3: Abrir Mercado Pago en navegador
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(preference.initPoint))
                startActivity(intent)

                Toast.makeText(
                    requireContext(),
                    "Redirigiendo a Mercado Pago...",
                    Toast.LENGTH_SHORT
                ).show()

                // NOTA: El carrito se limpia automáticamente en el backend
                // después de crear la compra exitosamente

                // PASO 4: Navegar a confirmación
                val fragment =
                    PagoConfirmacionFragment.newInstance(compraId, preference.totalAmount)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()

            } catch (e: Exception) {
                mostrarError("Error inesperado: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                btnPagar.isEnabled = true
                btnPagar.text = "Pagar con Mercado Pago"
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
    }

    companion object {
        fun newInstance(): CheckoutFragment {
            return CheckoutFragment()
        }
    }
}