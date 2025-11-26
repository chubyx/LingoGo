package com.example.lingogo

import com.example.lingogo.R

// Agregamos 'themeResId' a la clase de datos
data class LanguageOption(
    val id: String,
    val name: String,
    val flagResId: Int,
    val themeResId: Int // <--- NUEVO
)

object LanguageManager {
    val availableLanguages = listOf(
        // Español usa el tema por defecto (Theme.LingoGo)
        LanguageOption("es", "Español", R.drawable.ic_flag_spain, R.style.Theme_LingoGo),
        LanguageOption("en", "Inglés", R.drawable.ic_flag_us, R.style.Theme_LingoGo_English),
        LanguageOption("fr", "Francés", R.drawable.ic_flag_france, R.style.Theme_LingoGo_French),
        LanguageOption("de", "Alemán", R.drawable.ic_flag_germany, R.style.Theme_LingoGo_German),
        LanguageOption("it", "Italiano", R.drawable.ic_flag_italy, R.style.Theme_LingoGo_Italian),
        LanguageOption("pt", "Portugués", R.drawable.ic_flag_brazil, R.style.Theme_LingoGo_Portuguese),
        LanguageOption("ja", "Japonés", R.drawable.ic_flag_japan, R.style.Theme_LingoGo_Japanese),
        LanguageOption("zh", "Chino", R.drawable.ic_flag_china, R.style.Theme_LingoGo_Chinese),
        LanguageOption("ru", "Ruso", R.drawable.ic_flag_russia, R.style.Theme_LingoGo_Russian)
    )

    fun getLanguageById(id: String): LanguageOption? {
        return availableLanguages.find { it.id == id }
    }

    // Método de utilidad para la Activity
    fun getThemeForLanguage(langCode: String): Int {
        return getLanguageById(langCode)?.themeResId ?: R.style.Theme_LingoGo
    }
}