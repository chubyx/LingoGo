package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val btnLogin: Button? = null
    private var tvIrRegistro: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin = findViewById<Button>(R.id.btnLogin)


        //Acción para presionar el boton
        btnLogin.setOnClickListener(View.OnClickListener { view: View? ->
            val intent = Intent(this@MainActivity, InitionActivity::class.java)
            startActivity(intent)
        })

        //TextView para Ir al Registro
        tvIrRegistro = findViewById<TextView>(R.id.tvIrRegistro)
        tvIrRegistro!!.setOnClickListener(View.OnClickListener { view: View? ->
            val intent = Intent(this@MainActivity, RegistroActivity::class.java)
            startActivity(intent)
        })
    }
}