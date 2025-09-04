package com.asistencia.app.utils

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

object PinManager {
    
    private const val PREFS_NAME = "PinSecurityPrefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_ENABLED = "pin_enabled"
    private const val KEY_LAST_ACTIVITY = "last_activity"
    private const val TIMEOUT_MINUTES = 5 // 5 minutos de inactividad
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Configurar PIN
    fun setPin(context: Context, pin: String): Boolean {
        return try {
            if (pin.length == 4 && pin.all { it.isDigit() }) {
                val hashedPin = hashPin(pin)
                getPrefs(context).edit()
                    .putString(KEY_PIN_HASH, hashedPin)
                    .putBoolean(KEY_PIN_ENABLED, true)
                    .apply()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    // Verificar PIN
    fun verifyPin(context: Context, pin: String): Boolean {
        return try {
            val storedHash = getPrefs(context).getString(KEY_PIN_HASH, null)
            if (storedHash != null) {
                val inputHash = hashPin(pin)
                storedHash == inputHash
            } else {
                // PIN por defecto si no hay uno configurado
                val defaultPin = "1234"
                val defaultHash = hashPin(defaultPin)
                val inputHash = hashPin(pin)
                defaultHash == inputHash
            }
        } catch (e: Exception) {
            false
        }
    }
    
    // Verificar si PIN está habilitado
    fun isPinEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        val hasStoredPin = prefs.getString(KEY_PIN_HASH, null) != null
        val isEnabled = prefs.getBoolean(KEY_PIN_ENABLED, false)
        
        // Si no hay PIN configurado, usar PIN por defecto (siempre habilitado)
        return hasStoredPin && isEnabled || !hasStoredPin
    }
    
    // Deshabilitar PIN
    fun disablePin(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_PIN_ENABLED, false)
            .apply()
    }
    
    // Registrar actividad (para timeout)
    fun updateLastActivity(context: Context) {
        getPrefs(context).edit()
            .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            .apply()
    }
    
    // Verificar si necesita PIN por timeout
    fun needsPinByTimeout(context: Context): Boolean {
        if (!isPinEnabled(context)) return false
        
        val lastActivity = getPrefs(context).getLong(KEY_LAST_ACTIVITY, 0)
        val currentTime = System.currentTimeMillis()
        val timeoutMillis = TIMEOUT_MINUTES * 60 * 1000L
        
        return (currentTime - lastActivity) > timeoutMillis
    }
    
    // Hash del PIN para seguridad
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    // Obtener timeout en minutos
    fun getTimeoutMinutes(): Int = TIMEOUT_MINUTES
}
