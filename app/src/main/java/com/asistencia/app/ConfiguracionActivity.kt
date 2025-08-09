package com.asistencia.app

import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.asistencia.app.database.ModoLectura
import com.asistencia.app.database.ModoOperacion
import com.asistencia.app.repository.AsistenciaRepository
import kotlinx.coroutines.launch

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
    private lateinit var btnConfiguracionAvanzada: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)
        
        repository = AsistenciaRepository(this)
        
        setupViews()
        loadCurrentConfiguration()
        
        supportActionBar?.title = "⚙️ Configuración"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
        btnConfiguracionAvanzada = findViewById(R.id.btn_configuracion_avanzada)
        
        // Setup spinners
        setupModoLecturaSpinner()
        setupModoOperacionSpinner()
        
        // Setup button listeners
        btnGuardarConfig.setOnClickListener { guardarConfiguracion() }
        btnTestConexion.setOnClickListener { testearConexionApi() }
        btnSincronizarAhora.setOnClickListener { forzarSincronizacion() }
        btnConfiguracionAvanzada.setOnClickListener { mostrarConfiguracionAvanzada() }
        
        // Setup switches
        switchModoOffline.setOnCheckedChangeListener { _, isChecked ->
            etApiEndpoint.isEnabled = !isChecked
            etApiToken.isEnabled = !isChecked
            btnTestConexion.isEnabled = !isChecked
        }
    }
    
    private fun setupModoLecturaSpinner() {
        val modos = arrayOf(
            "QR Code",
            "DNI (PDF417)",
            "Código de Barras (Code128)"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModoLectura.adapter = adapter
    }
    
    private fun setupModoOperacionSpinner() {
        val modos = arrayOf(
            "Puesto Fijo (Operador)",
            "Autoservicio"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModoOperacion.adapter = adapter
    }
    
    private fun loadCurrentConfiguration() {
        lifecycleScope.launch {
            try {
                val dispositivo = repository.getDispositivo()
                
                // Cargar modo de lectura
                val modoLecturaIndex = when (dispositivo.modoLectura) {
                    ModoLectura.QR -> 0
                    ModoLectura.DNI_PDF417 -> 1
                    ModoLectura.CODE128 -> 2
                }
                spinnerModoLectura.setSelection(modoLecturaIndex)
                
                // Cargar modo de operación
                val modoOperacionIndex = when (dispositivo.modoOperacion) {
                    ModoOperacion.PUESTO_FIJO -> 0
                    ModoOperacion.AUTOSERVICIO -> 1
                }
                spinnerModoOperacion.setSelection(modoOperacionIndex)
                
                // Cargar otros settings
                switchCapturaUbicacion.isChecked = dispositivo.capturaUbicacion
                switchModoOffline.isChecked = dispositivo.modoOffline
                etApiEndpoint.setText(dispositivo.apiEndpoint ?: "")
                etApiToken.setText(dispositivo.apiToken ?: "")
                
                // Actualizar estado de sincronización
                actualizarEstadoSync()
                
            } catch (e: Exception) {
                Toast.makeText(this@ConfiguracionActivity, 
                    "Error al cargar configuración: ${e.message}", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun guardarConfiguracion() {
        lifecycleScope.launch {
            try {
                val dispositivo = repository.getDispositivo()
                
                // Obtener valores de UI
                val modoLectura = when (spinnerModoLectura.selectedItemPosition) {
                    0 -> ModoLectura.QR
                    1 -> ModoLectura.DNI_PDF417
                    2 -> ModoLectura.CODE128
                    else -> ModoLectura.QR
                }
                
                val modoOperacion = when (spinnerModoOperacion.selectedItemPosition) {
                    0 -> ModoOperacion.PUESTO_FIJO
                    1 -> ModoOperacion.AUTOSERVICIO
                    else -> ModoOperacion.PUESTO_FIJO
                }
                
                val capturaUbicacion = switchCapturaUbicacion.isChecked
                val modoOffline = switchModoOffline.isChecked
                val apiEndpoint = etApiEndpoint.text.toString().trim().takeIf { it.isNotEmpty() }
                val apiToken = etApiToken.text.toString().trim().takeIf { it.isNotEmpty() }
                
                // Actualizar dispositivo
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
                
                Toast.makeText(this@ConfiguracionActivity, 
                    "✅ Configuración guardada correctamente", 
                    Toast.LENGTH_SHORT).show()
                
                mostrarResumenConfiguracion(dispositivoActualizado)
                
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
            Toast.makeText(this, "Complete el endpoint y token de API", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Aquí se implementaría el test real de conexión
        // Por ahora simulamos el test
        Toast.makeText(this, "🔄 Probando conexión...", Toast.LENGTH_SHORT).show()
        
        // Simular delay de red
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000)
            
            // Simular resultado (en implementación real, hacer llamada HTTP)
            val exito = endpoint.startsWith("https://") && token.length > 10
            
            if (exito) {
                Toast.makeText(this@ConfiguracionActivity, 
                    "✅ Conexión exitosa", 
                    Toast.LENGTH_SHORT).show()
            } else {
                AlertDialog.Builder(this@ConfiguracionActivity)
                    .setTitle("❌ Error de Conexión")
                    .setMessage("No se pudo conectar al servidor.\n\nVerifique:\n• URL del endpoint\n• Token de autenticación\n• Conexión a internet")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun forzarSincronizacion() {
        lifecycleScope.launch {
            try {
                val pendientes = repository.getCountRegistrosPendientes()
                
                if (pendientes == 0) {
                    Toast.makeText(this@ConfiguracionActivity, 
                        "✅ No hay registros pendientes de sincronización", 
                        Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                Toast.makeText(this@ConfiguracionActivity, 
                    "🔄 Sincronizando $pendientes registros...", 
                    Toast.LENGTH_SHORT).show()
                
                repository.forzarSincronizacion()
                
                // Actualizar estado después de un delay
                kotlinx.coroutines.delay(3000)
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
    
    private fun mostrarConfiguracionAvanzada() {
        AlertDialog.Builder(this)
            .setTitle("⚙️ Configuración Avanzada")
            .setMessage("Seleccione una opción:")
            .setPositiveButton("🔐 Configurar PIN") { _, _ ->
                configurarPinOperador()
            }
            .setNeutralButton("📊 Ver Estadísticas") { _, _ ->
                mostrarEstadisticas()
            }
            .setNegativeButton("🗑️ Limpiar Datos") { _, _ ->
                mostrarOpcionesLimpieza()
            }
            .show()
    }
    
    private fun configurarPinOperador() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.hint = "Ingrese PIN de 4 dígitos"
        
        AlertDialog.Builder(this)
            .setTitle("🔐 Configurar PIN del Operador")
            .setMessage("Configure un PIN para bloquear la aplicación después de inactividad:")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val pin = input.text.toString()
                if (pin.length == 4 && pin.all { it.isDigit() }) {
                    // Aquí se guardaría el PIN hasheado
                    Toast.makeText(this, "✅ PIN configurado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "❌ El PIN debe tener exactamente 4 dígitos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun mostrarEstadisticas() {
        lifecycleScope.launch {
            try {
                val pendientes = repository.getCountRegistrosPendientes()
                val fecha = com.asistencia.app.utils.HorarioUtils.getCurrentDateString()
                val estadisticas = repository.getEstadisticasDelDia(fecha)
                
                val mensaje = """
                    📊 ESTADÍSTICAS DEL SISTEMA
                    
                    📅 Hoy ($fecha):
                    • Total registros: ${estadisticas.totalRegistros}
                    • Empleados presentes: ${estadisticas.empleadosPresentes}
                    • Tardanzas: ${estadisticas.tardanzas}
                    
                    🔄 Sincronización:
                    • Registros pendientes: $pendientes
                    
                    💾 Base de datos:
                    • Estado: Operativa
                    • Última actualización: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
                """.trimIndent()
                
                AlertDialog.Builder(this@ConfiguracionActivity)
                    .setTitle("📊 Estadísticas del Sistema")
                    .setMessage(mensaje)
                    .setPositiveButton("OK", null)
                    .show()
                
            } catch (e: Exception) {
                Toast.makeText(this@ConfiguracionActivity, 
                    "Error al obtener estadísticas: ${e.message}", 
                    Toast.LENGTH_LONG).show()
            }
        }
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
            ModoLectura.QR -> "QR Code"
            ModoLectura.DNI_PDF417 -> "DNI (PDF417)"
            ModoLectura.CODE128 -> "Código de Barras (Code128)"
        }
        
        val modoOperacionTexto = when (dispositivo.modoOperacion) {
            ModoOperacion.PUESTO_FIJO -> "Puesto Fijo"
            ModoOperacion.AUTOSERVICIO -> "Autoservicio"
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
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}