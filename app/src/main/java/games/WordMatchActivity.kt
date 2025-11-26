package com.example.lingogo.games
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.lingogo.LanguageManager
import com.example.lingogo.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class WordMatchActivity : AppCompatActivity() {

    private val viewModel: WordMatchViewModel by viewModels()

    private lateinit var tvProgress: TextView
    private lateinit var llLeftColumn: LinearLayout
    private lateinit var llRightColumn: LinearLayout

    // Mapas para rastrear los botones creados (ID del par -> Botón)
    private val leftButtons = mutableMapOf<Int, MaterialButton>()
    private val rightButtons = mutableMapOf<Int, MaterialButton>()

    // Rastreo visual de selección
    private var selectedBtnLeft: MaterialButton? = null
    private var selectedBtnRight: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_match)

        tvProgress = findViewById(R.id.tvProgress)
        llLeftColumn = findViewById(R.id.llLeftColumn)
        llRightColumn = findViewById(R.id.llRightColumn)

        setupObservers()

        // Iniciar juego
        viewModel.setupGame("English", "Easy")
    }

    private fun setupObservers() {
        viewModel.progressText.observe(this) { tvProgress.text = it }

        // Cargar columna izquierda
        viewModel.leftColumn.observe(this) { list ->
            llLeftColumn.removeAllViews()
            leftButtons.clear()
            list.forEach { pair ->
                val btn = createGameButton(pair.textA)
                btn.setOnClickListener {
                    handleSelectionLeft(btn, pair.id)
                }
                llLeftColumn.addView(btn)
                leftButtons[pair.id] = btn
            }
        }

        // Cargar columna derecha
        viewModel.rightColumn.observe(this) { list ->
            llRightColumn.removeAllViews()
            rightButtons.clear()
            list.forEach { pair ->
                val btn = createGameButton(pair.textB)
                btn.setOnClickListener {
                    handleSelectionRight(btn, pair.id)
                }
                llRightColumn.addView(btn)
                rightButtons[pair.id] = btn
            }
        }

        // Manejar estados del juego
        viewModel.gameState.observe(this) { state ->
            when (state) {
                is WordMatchViewModel.MatchState.MATCHED -> {
                    onMatchSuccess(state.id)
                }
                is WordMatchViewModel.MatchState.MISMATCH -> {
                    onMatchError()
                }
                is WordMatchViewModel.MatchState.LEVEL_COMPLETE -> {
                    showLevelCompleteDialog(state.nextLevel)
                }
                is WordMatchViewModel.MatchState.ALL_COMPLETE -> {
                    showAllCompleteDialog()
                }
                else -> {}
            }
        }
    }

    // --- Helpers de UI ---

    private fun createGameButton(text: String): MaterialButton {
        val btn = MaterialButton(this)
        btn.text = text
        btn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 8, 0, 8)
        }
        // Estilo base (Outlined)
        // Nota: Para hacerlo 100% Material 3 programáticamente a veces es complejo,
        // aquí configuramos propiedades básicas.
        btn.setBackgroundColor(Color.WHITE)
        btn.setTextColor(getColor(R.color.naranja)) // Usa tu color
        btn.strokeWidth = 2
        btn.strokeColor = getColorStateList(R.color.naranja)
        return btn
    }

    private fun handleSelectionLeft(btn: MaterialButton, id: Int) {
        // Reset visual anterior
        selectedBtnLeft?.setBackgroundColor(Color.WHITE)
        selectedBtnLeft?.setTextColor(getColor(R.color.naranja))

        // Marcar nuevo
        selectedBtnLeft = btn
        btn.setBackgroundColor(getColor(R.color.naranja)) // Relleno al seleccionar
        btn.setTextColor(Color.WHITE)

        viewModel.selectItemLeft(id)
    }

    private fun handleSelectionRight(btn: MaterialButton, id: Int) {
        // Reset visual anterior
        selectedBtnRight?.setBackgroundColor(Color.WHITE)
        selectedBtnRight?.setTextColor(getColor(R.color.naranja))

        // Marcar nuevo
        selectedBtnRight = btn
        btn.setBackgroundColor(getColor(R.color.naranja))
        btn.setTextColor(Color.WHITE)

        viewModel.selectItemRight(id)
    }

    private fun onMatchSuccess(id: Int) {
        // Feedback visual verde
        val btnL = leftButtons[id]
        val btnR = rightButtons[id]

        btnL?.setBackgroundColor(Color.GREEN)
        btnR?.setBackgroundColor(Color.GREEN)

        // Desaparecer después de un momento
        Handler(Looper.getMainLooper()).postDelayed({
            btnL?.visibility = View.INVISIBLE
            btnR?.visibility = View.INVISIBLE

            // Resetear referencias locales visuales
            selectedBtnLeft = null
            selectedBtnRight = null

            // --- ¡LA SOLUCIÓN! Desbloqueamos el ViewModel ---
            viewModel.resetGameState()

        }, 500)
    }

    private fun onMatchError() {
        // Feedback visual rojo
        selectedBtnLeft?.setBackgroundColor(Color.RED)
        selectedBtnRight?.setBackgroundColor(Color.RED)

        // Volver a la normalidad
        Handler(Looper.getMainLooper()).postDelayed({
            // Restaurar color original (blanco con texto naranja)
            selectedBtnLeft?.setBackgroundColor(Color.WHITE)
            selectedBtnLeft?.setTextColor(getColor(R.color.naranja)) // Asegúrate que este color exista

            selectedBtnRight?.setBackgroundColor(Color.WHITE)
            selectedBtnRight?.setTextColor(getColor(R.color.naranja))

            selectedBtnLeft = null
            selectedBtnRight = null

            // --- ¡LA SOLUCIÓN! Desbloqueamos el ViewModel ---
            viewModel.resetGameState()

        }, 500)
    }

    private fun showLevelCompleteDialog(nextLevel: String?) {
        MaterialAlertDialogBuilder(this)
            .setTitle("¡Nivel Completado!")
            .setMessage("¡Buen trabajo! ¿Listo para el siguiente?")
            .setPositiveButton("Siguiente Nivel") { _, _ ->
                if (nextLevel != null) viewModel.loadNextLevel()
            }
            .setCancelable(false)
            .show()
    }

    private fun showAllCompleteDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("¡Felicidades!")
            .setMessage("Has completado todos los niveles de Conecta Palabras.")
            .setPositiveButton("Reiniciar") { _, _ ->
                viewModel.restartGame()
            }
            .setNegativeButton("Salir") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}