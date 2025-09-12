package com.example.lingogo.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 1. Extensión para crear el DataStore (solo una instancia)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    // 2. Creamos la "llave" para nuestro dato
    companion object {
        // Usamos un Booleano: true = Modo Oscuro, false = Modo Claro
        val IS_DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")
    }

    // 3. Función para GUARDAR el modo oscuro
    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_DARK_MODE_KEY] = isDarkMode
        }
    }

    // 4. Flow para LEER el modo oscuro (se actualiza automáticamente)
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            // Si no existe, devuelve 'false' (valor por defecto = Claro)
            preferences[IS_DARK_MODE_KEY] ?: false
        }
}