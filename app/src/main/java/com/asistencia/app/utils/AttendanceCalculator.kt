package com.asistencia.app.utils

import com.asistencia.app.EmpleadosActivityMejorado.EmpleadoSimple
import java.util.*

/**
 * Utility class for handling employee attendance calculations
 */
object AttendanceCalculator {
    
    /**
     * Validates if an employee's check-in is within the 15-minute tolerance
     * @param checkInTime Actual check-in time in "HH:mm" format
     * @param scheduledStartTime Scheduled start time in "HH:mm" format
     * @return true if check-in is on time or within tolerance, false if late
     */
    fun isCheckInValid(checkInTime: String, scheduledStartTime: String): Boolean {
        val timeUtils = TimeUtils
        val toleranceEnd = timeUtils.addMinutesToTime(scheduledStartTime, 15)
        return timeUtils.compareTimes(checkInTime, toleranceEnd) <= 0
    }
    
    /**
     * Calculates the minutes late based on scheduled time and 15-minute tolerance
     * @param checkInTime Actual check-in time in "HH:mm" format
     * @param scheduledStartTime Scheduled start time in "HH:mm" format
     * @return Number of minutes late (0 if on time or within tolerance)
     */
    fun calculateMinutesLate(checkInTime: String, scheduledStartTime: String): Int {
        val timeUtils = TimeUtils
        val toleranceEnd = timeUtils.addMinutesToTime(scheduledStartTime, 15)
        
        // If check-in is before or at the end of tolerance period, not late
        if (timeUtils.compareTimes(checkInTime, toleranceEnd) <= 0) {
            return 0
        }
        
        // Calculate minutes late after tolerance period
        return timeUtils.getTimeDifferenceInMinutes(toleranceEnd, checkInTime)
    }
    
    /**
     * Validates if the break time is within working hours
     * @param breakStart Break start time in "HH:mm" format
     * @param breakEnd Break end time in "HH:mm" format
     * @param workStart Work start time in "HH:mm" format
     * @param workEnd Work end time in "HH:mm" format
     * @return true if break is within working hours, false otherwise
     */
    fun isValidBreakTime(
        breakStart: String,
        breakEnd: String,
        workStart: String,
        workEnd: String
    ): Boolean {
        val timeUtils = TimeUtils
        
        // Break must start after work starts and end before work ends
        val startsInWorkHours = timeUtils.compareTimes(breakStart, workStart) >= 0 &&
                              timeUtils.compareTimes(breakStart, workEnd) < 0
                              
        val endsInWorkHours = timeUtils.compareTimes(breakEnd, workStart) > 0 &&
                            timeUtils.compareTimes(breakEnd, workEnd) <= 0
                            
        val validDuration = timeUtils.compareTimes(breakStart, breakEnd) < 0
        
        return startsInWorkHours && endsInWorkHours && validDuration
    }
    
    /**
     * Calculates the total working hours for an employee for a given day
     * @param empleado The employee
     * @param checkInTime Actual check-in time in "HH:mm" format
     * @param checkOutTime Actual check-out time in "HH:mm" format
     * @return Triple of (totalHours, isLate, leftEarly) where totalHours is in "HH:mm" format
     */
    fun calculateDailyWorkHours(
        empleado: EmpleadoSimple,
        checkInTime: String,
        checkOutTime: String
    ): Triple<String, Boolean, Boolean> {
        return TimeUtils.calculateWorkedHours(
            checkIn = checkInTime,
            checkOut = checkOutTime,
            horaEntrada = empleado.horaEntrada,
            horaSalida = empleado.horaSalida,
            tieneRefrigerio = empleado.tieneRefrigerio,
            horaInicioRefrigerio = if (empleado.tieneRefrigerio) empleado.horaInicioRefrigerio else null,
            horaFinRefrigerio = if (empleado.tieneRefrigerio) empleado.horaFinRefrigerio else null
        )
    }
    
    /**
     * Formats minutes to a human-readable string (e.g., "2 hours 30 minutes")
     */
    fun formatMinutesToReadable(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        
        return when {
            hours > 0 && mins > 0 -> "$hours ${if (hours == 1) "hora" else "horas"} $mins ${if (mins == 1) "minuto" else "minutos"}"
            hours > 0 -> "$hours ${if (hours == 1) "hora" else "horas"}"
            else -> "$mins ${if (mins == 1) "minuto" else "minutos"}"
        }
    }
}
