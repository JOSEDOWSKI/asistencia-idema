package com.asistencia.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EmpleadosActivityMejorado : AppCompatActivity() {
    
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empleados_mejorado)
        
        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        
        // Configurar RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerEmpleados)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Cargar lista de empleados
        loadEmpleados()
    }
    
    private fun loadEmpleados() {
        try {
            // Datos de ejemplo para pruebas
            val empleados = listOf(
                EmpleadoSimple("12345678", "Juan", "Pérez"),
                EmpleadoSimple("87654321", "María", "González", "09:00", "18:00", "Ventas"),
                EmpleadoSimple("45678912", "Carlos", "López", "08:30", "17:30", "Soporte", false)
            )
            
            // Actualizar UI con la lista de empleados
            updateEmpleadosList(empleados)
            
        } catch (e: Exception) {
            showMessage("Error al cargar empleados: ${e.message}")
        }
    }
    
    private fun updateEmpleadosList(empleados: List<EmpleadoSimple>) {
        // Actualizar el RecyclerView con la lista de empleados
        val adapter = EmpleadosAdapter(empleados) { empleado, accion ->
            when (accion) {
                Accion.EDITAR -> editarEmpleado(empleado)
                Accion.ELIMINAR -> eliminarEmpleado(empleado)
                Accion.VER_DETALLE -> mostrarDetallesEmpleado(empleado)
                Accion.MARCAR_ASISTENCIA -> marcarAsistencia(empleado)
                Accion.VER_HISTORIAL -> verHistorial(empleado)
            }
        }
        
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerEmpleados)
        recyclerView.adapter = adapter
        
        // Notificar al adaptador que los datos han cambiado
        adapter.notifyDataSetChanged()
    }
    
    private fun mostrarDetallesEmpleado(empleado: EmpleadoSimple) {
        // Implementar lógica para mostrar detalles del empleado
        showMessage("Mostrando detalles de: ${empleado.nombres} ${empleado.apellidos}")
    }
    
    private fun editarEmpleado(empleado: EmpleadoSimple) {
        // Implementar lógica para editar empleado
        showMessage("Editando empleado: ${empleado.dni}")
    }
    
    private fun marcarAsistencia(empleado: EmpleadoSimple) {
        // Implementar lógica para marcar asistencia
        showMessage("Marcando asistencia para: ${empleado.nombres}")
    }
    
    private fun verHistorial(empleado: EmpleadoSimple) {
        // Implementar lógica para ver historial
        showMessage("Mostrando historial de: ${empleado.nombres}")
    }
    
    private fun eliminarEmpleado(empleado: EmpleadoSimple) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar empleado")
            .setMessage("¿Está seguro de eliminar a ${empleado.nombres}?")
            .setPositiveButton("Eliminar") { _, _ ->
                // Implementar lógica de eliminación
                showMessage("Empleado eliminado")
                loadEmpleados()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    // Clase de datos
    data class EmpleadoSimple(
        val dni: String,
        val nombres: String,
        val apellidos: String,
        val horaEntrada: String = "08:00",
        val horaSalida: String = "17:00",
        val area: String = "Sin área asignada",
        val activo: Boolean = true,
        val tipoHorario: String = "FIJO", // FIJO o FLEXIBLE
        val tieneRefrigerio: Boolean = false,
        val horaInicioRefrigerio: String = "13:00",
        val horaFinRefrigerio: String = "14:00",
        val horarioFlexible: Map<String, Pair<String, String>> = emptyMap() // Día -> (HoraEntrada, HoraSalida)
    ) {
        fun getHorarioDia(dia: String): Pair<String, String> {
            return if (tipoHorario == "FLEXIBLE") {
                horarioFlexible[dia] ?: Pair(horaEntrada, horaSalida)
            } else {
                Pair(horaEntrada, horaSalida)
            }
        }
    }
    
    // Adaptador para el RecyclerView
    inner class EmpleadosAdapter(
        private val empleados: List<EmpleadoSimple>,
        private val onItemClick: (EmpleadoSimple, Accion) -> Unit
    ) : RecyclerView.Adapter<EmpleadosAdapter.EmpleadoViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpleadoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_empleado, parent, false)
            return EmpleadoViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: EmpleadoViewHolder, position: Int) {
            val empleado = empleados[position]
            holder.bind(empleado, onItemClick, this@EmpleadosActivityMejorado)
        }
        
        override fun getItemCount() = empleados.size
        
        inner class EmpleadoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val txtNombre: TextView = itemView.findViewById(R.id.tv_nombre_empleado)
            private val txtDni: TextView = itemView.findViewById(R.id.tv_dni_empleado)
            private val txtArea: TextView = itemView.findViewById(R.id.tv_area_empleado)
            private val txtHorario: TextView = itemView.findViewById(R.id.tv_horario_empleado)
            private val txtEstado: TextView = itemView.findViewById(R.id.tv_estado_empleado)
            private val btnOpciones: ImageButton = itemView.findViewById(R.id.btn_opciones_empleado)
            
            fun bind(
                empleado: EmpleadoSimple,
                onItemClick: (EmpleadoSimple, Accion) -> Unit,
                activity: EmpleadosActivityMejorado
            ) {
                txtNombre.text = "${empleado.nombres} ${empleado.apellidos}"
                txtDni.text = "DNI: ${empleado.dni}"
                txtArea.text = empleado.area
                txtHorario.text = "⏰ ${empleado.horaEntrada} - ${empleado.horaSalida}"
                txtEstado.text = if (empleado.activo) "✅ Activo" else "❌ Inactivo"
                txtEstado.setTextColor(
                    if (empleado.activo) 
                        android.graphics.Color.parseColor("#4CAF50")
                    else 
                        android.graphics.Color.RED
                )
                
                itemView.setOnClickListener {
                    onItemClick(empleado, Accion.VER_DETALLE)
                }
                
                btnOpciones.setOnClickListener {
                    activity.showOptionsMenu(btnOpciones, empleado)
                }
            }
        }
    }
    
    enum class Accion {
        VER_DETALLE, EDITAR, ELIMINAR, MARCAR_ASISTENCIA, VER_HISTORIAL
    }
    
    private fun showOptionsMenu(view: View, empleado: EmpleadoSimple) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_empleado_item, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_editar -> {
                    editarEmpleado(empleado)
                    true
                }
                R.id.menu_eliminar -> {
                    eliminarEmpleado(empleado)
                    true
                }
                R.id.menu_marcar_asistencia -> {
                    marcarAsistencia(empleado)
                    true
                }
                R.id.menu_ver_historial -> {
                    verHistorial(empleado)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
}
