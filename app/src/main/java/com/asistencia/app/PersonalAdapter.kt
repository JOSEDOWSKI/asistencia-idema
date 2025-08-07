package com.asistencia.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PersonalAdapter(
    private val onItemClick: (Personal) -> Unit
) : RecyclerView.Adapter<PersonalAdapter.PersonalViewHolder>() {

    private var personalList = listOf<Personal>()

    fun submitList(newPersonal: List<Personal>) {
        personalList = newPersonal
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_personal, parent, false)
        return PersonalViewHolder(view)
    }

    override fun onBindViewHolder(holder: PersonalViewHolder, position: Int) {
        holder.bind(personalList[position])
    }

    override fun getItemCount(): Int = personalList.size

    inner class PersonalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        private val tvDni: TextView = itemView.findViewById(R.id.tvDni)
        private val tvHorario: TextView = itemView.findViewById(R.id.tvHorario)

        fun bind(personal: Personal) {
            tvNombre.text = personal.nombre
            tvDni.text = "DNI: ${personal.dni}"
            
            // Mostrar información completa con refrigerio
            try {
                val horarioBase = if (personal.tipoHorario == "FIJO") {
                    "${personal.horaEntrada} - ${personal.horaSalida}"
                } else {
                    "Variable"
                }
                
                val (horas, minutos) = personal.calcularHorasTrabajadasConRefrigerio()
                val horasTexto = "${horas}h ${minutos}m"
                
                tvHorario.text = if (personal.tieneRefrigerio) {
                    "$horarioBase ($horasTexto) 🍽️"
                } else {
                    "$horarioBase ($horasTexto)"
                }
            } catch (e: Exception) {
                tvHorario.text = "Horario: ${personal.horaEntrada} - ${personal.horaSalida}"
            }
            
            itemView.setOnClickListener {
                onItemClick(personal)
            }
        }
    }
}