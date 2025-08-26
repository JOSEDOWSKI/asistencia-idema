package com.asistencia.app

import java.text.SimpleDateFormat
import java.util.*

// Modelo para empleado con horario flexible
data class EmpleadoFlexible(
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val tipoHorario: String = "FLEXIBLE",
    val horariosSemanales: Map<String, Pair<String, String>>, // Código día -> (entrada, salida)
    val diasActivos: List<String>, // Lista de códigos de días activos
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    
    // Función para obtener horario de un día específico
    fun getHorarioDia(codigoDia: String): Pair<String, String>? {
        return if (diasActivos.contains(codigoDia)) {
            horariosSemanales[codigoDia]
        } else {
            null
        }
    }
    
    // Función para obtener horario del día actual
    fun getHorarioHoy(): Pair<String, String>? {
        val codigoDiaHoy = getDiaActualCodigo()
        return getHorarioDia(codigoDiaHoy)
    }
    
    // Función para verificar si trabaja hoy
    fun trabajaHoy(): Boolean {
        val codigoDiaHoy = getDiaActualCodigo()
        return diasActivos.contains(codigoDiaHoy)
    }
    
    // Función para obtener resumen de horarios (para compatibilidad)
    fun getHorarioResumen(): Pair<String, String> {
        if (horariosSemanales.isEmpty()) {
            return Pair("08:00", "17:00")
        }
        
        // Obtener el primer horario disponible como referencia
        val primerHorario = horariosSemanales.values.first()
        return primerHorario
    }
    
    // Función para obtener descripción completa de horarios
    fun getDescripcionHorarios(): String {
        if (diasActivos.isEmpty()) {
            return "Sin horarios configurados"
        }
        
        val diasNombres = mapOf(
            "L" to "Lun",
            "M" to "Mar", 
            "X" to "Mié",
            "J" to "Jue",
            "V" to "Vie",
            "S" to "Sáb",
            "D" to "Dom"
        )
        
        // Agrupar días con el mismo horario
        val gruposHorarios = mutableMapOf<Pair<String, String>, MutableList<String>>()
        
        diasActivos.forEach { codigo ->
            val horario = horariosSemanales[codigo]
            if (horario != null) {
                if (!gruposHorarios.containsKey(horario)) {
                    gruposHorarios[horario] = mutableListOf()
                }
                gruposHorarios[horario]?.add(diasNombres[codigo] ?: codigo)
            }
        }
        
        // Generar descripción
        return if (gruposHorarios.size == 1) {
            val horario = gruposHorarios.keys.first()
            val dias = gruposHorarios[horario]?.joinToString("") ?: ""
            "$dias: ${horario.first}-${horario.second}"
        } else {
            gruposHorarios.map { (horario, dias) ->
                "${dias.joinToString("")}: ${horario.first}-${horario.second}"
            }.joinToString(" | ")
        }
    }
    
    // Función para calcular horas semanales totales
    fun calcularHorasSemanales(): Pair<Int, Int> {
        var totalMinutos = 0
        
        diasActivos.forEach { codigo ->
            val horario = horariosSemanales[codigo]
            if (horario != null) {
                val minutosDelDia = calcularMinutosDia(horario.first, horario.second)
                totalMinutos += minutosDelDia
            }
        }
        
        return Pair(totalMinutos / 60, totalMinutos % 60)
    }
    
    // Función para obtener el próximo día de trabajo
    fun getProximoDiaTrabajo(): String? {
        val hoy = getDiaActualCodigo()
        val ordenDias = listOf("L", "M", "X", "J", "V", "S", "D")
        val indiceHoy = ordenDias.indexOf(hoy)
        
        // Buscar desde mañana
        for (i in 1..7) {
            val indiceProximo = (indiceHoy + i) % 7
            val codigoProximo = ordenDias[indiceProximo]
            if (diasActivos.contains(codigoProximo)) {
                return codigoProximo
            }
        }
        
        return null
    }
    
    // Función para verificar si está en horario de trabajo ahora
    fun estaEnHorarioTrabajo(): Boolean {
        if (!trabajaHoy()) return false
        
        val horarioHoy = getHorarioHoy() ?: return false
        val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        return try {
            val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
            val actual = formato.parse(horaActual)
            val entrada = formato.parse(horarioHoy.first)
            val salida = formato.parse(horarioHoy.second)
            
            actual != null && entrada != null && salida != null &&
            !actual.before(entrada) && !actual.after(salida)
        } catch (e: Exception) {
            false
        }
    }
    
    // Función para obtener estado actual del empleado
    fun getEstadoActual(): String {
        return when {
            !trabajaHoy() -> "🏠 No trabaja hoy"
            estaEnHorarioTrabajo() -> "✅ En horario de trabajo"
            else -> {
                val horarioHoy = getHorarioHoy()
                if (horarioHoy != null) {
                    val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    if (horaActual < horarioHoy.first) {
                        "⏰ Antes del horario (${horarioHoy.first})"
                    } else {
                        "🏠 Fuera del horario"
                    }
                } else {
                    "❓ Sin horario definido"
                }
            }
        }
    }
    
    // Funciones auxiliares privadas
    private fun getDiaActualCodigo(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "L"
            Calendar.TUESDAY -> "M"
            Calendar.WEDNESDAY -> "X"
            Calendar.THURSDAY -> "J"
            Calendar.FRIDAY -> "V"
            Calendar.SATURDAY -> "S"
            Calendar.SUNDAY -> "D"
            else -> "L"
        }
    }
    
    private fun calcularMinutosDia(entrada: String, salida: String): Int {
        return try {
            val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
            val horaEntrada = formato.parse(entrada)
            val horaSalida = formato.parse(salida)
            
            if (horaEntrada != null && horaSalida != null) {
                val diferencia = horaSalida.time - horaEntrada.time
                (diferencia / (1000 * 60)).toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    // Función para convertir a EmpleadoSimple (compatibilidad)
    fun toEmpleadoSimple(): EmpleadoSimple {
        val horarioResumen = getHorarioResumen()
        return EmpleadoSimple(
            dni = dni,
            nombres = nombres,
            apellidos = apellidos,
            horaEntrada = horarioResumen.first,
            horaSalida = horarioResumen.second,
            activo = activo
        )
    }
    
    // Función para obtener información detallada
    fun getInformacionDetallada(): String {
        val (horas, minutos) = calcularHorasSemanales()
        val diasTrabajo = diasActivos.size
        
        return buildString {
            append("👤 $nombres $apellidos\n")
            append("🆔 DNI: $dni\n")
            append("📅 Días de trabajo: $diasTrabajo días/semana\n")
            append("⏱️ Horas semanales: ${horas}h ${minutos}m\n")
            append("📋 Horarios: ${getDescripcionHorarios()}")
        }
    }
}

// Extensión para obtener nombre completo del día
fun String.getNombreDiaCompleto(): String {
    return when (this) {
        "L" -> "Lunes"
        "M" -> "Martes"
        "X" -> "Miércoles"
        "J" -> "Jueves"
        "V" -> "Viernes"
        "S" -> "Sábado"
        "D" -> "Domingo"
        else -> this
    }
}

// Extensión para obtener emoji del día
fun String.getEmojiDia(): String {
    return when (this) {
        "L" -> "🌅"
        "M" -> "💼"
        "X" -> "⚡"
        "J" -> "🚀"
        "V" -> "🎉"
        "S" -> "🌞"
        "D" -> "🏠"
        else -> "📅"
    }
}