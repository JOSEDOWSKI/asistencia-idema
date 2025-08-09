package com.asistencia.app.business

import com.asistencia.app.database.*
import com.asistencia.app.utils.HorarioUtils
import com.google.gson.Gson
import java.util.*

class ReglasAsistenciaEngine {
    
    private val gson = Gson()
    private val TOLERANCIA_ENTRADA_MINUTOS = 15
    private val TOLERANCIA_DUPLICADO_MS = 30000L // 30 segundos
    
    data class ResultadoValidacion(
        val esValido: Boolean,
        val mensaje: String,
        val proximoEventoEsperado: TipoEvento?,
        val marcasCalculo: MarcasCalculo? = null
    )
    
    suspend fun validarYCalcularRegistro(
        empleado: Empleado,
        tipoEvento: TipoEvento,
        timestamp: Long,
        registrosDelDia: List<RegistroAsistencia>
    ): ResultadoValidacion {
        
        val fecha = Date(timestamp)
        val horarioDia = HorarioUtils.getHorarioParaDia(empleado, fecha)
            ?: return ResultadoValidacion(false, "No se encontró horario configurado para este empleado", null)
        
        val horaActual = HorarioUtils.formatTimestamp(timestamp)
        
        // Validar secuencia de eventos
        val validacionSecuencia = validarSecuenciaEventos(tipoEvento, registrosDelDia)
        if (!validacionSecuencia.esValido) {
            return validacionSecuencia
        }
        
        // Calcular marcas según el tipo de evento
        val marcasCalculo = when (tipoEvento) {
            TipoEvento.ENTRADA_TURNO -> calcularEntradaTurno(horaActual, horarioDia.horaEntrada)
            TipoEvento.SALIDA_REFRIGERIO -> calcularSalidaRefrigerio(horaActual, horarioDia.refrigerioInicio)
            TipoEvento.ENTRADA_POST_REFRIGERIO -> calcularEntradaPostRefrigerio(horaActual, horarioDia.refrigerioFin)
            TipoEvento.SALIDA_TURNO -> calcularSalidaTurno(horaActual, horarioDia.horaSalida, registrosDelDia)
        }
        
        val proximoEvento = determinarProximoEvento(tipoEvento, horarioDia)
        val mensaje = generarMensajeResultado(tipoEvento, marcasCalculo)
        
        return ResultadoValidacion(true, mensaje, proximoEvento, marcasCalculo)
    }
    
    private fun validarSecuenciaEventos(
        tipoEvento: TipoEvento,
        registrosDelDia: List<RegistroAsistencia>
    ): ResultadoValidacion {
        
        val eventosExistentes = registrosDelDia.map { it.tipoEvento }.toSet()
        
        return when (tipoEvento) {
            TipoEvento.ENTRADA_TURNO -> {
                if (eventosExistentes.contains(TipoEvento.ENTRADA_TURNO)) {
                    ResultadoValidacion(false, "Ya se registró la entrada del turno", TipoEvento.SALIDA_REFRIGERIO)
                } else {
                    ResultadoValidacion(true, "", null)
                }
            }
            TipoEvento.SALIDA_REFRIGERIO -> {
                when {
                    !eventosExistentes.contains(TipoEvento.ENTRADA_TURNO) -> 
                        ResultadoValidacion(false, "Debe registrar primero la entrada del turno", TipoEvento.ENTRADA_TURNO)
                    eventosExistentes.contains(TipoEvento.SALIDA_REFRIGERIO) -> 
                        ResultadoValidacion(false, "Ya se registró la salida a refrigerio", TipoEvento.ENTRADA_POST_REFRIGERIO)
                    else -> ResultadoValidacion(true, "", null)
                }
            }
            TipoEvento.ENTRADA_POST_REFRIGERIO -> {
                when {
                    !eventosExistentes.contains(TipoEvento.SALIDA_REFRIGERIO) -> 
                        ResultadoValidacion(false, "Debe registrar primero la salida a refrigerio", TipoEvento.SALIDA_REFRIGERIO)
                    eventosExistentes.contains(TipoEvento.ENTRADA_POST_REFRIGERIO) -> 
                        ResultadoValidacion(false, "Ya se registró el regreso de refrigerio", TipoEvento.SALIDA_TURNO)
                    else -> ResultadoValidacion(true, "", null)
                }
            }
            TipoEvento.SALIDA_TURNO -> {
                when {
                    !eventosExistentes.contains(TipoEvento.ENTRADA_TURNO) -> 
                        ResultadoValidacion(false, "Debe registrar primero la entrada del turno", TipoEvento.ENTRADA_TURNO)
                    eventosExistentes.contains(TipoEvento.SALIDA_TURNO) -> 
                        ResultadoValidacion(false, "Ya se registró la salida del turno", null)
                    eventosExistentes.contains(TipoEvento.SALIDA_REFRIGERIO) && 
                    !eventosExistentes.contains(TipoEvento.ENTRADA_POST_REFRIGERIO) -> 
                        ResultadoValidacion(false, "Debe registrar primero el regreso de refrigerio", TipoEvento.ENTRADA_POST_REFRIGERIO)
                    else -> ResultadoValidacion(true, "", null)
                }
            }
        }
    }
    
