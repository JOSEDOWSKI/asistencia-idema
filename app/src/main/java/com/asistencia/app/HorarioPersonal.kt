package com.asistencia.app

import com.google.gson.annotations.SerializedName

// Modelo para horario de un día específico
data class HorarioDia(
    val entrada: String = "", // formato "HH:mm"
    val salida: String = "",  // formato "HH:mm"
    val activo: Boolean = true, // si trabaja este día
    
    // Para horarios partidos
    val esHorarioPartido: Boolean = false,
    val salidaDescanso: String = "", // hora de salida para descanso
    val entradaDescanso: String = "", // hora de regreso del descanso
    
    // Refrigerio por día individual
    val tieneRefrigerio: Boolean = false,
    val inicioRefrigerio: String = "12:00",
    val finRefrigerio: String = "13:00",
    val minutosRefrigerio: Int = 60,
    
    // Tolerancia y compensación
    val aplicarCompensacion: Boolean = true // si los minutos de retraso se compensan al final
) {
    // Función para obtener todos los horarios del día
    fun getHorarios(): List<Pair<String, String>> {
        return if (esHorarioPartido && salidaDescanso.isNotEmpty() && entradaDescanso.isNotEmpty()) {
            listOf(
                Pair(entrada, salidaDescanso), // Turno mañana
                Pair(entradaDescanso, salida)  // Turno tarde
            )
        } else {
            listOf(Pair(entrada, salida)) // Turno continuo
        }
    }
    
    // Función para determinar en qué turno está una hora específica
    fun getTurnoParaHora(hora: String): Int {
        if (!esHorarioPartido) return 1
        
        return try {
            val horaActual = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).parse(hora)
            val finTurno1 = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).parse(salidaDescanso)
            val inicioTurno2 = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).parse(entradaDescanso)
            
            when {
                horaActual?.before(finTurno1) == true -> 1 // Turno mañana
                horaActual?.after(inicioTurno2) == true || horaActual?.equals(inicioTurno2) == true -> 2 // Turno tarde
                else -> 0 // En descanso
            }
        } catch (e: Exception) {
            1
        }
    }
    
    // Función para calcular horas trabajadas en el día (considerando refrigerio)
    fun calcularHorasTrabajadas(): Pair<Int, Int> { // Retorna (horas, minutos)
        if (!activo || entrada.isEmpty() || salida.isEmpty()) {
            return Pair(0, 0)
        }
        
        return try {
            val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            var totalMinutos = 0
            
            if (esHorarioPartido && salidaDescanso.isNotEmpty() && entradaDescanso.isNotEmpty()) {
                // Calcular horas del turno mañana
                val entradaTime = formato.parse(entrada)?.time ?: 0
                val salidaDescansoTime = formato.parse(salidaDescanso)?.time ?: 0
                val minutosManana = ((salidaDescansoTime - entradaTime) / (1000 * 60)).toInt()
                
                // Calcular horas del turno tarde
                val entradaDescansoTime = formato.parse(entradaDescanso)?.time ?: 0
                val salidaTime = formato.parse(salida)?.time ?: 0
                val minutosTarde = ((salidaTime - entradaDescansoTime) / (1000 * 60)).toInt()
                
                totalMinutos = minutosManana + minutosTarde
            } else {
                // Jornada continua
                val entradaTime = formato.parse(entrada)?.time ?: 0
                val salidaTime = formato.parse(salida)?.time ?: 0
                totalMinutos = ((salidaTime - entradaTime) / (1000 * 60)).toInt()
            }
            
            // Descontar refrigerio si está configurado para este día
            if (tieneRefrigerio && inicioRefrigerio.isNotEmpty() && finRefrigerio.isNotEmpty()) {
                val inicioRefTime = formato.parse(inicioRefrigerio)?.time ?: 0
                val finRefTime = formato.parse(finRefrigerio)?.time ?: 0
                val minutosRef = ((finRefTime - inicioRefTime) / (1000 * 60)).toInt()
                
                if (minutosRef > 0) {
                    totalMinutos -= minutosRef
                }
            }
            
            if (totalMinutos > 0) {
                Pair(totalMinutos / 60, totalMinutos % 60)
            } else {
                Pair(0, 0)
            }
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }
    
    // Función para obtener descripción de horas trabajadas
    fun getDescripcionHoras(): String {
        val (horas, minutos) = calcularHorasTrabajadas()
        return "${horas}h ${minutos}m"
    }
}

