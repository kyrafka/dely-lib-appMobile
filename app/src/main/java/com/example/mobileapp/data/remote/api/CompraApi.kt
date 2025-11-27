package com.example.mobileapp.data.remote.api


import com.example.mobileapp.data.remote.model.compra.CompraDTO
import com.example.mobileapp.data.remote.model.pago.CompraConPagoRequest
import retrofit2.Response
import retrofit2.http.*

interface CompraApi {

    @POST("api/v1/compras")
    suspend fun crearCompra(
        @Header("X-Session-Id") sessionId: String,
        @Body compra: CompraDTO
    ): Response<CompraDTO>

    @POST("api/v1/compras/create-with-payment")
    suspend fun createCompraWithPayment(
        @Header("X-Session-Id") sessionId: String,
        @Body compra: CompraConPagoRequest
    ): Response<Map<String, Any>>

    @GET("api/v1/compras")
    suspend fun getMyCompras(
        @Header("X-Session-Id") sessionId: String
    ): Response<List<CompraDTO>>

    @GET("api/v1/compras/{id}")
    suspend fun getCompraById(
        @Header("X-Session-Id") sessionId: String,
        @Path("id") compraId: Long
    ): Response<CompraDTO>

    // Nuevo: Actualizar estado de compra (para EMPRESA)
    @PATCH("api/v1/compras/{id}/estado")
    suspend fun actualizarEstadoCompra(
        @Header("X-Session-Id") sessionId: String,
        @Path("id") compraId: Long,
        @Body estado: Map<String, String> // {"estadoProcesoCompra": "EMPAQUETADO"}
    ): Response<CompraDTO>

    // Nuevo: Obtener todas las compras (para EMPRESA)
    @GET("api/v1/compras/all")
    suspend fun getAllCompras(
        @Header("X-Session-Id") sessionId: String
    ): Response<List<CompraDTO>>
}
