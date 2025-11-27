package com.example.mobileapp.presentation.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R
import com.example.mobileapp.presentation.welcome.WelcomeActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Animar elementos
        animateSplash()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }, 3000)
    }

    private fun animateSplash() {
        val bgImage = findViewById<ImageView>(R.id.bgImage)
        val iconCart = findViewById<ImageView>(R.id.iconCart)
        val tvLibraryName = findViewById<TextView>(R.id.tvLibraryName)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)

        // Animación de la imagen de fondo: slide up
        val slideBg = ObjectAnimator.ofFloat(bgImage, View.TRANSLATION_Y, 100f, 0f)
        val fadeBg = ObjectAnimator.ofFloat(bgImage, View.ALPHA, 0f, 0.3f)
        
        val bgSet = AnimatorSet()
        bgSet.playTogether(slideBg, fadeBg)
        bgSet.duration = 1000
        bgSet.interpolator = AccelerateDecelerateInterpolator()

        // Animación del ícono: escala y fade in
        val scaleX = ObjectAnimator.ofFloat(iconCart, View.SCALE_X, 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(iconCart, View.SCALE_Y, 0f, 1f)
        val fadeIcon = ObjectAnimator.ofFloat(iconCart, View.ALPHA, 0f, 1f)

        val iconSet = AnimatorSet()
        iconSet.playTogether(scaleX, scaleY, fadeIcon)
        iconSet.duration = 800
        iconSet.startDelay = 400
        iconSet.interpolator = AccelerateDecelerateInterpolator()

        // Animación del nombre: fade in y slide up
        val fadeTitle = ObjectAnimator.ofFloat(tvLibraryName, View.ALPHA, 0f, 1f)
        val slideTitle = ObjectAnimator.ofFloat(tvLibraryName, View.TRANSLATION_Y, 50f, 0f)

        val titleSet = AnimatorSet()
        titleSet.playTogether(fadeTitle, slideTitle)
        titleSet.duration = 600
        titleSet.startDelay = 800

        // Animación del tagline
        val fadeTagline = ObjectAnimator.ofFloat(tvTagline, View.ALPHA, 0f, 1f)
        fadeTagline.duration = 600
        fadeTagline.startDelay = 1200

        // Ejecutar todas las animaciones
        bgSet.start()
        iconSet.start()
        titleSet.start()
        fadeTagline.start()
    }
}
