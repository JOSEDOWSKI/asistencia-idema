package com.asistencia.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.asistencia.app.repository.AsistenciaRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var repository: AsistenciaRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        repository = AsistenciaRepository(this)
        
        setupButtons()
        initializeApp()
    }
    
    private fun setupButtons() {
        // Scanner con detección automática de evento
        findViewById<Button>(R.id.btnScanner).setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }
        
        // Nueva gestión de empleados
        findViewById<Button>(R.id.btnGestionEmpleados).setOnClickListener {
            startActivity(Intent(this, EmpleadosActivity::class.java))
        }
        
        // Reportes (mantener la actividad existente por ahora)
        findViewById<Button>(R.id.btnReportes).setOnClickListener {
            startActivity(Intent(this, ReportesActivity::class.java))
        }
        
        // Nueva configuración
        findViewById<Button>(R.id.btnConfiguracion).setOnClickListener {
            startActivity(Intent(this, ConfiguracionActivity::class.java))
        }
    }
    
    private fun initializeApp() {
        lifecycleScope.launch {
            try {
                // Inicializar configuración del dispositivo
                repository.getDispositivo()
                
                // Inicializar sincronización periódica
                repository.iniciarSincronizacionPeriodica()
                
            } catch (e: Exception) {
                // Log error but don't crash the app
                e.printStackTrace()
            }
        }
    }
}