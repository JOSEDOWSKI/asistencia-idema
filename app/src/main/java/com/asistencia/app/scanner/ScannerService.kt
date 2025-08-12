package com.asistencia.app.scanner

import android.content.Context
import com.asistencia.app.database.ModoLectura
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import java.util.regex.Pattern

class ScannerService(private val context: Context) {
    
    interface ScannerCallback {
        fun onScanSuccess(empleadoId: String, rawCode: String, modoDetectado: ModoLectura)
        fun onScanError(error: String)
    }
    
    private var callback: ScannerCallback? = null
    private var modoLectura: ModoLectura = ModoLectura.QR
    
    fun configurarScanner(barcodeView: DecoratedBarcodeView, modo: ModoLectura) {
        this.modoLectura = modo
        
        val formats = when (modo) {
            ModoLectura.QR -> listOf(BarcodeFormat.QR_CODE)
            ModoLectura.DNI_PDF417 -> listOf(BarcodeFormat.PDF_417)
            ModoLectura.CODE128 -> listOf(BarcodeFormat.CODE_128)
        }
        
        barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        barcodeView.decodeContinuous(scanCallback)
    }
    
    fun configurarScannerKiosco(barcodeView: DecoratedBarcodeView, modo: ModoLectura) {
        this.modoLectura = modo
        
        val formats = when (modo) {
            ModoLectura.QR -> listOf(BarcodeFormat.QR_CODE)
            ModoLectura.DNI_PDF417 -> listOf(BarcodeFormat.PDF_417)
            ModoLectura.CODE128 -> listOf(BarcodeFormat.CODE_128)
        }
        
        // IMPORTANTE: Configurar cámara frontal ANTES de inicializar el decoder
        try {
            // Pausar si está activo
            barcodeView.pause()
            
            // Configurar para cámara frontal (ID = 1)
            val cameraSettings = barcodeView.barcodeView.cameraSettings
            cameraSettings.requestedCameraId = 1 // 1 = cámara frontal, 0 = cámara trasera
            
            // Configurar decoder
            barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
            
            // Configurar callback específico para modo kiosco
            barcodeView.decodeContinuous(scanCallbackKiosco)
            
        } catch (e: Exception) {
            // Si falla la cámara frontal, intentar con trasera como fallback
            callback?.onScanError("Error configurando cámara frontal: ${e.message}. Usando cámara trasera.")
            configurarScanner(barcodeView, modo)
        }
    }
    
