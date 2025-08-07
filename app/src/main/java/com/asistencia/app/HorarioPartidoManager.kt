package com.asistencia.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

// Modelo extendido para registros con compensación
data class RegistroAsistenciaExtendido(
    val dni: String,
    val fecha: String,
    val hora: String,
    val tipo: String, // "ENTRADA_TURNO1", "SALIDA_TURNO1", "ENTRADA_TURNO2", "SALIDA_TURNO2"
    val diaSemana: String,
    val llegadaTarde: Boolean = false,
    val minutosRetraso: Int = 0,
    val minutosCompensacion: Int = 0, // minutos que debe compensar al final
    val turno: Int = 1, // 1 = mañana, 2 = tarde
    val horaEsperada: String = "",
    val esHorarioPartido: Boolean = false
)

class HorarioPartidoManager(private val context: Context) {
    private val asistenciaManager = AsistenciaManager(context)
    private val configuracionManager = ConfiguracionManager(context)
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    fun registrarAsistenciaPartida(dni: String, personal: Personal): RegistroAsistenciaExtendido? {
        val horaActual = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val fechaActual = dateFormat.format(Date())
        val diaSemana = SimpleDateFormat("EEEE", Locale("es", "ES")).format(Date())
        
        val horarioDia = personal.getHorarioDia(diaSemana)
        
        if (!horarioDia.activo) {
            return null // No debe trabajar hoy
        }
        
        // Determinar qué tipo de registro es
        val tipoRegistro = determinarTipoRegistro(horaActual, horarioDia, dni, fechaActual)
        val turno = horarioDia.getTurnoParaHora(horaActual.substring(0, 5))
        
        // Calcular retraso y compensación
        val (llegadaTarde, minutosRetraso, horaEsperada, minutosCompensacion) = 
            calcularRetrasoYCompensacion(horaActual, tipoRegistro, horarioDia, dni, fechaActual)
        
        val registro = RegistroAsistenciaExtendido(
            dni = dni,
            fecha = fechaActual,
            hora = horaActual,
            tipo = tipoRegistro,
            diaSemana = diaSemana.capitalize(),
            llegadaTarde = llegadaTarde,
            minutosRetraso = minutosRetraso,
            minutosCompensacion = minutosCompensacion,
            turno = turno,
            horaEsperada = horaEsperada,
            esHorarioPartido = horarioDia.esHorarioPartido
        )
        
        // Guardar registro básico para compatibilidad
        val registroBasico = com.asistencia.app.RegistroAsistencia(
            dni = dni,
            fecha = fechaActual,
            hora = horaActual,
            tipo = tipoRegistro,
            diaSemana = diaSemana.capitalize(),
            llegadaTarde = llegadaTarde
        )
        
        // Guardar ambos registros
        guardarRegistroExtendido(registro)
        
        return registro
    }
    
    private fun determinarTipoRegistro(hora: String, horario: HorarioDia, dni: String, fecha: String): String {
        val horaLimpia = hora.substring(0, 5)
        val turno = horario.getTurnoParaHora(horaLimpia)
        
        // Obtener último registro del día
        val ultimoRegistro = getUltimoRegistroDelDia(dni, fecha)
        
        return when {
            // Horario partido
            horario.esHorarioPartido -> {
                when (turno) {
                    1 -> { // Turno mañana
                        if (ultimoRegistro?.tipo?.contains("ENTRADA_TURNO1") == true) {
                            "SALIDA_TURNO1"
                        } else {
                            "ENTRADA_TURNO1"
                        }
                    }
                    2 -> { // Turno tarde
                        if (ultimoRegistro?.tipo?.contains("ENTRADA_TURNO2") == true) {
                            "SALIDA_TURNO2"
                        } else {
                            "ENTRADA_TURNO2"
                        }
                    }
                    else -> "ENTRADA_TURNO1" // Por defecto
                }
            }
            // Horario continuo
            else -> {
                if (ultimoRegistro?.tipo == "ENTRADA") "SALIDA" else "ENTRADA"
            }
        }
    }
    
    private fun calcularRetrasoYCompensacion(
        hora: String, 
        tipoRegistro: String, 
        horario: HorarioDia, 
        dni: String, 
        fecha: String
    ): Quadruple<Boolean, Int, String, Int> {
        
        val horaLimpia = hora.substring(0, 5)
        val horaEsperada = when (tipoRegistro) {
            "ENTRADA_TURNO1" -> horario.entrada
            "SALIDA_TURNO1" -> horario.salidaDescanso
            "ENTRADA_TURNO2" -> horario.entradaDescanso
            "SALIDA_TURNO2" -> horario.salida
            "ENTRADA" -> horario.entrada
            "SALIDA" -> horario.salida
            else -> ""
        }
        
        if (horaEsperada.isEmpty()) {
            return Quadruple(false, 0, "", 0)
        }
        
        val minutosRetraso = calcularMinutosDiferencia(horaLimpia, horaEsperada)
        val tolerancia = configuracionManager.toleranciaMinutos
        
        // Para entradas, verificar si es tardanza
        val esEntrada = tipoRegistro.contains("ENTRADA")
        val llegadaTarde = esEntrada && minutosRetraso > tolerancia
        
        // Calcular compensación para salidas
        var minutosCompensacion = 0
        if (tipoRegistro == "SALIDA_TURNO1" || tipoRegistro == "SALIDA_TURNO2" || tipoRegistro == "SALIDA") {
            minutosCompensacion = calcularCompensacionNecesaria(dni, fecha, tipoRegistro, horario)
        }
        
        return Quadruple(llegadaTarde, maxOf(0, minutosRetraso), horaEsperada, minutosCompensacion)
    }
    
