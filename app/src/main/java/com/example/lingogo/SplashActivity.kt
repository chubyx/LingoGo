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

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var ivLogo: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var tvLoading: TextView
    private val splashTimeOut: Long = 3500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        enableEdgeToEdge()

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

    private fun checkUserSession() {
        // Redirigimos siempre a MainActivity para evitar crashes
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
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