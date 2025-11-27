package com.example.mobileapp.data.remote.model.resena

data class ResenaDTO(
    val idResena: Long? = null,
    val idLibro: Long,
    val idUsuario: Long,
    val nombreUsuario: String? = null,
    val calificacion: Int, // 1-5
    val comentario: String? = null,
    val fechaCreacion: String? = null
)

data class EstadisticasResena(
    val promedio: Double,
    val total: Long
)
