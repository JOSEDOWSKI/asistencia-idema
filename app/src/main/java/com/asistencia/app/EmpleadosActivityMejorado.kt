package com.asistencia.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EmpleadosActivityMejorado : AppCompatActivity() {
    
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private lateinit var mainLayout: LinearLayout
    private lateinit var empleadosList: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Usar SharedPreferences como respaldo
            sharedPreferences = getSharedPreferences("EmpleadosApp", Context.MODE_PRIVATE)
            
            createLayout()
            loadEmpleados()
            
        } catch (e: Exception) {
            // Si todo falla, mostrar mensaje básico
            showBasicError(e.message ?: "Error desconocido")
        }
    }
    
    private fun showBasicError(error: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        val errorText = TextView(this).apply {
            text = "❌ Error en Gestión de Empleados:\n\n$error\n\nUsando modo básico..."
            textSize = 16f
            setTextColor(android.graphics.Color.RED)
        }
        
        val btnBasico = Button(this).apply {
            text = "Modo Básico - Agregar Empleado"
            setOnClickListener { 
                mostrarDialogoBasico()
            }
        }
        
        layout.addView(errorText)
        layout.addView(btnBasico)
        setContentView(layout)
    }
    
    private fun createLayout() {
        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        
        // Título
        val title = TextView(this).apply {
            text = "👥 Gestión de Empleados"
            textSize = 24f
            setPadding(0, 0, 0, 32)
            setTextColor(android.graphics.Color.BLACK)
            gravity = android.view.Gravity.CENTER
        }
        mainLayout.addView(title)
        
        // Botón agregar
        val btnAgregar = Button(this).apply {
            text = "➕ Agregar Empleado"
            textSize = 16f
            setPadding(20, 20, 20, 20)
            setOnClickListener { 
                mostrarDialogoAgregar()
            }
        }
        mainLayout.addView(btnAgregar)
        
        // Botón limpiar (para debug)
        val btnLimpiar = Button(this).apply {
            text = "🗑️ Limpiar Todos (Debug)"
            textSize = 14f
            setPadding(20, 20, 20, 20)
            setBackgroundColor(android.graphics.Color.RED)
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { 
                limpiarTodos()
            }
        }
        mainLayout.addView(btnLimpiar)
        
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
        
        // Título lista
        val listTitle = TextView(this).apply {
            text = "📋 Empleados:"
            textSize = 18f
            setPadding(0, 0, 0, 16)
            setTextColor(android.graphics.Color.BLACK)
        }
        mainLayout.addView(listTitle)
        
        // Lista de empleados
        empleadosList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        mainLayout.addView(empleadosList)
        
        setContentView(mainLayout)
    }
    
    private fun loadEmpleados() {
        try {
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val type = object : TypeToken<List<EmpleadoSimple>>() {}.type
            val empleados: List<EmpleadoSimple> = gson.fromJson(empleadosJson, type) ?: emptyList()
            
            updateEmpleadosList(empleados)
            
        } catch (e: Exception) {
            showMessage("Error al cargar: ${e.message}")
            updateEmpleadosList(emptyList())
        }
    }
    
    private fun updateEmpleadosList(empleados: List<EmpleadoSimple>) {
        try {
            empleadosList.removeAllViews()
            
            // Cargar también empleados flexibles
            val empleadosFlexibles = cargarEmpleadosFlexibles()
            val totalEmpleados = empleados.size + empleadosFlexibles.size
            
            if (totalEmpleados == 0) {
                val emptyText = TextView(this).apply {
                    text = "No hay empleados registrados"
                    textSize = 14f
                    setPadding(16, 16, 16, 16)
                    setTextColor(android.graphics.Color.GRAY)
                    gravity = android.view.Gravity.CENTER
                }
                empleadosList.addView(emptyText)
            } else {
                // Mostrar empleados simples
                empleados.forEach { empleado ->
                    val empleadoView = createEmpleadoView(empleado, false)
                    empleadosList.addView(empleadoView)
                }
                
                // Mostrar empleados flexibles
                empleadosFlexibles.forEach { empleadoFlexible ->
                    val empleadoView = createEmpleadoFlexibleView(empleadoFlexible)
                    empleadosList.addView(empleadoView)
                }
            }
            
            // Mostrar contador
            val contador = TextView(this).apply {
                text = "Total: $totalEmpleados empleados (${empleados.size} fijos, ${empleadosFlexibles.size} flexibles)"
                textSize = 12f
                setPadding(0, 16, 0, 0)
                setTextColor(android.graphics.Color.GRAY)
                gravity = android.view.Gravity.CENTER
            }
            empleadosList.addView(contador)
            
        } catch (e: Exception) {
            showMessage("Error al mostrar lista: ${e.message}")
        }
    }
    
    private fun cargarEmpleadosFlexibles(): List<EmpleadoFlexible> {
        return try {
            val empleadosFlexiblesJson = sharedPreferences.getString("empleados_flexibles", "[]")
            val type = object : TypeToken<List<EmpleadoFlexible>>() {}.type
            gson.fromJson(empleadosFlexiblesJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun createEmpleadoView(empleado: EmpleadoSimple, esFlexible: Boolean = false): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
            // Hacer clickeable con efecto visual
            isClickable = true
            isFocusable = true
            setOnClickListener {
                mostrarDetallesCompletoEmpleado(empleado)
            }
            // Agregar efecto de presión
            setOnTouchListener { view, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        view.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        view.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                    }
                }
                false
            }
        }
        
        // Nombre
        val nombre = TextView(this).apply {
            text = "${empleado.nombres} ${empleado.apellidos}"
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(nombre)
        
        // DNI
        val dni = TextView(this).apply {
            text = "DNI: ${empleado.dni}"
            textSize = 14f
            setTextColor(android.graphics.Color.GRAY)
        }
        layout.addView(dni)
        
        // Horario
        val horario = TextView(this).apply {
            text = "⏰ ${empleado.horaEntrada} - ${empleado.horaSalida}"
            textSize = 14f
            setTextColor(android.graphics.Color.GRAY)
        }
        layout.addView(horario)
        
        // Estado
        val estado = TextView(this).apply {
            text = if (empleado.activo) "✅ Activo" else "❌ Inactivo"
            textSize = 14f
            setTextColor(if (empleado.activo) android.graphics.Color.GREEN else android.graphics.Color.RED)
        }
        layout.addView(estado)
        
        // Botones de acción
        val botonesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 0)
        }
        
        val btnEditar = Button(this).apply {
            text = "✏️ Editar"
            textSize = 12f
            setPadding(12, 8, 12, 8)
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 4, 0)
            }
            setOnClickListener {
                editarEmpleado(empleado)
            }
        }
        botonesLayout.addView(btnEditar)
        
        val btnEliminar = Button(this).apply {
            text = "🗑️ Eliminar"
            textSize = 12f
            setPadding(12, 8, 12, 8)
            setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(4, 0, 0, 0)
            }
            setOnClickListener {
                eliminarEmpleado(empleado)
            }
        }
        botonesLayout.addView(btnEliminar)
        
        layout.addView(botonesLayout)
        
        return layout
    }
    
    private fun createEmpleadoFlexibleView(empleadoFlexible: EmpleadoFlexible): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9")) // Verde claro para diferenciarlo
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
            // Hacer clickeable con efecto visual
            isClickable = true
            isFocusable = true
            setOnClickListener {
                mostrarDetallesEmpleadoFlexible(empleadoFlexible)
            }
            // Agregar efecto de presión
            setOnTouchListener { view, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        view.setBackgroundColor(android.graphics.Color.parseColor("#C8E6C9"))
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        view.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                    }
                }
                false
            }
        }
        
        // Nombre con indicador de horario flexible
        val nombre = TextView(this).apply {
            text = "⏰ ${empleadoFlexible.nombres} ${empleadoFlexible.apellidos}"
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(nombre)
        
        // DNI
        val dni = TextView(this).apply {
            text = "DNI: ${empleadoFlexible.dni}"
            textSize = 14f
            setTextColor(android.graphics.Color.GRAY)
        }
        layout.addView(dni)
        
        // Tipo de horario
        val tipoHorario = TextView(this).apply {
            text = "📅 Horario Flexible"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(tipoHorario)
        
        // Descripción de horarios
        val descripcionHorarios = TextView(this).apply {
            text = empleadoFlexible.getDescripcionHorarios()
            textSize = 12f
            setTextColor(android.graphics.Color.GRAY)
        }
        layout.addView(descripcionHorarios)
        
        // Estado activo/inactivo
        val estado = TextView(this).apply {
            text = if (empleadoFlexible.activo) "✅ Activo" else "❌ Inactivo"
            textSize = 14f
            setTextColor(if (empleadoFlexible.activo) android.graphics.Color.parseColor("#2E7D32") else android.graphics.Color.RED)
        }
        layout.addView(estado)
        
        // Botones de acción para empleado flexible
        val botonesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 0)
        }
        
        val btnDetalles = Button(this).apply {
            text = "📋 Detalles"
            textSize = 12f
            setPadding(12, 8, 12, 8)
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 2, 0)
            }
            setOnClickListener {
                mostrarDetallesEmpleadoFlexible(empleadoFlexible)
            }
        }
        botonesLayout.addView(btnDetalles)
        
        val btnEditar = Button(this).apply {
            text = "✏️ Editar"
            textSize = 12f
            setPadding(12, 8, 12, 8)
            setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(2, 0, 2, 0)
            }
            setOnClickListener {
                // TODO: Implementar edición de horarios flexibles
                showMessage("🚧 Función de edición en desarrollo")
            }
        }
        botonesLayout.addView(btnEditar)
        
        val btnEliminar = Button(this).apply {
            text = "🗑️ Eliminar"
            textSize = 12f
            setPadding(12, 8, 12, 8)
            setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(2, 0, 0, 0)
            }
            setOnClickListener {
                eliminarEmpleadoFlexible(empleadoFlexible)
            }
        }
        botonesLayout.addView(btnEliminar)
        
        layout.addView(botonesLayout)
        
        return layout
    }
    
    // FUNCIONES DE GESTIÓN INDIVIDUAL DE EMPLEADOS
    
    private fun mostrarDetallesCompletoEmpleado(empleado: EmpleadoSimple) {
        try {
            // Verificar si es empleado flexible
            val empleadoFlexible = buscarEmpleadoFlexiblePorDni(empleado.dni)
            
            val mensaje = if (empleadoFlexible != null) {
                // Es empleado flexible - mostrar información completa
                buildString {
                    append("👤 EMPLEADO CON HORARIO FLEXIBLE\n\n")
                    append("📝 Nombre: ${empleadoFlexible.nombres} ${empleadoFlexible.apellidos}\n")
                    append("🆔 DNI: ${empleadoFlexible.dni}\n")
                    append("📊 Estado: ${if (empleadoFlexible.activo) "✅ Activo" else "❌ Inactivo"}\n\n")
                    
                    append("📅 HORARIOS POR DÍA:\n")
                    val diasCodigos = listOf("L", "M", "X", "J", "V", "S", "D")
                    val diasNombres = listOf("🌅 Lunes", "💼 Martes", "⚡ Miércoles", "🔥 Jueves", "🎯 Viernes", "🏖️ Sábado", "🏠 Domingo")
                    
                    diasCodigos.forEachIndexed { index, codigo ->
                        val horario = empleadoFlexible.getHorarioDia(codigo)
                        val nombreDia = diasNombres[index]
                        if (horario != null) {
                            append("$nombreDia: ${horario.first} - ${horario.second}\n")
                        } else {
                            append("$nombreDia: No trabaja\n")
                        }
                    }
                    append("\n")
                    
                    val (horasSemanales, minutosSemanales) = empleadoFlexible.calcularHorasSemanales()
                    append("⏱️ Total semanal: ${horasSemanales}h ${minutosSemanales}min\n\n")
                    
                    append("📊 Estado actual: ${empleadoFlexible.getEstadoActual()}\n")
                    
                    if (empleadoFlexible.trabajaHoy()) {
                        val horarioHoy = empleadoFlexible.getHorarioHoy()
                        if (horarioHoy != null) {
                            append("🕐 Horario hoy: ${horarioHoy.first} - ${horarioHoy.second}")
                        }
                    } else {
                        append("🏠 Hoy no trabaja")
                    }
                }
            } else {
                // Es empleado fijo - mostrar información básica
                buildString {
                    append("👤 EMPLEADO CON HORARIO FIJO\n\n")
                    append("📝 Nombre: ${empleado.nombres} ${empleado.apellidos}\n")
                    append("🆔 DNI: ${empleado.dni}\n")
                    append("📊 Estado: ${if (empleado.activo) "✅ Activo" else "❌ Inactivo"}\n\n")
                    
                    append("📅 HORARIO FIJO:\n")
                    append("🕐 Entrada: ${empleado.horaEntrada}\n")
                    append("🕕 Salida: ${empleado.horaSalida}\n\n")
                    
                    append("📋 Días laborables: Lunes a Viernes\n")
                    append("⏱️ Horas diarias: ${calcularHorasDiarias(empleado.horaEntrada, empleado.horaSalida)}\n")
                    append("⏱️ Total semanal: ${calcularHorasSemanales(empleado.horaEntrada, empleado.horaSalida)} horas\n\n")
                    
                    val horaActual = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    val estadoActual = determinarEstadoActual(horaActual, empleado.horaEntrada, empleado.horaSalida)
                    append("📊 Estado actual: $estadoActual")
                }
            }
            
            AlertDialog.Builder(this)
                .setTitle("📋 Información Completa del Empleado")
                .setMessage(mensaje)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("✏️ Editar") { _, _ ->
                    if (empleadoFlexible != null) {
                        showMessage("🚧 Edición de horarios flexibles en desarrollo")
                    } else {
                        editarEmpleado(empleado)
                    }
                }
                .setNegativeButton("🗑️ Eliminar") { _, _ ->
                    if (empleadoFlexible != null) {
                        eliminarEmpleadoFlexible(empleadoFlexible)
                    } else {
                        eliminarEmpleado(empleado)
                    }
                }
                .show()
                
        } catch (e: Exception) {
            showMessage("Error al mostrar detalles: ${e.message}")
        }
    }
    
    private fun buscarEmpleadoFlexiblePorDni(dni: String): EmpleadoFlexible? {
        return try {
            val empleadosFlexiblesJson = sharedPreferences.getString("empleados_flexibles", "[]")
            val type = object : TypeToken<List<EmpleadoFlexible>>() {}.type
            val empleadosFlexibles: List<EmpleadoFlexible> = gson.fromJson(empleadosFlexiblesJson, type) ?: emptyList()
            empleadosFlexibles.find { it.dni == dni }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calcularHorasDiarias(entrada: String, salida: String): String {
        return try {
            val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val horaEntrada = formato.parse(entrada)
            val horaSalida = formato.parse(salida)
            
            if (horaEntrada != null && horaSalida != null) {
                val diferencia = horaSalida.time - horaEntrada.time
                val horas = diferencia / (1000 * 60 * 60)
                val minutos = (diferencia % (1000 * 60 * 60)) / (1000 * 60)
                "${horas}h ${minutos}min"
            } else {
                "No calculable"
            }
        } catch (e: Exception) {
            "Error en cálculo"
        }
    }
    
    private fun calcularHorasSemanales(entrada: String, salida: String): String {
        return try {
            val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val horaEntrada = formato.parse(entrada)
            val horaSalida = formato.parse(salida)
            
            if (horaEntrada != null && horaSalida != null) {
                val diferenciaDiaria = horaSalida.time - horaEntrada.time
                val horasDiarias = diferenciaDiaria / (1000 * 60 * 60)
                val horasSemanales = horasDiarias * 5 // Lunes a Viernes
                "$horasSemanales"
            } else {
                "No calculable"
            }
        } catch (e: Exception) {
            "Error"
        }
    }
    
    private fun determinarEstadoActual(horaActual: String, entrada: String, salida: String): String {
        return try {
            val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val actual = formato.parse(horaActual)
            val horaEntrada = formato.parse(entrada)
            val horaSalida = formato.parse(salida)
            
            if (actual != null && horaEntrada != null && horaSalida != null) {
                when {
                    actual.before(horaEntrada) -> "⏰ Antes del horario de entrada"
                    actual.after(horaSalida) -> "🏠 Fuera del horario laboral"
                    else -> "✅ En horario de trabajo"
                }
            } else {
                "❓ No determinable"
            }
        } catch (e: Exception) {
            "❌ Error en cálculo"
        }
    }
    
    private fun mostrarDetallesEmpleado(empleado: EmpleadoSimple) {
        try {
            val mensaje = buildString {
                append("👤 INFORMACIÓN DEL EMPLEADO\n\n")
                append("📝 Nombre: ${empleado.nombres} ${empleado.apellidos}\n")
                append("🆔 DNI: ${empleado.dni}\n")
                append("⏰ Horario: ${empleado.horaEntrada} - ${empleado.horaSalida}\n")
                append("📊 Estado: ${if (empleado.activo) "✅ Activo" else "❌ Inactivo"}\n\n")
                
                if (empleado.horaEntrada == "FLEXIBLE") {
                    append("📅 Tipo: Horario Flexible\n")
                    append("ℹ️ Ver detalles completos en la sección de empleados flexibles")
                } else {
                    append("📋 Tipo: Horario Fijo\n")
                    append("🕐 Entrada: ${empleado.horaEntrada}\n")
                    append("🕕 Salida: ${empleado.horaSalida}")
                }
            }
            
            AlertDialog.Builder(this)
                .setTitle("📋 Detalles del Empleado")
                .setMessage(mensaje)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("✏️ Editar") { _, _ ->
                    editarEmpleado(empleado)
                }
                .setNegativeButton("🗑️ Eliminar") { _, _ ->
                    eliminarEmpleado(empleado)
                }
                .show()
                
        } catch (e: Exception) {
            showMessage("Error al mostrar detalles: ${e.message}")
        }
    }
    
    private fun mostrarDetallesEmpleadoFlexible(empleado: EmpleadoFlexible) {
        try {
            val mensaje = empleado.getInformacionDetallada()
            
            AlertDialog.Builder(this)
                .setTitle("📋 Detalles del Empleado Flexible")
                .setMessage(mensaje)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("⏰ Editar Horarios") { _, _ ->
                    // TODO: Implementar edición de horarios flexibles
                    showMessage("🚧 Función de edición en desarrollo")
                }
                .setNegativeButton("🗑️ Eliminar") { _, _ ->
                    eliminarEmpleadoFlexible(empleado)
                }
                .show()
                
        } catch (e: Exception) {
            showMessage("Error al mostrar detalles: ${e.message}")
        }
    }
    
    private fun editarEmpleado(empleado: EmpleadoSimple) {
        try {
            val dialogLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }
            
            val etNombres = EditText(this).apply {
                hint = "Nombres"
                setText(empleado.nombres)
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etNombres)
            
            val etApellidos = EditText(this).apply {
                hint = "Apellidos"
                setText(empleado.apellidos)
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etApellidos)
            
            val etEntrada = EditText(this).apply {
                hint = "Hora entrada (ej: 07:00)"
                setText(if (empleado.horaEntrada != "FLEXIBLE") empleado.horaEntrada else "08:00")
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etEntrada)
            
            val etSalida = EditText(this).apply {
                hint = "Hora salida (ej: 17:00)"
                setText(if (empleado.horaSalida != "FLEXIBLE") empleado.horaSalida else "17:00")
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etSalida)
            
            val switchActivo = Switch(this).apply {
                text = "Empleado activo"
                isChecked = empleado.activo
                textSize = 16f
                setPadding(0, 16, 0, 16)
            }
            dialogLayout.addView(switchActivo)
            
            AlertDialog.Builder(this)
                .setTitle("✏️ Editar Empleado - ${empleado.nombres}")
                .setView(dialogLayout)
                .setPositiveButton("💾 Guardar Cambios") { _, _ ->
                    actualizarEmpleado(
                        empleado.dni,
                        etNombres.text.toString().trim(),
                        etApellidos.text.toString().trim(),
                        etEntrada.text.toString().trim(),
                        etSalida.text.toString().trim(),
                        switchActivo.isChecked
                    )
                }
                .setNegativeButton("Cancelar", null)
                .show()
                
        } catch (e: Exception) {
            showMessage("❌ Error al abrir editor: ${e.message}")
        }
    }
    
    private fun actualizarEmpleado(dni: String, nombres: String, apellidos: String, entrada: String, salida: String, activo: Boolean) {
        try {
            // Validaciones básicas
            if (nombres.isEmpty() || apellidos.isEmpty()) {
                showMessage("❌ Complete nombres y apellidos")
                return
            }
            
            if (entrada.isEmpty() || salida.isEmpty()) {
                showMessage("❌ Complete horarios")
                return
            }
            
            // Cargar lista actual
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val type = object : TypeToken<MutableList<EmpleadoSimple>>() {}.type
            val empleados: MutableList<EmpleadoSimple> = gson.fromJson(empleadosJson, type) ?: mutableListOf()
            
            // Buscar y actualizar empleado
            val index = empleados.indexOfFirst { it.dni == dni }
            if (index != -1) {
                empleados[index] = EmpleadoSimple(dni, nombres, apellidos, entrada, salida, activo)
                
                // Guardar cambios
                val nuevaLista = gson.toJson(empleados)
                sharedPreferences.edit().putString("empleados_list", nuevaLista).apply()
                
                showMessage("✅ Empleado actualizado: $nombres $apellidos")
                
                // Recargar lista
                loadEmpleados()
            } else {
                showMessage("❌ No se encontró el empleado para actualizar")
            }
            
        } catch (e: Exception) {
            showMessage("❌ Error al actualizar: ${e.message}")
        }
    }
    
    private fun eliminarEmpleado(empleado: EmpleadoSimple) {
        try {
            AlertDialog.Builder(this)
                .setTitle("🗑️ Confirmar Eliminación")
                .setMessage("¿Está seguro de eliminar a:\n\n👤 ${empleado.nombres} ${empleado.apellidos}\n🆔 DNI: ${empleado.dni}\n\n⚠️ Esta acción no se puede deshacer.")
                .setPositiveButton("🗑️ Eliminar") { _, _ ->
                    confirmarEliminacionEmpleado(empleado.dni)
                }
                .setNegativeButton("Cancelar", null)
                .show()
                
        } catch (e: Exception) {
            showMessage("❌ Error al confirmar eliminación: ${e.message}")
        }
    }
    
    private fun eliminarEmpleadoFlexible(empleado: EmpleadoFlexible) {
        try {
            AlertDialog.Builder(this)
                .setTitle("🗑️ Confirmar Eliminación")
                .setMessage("¿Está seguro de eliminar a:\n\n👤 ${empleado.nombres} ${empleado.apellidos}\n🆔 DNI: ${empleado.dni}\n📅 Horario Flexible\n\n⚠️ Esta acción no se puede deshacer.")
                .setPositiveButton("🗑️ Eliminar") { _, _ ->
                    confirmarEliminacionEmpleadoFlexible(empleado.dni)
                }
                .setNegativeButton("Cancelar", null)
                .show()
                
        } catch (e: Exception) {
            showMessage("❌ Error al confirmar eliminación: ${e.message}")
        }
    }
    
    private fun confirmarEliminacionEmpleado(dni: String) {
        try {
            // Cargar lista actual
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val type = object : TypeToken<MutableList<EmpleadoSimple>>() {}.type
            val empleados: MutableList<EmpleadoSimple> = gson.fromJson(empleadosJson, type) ?: mutableListOf()
            
            // Eliminar empleado
            val empleadoEliminado = empleados.find { it.dni == dni }
            val eliminado = empleados.removeAll { it.dni == dni }
            
            if (eliminado) {
                // Guardar cambios
                val nuevaLista = gson.toJson(empleados)
                sharedPreferences.edit().putString("empleados_list", nuevaLista).apply()
                
                showMessage("✅ Empleado eliminado: ${empleadoEliminado?.nombres} ${empleadoEliminado?.apellidos}")
                
                // Recargar lista
                loadEmpleados()
            } else {
                showMessage("❌ No se encontró el empleado para eliminar")
            }
            
        } catch (e: Exception) {
            showMessage("❌ Error al eliminar: ${e.message}")
        }
    }
    
    private fun confirmarEliminacionEmpleadoFlexible(dni: String) {
        try {
            // Cargar lista de empleados flexibles
            val empleadosFlexiblesJson = sharedPreferences.getString("empleados_flexibles", "[]")
            val type = object : TypeToken<MutableList<EmpleadoFlexible>>() {}.type
            val empleadosFlexibles: MutableList<EmpleadoFlexible> = gson.fromJson(empleadosFlexiblesJson, type) ?: mutableListOf()
            
            // Eliminar empleado flexible
            val empleadoEliminado = empleadosFlexibles.find { it.dni == dni }
            val eliminado = empleadosFlexibles.removeAll { it.dni == dni }
            
            if (eliminado) {
                // Guardar cambios en empleados flexibles
                val nuevaListaFlexible = gson.toJson(empleadosFlexibles)
                sharedPreferences.edit().putString("empleados_flexibles", nuevaListaFlexible).apply()
                
                // También eliminar de empleados simples si existe
                eliminarEmpleadoSimplePorDni(dni)
                
                showMessage("✅ Empleado flexible eliminado: ${empleadoEliminado?.nombres} ${empleadoEliminado?.apellidos}")
                
                // Recargar lista
                loadEmpleados()
            } else {
                showMessage("❌ No se encontró el empleado flexible para eliminar")
            }
            
        } catch (e: Exception) {
            showMessage("❌ Error al eliminar empleado flexible: ${e.message}")
        }
    }
    
    private fun eliminarEmpleadoSimplePorDni(dni: String) {
        try {
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val type = object : TypeToken<MutableList<EmpleadoSimple>>() {}.type
            val empleados: MutableList<EmpleadoSimple> = gson.fromJson(empleadosJson, type) ?: mutableListOf()
            
            empleados.removeAll { it.dni == dni }
            
            val nuevaLista = gson.toJson(empleados)
            sharedPreferences.edit().putString("empleados_list", nuevaLista).apply()
            
        } catch (e: Exception) {
            // No es crítico si falla
        }
    }
    
    // FUNCIONES ORIGINALES MANTENIDAS
    
    private fun mostrarDialogoAgregar() {
        try {
            val dialogLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }
            
            val etDni = EditText(this).apply {
                hint = "DNI (8 dígitos)"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                textSize = 16f
            }
            dialogLayout.addView(etDni)
            
            val etNombres = EditText(this).apply {
                hint = "Nombres"
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etNombres)
            
            val etApellidos = EditText(this).apply {
                hint = "Apellidos"
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etApellidos)
            
            val etEntrada = EditText(this).apply {
                hint = "Hora entrada (ej: 07:00)"
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etEntrada)
            
            val etSalida = EditText(this).apply {
                hint = "Hora salida (ej: 13:00)"
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etSalida)
            
            AlertDialog.Builder(this)
                .setTitle("➕ Agregar Empleado")
                .setView(dialogLayout)
                .setPositiveButton("Guardar") { _, _ ->
                    guardarEmpleado(
                        etDni.text.toString().trim(),
                        etNombres.text.toString().trim(),
                        etApellidos.text.toString().trim(),
                        etEntrada.text.toString().trim(),
                        etSalida.text.toString().trim()
                    )
                }
                .setNeutralButton("⏰ Horario Flexible") { _, _ ->
                    mostrarDialogoHorarioFlexible(
                        etDni.text.toString().trim(),
                        etNombres.text.toString().trim(),
                        etApellidos.text.toString().trim()
                    )
                }
                .setNegativeButton("Cancelar", null)
                .show()
                
        } catch (e: Exception) {
            mostrarDialogoBasico()
        }
    }
    
    private fun mostrarDialogoBasico() {
        // Diálogo ultra básico sin AlertDialog
        val toast = Toast.makeText(this, "Ingrese DNI 72221744 para Jose Molina", Toast.LENGTH_LONG)
        toast.show()
        
        // Agregar Jose Molina directamente
        guardarEmpleado("72221744", "Jose", "Molina", "07:00", "13:00")
    }
    
    private fun guardarEmpleado(dni: String, nombres: String, apellidos: String, entrada: String, salida: String) {
        try {
            // Validaciones básicas
            if (dni.length != 8 || !dni.all { it.isDigit() }) {
                showMessage("❌ DNI debe tener 8 dígitos")
                return
            }
            
            if (nombres.isEmpty() || apellidos.isEmpty()) {
                showMessage("❌ Complete nombres y apellidos")
                return
            }
            
            if (entrada.isEmpty() || salida.isEmpty()) {
                showMessage("❌ Complete horarios")
                return
            }
            
            // Cargar lista actual
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val type = object : TypeToken<MutableList<EmpleadoSimple>>() {}.type
            val empleados: MutableList<EmpleadoSimple> = gson.fromJson(empleadosJson, type) ?: mutableListOf()
            
            // Verificar si ya existe
            if (empleados.any { it.dni == dni }) {
                showMessage("❌ Ya existe empleado con DNI $dni")
                return
            }
            
            // Agregar nuevo empleado
            val nuevoEmpleado = EmpleadoSimple(dni, nombres, apellidos, entrada, salida, true)
            empleados.add(nuevoEmpleado)
            
            // Guardar
            val nuevaLista = gson.toJson(empleados)
            sharedPreferences.edit().putString("empleados_list", nuevaLista).apply()
            
            showMessage("✅ Empleado agregado: $nombres $apellidos")
            
            // Recargar lista
            loadEmpleados()
            
        } catch (e: Exception) {
            showMessage("❌ Error al guardar: ${e.message}")
        }
    }
    
    private fun limpiarTodos() {
        try {
            sharedPreferences.edit().remove("empleados_list").apply()
            sharedPreferences.edit().remove("empleados_flexibles").apply()
            showMessage("🗑️ Todos los empleados eliminados")
            loadEmpleados()
        } catch (e: Exception) {
            showMessage("Error al limpiar: ${e.message}")
        }
    }
    
    private fun mostrarDialogoHorarioFlexible(dni: String, nombres: String, apellidos: String) {
        try {
            // Validaciones básicas primero
            if (dni.length != 8 || !dni.all { it.isDigit() }) {
                showMessage("❌ DNI debe tener 8 dígitos")
                return
            }
            
            if (nombres.isEmpty() || apellidos.isEmpty()) {
                showMessage("❌ Complete nombres y apellidos")
                return
            }
            
            // Verificar si ya existe
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val type = object : TypeToken<List<EmpleadoSimple>>() {}.type
            val empleados: List<EmpleadoSimple> = gson.fromJson(empleadosJson, type) ?: emptyList()
            
            if (empleados.any { it.dni == dni }) {
                showMessage("❌ Ya existe empleado con DNI $dni")
                return
            }
            
            // Crear diálogo de horario flexible
            val dialogView = layoutInflater.inflate(R.layout.dialog_horario_flexible, null)
            
            // Configurar el diálogo
            val dialog = AlertDialog.Builder(this)
                .setTitle("⏰ Horario Flexible - $nombres $apellidos")
                .setView(dialogView)
                .setPositiveButton("Guardar") { _, _ ->
                    guardarEmpleadoConHorarioFlexible(dni, nombres, apellidos, dialogView)
                }
                .setNegativeButton("Cancelar", null)
                .create()
            
            // Configurar la funcionalidad del diálogo
            configurarDialogoHorarioFlexible(dialogView)
            
            dialog.show()
            
        } catch (e: Exception) {
            showMessage("❌ Error al abrir horario flexible: ${e.message}")
        }
    }
    
    private fun configurarDialogoHorarioFlexible(dialogView: View) {
        try {
            // Configurar aplicación rápida
            val etHoraBaseEntrada = dialogView.findViewById<EditText>(R.id.et_hora_base_entrada)
            val etHoraBaseSalida = dialogView.findViewById<EditText>(R.id.et_hora_base_salida)
            val btnAplicarLV = dialogView.findViewById<Button>(R.id.btn_aplicar_lv)
            val btnAplicarLS = dialogView.findViewById<Button>(R.id.btn_aplicar_ls)
            
            // Configurar valores por defecto
            etHoraBaseEntrada.setText("08:00")
            etHoraBaseSalida.setText("17:00")
            
            // Aplicar horario L-V (Lunes a Viernes)
            btnAplicarLV.setOnClickListener {
                val entrada = etHoraBaseEntrada.text.toString()
                val salida = etHoraBaseSalida.text.toString()
                
                if (entrada.isNotEmpty() && salida.isNotEmpty()) {
                    aplicarHorarioADias(dialogView, entrada, salida, listOf("L", "M", "X", "J", "V"))
                    showMessage("✅ Horario aplicado L-V: $entrada - $salida")
                }
            }
            
            // Aplicar horario L-S (Lunes a Sábado)
            btnAplicarLS.setOnClickListener {
                val entrada = etHoraBaseEntrada.text.toString()
                val salida = etHoraBaseSalida.text.toString()
                
                if (entrada.isNotEmpty() && salida.isNotEmpty()) {
                    aplicarHorarioADias(dialogView, entrada, salida, listOf("L", "M", "X", "J", "V", "S"))
                    showMessage("✅ Horario aplicado L-S: $entrada - $salida")
                }
            }
            
            // Configurar switches de días
            configurarSwitchesDias(dialogView)
            
        } catch (e: Exception) {
            showMessage("Error al configurar diálogo: ${e.message}")
        }
    }
    
    private fun aplicarHorarioADias(dialogView: View, entrada: String, salida: String, dias: List<String>) {
        dias.forEach { dia ->
            try {
                // Usar los nuevos IDs únicos para cada día
                val (switchId, layoutId, entradaId, salidaId) = when (dia) {
                    "L" -> arrayOf(R.id.switch_lunes, R.id.layout_horarios_lunes, R.id.et_entrada_lunes, R.id.et_salida_lunes)
                    "M" -> arrayOf(R.id.switch_martes, R.id.layout_horarios_martes, R.id.et_entrada_martes, R.id.et_salida_martes)
                    "X" -> arrayOf(R.id.switch_miercoles, R.id.layout_horarios_miercoles, R.id.et_entrada_miercoles, R.id.et_salida_miercoles)
                    "J" -> arrayOf(R.id.switch_jueves, R.id.layout_horarios_jueves, R.id.et_entrada_jueves, R.id.et_salida_jueves)
                    "V" -> arrayOf(R.id.switch_viernes, R.id.layout_horarios_viernes, R.id.et_entrada_viernes, R.id.et_salida_viernes)
                    "S" -> arrayOf(R.id.switch_sabado, R.id.layout_horarios_sabado, R.id.et_entrada_sabado, R.id.et_salida_sabado)
                    "D" -> arrayOf(R.id.switch_domingo, R.id.layout_horarios_domingo, R.id.et_entrada_domingo, R.id.et_salida_domingo)
                    else -> return@forEach
                }
                
                val switchActivo = dialogView.findViewById<Switch>(switchId)
                val layoutHorarios = dialogView.findViewById<LinearLayout>(layoutId)
                val etEntrada = dialogView.findViewById<EditText>(entradaId)
                val etSalida = dialogView.findViewById<EditText>(salidaId)
                
                // Activar el día y mostrar horarios
                switchActivo?.isChecked = true
                layoutHorarios?.visibility = View.VISIBLE
                
                // Establecer horarios
                etEntrada?.setText(entrada)
                etSalida?.setText(salida)
                
            } catch (e: Exception) {
                // Continuar con el siguiente día si hay error
                showMessage("Error configurando $dia: ${e.message}")
            }
        }
    }
    
    private fun configurarSwitchesDias(dialogView: View) {
        val dias = listOf("L", "M", "X", "J", "V", "S", "D")
        
        dias.forEach { dia ->
            try {
                // Usar los nuevos IDs únicos para cada día
                val (switchId, layoutId) = when (dia) {
                    "L" -> Pair(R.id.switch_lunes, R.id.layout_horarios_lunes)
                    "M" -> Pair(R.id.switch_martes, R.id.layout_horarios_martes)
                    "X" -> Pair(R.id.switch_miercoles, R.id.layout_horarios_miercoles)
                    "J" -> Pair(R.id.switch_jueves, R.id.layout_horarios_jueves)
                    "V" -> Pair(R.id.switch_viernes, R.id.layout_horarios_viernes)
                    "S" -> Pair(R.id.switch_sabado, R.id.layout_horarios_sabado)
                    "D" -> Pair(R.id.switch_domingo, R.id.layout_horarios_domingo)
                    else -> return@forEach
                }
                
                val switchActivo = dialogView.findViewById<Switch>(switchId)
                val layoutHorarios = dialogView.findViewById<LinearLayout>(layoutId)
                
                // Configurar el switch
                switchActivo?.setOnCheckedChangeListener { _, isChecked ->
                    layoutHorarios?.visibility = if (isChecked) View.VISIBLE else View.GONE
                }
                
            } catch (e: Exception) {
                showMessage("Error configurando switch $dia: ${e.message}")
            }
        }
    }
    
    private fun guardarEmpleadoConHorarioFlexible(dni: String, nombres: String, apellidos: String, dialogView: View) {
        try {
            // Recopilar horarios de todos los días
            val horarios = mutableMapOf<String, Pair<String, String>>()
            val diasActivos = mutableListOf<String>()
            
            val dias = mapOf(
                "L" to "Lunes",
                "M" to "Martes", 
                "X" to "Miércoles",
                "J" to "Jueves",
                "V" to "Viernes",
                "S" to "Sábado",
                "D" to "Domingo"
            )
            
            dias.forEach { (codigo, nombre) ->
                try {
                    // Usar los nuevos IDs únicos para cada día
                    val (switchId, entradaId, salidaId) = when (codigo) {
                        "L" -> Triple(R.id.switch_lunes, R.id.et_entrada_lunes, R.id.et_salida_lunes)
                        "M" -> Triple(R.id.switch_martes, R.id.et_entrada_martes, R.id.et_salida_martes)
                        "X" -> Triple(R.id.switch_miercoles, R.id.et_entrada_miercoles, R.id.et_salida_miercoles)
                        "J" -> Triple(R.id.switch_jueves, R.id.et_entrada_jueves, R.id.et_salida_jueves)
                        "V" -> Triple(R.id.switch_viernes, R.id.et_entrada_viernes, R.id.et_salida_viernes)
                        "S" -> Triple(R.id.switch_sabado, R.id.et_entrada_sabado, R.id.et_salida_sabado)
                        "D" -> Triple(R.id.switch_domingo, R.id.et_entrada_domingo, R.id.et_salida_domingo)
                        else -> return@forEach
                    }
                    
                    val switchActivo = dialogView.findViewById<Switch>(switchId)
                    val etEntrada = dialogView.findViewById<EditText>(entradaId)
                    val etSalida = dialogView.findViewById<EditText>(salidaId)
                    
                    if (switchActivo?.isChecked == true) {
                        val entrada = etEntrada?.text.toString().trim() ?: ""
                        val salida = etSalida?.text.toString().trim() ?: ""
                        
                        if (entrada.isNotEmpty() && salida.isNotEmpty()) {
                            horarios[codigo] = Pair(entrada, salida)
                            diasActivos.add(codigo)
                        }
                    }
                } catch (e: Exception) {
                    showMessage("Error procesando $nombre: ${e.message}")
                }
            }
            
            if (diasActivos.isEmpty()) {
                showMessage("❌ Debe configurar al menos un día de trabajo")
                return
            }
            
            // Crear empleado con horario flexible
            val empleadoFlexible = EmpleadoFlexible(
                dni = dni,
                nombres = nombres,
                apellidos = apellidos,
                tipoHorario = "FLEXIBLE",
                horariosSemanales = horarios,
                diasActivos = diasActivos,
                activo = true
            )
            
            // Guardar en SharedPreferences
            guardarEmpleadoFlexible(empleadoFlexible)
            
            showMessage("✅ Empleado con horario flexible guardado: $nombres $apellidos")
            
            // Recargar lista
            loadEmpleados()
            
        } catch (e: Exception) {
            showMessage("❌ Error al guardar horario flexible: ${e.message}")
        }
    }
    
    private fun guardarEmpleadoFlexible(empleado: EmpleadoFlexible) {
        try {
            // Cargar empleados flexibles existentes
            val empleadosFlexiblesJson = sharedPreferences.getString("empleados_flexibles", "[]")
            val type = object : TypeToken<MutableList<EmpleadoFlexible>>() {}.type
            val empleadosFlexibles: MutableList<EmpleadoFlexible> = gson.fromJson(empleadosFlexiblesJson, type) ?: mutableListOf()
            
            // Agregar nuevo empleado
            empleadosFlexibles.add(empleado)
            
            // Guardar lista actualizada
            val nuevaLista = gson.toJson(empleadosFlexibles)
            sharedPreferences.edit().putString("empleados_flexibles", nuevaLista).apply()
            
            // También crear un empleado simple equivalente para compatibilidad
            val empleadoSimpleEquivalente = EmpleadoSimple(
                dni = empleado.dni,
                nombres = empleado.nombres,
                apellidos = empleado.apellidos,
                horaEntrada = "FLEXIBLE",
                horaSalida = "FLEXIBLE",
                activo = empleado.activo
            )
            
            // Guardar también en lista simple para compatibilidad
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val typeSimple = object : TypeToken<MutableList<EmpleadoSimple>>() {}.type
            val empleados: MutableList<EmpleadoSimple> = gson.fromJson(empleadosJson, typeSimple) ?: mutableListOf()
            empleados.add(empleadoSimpleEquivalente)
            
            val nuevaListaSimple = gson.toJson(empleados)
            sharedPreferences.edit().putString("empleados_list", nuevaListaSimple).apply()
            
        } catch (e: Exception) {
            showMessage("❌ Error al guardar empleado flexible: ${e.message}")
        }
    }
    
    private fun showMessage(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Fallback si Toast falla
            println("Message: $message")
        }
    }
}