// Modelo para horarios semanales
data class HorarioSemanal(
    val lunes: HorarioDia = HorarioDia(),
    val martes: HorarioDia = HorarioDia(),
    val miercoles: HorarioDia = HorarioDia(),
    val jueves: HorarioDia = HorarioDia(),
    val viernes: HorarioDia = HorarioDia(),
    val sabado: HorarioDia = HorarioDia(),
    val domingo: HorarioDia = HorarioDia(activo = false) // domingo inactivo por defecto
)

// Modelo actualizado de Personal con horarios flexibles
data class Personal(
    val dni: String,
    val nombre: String,
    val tipoHorario: String = "FIJO", // "FIJO" o "VARIABLE"
    
    // Para horario fijo (compatibilidad con versión anterior)
    val horaEntrada: String = "08:00",
    val horaSalida: String = "17:00",
    
    // Refrigerio/Descanso
    val tieneRefrigerio: Boolean = false,
    val inicioRefrigerio: String = "12:00",
    val finRefrigerio: String = "13:00",
    val minutosRefrigerio: Int = 60, // duración en minutos
    
    // Para horario variable
    val horarioSemanal: HorarioSemanal = HorarioSemanal()
) {
    // Función para obtener horario de un día específico
    fun getHorarioDia(diaSemana: String): HorarioDia {
        return if (tipoHorario == "FIJO") {
            HorarioDia(horaEntrada, horaSalida, true)
        } else {
            when (diaSemana.lowercase()) {
                "lunes", "monday" -> horarioSemanal.lunes
                "martes", "tuesday" -> horarioSemanal.martes
                "miércoles", "miercoles", "wednesday" -> horarioSemanal.miercoles
                "jueves", "thursday" -> horarioSemanal.jueves
                "viernes", "friday" -> horarioSemanal.viernes
                "sábado", "sabado", "saturday" -> horarioSemanal.sabado
                "domingo", "sunday" -> horarioSemanal.domingo
                else -> HorarioDia(horaEntrada, horaSalida, true)
            }
        }
    }
    
    // Función para obtener resumen de horarios
    fun getResumenHorarios(): String {
        return if (tipoHorario == "FIJO") {
            "Horario fijo: $horaEntrada - $horaSalida"
        } else {
            val diasActivos = mutableListOf<String>()
            if (horarioSemanal.lunes.activo) diasActivos.add("L")
            if (horarioSemanal.martes.activo) diasActivos.add("M")
            if (horarioSemanal.miercoles.activo) diasActivos.add("X")
            if (horarioSemanal.jueves.activo) diasActivos.add("J")
            if (horarioSemanal.viernes.activo) diasActivos.add("V")
            if (horarioSemanal.sabado.activo) diasActivos.add("S")
            if (horarioSemanal.domingo.activo) diasActivos.add("D")
            
            "Horario variable: ${diasActivos.joinToString("")}"
        }
    }
    
    // Función para calcular horas trabajadas considerando refrigerio
    fun calcularHorasTrabajadasConRefrigerio(): Pair<Int, Int> {
        val (horasBase, minutosBase) = calcularHorasBasicas()
        
        if (!tieneRefrigerio) {
            return Pair(horasBase, minutosBase)
        }
        
        // Descontar tiempo de refrigerio
        val totalMinutosBase = horasBase * 60 + minutosBase
        val totalMinutosTrabajados = totalMinutosBase - minutosRefrigerio
        
        return if (totalMinutosTrabajados > 0) {
            Pair(totalMinutosTrabajados / 60, totalMinutosTrabajados % 60)
        } else {
            Pair(0, 0)
        }
    }
    
    // Función auxiliar para calcular horas básicas (sin descontar refrigerio)
    private fun calcularHorasBasicas(): Pair<Int, Int> {
        return try {
            val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val entradaTime = formato.parse(horaEntrada)?.time ?: 0
            val salidaTime = formato.parse(horaSalida)?.time ?: 0
            val totalMinutos = ((salidaTime - entradaTime) / (1000 * 60)).toInt()
            
            if (totalMinutos > 0) {
                Pair(totalMinutos / 60, totalMinutos % 60)
            } else {
                Pair(0, 0)
            }
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }
    
    // Función para obtener descripción completa de horarios con refrigerio
    fun getDescripcionHorariosCompleta(): String {
        val (horas, minutos) = calcularHorasTrabajadasConRefrigerio()
        val descripcionBase = if (tipoHorario == "FIJO") {
            "$horaEntrada - $horaSalida"
        } else {
            "Variable"
        }
        
        return if (tieneRefrigerio) {
            "$descripcionBase (${horas}h ${minutos}m) - Refrigerio: $inicioRefrigerio-$finRefrigerio"
        } else {
            "$descripcionBase (${horas}h ${minutos}m)"
        }
    }
}