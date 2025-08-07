package com.asistencia.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupButtons()
    }
    
    private fun setupButtons() {
        findViewById<Button>(R.id.btnRegistrarEntrada).setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            intent.putExtra("TIPO_REGISTRO", "ENTRADA")
            startActivity(intent)
        }
        
        findViewById<Button>(R.id.btnRegistrarSalida).setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            intent.putExtra("TIPO_REGISTRO", "SALIDA")
            startActivity(intent)
        }
        
        findViewById<Button>(R.id.btnGestionPersonal).setOnClickListener {
            startActivity(Intent(this, PersonalActivity::class.java))
        }
        
        findViewById<Button>(R.id.btnReportes).setOnClickListener {
            startActivity(Intent(this, ReportesActivity::class.java))
        }
        
        findViewById<Button>(R.id.btnConfiguracion).setOnClickListener {
            startActivity(Intent(this, ConfiguracionActivity::class.java))
        }
    }
}