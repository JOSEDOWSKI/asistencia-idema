package com.asistencia.app

import java.text.SimpleDateFormat
import java.util.*

data class CambioPendiente(
    val empleadoDni: String,
    val tipoEmpleado: String, // "SIMPLE" o "FLEXIBLE"
    val tipoCambio: String, // "INFORMACION", "HORARIO", "ESTADO"
    val datosAnteriores: Map<String, Any>,
    val datosNuevos: Map<String, Any>,
    val fechaCreacion: String,
    val fechaAplicacion: String,
    val aplicado: Boolean = false,
    val creadoPor: String = "sistema"
) {
    
    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        fun crearCambioInformacion(
            empleadoDni: String,
            tipoEmpleado: String,
            nombresAnterior: String,
            apellidosAnterior: String,
            nombresNuevo: String,
            apellidosNuevo: String
        ): CambioPendiente {
            val ahora = Date()
            val manana = Calendar.getInstance().apply {
                time = ahora
                add(Calendar.DAY_OF_MONTH, 1)
            }.time
            
            return CambioPendiente(
                empleadoDni = empleadoDni,
                tipoEmpleado = tipoEmpleado,
                tipoCambio = "INFORMACION",
                datosAnteriores = mapOf(
                    "nombres" to nombresAnterior,
                    "apellidos" to apellidosAnterior
                ),
                datosNuevos = mapOf(
                    "nombres" to nombresNuevo,
                    "apellidos" to apellidosNuevo
                ),
                fechaCreacion = dateTimeFormat.format(ahora),
                fechaAplicacion = dateFormat.format(manana)
            )
        }
        
        fun crearCambioHorarioSimple(
            empleadoDni: String,
            tipoEmpleado: String,
            entradaAnterior: String,
            salidaAnterior: String,
            entradaNueva: String,
            salidaNueva: String
        ): CambioPendiente {
            val ahora = Date()
            val manana = Calendar.getInstance().apply {
                time = ahora
                add(Calendar.DAY_OF_MONTH, 1)
            }.time
            
            return CambioPendiente(
                empleadoDni = empleadoDni,
                tipoEmpleado = tipoEmpleado,
                tipoCambio = "HORARIO",
                datosAnteriores = mapOf(
                    "horaEntrada" to entradaAnterior,
                    "horaSalida" to salidaAnterior
                ),
                datosNuevos = mapOf(
                    "horaEntrada" to entradaNueva,
                    "horaSalida" to salidaNueva
                ),
                fechaCreacion = dateTimeFormat.format(ahora),
                fechaAplicacion = dateFormat.format(manana)
            )
        }
        
        fun crearCambioHorarioFlexible(
            empleadoDni: String,
            tipoEmpleado: String,
            horariosAnteriores: Map<String, Pair<String, String>>,
            horariosNuevos: Map<String, Pair<String, String>>,
            diasActivosAnteriores: List<String>,
            diasActivosNuevos: List<String>
        ): CambioPendiente {
            val ahora = Date()
            val manana = Calendar.getInstance().apply {
                time = ahora
                add(Calendar.DAY_OF_MONTH, 1)
            }.time
            
            return CambioPendiente(
                empleadoDni = empleadoDni,
                tipoEmpleado = tipoEmpleado,
                tipoCambio = "HORARIO",
                datosAnteriores = mapOf(
                    "horariosSemanales" to horariosAnteriores,
                    "diasActivos" to diasActivosAnteriores
                ),
                datosNuevos = mapOf(
                    "horariosSemanales" to horariosNuevos,
                    "diasActivos" to diasActivosNuevos
                ),
                fechaCreacion = dateTimeFormat.format(ahora),
                fechaAplicacion = dateFormat.format(manana)
            )
        }
        
        fun crearCambioEstado(
            empleadoDni: String,
            tipoEmpleado: String,
            estadoAnterior: Boolean,
            estadoNuevo: Boolean
        ): CambioPendiente {
            val ahora = Date()
            val manana = Calendar.getInstance().apply {
                time = ahora
                add(Calendar.DAY_OF_MONTH, 1)
            }.time
            
            return CambioPendiente(
                empleadoDni = empleadoDni,
                tipoEmpleado = tipoEmpleado,
                tipoCambio = "ESTADO",
                datosAnteriores = mapOf(
                    "activo" to estadoAnterior
                ),
                datosNuevos = mapOf(
                    "activo" to estadoNuevo
                ),
                fechaCreacion = dateTimeFormat.format(ahora),
                fechaAplicacion = dateFormat.format(manana)
            )
        }
    }
    
    fun getDescripcionCambio(): String {
        return when (tipoCambio) {
            "INFORMACION" -> {
                val nombresNuevo = datosNuevos["nombres"] as? String ?: ""
                val apellidosNuevo = datosNuevos["apellidos"] as? String ?: ""
                "Cambio de información personal a: $nombresNuevo $apellidosNuevo"
            }
            "HORARIO" -> {
                if (tipoEmpleado == "FLEXIBLE") {
                    "Modificación de horario flexible"
                } else {
                    val entradaNueva = datosNuevos["horaEntrada"] as? String ?: ""
                    val salidaNueva = datosNuevos["horaSalida"] as? String ?: ""
                    "Cambio de horario a: $entradaNueva - $salidaNueva"
                }
            }
            "ESTADO" -> {
                val estadoNuevo = datosNuevos["activo"] as? Boolean ?: false
                if (estadoNuevo) "Activación del empleado" else "Desactivación del empleado"
            }
            else -> "Cambio desconocido"
        }
    }
    
    fun getFechaAplicacionFormateada(): String {
        return try {
            val fecha = dateFormat.parse(fechaAplicacion)
            val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            displayFormat.format(fecha ?: Date())
        } catch (e: Exception) {
            fechaAplicacion
        }
    }
    
    fun debeAplicarseHoy(): Boolean {
        return try {
            val hoy = dateFormat.format(Date())
            fechaAplicacion <= hoy && !aplicado
        } catch (e: Exception) {
            false
        }
    }
}