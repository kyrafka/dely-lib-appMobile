package com.example.mobileapp.data.local

import com.example.mobileapp.data.remote.model.LibroDTO
import com.example.mobileapp.data.remote.model.logreg.LoginResponse
import com.example.mobileapp.data.remote.model.genero.GeneroDTO
import com.example.mobileapp.data.remote.model.inventario.InventarioDTO

object MockData {

    val mockUser = LoginResponse(
        sessionId = "mock-session-id",
        userId = 999,
        nombre = "Usuario Offline",
        rol = "EMPRESA" // Para tener acceso a todo
    )

    val generos = listOf(
        GeneroDTO(1, "Fantasía", "Libros de magia y mundos imaginarios"),
        GeneroDTO(2, "Ciencia Ficción", "Futuro, tecnología y espacio"),
        GeneroDTO(3, "Terror", "Historias de miedo y suspenso"),
        GeneroDTO(4, "Romance", "Historias de amor"),
        GeneroDTO(5, "Aventura", "Viajes y descubrimientos")
    )

    val libros = listOf(
        LibroDTO(
            idLibro = 101,
            titulo = "El Señor de los Anillos",
            nombreCompletoAutor = "J.R.R. Tolkien",
            editorial = "Minotauro",
            isbn = "978-84-450-7372-8",
            numPaginas = 1200,
            fechaLanzamiento = "1954-07-29",
            edicion = "1ra Edición",
            idioma = "Español",
            imagenPortada = "https://covers.openlibrary.org/b/id/10603706-L.jpg", // URL pública real para prueba
            sinopsis = "Un anillo para gobernarlos a todos...",
            puntuacionPromedio = 4.8
        ),
        LibroDTO(
            idLibro = 102,
            titulo = "Harry Potter y la Piedra Filosofal",
            nombreCompletoAutor = "J.K. Rowling",
            editorial = "Salamandra",
            isbn = "978-84-7888-445-2",
            numPaginas = 300,
            fechaLanzamiento = "1997-06-26",
            edicion = "1ra Edición",
            idioma = "Español",
            imagenPortada = "https://covers.openlibrary.org/b/id/10522678-L.jpg",
            sinopsis = "El niño que vivió...",
            puntuacionPromedio = 4.7
        ),
        LibroDTO(
            idLibro = 103,
            titulo = "Dune",
            nombreCompletoAutor = "Frank Herbert",
            editorial = "Debolsillo",
            isbn = "978-84-9759-682-4",
            numPaginas = 700,
            fechaLanzamiento = "1965-08-01",
            edicion = "1ra Edición",
            idioma = "Español",
            imagenPortada = "https://covers.openlibrary.org/b/id/9269899-L.jpg",
            sinopsis = "La especia debe fluir...",
            puntuacionPromedio = 4.6
        ),
        LibroDTO(
            idLibro = 104,
            titulo = "It",
            nombreCompletoAutor = "Stephen King",
            editorial = "Debolsillo",
            isbn = "978-84-9759-379-3",
            numPaginas = 1500,
            fechaLanzamiento = "1986-09-15",
            edicion = "1ra Edición",
            idioma = "Español",
            imagenPortada = "https://covers.openlibrary.org/b/id/12547192-L.jpg",
            sinopsis = "Todos flotan...",
            puntuacionPromedio = 4.5
        )
    )

    val inventario = listOf(
        InventarioDTO(1, 101, 25.99, 10),
        InventarioDTO(2, 102, 19.99, 5),
        InventarioDTO(3, 103, 15.50, 0), // Sin stock
        InventarioDTO(4, 104, 22.00, 3)  // Stock bajo
    )
}
