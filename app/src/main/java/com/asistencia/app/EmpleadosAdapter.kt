package com.asistencia.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.asistencia.app.database.Empleado
import com.asistencia.app.database.TipoHorario

class EmpleadosAdapter(
    private val onEmpleadoAction: (Empleado, Accion) -> Unit
) : ListAdapter<Empleado, EmpleadosAdapter.EmpleadoViewHolder>(EmpleadoDiffCallback()) {
    
    enum class Accion {
        VER_DETALLE,
        EDITAR,
        GENERAR_QR,
        ACTIVAR_DESACTIVAR,
        ELIMINAR
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpleadoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_empleado, parent, false)
        return EmpleadoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: EmpleadoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class EmpleadoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_empleado)
        private val tvDni: TextView = itemView.findViewById(R.id.tv_dni_empleado)
        private val tvArea: TextView = itemView.findViewById(R.id.tv_area_empleado)
        private val tvHorario: TextView = itemView.findViewById(R.id.tv_horario_empleado)
        private val tvEstado: TextView = itemView.findViewById(R.id.tv_estado_empleado)
        private val btnOpciones: ImageButton = itemView.findViewById(R.id.btn_opciones_empleado)
        private val cardEmpleado: androidx.cardview.widget.CardView = itemView.findViewById(R.id.card_empleado)
        
        fun bind(empleado: Empleado) {
            tvNombre.text = "${empleado.nombres} ${empleado.apellidos}"
            tvDni.text = "DNI: ${empleado.dni}"
            tvArea.text = empleado.area ?: "Sin área asignada"
            
            // Configurar horario
            val horarioTexto = when (empleado.tipoHorario) {
                TipoHorario.REGULAR -> {
                    if (empleado.horaEntradaRegular != null && empleado.horaSalidaRegular != null) {
                        "⏰ Regular: ${empleado.horaEntradaRegular} - ${empleado.horaSalidaRegular}"
                    } else {
                        "⚠️ Horario no configurado"
                    }
                }
                TipoHorario.FLEXIBLE -> {
                    "📅 Flexible por días"
                }
            }
            tvHorario.text = horarioTexto
            
            // Configurar estado
            if (empleado.activo) {
                tvEstado.text = "✅ Activo"
                tvEstado.setTextColor(itemView.context.getColor(R.color.success_color))
                cardEmpleado.alpha = 1.0f
            } else {
                tvEstado.text = "❌ Inactivo"
                tvEstado.setTextColor(itemView.context.getColor(R.color.error_color))
                cardEmpleado.alpha = 0.6f
            }
            
            // Click en card deshabilitado - usar el botón de opciones (engranaje)
            cardEmpleado.setOnClickListener(null)
            cardEmpleado.isClickable = false
            
            // Configurar menú de opciones - ENGRANAJE MUY VISIBLE
            btnOpciones.apply {
                // Hacer el engranaje MUY visible y llamativo
                setBackgroundColor(itemView.context.getColor(R.color.promesa_green))
                setColorFilter(itemView.context.getColor(android.R.color.white))
                alpha = 1.0f  // Completamente opaco
                scaleX = 1.3f  // 30% más grande
                scaleY = 1.3f  // 30% más grande
                elevation = 4f  // Sombra para destacar
                
                // Agregar padding para hacer el área de toque más grande
                setPadding(8, 8, 8, 8)
                
                setOnClickListener {
                    mostrarMenuOpciones(it, empleado)
                }
            }
        }
        
        private fun mostrarMenuOpciones(view: View, empleado: Empleado) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_empleado_opciones, popup.menu)
            
            // Configurar texto del menú según estado
            val itemActivar = popup.menu.findItem(R.id.menu_activar_desactivar)
            itemActivar?.title = if (empleado.activo) "Desactivar" else "Activar"
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_ver_detalle -> {
                        onEmpleadoAction(empleado, Accion.VER_DETALLE)
                        true
                    }
                    R.id.menu_editar -> {
                        onEmpleadoAction(empleado, Accion.EDITAR)
                        true
                    }
                    R.id.menu_generar_qr -> {
                        onEmpleadoAction(empleado, Accion.GENERAR_QR)
                        true
                    }
                    R.id.menu_activar_desactivar -> {
                        onEmpleadoAction(empleado, Accion.ACTIVAR_DESACTIVAR)
                        true
                    }
                    R.id.menu_eliminar -> {
                        onEmpleadoAction(empleado, Accion.ELIMINAR)
                        true
                    }
                    else -> false
                }
            }
            
            popup.show()
        }
    }
    
    private class EmpleadoDiffCallback : DiffUtil.ItemCallback<Empleado>() {
        override fun areItemsTheSame(oldItem: Empleado, newItem: Empleado): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Empleado, newItem: Empleado): Boolean {
            return oldItem == newItem
        }
    }
}