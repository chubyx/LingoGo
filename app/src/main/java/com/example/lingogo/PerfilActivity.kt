package com.example.lingogo

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PerfilActivity : AppCompatActivity() {
    private var spinnerTags: Spinner? = null
    private var btnAgregarTag: Button? = null
    private var contenedorTags: LinearLayout? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        spinnerTags = findViewById<Spinner>(R.id.spinnerTags)
        btnAgregarTag = findViewById<Button>(R.id.btnAgregarTag)
        contenedorTags = findViewById<LinearLayout>(R.id.contenedorTags)

        btnAgregarTag!!.setOnClickListener(View.OnClickListener { v: View? ->
            val tagSeleccionado = spinnerTags!!.getSelectedItem().toString()
            val nuevoTag = TextView(this)
            nuevoTag.setText(tagSeleccionado)
            nuevoTag.setPadding(16, 8, 16, 8)
            nuevoTag.setBackgroundResource(R.drawable.bg_tag)
            nuevoTag.setTextColor(Color.WHITE)
            contenedorTags!!.addView(nuevoTag)
        })
    }
}
