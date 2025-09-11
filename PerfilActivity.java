package com.example.lingogo;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PerfilActivity extends AppCompatActivity {

    private Spinner spinnerTags;
    private Button btnAgregarTag;
    private LinearLayout contenedorTags;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        spinnerTags = findViewById(R.id.spinnerTags);
        btnAgregarTag = findViewById(R.id.btnAgregarTag);
        contenedorTags = findViewById(R.id.contenedorTags);

        btnAgregarTag.setOnClickListener(v ->{
            String tagSeleccionado = spinnerTags.getSelectedItem().toString();

            TextView nuevoTag = new TextView(this);
            nuevoTag.setText(tagSeleccionado);
            nuevoTag.setPadding(16, 8, 16, 8);
            nuevoTag.setBackgroundResource(R.drawable.bg_tag);
            nuevoTag.setTextColor(Color.WHITE);

            contenedorTags.addView(nuevoTag);
        });

    }
}
