package com.asistencia.app

// Clase simple para empleado compartida entre actividades
data class EmpleadoSimple(
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val horaEntrada: String,
    val horaSalida: String,
    val activo: Boolean = true
)