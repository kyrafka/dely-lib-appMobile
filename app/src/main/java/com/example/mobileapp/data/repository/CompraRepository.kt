package com.example.mobileapp.data.repository

import com.example.mobileapp.data.remote.api.CompraApi
import com.example.mobileapp.data.remote.api.MercadoPagoApi
import com.example.mobileapp.data.remote.model.compra.CompraDTO
import com.example.mobileapp.data.remote.model.pago.MercadoPagoResponse
import retrofit2.Response

class CompraRepository(
    private val compraApi: CompraApi,
    private val mercadoPagoApi: MercadoPagoApi
) {

    suspend fun crearCompra(sessionId: String, compra: CompraDTO): Response<CompraDTO> {
        return compraApi.crearCompra(sessionId, compra)
    }

    suspend fun crearPreferenciaPago(sessionId: String, compraId: Long): Response<MercadoPagoResponse> {
        return mercadoPagoApi.createPreference(sessionId, mapOf("compraId" to compraId))
    }

    suspend fun obtenerMisCompras(sessionId: String): Response<List<CompraDTO>> {
        return compraApi.getMyCompras(sessionId)
    }

    suspend fun obtenerCompraPorId(sessionId: String, compraId: Long): Response<CompraDTO> {
        return compraApi.getCompraById(sessionId, compraId)
    }

    // Actualizar estado de compra (para EMPRESA)
    suspend fun actualizarEstadoCompra(
        sessionId: String,
        compraId: Long,
        estado: Map<String, String>
    ): Response<CompraDTO> {
        return compraApi.actualizarEstadoCompra(sessionId, compraId, estado)
    }

    // Obtener todas las compras (para EMPRESA)
    suspend fun obtenerTodasLasCompras(sessionId: String): Response<List<CompraDTO>> {
        return compraApi.getAllCompras(sessionId)
    }
}
