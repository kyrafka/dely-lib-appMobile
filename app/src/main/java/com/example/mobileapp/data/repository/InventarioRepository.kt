package com.example.mobileapp.data.repository

import android.content.Context
import android.util.Log
import com.example.mobileapp.data.local.AppDatabase
import com.example.mobileapp.data.local.entity.toDTO
import com.example.mobileapp.data.local.entity.toEntity
import com.example.mobileapp.data.remote.api.InventarioApi
import com.example.mobileapp.data.remote.model.inventario.InventarioDTO
import com.example.mobileapp.util.NetworkHelper
import retrofit2.Response

class InventarioRepository(
    private val api: InventarioApi,
    private val context: Context
) {
    private val db = AppDatabase.getDatabase(context)
    private val inventarioDao = db.inventarioDao()

    suspend fun createInventario(sessionId: String, inventario: InventarioDTO): Response<InventarioDTO> {
        return if (NetworkHelper.isNetworkAvailable(context)) {
            api.createInventario(sessionId, inventario)
        } else {
            Response.error(503, okhttp3.ResponseBody.create(null, "Sin conexión"))
        }
    }

    suspend fun findAll(sessionId: String): Response<List<InventarioDTO>> {
        return try {
            if (NetworkHelper.isNetworkAvailable(context)) {
                val response = api.findAll(sessionId)
                if (response.isSuccessful && response.body() != null) {
                    inventarioDao.insertAll(response.body()!!.map { it.toEntity() })
                    Log.d("InventarioRepository", "${response.body()!!.size} items guardados en caché")
                }
                response
            } else {
                val cachedInventario = inventarioDao.getAll()
                Log.d("InventarioRepository", "${cachedInventario.size} items cargados desde caché")
                Response.success(cachedInventario.map { it.toDTO() })
            }
        } catch (e: Exception) {
            Log.e("InventarioRepository", "Error, usando caché: ${e.message}")
            val cachedInventario = inventarioDao.getAll()
            Response.success(cachedInventario.map { it.toDTO() })
        }
    }

    suspend fun findById(sessionId: String, id: Long): Response<InventarioDTO> {
        return try {
            if (NetworkHelper.isNetworkAvailable(context)) {
                val response = api.findById(sessionId, id)
                if (response.isSuccessful && response.body() != null) {
                    inventarioDao.insertAll(listOf(response.body()!!.toEntity()))
                }
                response
            } else {
                val cached = inventarioDao.getById(id)
                if (cached != null) {
                    Response.success(cached.toDTO())
                } else {
                    Response.error(404, okhttp3.ResponseBody.create(null, "Not found"))
                }
            }
        } catch (e: Exception) {
            val cached = inventarioDao.getById(id)
            if (cached != null) {
                Response.success(cached.toDTO())
            } else {
                Response.error(500, okhttp3.ResponseBody.create(null, "Error: ${e.message}"))
            }
        }
    }

    suspend fun findByLibroId(sessionId: String, libroId: Long): Response<InventarioDTO> {
        return try {
            if (NetworkHelper.isNetworkAvailable(context)) {
                val response = api.findByLibroId(sessionId, libroId)
                if (response.isSuccessful && response.body() != null) {
                    inventarioDao.insertAll(listOf(response.body()!!.toEntity()))
                }
                response
            } else {
                val cached = inventarioDao.getByLibroId(libroId)
                if (cached != null) {
                    Response.success(cached.toDTO())
                } else {
                    Response.error(404, okhttp3.ResponseBody.create(null, "Not found"))
                }
            }
        } catch (e: Exception) {
            val cached = inventarioDao.getByLibroId(libroId)
            if (cached != null) {
                Response.success(cached.toDTO())
            } else {
                Response.error(500, okhttp3.ResponseBody.create(null, "Error: ${e.message}"))
            }
        }
    }

    suspend fun updateInventario(sessionId: String, id: Long, inventario: InventarioDTO): Response<InventarioDTO> {
        return if (NetworkHelper.isNetworkAvailable(context)) {
            api.updateInventario(sessionId, id, inventario)
        } else {
            Response.error(503, okhttp3.ResponseBody.create(null, "Sin conexión"))
        }
    }

    suspend fun deleteInventario(sessionId: String, id: Long): Response<Unit> {
        return if (NetworkHelper.isNetworkAvailable(context)) {
            api.deleteInventario(sessionId, id)
        } else {
            Response.error(503, okhttp3.ResponseBody.create(null, "Sin conexión"))
        }
    }
}
