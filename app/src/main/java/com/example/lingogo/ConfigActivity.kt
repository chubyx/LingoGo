package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth

class ConfigActivity : AppCompatActivity() {

    private val TAG = "ConfigActivity"

    // Vistas
    private lateinit var toolbar: Toolbar
    private lateinit var spinnerIdioma: Spinner
    private lateinit var switchNotificaciones: SwitchMaterial
    private lateinit var radioTema: RadioGroup

    // Firebase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarConfig)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Configuración"

        // Enlazar Vistas
        spinnerIdioma = findViewById(R.id.spinnerIdioma)
        switchNotificaciones = findViewById(R.id.switchNotificaciones)
        radioTema = findViewById(R.id.radioTema)

        // (Opcional) Poblar Spinner de Idioma desde aquí (si no usas @array/idiomas)
        // val adapter = ArrayAdapter.createFromResource(this, R.array.idiomas, android.R.layout.simple_spinner_item)
        // adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // spinnerIdioma.adapter = adapter

        // Cargar las preferencias guardadas (A futuro)
        // cargarPreferencias()

        // Configurar Listeners
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        // Comprobar si el usuario está logueado
        if (auth.currentUser == null) {
            irALogin()
        }
    }

    private fun setupListeners() {
        // Listener para el Spinner de Idioma
        spinnerIdioma.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val seleccionado = parent?.getItemAtPosition(position).toString()
                if (seleccionado != "Español") { // Evitar el toast la primera vez
                    Log.d(TAG, "Idioma seleccionado: $seleccionado")
                    Toast.makeText(this@ConfigActivity, "Idioma cambiado a: $seleccionado (Aún no implementado)", Toast.LENGTH_SHORT).show()
                    // Aquí iría la lógica para cambiar el idioma de la app
                    // guardarPreferencia("idioma", seleccionado)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Listener para el Switch de Notificaciones
        switchNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Notificaciones: $isChecked")
            val estado = if (isChecked) "activadas" else "desactivadas"
            Toast.makeText(this, "Notificaciones $estado", Toast.LENGTH_SHORT).show()
            // Aquí iría la lógica para (des)activar notificaciones
            // guardarPreferencia("notificaciones", isChecked)
        }

        // Listener para el RadioGroup de Tema
        radioTema.setOnCheckedChangeListener { group, checkedId ->
            val tema = when (checkedId) {
                R.id.radioClaro -> "Claro"
                R.id.radioOscuro -> "Oscuro"
                else -> "Claro"
            }
            Log.d(TAG, "Tema seleccionado: $tema")
            Toast.makeText(this, "Tema cambiado a: $tema (Aún no implementado)", Toast.LENGTH_SHORT).show()
            // Aquí iría la lógica para cambiar el tema de la app (AppCompatDelegate)
            // guardarPreferencia("tema", tema)
        }
    }

    // (Función para el futuro)
    // private fun cargarPreferencias() {
    //    val prefs = getSharedPreferences("LingoGoPrefs", MODE_PRIVATE)
    //    val notificaciones = prefs.getBoolean("notificaciones", true)
    //    val tema = prefs.getString("tema", "Claro")
    //    ...
    //    switchNotificaciones.isChecked = notificaciones
    //    ...
    // }

    // (Función para el futuro)
    // private fun guardarPreferencia(key: String, value: Any) {
    //    val prefs = getSharedPreferences("LingoGoPrefs", MODE_PRIVATE).edit()
    //    when (value) {
    //        is String -> prefs.putString(key, value)
    //        is Boolean -> prefs.putBoolean(key, value)
    //    }
    //    prefs.apply()
    // }

    private fun irALogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Maneja el clic en la flecha "atrás" de la Toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
