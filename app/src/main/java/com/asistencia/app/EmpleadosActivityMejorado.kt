package com.asistencia.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.app.Dialog
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.asistencia.app.utils.PinManager
import com.asistencia.app.database.Empleado
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            
            // Verificar si se abrió para editar un empleado
            val editarEmpleado = intent.getBooleanExtra("EDITAR_EMPLEADO", false)
            val empleadoId = intent.getStringExtra("EMPLEADO_ID")
            
            if (editarEmpleado && empleadoId != null) {
                // Abrir directamente el editor de horarios para el empleado
                abrirEditorDirecto(empleadoId)
            } else {
                createLayout()
                loadEmpleados()
            }
            
            // Registrar actividad para el sistema de PIN
            PinManager.updateLastActivity(this)
            
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
    
    private fun abrirEditorDirecto(empleadoId: String) {
        try {
            // Buscar el empleado en la base de datos Room
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val empleado = buscarEmpleadoEnBD(empleadoId)
                    if (empleado != null) {
                        // Si es un empleado de la base de datos, mostrar opciones
                        mostrarOpcionesEmpleadoBD(empleado)
                    } else {
                        // Si no se encuentra, mostrar error y cerrar
                        showMessage("❌ No se encontró el empleado para editar")
                        finish()
                    }
                } catch (e: Exception) {
                    showMessage("❌ Error al buscar empleado: ${e.message}")
                    finish()
                }
            }
        } catch (e: Exception) {
            showMessage("❌ Error al abrir editor: ${e.message}")
            finish()
        }
    }
    
    private fun mostrarOpcionesEmpleadoBD(empleado: Empleado) {
        val opciones = arrayOf("✏️ Editar Horario", "📋 Ver Detalles", "❌ Cancelar")
        
        AlertDialog.Builder(this)
            .setTitle("👤 ${empleado.nombres} ${empleado.apellidos}")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> editarHorarioEmpleadoBD(empleado)
                    1 -> verDetallesEmpleadoBD(empleado)
                    2 -> finish()
                }
            }
            .setCancelable(false)
            .show()
    }
    
    private fun editarHorarioEmpleadoBD(empleado: Empleado) {
        // Por ahora, mostrar mensaje de que la edición de empleados de BD está en desarrollo
        showMessage("🚧 Edición de empleados de base de datos en desarrollo")
        finish()
    }
    
    private fun verDetallesEmpleadoBD(empleado: Empleado) {
        // Por ahora, mostrar mensaje y cerrar
        showMessage("📋 Detalles del empleado: ${empleado.nombres} ${empleado.apellidos}")
        finish()
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
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // Color verde de Promesa
            setOnClickListener { 
                mostrarDialogoAgregar()
            }
        }
        mainLayout.addView(btnAgregar)
        
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
        
        // Botón Editar eliminado - no funcionaba correctamente
        
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
        
        // Botón Editar eliminado - no funcionaba correctamente
        
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
            // Usar coroutine para buscar el empleado en la base de datos
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                val empleadoCompleto = buscarEmpleadoEnBD(empleado.dni)
                
                if (empleadoCompleto != null) {
                    // Abrir la nueva actividad de detalle
                    val intent = Intent(this@EmpleadosActivityMejorado, EmpleadoDetalleActivity::class.java).apply {
                        putExtra(EmpleadoDetalleActivity.EXTRA_EMPLEADO_ID, empleadoCompleto.id)
                    }
                    startActivity(intent)
                } else {
                    // Si no está en la BD, mostrar opciones
                    mostrarOpcionesEmpleadoBasico(empleado)
                }
            }
                
        } catch (e: Exception) {
            showMessage("Error al mostrar detalles: ${e.message}")
        }
    }
    
    private fun mostrarOpcionesEmpleadoBasico(empleado: EmpleadoSimple) {
        val opciones = arrayOf("📱 Ver Detalle Básico", "🔄 Migrar a Base de Datos", "✏️ Editar")
        
        AlertDialog.Builder(this)
            .setTitle("👤 ${empleado.nombres} ${empleado.apellidos}")
            .setMessage("Este empleado está en modo básico. ¿Qué desea hacer?")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> mostrarDetalleBasico(empleado)
                    1 -> migrarEmpleadoABD(empleado)
                    2 -> editarEmpleado(empleado)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun mostrarDetalleBasico(empleado: EmpleadoSimple) {
        val mensaje = buildString {
            append("👤 EMPLEADO EN MODO BÁSICO\n\n")
            append("📝 Nombre: ${empleado.nombres} ${empleado.apellidos}\n")
            append("🆔 DNI: ${empleado.dni}\n")
            append("⏰ Horario: ${empleado.horaEntrada} - ${empleado.horaSalida}\n")
            append("📊 Estado: ${if (empleado.activo) "✅ Activo" else "❌ Inactivo"}\n\n")
            append("💡 Para acceder a todas las funciones:\n")
            append("• Migre el empleado a la base de datos\n")
            append("• O use el modo de edición básico")
        }
        
        AlertDialog.Builder(this)
            .setTitle("📋 Detalle del Empleado")
            .setMessage(mensaje)
            .setPositiveButton("🔄 Migrar a BD") { _, _ ->
                migrarEmpleadoABD(empleado)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
    
    private fun migrarEmpleadoABD(empleado: EmpleadoSimple) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val empleadoBD = com.asistencia.app.database.Empleado(
                    dni = empleado.dni,
                    nombres = empleado.nombres,
                    apellidos = empleado.apellidos,
                    tipoHorario = com.asistencia.app.database.TipoHorario.REGULAR,
                    horaEntradaRegular = empleado.horaEntrada,
                    horaSalidaRegular = empleado.horaSalida,
                    activo = empleado.activo
                )
                
                val repository = com.asistencia.app.repository.AsistenciaRepository(this@EmpleadosActivityMejorado)
                repository.insertEmpleado(empleadoBD)
                
                showMessage("✅ Empleado migrado exitosamente a la base de datos")
                
                // Ahora abrir la nueva actividad
                val intent = Intent(this@EmpleadosActivityMejorado, EmpleadoDetalleActivity::class.java).apply {
                    putExtra(EmpleadoDetalleActivity.EXTRA_EMPLEADO_ID, empleadoBD.id)
                }
                startActivity(intent)
                
            } catch (e: Exception) {
                showMessage("❌ Error al migrar: ${e.message}")
            }
        }
    }
    
    private suspend fun buscarEmpleadoEnBD(dni: String): com.asistencia.app.database.Empleado? {
        return try {
            val repository = com.asistencia.app.repository.AsistenciaRepository(this)
            repository.getEmpleadoByDni(dni)
        } catch (e: Exception) {
            null
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
            val mensaje = empleado.getInfoDiaActual()
            
            AlertDialog.Builder(this)
                .setTitle("📋 Detalles del Empleado Flexible")
                .setMessage(mensaje)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("⏰ Editar Horarios") { dialog, which ->
                    editarHorarioFlexible(empleado)
                }
                .setNegativeButton("🗑️ Eliminar") { dialog, which ->
                    eliminarEmpleadoFlexible(empleado)
                }
                .show()
                
        } catch (e: Exception) {
            showMessage("Error al mostrar detalles: ${e.message}")
        }
    }
    
    private fun editarHorarioFlexible(empleado: EmpleadoFlexible) {
        try {
            // Crear el diálogo personalizado
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_editar_horario_flexible)
            
            // Configurar el diálogo
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setCancelable(true) // Permitir cerrar con botón atrás nativo
            
            // Configurar información del empleado
            dialog.findViewById<TextView>(R.id.tv_nombre_empleado_editar).text = "${empleado.nombres} ${empleado.apellidos}"
            dialog.findViewById<TextView>(R.id.tv_dni_empleado_editar).text = "DNI: ${empleado.dni}"
            
            // Cargar horarios actuales
            cargarHorariosActualesEnDialogo(dialog, empleado)
            
            // Los botones de acción rápida se han eliminado para simplificar la interfaz
            
            // Configurar botones principales
            dialog.findViewById<Button>(R.id.btn_cancelar_editar).setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.findViewById<Button>(R.id.btn_guardar_editar).setOnClickListener {
                guardarHorarioFlexibleEditado(dialog, empleado)
                dialog.dismiss()
            }
            
            // Mostrar el diálogo
            dialog.show()
            
        } catch (e: Exception) {
            showMessage("❌ Error al abrir editor de horarios: ${e.message}")
        }
    }
    
    private fun cargarHorariosActualesEnDialogo(dialog: Dialog, empleado: EmpleadoFlexible) {
        try {
            val dias = listOf("L", "M", "X", "J", "V", "S", "D")
            val nombresDias = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
            
            dias.forEachIndexed { index, codigo ->
                val nombreDia = nombresDias[index]
                val horario = empleado.horariosSemanales[codigo]
                val refrigerio = empleado.refrigeriosSemanales[codigo]
                val estaActivo = empleado.diasActivos.contains(codigo)
                
                // Configurar switch del día
                val switchId = resources.getIdentifier("switch_${nombreDia}_editar", "id", packageName)
                val switchDia = dialog.findViewById<CheckBox>(switchId)
                switchDia?.isChecked = estaActivo
                
                // Configurar campos de entrada y salida
                val etEntradaId = resources.getIdentifier("et_${nombreDia}_entrada_editar", "id", packageName)
                val etEntrada = dialog.findViewById<EditText>(etEntradaId)
                etEntrada?.setText(horario?.first ?: "08:00")
                
                val etSalidaId = resources.getIdentifier("et_${nombreDia}_salida_editar", "id", packageName)
                val etSalida = dialog.findViewById<EditText>(etSalidaId)
                etSalida?.setText(horario?.second ?: "17:00")
                
                // Configurar campos de refrigerio
                val etRefrigerioInicioId = resources.getIdentifier("et_${nombreDia}_refrigerio_inicio_editar", "id", packageName)
                val etRefrigerioInicio = dialog.findViewById<EditText>(etRefrigerioInicioId)
                etRefrigerioInicio?.setText(refrigerio?.first ?: "12:00")
                
                val etRefrigerioFinId = resources.getIdentifier("et_${nombreDia}_refrigerio_fin_editar", "id", packageName)
                val etRefrigerioFin = dialog.findViewById<EditText>(etRefrigerioFinId)
                etRefrigerioFin?.setText(refrigerio?.second ?: "13:00")
            }
            
        } catch (e: Exception) {
            showMessage("❌ Error al cargar horarios: ${e.message}")
        }
    }
    
    // Los botones de acción rápida se han eliminado para simplificar la interfaz
    
    // La función de aplicar horario rápido se ha eliminado
    
    private fun desactivarRefrigerioEnDialogo(dialog: Dialog) {
        try {
            val nombresDias = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
            
            nombresDias.forEach { nombreDia ->
                val etRefrigerioInicioId = resources.getIdentifier("et_${nombreDia}_refrigerio_inicio_editar", "id", packageName)
                val etRefrigerioInicio = dialog.findViewById<EditText>(etRefrigerioInicioId)
                etRefrigerioInicio?.setText("")
                
                val etRefrigerioFinId = resources.getIdentifier("et_${nombreDia}_refrigerio_fin_editar", "id", packageName)
                val etRefrigerioFin = dialog.findViewById<EditText>(etRefrigerioFinId)
                etRefrigerioFin?.setText("")
            }
            
        } catch (e: Exception) {
            showMessage("❌ Error al desactivar refrigerio: ${e.message}")
        }
    }
    
    private fun guardarHorarioFlexibleEditado(dialog: Dialog, empleado: EmpleadoFlexible) {
        try {
            val horarios = mutableMapOf<String, Pair<String, String>>()
            val refrigerios = mutableMapOf<String, Pair<String, String>>()
            val diasActivos = mutableListOf<String>()
            
            val dias = listOf("L", "M", "X", "J", "V", "S", "D")
            val nombresDias = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
            
            dias.forEachIndexed { index, codigo ->
                val nombreDia = nombresDias[index]
                
                // Obtener switch del día
                val switchId = resources.getIdentifier("switch_${nombreDia}_editar", "id", packageName)
                val switchDia = dialog.findViewById<CheckBox>(switchId)
                
                if (switchDia?.isChecked == true) {
                    // Obtener horarios
                    val etEntradaId = resources.getIdentifier("et_${nombreDia}_entrada_editar", "id", packageName)
                    val etEntrada = dialog.findViewById<EditText>(etEntradaId)
                    val entrada = etEntrada?.text.toString().trim()
                    
                    val etSalidaId = resources.getIdentifier("et_${nombreDia}_salida_editar", "id", packageName)
                    val etSalida = dialog.findViewById<EditText>(etSalidaId)
                    val salida = etSalida?.text.toString().trim()
                    
                    if (entrada.isNotEmpty() && salida.isNotEmpty()) {
                        horarios[codigo] = Pair(entrada, salida)
                        
                        // Obtener refrigerio
                        val etRefrigerioInicioId = resources.getIdentifier("et_${nombreDia}_refrigerio_inicio_editar", "id", packageName)
                        val etRefrigerioInicio = dialog.findViewById<EditText>(etRefrigerioInicioId)
                        val refrigerioInicio = etRefrigerioInicio?.text.toString().trim()
                        
                        val etRefrigerioFinId = resources.getIdentifier("et_${nombreDia}_refrigerio_fin_editar", "id", packageName)
                        val etRefrigerioFin = dialog.findViewById<EditText>(etRefrigerioFinId)
                        val refrigerioFin = etRefrigerioFin?.text.toString().trim()
                        
                        if (refrigerioInicio.isNotEmpty() && refrigerioFin.isNotEmpty()) {
                            refrigerios[codigo] = Pair(refrigerioInicio, refrigerioFin)
                        }
                        
                        diasActivos.add(codigo)
                    }
                }
            }
            
            if (diasActivos.isEmpty()) {
                showMessage("❌ Debe configurar al menos un día de trabajo")
                return
            }
            
            // Actualizar empleado flexible
            val empleadoActualizado = empleado.copy(
                horariosSemanales = horarios,
                refrigeriosSemanales = refrigerios,
                diasActivos = diasActivos
            )
            
            // Guardar cambios
            val empleadosFlexiblesJson = sharedPreferences.getString("empleados_flexibles", "[]")
            val type = object : TypeToken<MutableList<EmpleadoFlexible>>() {}.type
            val empleadosFlexibles: MutableList<EmpleadoFlexible> = gson.fromJson(empleadosFlexiblesJson, type) ?: mutableListOf()
            
            val index = empleadosFlexibles.indexOfFirst { it.dni == empleado.dni }
            if (index != -1) {
                empleadosFlexibles[index] = empleadoActualizado
                
                val nuevaLista = gson.toJson(empleadosFlexibles)
                sharedPreferences.edit().putString("empleados_flexibles", nuevaLista).apply()
                
                showMessage("✅ Horario flexible actualizado para ${empleado.nombres} ${empleado.apellidos}")
                loadEmpleados()
            } else {
                showMessage("❌ No se encontró el empleado para actualizar")
            }
            
        } catch (e: Exception) {
            showMessage("❌ Error al guardar horario flexible: ${e.message}")
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
                empleados[index] = EmpleadoSimple(dni, nombres, apellidos, entrada, salida, "12:00", "13:00", false, activo)
                
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
            

            
            AlertDialog.Builder(this)
                .setTitle("➕ Agregar Empleado")
                .setView(dialogLayout)
                .setPositiveButton("Continuar") { _, _ ->
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
            val nuevoEmpleado = EmpleadoSimple(dni, nombres, apellidos, entrada, salida, "12:00", "13:00", false, true)
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
    
    private fun limpiarDuplicados() {
        try {
            // Cargar empleados flexibles
            val empleadosFlexiblesJson = sharedPreferences.getString("empleados_flexibles", "[]")
            val typeFlexible = object : TypeToken<List<EmpleadoFlexible>>() {}.type
            val empleadosFlexibles: List<EmpleadoFlexible> = gson.fromJson(empleadosFlexiblesJson, typeFlexible) ?: emptyList()
            
            // Cargar empleados regulares
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val typeSimple = object : TypeToken<List<EmpleadoSimple>>() {}.type
            val empleados: List<EmpleadoSimple> = gson.fromJson(empleadosJson, typeSimple) ?: emptyList()
            
            // Filtrar empleados regulares que NO sean flexibles
            val empleadosRegularesFiltrados = empleados.filter { !it.esFlexible }
            
            // Guardar solo empleados regulares no flexibles
            val nuevaListaRegulares = gson.toJson(empleadosRegularesFiltrados)
            sharedPreferences.edit().putString("empleados_list", nuevaListaRegulares).apply()
            
            showMessage("✅ Duplicados eliminados")
            loadEmpleados()
            
        } catch (e: Exception) {
            showMessage("Error al limpiar duplicados: ${e.message}")
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
            
            // Crear diálogo de horario flexible CON REFRIGERIO
            val dialogView = layoutInflater.inflate(R.layout.dialog_horario_flexible_con_refrigerio, null)
            
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
            // Configurar botones de aplicación rápida
            val btnAplicarLV = dialogView.findViewById<Button>(R.id.btnAplicarLunesViernes)
            val btnAplicarLS = dialogView.findViewById<Button>(R.id.btnAplicarLunesSabado)
            val btnDesactivarRefrigerio = dialogView.findViewById<Button>(R.id.btnDesactivarRefrigerio)
            
            // Aplicar horario L-V (Lunes a Viernes)
            btnAplicarLV.setOnClickListener {
                val entrada = dialogView.findViewById<EditText>(R.id.et_hora_base_entrada).text.toString().ifEmpty { "08:00" }
                val salida = dialogView.findViewById<EditText>(R.id.et_hora_base_salida).text.toString().ifEmpty { "17:00" }
                val refrigerioInicio = dialogView.findViewById<EditText>(R.id.et_refrigerio_inicio).text.toString().ifEmpty { "12:00" }
                val refrigerioFin = dialogView.findViewById<EditText>(R.id.et_refrigerio_fin).text.toString().ifEmpty { "13:00" }
                
                aplicarHorarioYRefrigerioADias(dialogView, entrada, salida, refrigerioInicio, refrigerioFin, listOf("L", "M", "X", "J", "V"))
                showMessage("✅ Horario aplicado L-V: $entrada-$salida (Refrigerio: $refrigerioInicio-$refrigerioFin)")
            }
            
            // Aplicar horario L-S (Lunes a Sábado)
            btnAplicarLS.setOnClickListener {
                val entrada = dialogView.findViewById<EditText>(R.id.et_hora_base_entrada).text.toString().ifEmpty { "08:00" }
                val salida = dialogView.findViewById<EditText>(R.id.et_hora_base_salida).text.toString().ifEmpty { "17:00" }
                val refrigerioInicio = dialogView.findViewById<EditText>(R.id.et_refrigerio_inicio).text.toString().ifEmpty { "12:00" }
                val refrigerioFin = dialogView.findViewById<EditText>(R.id.et_refrigerio_fin).text.toString().ifEmpty { "13:00" }
                
                aplicarHorarioYRefrigerioADias(dialogView, entrada, salida, refrigerioInicio, refrigerioFin, listOf("L", "M", "X", "J", "V", "S"))
                showMessage("✅ Horario aplicado L-S: $entrada-$salida (Refrigerio: $refrigerioInicio-$refrigerioFin)")
            }
            
            // Desactivar refrigerio
            btnDesactivarRefrigerio.setOnClickListener {
                desactivarRefrigerioEnTodosLosDias(dialogView)
                showMessage("🚫 Refrigerio desactivado en todos los días")
            }
            
            // Configurar switches de días
            configurarSwitchesDias(dialogView)
            
        } catch (e: Exception) {
            showMessage("Error al configurar diálogo: ${e.message}")
        }
    }
    
    private fun aplicarHorarioYRefrigerioADias(dialogView: View, entrada: String, salida: String, refrigerioInicio: String, refrigerioFin: String, dias: List<String>) {
        dias.forEach { dia ->
            try {
                // Obtener IDs para el día específico
                val (switchId, entradaId, salidaId, refrigerioInicioId, refrigerioFinId) = when (dia) {
                    "L" -> arrayOf(R.id.switch_lunes, R.id.et_entrada_lunes, R.id.et_salida_lunes, R.id.et_refrigerio_inicio_lunes, R.id.et_refrigerio_fin_lunes)
                    "M" -> arrayOf(R.id.switch_martes, R.id.et_entrada_martes, R.id.et_salida_martes, R.id.et_refrigerio_inicio_martes, R.id.et_refrigerio_fin_martes)
                    "X" -> arrayOf(R.id.switch_miercoles, R.id.et_entrada_miercoles, R.id.et_salida_miercoles, R.id.et_refrigerio_inicio_miercoles, R.id.et_refrigerio_fin_miercoles)
                    "J" -> arrayOf(R.id.switch_jueves, R.id.et_entrada_jueves, R.id.et_salida_jueves, R.id.et_refrigerio_inicio_jueves, R.id.et_refrigerio_fin_jueves)
                    "V" -> arrayOf(R.id.switch_viernes, R.id.et_entrada_viernes, R.id.et_salida_viernes, R.id.et_refrigerio_inicio_viernes, R.id.et_refrigerio_fin_viernes)
                    "S" -> arrayOf(R.id.switch_sabado, R.id.et_entrada_sabado, R.id.et_salida_sabado, R.id.et_refrigerio_inicio_sabado, R.id.et_refrigerio_fin_sabado)
                    "D" -> arrayOf(R.id.switch_domingo, R.id.et_entrada_domingo, R.id.et_salida_domingo, R.id.et_refrigerio_inicio_domingo, R.id.et_refrigerio_fin_domingo)
                    else -> return@forEach
                }
                
                val switchActivo = dialogView.findViewById<Switch>(switchId)
                val etEntrada = dialogView.findViewById<EditText>(entradaId)
                val etSalida = dialogView.findViewById<EditText>(salidaId)
                val etRefrigerioInicio = dialogView.findViewById<EditText>(refrigerioInicioId)
                val etRefrigerioFin = dialogView.findViewById<EditText>(refrigerioFinId)
                
                // Activar el día
                switchActivo?.isChecked = true
                
                // Aplicar horarios de trabajo
                etEntrada?.setText(entrada)
                etSalida?.setText(salida)
                
                // Aplicar horarios de refrigerio
                etRefrigerioInicio?.setText(refrigerioInicio)
                etRefrigerioFin?.setText(refrigerioFin)
                
            } catch (e: Exception) {
                showMessage("Error aplicando horario a $dia: ${e.message}")
            }
        }
    }
    
    private fun desactivarRefrigerioEnTodosLosDias(dialogView: View) {
        val dias = listOf("L", "M", "X", "J", "V", "S", "D")
        
        dias.forEach { dia ->
            try {
                // Obtener IDs para el día específico
                val (refrigerioInicioId, refrigerioFinId) = when (dia) {
                    "L" -> Pair(R.id.et_refrigerio_inicio_lunes, R.id.et_refrigerio_fin_lunes)
                    "M" -> Pair(R.id.et_refrigerio_inicio_martes, R.id.et_refrigerio_fin_martes)
                    "X" -> Pair(R.id.et_refrigerio_inicio_miercoles, R.id.et_refrigerio_fin_miercoles)
                    "J" -> Pair(R.id.et_refrigerio_inicio_jueves, R.id.et_refrigerio_fin_jueves)
                    "V" -> Pair(R.id.et_refrigerio_inicio_viernes, R.id.et_refrigerio_fin_viernes)
                    "S" -> Pair(R.id.et_refrigerio_inicio_sabado, R.id.et_refrigerio_fin_sabado)
                    "D" -> Pair(R.id.et_refrigerio_inicio_domingo, R.id.et_refrigerio_fin_domingo)
                    else -> return@forEach
                }
                
                val etRefrigerioInicio = dialogView.findViewById<EditText>(refrigerioInicioId)
                val etRefrigerioFin = dialogView.findViewById<EditText>(refrigerioFinId)
                
                // Desactivar refrigerio (establecer horarios vacíos)
                etRefrigerioInicio?.setText("")
                etRefrigerioFin?.setText("")
                
            } catch (e: Exception) {
                showMessage("Error desactivando refrigerio en $dia: ${e.message}")
            }
        }
    }
    
    private fun configurarSwitchesDias(dialogView: View) {
        val dias = listOf("L", "M", "X", "J", "V", "S", "D")
        
        dias.forEach { dia ->
            try {
                // Usar los IDs correctos del modal de horario flexible con refrigerio
                val entradaId = when (dia) {
                    "L" -> R.id.et_entrada_lunes
                    "M" -> R.id.et_entrada_martes
                    "X" -> R.id.et_entrada_miercoles
                    "J" -> R.id.et_entrada_jueves
                    "V" -> R.id.et_entrada_viernes
                    "S" -> R.id.et_entrada_sabado
                    else -> return@forEach
                }
                
                val etEntrada = dialogView.findViewById<EditText>(entradaId)
                
                // El nuevo modal no tiene switches individuales por día
                // Los campos siempre están visibles
                
            } catch (e: Exception) {
                showMessage("Error configurando switch $dia: ${e.message}")
            }
        }
    }
    
    private fun guardarEmpleadoConHorarioFlexible(dni: String, nombres: String, apellidos: String, dialogView: View) {
        try {
            // Recopilar horarios y refrigerios de todos los días
            val horarios = mutableMapOf<String, Pair<String, String>>()
            val refrigerios = mutableMapOf<String, Pair<String, String>>()
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
                // Usar los IDs correctos del modal de horario flexible con refrigerio
                val (entradaId, salidaId) = when (codigo) {
                    "L" -> Pair(R.id.et_entrada_lunes, R.id.et_salida_lunes)
                    "M" -> Pair(R.id.et_entrada_martes, R.id.et_salida_martes)
                    "X" -> Pair(R.id.et_entrada_miercoles, R.id.et_salida_miercoles)
                    "J" -> Pair(R.id.et_entrada_jueves, R.id.et_salida_jueves)
                    "V" -> Pair(R.id.et_entrada_viernes, R.id.et_salida_viernes)
                    "S" -> Pair(R.id.et_entrada_sabado, R.id.et_salida_sabado)
                    "D" -> Pair(R.id.et_entrada_domingo, R.id.et_salida_domingo)
                    else -> return@forEach
                }
                
                val etEntrada = dialogView.findViewById<EditText>(entradaId)
                val etSalida = dialogView.findViewById<EditText>(salidaId)
                
                val entrada = etEntrada?.text.toString().trim() ?: ""
                val salida = etSalida?.text.toString().trim() ?: ""
                
                if (entrada.isNotEmpty() && salida.isNotEmpty()) {
                    horarios[codigo] = Pair(entrada, salida)
                    diasActivos.add(codigo)
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
                refrigeriosSemanales = refrigerios, // Refrigerios configurados
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
            
            // Verificar si ya existe un empleado con el mismo DNI
            if (empleadosFlexibles.any { it.dni == empleado.dni }) {
                showMessage("❌ Ya existe un empleado con DNI ${empleado.dni}")
                return
            }
            
            // Agregar nuevo empleado
            empleadosFlexibles.add(empleado)
            
            // Guardar lista actualizada
            val nuevaLista = gson.toJson(empleadosFlexibles)
            sharedPreferences.edit().putString("empleados_flexibles", nuevaLista).apply()
            
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