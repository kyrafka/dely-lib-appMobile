package com.example.mobileapp.data.repository

import com.example.mobileapp.data.model.RegistroClienteRequest
import com.example.mobileapp.data.remote.api.AuthApi
import com.example.mobileapp.data.remote.model.logreg.LoginRequest
import com.example.mobileapp.data.remote.model.logreg.LoginResponse
import retrofit2.Response

class AuthRepository(private val api: AuthApi) {

    /** Login: llama al endpoint /login */
    suspend fun login(req: LoginRequest): Response<LoginResponse> {
        if (com.example.mobileapp.data.remote.SessionStore.isOfflineMode) {
            return Response.success(com.example.mobileapp.data.local.MockData.mockUser)
        }
        return api.login(req)
    }

    /** Registro: llama al endpoint /register */
    suspend fun registerCliente(request: RegistroClienteRequest): Response<Long> {
        return api.register(request)
    }
}
