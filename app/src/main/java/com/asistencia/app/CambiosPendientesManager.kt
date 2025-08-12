package com.asistencia.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CambiosPendientesManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("CambiosPendientes", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun agregarCambioPendiente(cambio: CambioPendiente) {
        try {
            val cambiosActuales = obtenerCambiosPendientes().toMutableList()
            
            // Cancelar cambios anteriores del mismo empleado y tipo
            cambiosActuales.removeAll { 
                it.empleadoDni == cambio.empleadoDni && 
                it.tipoCambio == cambio.tipoCambio && 
                !it.aplicado 
            }
            
            // Agregar el nuevo cambio
            cambiosActuales.add(cambio)
            
            // Guardar
            val json = gson.toJson(cambiosActuales)
            sharedPreferences.edit().putString("cambios_pendientes", json).apply()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun obtenerCambiosPendientes(): List<CambioPendiente> {
        return try {
            val json = sharedPreferences.getString("cambios_pendientes", "[]")
            val type = object : TypeToken<List<CambioPendiente>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun obtenerCambiosPendientesParaEmpleado(dni: String): List<CambioPendiente> {
        return obtenerCambiosPendientes().filter { 
            it.empleadoDni == dni && !it.aplicado 
        }
    }
    
    fun aplicarCambiosPendientes(): Int {
        try {
            val cambios = obtenerCambiosPendientes().toMutableList()
            val cambiosParaAplicar = cambios.filter { it.debeAplicarseHoy() }
            var cambiosAplicados = 0
            
            val empleadosManager = EmpleadosManager(context)
            
            cambiosParaAplicar.forEach { cambio ->
                try {
                    when (cambio.tipoCambio) {
                        "INFORMACION" -> {
                            aplicarCambioInformacion(cambio, empleadosManager)
                            cambiosAplicados++
                        }
                        "HORARIO" -> {
                            aplicarCambioHorario(cambio, empleadosManager)
                            cambiosAplicados++
                        }
                        "ESTADO" -> {
                            aplicarCambioEstado(cambio, empleadosManager)
                            cambiosAplicados++
                        }
                    }
                    
                    // Marcar como aplicado
                    val index = cambios.indexOfFirst { 
                        it.empleadoDni == cambio.empleadoDni && 
                        it.tipoCambio == cambio.tipoCambio && 
                        it.fechaCreacion == cambio.fechaCreacion 
                    }
                    if (index >= 0) {
                        cambios[index] = cambio.copy(aplicado = true)
                    }
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Guardar cambios actualizados
            if (cambiosAplicados > 0) {
                val json = gson.toJson(cambios)
                sharedPreferences.edit().putString("cambios_pendientes", json).apply()
            }
            
            return cambiosAplicados
            
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }
    
    private fun aplicarCambioInformacion(cambio: CambioPendiente, empleadosManager: EmpleadosManager) {
        val nombres = cambio.datosNuevos["nombres"] as? String ?: return
        val apellidos = cambio.datosNuevos["apellidos"] as? String ?: return
        
        if (cambio.tipoEmpleado == "SIMPLE") {
            empleadosManager.actualizarInformacionEmpleadoSimple(cambio.empleadoDni, nombres, apellidos)
        } else {
            empleadosManager.actualizarInformacionEmpleadoFlexible(cambio.empleadoDni, nombres, apellidos)
        }
    }
    
    private fun aplicarCambioHorario(cambio: CambioPendiente, empleadosManager: EmpleadosManager) {
        if (cambio.tipoEmpleado == "SIMPLE") {
            val entrada = cambio.datosNuevos["horaEntrada"] as? String ?: return
            val salida = cambio.datosNuevos["horaSalida"] as? String ?: return
            empleadosManager.actualizarHorarioEmpleadoSimple(cambio.empleadoDni, entrada, salida)
        } else {
            @Suppress("UNCHECKED_CAST")
            val horarios = cambio.datosNuevos["horariosSemanales"] as? Map<String, Pair<String, String>> ?: return
            @Suppress("UNCHECKED_CAST")
            val diasActivos = cambio.datosNuevos["diasActivos"] as? List<String> ?: return
            empleadosManager.actualizarHorarioEmpleadoFlexible(cambio.empleadoDni, horarios, diasActivos)
        }
    }
    
    private fun aplicarCambioEstado(cambio: CambioPendiente, empleadosManager: EmpleadosManager) {
        val activo = cambio.datosNuevos["activo"] as? Boolean ?: return
        
        if (cambio.tipoEmpleado == "SIMPLE") {
            empleadosManager.cambiarEstadoEmpleadoSimple(cambio.empleadoDni, activo)
        } else {
            empleadosManager.cambiarEstadoEmpleadoFlexible(cambio.empleadoDni, activo)
        }
    }
    
    fun limpiarCambiosAplicados() {
        try {
            val cambios = obtenerCambiosPendientes().filter { !it.aplicado }
            val json = gson.toJson(cambios)
            sharedPreferences.edit().putString("cambios_pendientes", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun cancelarCambiosPendientesParaEmpleado(dni: String) {
        try {
            val cambios = obtenerCambiosPendientes().filter { 
                !(it.empleadoDni == dni && !it.aplicado) 
            }
            val json = gson.toJson(cambios)
            sharedPreferences.edit().putString("cambios_pendientes", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}