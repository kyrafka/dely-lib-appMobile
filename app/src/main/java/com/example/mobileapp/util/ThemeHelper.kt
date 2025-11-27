package com.example.mobileapp.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    
    const val MODE_LIGHT = 0
    const val MODE_DARK = 1
    const val MODE_SYSTEM = 2
    
    fun applyTheme(mode: Int) {
        when (mode) {
            MODE_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            MODE_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            MODE_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    fun saveThemeMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        applyTheme(mode)
    }
    
    fun getThemeMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM)
    }
    
    fun isDarkMode(context: Context): Boolean {
        return when (getThemeMode(context)) {
            MODE_DARK -> true
            MODE_LIGHT -> false
            else -> {
                val nightMode = context.resources.configuration.uiMode and 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}
