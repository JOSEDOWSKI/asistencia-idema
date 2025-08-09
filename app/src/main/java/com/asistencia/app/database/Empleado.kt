package com.asistencia.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "empleados")
data class Empleado(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val dni: String, // 8 dígitos
    val nombres: String,
    val apellidos: String,
    val area: String? = null,
    val activo: Boolean = true,
    val tipoHorario: TipoHorario = TipoHorario.REGULAR,
    
    // Horario regular
    val horaEntradaRegular: String? = null, // formato "HH:mm"
    val horaSalidaRegular: String? = null,  // formato "HH:mm"
    val refrigerioInicioRegular: String? = null, // formato "HH:mm"
    val refrigerioFinRegular: String? = null,    // formato "HH:mm"
    
    // Horario flexible (JSON por día)
    val horarioFlexibleJson: String? = null, // JSON con horarios por día
    val refrigerioFlexibleJson: String? = null, // JSON con refrigerios por día
    
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
)

enum class TipoHorario {
    REGULAR,
    FLEXIBLE
}