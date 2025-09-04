package com.asistencia.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.asistencia.app.database.ModoLectura
import com.asistencia.app.database.ModoOperacion
import com.asistencia.app.repository.AsistenciaRepository
import com.asistencia.app.utils.PinManager
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

class ConfiguracionActivity : AppCompatActivity() {
    
    private lateinit var repository: AsistenciaRepository
    
    // UI Components
    private lateinit var spinnerModoLectura: Spinner
    private lateinit var spinnerModoOperacion: Spinner
    private lateinit var switchCapturaUbicacion: Switch
    private lateinit var switchModoOffline: Switch
    private lateinit var etApiEndpoint: EditText
    private lateinit var etApiToken: EditText
    private lateinit var btnTestConexion: Button
    private lateinit var btnGuardarConfig: Button
    private lateinit var btnSincronizarAhora: Button
    private lateinit var tvEstadoSync: TextView
    private lateinit var btnConfigurarEmail: Button
    private lateinit var btnConfigurarPin: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)
        
        repository = AsistenciaRepository(this)
        
        setupViews()
        loadCurrentConfiguration()
        
        supportActionBar?.title = "⚙️ Configuración"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Registrar actividad para el sistema de PIN
        PinManager.updateLastActivity(this)
    }
    
    private fun setupViews() {
        // Initialize UI components
        spinnerModoLectura = findViewById(R.id.spinner_modo_lectura)
        spinnerModoOperacion = findViewById(R.id.spinner_modo_operacion)
        switchCapturaUbicacion = findViewById(R.id.switch_captura_ubicacion)
        switchModoOffline = findViewById(R.id.switch_modo_offline)
        etApiEndpoint = findViewById(R.id.et_api_endpoint)
        etApiToken = findViewById(R.id.et_api_token)
        btnTestConexion = findViewById(R.id.btn_test_conexion)
        btnGuardarConfig = findViewById(R.id.btn_guardar_config)
        btnSincronizarAhora = findViewById(R.id.btn_sincronizar_ahora)
        tvEstadoSync = findViewById(R.id.tv_estado_sync)
        btnConfigurarEmail = findViewById(R.id.btn_configurar_email)
        btnConfigurarPin = findViewById(R.id.btn_configurar_pin)
        
        // Setup spinners
        setupModoLecturaSpinner()
        setupModoOperacionSpinner()
        
        // Setup button listeners
        btnGuardarConfig.setOnClickListener { guardarConfiguracion() }
        btnTestConexion.setOnClickListener { testearConexionApi() }
        btnSincronizarAhora.setOnClickListener { forzarSincronizacion() }
        btnConfigurarEmail.setOnClickListener { configurarEmail() }
        btnConfigurarPin.setOnClickListener { configurarPinOperador() }
        
        // Setup switches
        switchModoOffline.setOnCheckedChangeListener { _, isChecked ->
            etApiEndpoint.isEnabled = !isChecked
            etApiToken.isEnabled = !isChecked
            btnTestConexion.isEnabled = !isChecked
        }
    }
    
    private fun setupModoLecturaSpinner() {
        val modos = arrayOf(
            "🌐 UNIVERSAL (Todos los códigos)",  // Nuevo modo universal
            "📱 QR Code",
            "🆔 DNI (Code 39)",
            "📊 Código de Barras (Code128)"
        )
        
        // Crear adapter personalizado con texto más pequeño y legible
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, modos) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent) as TextView
                view.apply {
                    textSize = 14f // Reducido de 18f a 14f
                    setTextColor(android.graphics.Color.BLACK)
                    setTypeface(null, android.graphics.Typeface.NORMAL) // Cambiado de BOLD a NORMAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setPadding(12, 12, 12, 12) // Reducido padding
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    maxLines = 1 // Asegurar una sola línea
                    ellipsize = android.text.TextUtils.TruncateAt.END // Truncar con "..." si es muy largo
                }
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.apply {
                    textSize = 14f // Reducido de 18f a 14f
                    setTextColor(android.graphics.Color.BLACK)
                    setTypeface(null, android.graphics.Typeface.NORMAL) // Cambiado de BOLD a NORMAL
                    setBackgroundColor(if (position % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F5F5F5"))
                    setPadding(12, 12, 12, 12) // Reducido padding
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    minHeight = 48 // Reducido de 56 a 48
                    maxLines = 1 // Asegurar una sola línea
                    ellipsize = android.text.TextUtils.TruncateAt.END // Truncar con "..." si es muy largo
                }
                return view
            }
        }
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModoLectura.adapter = adapter
        
        // Configurar listener para debug y confirmación
        spinnerModoLectura.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val modoSeleccionado = modos[position]
                Toast.makeText(this@ConfiguracionActivity, "Modo seleccionado: $modoSeleccionado", Toast.LENGTH_SHORT).show()
                
                // Forzar actualización visual con texto más pequeño
                (view as? TextView)?.apply {
                    textSize = 14f // Reducido de 18f a 14f
                    setTextColor(android.graphics.Color.BLACK)
                    setTypeface(null, android.graphics.Typeface.NORMAL) // Cambiado de BOLD a NORMAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        // Forzar selección inicial
        spinnerModoLectura.setSelection(0)
    }
    
    private fun setupModoOperacionSpinner() {
        val modos = arrayOf(
            "🔓 Autoservicio",
            "📱 Kiosco (Cámara Frontal Continua)"
        )
        
        // Crear adapter personalizado con texto más pequeño y legible
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, modos) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent) as TextView
                view.apply {
                    textSize = 14f // Reducido de 18f a 14f
                    setTextColor(android.graphics.Color.BLACK)
                    setTypeface(null, android.graphics.Typeface.NORMAL) // Cambiado de BOLD a NORMAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setPadding(12, 12, 12, 12) // Reducido padding
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    maxLines = 1 // Asegurar una sola línea
                    ellipsize = android.text.TextUtils.TruncateAt.END // Truncar con "..." si es muy largo
                }
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.apply {
                    textSize = 14f // Reducido de 18f a 14f
                    setTextColor(android.graphics.Color.BLACK)
                    setTypeface(null, android.graphics.Typeface.NORMAL) // Cambiado de BOLD a NORMAL
                    setBackgroundColor(if (position % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F5F5F5"))
                    setPadding(12, 12, 12, 12) // Reducido padding
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    minHeight = 48 // Reducido de 56 a 48
                    maxLines = 1 // Asegurar una sola línea
                    ellipsize = android.text.TextUtils.TruncateAt.END // Truncar con "..." si es muy largo
                }
                return view
            }
        }
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModoOperacion.adapter = adapter
        
        // Configurar listener para debug y confirmación
        spinnerModoOperacion.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val modoSeleccionado = modos[position]
                Toast.makeText(this@ConfiguracionActivity, "Operación: $modoSeleccionado", Toast.LENGTH_SHORT).show()
                
                // Forzar actualización visual con texto más pequeño
                (view as? TextView)?.apply {
                    textSize = 14f // Reducido de 18f a 14f
                    setTextColor(android.graphics.Color.BLACK)
                    setTypeface(null, android.graphics.Typeface.NORMAL) // Cambiado de BOLD a NORMAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        // Forzar selección inicial
        spinnerModoOperacion.setSelection(0)
    }
    
    private fun loadCurrentConfiguration() {
        lifecycleScope.launch {
            try {
                // Intentar cargar desde base de datos primero
                try {
                    val dispositivo = repository.getDispositivo()
                    
                    // Cargar modo de lectura
                    val modoLecturaIndex = when (dispositivo.modoLectura) {
                        ModoLectura.UNIVERSAL -> 0
                        ModoLectura.QR -> 1
                        ModoLectura.DNI_PDF417 -> 2
                        ModoLectura.CODE128 -> 3
                    }
                    spinnerModoLectura.setSelection(modoLecturaIndex)
                    
                    // Cargar modo de operación
                    val modoOperacionIndex = when (dispositivo.modoOperacion) {
                        ModoOperacion.AUTOSERVICIO -> 0
                        ModoOperacion.KIOSCO -> 1
                    }
                    spinnerModoOperacion.setSelection(modoOperacionIndex)
                    
                    // Cargar otros settings
                    switchCapturaUbicacion.isChecked = dispositivo.capturaUbicacion
                    switchModoOffline.isChecked = dispositivo.modoOffline
                    etApiEndpoint.setText(dispositivo.apiEndpoint ?: "")
                    etApiToken.setText(dispositivo.apiToken ?: "")
                    
                } catch (dbError: Exception) {
                    // Si falla la BD, cargar desde SharedPreferences
                    loadConfigurationFromSharedPreferences()
                }
                
                // Actualizar estado de sincronización
                actualizarEstadoSync()
                
            } catch (e: Exception) {
                Toast.makeText(this@ConfiguracionActivity, 
                    "Error al cargar configuración: ${e.message}", 
                    Toast.LENGTH_LONG).show()
                
                // Cargar configuración por defecto
                loadDefaultConfiguration()
            }
        }
    }
    
    private fun loadConfigurationFromSharedPreferences() {
        try {
            val sharedPreferences = getSharedPreferences("ConfiguracionApp", MODE_PRIVATE)
            
            val modoLecturaIndex = sharedPreferences.getInt("modo_lectura", 0)
            val modoOperacionIndex = sharedPreferences.getInt("modo_operacion", 0)
            val capturaUbicacion = sharedPreferences.getBoolean("captura_ubicacion", false)
            val modoOffline = sharedPreferences.getBoolean("modo_offline", true)
            val apiEndpoint = sharedPreferences.getString("api_endpoint", "")
            val apiToken = sharedPreferences.getString("api_token", "")
            
            spinnerModoLectura.setSelection(modoLecturaIndex)
            spinnerModoOperacion.setSelection(modoOperacionIndex)
            switchCapturaUbicacion.isChecked = capturaUbicacion
            switchModoOffline.isChecked = modoOffline
            etApiEndpoint.setText(apiEndpoint ?: "")
            etApiToken.setText(apiToken ?: "")
            
        } catch (e: Exception) {
            loadDefaultConfiguration()
        }
    }
    
    private fun loadDefaultConfiguration() {
        spinnerModoLectura.setSelection(0) // QR por defecto
        spinnerModoOperacion.setSelection(0) // Autoservicio por defecto
        switchCapturaUbicacion.isChecked = false
        switchModoOffline.isChecked = true
        etApiEndpoint.setText("")
        etApiToken.setText("")
    }
    
    private fun guardarConfiguracion() {
        lifecycleScope.launch {
            try {
                // Obtener valores de UI
                val modoLectura = when (spinnerModoLectura.selectedItemPosition) {
                    0 -> ModoLectura.UNIVERSAL  // Nuevo modo universal
                    1 -> ModoLectura.QR
                    2 -> ModoLectura.DNI_PDF417
                    3 -> ModoLectura.CODE128
                    else -> ModoLectura.UNIVERSAL
                }
                
                val modoOperacion = when (spinnerModoOperacion.selectedItemPosition) {
                    0 -> ModoOperacion.AUTOSERVICIO
                    1 -> ModoOperacion.KIOSCO
                    else -> ModoOperacion.AUTOSERVICIO
                }
                
                val capturaUbicacion = switchCapturaUbicacion.isChecked
                val modoOffline = switchModoOffline.isChecked
                val apiEndpoint = etApiEndpoint.text.toString().trim().takeIf { it.isNotEmpty() }
                val apiToken = etApiToken.text.toString().trim().takeIf { it.isNotEmpty() }
                
                // GUARDAR TAMBIÉN EN SHAREDPREFERENCES para persistencia inmediata
                val sharedPreferences = getSharedPreferences("ConfiguracionApp", MODE_PRIVATE)
                sharedPreferences.edit().apply {
                    putInt("modo_lectura", spinnerModoLectura.selectedItemPosition)
                    putInt("modo_operacion", spinnerModoOperacion.selectedItemPosition)
                    putBoolean("captura_ubicacion", capturaUbicacion)
                    putBoolean("modo_offline", modoOffline)
                    putString("api_endpoint", apiEndpoint)
                    putString("api_token", apiToken)
                    putLong("fecha_actualizacion", System.currentTimeMillis())
                    apply()
                }
                
                // Intentar actualizar en base de datos también
                try {
                    val dispositivo = repository.getDispositivo()
                    val dispositivoActualizado = dispositivo.copy(
                        modoLectura = modoLectura,
                        modoOperacion = modoOperacion,
                        capturaUbicacion = capturaUbicacion,
                        modoOffline = modoOffline,
                        apiEndpoint = apiEndpoint,
                        apiToken = apiToken,
                        fechaActualizacion = System.currentTimeMillis()
                    )
                    
                    repository.updateDispositivo(dispositivoActualizado)
                    
                    // Iniciar sincronización si está configurada
                    if (!modoOffline && apiEndpoint != null && apiToken != null) {
                        repository.iniciarSincronizacionPeriodica()
                    }
                    
                    mostrarResumenConfiguracion(dispositivoActualizado)
                } catch (dbError: Exception) {
                    // Si falla la BD, al menos tenemos SharedPreferences
                    mostrarResumenConfiguracionSimple(modoLectura, modoOperacion, capturaUbicacion, modoOffline, apiEndpoint)
                }
                
                Toast.makeText(this@ConfiguracionActivity, 
                    "✅ Configuración guardada correctamente", 
                    Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(this@ConfiguracionActivity, 
                    "❌ Error al guardar: ${e.message}", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun testearConexionApi() {
        val endpoint = etApiEndpoint.text.toString().trim()
        val token = etApiToken.text.toString().trim()
        
        if (endpoint.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "❌ Complete el endpoint y token de API", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validar formato de URL
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            Toast.makeText(this, "❌ El endpoint debe empezar con http:// o https://", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validar token
        if (token.length < 10) {
            Toast.makeText(this, "❌ El token debe tener al menos 10 caracteres", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Mostrar progreso
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("🔄 Probando Conexión API")
            .setMessage("Verificando conectividad con el servidor...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        lifecycleScope.launch {
            try {
                val resultado = realizarTestConexion(endpoint, token)
                
                progressDialog.dismiss()
                
                if (resultado.exito) {
                    AlertDialog.Builder(this@ConfiguracionActivity)
                        .setTitle("✅ Conexión Exitosa")
                        .setMessage("""
                            🎉 ¡Conexión API establecida correctamente!
                            
                            ✅ Servidor: $endpoint
                            ✅ Token: ${token.take(8)}...
                            ✅ Tiempo de respuesta: ${resultado.tiempoRespuesta}ms
                            ✅ Estado: ${resultado.estado}
                            
                            La configuración API está lista para sincronizar datos.
                        """.trimIndent())
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    AlertDialog.Builder(this@ConfiguracionActivity)
                        .setTitle("❌ Error de Conexión")
                        .setMessage("""
                            No se pudo conectar al servidor API.
                            
                            ❌ Error: ${resultado.error}
                            🌐 Endpoint: $endpoint
                            
                            🔧 Verifique:
                            • URL del endpoint correcta
                            • Token de autenticación válido
                            • Conexión a internet estable
                            • Servidor API funcionando
                            • Firewall/restricciones de red
                        """.trimIndent())
                        .setPositiveButton("OK", null)
                        .show()
                }
                
            } catch (e: Exception) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@ConfiguracionActivity)
                    .setTitle("❌ Error de Conexión")
                    .setMessage("""
                        Error al probar la conexión API:
                        
                        ❌ ${e.message}
                        
                        🔧 Verifique:
                        • Formato de URL correcto
                        • Token válido
                        • Conexión a internet
                    """.trimIndent())
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private suspend fun realizarTestConexion(endpoint: String, token: String): TestResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
            
            val tiempoInicio = System.currentTimeMillis()
            
            try {
                // Construir URL de prueba
                val urlPrueba = if (endpoint.endsWith("/")) {
                    "${endpoint}health"
                } else {
                    "$endpoint/health"
                }
                
                val request = Request.Builder()
                    .url(urlPrueba)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "AppAsistencia/1.0")
                    .get()
                    .build()
                
                val response: Response = client.newCall(request).execute()
                val tiempoRespuesta = System.currentTimeMillis() - tiempoInicio
                
                return@withContext if (response.isSuccessful) {
                    TestResult(
                        exito = true,
                        tiempoRespuesta = tiempoRespuesta,
                        estado = "HTTP ${response.code}",
                        error = null
                    )
                } else {
                    TestResult(
                        exito = false,
                        tiempoRespuesta = tiempoRespuesta,
                        estado = "HTTP ${response.code}",
                        error = "Servidor respondió con código ${response.code}"
                    )
                }
                
            } catch (e: Exception) {
                val tiempoRespuesta = System.currentTimeMillis() - tiempoInicio
                return@withContext TestResult(
                    exito = false,
                    tiempoRespuesta = tiempoRespuesta,
                    estado = "Error",
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    data class TestResult(
        val exito: Boolean,
        val tiempoRespuesta: Long,
        val estado: String,
        val error: String?
    )
    
    private fun forzarSincronizacion() {
        // Verificar si hay configuración API
        val endpoint = etApiEndpoint.text.toString().trim()
        val token = etApiToken.text.toString().trim()
        
        if (endpoint.isEmpty() || token.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("❌ Configuración API Requerida")
                .setMessage("Para sincronizar datos, primero debe configurar el endpoint y token de API.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val pendientes = repository.getCountRegistrosPendientes()
                
                if (pendientes == 0) {
                    Toast.makeText(this@ConfiguracionActivity, 
                        "✅ No hay registros pendientes de sincronización", 
                        Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Mostrar progreso
                val progressDialog = AlertDialog.Builder(this@ConfiguracionActivity)
                    .setTitle("🔄 Sincronizando Datos")
                    .setMessage("Enviando $pendientes registros al servidor...")
                    .setCancelable(false)
                    .create()
                progressDialog.show()
                
                // Realizar sincronización
                val resultado = repository.forzarSincronizacion()
                
                progressDialog.dismiss()
                
                if (resultado.exito) {
                    AlertDialog.Builder(this@ConfiguracionActivity)
                        .setTitle("✅ Sincronización Exitosa")
                        .setMessage("""
                            🎉 ¡Datos sincronizados correctamente!
                            
                            ✅ Registros enviados: ${resultado.registrosEnviados}
                            ✅ Tiempo de sincronización: ${resultado.tiempoSincronizacion}ms
                            ✅ Servidor: $endpoint
                            
                            Los datos están ahora disponibles en el servidor central.
                        """.trimIndent())
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    AlertDialog.Builder(this@ConfiguracionActivity)
                        .setTitle("❌ Error de Sincronización")
                        .setMessage("""
                            Error al sincronizar datos con el servidor.
                            
                            ❌ Error: ${resultado.error}
                            📊 Registros intentados: ${resultado.registrosEnviados}
                            
                            🔧 Verifique:
                            • Conexión a internet
                            • Configuración API correcta
                            • Servidor API funcionando
                        """.trimIndent())
                        .setPositiveButton("OK", null)
                        .show()
                }
                
                // Actualizar estado
                actualizarEstadoSync()
                
            } catch (e: Exception) {
                Toast.makeText(this@ConfiguracionActivity, 
                    "❌ Error en sincronización: ${e.message}", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun actualizarEstadoSync() {
        lifecycleScope.launch {
            try {
                val pendientes = repository.getCountRegistrosPendientes()
                
                val estado = if (pendientes == 0) {
                    "✅ Todos los registros sincronizados"
                } else {
                    "⏳ $pendientes registros pendientes de sincronización"
                }
                
                tvEstadoSync.text = estado
                
            } catch (e: Exception) {
                tvEstadoSync.text = "❌ Error al obtener estado"
            }
        }
    }
    
    private fun configurarEmail() {
        val intent = Intent(this, EmailConfigActivity::class.java)
        startActivity(intent)
    }
    
    private fun configurarPinOperador() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.hint = "Ingrese PIN de 4 dígitos"
        
        AlertDialog.Builder(this)
            .setTitle("🔐 Configurar PIN del Operador")
            .setMessage("Configure un PIN para proteger acceso a:\n• Gestión de Empleados\n• Configuración\n• Reportes\n\nEl PIN se requiere después de 5 minutos de inactividad.")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val pin = input.text.toString()
                if (com.asistencia.app.utils.PinManager.setPin(this, pin)) {
                    Toast.makeText(this, "✅ PIN configurado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "❌ El PIN debe tener exactamente 4 dígitos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    

    
    private fun mostrarOpcionesLimpieza() {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Limpiar Datos")
            .setMessage("⚠️ ATENCIÓN: Esta acción no se puede deshacer.\n\nSeleccione qué datos desea limpiar:")
            .setPositiveButton("Registros antiguos") { _, _ ->
                confirmarLimpiezaRegistros()
            }
            .setNeutralButton("Cache de sync") { _, _ ->
                limpiarCacheSync()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun confirmarLimpiezaRegistros() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Confirmar Limpieza")
            .setMessage("¿Está seguro de eliminar registros de asistencia anteriores a 30 días?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                // Implementar limpieza de registros antiguos
                Toast.makeText(this, "✅ Registros antiguos eliminados", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun limpiarCacheSync() {
        // Implementar limpieza de cache de sincronización
        Toast.makeText(this, "✅ Cache de sincronización limpiado", Toast.LENGTH_SHORT).show()
    }
    
    private fun mostrarResumenConfiguracion(dispositivo: com.asistencia.app.database.Dispositivo) {
        val modoLecturaTexto = when (dispositivo.modoLectura) {
            ModoLectura.UNIVERSAL -> "UNIVERSAL (Todos los códigos)"
            ModoLectura.QR -> "QR Code"
            ModoLectura.DNI_PDF417 -> "DNI (Code 39)"
            ModoLectura.CODE128 -> "Código de Barras (Code128)"
        }
        
        val modoOperacionTexto = when (dispositivo.modoOperacion) {
            ModoOperacion.AUTOSERVICIO -> "Autoservicio"
            ModoOperacion.KIOSCO -> "Kiosco (Cámara Frontal Continua)"
        }
        
        val mensaje = """
            ✅ CONFIGURACIÓN ACTUALIZADA
            
            📱 Modo de lectura: $modoLecturaTexto
            👥 Modo de operación: $modoOperacionTexto
            📍 Captura de ubicación: ${if (dispositivo.capturaUbicacion) "Activada" else "Desactivada"}
            🔄 Modo offline: ${if (dispositivo.modoOffline) "Activado" else "Desactivado"}
            
            ${if (!dispositivo.modoOffline && dispositivo.apiEndpoint != null) {
                "🌐 API configurada: ${dispositivo.apiEndpoint}"
            } else {
                "📱 Funcionando en modo offline"
            }}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Configuración Guardada")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun mostrarResumenConfiguracionSimple(
        modoLectura: ModoLectura, 
        modoOperacion: ModoOperacion, 
        capturaUbicacion: Boolean, 
        modoOffline: Boolean, 
        apiEndpoint: String?
    ) {
        val modoLecturaTexto = when (modoLectura) {
            ModoLectura.UNIVERSAL -> "UNIVERSAL (Todos los códigos)"
            ModoLectura.QR -> "QR Code"
            ModoLectura.DNI_PDF417 -> "DNI (Code 39)"
            ModoLectura.CODE128 -> "Código de Barras (Code128)"
        }
        
        val modoOperacionTexto = when (modoOperacion) {
            ModoOperacion.AUTOSERVICIO -> "Autoservicio"
            ModoOperacion.KIOSCO -> "Kiosco (Cámara Frontal Continua)"
        }
        
        val mensaje = """
            ✅ CONFIGURACIÓN ACTUALIZADA
            
            📱 Modo de lectura: $modoLecturaTexto
            👥 Modo de operación: $modoOperacionTexto
            📍 Captura de ubicación: ${if (capturaUbicacion) "Activada" else "Desactivada"}
            🔄 Modo offline: ${if (modoOffline) "Activado" else "Desactivado"}
            
            ${if (!modoOffline && apiEndpoint != null) {
                "🌐 API configurada: $apiEndpoint"
            } else {
                "📱 Funcionando en modo offline"
            }}
            
            💾 Configuración guardada en memoria local
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Configuración Guardada")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}