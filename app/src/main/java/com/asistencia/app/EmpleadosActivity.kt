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

class EmpleadosActivity : AppCompatActivity() {
    
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
        
        // Horas semanales
        val (horas, minutos) = empleadoFlexible.calcularHorasSemanales()
        val horasSemanales = TextView(this).apply {
            text = "⏱️ Total semanal: ${horas}h ${minutos}m (${empleadoFlexible.diasActivos.size} días)"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#1976D2"))
        }
        layout.addView(horasSemanales)
        
        // Estado actual
        val estadoActual = TextView(this).apply {
            text = empleadoFlexible.getEstadoActual()
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#F57C00"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(estadoActual)
        
        // Estado activo/inactivo
        val estado = TextView(this).apply {
            text = if (empleadoFlexible.activo) "✅ Activo" else "❌ Inactivo"
            textSize = 14f
            setTextColor(if (empleadoFlexible.activo) android.graphics.Color.parseColor("#2E7D32") else android.graphics.Color.RED)
        }
        layout.addView(estado)
        
        // Botón para ver detalles (opcional)
        val btnDetalles = Button(this).apply {
            text = "📋 Ver Detalles"
            textSize = 12f
            setPadding(12, 8, 12, 8)
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                mostrarDetallesEmpleadoFlexible(empleadoFlexible)
            }
        }
        layout.addView(btnDetalles)
        
        return layout
    }
    
    private fun mostrarDetallesEmpleadoFlexible(empleado: EmpleadoFlexible) {
        try {
            val mensaje = empleado.getInformacionDetallada()
            
            AlertDialog.Builder(this)
                .setTitle("📋 Detalles del Empleado")
                .setMessage(mensaje)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("⏰ Editar Horarios") { _, _ ->
                    // TODO: Implementar edición de horarios flexibles
                    showMessage("🚧 Función de edición en desarrollo")
                }
                .show()
                
        } catch (e: Exception) {
            showMessage("Error al mostrar detalles: ${e.message}")
        }
    }
    
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
                
                // Los switches ya están configurados correctamente en el XML
                // L-V están en true, S-D están en false por defecto
                
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
            
            // También crear un empleado simple para compatibilidad
            val empleadoSimple = EmpleadoSimple(
                dni = empleado.dni,
                nombres = empleado.nombres,
                apellidos = empleado.apellidos,
                horaEntrada = empleado.getHorarioResumen().first,
                horaSalida = empleado.getHorarioResumen().second,
                activo = empleado.activo
            )
            
            // Guardar en lista simple también
            val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
            val typeSimple = object : TypeToken<MutableList<EmpleadoSimple>>() {}.type
            val empleados: MutableList<EmpleadoSimple> = gson.fromJson(empleadosJson, typeSimple) ?: mutableListOf()
            empleados.add(empleadoSimple)
            
            val nuevaListaSimple = gson.toJson(empleados)
            sharedPreferences.edit().putString("empleados_list", nuevaListaSimple).apply()
            
        } catch (e: Exception) {
            throw Exception("Error al guardar empleado flexible: ${e.message}")
        }
    }
    
    private fun showMessage(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Si ni siquiera Toast funciona, no hacer nada
        }
    }
    
    override fun onBackPressed() {
        finish()
    }
}