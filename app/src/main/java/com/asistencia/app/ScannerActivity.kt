package com.asistencia.app

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.google.zxing.ResultPoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScannerActivity : AppCompatActivity() {
    
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var asistenciaManager: AsistenciaManager
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private var tipoRegistro: String = "ENTRADA"
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
    
    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (result.text != null) {
                processBarcodeResult(result.text)
            }
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        
        // Obtener el tipo de registro del intent
        tipoRegistro = intent.getStringExtra("TIPO_REGISTRO") ?: "ENTRADA"
        
        asistenciaManager = AsistenciaManager(this)
        sharedPreferences = getSharedPreferences("AsistenciaApp", Context.MODE_PRIVATE)
        barcodeView = findViewById(R.id.barcode_scanner)
        
        // Verificar permisos de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        } else {
            ActivityCompat.requestPermissions(this, 
                arrayOf(Manifest.permission.CAMERA), 
                CAMERA_PERMISSION_REQUEST)
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(this, "Se necesita permiso de cámara para escanear códigos", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun startScanning() {
        barcodeView.decodeContinuous(callback)
    }
    
    private fun processBarcodeResult(codigoEscaneado: String) {
        // Pausar el scanner para evitar múltiples lecturas
        barcodeView.pause()
        
        // Limpiar y validar el código escaneado
        val dni = codigoEscaneado.trim()
        
        // Validar formato de DNI (solo números, 8 dígitos)
        if (!validarFormatoDNI(dni)) {
            mostrarErrorYReiniciar(
                "❌ Código no válido",
                "El código escaneado no tiene formato de DNI válido:\n\"$codigoEscaneado\"\n\nDebe ser un número de 8 dígitos."
            )
            return
        }
        
        // Buscar el personal por DNI
        val personalJson = sharedPreferences.getString("personal_list", "[]")
        val type = object : TypeToken<List<Personal>>() {}.type
        val personalList: List<Personal> = gson.fromJson(personalJson, type) ?: emptyList()
        
        // Verificar si hay empleados registrados
        if (personalList.isEmpty()) {
            mostrarErrorYReiniciar(
                "❌ Sin empleados registrados",
                "No hay empleados registrados en el sistema.\n\nPor favor, registre empleados primero en 'Gestión de Personal'."
            )
            return
        }
        
        val personal = personalList.find { it.dni == dni }
        
        if (personal == null) {
            // Mostrar DNIs válidos para ayudar al usuario
            val dnisValidos = personalList.map { it.dni }.sorted().joinToString(", ")
            mostrarErrorYReiniciar(
                "❌ Empleado no encontrado",
                "El DNI escaneado no corresponde a ningún empleado registrado:\n\n" +
                "DNI escaneado: $dni\n\n" +
                "DNIs válidos registrados:\n$dnisValidos\n\n" +
                "Por favor, verifique el código o registre al empleado en 'Gestión de Personal'."
            )
            return
        }
        
        // Verificar si el empleado debe trabajar hoy
        if (!verificarEmpleadoActivo(personal)) {
            val diaSemana = java.text.SimpleDateFormat("EEEE", java.util.Locale("es", "ES"))
                .format(java.util.Date())
            mostrarErrorYReiniciar(
                "❌ Empleado no programado",
                "El empleado ${personal.nombre} no tiene horario configurado para trabajar hoy ($diaSemana).\n\n" +
                "Verifique la configuración de horarios en 'Gestión de Personal'."
            )
            return
        }
        
        // Verificar si tiene horario partido y usar el manager apropiado
        val horarioDia = personal.getHorarioDia(java.text.SimpleDateFormat("EEEE", java.util.Locale("es", "ES")).format(java.util.Date()))
        
        if (horarioDia.esHorarioPartido) {
            // Usar el sistema de horarios partidos
            val horarioPartidoManager = HorarioPartidoManager(this)
            val registroExtendido = horarioPartidoManager.registrarAsistenciaPartida(dni, personal)
            
            if (registroExtendido == null) {
                mostrarErrorYReiniciar(
                    "❌ Error de registro",
                    "No se pudo registrar la asistencia. Verifique la configuración de horarios."
                )
                return
            }
            
            // Mostrar mensaje detallado para horario partido
            mostrarMensajeHorarioPartido(personal, registroExtendido)
            
        } else {
            // Usar el sistema tradicional
            val registro = asistenciaManager.registrarAsistencia(dni, tipoRegistro, personal)
            
            // Verificar alertas de tardanzas
            val tardanzasManager = TardanzasManager(this)
            val mensajeAlerta = tardanzasManager.getMensajeAlerta(dni, personal.nombre)
            
            // Obtener información adicional sobre el retraso
            val horaEsperada = if (tipoRegistro == "ENTRADA") horarioDia.entrada else horarioDia.salida
            
            // Mostrar mensaje de confirmación tradicional
            val emoji = if (tipoRegistro == "ENTRADA") "📥" else "📤"
            val estadoTarde = if (registro.llegadaTarde) {
                val minutosRetraso = asistenciaManager.getMinutosRetraso(registro.hora, horaEsperada)
                "\n⚠️ LLEGADA TARDÍA ($minutosRetraso min de retraso)"
            } else {
                val configuracionManager = ConfiguracionManager(this)
                val tolerancia = configuracionManager.toleranciaMinutos
                if (tolerancia > 0 && tipoRegistro == "ENTRADA") {
                    "\n✅ PUNTUAL (dentro de tolerancia de $tolerancia min)"
                } else {
                    ""
                }
            }
            
            val mensaje = "$emoji ${tipoRegistro.capitalize()} registrada\n" +
                    "👤 ${personal.nombre}\n" +
                    "🆔 DNI: $dni\n" +
                    "📅 ${registro.diaSemana}, ${registro.fecha}\n" +
                    "🕐 ${registro.hora}" +
                    (if (horaEsperada.isNotEmpty()) " (Esperado: $horaEsperada)" else "") +
                    estadoTarde
            
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            
            // Mostrar alerta de tardanzas si es necesario
            mensajeAlerta?.let { alerta ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Alerta de Tardanzas")
                    .setMessage(alerta)
                    .setPositiveButton("Entendido", null)
                    .show()
            }
        }
        
        finish()
    }
    
    private fun validarFormatoDNI(dni: String): Boolean {
        // Verificar que solo contenga números y tenga exactamente 8 dígitos
        val regex = Regex("^[0-9]{8}$")
        return regex.matches(dni)
    }
    
    private fun mostrarErrorYReiniciar(titulo: String, mensaje: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Reintentar") { _, _ ->
                // Reanudar el scanner para intentar de nuevo
                barcodeView.resume()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun verificarEmpleadoActivo(personal: Personal): Boolean {
        // Verificar si el empleado tiene horarios configurados para hoy
        val diaSemana = java.text.SimpleDateFormat("EEEE", java.util.Locale("es", "ES"))
            .format(java.util.Date()).lowercase()
        
        val horarioDia = personal.getHorarioDia(diaSemana)
        
        return if (personal.tipoHorario == "FIJO") {
            // Para horario fijo, siempre está activo de lunes a viernes
            val calendar = java.util.Calendar.getInstance()
            val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            dayOfWeek in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY
        } else {
            // Para horario variable, verificar si el día está activo
            horarioDia.activo && horarioDia.entrada.isNotEmpty() && horarioDia.salida.isNotEmpty()
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (::barcodeView.isInitialized) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeView.isInitialized) {
            barcodeView.pause()
        }
    }
    
    private fun mostrarMensajeHorarioPartido(personal: Personal, registro: RegistroAsistenciaExtendido) {
        val horarioPartidoManager = HorarioPartidoManager(this)
        val estadisticasCompensacion = horarioPartidoManager.getEstadisticasCompensacion(registro.dni)
        
        // Determinar emoji y descripción según el tipo de registro
        val (emoji, descripcion) = when (registro.tipo) {
            "ENTRADA_TURNO1" -> "🌅" to "Entrada Turno Mañana"
            "SALIDA_TURNO1" -> "🍽️" to "Salida a Descanso"
            "ENTRADA_TURNO2" -> "🌆" to "Entrada Turno Tarde"
            "SALIDA_TURNO2" -> "🏠" to "Salida Final"
            else -> "📥" to "Registro"
        }
        
        // Información sobre retraso
        val infoRetraso = if (registro.llegadaTarde) {
            "\n⚠️ TARDANZA: ${registro.minutosRetraso} minutos de retraso"
        } else if (registro.tipo.contains("ENTRADA")) {
            val configuracionManager = ConfiguracionManager(this)
            val tolerancia = configuracionManager.toleranciaMinutos
            if (tolerancia > 0) {
                "\n✅ PUNTUAL (dentro de tolerancia de $tolerancia min)"
            } else {
                "\n✅ PUNTUAL"
            }
        } else {
            ""
        }
        
        // Información sobre compensación
        val infoCompensacion = if (registro.minutosCompensacion > 0) {
            "\n⏰ COMPENSACIÓN: Debe trabajar ${registro.minutosCompensacion} minutos extra"
        } else {
            ""
        }
        
        // Información sobre llegada temprana (antes de las 15:00 para turno tarde)
        val infoLlegadaTemprana = if (registro.tipo == "ENTRADA_TURNO2") {
            val horaActual = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .parse(registro.hora.substring(0, 5))
            val horaEsperada = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .parse(registro.horaEsperada)
            
            if (horaActual != null && horaEsperada != null && horaActual.before(horaEsperada)) {
                val minutosAntes = ((horaEsperada.time - horaActual.time) / (1000 * 60)).toInt()
                "\n📝 NOTA: Llegó $minutosAntes minutos antes. El tiempo antes de las ${registro.horaEsperada} no cuenta como trabajo."
            } else {
                ""
            }
        } else {
            ""
        }
        
        val mensaje = "$emoji $descripcion registrada\n" +
                "👤 ${personal.nombre}\n" +
                "🆔 DNI: ${registro.dni}\n" +
                "📅 ${registro.diaSemana}, ${registro.fecha}\n" +
                "🕐 ${registro.hora}" +
                (if (registro.horaEsperada.isNotEmpty()) " (Esperado: ${registro.horaEsperada})" else "") +
                infoRetraso +
                infoCompensacion +
                infoLlegadaTemprana +
                "\n\n📊 Estado del día: $estadisticasCompensacion"
        
        // Mostrar mensaje principal
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        
        // Verificar alertas de tardanzas
        val tardanzasManager = TardanzasManager(this)
        val mensajeAlerta = tardanzasManager.getMensajeAlerta(registro.dni, personal.nombre)
        
        mensajeAlerta?.let { alerta ->
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Alerta de Tardanzas")
                .setMessage(alerta)
                .setPositiveButton("Entendido", null)
                .show()
        }
        
        // Mostrar información adicional para horarios partidos
        if (registro.tipo == "SALIDA_TURNO2" && registro.minutosCompensacion > 0) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⏰ Compensación de Tiempo")
                .setMessage(
                    "El empleado ${personal.nombre} tenía ${registro.minutosCompensacion} minutos de retraso acumulados hoy.\n\n" +
                    "Según la configuración, estos minutos deben ser compensados trabajando hasta las " +
                    "${calcularHoraCompensacion(registro.horaEsperada, registro.minutosCompensacion)} " +
                    "en lugar de las ${registro.horaEsperada}.\n\n" +
                    "Estado actual: $estadisticasCompensacion"
                )
                .setPositiveButton("Entendido", null)
                .show()
        }
    }
    
    private fun calcularHoraCompensacion(horaSalida: String, minutosExtra: Int): String {
        return try {
            val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val hora = formato.parse(horaSalida)
            if (hora != null) {
                val calendar = java.util.Calendar.getInstance()
                calendar.time = hora
                calendar.add(java.util.Calendar.MINUTE, minutosExtra)
                formato.format(calendar.time)
            } else {
                horaSalida
            }
        } catch (e: Exception) {
            horaSalida
        }
    }}
