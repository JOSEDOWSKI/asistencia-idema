package com.asistencia.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

data class ReporteItem(
    val nombre: String,
    val dni: String,
    val fecha: String,
    val hora: String,
    val tipo: String,
    val llegadaTarde: Boolean,
    val observaciones: String
)

class ReportesAdapter : RecyclerView.Adapter<ReportesAdapter.ReporteViewHolder>() {

    private var reportesList = listOf<ReporteItem>()

    fun submitList(newReportes: List<ReporteItem>) {
        reportesList = newReportes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReporteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reporte, parent, false)
        return ReporteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
        holder.bind(reportesList[position])
    }

    override fun getItemCount(): Int = reportesList.size

    inner class ReporteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        private val tvDni: TextView = itemView.findViewById(R.id.tvDni)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        private val tvHora: TextView = itemView.findViewById(R.id.tvHora)
        private val tvTipo: TextView = itemView.findViewById(R.id.tvTipo)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        private val tvObservaciones: TextView = itemView.findViewById(R.id.tvObservaciones)

        fun bind(reporte: ReporteItem) {
            tvNombre.text = reporte.nombre
            tvDni.text = "DNI: ${reporte.dni}"
            tvFecha.text = reporte.fecha
            tvHora.text = reporte.hora
            
            // Configurar tipo con emoji y color
            when (reporte.tipo) {
                "ENTRADA" -> {
                    tvTipo.text = "📥 Entrada"
                    tvTipo.setTextColor(ContextCompat.getColor(itemView.context, R.color.success_color))
                }
                "SALIDA" -> {
                    tvTipo.text = "📤 Salida"
                    tvTipo.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_color))
                }
                else -> {
                    tvTipo.text = "📋 ${reporte.tipo}"
                    tvTipo.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                }
            }
            
            // Configurar estado con colores mejorados
            if (reporte.llegadaTarde) {
                tvEstado.text = "⚠️ TARDE"
                tvEstado.setTextColor(Color.WHITE)
                tvEstado.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.error_color))
                tvEstado.setPadding(16, 8, 16, 8)
                
                // Cambiar color de fondo del item para tardanzas
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.error_background))
            } else {
                tvEstado.text = "✅ PUNTUAL"
                tvEstado.setTextColor(Color.WHITE)
                tvEstado.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.success_color))
                tvEstado.setPadding(16, 8, 16, 8)
                
                // Color de fondo normal
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.background_primary))
            }
            
            // Configurar observaciones
            if (reporte.observaciones.isNotEmpty()) {
                tvObservaciones.text = reporte.observaciones
                tvObservaciones.visibility = View.VISIBLE
                tvObservaciones.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            } else {
                tvObservaciones.visibility = View.GONE
            }
            
            // Mejorar contraste de textos
            tvNombre.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            tvDni.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            tvFecha.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            tvHora.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_color))
            
            // Hacer el texto más legible
            tvNombre.textSize = 16f
            tvDni.textSize = 14f
            tvFecha.textSize = 14f
            tvHora.textSize = 15f
            tvTipo.textSize = 14f
            tvEstado.textSize = 12f
            tvObservaciones.textSize = 12f
            
            // Aplicar estilos de fuente
            tvNombre.setTypeface(tvNombre.typeface, android.graphics.Typeface.BOLD)
            tvHora.setTypeface(tvHora.typeface, android.graphics.Typeface.BOLD)
            tvEstado.setTypeface(tvEstado.typeface, android.graphics.Typeface.BOLD)
        }
    }
}