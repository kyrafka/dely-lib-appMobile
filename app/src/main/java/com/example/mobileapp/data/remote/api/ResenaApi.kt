package com.example.mobileapp.data.remote.api

import com.example.mobileapp.data.remote.model.resena.EstadisticasResena
import com.example.mobileapp.data.remote.model.resena.ResenaDTO
import retrofit2.Response
import retrofit2.http.*

interface ResenaApi {
    
    @POST("api/resenas")
    suspend fun create(
        @Header("Session-Id") sessionId: String,
        @Body resena: ResenaDTO
    ): Response<ResenaDTO>
    
    @PUT("api/resenas/{id}")
    suspend fun update(
        @Header("Session-Id") sessionId: String,
        @Path("id") id: Long,
        @Body resena: ResenaDTO
    ): Response<ResenaDTO>
    
    @DELETE("api/resenas/{id}")
    suspend fun delete(
        @Header("Session-Id") sessionId: String,
        @Path("id") id: Long
    ): Response<Unit>
    
    @GET("api/resenas/libro/{idLibro}")
    suspend fun findByLibro(
        @Header("Session-Id") sessionId: String,
        @Path("idLibro") idLibro: Long
    ): Response<List<ResenaDTO>>
    
    @GET("api/resenas/libro/{idLibro}/estadisticas")
    suspend fun getEstadisticas(
        @Header("Session-Id") sessionId: String,
        @Path("idLibro") idLibro: Long
    ): Response<EstadisticasResena>
}