    private val scanCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            procesarResultado(result.result)
        }
        
        override fun possibleResultPoints(resultPoints: List<com.google.zxing.ResultPoint>) {
            // No implementado
        }
    }
    
    private val scanCallbackKiosco = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            // En modo kiosco, procesar inmediatamente y continuar escaneando
            procesarResultadoKiosco(result.result)
        }
        
        override fun possibleResultPoints(resultPoints: List<com.google.zxing.ResultPoint>) {
            // No implementado
        }
    }
    
    private fun procesarResultado(result: Result) {
        val rawCode = result.text
        val format = result.barcodeFormat
        
        try {
            when (format) {
                BarcodeFormat.QR_CODE -> procesarQR(rawCode)
                BarcodeFormat.PDF_417 -> procesarPDF417(rawCode)
                BarcodeFormat.CODE_128 -> procesarCode128(rawCode)
                else -> callback?.onScanError("Formato de código no soportado")
            }
        } catch (e: Exception) {
            callback?.onScanError("Error al procesar código: ${e.message}")
        }
    }
    
    private fun procesarResultadoKiosco(result: Result) {
        val rawCode = result.text
        val format = result.barcodeFormat
        
        try {
            when (format) {
                BarcodeFormat.QR_CODE -> procesarQRKiosco(rawCode)
                BarcodeFormat.PDF_417 -> procesarPDF417Kiosco(rawCode)
                BarcodeFormat.CODE_128 -> procesarCode128Kiosco(rawCode)
                else -> callback?.onScanError("Formato de código no soportado en modo kiosco")
            }
        } catch (e: Exception) {
            callback?.onScanError("Error al procesar código en modo kiosco: ${e.message}")
        }
    }
    
    private fun procesarQR(rawCode: String) {
        // Intentar extraer ID de empleado del QR
        // Formato esperado: vCard, JSON, o texto plano con ID
        
        val empleadoId = when {
            // vCard format
            rawCode.startsWith("BEGIN:VCARD") -> extraerIdDeVCard(rawCode)
            // JSON format
            rawCode.startsWith("{") -> extraerIdDeJson(rawCode)
            // Texto plano - asumir que es el ID directamente
            else -> {
                // Limpiar el código y validar
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
        
        if (empleadoId.isNotEmpty()) {
            callback?.onScanSuccess(empleadoId, rawCode, ModoLectura.QR)
        } else {
            callback?.onScanError("No se pudo extraer ID del empleado del código QR\n\nCódigo leído: $rawCode")
        }
    }
    
    private fun procesarPDF417(rawCode: String) {
        // Procesar código PDF417 del DNI peruano
        val dni = extraerDniDePDF417(rawCode)
        
        if (dni != null && dni.length == 8 && dni.all { it.isDigit() }) {
            callback?.onScanSuccess(dni, rawCode, ModoLectura.DNI_PDF417)
        } else {
            callback?.onScanError("DNI no válido en código PDF417")
        }
    }
    
    private fun procesarCode128(rawCode: String) {
        // Code128 puede contener ID de empleado o DNI según configuración
        val codigo = rawCode.trim()
        
        if (codigo.isEmpty()) {
            callback?.onScanError("Código Code128 vacío")
            return
        }
        
        // Intentar diferentes formatos de Code128
        val empleadoId = when {
            // Si es un DNI de 8 dígitos, usarlo directamente
            codigo.length == 8 && codigo.all { it.isDigit() } -> codigo
            
            // Si contiene solo números pero no es de 8 dígitos, intentar extraer DNI
            codigo.all { it.isDigit() } -> {
                if (codigo.length > 8) {
                    // Tomar los últimos 8 dígitos como DNI
                    codigo.takeLast(8)
                } else {
                    // Rellenar con ceros a la izquierda si es menor a 8
                    codigo.padStart(8, '0')
                }
            }
            
            // Si contiene letras y números, intentar extraer el DNI
            else -> {
                val numerosEncontrados = codigo.filter { it.isDigit() }
                when {
                    numerosEncontrados.length == 8 -> numerosEncontrados
                    numerosEncontrados.length > 8 -> numerosEncontrados.takeLast(8)
                    numerosEncontrados.length > 0 -> numerosEncontrados.padStart(8, '0')
                    else -> codigo // Usar el código tal como está si no hay números
                }
            }
        }
        
        if (empleadoId.isNotEmpty()) {
            callback?.onScanSuccess(empleadoId, rawCode, ModoLectura.CODE128)
        } else {
            callback?.onScanError("No se pudo procesar el código Code128\n\nCódigo leído: $rawCode")
        }
    }
    
    private fun extraerIdDeVCard(vcard: String): String {
        // Buscar campo personalizado con ID de empleado
        val lines = vcard.split("\n")
        
        for (line in lines) {
            when {
                line.startsWith("FN:") -> {
                    // Usar nombre completo como fallback
                    return line.substring(3).trim()
                }
                line.startsWith("ORG:") -> {
                    // Buscar ID en organización
                    val org = line.substring(4).trim()
                    if (org.contains("ID:")) {
                        return org.substringAfter("ID:").trim()
                    }
                }
                line.startsWith("NOTE:") -> {
                    // Buscar ID en notas
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
            val idPattern = Pattern.compile("\"(?:id|empleadoId|dni)\"\\s*:\\s*\"([^\"]+)\"")
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
            // El formato del DNI peruano en PDF417 varía, pero generalmente contiene:
            // - Número de DNI de 8 dígitos
            // - Otros datos separados por delimitadores
            
            // Buscar patrón de 8 dígitos consecutivos
            val dniPattern = Pattern.compile("\\b(\\d{8})\\b")
            val matcher = dniPattern.matcher(rawCode)
            
            if (matcher.find()) {
                matcher.group(1)
            } else {
                // Intentar extraer de formato específico del DNI peruano
                // Los datos suelen estar separados por @ o |
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
    
    // Funciones específicas para modo kiosco
    private fun procesarQRKiosco(rawCode: String) {
        // En modo kiosco, usar la misma lógica pero con procesamiento continuo
        val empleadoId = when {
            rawCode.startsWith("BEGIN:VCARD") -> extraerIdDeVCard(rawCode)
            rawCode.startsWith("{") -> extraerIdDeJson(rawCode)
            else -> {
                val codigo = rawCode.trim()
                if (codigo.length == 8 && codigo.all { it.isDigit() }) {
                    codigo
                } else if (codigo.isNotEmpty()) {
                    val numerosEncontrados = codigo.filter { it.isDigit() }
                    if (numerosEncontrados.length == 8) {
                        numerosEncontrados
                    } else {
                        codigo
                    }
                } else {
                    ""
                }
            }
        }
        
        if (empleadoId.isNotEmpty()) {
            callback?.onScanSuccess(empleadoId, rawCode, ModoLectura.QR)
        } else {
            callback?.onScanError("QR no válido en modo kiosco")
        }
    }
    
    private fun procesarPDF417Kiosco(rawCode: String) {
        val dni = extraerDniDePDF417(rawCode)
        
        if (dni != null && dni.length == 8 && dni.all { it.isDigit() }) {
            callback?.onScanSuccess(dni, rawCode, ModoLectura.DNI_PDF417)
        } else {
            callback?.onScanError("DNI no válido en modo kiosco")
        }
    }
    
    private fun procesarCode128Kiosco(rawCode: String) {
        val codigo = rawCode.trim()
        
        if (codigo.isEmpty()) {
            callback?.onScanError("Código vacío en modo kiosco")
            return
        }
        
        val empleadoId = when {
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
        
        if (empleadoId.isNotEmpty()) {
            callback?.onScanSuccess(empleadoId, rawCode, ModoLectura.CODE128)
        } else {
            callback?.onScanError("Código no válido en modo kiosco")
        }
    }
    
    fun setCallback(callback: ScannerCallback) {
        this.callback = callback
    }
    
    fun removeCallback() {
        this.callback = null
    }
    
    companion object {
        fun generarQRParaEmpleado(empleadoId: String, nombre: String): String {
            // Generar QR en formato JSON simple
            return """{"id":"$empleadoId","nombre":"$nombre","tipo":"empleado"}"""
        }
        
        fun validarDNI(dni: String): Boolean {
            return dni.length == 8 && dni.all { it.isDigit() }
        }
    }
}