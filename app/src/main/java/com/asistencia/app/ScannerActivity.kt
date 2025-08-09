package com.asistencia.app

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.asistencia.app.database.*
import com.asistencia.app.repository.AsistenciaRepository
import com.asistencia.app.repository.ResultadoRegistro
import com.asistencia.app.scanner.ScannerService
import com.asistencia.app.utils.HorarioUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.coroutines.launch
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScannerActivity : AppCompatActivity(), ScannerService.ScannerCallback {
    
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var repository: AsistenciaRepository
    private lateinit var scannerService: ScannerService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    // UI Components
    private lateinit var tvProximoEvento: TextView
    private lateinit var tvEmpleadoInfo: TextView
    private lateinit var btnLinterna: ImageButton
    private lateinit var progressBar: ProgressBar
    
    private var currentLocation: Location? = null
    private var isProcessing = false
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val LOCATION_PERMISSION_REQUEST = 101
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        
        initializeComponents()
        setupUI()
        checkPermissions()
    }
    
    private fun initializeComponents() {
        repository = AsistenciaRepository(this)
        scannerService = ScannerService(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialize UI components
        barcodeView = findViewById(R.id.barcode_scanner)
        tvProximoEvento = findViewById(R.id.tv_proximo_evento)
        tvEmpleadoInfo = findViewById(R.id.tv_empleado_info)
        btnLinterna = findViewById(R.id.btn_linterna)
        progressBar = findViewById(R.id.progress_bar)
        
        scannerService.setCallback(this)
    }
    
    private fun setupUI() {
        // Configurar botón de linterna
        btnLinterna.setOnClickListener {
            toggleFlashlight()
        }
        
        // Mostrar información inicial
        updateProximoEventoDisplay()
        
        // Configurar título
        supportActionBar?.title = "Escanear Asistencia"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun checkPermissions() {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        
        val permissionsNeeded = mutableListOf<String>()
        
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        
        // Solo pedir ubicación si está habilitada en configuración
        lifecycleScope.launch {
            val dispositivo = repository.getDispositivo()
            if (dispositivo.capturaUbicacion && locationPermission != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            
            if (permissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this@ScannerActivity,
                    permissionsNeeded.toTypedArray(),
                    CAMERA_PERMISSION_REQUEST
                )
            } else {
                startScanning()
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                val cameraGranted = grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                
                if (cameraGranted) {
                    startScanning()
                } else {
                    showErrorDialog(
                        "Permiso requerido",
                        "Se necesita acceso a la cámara para escanear códigos."
                    ) { finish() }
                }
            }
        }
    }
    
    private fun startScanning() {
        lifecycleScope.launch {
            try {
                val dispositivo = repository.getDispositivo()
                scannerService.configurarScanner(barcodeView, dispositivo.modoLectura)
                
                // Obtener ubicación si está habilitada
                if (dispositivo.capturaUbicacion) {
                    getCurrentLocation()
                }
                
                barcodeView.resume()
                
            } catch (e: Exception) {
                showErrorDialog("Error", "Error al inicializar el scanner: ${e.message}") {
                    finish()
                }
            }
        }
    }
    
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                currentLocation = location
            }
        }
    }
    
    private fun toggleFlashlight() {
        try {
            // Simplificado por ahora - la funcionalidad de linterna se implementará después
            Toast.makeText(this, "Función de linterna en desarrollo", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al controlar la linterna", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateProximoEventoDisplay() {
        tvProximoEvento.text = "Apunte la cámara al código para escanear"
        tvEmpleadoInfo.text = "Esperando escaneo..."
    }
    
    // Implementación de ScannerService.ScannerCallback
    override fun onScanSuccess(empleadoId: String, rawCode: String, modoDetectado: ModoLectura) {
        if (isProcessing) return
        
        isProcessing = true
        progressBar.visibility = View.VISIBLE
        barcodeView.pause()
        
        lifecycleScope.launch {
            try {
                // NUEVO: Primero buscar en SharedPreferences (sistema simple)
                val empleadoSimple = buscarEmpleadoEnSharedPreferences(empleadoId)
                
                if (empleadoSimple != null) {
                    // Encontrado en sistema simple - mostrar éxito
                    showSuccessDialogSimple(empleadoSimple, empleadoId)
                    return@launch
                }
                
                // Si no está en sistema simple, buscar en sistema complejo
                val debugInfo = repository.verificarEmpleadoExiste(empleadoId)
                val proximoEvento = repository.determinarProximoEvento(empleadoId)
                
                if (proximoEvento == null) {
                    val empleado = repository.getEmpleadoByDni(empleadoId) ?: repository.getEmpleadoById(empleadoId)
                    if (empleado == null) {
                        showErrorDialog(
                            "Empleado no encontrado",
                            "No se encontró empleado con DNI: $empleadoId\n\n$debugInfo\n\nVerifique que esté registrado en 'Gestión de Empleados'"
                        ) { resetScanner() }
                        return@launch
                    } else {
                        showErrorDialog(
                            "Jornada completa",
                            "El empleado ya completó todos los registros del día."
                        ) { resetScanner() }
                        return@launch
                    }
                }
                
                // Registrar la asistencia en sistema complejo
                val resultado = repository.registrarAsistencia(
                    empleadoIdentificador = empleadoId,
                    tipoEvento = proximoEvento,
                    modoLectura = modoDetectado,
                    rawCode = rawCode,
                    gpsLat = currentLocation?.latitude,
                    gpsLon = currentLocation?.longitude
                )
                
                when (resultado) {
                    is ResultadoRegistro.Exito -> {
                        showSuccessDialog(resultado)
                    }
                    is ResultadoRegistro.Error -> {
                        val mensaje = if (resultado.mensaje.contains("Empleado no encontrado")) {
                            "${resultado.mensaje}\n\n$debugInfo"
                        } else {
                            resultado.mensaje
                        }
                        
                        showErrorDialog("Error de registro", mensaje) {
                            resetScanner()
                        }
                    }
                }
                
            } catch (e: Exception) {
                showErrorDialog("Error interno", "Error al procesar el registro: ${e.message}") {
                    resetScanner()
                }
            } finally {
                progressBar.visibility = View.GONE
                isProcessing = false
            }
        }
    }
    
    override fun onScanError(error: String) {
        showErrorDialog("Error de escaneo", error) {
            resetScanner()
        }
    }
    
    private fun showSuccessDialog(resultado: ResultadoRegistro.Exito) {
        val empleado = resultado.empleado
        val registro = resultado.registro
        
        // Generar mensaje según el tipo de evento
        val (emoji, tipoTexto) = when (registro.tipoEvento) {
            TipoEvento.ENTRADA_TURNO -> "🌅" to "Entrada de Turno"
            TipoEvento.SALIDA_REFRIGERIO -> "🍽️" to "Salida a Refrigerio"
            TipoEvento.ENTRADA_POST_REFRIGERIO -> "🔄" to "Regreso de Refrigerio"
            TipoEvento.SALIDA_TURNO -> "🏠" to "Salida de Turno"
        }
        
        val horaRegistro = HorarioUtils.formatTimestamp(registro.timestampDispositivo)
        val fechaRegistro = HorarioUtils.formatDateTimestamp(registro.timestampDispositivo)
        
        val mensaje = buildString {
            append("$emoji $tipoTexto registrado\n\n")
            append("👤 ${empleado.nombres} ${empleado.apellidos}\n")
            append("🆔 DNI: ${empleado.dni}\n")
            append("📅 $fechaRegistro\n")
            append("🕐 $horaRegistro\n\n")
            append("📝 ${resultado.mensaje}")
            
            if (resultado.proximoEvento != null) {
                val proximoTexto = when (resultado.proximoEvento) {
                    TipoEvento.ENTRADA_TURNO -> "Entrada de Turno"
                    TipoEvento.SALIDA_REFRIGERIO -> "Salida a Refrigerio"
                    TipoEvento.ENTRADA_POST_REFRIGERIO -> "Regreso de Refrigerio"
                    TipoEvento.SALIDA_TURNO -> "Salida de Turno"
                }
                append("\n\n➡️ Próximo evento: $proximoTexto")
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("✅ Registro Exitoso")
            .setMessage(mensaje)
            .setPositiveButton("Continuar") { _, _ ->
                resetScanner()
            }
            .setNegativeButton("Salir") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showErrorDialog(title: String, message: String, onDismiss: () -> Unit = {}) {
        AlertDialog.Builder(this)
            .setTitle("❌ $title")
            .setMessage(message)
            .setPositiveButton("Reintentar") { _, _ ->
                onDismiss()
            }
            .setNegativeButton("Salir") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun resetScanner() {
        isProcessing = false
        updateProximoEventoDisplay()
        barcodeView.resume()
    }
    
    override fun onResume() {
        super.onResume()
        if (::barcodeView.isInitialized && !isProcessing) {
            barcodeView.resume()
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (::barcodeView.isInitialized) {
            barcodeView.pause()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::scannerService.isInitialized) {
            scannerService.removeCallback()
        }
    }
    
    // NUEVAS FUNCIONES para buscar en SharedPreferences
    private fun buscarEmpleadoEnSharedPreferences(dni: String): EmpleadoSimple? {
        return try {
            val sharedPreferences = getSharedPreferences("EmpleadosApp", Context.MODE_PRIVATE)
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val type = object : TypeToken<List<EmpleadoSimple>>() {}.type
            val empleados: List<EmpleadoSimple> = Gson().fromJson(empleadosJson, type) ?: emptyList()
            
            empleados.find { it.dni == dni && it.activo }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun showSuccessDialogSimple(empleado: EmpleadoSimple, dni: String) {
        val horaActual = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val fechaActual = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        
        // CORREGIDO: Determinar si es entrada o salida basado en el último registro del empleado
        val ultimoRegistro = obtenerUltimoRegistroEmpleado(dni, fechaActual)
        val esEntrada = determinarSiEsEntrada(ultimoRegistro, horaActual, empleado)
        val tipoEvento = if (esEntrada) "📥 ENTRADA" else "📤 SALIDA"
        val emoji = if (esEntrada) "🌅" else "🏠"
        
        // Verificar si está dentro del horario
        val dentroHorario = if (esEntrada) {
            horaActual <= empleado.horaEntrada || 
            calcularDiferenciaMinutos(horaActual, empleado.horaEntrada) <= 15
        } else {
            horaActual >= empleado.horaSalida
        }
        
        val estadoHorario = if (dentroHorario) {
            "✅ PUNTUAL"
        } else {
            if (esEntrada) {
                val minutosRetraso = calcularDiferenciaMinutos(empleado.horaEntrada, horaActual)
                if (minutosRetraso <= 15) {
                    "⚠️ RETRASO RECUPERABLE ($minutosRetraso min)"
                } else {
                    "❌ TARDANZA ($minutosRetraso min)"
                }
            } else {
                "⏰ SALIDA TEMPRANA"
            }
        }
        
        val mensaje = buildString {
            append("$emoji $tipoEvento REGISTRADO\n\n")
            append("👤 ${empleado.nombres} ${empleado.apellidos}\n")
            append("🆔 DNI: ${empleado.dni}\n")
            append("📅 $fechaActual\n")
            append("🕐 $horaActual\n")
            append("⏰ Horario: ${empleado.horaEntrada} - ${empleado.horaSalida}\n\n")
            append("📊 Estado: $estadoHorario\n\n")
            append("✅ Registro guardado correctamente")
        }
        
        // Guardar el registro en SharedPreferences
        guardarRegistroSimple(empleado, tipoEvento, horaActual, fechaActual, estadoHorario)
        
        AlertDialog.Builder(this)
            .setTitle("✅ Asistencia Registrada")
            .setMessage(mensaje)
            .setPositiveButton("Continuar") { _, _ ->
                resetScanner()
            }
            .setNegativeButton("Salir") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun calcularDiferenciaMinutos(hora1: String, hora2: String): Int {
        return try {
            val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val time1 = formato.parse(hora1)
            val time2 = formato.parse(hora2)
            
            if (time1 != null && time2 != null) {
                val diferencia = kotlin.math.abs(time1.time - time2.time)
                (diferencia / (1000 * 60)).toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun obtenerUltimoRegistroEmpleado(dni: String, fecha: String): Map<String, String>? {
        return try {
            val sharedPreferences = getSharedPreferences("RegistrosApp", Context.MODE_PRIVATE)
            val registrosJson = sharedPreferences.getString("registros_list", "[]")
            val type = object : TypeToken<List<Map<String, String>>>() {}.type
            val registros: List<Map<String, String>> = Gson().fromJson(registrosJson, type) ?: emptyList()
            
            // Buscar el último registro del empleado en la fecha actual
            registros
                .filter { it["dni"] == dni && it["fecha"] == fecha }
                .maxByOrNull { it["timestamp"]?.toLongOrNull() ?: 0L }
                
        } catch (e: Exception) {
            null
        }
    }
    
    private fun determinarSiEsEntrada(ultimoRegistro: Map<String, String>?, horaActual: String, empleado: EmpleadoSimple): Boolean {
        return try {
            // Si no hay registro previo, es entrada
            if (ultimoRegistro == null) {
                return true
            }
            
            val ultimoTipoEvento = ultimoRegistro["tipoEvento"] ?: ""
            
            // Lógica simple pero correcta:
            // - Si el último fue ENTRADA -> ahora es SALIDA
            // - Si el último fue SALIDA -> ahora es ENTRADA
            when {
                ultimoTipoEvento.contains("ENTRADA") -> false // Próximo es SALIDA
                ultimoTipoEvento.contains("SALIDA") -> true   // Próximo es ENTRADA
                else -> {
                    // Si no hay registro claro, usar lógica de horario
                    // Si está más cerca de la hora de entrada que de salida, es entrada
                    val minutosDesdeEntrada = calcularDiferenciaMinutos(horaActual, empleado.horaEntrada)
                    val minutosHastaSalida = calcularDiferenciaMinutos(horaActual, empleado.horaSalida)
                    
                    minutosDesdeEntrada < minutosHastaSalida
                }
            }
        } catch (e: Exception) {
            // En caso de error, usar lógica de horario como fallback
            val minutosDesdeEntrada = calcularDiferenciaMinutos(horaActual, empleado.horaEntrada)
            val minutosHastaSalida = calcularDiferenciaMinutos(horaActual, empleado.horaSalida)
            
            minutosDesdeEntrada < minutosHastaSalida
        }
    }
    
    private fun guardarRegistroSimple(empleado: EmpleadoSimple, tipoEvento: String, hora: String, fecha: String, estado: String) {
        try {
            val sharedPreferences = getSharedPreferences("RegistrosApp", Context.MODE_PRIVATE)
            val registrosJson = sharedPreferences.getString("registros_list", "[]")
            val type = object : TypeToken<MutableList<Map<String, String>>>() {}.type
            val registros: MutableList<Map<String, String>> = Gson().fromJson(registrosJson, type) ?: mutableListOf()
            
            val nuevoRegistro = mapOf(
                "dni" to empleado.dni,
                "nombre" to "${empleado.nombres} ${empleado.apellidos}",
                "tipoEvento" to tipoEvento,
                "hora" to hora,
                "fecha" to fecha,
                "estado" to estado,
                "timestamp" to System.currentTimeMillis().toString()
            )
            
            registros.add(nuevoRegistro)
            
            val nuevaLista = Gson().toJson(registros)
            sharedPreferences.edit().putString("registros_list", nuevaLista).apply()
            
        } catch (e: Exception) {
            // Si falla el guardado, no importa mucho para el demo
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}