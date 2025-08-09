package com.asistencia.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "registros_asistencia",
    foreignKeys = [
        ForeignKey(
            entity = Empleado::class,
            parentColumns = ["id"],
            childColumns = ["empleadoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RegistroAsistencia(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val empleadoId: String,
    val fecha: String, // formato "yyyy-MM-dd"
    val tipoEvento: TipoEvento,
    val timestampDispositivo: Long,
    val timestampServidor: Long? = null,
    val deviceId: String,
    val modoLectura: ModoLectura,
    val rawCode: String,
    val gpsLat: Double? = null,
    val gpsLon: Double? = null,
    val nota: String? = null,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
    val marcasCalculoJson: String? = null, // JSON con cálculos de tardanzas y recuperaciones
    val fechaCreacion: Long = System.currentTimeMillis()
)

enum class TipoEvento {
    ENTRADA_TURNO,
    SALIDA_REFRIGERIO,
    ENTRADA_POST_REFRIGERIO,
    SALIDA_TURNO
}

enum class ModoLectura {
    QR,
    DNI_PDF417,
    CODE128
}

enum class EstadoSync {
    PENDIENTE,
    ENVIADO,
    ERROR
}

// Clase para los cálculos de marcas
data class MarcasCalculo(
    val retrasoRecuperable: Int = 0, // minutos
    val tardanzaNoRecuperable: Int = 0, // minutos
    val tardanzaRefrigerio: Int = 0, // minutos
    val incumplimientoRecuperacion: Int = 0, // minutos
    val salidaMinimaRequerida: String? = null // formato "HH:mm"
)