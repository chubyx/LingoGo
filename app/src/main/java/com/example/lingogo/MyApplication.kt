package com.example.lingogo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.lingogo.database.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MyApplication : Application() {

    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate() {
        super.onCreate()

        settingsDataStore = SettingsDataStore(this)

        // Usamos runBlocking para forzar a la app a esperar
        // a que se carguen las preferencias ANTES de continuar.
        runBlocking {
            // 1. Cargar el Tema
            val isDarkMode = settingsDataStore.isDarkMode.first()
            val themeMode = if (isDarkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(themeMode)

            // 2. Cargar el Idioma
            val languageCode = settingsDataStore.language.first()
            if (languageCode.isNotEmpty()) {
                val appLocale = LocaleListCompat.forLanguageTags(languageCode)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
        }
    }
}