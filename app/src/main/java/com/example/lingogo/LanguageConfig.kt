package com.example.lingogo

import com.example.lingogo.R

data class LanguageOption(
    val id: String,
    val name: String,
    val flagResId: Int
)

object LanguageManager {
    // LISTA DE 8 IDIOMAS
    val availableLanguages = listOf(
        LanguageOption("en", "Inglés", R.drawable.ic_flag_us),
        LanguageOption("fr", "Francés", R.drawable.ic_flag_france),
        LanguageOption("de", "Alemán", R.drawable.ic_flag_germany),
        LanguageOption("it", "Italiano", R.drawable.ic_flag_italy),
        LanguageOption("pt", "Portugués", R.drawable.ic_flag_brazil),
        LanguageOption("ja", "Japonés", R.drawable.ic_flag_japan),
        LanguageOption("zh", "Chino", R.drawable.ic_flag_china),
        LanguageOption("ru", "Ruso", R.drawable.ic_flag_russia)
    )

    fun getLanguageById(id: String): LanguageOption? {
        return availableLanguages.find { it.id == id }
    }
}