package com.asistencia.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class EmailDestinatario(
    val nombre: String,
    val email: String,
    val tipo: String = "empleador", // empleador, supervisor, etc.
    val activo: Boolean = true
)

data class EmailConfig(
    val smtpServer: String = "",
    val smtpPort: Int = 587,
    val emailFrom: String = "",
    val password: String = "",
    val useSSL: Boolean = true,
    val destinatarios: List<EmailDestinatario> = emptyList(),
    val enviarAutomatico: Boolean = false,
    val horaEnvio: String = "18:00", // Hora para envío automático
    val diasEnvio: List<String> = listOf("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES")
)

object EmailConfigManager {
    
    private const val PREFS_NAME = "EmailConfigPrefs"
    private const val KEY_EMAIL_CONFIG = "email_config"
    private const val KEY_DESTINATARIOS = "destinatarios"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Guardar configuración de email
    fun saveEmailConfig(context: Context, config: EmailConfig) {
        val configJson = Gson().toJson(config)
        getPrefs(context).edit()
            .putString(KEY_EMAIL_CONFIG, configJson)
            .apply()
    }
    
    // Cargar configuración de email
    fun loadEmailConfig(context: Context): EmailConfig {
        val configJson = getPrefs(context).getString(KEY_EMAIL_CONFIG, null)
        return if (configJson != null) {
            try {
                Gson().fromJson(configJson, EmailConfig::class.java)
            } catch (e: Exception) {
                EmailConfig()
            }
        } else {
            EmailConfig()
        }
    }
    
    // Guardar destinatarios
    fun saveDestinatarios(context: Context, destinatarios: List<EmailDestinatario>) {
        val destinatariosJson = Gson().toJson(destinatarios)
        getPrefs(context).edit()
            .putString(KEY_DESTINATARIOS, destinatariosJson)
            .apply()
    }
    
    // Cargar destinatarios
    fun loadDestinatarios(context: Context): List<EmailDestinatario> {
        val destinatariosJson = getPrefs(context).getString(KEY_DESTINATARIOS, "[]")
        return try {
            val type = object : TypeToken<List<EmailDestinatario>>() {}.type
            Gson().fromJson(destinatariosJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Agregar destinatario
    fun addDestinatario(context: Context, destinatario: EmailDestinatario) {
        val destinatarios = loadDestinatarios(context).toMutableList()
        destinatarios.add(destinatario)
        saveDestinatarios(context, destinatarios)
    }
    
    // Eliminar destinatario
    fun removeDestinatario(context: Context, email: String) {
        val destinatarios = loadDestinatarios(context).toMutableList()
        destinatarios.removeAll { it.email == email }
        saveDestinatarios(context, destinatarios)
    }
    
    // Verificar si la configuración está completa
    fun isConfigComplete(context: Context): Boolean {
        val config = loadEmailConfig(context)
        val destinatarios = loadDestinatarios(context)
        
        return config.smtpServer.isNotEmpty() &&
               config.emailFrom.isNotEmpty() &&
               config.password.isNotEmpty() &&
               destinatarios.isNotEmpty()
    }
    
    // Obtener destinatarios activos
    fun getDestinatariosActivos(context: Context): List<EmailDestinatario> {
        return loadDestinatarios(context).filter { it.activo }
    }
    
    // Verificar si es hora de envío automático
    fun shouldSendAutomatic(context: Context): Boolean {
        val config = loadEmailConfig(context)
        if (!config.enviarAutomatico) return false
        
        val calendar = java.util.Calendar.getInstance()
        val currentDay = getDayOfWeek(calendar.get(java.util.Calendar.DAY_OF_WEEK))
        val currentTime = String.format("%02d:%02d", 
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE))
        
        return config.diasEnvio.contains(currentDay) && currentTime == config.horaEnvio
    }
    
    private fun getDayOfWeek(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            java.util.Calendar.SUNDAY -> "DOMINGO"
            java.util.Calendar.MONDAY -> "LUNES"
            java.util.Calendar.TUESDAY -> "MARTES"
            java.util.Calendar.WEDNESDAY -> "MIERCOLES"
            java.util.Calendar.THURSDAY -> "JUEVES"
            java.util.Calendar.FRIDAY -> "VIERNES"
            java.util.Calendar.SATURDAY -> "SABADO"
            else -> "LUNES"
        }
    }
}
