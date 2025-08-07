package com.asistencia.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

// Modelo de datos para registros de asistencia
data class RegistroAsistencia(
    val dni: String,
    val fecha: String,
    val hora: String,
    val tipo: String, // "ENTRADA" o "SALIDA"
    val diaSemana: String,
    val llegadaTarde: Boolean = false
)

class AsistenciaManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("AsistenciaApp", Context.MODE_PRIVATE)
    private val configuracionManager = ConfiguracionManager(context)
    private val gson = Gson()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEEE", Locale("es", "ES"))
    
    fun registrarAsistencia(dni: String, tipo: String, personal: Personal? = null): RegistroAsistencia {
        val fechaActual = dateFormat.format(Date())
        val horaActual = timeFormat.format(Date())
        val diaSemana = dayFormat.format(Date())
        
        // Obtener horario esperado según el tipo de horario del empleado
        val horaEsperada = personal?.let { p ->
            val horarioDia = p.getHorarioDia(diaSemana)
            if (tipo == "ENTRADA") horarioDia.entrada else horarioDia.salida
        }
        
        // Calcular si llegó tarde (solo para entradas)
        val llegadaTarde = if (tipo == "ENTRADA" && horaEsperada != null && horaEsperada.isNotEmpty()) {
            calcularLlegadaTarde(horaActual, horaEsperada)
        } else false
        
        val registro = RegistroAsistencia(
            dni = dni,
            fecha = fechaActual,
            hora = horaActual,
            tipo = tipo,
            diaSemana = diaSemana.capitalize(),
            llegadaTarde = llegadaTarde
        )
        
        // Guardar el registro
        val registros = getRegistrosAsistencia().toMutableList()
        registros.add(registro)
        saveRegistrosAsistencia(registros)
        
        return registro
    }
    
    fun getRegistrosAsistencia(): List<RegistroAsistencia> {
        val registrosJson = sharedPreferences.getString("registros_asistencia", "[]")
        val type = object : TypeToken<List<RegistroAsistencia>>() {}.type
        return gson.fromJson(registrosJson, type) ?: emptyList()
    }
    
    fun getRegistrosByDni(dni: String): List<RegistroAsistencia> {
        return getRegistrosAsistencia().filter { it.dni == dni }
            .sortedByDescending { "${it.fecha} ${it.hora}" }
    }
    
    fun getUltimoRegistro(dni: String, fecha: String): RegistroAsistencia? {
        return getRegistrosAsistencia()
            .filter { it.dni == dni && it.fecha == fecha }
            .maxByOrNull { it.hora }
    }
    
    private fun saveRegistrosAsistencia(registros: List<RegistroAsistencia>) {
        val registrosJson = gson.toJson(registros)
        sharedPreferences.edit().putString("registros_asistencia", registrosJson).apply()
    }
    
    private fun calcularLlegadaTarde(horaActual: String, horaEsperada: String): Boolean {
        return try {
            val timeFormatHour = SimpleDateFormat("HH:mm", Locale.getDefault())
            val actual = timeFormatHour.parse(horaActual.substring(0, 5))
            val esperada = timeFormatHour.parse(horaEsperada)
            
            if (actual == null || esperada == null) return false
            
            // Calcular diferencia en minutos
            val diferenciaMs = actual.time - esperada.time
            val diferenciaMinutos = diferenciaMs / (1000 * 60)
            
            // Aplicar tolerancia configurada
            val tolerancia = configuracionManager.toleranciaMinutos
            
            // Es tarde si la diferencia supera la tolerancia
            diferenciaMinutos > tolerancia
        } catch (e: Exception) {
            false
        }
    }
    
    // Función para obtener los minutos de retraso
    fun getMinutosRetraso(horaActual: String, horaEsperada: String): Int {
        return try {
            val timeFormatHour = SimpleDateFormat("HH:mm", Locale.getDefault())
            val actual = timeFormatHour.parse(horaActual.substring(0, 5))
            val esperada = timeFormatHour.parse(horaEsperada)
            
            if (actual == null || esperada == null) return 0
            
            val diferenciaMs = actual.time - esperada.time
            val diferenciaMinutos = (diferenciaMs / (1000 * 60)).toInt()
            
            maxOf(0, diferenciaMinutos) // Solo valores positivos
        } catch (e: Exception) {
            0
        }
    }
    
    fun limpiarRegistros() {
        sharedPreferences.edit().remove("registros_asistencia").apply()
    }
}