package com.asistencia.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PasswordActivity : AppCompatActivity() {
    
    private lateinit var etPassword: EditText
    private lateinit var btnIngresar: Button
    private lateinit var btnCancelar: Button
    private lateinit var sharedPreferences: SharedPreferences
    
    // Contraseña por defecto (en producción debería ser configurable)
    private val defaultPassword = "admin123"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)
        
        initializeComponents()
        setupUI()
        setupListeners()
    }
    
    private fun initializeComponents() {
        etPassword = findViewById(R.id.et_password)
        btnIngresar = findViewById(R.id.btn_ingresar)
        btnCancelar = findViewById(R.id.btn_cancelar)
        
        // Configurar SharedPreferences encriptado
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "admin_password",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    private fun setupUI() {
        // Configurar título
        supportActionBar?.title = "Acceso Administrativo"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        
        // Enfocar en el campo de contraseña
        etPassword.requestFocus()
    }
    
    private fun setupListeners() {
        btnIngresar.setOnClickListener {
            verificarContrasena()
        }
        
        btnCancelar.setOnClickListener {
            finish()
        }
        
        // Permitir ingresar con Enter
        etPassword.setOnEditorActionListener { _, _, _ ->
            verificarContrasena()
            true
        }
    }
    
    private fun verificarContrasena() {
        val passwordIngresada = etPassword.text.toString().trim()
        
        if (passwordIngresada.isEmpty()) {
            etPassword.error = "Ingrese la contraseña"
            etPassword.requestFocus()
            return
        }
        
        // Obtener contraseña almacenada o usar la por defecto
        val passwordAlmacenada = sharedPreferences.getString("admin_password", defaultPassword) ?: defaultPassword
        
        if (passwordIngresada == passwordAlmacenada) {
            // Contraseña correcta
            Toast.makeText(this, "✅ Acceso autorizado", Toast.LENGTH_SHORT).show()
            
            // Marcar como autenticado
            sharedPreferences.edit()
                .putBoolean("is_authenticated", true)
                .putLong("auth_timestamp", System.currentTimeMillis())
                .apply()
            
            // Regresar a la actividad anterior con resultado exitoso
            val resultIntent = Intent()
            resultIntent.putExtra("target_activity", intent.getStringExtra("target_activity"))
            setResult(RESULT_OK, resultIntent)
            finish()
            
        } else {
            // Contraseña incorrecta
            etPassword.error = "Contraseña incorrecta"
            etPassword.setText("")
            etPassword.requestFocus()
            
            Toast.makeText(this, "❌ Contraseña incorrecta", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onBackPressed() {
        // No permitir volver atrás sin autenticación
        Toast.makeText(this, "Debe ingresar la contraseña para continuar", Toast.LENGTH_SHORT).show()
    }
    
    companion object {
        const val REQUEST_CODE_PASSWORD = 1001
        const val RESULT_AUTHENTICATED = RESULT_OK
    }
}
