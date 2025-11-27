package com.example.mobileapp.data.repository

import android.content.Context
import android.util.Log
import com.example.mobileapp.data.local.AppDatabase
import com.example.mobileapp.data.local.entity.toDTO
import com.example.mobileapp.data.local.entity.toEntity
import com.example.mobileapp.data.remote.api.GeneroApi
import com.example.mobileapp.data.remote.model.LibroDTO
import com.example.mobileapp.data.remote.model.genero.GeneroDTO
import com.example.mobileapp.util.NetworkHelper
import retrofit2.Response

class GeneroRepository(
    private val api: GeneroApi,
    private val context: Context
) {
    private val db = AppDatabase.getDatabase(context)
    private val generoDao = db.generoDao()
    private val libroDao = db.libroDao()

    suspend fun findAllGeneros(sessionId: String): Response<List<GeneroDTO>> {
        return try {
            if (NetworkHelper.isNetworkAvailable(context)) {
                val response = api.findAll(sessionId)
                if (response.isSuccessful && response.body() != null) {
                    generoDao.insertAll(response.body()!!.map { it.toEntity() })
                    Log.d("GeneroRepository", "${response.body()!!.size} géneros guardados en caché")
                }
                response
            } else {
                val cachedGeneros = generoDao.getAll()
                Log.d("GeneroRepository", "${cachedGeneros.size} géneros cargados desde caché")
                Response.success(cachedGeneros.map { it.toDTO() })
            }
        } catch (e: Exception) {
            Log.e("GeneroRepository", "Error, usando caché: ${e.message}")
            val cachedGeneros = generoDao.getAll()
            Response.success(cachedGeneros.map { it.toDTO() })
        }
    }

    suspend fun findLibrosByGenero(sessionId: String, generoId: Long): Response<List<LibroDTO>> {
        return try {
            if (NetworkHelper.isNetworkAvailable(context)) {
                val response = api.findLibrosByGenero(sessionId, generoId)
                if (response.isSuccessful && response.body() != null) {
                    // Guardar libros en caché
                    libroDao.insertAll(response.body()!!.map { it.toEntity() })
                    Log.d("GeneroRepository", "Libros del género $generoId guardados en caché")
                }
                response
            } else {
                // Sin internet: devolver todos los libros en caché (no podemos filtrar por género sin más info)
                val cachedLibros = libroDao.getAll()
                Log.d("GeneroRepository", "Libros cargados desde caché (sin filtro de género)")
                Response.success(cachedLibros.map { it.toDTO() })
            }
        } catch (e: Exception) {
            Log.e("GeneroRepository", "Error, usando caché: ${e.message}")
            val cachedLibros = libroDao.getAll()
            Response.success(cachedLibros.map { it.toDTO() })
        }
    }
}
