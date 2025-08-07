package com.asistencia.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class ReportesActivity : AppCompatActivity() {
    
    private lateinit var reportesAdapter: ReportesAdapter
    private lateinit var asistenciaManager: AsistenciaManager
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)
        
        asistenciaManager = AsistenciaManager(this)
        sharedPreferences = getSharedPreferences("AsistenciaApp", Context.MODE_PRIVATE)
        
        setupViews()
        setupRecyclerView()
        loadReportesData()
    }
    
    private fun setupViews() {
        findViewById<Button>(R.id.btnExportarCSV).setOnClickListener {
            exportarDatosCSV()
        }
        
        findViewById<Button>(R.id.btnLimpiarDatos).setOnClickListener {
            limpiarDatos()
        }
        
        findViewById<Button>(R.id.btnEstadisticasTardanzas).setOnClickListener {
            mostrarEstadisticasTardanzas()
        }
    }
    
    private fun setupRecyclerView() {
        reportesAdapter = ReportesAdapter()
        
        findViewById<RecyclerView>(R.id.recyclerViewReportes).apply {
            layoutManager = LinearLayoutManager(this@ReportesActivity)
            adapter = reportesAdapter
        }
    }
    
    private fun loadReportesData() {
        val registros = asistenciaManager.getRegistrosAsistencia()
        
        // Obtener lista de personal para mostrar nombres
        val personalJson = sharedPreferences.getString("personal_list", "[]")
        val type = object : TypeToken<List<Personal>>() {}.type
        val personalList: List<Personal> = gson.fromJson(personalJson, type) ?: emptyList()
        
        // Convertir registros a ReporteItem
        val reportes = registros.map { registro ->
            val personal = personalList.find { it.dni == registro.dni }
            ReporteItem(
                nombre = personal?.nombre ?: "Desconocido",
                dni = registro.dni,
                fecha = "${registro.diaSemana}, ${registro.fecha}",
                hora = registro.hora,
                tipo = registro.tipo,
                llegadaTarde = registro.llegadaTarde,
                observaciones = if (registro.llegadaTarde) "Llegada tardía" else ""
            )
        }.sortedByDescending { "${it.fecha} ${it.hora}" }
        
        reportesAdapter.submitList(reportes)
        
        // Mostrar mensaje si no hay datos
        if (reportes.isEmpty()) {
            Toast.makeText(this, "No hay registros de asistencia para mostrar", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun exportarDatosCSV() {
        val registros = asistenciaManager.getRegistrosAsistencia()
        
        if (registros.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Mostrar opciones de exportación
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📊 Opciones de Exportación")
            .setMessage("Seleccione el formato de exportación:")
            .setPositiveButton("📋 Hoja General") { _, _ ->
                exportarHojaGeneral(registros)
            }
            .setNeutralButton("👥 Hojas por Empleado") { _, _ ->
                exportarHojasPorEmpleado(registros)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun exportarHojaGeneral(registros: List<RegistroAsistencia>) {
        try {
            // Obtener lista de personal
            val personalJson = sharedPreferences.getString("personal_list", "[]")
            val type = object : TypeToken<List<Personal>>() {}.type
            val personalList: List<Personal> = gson.fromJson(personalJson, type) ?: emptyList()
            
            // Crear contenido CSV mejorado
            val csvContent = StringBuilder()
            
            // Encabezados mejorados con información de horarios
            csvContent.append("DNI,Nombre,Fecha,Día,Hora,Tipo,Estado,")
            csvContent.append("Horario_Entrada,Horario_Salida,Tiene_Refrigerio,")
            csvContent.append("Inicio_Refrigerio,Fin_Refrigerio,Horas_Trabajadas_Teoricas,Observaciones\n")
            
            // Agregar datos con información completa
            registros.forEach { registro ->
                val personal = personalList.find { it.dni == registro.dni }
                val nombre = personal?.nombre ?: "Desconocido"
                val estado = if (registro.llegadaTarde) "TARDE" else "PUNTUAL"
                val observaciones = if (registro.llegadaTarde) "Llegada tardía" else ""
                
                // Información de horarios
                val horarioEntrada = personal?.horaEntrada ?: ""
                val horarioSalida = personal?.horaSalida ?: ""
                val tieneRefrigerio = if (personal?.tieneRefrigerio == true) "SÍ" else "NO"
                val inicioRefrigerio = if (personal?.tieneRefrigerio == true) personal.inicioRefrigerio else ""
                val finRefrigerio = if (personal?.tieneRefrigerio == true) personal.finRefrigerio else ""
                
                // Calcular horas trabajadas teóricas
                val horasTrabajadasTeorica = personal?.let {
                    val (horas, minutos) = it.calcularHorasTrabajadasConRefrigerio()
                    "${horas}h ${minutos}m"
                } ?: ""
                
                csvContent.append("${registro.dni},")
                csvContent.append("\"$nombre\",")
                csvContent.append("${registro.fecha},")
                csvContent.append("\"${registro.diaSemana}\",")
                csvContent.append("${registro.hora},")
                csvContent.append("${registro.tipo},")
                csvContent.append("$estado,")
                csvContent.append("$horarioEntrada,")
                csvContent.append("$horarioSalida,")
                csvContent.append("$tieneRefrigerio,")
                csvContent.append("$inicioRefrigerio,")
                csvContent.append("$finRefrigerio,")
                csvContent.append("\"$horasTrabajadasTeorica\",")
                csvContent.append("\"$observaciones\"\n")
            }
            
            // Guardar archivo
            val fileName = "asistencias_general_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            guardarArchivoCSV(fileName, csvContent.toString(), registros.size)
                
        } catch (e: Exception) {
            mostrarErrorExportacion(e)
        }
    }
    
    private fun exportarHojasPorEmpleado(registros: List<RegistroAsistencia>) {
        try {
            // Obtener lista de personal
            val personalJson = sharedPreferences.getString("personal_list", "[]")
            val type = object : TypeToken<List<Personal>>() {}.type
            val personalList: List<Personal> = gson.fromJson(personalJson, type) ?: emptyList()
            
            // Agrupar registros por empleado
            val registrosPorEmpleado = registros.groupBy { it.dni }
            
            if (registrosPorEmpleado.isEmpty()) {
                Toast.makeText(this, "No hay registros para exportar", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Crear contenido CSV con múltiples hojas (separadas por líneas vacías)
            val csvContent = StringBuilder()
            var totalRegistrosExportados = 0
            
            registrosPorEmpleado.forEach { (dni, registrosEmpleado) ->
                val personal = personalList.find { it.dni == dni }
                val nombreEmpleado = personal?.nombre ?: "Empleado Desconocido"
                
                // Separador de hoja (para Excel/Sheets que soporten múltiples hojas en CSV)
                csvContent.append("\n")
                csvContent.append("=== HOJA: $nombreEmpleado (DNI: $dni) ===\n")
                csvContent.append("\n")
                
                // Información del empleado
                csvContent.append("INFORMACIÓN DEL EMPLEADO\n")
                csvContent.append("Nombre,\"$nombreEmpleado\"\n")
                csvContent.append("DNI,$dni\n")
                csvContent.append("Horario_Entrada,${personal?.horaEntrada ?: "No definido"}\n")
                csvContent.append("Horario_Salida,${personal?.horaSalida ?: "No definido"}\n")
                csvContent.append("Tiene_Refrigerio,${if (personal?.tieneRefrigerio == true) "SÍ" else "NO"}\n")
                
                if (personal?.tieneRefrigerio == true) {
                    csvContent.append("Inicio_Refrigerio,${personal.inicioRefrigerio}\n")
                    csvContent.append("Fin_Refrigerio,${personal.finRefrigerio}\n")
                    csvContent.append("Duración_Refrigerio,${personal.minutosRefrigerio} minutos\n")
                }
                
                val (horas, minutos) = personal?.calcularHorasTrabajadasConRefrigerio() ?: Pair(0, 0)
                csvContent.append("Horas_Trabajadas_Diarias,\"${horas}h ${minutos}m\"\n")
                csvContent.append("\n")
                
                // Encabezados de registros
                csvContent.append("REGISTROS DE ASISTENCIA\n")
                csvContent.append("Fecha,Día,Hora,Tipo,Estado,Minutos_Tarde,Observaciones\n")
                
                // Registros del empleado ordenados por fecha
                registrosEmpleado.sortedBy { "${it.fecha} ${it.hora}" }.forEach { registro ->
                    val estado = if (registro.llegadaTarde) "TARDE" else "PUNTUAL"
                    val minutosLlegadaTarde = if (registro.llegadaTarde) {
                        calcularMinutosTarde(personal, registro)
                    } else 0
                    
                    val observaciones = when {
                        registro.llegadaTarde -> "Llegada tardía ($minutosLlegadaTarde min)"
                        else -> "Registro normal"
                    }
                    
                    csvContent.append("${registro.fecha},")
                    csvContent.append("\"${registro.diaSemana}\",")
                    csvContent.append("${registro.hora},")
                    csvContent.append("${registro.tipo},")
                    csvContent.append("$estado,")
                    csvContent.append("$minutosLlegadaTarde,")
                    csvContent.append("\"$observaciones\"\n")
                }
                
                // Estadísticas del empleado
                csvContent.append("\n")
                csvContent.append("ESTADÍSTICAS DEL EMPLEADO\n")
                val totalRegistros = registrosEmpleado.size
                val registrosTarde = registrosEmpleado.count { it.llegadaTarde }
                val porcentajePuntualidad = if (totalRegistros > 0) {
                    ((totalRegistros - registrosTarde) * 100.0 / totalRegistros)
                } else 0.0
                
                csvContent.append("Total_Registros,$totalRegistros\n")
                csvContent.append("Registros_Tarde,$registrosTarde\n")
                csvContent.append("Registros_Puntuales,${totalRegistros - registrosTarde}\n")
                csvContent.append("Porcentaje_Puntualidad,\"${String.format("%.1f", porcentajePuntualidad)}%\"\n")
                
                // Separador entre empleados
                csvContent.append("\n")
                csvContent.append("=" .repeat(50))
                csvContent.append("\n")
                
                totalRegistrosExportados += registrosEmpleado.size
            }
            
            // Resumen general al final
            csvContent.append("\n")
            csvContent.append("=== RESUMEN GENERAL ===\n")
            csvContent.append("Total_Empleados,${registrosPorEmpleado.size}\n")
            csvContent.append("Total_Registros,$totalRegistrosExportados\n")
            csvContent.append("Fecha_Exportación,\"${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\"\n")
            
            // Guardar archivo
            val fileName = "asistencias_por_empleado_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            guardarArchivoCSV(fileName, csvContent.toString(), totalRegistrosExportados)
                
        } catch (e: Exception) {
            mostrarErrorExportacion(e)
        }
    }
    
    private fun calcularMinutosTarde(personal: Personal?, registro: RegistroAsistencia): Int {
        if (personal == null || !registro.llegadaTarde) return 0
        
        return try {
            val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
            val horaLlegada = formato.parse(registro.hora.substring(0, 5))
            val horaEntrada = formato.parse(personal.horaEntrada)
            
            if (horaLlegada != null && horaEntrada != null) {
                val diferencia = horaLlegada.time - horaEntrada.time
                (diferencia / (1000 * 60)).toInt()
            } else 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun guardarArchivoCSV(fileName: String, content: String, recordCount: Int) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10+ - usar MediaStore
            val resolver = contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { fileUri ->
                resolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                
                // Mostrar mensaje de éxito mejorado
                showExportSuccessDialog(fileName, recordCount, fileUri)
            }
        } else {
            // Android 9 y anteriores
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            
            file.writeText(content)
            
            // Mostrar mensaje de éxito
            showExportSuccessDialog(fileName, recordCount, android.net.Uri.fromFile(file))
        }
    }
    
    private fun mostrarErrorExportacion(e: Exception) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("❌ Error de Exportación")
            .setMessage("No se pudo exportar el archivo:\n\n${e.message}\n\nVerifique que la aplicación tenga permisos de escritura.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showExportSuccessDialog(fileName: String, recordCount: Int, fileUri: android.net.Uri) {
        val tipoExportacion = if (fileName.contains("por_empleado")) {
            "📋 Hojas separadas por empleado"
        } else {
            "📊 Hoja general consolidada"
        }
        
        val mensaje = StringBuilder()
        mensaje.append("🎉 ¡Exportación completada exitosamente!\n\n")
        mensaje.append("📁 Archivo: $fileName\n")
        mensaje.append("📂 Ubicación: Carpeta Descargas\n")
        mensaje.append("📊 Registros exportados: $recordCount\n")
        mensaje.append("📋 Formato: $tipoExportacion\n\n")
        mensaje.append("✨ CARACTERÍSTICAS INCLUIDAS:\n")
        mensaje.append("• Información completa de horarios\n")
        mensaje.append("• Datos de refrigerio/almuerzo\n")
        mensaje.append("• Cálculo de horas trabajadas\n")
        mensaje.append("• Estados de puntualidad\n")
        mensaje.append("• Estadísticas por empleado\n\n")
        mensaje.append("📱 Compatible con Excel, Google Sheets y editores de texto")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("✅ Exportación Exitosa")
            .setMessage(mensaje.toString())
            .setPositiveButton("📤 Compartir") { _, _ ->
                // Compartir archivo
                try {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                        putExtra(android.content.Intent.EXTRA_TEXT, "Reporte de asistencias generado desde la app de control de asistencia")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(android.content.Intent.createChooser(shareIntent, "Compartir reporte de asistencias"))
                } catch (e: Exception) {
                    Toast.makeText(this, "No se pudo compartir el archivo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("👁️ Ver Archivo") { _, _ ->
                // Intentar abrir el archivo
                try {
                    val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, "text/csv")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    if (viewIntent.resolveActivity(packageManager) != null) {
                        startActivity(viewIntent)
                    } else {
                        Toast.makeText(this, "No hay aplicación disponible para abrir archivos CSV", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "No se pudo abrir el archivo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
    
    private fun limpiarDatos() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Limpiar Datos")
            .setMessage("¿Está seguro de eliminar todos los registros de asistencia?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                asistenciaManager.limpiarRegistros()
                loadReportesData() // Recargar la lista vacía
                Toast.makeText(this, "✅ Todos los registros han sido eliminados", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar datos cuando se regresa a la actividad
        loadReportesData()
    }
    
    private fun mostrarEstadisticasTardanzas() {
        // Obtener lista de personal
        val personalJson = sharedPreferences.getString("personal_list", "[]")
        val type = object : TypeToken<List<Personal>>() {}.type
        val personalList: List<Personal> = gson.fromJson(personalJson, type) ?: emptyList()
        
        if (personalList.isEmpty()) {
            Toast.makeText(this, "No hay personal registrado para mostrar estadísticas", Toast.LENGTH_SHORT).show()
            return
        }
        
        val tardanzasManager = TardanzasManager(this)
        val configuracionManager = ConfiguracionManager(this)
        val estadisticas = tardanzasManager.getEstadisticasTardanzas(personalList)
        val personalConProblemas = tardanzasManager.getPersonalQueExcedeLimite(personalList)
        
        val mensaje = StringBuilder()
        mensaje.append("📊 ESTADÍSTICAS DE TARDANZAS\n\n")
        
        // Configuración actual
        mensaje.append("⚙️ CONFIGURACIÓN ACTUAL:\n")
        mensaje.append("• ${configuracionManager.getDescripcionTolerancia()}\n")
        mensaje.append("• ${configuracionManager.getDescripcionLimite()}\n\n")
        
        // Personal que excede límite
        if (personalConProblemas.isNotEmpty()) {
            mensaje.append("⚠️ PERSONAL QUE EXCEDE LÍMITE:\n")
            personalConProblemas.forEach { stat ->
                mensaje.append("• ${stat.nombre}: ${stat.tardanzasEsteMes} tardanzas este mes\n")
                stat.ultimaTardanza?.let { fecha ->
                    mensaje.append("  Última tardanza: $fecha\n")
                }
            }
            mensaje.append("\n")
        }
        
        // Resumen general
        val totalTardanzasEsteMes = estadisticas.sumOf { it.tardanzasEsteMes }
        val totalTardanzasGeneral = estadisticas.sumOf { it.tardanzasTotal }
        val personalConTardanzas = estadisticas.count { it.tardanzasTotal > 0 }
        
        mensaje.append("📈 RESUMEN GENERAL:\n")
        mensaje.append("• Total tardanzas este mes: $totalTardanzasEsteMes\n")
        mensaje.append("• Total tardanzas históricas: $totalTardanzasGeneral\n")
        mensaje.append("• Personal con tardanzas: $personalConTardanzas/${personalList.size}\n\n")
        
        // Top 5 con más tardanzas
        mensaje.append("🏆 TOP 5 CON MÁS TARDANZAS ESTE MES:\n")
        estadisticas.take(5).forEachIndexed { index, stat ->
            val posicion = when (index) {
                0 -> "🥇"
                1 -> "🥈" 
                2 -> "🥉"
                else -> "${index + 1}."
            }
            val indicador = when {
                stat.excedeLimite -> "⚠️"
                stat.tardanzasEsteMes > 0 -> "📊"
                else -> "✅"
            }
            mensaje.append("$posicion $indicador ${stat.nombre}: ${stat.tardanzasEsteMes} este mes (${stat.tardanzasTotal} total)\n")
        }
        
        if (estadisticas.all { it.tardanzasTotal == 0 }) {
            mensaje.append("✅ ¡Excelente! No hay tardanzas registradas")
        }
        
        // Crear ScrollView para el diálogo
        val scrollView = android.widget.ScrollView(this)
        val textView = android.widget.TextView(this).apply {
            text = mensaje.toString()
            setPadding(50, 30, 50, 30)
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        scrollView.addView(textView)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📊 Estadísticas de Tardanzas")
            .setView(scrollView)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Configurar") { _, _ ->
                // Abrir configuración
                startActivity(android.content.Intent(this, ConfiguracionActivity::class.java))
            }
            .show()
    }}
