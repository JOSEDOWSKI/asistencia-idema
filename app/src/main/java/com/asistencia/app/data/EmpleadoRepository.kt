package com.asistencia.app.data

import android.content.Context
import android.content.SharedPreferences
import com.asistencia.app.EmpleadosActivityMejorado.EmpleadoSimple
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EmpleadoRepository(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "empleados_prefs",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val keyEmpleados = "lista_empleados"

    fun guardarEmpleados(empleados: List<EmpleadoSimple>) {
        val json = gson.toJson(empleados)
        sharedPreferences.edit().putString(keyEmpleados, json).apply()
    }

    fun obtenerEmpleados(): List<EmpleadoSimple> {
        val json = sharedPreferences.getString(keyEmpleados, null) ?: return emptyList()
        val type = object : TypeToken<List<EmpleadoSimple>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun agregarEmpleado(empleado: EmpleadoSimple): Boolean {
        val empleados = obtenerEmpleados().toMutableList()
        
        // Verificar si ya existe un empleado con el mismo DNI
        if (empleados.any { it.dni == empleado.dni }) {
            return false
        }
        
        empleados.add(empleado)
        guardarEmpleados(empleados)
        return true
    }

    fun actualizarEmpleado(empleado: EmpleadoSimple): Boolean {
        val empleados = obtenerEmpleados().toMutableList()
        val index = empleados.indexOfFirst { it.dni == empleado.dni }
        
        if (index == -1) {
            return false
        }
        
        empleados[index] = empleado
        guardarEmpleados(empleados)
        return true
    }

    fun eliminarEmpleado(dni: String): Boolean {
        val empleados = obtenerEmpleados().toMutableList()
        val empleado = empleados.find { it.dni == dni } ?: return false
        
        // Marcar como inactivo en lugar de eliminar para mantener el historial
        val empleadoActualizado = empleado.copy(activo = false)
        val index = empleados.indexOf(empleado)
        empleados[index] = empleadoActualizado
        
        guardarEmpleados(empleados)
        return true
    }

    fun buscarEmpleadoPorDni(dni: String): EmpleadoSimple? {
        return obtenerEmpleados().find { it.dni == dni && it.activo }
    }
}
