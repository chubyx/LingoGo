package com.example.lingogo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button btnLogin;
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

    }
}