package com.asistencia.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

data class ResumenHorasTrabajadas(
    val dni: String,
    val nombre: String,
    val fecha: String,
    val horasTeoricas: Pair<Int, Int>, // (horas, minutos) que debería trabajar
    val horasReales: Pair<Int, Int>,   // (horas, minutos) que realmente trabajó
    val diferencia: Pair<Int, Int>,    // diferencia entre real y teórico
    val registrosCompletos: Boolean,   // si tiene entrada y salida
    val observaciones: String
)

class HorasTrabajadasManager(private val context: Context) {
    private val asistenciaManager = AsistenciaManager(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    fun calcularHorasTrabajadasDia(personal: Personal, fecha: String): ResumenHorasTrabajadas {
        val registros = asistenciaManager.getRegistrosAsistencia()
            .filter { it.dni == personal.dni && it.fecha == fecha }
            .sortedBy { it.hora }
        
        val diaSemana = obtenerDiaSemana(fecha)
        val horarioDia = personal.getHorarioDia(diaSemana)
        
        // Calcular horas teóricas considerando refrigerio
        val horasTeoricas = if (personal.tipoHorario == "FIJO") {
            // Para horario fijo, usar el cálculo con refrigerio
            personal.calcularHorasTrabajadasConRefrigerio()
        } else {
            // Para horario variable, usar el cálculo del día específico
            horarioDia.calcularHorasTrabajadas()
        }
        
        // Calcular horas reales trabajadas
        val (horasReales, registrosCompletos, observaciones) = calcularHorasReales(registros, horarioDia)
        
        // Calcular diferencia
        val totalMinutosTeoricas = horasTeoricas.first * 60 + horasTeoricas.second
        val totalMinutosReales = horasReales.first * 60 + horasReales.second
        val diferenciaMinutos = totalMinutosReales - totalMinutosTeoricas
        val diferencia = Pair(diferenciaMinutos / 60, Math.abs(diferenciaMinutos % 60))
        
        return ResumenHorasTrabajadas(
            dni = personal.dni,
            nombre = personal.nombre,
            fecha = fecha,
            horasTeoricas = horasTeoricas,
            horasReales = horasReales,
            diferencia = diferencia,
            registrosCompletos = registrosCompletos,
            observaciones = observaciones
        )
    }
    
    private fun calcularHorasReales(
        registros: List<RegistroAsistencia>, 
        horarioDia: HorarioDia
    ): Triple<Pair<Int, Int>, Boolean, String> {
        
        if (registros.isEmpty()) {
            return Triple(Pair(0, 0), false, "Sin registros")
        }
        
        return if (horarioDia.esHorarioPartido) {
            calcularHorasHorarioPartido(registros)
        } else {
            calcularHorasHorarioContinuo(registros)
        }
    }
    
    private fun calcularHorasHorarioContinuo(registros: List<RegistroAsistencia>): Triple<Pair<Int, Int>, Boolean, String> {
        val entradas = registros.filter { it.tipo.contains("ENTRADA") }
        val salidas = registros.filter { it.tipo.contains("SALIDA") }
        
        if (entradas.isEmpty()) {
            return Triple(Pair(0, 0), false, "Sin entrada registrada")
        }
        
        if (salidas.isEmpty()) {
            return Triple(Pair(0, 0), false, "Sin salida registrada")
        }
        
        try {
            val entrada = timeFormat.parse(entradas.first().hora.substring(0, 5))
            val salida = timeFormat.parse(salidas.last().hora.substring(0, 5))
            
            if (entrada != null && salida != null) {
                val diferenciaMs = salida.time - entrada.time
                val totalMinutos = (diferenciaMs / (1000 * 60)).toInt()
                
                if (totalMinutos < 0) {
                    return Triple(Pair(0, 0), false, "Horario inválido")
                }
                
                val horas = totalMinutos / 60
                val minutos = totalMinutos % 60
                
                return Triple(Pair(horas, minutos), true, "Jornada completa")
            }
        } catch (e: Exception) {
            return Triple(Pair(0, 0), false, "Error al calcular horas")
        }
        
        return Triple(Pair(0, 0), false, "Error en registros")
    }
    
    private fun calcularHorasHorarioPartido(registros: List<RegistroAsistencia>): Triple<Pair<Int, Int>, Boolean, String> {
        val entradasTurno1 = registros.filter { it.tipo == "ENTRADA_TURNO1" }
        val salidasTurno1 = registros.filter { it.tipo == "SALIDA_TURNO1" }
        val entradasTurno2 = registros.filter { it.tipo == "ENTRADA_TURNO2" }
        val salidasTurno2 = registros.filter { it.tipo == "SALIDA_TURNO2" }
        
        var totalMinutos = 0
        var observaciones = mutableListOf<String>()
        var registrosCompletos = true
        
        // Calcular turno mañana
        if (entradasTurno1.isNotEmpty() && salidasTurno1.isNotEmpty()) {
            try {
                val entrada = timeFormat.parse(entradasTurno1.first().hora.substring(0, 5))
                val salida = timeFormat.parse(salidasTurno1.first().hora.substring(0, 5))
                
                if (entrada != null && salida != null) {
                    val minutosManana = ((salida.time - entrada.time) / (1000 * 60)).toInt()
                    if (minutosManana > 0) {
                        totalMinutos += minutosManana
                        observaciones.add("Turno mañana: ${minutosManana / 60}h ${minutosManana % 60}m")
                    }
                }
            } catch (e: Exception) {
                observaciones.add("Error en turno mañana")
                registrosCompletos = false
            }
        } else {
            observaciones.add("Turno mañana incompleto")
            registrosCompletos = false
        }
        
        // Calcular turno tarde
        if (entradasTurno2.isNotEmpty() && salidasTurno2.isNotEmpty()) {
            try {
                val entrada = timeFormat.parse(entradasTurno2.first().hora.substring(0, 5))
                val salida = timeFormat.parse(salidasTurno2.first().hora.substring(0, 5))
                
                if (entrada != null && salida != null) {
                    val minutosTarde = ((salida.time - entrada.time) / (1000 * 60)).toInt()
                    if (minutosTarde > 0) {
                        totalMinutos += minutosTarde
                        observaciones.add("Turno tarde: ${minutosTarde / 60}h ${minutosTarde % 60}m")
                    }
                }
            } catch (e: Exception) {
                observaciones.add("Error en turno tarde")
                registrosCompletos = false
            }
        } else {
            observaciones.add("Turno tarde incompleto")
            registrosCompletos = false
        }
        
        val horas = totalMinutos / 60
        val minutos = totalMinutos % 60
        
        return Triple(
            Pair(horas, minutos), 
            registrosCompletos, 
            observaciones.joinToString("; ")
        )
    }
    
    private fun obtenerDiaSemana(fecha: String): String {
        return try {
            val date = dateFormat.parse(fecha)
            val dayFormat = SimpleDateFormat("EEEE", Locale("es", "ES"))
            dayFormat.format(date ?: Date()).lowercase()
        } catch (e: Exception) {
            "lunes" // Por defecto
        }
    }
    
    fun calcularHorasSemanales(personal: Personal, fechaInicio: String, fechaFin: String): List<ResumenHorasTrabajadas> {
        val resultados = mutableListOf<ResumenHorasTrabajadas>()
        
        try {
            val inicio = dateFormat.parse(fechaInicio)
            val fin = dateFormat.parse(fechaFin)
            
            if (inicio != null && fin != null) {
                val calendar = Calendar.getInstance()
                calendar.time = inicio
                
                while (calendar.time <= fin) {
                    val fechaActual = dateFormat.format(calendar.time)
                    val resumen = calcularHorasTrabajadasDia(personal, fechaActual)
                    resultados.add(resumen)
                    
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        } catch (e: Exception) {
            // Error al procesar fechas
        }
        
        return resultados
    }
    
    fun generarResumenSemanal(resumenes: List<ResumenHorasTrabajadas>): String {
        if (resumenes.isEmpty()) return "Sin datos"
        
        val totalHorasTeoricas = resumenes.sumOf { it.horasTeoricas.first * 60 + it.horasTeoricas.second }
        val totalHorasReales = resumenes.sumOf { it.horasReales.first * 60 + it.horasReales.second }
        val diasCompletos = resumenes.count { it.registrosCompletos }
        
        val horasTeoricas = totalHorasTeoricas / 60
        val minutosTeoricas = totalHorasTeoricas % 60
        val horasReales = totalHorasReales / 60
        val minutosReales = totalHorasReales % 60
        
        val diferencia = totalHorasReales - totalHorasTeoricas
        val horasDiferencia = Math.abs(diferencia) / 60
        val minutosDiferencia = Math.abs(diferencia) % 60
        val signo = if (diferencia >= 0) "+" else "-"
        
        return """
            📊 RESUMEN SEMANAL
            
            👤 ${resumenes.first().nombre}
            📅 ${resumenes.first().fecha} - ${resumenes.last().fecha}
            
            ⏰ Horas teóricas: ${horasTeoricas}h ${minutosTeoricas}m
            ⏱️ Horas reales: ${horasReales}h ${minutosReales}m
            📈 Diferencia: $signo${horasDiferencia}h ${minutosDiferencia}m
            
            ✅ Días completos: $diasCompletos/${resumenes.size}
            
            ${if (diferencia > 0) "🎉 Horas extras trabajadas" else if (diferencia < 0) "⚠️ Horas faltantes" else "✅ Horas exactas"}
        """.trimIndent()
    }
}