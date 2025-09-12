package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RegistroActivity : AppCompatActivity() {
    private var tvLogin: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        //Textview para ir al login
        tvLogin = findViewById<TextView>(R.id.tvLogin)
        tvLogin!!.setOnClickListener(View.OnClickListener { view: View? ->
            val intent = Intent(this@RegistroActivity, MainActivity::class.java)
            startActivity(intent)
        })
    }
}
