package com.example.mobileapp.data.repository

import android.content.Context
import android.util.Log
import com.example.mobileapp.data.local.AppDatabase
import com.example.mobileapp.data.local.entity.toDTO
import com.example.mobileapp.data.local.entity.toEntity
import com.example.mobileapp.data.remote.api.LibroApi
import com.example.mobileapp.data.remote.model.LibroDTO
import com.example.mobileapp.util.NetworkHelper
import retrofit2.Response

class LibroRepository(
    private val api: LibroApi,
    private val context: Context
) {
    private val db = AppDatabase.getDatabase(context)
    private val libroDao = db.libroDao()

    suspend fun findById(sessionId: String, id: Long): Response<LibroDTO> {
        return api.findById(sessionId, id)
    }

    suspend fun findAll(sessionId: String): Response<List<LibroDTO>> {
        return api.findAll(sessionId)
    }

    suspend fun deleteLibro(sessionId: String, id: Long): Response<Unit> {
        return api.deleteLibro(sessionId, id)
    }
}
