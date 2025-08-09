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
    
    private val scanCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            procesarResultado(result.result)
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
    
    private fun procesarQR(rawCode: String) {
        // Intentar extraer ID de empleado del QR
        // Formato esperado: vCard, JSON, o texto plano con ID
        
        val empleadoId = when {
            // vCard format
            rawCode.startsWith("BEGIN:VCARD") -> extraerIdDeVCard(rawCode)
            // JSON format
            rawCode.startsWith("{") -> extraerIdDeJson(rawCode)
            // Texto plano - asumir que es el ID directamente
            else -> rawCode.trim()
        }
        
        if (empleadoId.isNotEmpty()) {
            callback?.onScanSuccess(empleadoId, rawCode, ModoLectura.QR)
        } else {
            callback?.onScanError("No se pudo extraer ID del empleado del código QR")
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
        val empleadoId = rawCode.trim()
        
        if (empleadoId.isNotEmpty()) {
            callback?.onScanSuccess(empleadoId, rawCode, ModoLectura.CODE128)
        } else {
            callback?.onScanError("Código Code128 vacío")
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