package com.example.mobileapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.presentation.logreg.LoginFragment
import com.example.mobileapp.presentation.ui.genero.GenerosFragment
import com.example.mobileapp.data.remote.SessionStore
import com.example.mobileapp.util.ThemeHelper

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            // Cargar sesión desde SharedPreferences
            val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            val sessionId = prefs.getString("SESSION_ID", null)
            val role = prefs.getString("USER_ROLE", null)
            // Hidratar SessionStore
            SessionStore.sessionId = sessionId
            SessionStore.rol = role

            val initialFragment = if (!sessionId.isNullOrBlank()) GenerosFragment() else LoginFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, initialFragment)
                .commit()
        }
    }
}
