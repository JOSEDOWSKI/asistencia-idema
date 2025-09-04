package com.asistencia.app

// Clase simple para empleado compartida entre actividades
data class EmpleadoSimple(
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val horaEntrada: String,
    val horaSalida: String,
    val refrigerioInicio: String = "12:00",
    val refrigerioFin: String = "13:00",
    val esFlexible: Boolean = false,
    val activo: Boolean = true
)