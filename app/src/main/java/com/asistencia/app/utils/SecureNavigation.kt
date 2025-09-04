package com.asistencia.app.utils

import android.content.Context
import android.content.Intent

object SecureNavigation {
    
    // Actividades que requieren PIN
    private val SECURED_ACTIVITIES = setOf(
        "com.asistencia.app.EmpleadosActivityMejorado",
        "com.asistencia.app.ConfiguracionActivity", 
        "com.asistencia.app.ReportesActivity"
    )
    
    // Nombres amigables para mostrar
    private val ACTIVITY_NAMES = mapOf(
        "com.asistencia.app.EmpleadosActivityMejorado" to "Gestión de Empleados",
        "com.asistencia.app.ConfiguracionActivity" to "Configuración",
        "com.asistencia.app.ReportesActivity" to "Reportes"
    )
    
    /**
     * Navega a una actividad con verificación de PIN si es necesario
     */
    fun navigateToActivity(context: Context, targetActivityClass: Class<*>) {
        val targetActivityName = targetActivityClass.name
        
        if (SECURED_ACTIVITIES.contains(targetActivityName)) {
            // Actividad protegida - SIEMPRE pedir PIN
            val intent = Intent(context, Class.forName("com.asistencia.app.PinActivity"))
            intent.putExtra("target_activity", targetActivityName)
            intent.putExtra("target_activity_name", ACTIVITY_NAMES[targetActivityName])
            context.startActivity(intent)
        } else {
            // Actividad no protegida - navegar directamente
            val intent = Intent(context, targetActivityClass)
            context.startActivity(intent)
        }
    }
    
    /**
     * Verifica si una actividad está protegida por PIN
     */
    fun isActivitySecured(activityClass: Class<*>): Boolean {
        return SECURED_ACTIVITIES.contains(activityClass.name)
    }
    
    /**
     * Obtiene el nombre amigable de una actividad
     */
    fun getActivityDisplayName(activityClass: Class<*>): String {
        return ACTIVITY_NAMES[activityClass.name] ?: activityClass.simpleName
    }
}