    private fun calcularEntradaTurno(horaActual: String, horaEntradaProgramada: String): MarcasCalculo {
        val minutosActuales = HorarioUtils.timeStringToMinutes(horaActual)
        val minutosProgramados = HorarioUtils.timeStringToMinutes(horaEntradaProgramada)
        val diferencia = minutosActuales - minutosProgramados
        
        return if (diferencia <= 0) {
            // Llegó temprano o puntual
            MarcasCalculo()
        } else if (diferencia <= TOLERANCIA_ENTRADA_MINUTOS) {
            // Llegó dentro de la tolerancia - debe recuperar
            val salidaMinima = HorarioUtils.addMinutesToTime(horaEntradaProgramada, diferencia)
            MarcasCalculo(
                retrasoRecuperable = diferencia,
                salidaMinimaRequerida = salidaMinima
            )
        } else {
            // Tardanza no recuperable
            MarcasCalculo(tardanzaNoRecuperable = diferencia)
        }
    }
    
    private fun calcularSalidaRefrigerio(horaActual: String, refrigerioInicio: String?): MarcasCalculo {
        // La salida a refrigerio no tiene penalizaciones, solo se registra
        return MarcasCalculo()
    }
    
    private fun calcularEntradaPostRefrigerio(horaActual: String, refrigerioFin: String?): MarcasCalculo {
        if (refrigerioFin == null) return MarcasCalculo()
        
        val minutosActuales = HorarioUtils.timeStringToMinutes(horaActual)
        val minutosFinRefrigerio = HorarioUtils.timeStringToMinutes(refrigerioFin)
        val diferencia = minutosActuales - minutosFinRefrigerio
        
        return if (diferencia > 0) {
            // Tardanza en regreso de refrigerio (no recuperable)
            MarcasCalculo(tardanzaRefrigerio = diferencia)
        } else {
            MarcasCalculo()
        }
    }
    
    private fun calcularSalidaTurno(
        horaActual: String, 
        horaSalidaProgramada: String,
        registrosDelDia: List<RegistroAsistencia>
    ): MarcasCalculo {
        
        // Buscar si hay retraso recuperable de la entrada
        val entradaTurno = registrosDelDia.find { it.tipoEvento == TipoEvento.ENTRADA_TURNO }
        val marcasEntrada = entradaTurno?.let { 
            gson.fromJson(it.marcasCalculoJson, MarcasCalculo::class.java) 
        }
        
        val retrasoRecuperable = marcasEntrada?.retrasoRecuperable ?: 0
        
        if (retrasoRecuperable > 0) {
            val horaSalidaMinima = HorarioUtils.addMinutesToTime(horaSalidaProgramada, retrasoRecuperable)
            val minutosActuales = HorarioUtils.timeStringToMinutes(horaActual)
            val minutosMinimos = HorarioUtils.timeStringToMinutes(horaSalidaMinima)
            
            if (minutosActuales < minutosMinimos) {
                val incumplimiento = minutosMinimos - minutosActuales
                return MarcasCalculo(
                    incumplimientoRecuperacion = incumplimiento,
                    salidaMinimaRequerida = horaSalidaMinima
                )
            }
        }
        
        return MarcasCalculo()
    }
    
