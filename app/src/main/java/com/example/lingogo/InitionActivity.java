package com.example.lingogo;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class InitionActivity extends AppCompatActivity {
    private CardView cardViewComunidad, cardViewConfig;
    @Override

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        cardViewComunidad = findViewById(R.id.cardComunidad);
        cardViewConfig = findViewById(R.id.cardConfiguracion);

        //Acción para presionar
        cardViewComunidad.setOnClickListener(view -> {
            Intent intent = new Intent(InitionActivity.this, CommunityActivity.class);
            startActivity(intent);
        });

        cardViewConfig.setOnClickListener(view -> {
            Intent intent = new Intent(InitionActivity.this, ConfigActivity.class);
            startActivity(intent);
        });
    }
}
