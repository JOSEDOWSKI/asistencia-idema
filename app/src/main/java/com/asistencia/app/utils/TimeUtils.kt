package com.asistencia.app.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object TimeUtils {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    /**
     * Calculates worked hours considering 15-minute tolerance and optional break time
     * @param checkIn Check-in time in "HH:mm" format
     * @param checkOut Check-out time in "HH:mm" format
     * @param breakStart Break start time in "HH:mm" format (optional)
     * @param breakEnd Break end time in "HH:mm" format (optional)
     * @return String in "HH:mm" format with total worked hours
     */
    fun calculateWorkedHours(
        checkIn: String,
        checkOut: String,
        breakStart: String? = null,
        breakEnd: String? = null
    ): String {
        try {
            val checkInTime = timeFormat.parse(checkIn) ?: return "00:00"
            var checkOutTime = timeFormat.parse(checkOut) ?: return "00:00"
            
            // Apply 15-minute tolerance to check-in time
            val calendar = Calendar.getInstance().apply {
                time = checkInTime
                add(Calendar.MINUTE, 15)
            }
            val checkInWithTolerance = calendar.time
            
            // If check-out is before check-in + tolerance, return 0 (unless it's the next day)
            if (checkOutTime.before(checkInWithTolerance) && !isSameDay(checkInTime, checkOutTime)) {
                return "00:00"
            }
            
            // Calculate total minutes worked
            var totalMinutes = TimeUnit.MILLISECONDS.toMinutes(
                checkOutTime.time - checkInTime.time
            ).toInt()
            
            // Subtract break time if configured
            if (!breakStart.isNullOrEmpty() && !breakEnd.isNullOrEmpty()) {
                val breakStartTime = timeFormat.parse(breakStart)
                val breakEndTime = timeFormat.parse(breakEnd)
                
                if (breakStartTime != null && breakEndTime != null) {
                    val breakMinutes = TimeUnit.MILLISECONDS.toMinutes(
                        breakEndTime.time - breakStartTime.time
                    ).toInt()
                    totalMinutes -= breakMinutes
                }
            }
            
            // Ensure non-negative
            totalMinutes = totalMinutes.coerceAtLeast(0)
            
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            
            return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
        } catch (e: Exception) {
            e.printStackTrace()
            return "00:00"
        }
    }
    
    /**
     * Checks if two dates are on the same day
     */
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Adds minutes to a given time
     * @param time Time in "HH:mm" format
     * @param minutes Minutes to add
     * @return Time in "HH:mm" format with added minutes
     */
    fun addMinutesToTime(time: String, minutes: Int): String {
        val timeParts = time.split(":")
        val hours = timeParts[0].toInt()
        val mins = timeParts[1].toInt()
        
        val newMins = mins + minutes
        val newHours = hours + (newMins / 60)
        val newMinutes = newMins % 60
        
        return String.format(Locale.getDefault(), "%02d:%02d", newHours, newMinutes)
    }
    
    /**
     * Calculates time difference in minutes between two times
     * @param time1 Time in "HH:mm" format
     * @param time2 Time in "HH:mm" format
     * @return Time difference in minutes
     */
    fun getTimeDifferenceInMinutes(time1: String, time2: String): Int {
        val time1Parts = time1.split(":")
        val time2Parts = time2.split(":")
        
        val hours1 = time1Parts[0].toInt()
        val mins1 = time1Parts[1].toInt()
        val hours2 = time2Parts[0].toInt()
        val mins2 = time2Parts[1].toInt()
        
        val totalMins1 = hours1 * 60 + mins1
        val totalMins2 = hours2 * 60 + mins2
        
        return Math.abs(totalMins2 - totalMins1)
    }
    
    /**
     * Validates if a time string is in correct "HH:mm" format
     */
    fun isValidTime(time: String): Boolean {
        return try {
            timeFormat.isLenient = false
            timeFormat.parse(time)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Compares two time strings in "HH:mm" format
     * @return 1 if time1 > time2, -1 if time1 < time2, 0 if equal
     */
    fun compareTimes(time1: String, time2: String): Int {
        return try {
            val t1 = timeFormat.parse(time1) ?: return 0
            val t2 = timeFormat.parse(time2) ?: return 0
            t1.compareTo(t2)
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Calculates worked hours considering 15-minute tolerance and break time
     * @param checkIn Actual check-in time in "HH:mm" format
     * @param checkOut Actual check-out time in "HH:mm" format
     * @param horaEntrada Scheduled start time in "HH:mm" format
     * @param horaSalida Scheduled end time in "HH:mm" format
     * @param tieneRefrigerio Whether the employee has a break time
     * @param horaInicioRefrigerio Break start time in "HH:mm" format (optional)
     * @param horaFinRefrigerio Break end time in "HH:mm" format (optional)
     * @return Pair of (workedHours, isLate, leftEarly) where workedHours is in "HH:mm" format
     */
    fun calculateWorkedHours(
        checkIn: String,
        checkOut: String,
        horaEntrada: String,
        horaSalida: String,
        tieneRefrigerio: Boolean = false,
        horaInicioRefrigerio: String? = null,
        horaFinRefrigerio: String? = null
    ): Triple<String, Boolean, Boolean> {
        try {
            // Parse all time strings
            val checkInTime = timeFormat.parse(checkIn) ?: return Triple("00:00", false, false)
            val checkOutTime = timeFormat.parse(checkOut) ?: return Triple("00:00", false, false)
            val entradaTime = timeFormat.parse(horaEntrada) ?: return Triple("00:00", false, false)
            val salidaTime = timeFormat.parse(horaSalida) ?: return Triple("00:00", false, false)
            
            // Apply 15-minute tolerance to check-in time
            val calendar = Calendar.getInstance().apply { time = entradaTime }
            calendar.add(Calendar.MINUTE, 15)
            val entradaConTolerancia = calendar.time
            
            // Check if employee is late
            val isLate = checkInTime.after(entradaConTolerancia)
            
            // Check if employee left early (before scheduled time)
            val leftEarly = checkOutTime.before(salidaTime) && 
                           compareTimes(checkOut, horaSalida) < 0
            
            // Calculate total minutes worked
            var totalMinutes = TimeUnit.MILLISECONDS.toMinutes(
                checkOutTime.time - checkInTime.time
            ).toInt()
            
            // Subtract break time if configured
            if (tieneRefrigerio && !horaInicioRefrigerio.isNullOrEmpty() && !horaFinRefrigerio.isNullOrEmpty()) {
                val breakStartTime = timeFormat.parse(horaInicioRefrigerio)
                val breakEndTime = timeFormat.parse(horaFinRefrigerio)
                
                if (breakStartTime != null && breakEndTime != null) {
                    val breakMinutes = TimeUnit.MILLISECONDS.toMinutes(
                        breakEndTime.time - breakStartTime.time
                    ).toInt()
                    totalMinutes -= breakMinutes
                }
            }
            
            // Ensure non-negative
            totalMinutes = totalMinutes.coerceAtLeast(0)
            
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            
            // Format the result
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
            
            return Triple(formattedTime, isLate, leftEarly)
        } catch (e: Exception) {
            e.printStackTrace()
            return Triple("00:00", false, false)
        }
    }
}
