package com.example.lingogo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RegistroActivity extends AppCompatActivity {
    private TextView tvLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        //Textview para ir al login
        tvLogin = findViewById(R.id.tvLogin);
        tvLogin.setOnClickListener(view -> {
            Intent intent = new Intent(RegistroActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}
