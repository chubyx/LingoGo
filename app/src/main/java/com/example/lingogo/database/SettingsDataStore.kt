package com.example.lingogo.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey // ¡NUEVO!
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        // Llave para el Modo Oscuro
        val IS_DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")

        // Llave para el Idioma
        // Guardaremos el código ("es", "en", "fr", etc.)
        val LANGUAGE_KEY = stringPreferencesKey("language_code")
    }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_DARK_MODE_KEY] = isDarkMode
        }
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_DARK_MODE_KEY] ?: false
        }

    // FUNCIONES PARA IDIOMA

    // Función para GUARDAR el idioma
    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { settings ->
            settings[LANGUAGE_KEY] = languageCode
        }
    }

    // Flow para LEER el idioma
    val language: Flow<String> = context.dataStore.data
        .map { preferences ->
            // Si no existe, devuelve "es" (Español) como valor por defecto
            preferences[LANGUAGE_KEY] ?: "es"
        }
}