package com.example.mobileapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mobileapp.data.remote.model.genero.GeneroDTO

@Entity(tableName = "generos")
data class GeneroEntity(
    @PrimaryKey val idGenero: Long,
    val nombre: String,
    val descripcion: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)

fun GeneroDTO.toEntity() = GeneroEntity(
    idGenero = idGenero ?: 0,
    nombre = nombre,
    descripcion = descripcion
)

fun GeneroEntity.toDTO() = GeneroDTO(
    idGenero = idGenero,
    nombre = nombre,
    descripcion = descripcion
)
