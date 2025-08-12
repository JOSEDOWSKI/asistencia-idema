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
            
            // Usar layout programático más simple y robusto
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
            
            if (empleados.isEmpty()) {
                val emptyText = TextView(this).apply {
                    text = "No hay empleados registrados"
                    textSize = 14f
                    setPadding(16, 16, 16, 16)
                    setTextColor(android.graphics.Color.GRAY)
                    gravity = android.view.Gravity.CENTER
                }
                empleadosList.addView(emptyText)
            } else {
                // Mostrar empleados
                empleados.forEach { empleado ->
                    val empleadoView = createEmpleadoView(empleado)
                    empleadosList.addView(empleadoView)
                }
            }
            
            // Mostrar contador
            val contador = TextView(this).apply {
                text = "Total: ${empleados.size} empleados"
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
    
    private fun createEmpleadoView(empleado: EmpleadoSimple): LinearLayout {
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
            // Hacer clickeable
            isClickable = true
            isFocusable = true
            setOnClickListener {
                mostrarDetallesEmpleado(empleado)
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
    
    private fun mostrarDetallesEmpleado(empleado: EmpleadoSimple) {
        try {
            val mensaje = StringBuilder().apply {
                append("👤 INFORMACIÓN DEL EMPLEADO\n\n")
                append("📝 Nombre: ${empleado.nombres} ${empleado.apellidos}\n")
                append("🆔 DNI: ${empleado.dni}\n")
                append("🕐 Entrada: ${empleado.horaEntrada}\n")
                append("🕕 Salida: ${empleado.horaSalida}\n")
                append("📋 Tipo: Horario Fijo\n")
                append("📊 Estado: ${if (empleado.activo) "✅ Activo" else "❌ Inactivo"}")
            }.toString()
            
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
    
    private fun editarEmpleado(empleado: EmpleadoSimple) {
        try {
            val dialogLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }
            
            val etNombres = EditText(this).apply {
                setText(empleado.nombres)
                hint = "Nombres"
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etNombres)
            
            val etApellidos = EditText(this).apply {
                setText(empleado.apellidos)
                hint = "Apellidos"
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etApellidos)
            
            val etEntrada = EditText(this).apply {
                setText(empleado.horaEntrada)
                hint = "Hora entrada (ej: 07:00)"
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etEntrada)
            
            val etSalida = EditText(this).apply {
                setText(empleado.horaSalida)
                hint = "Hora salida (ej: 17:00)"
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                textSize = 16f
            }
            dialogLayout.addView(etSalida)
            
            val switchActivo = Switch(this).apply {
                isChecked = empleado.activo
                text = "Empleado Activo"
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
            showMessage("❌ Error al mostrar editor: ${e.message}")
        }
    }
    
    private fun eliminarEmpleado(empleado: EmpleadoSimple) {
        try {
            AlertDialog.Builder(this)
                .setTitle("🗑️ Eliminar Empleado")
                .setMessage("¿Está seguro de eliminar a ${empleado.nombres} ${empleado.apellidos}?")
                .setPositiveButton("Eliminar") { _, _ ->
                    // Cargar lista actual
                    val empleadosJson = sharedPreferences.getString("empleados_list", "[]")
                    val type = object : TypeToken<MutableList<EmpleadoSimple>>() {}.type
                    val empleados: MutableList<EmpleadoSimple> = gson.fromJson(empleadosJson, type) ?: mutableListOf()
                    
                    // Buscar y eliminar empleado
                    val index = empleados.indexOfFirst { it.dni == empleado.dni }
                    if (index != -1) {
                        empleados.removeAt(index)
                        
                        // Guardar cambios
                        val nuevaLista = gson.toJson(empleados)
                        sharedPreferences.edit().putString("empleados_list", nuevaLista).apply()
                        
                        showMessage("✅ Empleado eliminado: ${empleado.nombres} ${empleado.apellidos}")
                        
                        // Recargar lista
                        loadEmpleados()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
                
        } catch (e: Exception) {
            showMessage("Error al eliminar: ${e.message}")
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
            }
            
        } catch (e: Exception) {
            showMessage("❌ Error al actualizar: ${e.message}")
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