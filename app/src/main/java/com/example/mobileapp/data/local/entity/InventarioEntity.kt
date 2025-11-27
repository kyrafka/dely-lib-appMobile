package com.example.mobileapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mobileapp.data.remote.model.inventario.InventarioDTO

@Entity(tableName = "inventario")
data class InventarioEntity(
    @PrimaryKey val idInventario: Long,
    val idLibro: Long,
    val precio: Double,
    val cantidadStock: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

fun InventarioDTO.toEntity() = InventarioEntity(
    idInventario = idInventario ?: 0,
    idLibro = idLibro,
    precio = precio,
    cantidadStock = cantidadStock
)

fun InventarioEntity.toDTO() = InventarioDTO(
    idInventario = idInventario,
    idLibro = idLibro,
    precio = precio,
    cantidadStock = cantidadStock
)
