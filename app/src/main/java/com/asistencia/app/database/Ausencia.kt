package com.asistencia.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "ausencias")
data class Ausencia(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val empleadoId: String,
    val fecha: String, // formato "yyyy-MM-dd"
    val tipo: TipoAusencia,
    val motivo: String,
    val descripcion: String? = null,
    val documentoAdjunto: String? = null, // ruta del archivo
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
)

enum class TipoAusencia {
    JUSTIFICACION,
    DESCANSO_MEDICO,
    VACACIONES,
    PERMISO_PERSONAL,
    SUSPENSION,
    OTRO
}
