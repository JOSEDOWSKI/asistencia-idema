package com.asistencia.app.utils

import com.asistencia.app.database.Empleado
import com.asistencia.app.database.TipoHorario
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class HorarioDia(
    val horaEntrada: String, // "HH:mm"
    val horaSalida: String,  // "HH:mm"
    val refrigerioInicio: String? = null, // "HH:mm"
    val refrigerioFin: String? = null     // "HH:mm"
)

object HorarioUtils {
    
    private val gson = Gson()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    fun getHorarioParaDia(empleado: Empleado, fecha: Date): HorarioDia? {
        return when (empleado.tipoHorario) {
            TipoHorario.REGULAR -> {
                if (empleado.horaEntradaRegular != null && empleado.horaSalidaRegular != null) {
                    HorarioDia(
                        horaEntrada = empleado.horaEntradaRegular,
                        horaSalida = empleado.horaSalidaRegular,
                        refrigerioInicio = empleado.refrigerioInicioRegular,
                        refrigerioFin = empleado.refrigerioFinRegular
                    )
                } else null
            }
            TipoHorario.FLEXIBLE -> {
                getHorarioFlexible(empleado, fecha)
            }
        }
    }
    
    private fun getHorarioFlexible(empleado: Empleado, fecha: Date): HorarioDia? {
        if (empleado.horarioFlexibleJson.isNullOrEmpty()) return null
        
        try {
            val calendar = Calendar.getInstance()
            calendar.time = fecha
            val diaSemana = getDiaSemanaKey(calendar.get(Calendar.DAY_OF_WEEK))
            
            val type = object : TypeToken<Map<String, HorarioDia>>() {}.type
            val horariosMap: Map<String, HorarioDia> = gson.fromJson(empleado.horarioFlexibleJson, type)
            
            return horariosMap[diaSemana]
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun getDiaSemanaKey(dayOfWeek: Int): String {
        return when (dayOfWeek) {
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
    
    fun crearHorarioFlexibleJson(horarios: Map<String, HorarioDia>): String {
        return gson.toJson(horarios)
    }
    
    fun parseHorarioFlexibleJson(json: String): Map<String, HorarioDia>? {
        return try {
            val type = object : TypeToken<Map<String, HorarioDia>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
    
    fun timeStringToMinutes(timeString: String): Int {
        try {
            val parts = timeString.split(":")
            return parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            return 0
        }
    }
    
    fun minutesToTimeString(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format("%02d:%02d", hours, mins)
    }
    
    fun addMinutesToTime(timeString: String, minutesToAdd: Int): String {
        val totalMinutes = timeStringToMinutes(timeString) + minutesToAdd
        return minutesToTimeString(totalMinutes)
    }
    
    fun calculateTimeDifferenceInMinutes(startTime: String, endTime: String): Int {
        val startMinutes = timeStringToMinutes(startTime)
        val endMinutes = timeStringToMinutes(endTime)
        return endMinutes - startMinutes
    }
    
    fun isTimeAfter(time1: String, time2: String): Boolean {
        return timeStringToMinutes(time1) > timeStringToMinutes(time2)
    }
    
    fun isTimeBefore(time1: String, time2: String): Boolean {
        return timeStringToMinutes(time1) < timeStringToMinutes(time2)
    }
    
    fun getCurrentTimeString(): String {
        return timeFormat.format(Date())
    }
    
    fun getCurrentDateString(): String {
        return dateFormat.format(Date())
    }
    
    fun formatTimestamp(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
    
    fun formatDateTimestamp(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
}