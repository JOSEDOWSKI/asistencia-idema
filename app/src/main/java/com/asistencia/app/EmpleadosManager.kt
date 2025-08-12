package com.asistencia.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EmpleadosManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("EmpleadosApp", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Empleados Simples
    fun obtenerEmpleadosSimples(): List<EmpleadoSimple> {
        return try {
            val json = sharedPreferences.getString("empleados_list", "[]")
            val type = object : TypeToken<List<EmpleadoSimple>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun obtenerEmpleadoSimple(dni: String): EmpleadoSimple? {
        return obtenerEmpleadosSimples().find { it.dni == dni }
    }
    
    fun actualizarInformacionEmpleadoSimple(dni: String, nombres: String, apellidos: String) {
        try {
            val empleados = obtenerEmpleadosSimples().toMutableList()
            val index = empleados.indexOfFirst { it.dni == dni }
            
            if (index >= 0) {
                empleados[index] = empleados[index].copy(
                    nombres = nombres,
                    apellidos = apellidos
                )
                
                val json = gson.toJson(empleados)
                sharedPreferences.edit().putString("empleados_list", json).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun actualizarHorarioEmpleadoSimple(dni: String, entrada: String, salida: String) {
        try {
            val empleados = obtenerEmpleadosSimples().toMutableList()
            val index = empleados.indexOfFirst { it.dni == dni }
            
            if (index >= 0) {
                empleados[index] = empleados[index].copy(
                    horaEntrada = entrada,
                    horaSalida = salida
                )
                
                val json = gson.toJson(empleados)
                sharedPreferences.edit().putString("empleados_list", json).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun cambiarEstadoEmpleadoSimple(dni: String, activo: Boolean) {
        try {
            val empleados = obtenerEmpleadosSimples().toMutableList()
            val index = empleados.indexOfFirst { it.dni == dni }
            
            if (index >= 0) {
                empleados[index] = empleados[index].copy(activo = activo)
                
                val json = gson.toJson(empleados)
                sharedPreferences.edit().putString("empleados_list", json).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Empleados Flexibles
    fun obtenerEmpleadosFlexibles(): List<EmpleadoFlexible> {
        return try {
            val json = sharedPreferences.getString("empleados_flexibles", "[]")
            val type = object : TypeToken<List<EmpleadoFlexible>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun obtenerEmpleadoFlexible(dni: String): EmpleadoFlexible? {
        return obtenerEmpleadosFlexibles().find { it.dni == dni }
    }
    
    fun actualizarInformacionEmpleadoFlexible(dni: String, nombres: String, apellidos: String) {
        try {
            val empleados = obtenerEmpleadosFlexibles().toMutableList()
            val index = empleados.indexOfFirst { it.dni == dni }
            
            if (index >= 0) {
                empleados[index] = empleados[index].copy(
                    nombres = nombres,
                    apellidos = apellidos
                )
                
                val json = gson.toJson(empleados)
                sharedPreferences.edit().putString("empleados_flexibles", json).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun actualizarHorarioEmpleadoFlexible(
        dni: String, 
        horarios: Map<String, Pair<String, String>>, 
        diasActivos: List<String>
    ) {
        try {
            val empleados = obtenerEmpleadosFlexibles().toMutableList()
            val index = empleados.indexOfFirst { it.dni == dni }
            
            if (index >= 0) {
                empleados[index] = empleados[index].copy(
                    horariosSemanales = horarios,
                    diasActivos = diasActivos
                )
                
                val json = gson.toJson(empleados)
                sharedPreferences.edit().putString("empleados_flexibles", json).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun cambiarEstadoEmpleadoFlexible(dni: String, activo: Boolean) {
        try {
            val empleados = obtenerEmpleadosFlexibles().toMutableList()
            val index = empleados.indexOfFirst { it.dni == dni }
            
            if (index >= 0) {
                empleados[index] = empleados[index].copy(activo = activo)
                
                val json = gson.toJson(empleados)
                sharedPreferences.edit().putString("empleados_flexibles", json).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Métodos generales
    fun obtenerEmpleadoPorDni(dni: String): Any? {
        return obtenerEmpleadoSimple(dni) ?: obtenerEmpleadoFlexible(dni)
    }
    
    fun esEmpleadoFlexible(dni: String): Boolean {
        return obtenerEmpleadoFlexible(dni) != null
    }
    
    fun obtenerTotalEmpleados(): Int {
        return obtenerEmpleadosSimples().size + obtenerEmpleadosFlexibles().size
    }
    
    fun obtenerEmpleadosActivos(): Int {
        val simplesActivos = obtenerEmpleadosSimples().count { it.activo }
        val flexiblesActivos = obtenerEmpleadosFlexibles().count { it.activo }
        return simplesActivos + flexiblesActivos
    }
}