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
    private var esModoKiosco = false
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val LOCATION_PERMISSION_REQUEST = 101
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // VERIFICAR MODO ANTES DE CONFIGURAR EL LAYOUT
        val modoOperacion = verificarModoOperacion()
        esModoKiosco = (modoOperacion == ModoOperacion.KIOSCO)
        
        // USAR LAYOUT ESPECÍFICO SEGÚN EL MODO
        if (esModoKiosco) {
            setContentView(R.layout.activity_scanner_kiosco) // Layout con cámara frontal
            Toast.makeText(this, "📱 Iniciando modo KIOSCO con cámara frontal", Toast.LENGTH_LONG).show()
        } else {
            setContentView(R.layout.activity_scanner) // Layout con cámara trasera
            Toast.makeText(this, "🔓 Iniciando modo AUTOSERVICIO con cámara trasera", Toast.LENGTH_SHORT).show()
        }
        
        initializeComponents()
        setupUI()
        checkPermissions()
    }
    
    private fun verificarModoOperacion(): ModoOperacion {
        return try {
            // Cargar desde SharedPreferences directamente (más rápido y confiable)
            val sharedPreferences = getSharedPreferences("ConfiguracionApp", MODE_PRIVATE)
            val modoOperacionIndex = sharedPreferences.getInt("modo_operacion", 0)
            
            when (modoOperacionIndex) {
                0 -> ModoOperacion.AUTOSERVICIO
                1 -> ModoOperacion.KIOSCO
                else -> ModoOperacion.AUTOSERVICIO
            }
        } catch (e: Exception) {
            // Fallback a autoservicio
            ModoOperacion.AUTOSERVICIO
        }
    }
    
    private fun configurarCamaraFrontalInicial() {
        try {
            // MÉTODO DEFINITIVO: Cambiar el atributo XML dinámicamente
            
            // 1. Obtener el layout padre
            val parentLayout = findViewById<android.widget.RelativeLayout>(android.R.id.content)
            
            // 2. Remover el BarcodeView actual
            val currentBarcodeView = findViewById<com.journeyapps.barcodescanner.DecoratedBarcodeView>(R.id.barcode_scanner)
            val parent = currentBarcodeView.parent as android.view.ViewGroup
            val layoutParams = currentBarcodeView.layoutParams
            parent.removeView(currentBarcodeView)
            
            // 3. Crear nuevo BarcodeView con cámara frontal
            val nuevoBarcodeView = com.journeyapps.barcodescanner.DecoratedBarcodeView(this, null)
            nuevoBarcodeView.id = R.id.barcode_scanner
            nuevoBarcodeView.layoutParams = layoutParams
            
            // 4. CONFIGURAR CÁMARA FRONTAL ANTES DE AGREGAR AL LAYOUT
            val cameraSettings = nuevoBarcodeView.barcodeView.cameraSettings
            cameraSettings.requestedCameraId = 1 // 1 = cámara frontal
            
            // 5. Agregar el nuevo BarcodeView
            parent.addView(nuevoBarcodeView, 0) // Agregar como primer hijo
            
            // 6. Actualizar la referencia
            barcodeView = nuevoBarcodeView
            
            Toast.makeText(this, "📱 Cámara frontal configurada exitosamente", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ Error configurando cámara frontal: ${e.message}", Toast.LENGTH_LONG).show()
            
            // Fallback: usar cámara trasera
            esModoKiosco = false
        }
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
        
        // CONFIGURAR CÁMARA FRONTAL INMEDIATAMENTE SI ES MODO KIOSCO
        if (esModoKiosco) {
            configurarCamaraFrontal()
        }
        
        scannerService.setCallback(this)
    }
    
    private fun configurarCamaraFrontal() {
        try {
            // Configurar cámara frontal INMEDIATAMENTE después de inicializar el BarcodeView
            val cameraSettings = barcodeView.barcodeView.cameraSettings
            cameraSettings.requestedCameraId = 1 // 1 = cámara frontal, 0 = cámara trasera
            
            Toast.makeText(this, "📱 Cámara frontal configurada para modo kiosco", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ Error configurando cámara frontal: ${e.message}", Toast.LENGTH_LONG).show()
            
            // Si falla, cambiar a modo autoservicio
            esModoKiosco = false
        }
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
            try {
                val dispositivo = repository.getDispositivo()
                if (dispositivo.capturaUbicacion && locationPermission != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } catch (e: Exception) {
                // Ignorar error de BD
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
                // Intentar cargar configuración desde base de datos
                var modoLectura = ModoLectura.QR
                var modoOperacion = ModoOperacion.AUTOSERVICIO
                var capturaUbicacion = false
                
                try {
                    val dispositivo = repository.getDispositivo()
                    modoLectura = dispositivo.modoLectura
                    modoOperacion = dispositivo.modoOperacion
                    capturaUbicacion = dispositivo.capturaUbicacion
                } catch (dbError: Exception) {
                    // Si falla la BD, cargar desde SharedPreferences
                    val sharedPreferences = getSharedPreferences("ConfiguracionApp", MODE_PRIVATE)
                    val modoLecturaIndex = sharedPreferences.getInt("modo_lectura", 0)
                    val modoOperacionIndex = sharedPreferences.getInt("modo_operacion", 0)
                    
                    modoLectura = when (modoLecturaIndex) {
                        0 -> ModoLectura.QR
                        1 -> ModoLectura.DNI_PDF417
                        2 -> ModoLectura.CODE128
                        else -> ModoLectura.QR
                    }
                    
                    modoOperacion = when (modoOperacionIndex) {
                        0 -> ModoOperacion.AUTOSERVICIO
                        1 -> ModoOperacion.KIOSCO
                        else -> ModoOperacion.AUTOSERVICIO
                    }
                    
                    capturaUbicacion = sharedPreferences.getBoolean("captura_ubicacion", false)
                }
                
                // Configurar scanner según el modo de operación
                esModoKiosco = (modoOperacion == ModoOperacion.KIOSCO)
                
                // IMPORTANTE: Pausar primero para reconfigurar
                barcodeView.pause()
                
                if (esModoKiosco) {
                    configurarModoKiosco(modoLectura)
                } else {
                    configurarModoAutoservicio(modoLectura)
                }
                
                // Obtener ubicación si está habilitada
                if (capturaUbicacion) {
                    getCurrentLocation()
                }
                
                // Esperar un momento antes de reanudar para que la configuración se aplique
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    barcodeView.resume()
                }, 500)
                
            } catch (e: Exception) {
                showErrorDialog("Error", "Error al inicializar el scanner: ${e.message}") {
                    finish()
                }
            }
        }
    }
    
    private fun configurarModoKiosco(modoLectura: ModoLectura) {
        try {
            // MÉTODO ALTERNATIVO: Recrear el BarcodeView con configuración de cámara frontal
            recrearBarcodeViewConCamaraFrontal(modoLectura)
            
            // Mostrar información del modo Kiosco
            updateProximoEventoDisplay("📱 MODO KIOSCO - Cámara Frontal Activa")
            tvEmpleadoInfo.text = "Acerque su código QR a la cámara frontal"
            
            // Ocultar botón de linterna (no disponible en cámara frontal)
            btnLinterna.visibility = View.GONE
            
            // Configurar UI para modo kiosco
            configurarUIKiosco()
            
        } catch (e: Exception) {
            // Si falla la configuración de cámara frontal, mostrar error
            showErrorDialog(
                "Error de Cámara Frontal", 
                "No se pudo configurar la cámara frontal. Usando cámara trasera.\n\nError: ${e.message}"
            ) {
                // Fallback a modo autoservicio con cámara trasera
                configurarModoAutoservicio(modoLectura)
            }
        }
    }
    
    private fun recrearBarcodeViewConCamaraFrontal(modoLectura: ModoLectura) {
        try {
            // SOLUCIÓN DEFINITIVA: Recrear completamente el DecoratedBarcodeView
            
            // 1. Pausar y remover el BarcodeView actual
            barcodeView.pause()
            
            // 2. Obtener el contenedor padre
            val parentLayout = barcodeView.parent as android.view.ViewGroup
            val layoutParams = barcodeView.layoutParams
            
            // 3. Remover el BarcodeView actual
            parentLayout.removeView(barcodeView)
            
            // 4. Crear un nuevo DecoratedBarcodeView con configuración de cámara frontal
            val nuevoBarcodeView = com.journeyapps.barcodescanner.DecoratedBarcodeView(this)
            nuevoBarcodeView.layoutParams = layoutParams
            nuevoBarcodeView.id = R.id.barcode_scanner
            
            // 5. CONFIGURAR CÁMARA FRONTAL ANTES DE CUALQUIER OTRA CONFIGURACIÓN
            val cameraSettings = nuevoBarcodeView.barcodeView.cameraSettings
            cameraSettings.requestedCameraId = 1 // 1 = cámara frontal
            
            // 6. Configurar formatos de código
            val formats = when (modoLectura) {
                ModoLectura.QR -> listOf(com.google.zxing.BarcodeFormat.QR_CODE)
                ModoLectura.DNI_PDF417 -> listOf(com.google.zxing.BarcodeFormat.PDF_417)
                ModoLectura.CODE128 -> listOf(com.google.zxing.BarcodeFormat.CODE_128)
            }
            
            // 7. Configurar decoder
            nuevoBarcodeView.barcodeView.decoderFactory = com.journeyapps.barcodescanner.DefaultDecoderFactory(formats)
            
            // 8. Configurar callback personalizado para kiosco
            nuevoBarcodeView.decodeContinuous(object : com.journeyapps.barcodescanner.BarcodeCallback {
                override fun barcodeResult(result: com.journeyapps.barcodescanner.BarcodeResult) {
                    procesarResultadoKiosco(result.text, modoLectura)
                }
                
                override fun possibleResultPoints(resultPoints: List<com.google.zxing.ResultPoint>) {
                    // No implementado
                }
            })
            
            // 9. Agregar el nuevo BarcodeView al layout
            parentLayout.addView(nuevoBarcodeView)
            
            // 10. Actualizar la referencia
            barcodeView = nuevoBarcodeView
            
            // Mostrar mensaje de confirmación
            Toast.makeText(this, "📱 Cámara frontal configurada para modo kiosco", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            throw Exception("Error recreando BarcodeView con cámara frontal: ${e.message}")
        }
    }
    
    private fun configurarModoAutoservicio(modoLectura: ModoLectura) {
        // Configurar scanner normal con cámara trasera
        scannerService.configurarScanner(barcodeView, modoLectura)
        
        // Mostrar modo de lectura actual
        updateProximoEventoDisplay("🔓 AUTOSERVICIO - ${getModoLecturaTexto(modoLectura)}")
        tvEmpleadoInfo.text = "Apunte la cámara hacia el código"
        
        // Botón de linterna permanece oculto
        btnLinterna.visibility = View.GONE
    }
    
    private fun configurarUIKiosco() {
        // Configurar UI específica para modo kiosco
        // Pantalla siempre encendida
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Ocultar barra de navegación y estado para pantalla completa
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        // Configurar texto más grande para visibilidad desde lejos
        tvProximoEvento.textSize = 24f
        tvEmpleadoInfo.textSize = 20f
        
        // Cambiar colores para mejor visibilidad
        tvProximoEvento.setTextColor(android.graphics.Color.WHITE)
        tvEmpleadoInfo.setTextColor(android.graphics.Color.YELLOW)
        
        // Fondo oscuro para mejor contraste
        findViewById<View>(android.R.id.content).setBackgroundColor(android.graphics.Color.BLACK)
    }
    
    private fun getModoLecturaTexto(modo: ModoLectura): String {
        return when (modo) {
            ModoLectura.QR -> "QR Code"
            ModoLectura.DNI_PDF417 -> "DNI (PDF417)"
            ModoLectura.CODE128 -> "Código de Barras"
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
            Toast.makeText(this, "Función de linterna en desarrollo", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al controlar la linterna", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateProximoEventoDisplay(mensaje: String = "Apunte la cámara al código para escanear") {
        tvProximoEvento.text = mensaje
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
                val empleado = buscarEmpleado(empleadoId)
                
                if (empleado != null) {
                    if (esModoKiosco) {
                        procesarAsistenciaKiosco(empleado, empleadoId)
                    } else {
                        showSuccessDialogSimple(empleado, empleadoId)
                    }
                } else {
                    if (esModoKiosco) {
                        mostrarErrorKiosco("Empleado no encontrado", "ID: $empleadoId")
                    } else {
                        showErrorDialog(
                            "Empleado no encontrado",
                            "No se encontró empleado con ID: $empleadoId\n\nCódigo escaneado: $rawCode"
                        ) { resetScanner() }
                    }
                }
                
            } catch (e: Exception) {
                if (esModoKiosco) {
                    mostrarErrorKiosco("Error", e.message ?: "Error desconocido")
                } else {
                    showErrorDialog("Error", "Error al procesar: ${e.message}") { resetScanner() }
                }
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    override fun onScanError(error: String) {
        if (esModoKiosco) {
            mostrarErrorKiosco("Error de escaneo", error)
        } else {
            showErrorDialog("Error de escaneo", error) { resetScanner() }
        }
    }
    
    private fun buscarEmpleado(dni: String): EmpleadoSimple? {
        return try {
            val sharedPreferences = getSharedPreferences("EmpleadosApp", Context.MODE_PRIVATE)
            
            // Primero buscar en empleados flexibles
            val empleadoFlexible = buscarEmpleadoFlexible(dni)
            if (empleadoFlexible != null) {
                return empleadoFlexible.toEmpleadoSimple()
            }
            
            // Si no está en flexibles, buscar en simples
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val type = object : TypeToken<List<EmpleadoSimple>>() {}.type
            val empleados: List<EmpleadoSimple> = Gson().fromJson(empleadosJson, type) ?: emptyList()
            
            empleados.find { it.dni == dni && it.activo }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun buscarEmpleadoFlexible(dni: String): EmpleadoFlexible? {
        return try {
            val sharedPreferences = getSharedPreferences("EmpleadosApp", Context.MODE_PRIVATE)
            val empleadosFlexiblesJson = sharedPreferences.getString("empleados_flexibles", "[]")
            val type = object : TypeToken<List<EmpleadoFlexible>>() {}.type
            val empleadosFlexibles: List<EmpleadoFlexible> = Gson().fromJson(empleadosFlexiblesJson, type) ?: emptyList()
            
            empleadosFlexibles.find { it.dni == dni && it.activo }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun procesarAsistenciaKiosco(empleado: EmpleadoSimple, dni: String) {
        val horaActual = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val fechaActual = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        
        // Verificar si es empleado flexible
        val empleadoFlexible = buscarEmpleadoFlexible(dni)
        val esFlexible = empleadoFlexible != null
        
        // Obtener horario específico para hoy (si es flexible)
        val (horaEntrada, horaSalida) = if (esFlexible && empleadoFlexible != null) {
            if (empleadoFlexible.trabajaHoy()) {
                val horarioHoy = empleadoFlexible.getHorarioHoy()
                horarioHoy ?: Pair(empleado.horaEntrada, empleado.horaSalida)
            } else {
                mostrarErrorKiosco("No trabaja hoy", "${empleado.nombres} ${empleado.apellidos}")
                return
            }
        } else {
            Pair(empleado.horaEntrada, empleado.horaSalida)
        }
        
        // Determinar si es entrada o salida
        val ultimoRegistro = obtenerUltimoRegistroEmpleado(dni, fechaActual)
        val esEntrada = determinarSiEsEntrada(ultimoRegistro, horaActual, empleado)
        val tipoEvento = if (esEntrada) "📥 ENTRADA" else "📤 SALIDA"
        
        // Verificar si está dentro del horario
        val dentroHorario = if (esEntrada) {
            horaActual <= horaEntrada || 
            calcularDiferenciaMinutos(horaActual, horaEntrada) <= 15
        } else {
            horaActual >= horaSalida
        }
        
        val estadoHorario = if (dentroHorario) {
            "✅ PUNTUAL"
        } else {
            if (esEntrada) {
                val minutosRetraso = calcularDiferenciaMinutos(horaEntrada, horaActual)
                if (minutosRetraso <= 15) {
                    "⚠️ RETRASO RECUPERABLE ($minutosRetraso min)"
                } else {
                    "❌ TARDANZA ($minutosRetraso min)"
                }
            } else {
                "⏰ SALIDA TEMPRANA"
            }
        }
        
        // Guardar el registro
        guardarRegistroFlexible(empleado, tipoEvento, horaActual, fechaActual, estadoHorario, esFlexible)
        
        // Mostrar confirmación rápida en modo kiosco
        mostrarConfirmacionKiosco(empleado, tipoEvento, estadoHorario, horaActual)
    }
    
    private fun mostrarConfirmacionKiosco(empleado: EmpleadoSimple, tipoEvento: String, estado: String, hora: String) {
        // Actualizar UI con información del registro
        tvProximoEvento.text = "✅ REGISTRO EXITOSO"
        tvEmpleadoInfo.text = "${empleado.nombres} ${empleado.apellidos} - $tipoEvento - $hora"
        
        // Cambiar colores temporalmente para feedback visual
        tvProximoEvento.setTextColor(android.graphics.Color.GREEN)
        tvEmpleadoInfo.setTextColor(android.graphics.Color.WHITE)
        
        // Mostrar toast rápido
        Toast.makeText(this, "✅ ${empleado.nombres} - $tipoEvento\n$estado", Toast.LENGTH_SHORT).show()
        
        // Reiniciar automáticamente después de 2 segundos
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            resetScannerKiosco()
        }, 2000)
    }
    
    private fun mostrarErrorKiosco(titulo: String, mensaje: String) {
        // Mostrar error brevemente en modo kiosco
        tvProximoEvento.text = "❌ $titulo"
        tvEmpleadoInfo.text = mensaje
        
        // Cambiar colores para indicar error
        tvProximoEvento.setTextColor(android.graphics.Color.RED)
        tvEmpleadoInfo.setTextColor(android.graphics.Color.YELLOW)
        
        // Mostrar toast
        Toast.makeText(this, "❌ $titulo: $mensaje", Toast.LENGTH_SHORT).show()
        
        // Reiniciar automáticamente después de 3 segundos
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            resetScannerKiosco()
        }, 3000)
    }
    
    private fun resetScannerKiosco() {
        // Restaurar UI del modo kiosco
        tvProximoEvento.text = "📱 MODO KIOSCO - Cámara Frontal Activa"
        tvProximoEvento.setTextColor(android.graphics.Color.WHITE)
        
        tvEmpleadoInfo.text = "Acerque su código QR a la cámara frontal"
        tvEmpleadoInfo.setTextColor(android.graphics.Color.YELLOW)
        
        // Reiniciar el procesamiento
        isProcessing = false
        
        // La cámara sigue activa automáticamente en modo kiosco
        barcodeView.resume()
    }
    
    private fun procesarResultadoKiosco(rawCode: String, modoDetectado: ModoLectura) {
        if (isProcessing) return
        
        isProcessing = true
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Extraer ID del empleado del código escaneado
                val empleadoId = extraerIdDelCodigo(rawCode, modoDetectado)
                
                if (empleadoId.isNotEmpty()) {
                    val empleado = buscarEmpleado(empleadoId)
                    
                    if (empleado != null) {
                        procesarAsistenciaKiosco(empleado, empleadoId)
                    } else {
                        mostrarErrorKiosco("Empleado no encontrado", "ID: $empleadoId")
                    }
                } else {
                    mostrarErrorKiosco("Código no válido", "No se pudo leer el código QR")
                }
                
            } catch (e: Exception) {
                mostrarErrorKiosco("Error", e.message ?: "Error desconocido")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun extraerIdDelCodigo(rawCode: String, modo: ModoLectura): String {
        return try {
            when (modo) {
                ModoLectura.QR -> {
                    // Intentar extraer ID de empleado del QR
                    when {
                        // vCard format
                        rawCode.startsWith("BEGIN:VCARD") -> extraerIdDeVCard(rawCode)
                        // JSON format
                        rawCode.startsWith("{") -> extraerIdDeJson(rawCode)
                        // Texto plano - asumir que es el ID directamente
                        else -> {
                            val codigo = rawCode.trim()
                            
                            // Si es un DNI de 8 dígitos, usarlo directamente
                            if (codigo.length == 8 && codigo.all { it.isDigit() }) {
                                codigo
                            } else if (codigo.isNotEmpty()) {
                                // Si no es DNI pero tiene contenido, intentar extraer números
                                val numerosEncontrados = codigo.filter { it.isDigit() }
                                if (numerosEncontrados.length == 8) {
                                    numerosEncontrados
                                } else {
                                    codigo // Usar tal como está
                                }
                            } else {
                                ""
                            }
                        }
                    }
                }
                ModoLectura.DNI_PDF417 -> {
                    // Extraer DNI de código PDF417
                    extraerDniDePDF417(rawCode) ?: ""
                }
                ModoLectura.CODE128 -> {
                    // Procesar Code128
                    val codigo = rawCode.trim()
                    when {
                        codigo.length == 8 && codigo.all { it.isDigit() } -> codigo
                        codigo.all { it.isDigit() } -> {
                            if (codigo.length > 8) {
                                codigo.takeLast(8)
                            } else {
                                codigo.padStart(8, '0')
                            }
                        }
                        else -> {
                            val numerosEncontrados = codigo.filter { it.isDigit() }
                            when {
                                numerosEncontrados.length == 8 -> numerosEncontrados
                                numerosEncontrados.length > 8 -> numerosEncontrados.takeLast(8)
                                numerosEncontrados.length > 0 -> numerosEncontrados.padStart(8, '0')
                                else -> codigo
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun extraerIdDeVCard(vcard: String): String {
        val lines = vcard.split("\n")
        
        for (line in lines) {
            when {
                line.startsWith("FN:") -> {
                    return line.substring(3).trim()
                }
                line.startsWith("ORG:") -> {
                    val org = line.substring(4).trim()
                    if (org.contains("ID:")) {
                        return org.substringAfter("ID:").trim()
                    }
                }
                line.startsWith("NOTE:") -> {
                    val note = line.substring(5).trim()
                    if (note.contains("ID:")) {
                        return note.substringAfter("ID:").trim()
                    }
                }
            }
        }
        
        return ""
    }
    
    private fun extraerIdDeJson(json: String): String {
        return try {
            // Buscar campo "id", "empleadoId", "dni" en JSON
            val idPattern = java.util.regex.Pattern.compile("\"(?:id|empleadoId|dni)\"\\s*:\\s*\"([^\"]+)\"")
            val matcher = idPattern.matcher(json)
            
            if (matcher.find()) {
                matcher.group(1) ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun extraerDniDePDF417(rawCode: String): String? {
        return try {
            // Buscar patrón de 8 dígitos consecutivos
            val dniPattern = java.util.regex.Pattern.compile("\\b(\\d{8})\\b")
            val matcher = dniPattern.matcher(rawCode)
            
            if (matcher.find()) {
                matcher.group(1)
            } else {
                // Intentar extraer de formato específico del DNI peruano
                val parts = rawCode.split("[@|]")
                
                for (part in parts) {
                    val cleanPart = part.trim()
                    if (cleanPart.length == 8 && cleanPart.all { it.isDigit() }) {
                        return cleanPart
                    }
                }
                
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun showSuccessDialogSimple(empleado: EmpleadoSimple, dni: String) {
        val horaActual = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val fechaActual = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        
        // Verificar si es empleado flexible
        val empleadoFlexible = buscarEmpleadoFlexible(dni)
        val esFlexible = empleadoFlexible != null
        
        // Obtener horario específico para hoy (si es flexible)
        val (horaEntrada, horaSalida) = if (esFlexible && empleadoFlexible != null) {
            if (empleadoFlexible.trabajaHoy()) {
                val horarioHoy = empleadoFlexible.getHorarioHoy()
                horarioHoy ?: Pair(empleado.horaEntrada, empleado.horaSalida)
            } else {
                // No trabaja hoy
                showErrorDialog(
                    "No trabaja hoy",
                    "⏰ ${empleado.nombres} ${empleado.apellidos} no tiene horario configurado para hoy.\n\n" +
                    "📅 Días de trabajo: ${empleadoFlexible.getDescripcionHorarios()}"
                ) { resetScanner() }
                return
            }
        } else {
            Pair(empleado.horaEntrada, empleado.horaSalida)
        }
        
        // Determinar si es entrada o salida basado en el último registro del empleado
        val ultimoRegistro = obtenerUltimoRegistroEmpleado(dni, fechaActual)
        val esEntrada = determinarSiEsEntrada(ultimoRegistro, horaActual, empleado)
        val tipoEvento = if (esEntrada) "📥 ENTRADA" else "📤 SALIDA"
        val emoji = if (esEntrada) "🌅" else "🏠"
        
        // Verificar si está dentro del horario (usando horario específico del día)
        val dentroHorario = if (esEntrada) {
            horaActual <= horaEntrada || 
            calcularDiferenciaMinutos(horaActual, horaEntrada) <= 15
        } else {
            horaActual >= horaSalida
        }
        
        val estadoHorario = if (dentroHorario) {
            "✅ PUNTUAL"
        } else {
            if (esEntrada) {
                val minutosRetraso = calcularDiferenciaMinutos(horaEntrada, horaActual)
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
            
            if (esFlexible) {
                append("⏰ Horario hoy: $horaEntrada - $horaSalida\n")
                append("📋 Tipo: Horario Flexible\n")
                if (empleadoFlexible != null) {
                    append("📊 ${empleadoFlexible.getEstadoActual()}\n")
                }
            } else {
                append("⏰ Horario: $horaEntrada - $horaSalida\n")
                append("📋 Tipo: Horario Fijo\n")
            }
            
            append("\n📊 Estado: $estadoHorario\n\n")
            append("✅ Registro guardado correctamente")
        }
        
        // Guardar el registro en SharedPreferences
        guardarRegistroFlexible(empleado, tipoEvento, horaActual, fechaActual, estadoHorario, esFlexible)
        
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
            val ultimaHora = ultimoRegistro["hora"] ?: ""
            
            // NUEVA LÓGICA MEJORADA para permitir múltiples entradas/salidas
            when {
                ultimoTipoEvento.contains("ENTRADA") -> {
                    // Si el último fue ENTRADA, verificar si ha pasado suficiente tiempo para una nueva entrada
                    val minutosDesdeUltimoRegistro = if (ultimaHora.isNotEmpty()) {
                        calcularDiferenciaMinutos(ultimaHora, horaActual)
                    } else {
                        0
                    }
                    
                    // Si han pasado más de 30 minutos desde la última entrada, permitir nueva entrada
                    // Esto permite horarios partidos (ej: salió a almorzar y regresa)
                    if (minutosDesdeUltimoRegistro > 30) {
                        // Verificar si está en horario de entrada (mañana o tarde)
                        esHorarioDeEntrada(horaActual, empleado)
                    } else {
                        false // Próximo es SALIDA (muy poco tiempo desde última entrada)
                    }
                }
                
                ultimoTipoEvento.contains("SALIDA") -> {
                    // Si el último fue SALIDA, verificar si puede ser una nueva entrada
                    val minutosDesdeUltimoRegistro = if (ultimaHora.isNotEmpty()) {
                        calcularDiferenciaMinutos(ultimaHora, horaActual)
                    } else {
                        0
                    }
                    
                    // Si han pasado más de 15 minutos desde la salida, permitir nueva entrada
                    if (minutosDesdeUltimoRegistro > 15) {
                        true // Permitir nueva ENTRADA
                    } else {
                        // Si es muy poco tiempo, verificar por horario
                        esHorarioDeEntrada(horaActual, empleado)
                    }
                }
                
                else -> {
                    // Si no hay registro claro, usar lógica de horario
                    esHorarioDeEntrada(horaActual, empleado)
                }
            }
        } catch (e: Exception) {
            // En caso de error, usar lógica de horario como fallback
            esHorarioDeEntrada(horaActual, empleado)
        }
    }
    
    private fun esHorarioDeEntrada(horaActual: String, empleado: EmpleadoSimple): Boolean {
        return try {
            // Lógica mejorada para determinar si es horario de entrada
            val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val actual = formato.parse(horaActual)
            val horaEntrada = formato.parse(empleado.horaEntrada)
            val horaSalida = formato.parse(empleado.horaSalida)
            
            if (actual != null && horaEntrada != null && horaSalida != null) {
                // Calcular punto medio del horario laboral
                val puntoMedio = java.util.Date((horaEntrada.time + horaSalida.time) / 2)
                
                // Si está antes del punto medio, probablemente es entrada
                // Si está después del punto medio, probablemente es salida
                actual.before(puntoMedio)
            } else {
                // Fallback: si está más cerca de la hora de entrada que de salida
                val minutosDesdeEntrada = calcularDiferenciaMinutos(horaActual, empleado.horaEntrada)
                val minutosHastaSalida = calcularDiferenciaMinutos(horaActual, empleado.horaSalida)
                
                minutosDesdeEntrada < minutosHastaSalida
            }
        } catch (e: Exception) {
            true // En caso de error, asumir entrada
        }
    }
    
    private fun guardarRegistroFlexible(empleado: EmpleadoSimple, tipoEvento: String, hora: String, fecha: String, estado: String, esFlexible: Boolean) {
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
                "esFlexible" to esFlexible.toString(),
                "tipoHorario" to if (esFlexible) "FLEXIBLE" else "FIJO",
                "timestamp" to System.currentTimeMillis().toString()
            )
            
            registros.add(nuevoRegistro)
            
            val nuevaLista = Gson().toJson(registros)
            sharedPreferences.edit().putString("registros_list", nuevaLista).apply()
            
        } catch (e: Exception) {
            // Si falla el guardado, no importa mucho para el demo
        }
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
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}