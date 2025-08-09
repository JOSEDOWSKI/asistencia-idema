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
    
    private lateinit var mainLayout: LinearLayout
    private lateinit var registrosList: LinearLayout
    private val gson = Gson()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            createLayout()
            loadRegistros()
        } catch (e: Exception) {
            showError("Error al inicializar reportes: ${e.message}")
        }
    }
    
    private fun createLayout() {
        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        
        // Título
        val title = TextView(this).apply {
            text = "📊 Reportes de Asistencia"
            textSize = 24f
            setPadding(0, 0, 0, 32)
            setTextColor(android.graphics.Color.BLACK)
            gravity = android.view.Gravity.CENTER
        }
        mainLayout.addView(title)
        
        // Botones de acción
        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val btnRefresh = Button(this).apply {
            text = "🔄 Actualizar"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 4, 0)
            }
            setOnClickListener { loadRegistros() }
        }
        buttonsLayout.addView(btnRefresh)
        
        val btnExportar = Button(this).apply {
            text = "📤 Exportar"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(4, 0, 4, 0)
            }
            setBackgroundColor(android.graphics.Color.parseColor("#28A745"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { exportarDatos() }
        }
        buttonsLayout.addView(btnExportar)
        
        val btnLimpiar = Button(this).apply {
            text = "🗑️ Limpiar"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(4, 0, 0, 0)
            }
            setBackgroundColor(android.graphics.Color.RED)
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { limpiarRegistros() }
        }
        buttonsLayout.addView(btnLimpiar)
        
        mainLayout.addView(buttonsLayout)
        
        // Separador
        val separator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                3
            ).apply {
                setMargins(0, 32, 0, 32)
            }
            setBackgroundColor(android.graphics.Color.GRAY)
        }
        mainLayout.addView(separator)
        
        // Sección de estadísticas
        val estadisticasLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#E8F5E8"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }
        
        val estadisticasTitle = TextView(this).apply {
            text = "📈 Estadísticas del Día"
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }
        estadisticasLayout.addView(estadisticasTitle)
        
        // Aquí se mostrarán las estadísticas calculadas
        val estadisticasContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "estadisticas_content" // Para poder encontrarlo después
        }
        estadisticasLayout.addView(estadisticasContent)
        
        mainLayout.addView(estadisticasLayout)
        
        // Título de registros
        val registrosTitle = TextView(this).apply {
            text = "📋 Registros de Asistencia:"
            textSize = 18f
            setPadding(0, 0, 0, 16)
            setTextColor(android.graphics.Color.BLACK)
        }
        mainLayout.addView(registrosTitle)
        
        // Lista de registros
        registrosList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        mainLayout.addView(registrosList)
        
        setContentView(mainLayout)
        
        // Configurar action bar
        try {
            supportActionBar?.title = "Reportes de Asistencia"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } catch (e: Exception) {
            // Ignorar si no hay action bar
        }
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
        
        // Fecha y hora
        val fechaHora = TextView(this).apply {
            text = "📅 ${registro["fecha"]} - 🕐 ${registro["hora"]}"
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
        val timestamp = registro["timestamp"]?.toLongOrNull()
        if (timestamp != null) {
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
            val estadisticasContent = mainLayout.findViewWithTag<LinearLayout>("estadisticas_content")
            estadisticasContent?.removeAllViews()
            
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
        
        // Encabezados
        csv.append("Fecha,Hora,DNI,Nombre,Tipo_Evento,Estado,Timestamp\n")
        
        // Datos
        registros.sortedBy { it["timestamp"]?.toLongOrNull() ?: 0L }.forEach { registro ->
            csv.append("${registro["fecha"]},")
            csv.append("${registro["hora"]},")
            csv.append("${registro["dni"]},")
            csv.append("\"${registro["nombre"]}\",")
            csv.append("${registro["tipoEvento"]},")
            csv.append("\"${registro["estado"]}\",")
            csv.append("${registro["timestamp"]}\n")
        }
        
        return csv.toString()
    }
    
    private fun exportarCSV(csvContent: String) {
        try {
            // Crear nombre de archivo con fecha
            val fechaActual = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val nombreArchivo = "asistencia_$fechaActual.csv"
            
            // Por simplicidad, mostrar el contenido en un diálogo
            // En una implementación completa, se guardaría en almacenamiento externo
            val scrollView = ScrollView(this)
            val textView = TextView(this).apply {
                text = "Archivo: $nombreArchivo\n\n$csvContent"
                textSize = 12f
                setPadding(16, 16, 16, 16)
                setTextIsSelectable(true)
            }
            scrollView.addView(textView)
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📄 Datos CSV Generados")
                .setView(scrollView)
                .setPositiveButton("Copiar") { _, _ ->
                    copiarAlPortapapeles(csvContent)
                }
                .setNegativeButton("Cerrar", null)
                .show()
                
            showMessage("✅ CSV generado correctamente")
            
        } catch (e: Exception) {
            showError("Error al generar CSV: ${e.message}")
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