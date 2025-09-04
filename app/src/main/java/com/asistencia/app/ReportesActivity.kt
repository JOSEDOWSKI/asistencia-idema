package com.asistencia.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asistencia.app.utils.PinManager
import com.asistencia.app.utils.ReporteEmailSender
import com.asistencia.app.utils.EmailConfigManager
import com.asistencia.app.workers.EmailWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReportesActivity : AppCompatActivity() {
    
    private lateinit var registrosList: LinearLayout
    private val gson = Gson()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)
        
        try {
            initializeViews()
            setupClickListeners()
            loadRegistros()
            
            // Registrar actividad para el sistema de PIN
            PinManager.updateLastActivity(this)
            
        } catch (e: Exception) {
            showError("Error al inicializar reportes: ${e.message}")
        }
    }
    
    private fun initializeViews() {
        registrosList = findViewById(R.id.registrosList)
        
        // Configurar action bar
        try {
            supportActionBar?.title = "Reportes de Asistencia"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } catch (e: Exception) {
            // Ignorar si no hay action bar
        }
    }
    
    private fun setupClickListeners() {
        findViewById<Button>(R.id.btnEnviar).setOnClickListener { mostrarModalEnviar() }
        findViewById<Button>(R.id.btnLimpiarDatos).setOnClickListener { limpiarRegistros() }
        findViewById<Button>(R.id.btnEnvioAutomatico).setOnClickListener { configurarEnvioAutomatico() }
        findViewById<Button>(R.id.btnConfiguracionEmail).setOnClickListener { configurarEmail() }
    }
    
    private fun loadRegistros() {
        try {
            // Cargar registros del sistema simple (donde se guardan los nuevos)
            val sharedPreferences = getSharedPreferences("RegistrosApp", Context.MODE_PRIVATE)
            val registrosJson = sharedPreferences.getString("registros_list", "[]")
            val type = object : TypeToken<List<Map<String, String>>>() {}.type
            val registros: List<Map<String, String>> = gson.fromJson(registrosJson, type) ?: emptyList()
            
            updateRegistrosList(registros)
            
        } catch (e: Exception) {
            showError("Error al cargar registros: ${e.message}")
            updateRegistrosList(emptyList())
        }
    }
    
    private fun updateRegistrosList(registros: List<Map<String, String>>) {
        try {
            registrosList.removeAllViews()
            
            if (registros.isEmpty()) {
                val emptyText = TextView(this).apply {
                    text = "No hay registros de asistencia\n\nLos registros aparecerán aquí después de escanear empleados"
                    textSize = 14f
                    setPadding(16, 16, 16, 16)
                    setTextColor(android.graphics.Color.GRAY)
                    gravity = android.view.Gravity.CENTER
                }
                registrosList.addView(emptyText)
            } else {
                // Ordenar por timestamp (más reciente primero)
                val registrosOrdenados = registros.sortedByDescending { 
                    it["timestamp"]?.toLongOrNull() ?: 0L 
                }
                
                registrosOrdenados.forEach { registro ->
                    val registroView = createRegistroView(registro)
                    registrosList.addView(registroView)
                }
                
                // Mostrar contador
                val contador = TextView(this).apply {
                    text = "Total: ${registros.size} registros"
                    textSize = 12f
                    setPadding(0, 16, 0, 0)
                    setTextColor(android.graphics.Color.GRAY)
                    gravity = android.view.Gravity.CENTER
                }
                registrosList.addView(contador)
            }
            
        } catch (e: Exception) {
            showError("Error al mostrar registros: ${e.message}")
        }
    }
    
    private fun createRegistroView(registro: Map<String, String>): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#F8F9FA"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        }
        
        // Header con nombre y tipo de evento
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val nombre = TextView(this).apply {
            text = registro["nombre"] ?: "Empleado"
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(nombre)
        
        val tipoEvento = TextView(this).apply {
            text = registro["tipoEvento"] ?: ""
            textSize = 14f
            setTextColor(
                if (registro["tipoEvento"]?.contains("ENTRADA") == true) 
                    android.graphics.Color.parseColor("#28A745")
                else 
                    android.graphics.Color.parseColor("#DC3545")
            )
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        header.addView(tipoEvento)
        
        layout.addView(header)
        
        // DNI
        val dni = TextView(this).apply {
            text = "🆔 DNI: ${registro["dni"]}"
            textSize = 14f
            setTextColor(android.graphics.Color.GRAY)
        }
        layout.addView(dni)
        
        // Fecha, día de la semana y hora
        val timestamp = registro["timestamp"]?.toLongOrNull() ?: 0L
        val diaSemana = if (timestamp > 0) obtenerDiaSemana(Date(timestamp)) else ""
        val fechaHora = TextView(this).apply {
            text = "📅 ${registro["fecha"]} ($diaSemana) - 🕐 ${registro["hora"]}"
            textSize = 14f
            setTextColor(android.graphics.Color.GRAY)
        }
        layout.addView(fechaHora)
        
        // Estado
        val estado = TextView(this).apply {
            text = "📊 ${registro["estado"]}"
            textSize = 14f
            setTextColor(
                when {
                    registro["estado"]?.contains("PUNTUAL") == true -> android.graphics.Color.GREEN
                    registro["estado"]?.contains("RETRASO") == true -> android.graphics.Color.parseColor("#FF8C00")
                    registro["estado"]?.contains("TARDANZA") == true -> android.graphics.Color.RED
                    else -> android.graphics.Color.GRAY
                }
            )
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(estado)
        
        // Timestamp para debug
        if (timestamp != null && timestamp > 0) {
            val fechaCompleta = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
            val timestampView = TextView(this).apply {
                text = "⏰ Registrado: $fechaCompleta"
                textSize = 12f
                setTextColor(android.graphics.Color.LTGRAY)
                setPadding(0, 4, 0, 0)
            }
            layout.addView(timestampView)
        }
        
        return layout
    }
    
    private fun mostrarModalEnviar() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_enviar_reporte, null)
            
            // Configurar el diálogo
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            
            // Configurar botones del modal
            dialogView.findViewById<Button>(R.id.btnEnviarEmail).setOnClickListener {
                dialog.dismiss()
                enviarReportePorEmail()
            }
            
            dialogView.findViewById<Button>(R.id.btnExportarExcel).setOnClickListener {
                dialog.dismiss()
                exportarExcel()
            }
            
            dialogView.findViewById<Button>(R.id.btnExportarCSV).setOnClickListener {
                dialog.dismiss()
                exportarDatos()
            }
            
            dialogView.findViewById<Button>(R.id.btnEnviarPruebas).setOnClickListener {
                dialog.dismiss()
                enviarReportePruebas()
            }
            
            dialogView.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
                dialog.dismiss()
            }
            
            // Mostrar el modal
            dialog.show()
            
        } catch (e: Exception) {
            showError("Error al mostrar modal: ${e.message}")
        }
    }
    
    private fun configurarEmail() {
        try {
            val intent = Intent(this, EmailConfigActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            showError("Error al abrir configuración de email: ${e.message}")
        }
    }
    
    private fun limpiarRegistros() {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Confirmar Limpieza")
                .setMessage("¿Está seguro de eliminar TODOS los registros de asistencia?\n\nEsta acción no se puede deshacer.")
                .setPositiveButton("Eliminar") { _, _ ->
                    val sharedPreferences = getSharedPreferences("RegistrosApp", Context.MODE_PRIVATE)
                    sharedPreferences.edit().remove("registros_list").apply()
                    showMessage("🗑️ Todos los registros eliminados")
                    loadRegistros()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } catch (e: Exception) {
            showError("Error al limpiar: ${e.message}")
        }
    }
    

    
    private fun exportarDatos() {
        try {
            // Cargar registros
            val sharedPreferences = getSharedPreferences("RegistrosApp", Context.MODE_PRIVATE)
            val registrosJson = sharedPreferences.getString("registros_list", "[]")
            val type = object : TypeToken<List<Map<String, String>>>() {}.type
            val registros: List<Map<String, String>> = gson.fromJson(registrosJson, type) ?: emptyList()
            
            if (registros.isEmpty()) {
                showMessage("No hay datos para exportar")
                return
            }
            
            // Generar CSV
            val csvContent = generarCSV(registros)
            
            // Mostrar opciones de exportación
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📤 Exportar Datos")
                .setMessage("Seleccione el formato de exportación:")
                .setPositiveButton("📄 CSV") { _, _ ->
                    exportarCSV(csvContent)
                }
                .setNeutralButton("📋 Copiar") { _, _ ->
                    copiarAlPortapapeles(csvContent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
                
        } catch (e: Exception) {
            showError("Error al exportar: ${e.message}")
        }
    }
    
    private fun generarCSV(registros: List<Map<String, String>>): String {
        val csv = StringBuilder()
        
        // Encabezados mejorados con día de la semana y horarios detallados
        csv.append("Fecha,Dia_Semana,Hora_Ingreso,DNI,Nombre_Empleado,Tipo_Evento,Estado_Puntualidad,Horario_Asignado,Minutos_Diferencia,Timestamp_Sistema\n")
        
        // Cargar datos de empleados para obtener horarios
        val empleados = cargarEmpleados()
        
        // Datos ordenados por timestamp
        registros.sortedBy { it["timestamp"]?.toLongOrNull() ?: 0L }.forEach { registro ->
            val timestamp = registro["timestamp"]?.toLongOrNull() ?: 0L
            val fecha = Date(timestamp)
            val diaSemana = obtenerDiaSemana(fecha)
            val empleado = empleados.find { it.dni == registro["dni"] }
            val horarioAsignado = empleado?.let { "${it.horaEntrada} - ${it.horaSalida}" } ?: "No definido"
            val minutosDiferencia = "N/A"
            
            csv.append("\"${registro["fecha"]}\",")
            csv.append("\"$diaSemana\",")
            csv.append("\"${registro["hora"]}\",")
            csv.append("\"${registro["dni"]}\",")
            csv.append("\"${registro["nombre"]}\",")
            csv.append("\"${registro["tipoEvento"]}\",")
            csv.append("\"${registro["estado"]}\",")
            csv.append("\"$horarioAsignado\",")
            csv.append("\"$minutosDiferencia\",")
            csv.append("\"${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(fecha)}\"\n")
        }
        
        return csv.toString()
    }
    
    private fun obtenerDiaSemana(fecha: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = fecha
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Domingo"
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            else -> "Desconocido"
        }
    }
    
    private fun cargarEmpleados(): List<EmpleadoSimple> {
        return try {
            val sharedPreferences = getSharedPreferences("EmpleadosApp", Context.MODE_PRIVATE)
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val type = object : TypeToken<List<EmpleadoSimple>>() {}.type
            gson.fromJson(empleadosJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun exportarCSV(csvContent: String) {
        try {
            // Crear nombre de archivo con fecha
            val fechaActual = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val nombreArchivo = "asistencia_$fechaActual.csv"
            
            // Crear archivo temporal para compartir
            val archivo = crearArchivoTemporal(nombreArchivo, csvContent)
            
            if (archivo != null) {
                // Mostrar opciones de compartir
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("📤 Exportar Reporte de Asistencia")
                    .setMessage("¿Cómo desea compartir el reporte?\n\nArchivo: $nombreArchivo")
                    .setPositiveButton("📱 Compartir") { _, _ ->
                        compartirArchivo(archivo, nombreArchivo)
                    }
                    .setNeutralButton("📋 Copiar Texto") { _, _ ->
                        copiarAlPortapapeles(csvContent)
                    }
                    .setNegativeButton("👁️ Ver Contenido") { _, _ ->
                        mostrarContenidoCSV(csvContent, nombreArchivo)
                    }
                    .show()
            } else {
                // Fallback: mostrar contenido
                mostrarContenidoCSV(csvContent, nombreArchivo)
            }
            
        } catch (e: Exception) {
            showError("Error al generar CSV: ${e.message}")
            // Fallback: mostrar contenido
            mostrarContenidoCSV(csvContent, "asistencia_error.csv")
        }
    }
    
    private fun crearArchivoTemporal(nombreArchivo: String, contenido: String): java.io.File? {
        return try {
            // Crear archivo en el directorio de caché de la app
            val archivo = java.io.File(cacheDir, nombreArchivo)
            archivo.writeText(contenido, Charsets.UTF_8)
            archivo
        } catch (e: Exception) {
            null
        }
    }
    
    private fun compartirArchivo(archivo: java.io.File, nombreArchivo: String) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                archivo
            )
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Reporte de Asistencia - $nombreArchivo")
                putExtra(android.content.Intent.EXTRA_TEXT, 
                    "📊 Reporte de Asistencia generado por la App de Control de Asistencia\n\n" +
                    "Archivo: $nombreArchivo\n" +
                    "Fecha de generación: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\n\n" +
                    "Este archivo contiene los registros detallados de asistencia con:\n" +
                    "• Fecha y día de la semana\n" +
                    "• Horarios de ingreso\n" +
                    "• Estado de puntualidad\n" +
                    "• Diferencias en minutos\n\n" +
                    "#AsistenciaLaboral #ControlHorario #ReporteCSV"
                )
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = android.content.Intent.createChooser(intent, "📤 Compartir Reporte de Asistencia")
            startActivity(chooser)
            
            showMessage("📱 Compartiendo reporte...")
            
        } catch (e: Exception) {
            showError("Error al compartir archivo: ${e.message}")
            // Fallback: copiar al portapapeles
            copiarAlPortapapeles(archivo.readText())
        }
    }
    
    private fun mostrarContenidoCSV(csvContent: String, nombreArchivo: String) {
        try {
            val scrollView = ScrollView(this)
            val textView = TextView(this).apply {
                text = "📄 $nombreArchivo\n\n$csvContent"
                textSize = 11f
                setPadding(16, 16, 16, 16)
                setTextIsSelectable(true)
                setTypeface(android.graphics.Typeface.MONOSPACE)
            }
            scrollView.addView(textView)
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📄 Contenido del Reporte CSV")
                .setView(scrollView)
                .setPositiveButton("📋 Copiar Todo") { _, _ ->
                    copiarAlPortapapeles(csvContent)
                }
                .setNeutralButton("📱 Compartir Texto") { _, _ ->
                    compartirTexto(csvContent, nombreArchivo)
                }
                .setNegativeButton("Cerrar", null)
                .show()
                
        } catch (e: Exception) {
            showError("Error al mostrar contenido: ${e.message}")
        }
    }
    
    private fun compartirTexto(contenido: String, nombreArchivo: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Reporte de Asistencia - $nombreArchivo")
                putExtra(android.content.Intent.EXTRA_TEXT, 
                    "📊 REPORTE DE ASISTENCIA\n" +
                    "=" .repeat(30) + "\n\n" +
                    "Archivo: $nombreArchivo\n" +
                    "Generado: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\n\n" +
                    contenido + "\n\n" +
                    "📱 Generado por App de Control de Asistencia\n" +
                    "#AsistenciaLaboral #ControlHorario #ReporteCSV"
                )
            }
            
            val chooser = android.content.Intent.createChooser(intent, "📤 Compartir Reporte")
            startActivity(chooser)
            
            showMessage("📱 Compartiendo reporte como texto...")
            
        } catch (e: Exception) {
            showError("Error al compartir texto: ${e.message}")
        }
    }
    
    private fun copiarAlPortapapeles(texto: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Datos de Asistencia", texto)
            clipboard.setPrimaryClip(clip)
            showMessage("📋 Datos copiados al portapapeles")
        } catch (e: Exception) {
            showError("Error al copiar: ${e.message}")
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, "❌ $message", Toast.LENGTH_LONG).show()
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun enviarReportePorEmail() {
        // Verificar si hay configuración de email
        if (!EmailConfigManager.isConfigComplete(this)) {
            AlertDialog.Builder(this)
                .setTitle("📧 Configuración Requerida")
                .setMessage("Para enviar reportes por email, primero debe configurar el servidor SMTP y agregar destinatarios.\n\n¿Desea ir a la configuración de email?")
                .setPositiveButton("Configurar Email") { _, _ ->
                    val intent = Intent(this, EmailConfigActivity::class.java)
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }
        
        // Obtener fecha actual
        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        
        // Mostrar diálogo de confirmación
        AlertDialog.Builder(this)
            .setTitle("📧 Enviar Reporte por Email")
            .setMessage("¿Desea enviar el reporte de asistencia del $fechaActual a todos los destinatarios configurados?")
            .setPositiveButton("Enviar") { _, _ ->
                enviarReporteEmail(fechaActual)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun enviarReporteEmail(fecha: String) {
        val emailSender = ReporteEmailSender(this)
        
        // Mostrar progreso
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("📧 Enviando Reporte")
            .setMessage("Generando y enviando reporte...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        emailSender.enviarReporteDiario(fecha, object : ReporteEmailSender.EmailCallback {
            override fun onSuccess(message: String) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@ReportesActivity)
                    .setTitle("✅ Reporte Enviado")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
            
            override fun onError(error: String) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@ReportesActivity)
                    .setTitle("❌ Error al Enviar")
                    .setMessage(error)
                    .setPositiveButton("OK", null)
                    .show()
            }
        })
    }
    
    private fun configurarEnvioAutomatico() {
        // Verificar si hay configuración de email
        if (!EmailConfigManager.isConfigComplete(this)) {
            AlertDialog.Builder(this)
                .setTitle("📧 Configuración Requerida")
                .setMessage("Para configurar el envío automático, primero debe configurar el servidor SMTP y agregar destinatarios.\n\n¿Desea ir a la configuración de email?")
                .setPositiveButton("Configurar Email") { _, _ ->
                    val intent = Intent(this, EmailConfigActivity::class.java)
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }
        
        // Mostrar opciones de configuración
        val opciones = arrayOf(
            "⏰ Activar Envío Automático",
            "⏰ Desactivar Envío Automático",
            "📧 Enviar Reporte Ahora",
            "⏰ Configurar Horario",
            "📧 Ver Destinatarios"
        )
        
        AlertDialog.Builder(this)
            .setTitle("⏰ Configurar Envío Automático")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> activarEnvioAutomatico()
                    1 -> desactivarEnvioAutomatico()
                    2 -> enviarReporteAhora()
                    3 -> configurarHorarioEnvio()
                    4 -> mostrarDestinatarios()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun activarEnvioAutomatico() {
        val config = EmailConfigManager.loadEmailConfig(this)
        val nuevaConfig = config.copy(enviarAutomatico = true)
        EmailConfigManager.saveEmailConfig(this, nuevaConfig)
        
        // Programar envío automático
        EmailWorker.programarEnvioAutomatico(this)
        
        Toast.makeText(this, "✅ Envío automático activado", Toast.LENGTH_SHORT).show()
        
        AlertDialog.Builder(this)
            .setTitle("✅ Envío Automático Activado")
            .setMessage("""
                El reporte se enviará automáticamente todos los días laborables a las ${config.horaEnvio}.
                
                📧 Destinatarios: ${EmailConfigManager.getDestinatariosActivos(this).size}
                ⏰ Hora: ${config.horaEnvio}
                📅 Días: Lunes a Viernes
                
                🔄 Sistema programado y funcionando
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun desactivarEnvioAutomatico() {
        val config = EmailConfigManager.loadEmailConfig(this)
        val nuevaConfig = config.copy(enviarAutomatico = false)
        EmailConfigManager.saveEmailConfig(this, nuevaConfig)
        
        // Cancelar envío automático
        EmailWorker.cancelarEnvioAutomatico(this)
        
        Toast.makeText(this, "❌ Envío automático desactivado", Toast.LENGTH_SHORT).show()
        
        AlertDialog.Builder(this)
            .setTitle("❌ Envío Automático Desactivado")
            .setMessage("""
                El envío automático ha sido desactivado.
                
                Los reportes ya no se enviarán automáticamente.
                Puedes activarlo nuevamente cuando lo necesites.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun configurarHorarioEnvio() {
        val intent = Intent(this, EmailConfigActivity::class.java)
        startActivity(intent)
    }
    
    private fun mostrarDestinatarios() {
        val destinatarios = EmailConfigManager.getDestinatariosActivos(this)
        
        if (destinatarios.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("📧 Sin Destinatarios")
                .setMessage("No hay destinatarios configurados para el envío automático.")
                .setPositiveButton("Configurar", { _, _ ->
                    val intent = Intent(this, EmailConfigActivity::class.java)
                    startActivity(intent)
                })
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            val listaDestinatarios = destinatarios.joinToString("\n") { "• ${it.nombre} (${it.email})" }
            
            AlertDialog.Builder(this)
                .setTitle("📧 Destinatarios Configurados")
                .setMessage("""
                    Los siguientes destinatarios recibirán los reportes automáticos:
                    
                    $listaDestinatarios
                    
                    Total: ${destinatarios.size} destinatarios
                """.trimIndent())
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
    private fun enviarReporteAhora() {
        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        
        AlertDialog.Builder(this)
            .setTitle("📧 Enviar Reporte Ahora")
            .setMessage("¿Desea enviar el reporte de asistencia del $fechaActual inmediatamente?")
            .setPositiveButton("Enviar") { _, _ ->
                enviarReporteEmail(fechaActual)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun enviarReportePruebas() {
        // Verificar si hay configuración de email
        if (!EmailConfigManager.isConfigComplete(this)) {
            AlertDialog.Builder(this)
                .setTitle("📧 Configuración Requerida")
                .setMessage("Para enviar reportes por email, primero debe configurar el servidor SMTP y agregar destinatarios.\n\n¿Desea ir a la configuración de email?")
                .setPositiveButton("Configurar Email") { _, _ ->
                    val intent = Intent(this, EmailConfigActivity::class.java)
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }
        
        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        
        AlertDialog.Builder(this)
            .setTitle("🧪 Enviar Reporte de Pruebas")
            .setMessage("""
                ¿Desea enviar un reporte de pruebas inmediatamente?
                
                📅 Fecha: $fechaActual
                📧 Destinatarios: ${EmailConfigManager.getDestinatariosActivos(this).size}
                
                Este reporte incluirá todos los registros de asistencia disponibles para pruebas.
            """.trimIndent())
            .setPositiveButton("🧪 Enviar Prueba") { _, _ ->
                enviarReporteEmailPruebas(fechaActual)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun enviarReporteEmailPruebas(fecha: String) {
        val emailSender = ReporteEmailSender(this)
        
        // Mostrar progreso
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("🧪 Enviando Reporte de Pruebas")
            .setMessage("Generando y enviando reporte de pruebas...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        emailSender.enviarReporteDiario(fecha, object : ReporteEmailSender.EmailCallback {
            override fun onSuccess(message: String) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@ReportesActivity)
                    .setTitle("✅ Reporte de Pruebas Enviado")
                    .setMessage("""
                        🎉 ¡Reporte de pruebas enviado exitosamente!
                        
                        $message
                        
                        📧 Verifica tu bandeja de entrada para confirmar la recepción.
                        🧪 Este reporte incluye todos los datos de asistencia disponibles.
                    """.trimIndent())
                    .setPositiveButton("OK", null)
                    .show()
            }
            
            override fun onError(error: String) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@ReportesActivity)
                    .setTitle("❌ Error al Enviar Pruebas")
                    .setMessage("""
                        Error al enviar el reporte de pruebas:
                        
                        ❌ $error
                        
                        🔧 Verifica:
                        • Configuración SMTP correcta
                        • Conexión a internet
                        • Destinatarios válidos
                        • Credenciales de email
                    """.trimIndent())
                    .setPositiveButton("OK", null)
                    .show()
            }
        })
    }
    
    private fun exportarExcel() {
        try {
            // Cargar registros
            val sharedPreferences = getSharedPreferences("RegistrosApp", Context.MODE_PRIVATE)
            val registrosJson = sharedPreferences.getString("registros_list", "[]")
            val registros = gson.fromJson<List<Map<String, String>>>(registrosJson, object : TypeToken<List<Map<String, String>>>() {}.type) ?: emptyList()
            
            if (registros.isEmpty()) {
                Toast.makeText(this, "❌ No hay datos para exportar", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Generar contenido Excel (formato CSV mejorado)
            val fechaActual = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val nombreArchivo = "reporte_asistencia_$fechaActual.csv"
            
            val csvContent = StringBuilder()
            csvContent.append("Empleado,DNI,Evento,Fecha,Hora,Estado,Ubicación,Latitud,Longitud\n")
            
            registros.forEach { registro ->
                val nombre = registro["nombre"] ?: ""
                val dni = registro["dni"] ?: ""
                val evento = registro["tipoEvento"] ?: ""
                val fecha = registro["fecha"] ?: ""
                val hora = registro["hora"] ?: ""
                val estado = registro["estado"] ?: ""
                val ubicacion = registro["ubicacion"] ?: ""
                val latitud = registro["latitud"] ?: ""
                val longitud = registro["longitud"] ?: ""
                
                csvContent.append("$nombre,$dni,$evento,$fecha,$hora,$estado,$ubicacion,$latitud,$longitud\n")
            }
            
            // Guardar archivo
            val archivo = File(getExternalFilesDir(null), nombreArchivo)
            archivo.writeText(csvContent.toString())
            
            // Compartir archivo
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                archivo
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "📊 Reporte de Asistencia - $fechaActual")
                putExtra(Intent.EXTRA_TEXT, 
                    "📊 REPORTE DE ASISTENCIA EN FORMATO EXCEL\n" +
                    "=" .repeat(40) + "\n\n" +
                    "Archivo: $nombreArchivo\n" +
                    "Registros: ${registros.size}\n" +
                    "Generado: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\n\n" +
                    "📱 Generado por App de Control de Asistencia\n" +
                    "#AsistenciaLaboral #ControlHorario #ReporteExcel"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "📊 Compartir Reporte Excel")
            startActivity(chooser)
            
            showMessage("📊 Exportando reporte en formato Excel...")
            
        } catch (e: Exception) {
            showError("Error al exportar Excel: ${e.message}")
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onBackPressed() {
        finish()
    }
}