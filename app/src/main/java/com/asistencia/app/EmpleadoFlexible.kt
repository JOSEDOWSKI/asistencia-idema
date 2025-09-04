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
    val refrigeriosSemanales: Map<String, Pair<String, String>>, // Código día -> (inicio_refrigerio, fin_refrigerio)
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
    
    // Función para obtener horario de refrigerio de un día específico
    fun getRefrigerioDia(codigoDia: String): Pair<String, String>? {
        return if (diasActivos.contains(codigoDia)) {
            refrigeriosSemanales[codigoDia]
        } else {
            null
        }
    }
    
    // Función para obtener horario del día actual
    fun getHorarioHoy(): Pair<String, String>? {
        val codigoDiaHoy = getDiaActualCodigo()
        return getHorarioDia(codigoDiaHoy)
    }
    
    // Función para obtener horario de refrigerio del día actual
    fun getRefrigerioHoy(): Pair<String, String>? {
        val codigoDiaHoy = getDiaActualCodigo()
        val refrigerioDia = getRefrigerioDia(codigoDiaHoy)
        
        // Si no hay refrigerio configurado para este día, retornar null
        // El sistema usará los valores por defecto del EmpleadoSimple
        return refrigerioDia
    }
    
    // Función para verificar si tiene refrigerio configurado para hoy
    fun tieneRefrigerioHoy(): Boolean {
        val codigoDiaHoy = getDiaActualCodigo()
        return refrigeriosSemanales.containsKey(codigoDiaHoy) && 
               refrigeriosSemanales[codigoDiaHoy] != null
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
    
    // Función para obtener resumen de refrigerios (para compatibilidad)
    fun getRefrigerioResumen(): Pair<String, String> {
        if (refrigeriosSemanales.isEmpty()) {
            return Pair("12:00", "13:00")
        }
        
        // Obtener el primer refrigerio disponible como referencia
        val primerRefrigerio = refrigeriosSemanales.values.first()
        return primerRefrigerio
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
    
    // Función para obtener descripción completa de refrigerios
    fun getDescripcionRefrigerios(): String {
        if (diasActivos.isEmpty()) {
            return "Sin refrigerios configurados"
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
        
        // Agrupar días con el mismo refrigerio
        val gruposRefrigerios = mutableMapOf<Pair<String, String>, MutableList<String>>()
        
        diasActivos.forEach { codigo ->
            val refrigerio = refrigeriosSemanales[codigo]
            if (refrigerio != null) {
                if (!gruposRefrigerios.containsKey(refrigerio)) {
                    gruposRefrigerios[refrigerio] = mutableListOf()
                }
                gruposRefrigerios[refrigerio]?.add(diasNombres[codigo] ?: codigo)
            }
        }
        
        // Generar descripción
        return if (gruposRefrigerios.size == 1) {
            val refrigerio = gruposRefrigerios.keys.first()
            val dias = gruposRefrigerios[refrigerio]?.joinToString("") ?: ""
            "$dias: ${refrigerio.first}-${refrigerio.second}"
        } else {
            gruposRefrigerios.map { (refrigerio, dias) ->
                "${dias.joinToString("")}: ${refrigerio.first}-${refrigerio.second}"
            }.joinToString(" | ")
        }
    }
    
    // Función para calcular horas semanales totales (EXCLUYENDO REFRIGERIOS)
    fun calcularHorasSemanales(): Pair<Int, Int> {
        var totalMinutos = 0
        
        diasActivos.forEach { codigo ->
            val horario = horariosSemanales[codigo]
            val refrigerio = refrigeriosSemanales[codigo]
            
            if (horario != null) {
                val minutosDelDia = calcularMinutosDia(horario.first, horario.second)
                
                // Restar tiempo de refrigerio si está configurado
                if (refrigerio != null) {
                    val minutosRefrigerio = calcularMinutosDia(refrigerio.first, refrigerio.second)
                    totalMinutos += (minutosDelDia - minutosRefrigerio)
                } else {
                    totalMinutos += minutosDelDia
                }
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
        val refrigerioHoy = getRefrigerioHoy()
        val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        return try {
            val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
            val actual = formato.parse(horaActual)
            val entrada = formato.parse(horarioHoy.first)
            val salida = formato.parse(horarioHoy.second)
            
            if (actual != null && entrada != null && salida != null) {
                val estaEnHorarioLaboral = !actual.before(entrada) && !actual.after(salida)
                
                // Si está en horario laboral, verificar que no esté en refrigerio
                if (estaEnHorarioLaboral && refrigerioHoy != null) {
                    val inicioRefrigerio = formato.parse(refrigerioHoy.first)
                    val finRefrigerio = formato.parse(refrigerioHoy.second)
                    
                    if (inicioRefrigerio != null && finRefrigerio != null) {
                        // Si está en horario de refrigerio, no está trabajando
                        val estaEnRefrigerio = !actual.before(inicioRefrigerio) && !actual.after(finRefrigerio)
                        return !estaEnRefrigerio
                    }
                }
                
                return estaEnHorarioLaboral
            }
            false
        } catch (e: Exception) {
            false
        }
    }
    
    // Función para verificar si está en horario de refrigerio
    fun estaEnRefrigerio(): Boolean {
        if (!trabajaHoy()) return false
        
        val refrigerioHoy = getRefrigerioHoy() ?: return false
        val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        return try {
            val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
            val actual = formato.parse(horaActual)
            val inicioRefrigerio = formato.parse(refrigerioHoy.first)
            val finRefrigerio = formato.parse(refrigerioHoy.second)
            
            actual != null && inicioRefrigerio != null && finRefrigerio != null &&
            !actual.before(inicioRefrigerio) && !actual.after(finRefrigerio)
        } catch (e: Exception) {
            false
        }
    }
    
    // Función para obtener estado actual del empleado
    fun getEstadoActual(): String {
        return when {
            !trabajaHoy() -> "🏠 No trabaja hoy"
            estaEnRefrigerio() -> "🍽️ En horario de refrigerio"
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
    
    // Función para obtener información completa del día actual
    fun getInfoDiaActual(): String {
        if (!trabajaHoy()) {
            return "🏠 No trabaja hoy"
        }
        
        val horarioHoy = getHorarioHoy()
        val refrigerioHoy = getRefrigerioHoy()
        
        return buildString {
            append("📅 Trabaja hoy\n")
            if (horarioHoy != null) {
                append("⏰ Horario: ${horarioHoy.first} - ${horarioHoy.second}\n")
            }
            if (refrigerioHoy != null) {
                append("🍽️ Refrigerio: ${refrigerioHoy.first} - ${refrigerioHoy.second}\n")
            }
            append("📊 Estado: ${getEstadoActual()}")
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
        val refrigerioResumen = getRefrigerioResumen()
        return EmpleadoSimple(
            dni = dni,
            nombres = nombres,
            apellidos = apellidos,
            horaEntrada = horarioResumen.first,
            horaSalida = horarioResumen.second,
            refrigerioInicio = refrigerioResumen.first,
            refrigerioFin = refrigerioResumen.second,
            esFlexible = true
        )
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