package com.asistencia.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.asistencia.app.repository.AsistenciaRepository
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    
    private lateinit var repository: AsistenciaRepository
    private val splashTimeOut = 3000L // 3 segundos
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        repository = AsistenciaRepository(this)
        
        setupAnimations()
        initializeApp()
    }
    
    private fun setupAnimations() {
        // Animación del logo
        val logoImageView = findViewById<ImageView>(R.id.iv_logo)
        val fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeInAnimation.duration = 1000
        logoImageView.startAnimation(fadeInAnimation)
        
        // Animación del texto
        val appNameTextView = findViewById<TextView>(R.id.tv_app_name)
        val slideInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        slideInAnimation.duration = 800
        slideInAnimation.startOffset = 500
        appNameTextView.startAnimation(slideInAnimation)
        
        // Animación del subtítulo
        val subtitleTextView = findViewById<TextView>(R.id.tv_subtitle)
        val slideInRightAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        slideInRightAnimation.duration = 800
        slideInRightAnimation.startOffset = 700
        subtitleTextView.startAnimation(slideInRightAnimation)
    }
    
    private fun initializeApp() {
        lifecycleScope.launch {
            try {
                // Actualizar texto de carga
                updateLoadingText(getString(R.string.splash_loading_database))
                
                // Inicializar configuración del dispositivo
                repository.getDispositivo()
                
                updateLoadingText(getString(R.string.splash_loading_sync))
                
                // Inicializar sincronización periódica
                repository.iniciarSincronizacionPeriodica()
                
                updateLoadingText(getString(R.string.splash_loading_ui))
                
                // Simular tiempo de carga mínimo para mostrar el splash
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToMain()
                }, splashTimeOut)
                
            } catch (e: Exception) {
                // En caso de error, continuar a la actividad principal
                e.printStackTrace()
                updateLoadingText(getString(R.string.splash_loading_finish))
                
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToMain()
                }, 1000)
            }
        }
    }
    
    private fun updateLoadingText(text: String) {
        runOnUiThread {
            findViewById<TextView>(R.id.tv_loading).text = text
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        
        // Animación de transición suave
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    override fun onBackPressed() {
        // Deshabilitar el botón de retroceso en el splash
        // No hacer nada
    }
}