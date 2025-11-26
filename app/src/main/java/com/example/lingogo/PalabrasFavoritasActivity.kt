package com.example.lingogo

import com.example.lingogo.database.FavoriteWord
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.example.lingogo.database.AppDatabase
class PalabrasFavoritasActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var rvPalabras: RecyclerView
    private lateinit var tvNoHayPalabras: TextView
    private lateinit var palabrasAdapter: PalabrasFavoritasAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_palabras_favoritas)

        // 1. Configurar la Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 2. Obtener la instancia de la base de datos
        db = AppDatabase.getDatabase(this)

        // 3. Encontrar las vistas
        rvPalabras = findViewById(R.id.rvPalabrasFavoritas)
        tvNoHayPalabras = findViewById(R.id.tvNoHayPalabras)

        // 4. Configurar el Adapter y el RecyclerView
        setupRecyclerView()

        // 5. Cargar las palabras de la base de datos
        cargarPalabras()
    }

    private fun setupRecyclerView() {
        // Inicializamos el Adapter. Le decimos qué hacer cuando se presione "borrar"
        palabrasAdapter = PalabrasFavoritasAdapter { palabraABorrar ->
            borrarPalabra(palabraABorrar)
        }

        rvPalabras.adapter = palabrasAdapter
        rvPalabras.layoutManager = LinearLayoutManager(this)
    }

    private fun cargarPalabras() {
        // Usamos lifecycleScope para la corutina
        lifecycleScope.launch {
            // Room (getAllFavorites) es una función 'suspend', debe ir en corutina
            val listaPalabras = db.favoriteWordDao().getAllFavorites()

            // Actualizar el adapter en el hilo principal
            palabrasAdapter.submitList(listaPalabras)

            // Mostrar u ocultar el mensaje de "lista vacía"
            if (listaPalabras.isEmpty()) {
                tvNoHayPalabras.visibility = View.VISIBLE
                rvPalabras.visibility = View.GONE
            } else {
                tvNoHayPalabras.visibility = View.GONE
                rvPalabras.visibility = View.VISIBLE
            }
        }
    }

    private fun borrarPalabra(palabra: FavoriteWord) {
        lifecycleScope.launch {
            // Llamamos a la función de Room para borrar
            db.favoriteWordDao().removeFavorite(palabra.word)

            // Volvemos a cargar la lista para que se actualice
            cargarPalabras()
        }
    }

    // Para que la flecha de "atrás" en la toolbar funcione
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}