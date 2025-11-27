package com.example.mobileapp.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "language"
    
    const val SPANISH = "es"
    const val ENGLISH = "en"
    
    fun setLocale(context: Context, languageCode: String): Context {
        saveLanguage(context, languageCode)
        return updateResources(context, languageCode)
    }
    
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, SPANISH) ?: SPANISH
    }
    
    private fun saveLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
    
    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
}
