package com.example.lingogo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class InitionActivity : AppCompatActivity() {
    private var cardViewComunidad: CardView? = null
    private var cardViewConfig: CardView? = null
    private var cardGames: CardView? = null
    private var cardPerfil: CardView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        cardViewComunidad = findViewById<CardView>(R.id.cardComunidad)
        cardViewConfig = findViewById<CardView>(R.id.cardConfiguracion)
        cardGames = findViewById<CardView>(R.id.cardGames)
        cardPerfil = findViewById<CardView>(R.id.cardPerfil)

        //Acción para presionar
        cardViewComunidad!!.setOnClickListener(View.OnClickListener { view: View? ->
            val intent = Intent(this@InitionActivity, CommunityActivity::class.java)
            startActivity(intent)
        })

        cardViewConfig!!.setOnClickListener(View.OnClickListener { view: View? ->
            val intent = Intent(this@InitionActivity, ConfigActivity::class.java)
            startActivity(intent)
        })

        cardGames!!.setOnClickListener(View.OnClickListener { view: View? ->
            val intent = Intent(this@InitionActivity, GamesActivity::class.java)
            startActivity(intent)
        })
        cardPerfil!!.setOnClickListener(View.OnClickListener { view: View? ->
            val intent = Intent(this@InitionActivity, PerfilActivity::class.java)
            startActivity(intent)
        })
    }
}
