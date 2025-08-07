package com.asistencia.app

import android.content.Context
import android.content.SharedPreferences

class ConfiguracionManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("AsistenciaConfig", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_TOLERANCIA_MINUTOS = "tolerancia_minutos"
        private const val KEY_CONTAR_TARDANZAS = "contar_tardanzas"
        private const val KEY_LIMITE_TARDANZAS_MES = "limite_tardanzas_mes"
        private const val KEY_NOTIFICAR_TARDANZAS = "notificar_tardanzas"
        
        // Valores por defecto
        private const val DEFAULT_TOLERANCIA = 15 // 15 minutos
        private const val DEFAULT_LIMITE_TARDANZAS = 3 // 3 tardanzas por mes
    }
    
    // Tolerancia en minutos para llegadas tarde
    var toleranciaMinutos: Int
        get() = sharedPreferences.getInt(KEY_TOLERANCIA_MINUTOS, DEFAULT_TOLERANCIA)
        set(value) = sharedPreferences.edit().putInt(KEY_TOLERANCIA_MINUTOS, value).apply()
    
    // Si se debe contar las tardanzas
    var contarTardanzas: Boolean
        get() = sharedPreferences.getBoolean(KEY_CONTAR_TARDANZAS, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_CONTAR_TARDANZAS, value).apply()
    
    // Límite de tardanzas por mes
    var limiteTardanzasMes: Int
        get() = sharedPreferences.getInt(KEY_LIMITE_TARDANZAS_MES, DEFAULT_LIMITE_TARDANZAS)
        set(value) = sharedPreferences.edit().putInt(KEY_LIMITE_TARDANZAS_MES, value).apply()
    
    // Si se debe notificar cuando se acerque al límite
    var notificarTardanzas: Boolean
        get() = sharedPreferences.getBoolean(KEY_NOTIFICAR_TARDANZAS, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_NOTIFICAR_TARDANZAS, value).apply()
    
    // Obtener configuración como texto descriptivo
    fun getDescripcionTolerancia(): String {
        return if (toleranciaMinutos > 0) {
            "Tolerancia: $toleranciaMinutos minutos"
        } else {
            "Sin tolerancia - Puntualidad estricta"
        }
    }
    
    fun getDescripcionLimite(): String {
        return if (contarTardanzas) {
            "Límite: $limiteTardanzasMes tardanzas por mes"
        } else {
            "Sin límite de tardanzas"
        }
    }
}