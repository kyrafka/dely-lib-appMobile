package com.example.mobileapp.presentation.logreg

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.mobileapp.R
import com.example.mobileapp.data.remote.SessionStore
import com.example.mobileapp.data.remote.RetrofitClient
import com.example.mobileapp.data.repository.AuthRepository
import com.example.mobileapp.presentation.auth.AuthViewModel
import com.example.mobileapp.presentation.auth.AuthViewModelFactory
import com.example.mobileapp.presentation.ui.genero.GenerosFragment

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthRepository(RetrofitClient.authApi))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailEt = view.findViewById<EditText>(R.id.etCorreo)
        val passEt = view.findViewById<EditText>(R.id.etPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)

        // Forzar foco y mostrar teclado al abrir
        emailEt.post {
            emailEt.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(emailEt, InputMethodManager.SHOW_IMPLICIT)
        }

        // Botón de Login
        btnLogin.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val pass = passEt.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Completa los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Iniciando sesión...", Toast.LENGTH_SHORT).show()

            // Ejecutar login
            viewModel.login(email, pass)
        }

        // Botón de registro
        btnRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SignUpFragment())
                .addToBackStack(null)
                .commit()
        }

        // Observamos resultado del login
        viewModel.loginResult.observe(viewLifecycleOwner) { response ->
            try {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // Guardar sesión en SharedPreferences
                    val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("SESSION_ID", body.sessionId)
                        putString("USER_ID", body.userId.toString())
                        putString("USER_NAME", body.nombre)
                        putString("USER_ROLE", body.rol)
                        apply()
                    }

                    // ⚡ Guardar también en SessionStore (para usar en otras pantallas)
                    SessionStore.sessionId = body.sessionId
                    SessionStore.rol = body.rol
                    SessionStore.isOfflineMode = false // Asegurar que no esté en offline si loguea bien

                    Log.d("LoginFragment", "Session ID recibido: ${body.sessionId}")

                    // Mostrar bienvenida
                    Toast.makeText(requireContext(), "Bienvenido ${body.nombre}", Toast.LENGTH_SHORT).show()
                    // Navegar a LibrosFragment
                    activity?.supportFragmentManager?.beginTransaction()
                        ?.replace(R.id.fragmentContainer, GenerosFragment())
                        ?.commit()

                } else {
                    val message = when (response.code()) {
                        400 -> "Petición inválida."
                        401 -> "Correo o contraseña incorrecta."
                        404 -> "Usuario no encontrado."
                        500 -> "Error del servidor, inténtalo más tarde."
                        else -> "Error ${response.code()}: ${response.errorBody()?.string() ?: "Desconocido"}"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    Log.e("LoginFragment", "Login failed: $message")
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("LoginFragment", "Exception", e)
            }
        }
    }
}