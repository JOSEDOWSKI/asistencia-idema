package com.asistencia.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class ReportesActivity : AppCompatActivity() {
    
    private lateinit var registrosList: LinearLayout
    private lateinit var estadisticasContent: LinearLayout
    private val gson = Gson()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)
        
        try {
            initializeViews()
            setupClickListeners()
            loadRegistros()
        } catch (e: Exception) {
            showError("Error al inicializar reportes: ${e.message}")
        }
    }
    
    private fun initializeViews() {
        registrosList = findViewById(R.id.registrosList)
        estadisticasContent = findViewById(R.id.estadisticasContent)
        
        // Configurar action bar
        try {
            supportActionBar?.title = "Reportes de Asistencia"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } catch (e: Exception) {
            // Ignorar si no hay action bar
        }
    }
    
    private fun setupClickListeners() {
        findViewById<Button>(R.id.btnExportarCSV).setOnClickListener { exportarDatos() }
        findViewById<Button>(R.id.btnLimpiarDatos).setOnClickListener { limpiarRegistros() }
        findViewById<Button>(R.id.btnEstadisticasTardanzas).setOnClickListener { mostrarEstadisticasDetalladas() }
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
            
            // Actualizar estadísticas
            updateEstadisticas(registros)
            
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
    
    private fun updateEstadisticas(registros: List<Map<String, String>>) {
        try {
            estadisticasContent.removeAllViews()
            
            if (registros.isEmpty()) {
                val noDataText = TextView(this).apply {
                    text = "No hay datos para mostrar estadísticas"
                    textSize = 14f
                    setTextColor(android.graphics.Color.GRAY)
                }
                estadisticasContent?.addView(noDataText)
                return
            }
            
            // Agrupar registros por empleado y fecha
            val registrosPorEmpleado = registros.groupBy { "${it["dni"]}_${it["fecha"]}" }
            
            var totalEmpleados = 0
            var totalHorasTrabajadas = 0
            var totalMinutosTrabajados = 0
            var empleadosPuntuales = 0
            var empleadosConTardanza = 0
            
            val detalleEmpleados = mutableListOf<String>()
            
            registrosPorEmpleado.forEach { (key, registrosEmpleado) ->
                val dni = registrosEmpleado.first()["dni"] ?: ""
                val nombre = registrosEmpleado.first()["nombre"] ?: ""
                val fecha = registrosEmpleado.first()["fecha"] ?: ""
                
                totalEmpleados++
                
                // Calcular horas trabajadas para este empleado
                val horasTrabajadas = calcularHorasTrabajadasEmpleado(registrosEmpleado, dni)
                totalHorasTrabajadas += horasTrabajadas.first
                totalMinutosTrabajados += horasTrabajadas.second
                
                // Verificar puntualidad
                val tieneTardanza = registrosEmpleado.any { 
                    it["estado"]?.contains("TARDANZA") == true || 
                    it["estado"]?.contains("RETRASO") == true 
                }
                
                if (tieneTardanza) {
                    empleadosConTardanza++
                } else {
                    empleadosPuntuales++
                }
                
                // Agregar detalle del empleado
                val horasTexto = if (horasTrabajadas.first > 0 || horasTrabajadas.second > 0) {
                    "${horasTrabajadas.first}h ${horasTrabajadas.second}m"
                } else {
                    "Sin jornada completa"
                }
                
                val estadoTexto = if (tieneTardanza) "⚠️ Con tardanza" else "✅ Puntual"
                detalleEmpleados.add("• $nombre: $horasTexto - $estadoTexto")
            }
            
            // Convertir minutos extra a horas
            val horasExtra = totalMinutosTrabajados / 60
            totalHorasTrabajadas += horasExtra
            totalMinutosTrabajados %= 60
            
            // Mostrar estadísticas generales
            val statsGenerales = TextView(this).apply {
                text = buildString {
                    append("👥 Empleados registrados: $totalEmpleados\n")
                    append("⏰ Total horas trabajadas: ${totalHorasTrabajadas}h ${totalMinutosTrabajados}m\n")
                    append("✅ Empleados puntuales: $empleadosPuntuales\n")
                    append("⚠️ Empleados con tardanza: $empleadosConTardanza\n")
                    append("📊 Promedio por empleado: ${if (totalEmpleados > 0) "${(totalHorasTrabajadas * 60 + totalMinutosTrabajados) / totalEmpleados / 60}h ${((totalHorasTrabajadas * 60 + totalMinutosTrabajados) / totalEmpleados) % 60}m" else "0h 0m"}")
                }
                textSize = 14f
                setTextColor(android.graphics.Color.BLACK)
                setPadding(0, 0, 0, 16)
            }
            estadisticasContent?.addView(statsGenerales)
            
            // Mostrar detalle por empleado si hay pocos empleados
            if (totalEmpleados <= 5 && detalleEmpleados.isNotEmpty()) {
                val detalleTitle = TextView(this).apply {
                    text = "📋 Detalle por empleado:"
                    textSize = 14f
                    setTextColor(android.graphics.Color.BLACK)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 8, 0, 4)
                }
                estadisticasContent?.addView(detalleTitle)
                
                val detalleText = TextView(this).apply {
                    text = detalleEmpleados.joinToString("\n")
                    textSize = 12f
                    setTextColor(android.graphics.Color.GRAY)
                }
                estadisticasContent?.addView(detalleText)
            }
            
        } catch (e: Exception) {
            showError("Error al calcular estadísticas: ${e.message}")
        }
    }
    
    private fun calcularHorasTrabajadasEmpleado(registrosEmpleado: List<Map<String, String>>, dni: String): Pair<Int, Int> {
        try {
            // Buscar entrada y salida del empleado
            val entrada = registrosEmpleado.find { it["tipoEvento"]?.contains("ENTRADA") == true }
            val salida = registrosEmpleado.find { it["tipoEvento"]?.contains("SALIDA") == true }
            
            if (entrada == null || salida == null) {
                return Pair(0, 0) // No hay jornada completa
            }
            
            val horaEntrada = entrada["hora"] ?: return Pair(0, 0)
            val horaSalida = salida["hora"] ?: return Pair(0, 0)
            
            // Calcular diferencia en minutos
            val minutosTotal = calcularDiferenciaMinutos(horaEntrada, horaSalida)
            
            // Descontar tiempo de refrigerio (asumimos 1 hora estándar si no hay registros específicos)
            val minutosRefrigerio = obtenerMinutosRefrigerio(dni)
            val minutosTrabajadasNeto = maxOf(0, minutosTotal - minutosRefrigerio)
            
            val horas = minutosTrabajadasNeto / 60
            val minutos = minutosTrabajadasNeto % 60
            
            return Pair(horas, minutos)
            
        } catch (e: Exception) {
            return Pair(0, 0)
        }
    }
    
    private fun calcularDiferenciaMinutos(hora1: String, hora2: String): Int {
        return try {
            val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time1 = formato.parse(hora1)
            val time2 = formato.parse(hora2)
            
            if (time1 != null && time2 != null) {
                val diferencia = time2.time - time1.time
                (diferencia / (1000 * 60)).toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun obtenerMinutosRefrigerio(dni: String): Int {
        try {
            // Buscar el empleado en la lista para obtener su horario de refrigerio
            val sharedPreferences = getSharedPreferences("EmpleadosApp", Context.MODE_PRIVATE)
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val type = object : TypeToken<List<EmpleadoSimple>>() {}.type
            val empleados: List<EmpleadoSimple> = gson.fromJson(empleadosJson, type) ?: emptyList()
            
            val empleado = empleados.find { it.dni == dni }
            
            // Por ahora, asumimos 60 minutos de refrigerio estándar
            // En el futuro, esto se puede configurar por empleado
            return 60 // 1 hora de refrigerio estándar
            
        } catch (e: Exception) {
            return 60 // Valor por defecto
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
            val minutosDiferencia = calcularMinutosDiferencia(registro, empleado)
            
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
    
    private fun calcularMinutosDiferencia(registro: Map<String, String>, empleado: EmpleadoSimple?): String {
        return try {
            if (empleado == null) return "N/A"
            
            val horaRegistro = registro["hora"] ?: return "N/A"
            val tipoEvento = registro["tipoEvento"] ?: return "N/A"
            
            val horaEsperada = if (tipoEvento.contains("ENTRADA")) {
                empleado.horaEntrada
            } else if (tipoEvento.contains("SALIDA")) {
                empleado.horaSalida
            } else {
                return "N/A"
            }
            
            val diferencia = calcularDiferenciaMinutos(horaEsperada, horaRegistro)
            when {
                diferencia > 0 -> "+$diferencia min (tarde)"
                diferencia < 0 -> "${diferencia} min (temprano)"
                else -> "0 min (puntual)"
            }
        } catch (e: Exception) {
            "Error"
        }
    }
    
    private fun mostrarEstadisticasDetalladas() {
        try {
            val sharedPreferences = getSharedPreferences("RegistrosApp", Context.MODE_PRIVATE)
            val registrosJson = sharedPreferences.getString("registros_list", "[]")
            val type = object : TypeToken<List<Map<String, String>>>() {}.type
            val registros: List<Map<String, String>> = gson.fromJson(registrosJson, type) ?: emptyList()
            
            if (registros.isEmpty()) {
                showMessage("No hay datos para mostrar estadísticas detalladas")
                return
            }
            
            val estadisticasDetalladas = generarEstadisticasDetalladas(registros)
            
            val scrollView = ScrollView(this)
            val textView = TextView(this).apply {
                text = estadisticasDetalladas
                textSize = 12f
                setPadding(16, 16, 16, 16)
                setTextIsSelectable(true)
                setTypeface(android.graphics.Typeface.MONOSPACE)
            }
            scrollView.addView(textView)
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📈 Estadísticas Detalladas de Tardanzas")
                .setView(scrollView)
                .setPositiveButton("Copiar") { _, _ ->
                    copiarAlPortapapeles(estadisticasDetalladas)
                }
                .setNegativeButton("Cerrar", null)
                .show()
                
        } catch (e: Exception) {
            showError("Error al generar estadísticas detalladas: ${e.message}")
        }
    }
    
    private fun generarEstadisticasDetalladas(registros: List<Map<String, String>>): String {
        val stats = StringBuilder()
        val empleados = cargarEmpleados()
        
        stats.append("📊 REPORTE DETALLADO DE ASISTENCIA\n")
        stats.append("=" .repeat(50) + "\n\n")
        
        // Agrupar por empleado
        val registrosPorEmpleado = registros.groupBy { it["dni"] }
        
        registrosPorEmpleado.forEach { (dni, registrosEmpleado) ->
            val empleado = empleados.find { it.dni == dni }
            val nombre = registrosEmpleado.first()["nombre"] ?: "Empleado"
            
            stats.append("👤 $nombre (DNI: $dni)\n")
            stats.append("-".repeat(30) + "\n")
            
            if (empleado != null) {
                stats.append("⏰ Horario: ${empleado.horaEntrada} - ${empleado.horaSalida}\n")
                stats.append("📋 Estado: ${if (empleado.activo) "Activo" else "Inactivo"}\n\n")
            }
            
            // Agrupar por fecha
            val registrosPorFecha = registrosEmpleado.groupBy { it["fecha"] }
            
            registrosPorFecha.forEach { (fecha, registrosDia) ->
                val timestamp = registrosDia.first()["timestamp"]?.toLongOrNull() ?: 0L
                val diaSemana = obtenerDiaSemana(Date(timestamp))
                
                stats.append("📅 $fecha ($diaSemana)\n")
                
                registrosDia.sortedBy { it["hora"] }.forEach { registro ->
                    val hora = registro["hora"] ?: ""
                    val tipoEvento = registro["tipoEvento"] ?: ""
                    val estado = registro["estado"] ?: ""
                    val diferencia = calcularMinutosDiferencia(registro, empleado)
                    
                    val icono = when {
                        tipoEvento.contains("ENTRADA") -> "🔵"
                        tipoEvento.contains("SALIDA") -> "🔴"
                        else -> "⚪"
                    }
                    
                    stats.append("  $icono $hora - $tipoEvento ($diferencia)\n")
                    stats.append("     Estado: $estado\n")
                }
                stats.append("\n")
            }
            stats.append("\n")
        }
        
        // Resumen general
        stats.append("📈 RESUMEN GENERAL\n")
        stats.append("=" .repeat(50) + "\n")
        
        val totalRegistros = registros.size
        val totalEmpleados = registrosPorEmpleado.size
        val registrosConTardanza = registros.count { 
            it["estado"]?.contains("TARDANZA") == true || 
            it["estado"]?.contains("RETRASO") == true 
        }
        
        stats.append("📊 Total de registros: $totalRegistros\n")
        stats.append("👥 Total de empleados: $totalEmpleados\n")
        stats.append("⚠️ Registros con tardanza: $registrosConTardanza\n")
        stats.append("✅ Registros puntuales: ${totalRegistros - registrosConTardanza}\n")
        stats.append("📈 Porcentaje de puntualidad: ${if (totalRegistros > 0) "%.1f%%".format((totalRegistros - registrosConTardanza) * 100.0 / totalRegistros) else "0%"}\n")
        
        return stats.toString()
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
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onBackPressed() {
        finish()
    }
}