    private fun calcularCompensacionNecesaria(dni: String, fecha: String, tipoSalida: String, horario: HorarioDia): Int {
        if (!horario.aplicarCompensacion) return 0
        
        val registrosDelDia = getRegistrosDelDia(dni, fecha)
        var minutosACompensar = 0
        
        when (tipoSalida) {
            "SALIDA_TURNO1" -> {
                // Buscar entrada del turno 1
                val entradaTurno1 = registrosDelDia.find { it.tipo == "ENTRADA_TURNO1" }
                entradaTurno1?.let { entrada ->
                    if (entrada.llegadaTarde) {
                        minutosACompensar = entrada.minutosRetraso
                    }
                }
            }
            "SALIDA_TURNO2" -> {
                // Buscar todas las tardanzas del día
                registrosDelDia.forEach { registro ->
                    if (registro.llegadaTarde) {
                        minutosACompensar += registro.minutosRetraso
                    }
                }
            }
            "SALIDA" -> {
                // Para horario continuo
                val entrada = registrosDelDia.find { it.tipo == "ENTRADA" }
                entrada?.let { 
                    if (it.llegadaTarde) {
                        minutosACompensar = it.minutosRetraso
                    }
                }
            }
        }
        
        return minutosACompensar
    }
    
    private fun calcularMinutosDiferencia(horaActual: String, horaEsperada: String): Int {
        return try {
            val actual = timeFormat.parse(horaActual)
            val esperada = timeFormat.parse(horaEsperada)
            
            if (actual == null || esperada == null) return 0
            
            val diferenciaMs = actual.time - esperada.time
            (diferenciaMs / (1000 * 60)).toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getUltimoRegistroDelDia(dni: String, fecha: String): RegistroAsistenciaExtendido? {
        return getRegistrosDelDia(dni, fecha).maxByOrNull { it.hora }
    }
    
    private fun getRegistrosDelDia(dni: String, fecha: String): List<RegistroAsistenciaExtendido> {
        val registrosJson = context.getSharedPreferences("AsistenciaApp", Context.MODE_PRIVATE)
            .getString("registros_extendidos", "[]")
        val type = object : com.google.gson.reflect.TypeToken<List<RegistroAsistenciaExtendido>>() {}.type
        val todosRegistros: List<RegistroAsistenciaExtendido> = 
            com.google.gson.Gson().fromJson(registrosJson, type) ?: emptyList()
        
        return todosRegistros.filter { it.dni == dni && it.fecha == fecha }
    }
    
    private fun guardarRegistroExtendido(registro: RegistroAsistenciaExtendido) {
        val sharedPreferences = context.getSharedPreferences("AsistenciaApp", Context.MODE_PRIVATE)
        val registrosJson = sharedPreferences.getString("registros_extendidos", "[]")
        val type = object : com.google.gson.reflect.TypeToken<List<RegistroAsistenciaExtendido>>() {}.type
        val registros = com.google.gson.Gson().fromJson<MutableList<RegistroAsistenciaExtendido>>(registrosJson, type) 
            ?: mutableListOf()
        
        registros.add(registro)
        
        val nuevosRegistrosJson = com.google.gson.Gson().toJson(registros)
        sharedPreferences.edit().putString("registros_extendidos", nuevosRegistrosJson).apply()
    }
    
    fun getEstadisticasCompensacion(dni: String): String {
        val fechaActual = dateFormat.format(Date())
        val registrosHoy = getRegistrosDelDia(dni, fechaActual)
        
        val totalRetraso = registrosHoy.filter { it.llegadaTarde }.sumOf { it.minutosRetraso }
        val totalCompensacion = registrosHoy.sumOf { it.minutosCompensacion }
        
        return when {
            totalRetraso == 0 -> "✅ Sin retrasos hoy"
            totalCompensacion >= totalRetraso -> "✅ Tiempo compensado ($totalRetraso min)"
            else -> "⚠️ Debe compensar ${totalRetraso - totalCompensacion} minutos"
        }
    }
}

// Clase auxiliar para retornar 4 valores
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)