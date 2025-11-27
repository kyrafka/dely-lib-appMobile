package com.example.mobileapp.presentation.books

import androidx.lifecycle.*
import com.example.mobileapp.data.remote.model.LibroDTO
import com.example.mobileapp.data.remote.model.genero.GeneroDTO
import com.example.mobileapp.data.repository.GeneroRepository
import kotlinx.coroutines.launch

class GeneroViewModel(private val repo: GeneroRepository) : ViewModel() {

    private val _generos = MutableLiveData<List<GeneroDTO>>()
    val generos: LiveData<List<GeneroDTO>> = _generos

    private val _librosPorGenero = MutableLiveData<Map<Long, List<LibroDTO>>>(emptyMap())
    val librosPorGenero: LiveData<Map<Long, List<LibroDTO>>> = _librosPorGenero

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun cargarGeneros(sessionId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                
                android.util.Log.d("GeneroViewModel", "Cargando géneros con sessionId: $sessionId")
                
                val response = repo.findAllGeneros(sessionId)
                
                android.util.Log.d("GeneroViewModel", "Response code: ${response.code()}")
                android.util.Log.d("GeneroViewModel", "Response successful: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val generos = response.body() ?: emptyList()
                    android.util.Log.d("GeneroViewModel", "Géneros recibidos: ${generos.size}")
                    generos.forEach { genero ->
                        android.util.Log.d("GeneroViewModel", "Género: ${genero.nombre} (ID: ${genero.idGenero})")
                    }
                    _generos.value = generos
                    
                    if (generos.isEmpty()) {
                        _error.value = "No hay géneros disponibles en el sistema"
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("GeneroViewModel", "Error response: $errorBody")
                    _error.value = "Error al cargar géneros: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                android.util.Log.e("GeneroViewModel", "Exception al cargar géneros", e)
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun cargarLibrosPorGenero(sessionId: String, generoId: Long) {
        viewModelScope.launch {
            try {
                val response = repo.findLibrosByGenero(sessionId, generoId)
                if (response.isSuccessful) {
                    val libros = response.body() ?: emptyList()
                    val currentMap = _librosPorGenero.value?.toMutableMap() ?: mutableMapOf()
                    currentMap[generoId] = libros
                    _librosPorGenero.value = currentMap
                } else {
                    _error.value = "Error al cargar libros del género $generoId"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
