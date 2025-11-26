package com.example.lingogo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button btnLogin;
    private TextView tvIrRegistro;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button btnLogin = findViewById(R.id.btnLogin);


        //Acción para presionar el boton
        btnLogin.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, InitionActivity.class);
            startActivity(intent);
        });

        //TextView para Ir al Registro
        tvIrRegistro = findViewById(R.id.tvIrRegistro);
        tvIrRegistro.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RegistroActivity.class);
            startActivity(intent);
        });
    }
}