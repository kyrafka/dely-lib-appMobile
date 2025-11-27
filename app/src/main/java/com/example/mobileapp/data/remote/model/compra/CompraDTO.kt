package com.example.mobileapp.data.remote.model.compra

data class CompraDTO(
    val idCompra: Long? = null,
    val idUsuario: Long,
    val nombreUsuario: String? = null,
    val direccionEnvio: String,
    val distrito: String,
    val calle: String,
    val ciudad: String,
    val fechaPago: String? = null,
    val fechaCreacionEmpaquetado: String? = null,
    val fechaEntrega: String? = null,
    val estadoProcesoCompra: String = "PENDIENTE"
)

