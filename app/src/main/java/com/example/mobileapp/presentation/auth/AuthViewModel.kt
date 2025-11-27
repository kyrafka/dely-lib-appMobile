package com.example.mobileapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.example.mobileapp.data.repository.AuthRepository
import com.example.mobileapp.data.remote.model.logreg.LoginRequest
import com.example.mobileapp.data.remote.model.logreg.LoginResponse
import retrofit2.Response
import kotlinx.coroutines.launch
import android.util.Log

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _loginResult = MutableLiveData<Response<LoginResponse>>()
    val loginResult: LiveData<Response<LoginResponse>> get() = _loginResult

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Iniciando login con: $email")
                val loginRequest = LoginRequest(email, password)
                val result = repository.login(loginRequest)
                Log.d("AuthViewModel", "Respuesta: ${result.code()}, Exitoso: ${result.isSuccessful}")
                _loginResult.postValue(result)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login error: ${e.message}", e)
                // Crear una respuesta de error para notificar al UI
                val errorResponse = Response.error<LoginResponse>(500, okhttp3.ResponseBody.create(null, "Error de conexión"))
                _loginResult.postValue(errorResponse)
            }
        }
    }
}