    private fun determinarProximoEvento(tipoEvento: TipoEvento, horarioDia: com.asistencia.app.utils.HorarioDia): TipoEvento? {
        return when (tipoEvento) {
            TipoEvento.ENTRADA_TURNO -> {
                if (horarioDia.refrigerioInicio != null) TipoEvento.SALIDA_REFRIGERIO
                else TipoEvento.SALIDA_TURNO
            }
            TipoEvento.SALIDA_REFRIGERIO -> TipoEvento.ENTRADA_POST_REFRIGERIO
            TipoEvento.ENTRADA_POST_REFRIGERIO -> TipoEvento.SALIDA_TURNO
            TipoEvento.SALIDA_TURNO -> null
        }
    }
    
    private fun generarMensajeResultado(tipoEvento: TipoEvento, marcas: MarcasCalculo): String {
        return when (tipoEvento) {
            TipoEvento.ENTRADA_TURNO -> {
                when {
                    marcas.retrasoRecuperable > 0 -> 
                        "Entrada registrada. Debe recuperar ${marcas.retrasoRecuperable} minutos al salir."
                    marcas.tardanzaNoRecuperable > 0 -> 
                        "Entrada registrada. Tardanza de ${marcas.tardanzaNoRecuperable} minutos (no recuperable)."
                    else -> "Entrada registrada correctamente."
                }
            }
            TipoEvento.SALIDA_REFRIGERIO -> "Salida a refrigerio registrada."
            TipoEvento.ENTRADA_POST_REFRIGERIO -> {
                if (marcas.tardanzaRefrigerio > 0) {
                    "Regreso de refrigerio registrado. Tardanza de ${marcas.tardanzaRefrigerio} minutos."
                } else {
                    "Regreso de refrigerio registrado correctamente."
                }
            }
            TipoEvento.SALIDA_TURNO -> {
                if (marcas.incumplimientoRecuperacion > 0) {
                    "Salida registrada. Incumplimiento de recuperación: ${marcas.incumplimientoRecuperacion} minutos."
                } else {
                    "Salida registrada correctamente."
                }
            }
        }
    }
    
    fun calcularHorasTrabajadas(registrosDelDia: List<RegistroAsistencia>): Int {
        val entrada = registrosDelDia.find { it.tipoEvento == TipoEvento.ENTRADA_TURNO }
        val salida = registrosDelDia.find { it.tipoEvento == TipoEvento.SALIDA_TURNO }
        val salidaRefrigerio = registrosDelDia.find { it.tipoEvento == TipoEvento.SALIDA_REFRIGERIO }
        val entradaRefrigerio = registrosDelDia.find { it.tipoEvento == TipoEvento.ENTRADA_POST_REFRIGERIO }
        
        if (entrada == null || salida == null) return 0
        
        val horaEntrada = HorarioUtils.formatTimestamp(entrada.timestampDispositivo)
        val horaSalida = HorarioUtils.formatTimestamp(salida.timestampDispositivo)
        
        var minutosTotal = HorarioUtils.calculateTimeDifferenceInMinutes(horaEntrada, horaSalida)
        
        // Restar tiempo de refrigerio si existe
        if (salidaRefrigerio != null && entradaRefrigerio != null) {
            val horaSalidaRefr = HorarioUtils.formatTimestamp(salidaRefrigerio.timestampDispositivo)
            val horaEntradaRefr = HorarioUtils.formatTimestamp(entradaRefrigerio.timestampDispositivo)
            val minutosRefrigerio = HorarioUtils.calculateTimeDifferenceInMinutes(horaSalidaRefr, horaEntradaRefr)
            minutosTotal -= minutosRefrigerio
        }
        
        return maxOf(0, minutosTotal)
    }
}