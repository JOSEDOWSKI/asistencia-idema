package com.asistencia.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dispositivo")
data class Dispositivo(
    @PrimaryKey
    val deviceId: String,
    val operadorPinHash: String? = null,
    val skewMs: Long = 0, // Diferencia con el servidor en milisegundos
    val modoOperacion: ModoOperacion = ModoOperacion.AUTOSERVICIO,
    val modoLectura: ModoLectura = ModoLectura.QR,
    val capturaUbicacion: Boolean = false,
    val modoOffline: Boolean = true,
    val apiEndpoint: String? = null,
    val apiToken: String? = null,
    val fechaActualizacion: Long = System.currentTimeMillis()
)

enum class ModoOperacion {
    AUTOSERVICIO,
    KIOSCO
}