package com.example.lingogo.games

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.lingogo.LanguageManager
import com.example.lingogo.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class StoryBuilderActivity : AppCompatActivity() {

    private val viewModel: StoryBuilderViewModel by viewModels()

    private lateinit var tvSentenceDisplay: TextView
    private lateinit var chipGroupOptions: ChipGroup
    private lateinit var btnCheck: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var tvFeedback: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_builder)

        // Enlazar Vistas
        tvSentenceDisplay = findViewById(R.id.tvSentenceDisplay)
        chipGroupOptions = findViewById(R.id.chipGroupOptions)
        btnCheck = findViewById(R.id.btnCheck)
        btnNext = findViewById(R.id.btnNext)
        tvFeedback = findViewById(R.id.tvFeedback)

        setupObservers()
        setupListeners()
    }

    private fun setupListeners() {
        btnCheck.setOnClickListener {
            viewModel.checkAnswer()
        }

        btnNext.setOnClickListener {
            viewModel.nextSentence()
            resetUI()
        }
    }

    private fun setupObservers() {
        // 1. Observar la frase actual
        viewModel.currentSentence.observe(this) { sentence ->
            renderSentence(sentence.part1, sentence.part2, null)
            createOptionChips(sentence.options)
        }

        // 2. Observar la selección del usuario (para actualizar la frase)
        viewModel.userSelection.observe(this) { selection ->
            val current = viewModel.currentSentence.value
            if (current != null) {
                // Si hay selección, la mostramos en el hueco. Si no, mostramos "___"
                renderSentence(current.part1, current.part2, selection)
                btnCheck.isEnabled = (selection != null)
            }
        }

        // 3. Observar si es correcto
        viewModel.isCorrect.observe(this) { correct ->
            // Este observer se dispara cuando verificamos
        }

        // 4. Feedback
        viewModel.feedback.observe(this) { status ->
            if (status == "CORRECT") {
                tvFeedback.text = getString(R.string.story_correcto)
                tvFeedback.setTextColor(getColor(android.R.color.holo_green_dark))
                btnCheck.visibility = View.GONE
                btnNext.visibility = View.VISIBLE
                chipGroupOptions.isEnabled = false // Bloquear cambios
            } else if (status == "INCORRECT") {
                tvFeedback.text = getString(R.string.story_incorrecto)
                tvFeedback.setTextColor(getColor(android.R.color.holo_red_dark))
            } else {
                tvFeedback.text = ""
            }
        }
    }

    private fun renderSentence(part1: String, part2: String, selection: String?) {
        val middle = selection ?: "______"
        val text = "$part1 $middle $part2"
        tvSentenceDisplay.text = text
    }

    private fun createOptionChips(options: List<String>) {
        chipGroupOptions.removeAllViews()
        for (option in options) {
            val chip = Chip(this)
            chip.text = option
            chip.isCheckable = true
            chip.setOnClickListener {
                viewModel.selectOption(option)
                // Limpiar feedback al cambiar opción
                tvFeedback.text = ""
            }
            chipGroupOptions.addView(chip)
        }
    }

    private fun resetUI() {
        btnNext.visibility = View.GONE
        btnCheck.visibility = View.VISIBLE
        btnCheck.isEnabled = false
        tvFeedback.text = ""
        chipGroupOptions.clearCheck()
    }
}