package com.asistencia.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.asistencia.app.utils.PinManager

class PinActivity : AppCompatActivity() {
    
    private lateinit var etPin: EditText
    private lateinit var btnConfirmar: Button
    private lateinit var btnCancelar: Button
    private lateinit var tvMensaje: TextView
    private lateinit var tvIntentos: TextView
    
    private var intentosRestantes = 3
    private var targetActivity: String? = null
    
    companion object {
        const val EXTRA_TARGET_ACTIVITY = "target_activity"
        const val EXTRA_TARGET_ACTIVITY_NAME = "target_activity_name"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)
        
        // Obtener actividad destino
        targetActivity = intent.getStringExtra(EXTRA_TARGET_ACTIVITY)
        
        setupViews()
        setupListeners()
        
        // Configurar título
        val activityName = intent.getStringExtra(EXTRA_TARGET_ACTIVITY_NAME) ?: "Función"
        supportActionBar?.title = "🔐 PIN - $activityName"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Enfocar en el campo PIN
        etPin.requestFocus()
    }
    
    private fun setupViews() {
        etPin = findViewById(R.id.et_pin)
        btnConfirmar = findViewById(R.id.btn_confirmar_pin)
        btnCancelar = findViewById(R.id.btn_cancelar_pin)
        tvMensaje = findViewById(R.id.tv_mensaje_pin)
        tvIntentos = findViewById(R.id.tv_intentos_pin)
        
        // Configurar campo PIN
        etPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        etPin.hint = "Ingrese PIN de 4 dígitos"
        
        // Configurar mensaje inicial
        val activityName = intent.getStringExtra(EXTRA_TARGET_ACTIVITY_NAME) ?: "esta función"
        tvMensaje.text = "Ingrese su PIN para acceder a $activityName"
        
        updateIntentosDisplay()
    }
    
    private fun setupListeners() {
        btnConfirmar.setOnClickListener {
            verificarPin()
        }
        
        btnCancelar.setOnClickListener {
            finish()
        }
        
        // Permitir confirmar con Enter
        etPin.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                verificarPin()
                true
            } else {
                false
            }
        }
        
        // Actualizar botón confirmar cuando cambie el texto
        etPin.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                btnConfirmar.isEnabled = s?.length == 4
            }
        })
    }
    
    private fun verificarPin() {
        val pin = etPin.text.toString()
        
        if (pin.length != 4) {
            tvMensaje.text = "❌ El PIN debe tener 4 dígitos"
            return
        }
        
        if (PinManager.verifyPin(this, pin)) {
            // PIN correcto
            PinManager.updateLastActivity(this)
            
            // Navegar a la actividad destino
            targetActivity?.let { target ->
                try {
                    val intent = Intent(this, Class.forName(target))
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al abrir la función", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } ?: run {
                finish()
            }
            
        } else {
            // PIN incorrecto
            intentosRestantes--
            updateIntentosDisplay()
            
            if (intentosRestantes > 0) {
                tvMensaje.text = "❌ PIN incorrecto. Intentos restantes: $intentosRestantes"
                etPin.text.clear()
                etPin.requestFocus()
            } else {
                // Sin intentos restantes
                tvMensaje.text = "❌ Demasiados intentos incorrectos"
                etPin.isEnabled = false
                btnConfirmar.isEnabled = false
                
                // Bloquear por 5 minutos
                Handler().postDelayed({
                    finish()
                }, 5000) // 5 segundos de bloqueo
            }
        }
    }
    
    private fun updateIntentosDisplay() {
        tvIntentos.text = "Intentos restantes: $intentosRestantes"
        tvIntentos.visibility = if (intentosRestantes < 3) View.VISIBLE else View.GONE
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onBackPressed() {
        finish()
    }
}
