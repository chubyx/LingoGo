package com.example.lingogo

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth // <--- IMPORTANTE

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var ivLogo: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var tvLoading: TextView
    // Puedes bajar esto a 2000 o 3000 si sientes que 3.5s es mucho tiempo
    private val splashTimeOut: Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("Ajustes", MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma_seleccionado", "es") ?: "es"
        val themeId = LanguageManager.getThemeForLanguage(idiomaGuardado)
        setTheme(themeId)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        enableEdgeToEdge()

        // Asegúrate de que estos IDs existan en tu activity_splash.xml
        ivLogo = findViewById(R.id.ivLogoSplash)
        tvAppName = findViewById(R.id.tvAppNameSplash)
        progressBar = findViewById(R.id.progressBarSplash)
        tvLoading = findViewById(R.id.tvLoadingSplash)

        startEntranceAnimations()

        ivLogo.postDelayed({
            checkUserSession()
        }, splashTimeOut)
    }

    private fun startEntranceAnimations() {
        val interpolator = DecelerateInterpolator()

        val logoAnimY = ObjectAnimator.ofFloat(ivLogo, View.TRANSLATION_Y, 0f).apply { duration = 1000 }
        val logoAnimAlpha = ObjectAnimator.ofFloat(ivLogo, View.ALPHA, 1f).apply { duration = 1000 }

        val textAnimY = ObjectAnimator.ofFloat(tvAppName, View.TRANSLATION_Y, 0f).apply {
            duration = 800
            startDelay = 300
        }
        val textAnimAlpha = ObjectAnimator.ofFloat(tvAppName, View.ALPHA, 1f).apply {
            duration = 800
            startDelay = 300
        }

        val progressAnimAlpha = ObjectAnimator.ofFloat(progressBar, View.ALPHA, 1f).apply {
            duration = 600
            startDelay = 1200
        }
        val loadingTextAnimAlpha = ObjectAnimator.ofFloat(tvLoading, View.ALPHA, 1f).apply {
            duration = 600
            startDelay = 1200
        }

        AnimatorSet().apply {
            playTogether(logoAnimY, logoAnimAlpha, textAnimY, textAnimAlpha, progressAnimAlpha, loadingTextAnimAlpha)
            this.interpolator = interpolator
            start()
        }
    }

    // --- AQUÍ ESTÁ EL CAMBIO IMPORTANTE ---
    private fun checkUserSession() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // SI ESTÁ LOGUEADO -> Va directo al Inicio (Dashboard)
            val intent = Intent(this, InitionActivity::class.java)
            // Estas flags borran el historial para que no pueda volver al splash con "atrás"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            // NO ESTÁ LOGUEADO -> Va al Login/Registro (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        finish()
    }

    private fun enableEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}