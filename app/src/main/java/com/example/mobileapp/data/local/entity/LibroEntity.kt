package com.example.mobileapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mobileapp.data.remote.model.LibroDTO

@Entity(tableName = "libros")
data class LibroEntity(
    @PrimaryKey val idLibro: Long,
    val titulo: String,
    val puntuacionPromedio: Double?,
    val sinopsis: String?,
    val fechaLanzamiento: String?,
    val isbn: String?,
    val edicion: String?,
    val editorial: String?,
    val idioma: String?,
    val numPaginas: Int?,
    val nombreCompletoAutor: String?,
    val imagenPortada: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)

// Conversión de DTO a Entity
fun LibroDTO.toEntity() = LibroEntity(
    idLibro = idLibro ?: 0,
    titulo = titulo,
    puntuacionPromedio = puntuacionPromedio,
    sinopsis = sinopsis,
    fechaLanzamiento = fechaLanzamiento,
    isbn = isbn,
    edicion = edicion,
    editorial = editorial,
    idioma = idioma,
    numPaginas = numPaginas,
    nombreCompletoAutor = nombreCompletoAutor,
    imagenPortada = imagenPortada
)

// Conversión de Entity a DTO
fun LibroEntity.toDTO() = LibroDTO(
    idLibro = idLibro,
    titulo = titulo,
    puntuacionPromedio = puntuacionPromedio,
    sinopsis = sinopsis,
    fechaLanzamiento = fechaLanzamiento,
    isbn = isbn,
    edicion = edicion,
    editorial = editorial,
    idioma = idioma,
    numPaginas = numPaginas,
    nombreCompletoAutor = nombreCompletoAutor,
    imagenPortada = imagenPortada
)
