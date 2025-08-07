package com.asistencia.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

data class EstadisticaTardanzas(
    val dni: String,
    val nombre: String,
    val tardanzasEsteMes: Int,
    val tardanzasTotal: Int,
    val ultimaTardanza: String?,
    val excedeLimite: Boolean
)

class TardanzasManager(private val context: Context) {
    private val asistenciaManager = AsistenciaManager(context)
    private val configuracionManager = ConfiguracionManager(context)
    
    private val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    fun getEstadisticasTardanzas(personalList: List<Personal>): List<EstadisticaTardanzas> {
        val mesActual = dateFormat.format(Date())
        
        return personalList.map { personal ->
            val registros = asistenciaManager.getRegistrosByDni(personal.dni)
            val tardanzasTotal = registros.count { it.llegadaTarde }
            
            // Contar tardanzas del mes actual
            val tardanzasEsteMes = registros.count { registro ->
                registro.llegadaTarde && registro.fecha.startsWith(mesActual)
            }
            
            // Última tardanza
            val ultimaTardanza = registros
                .filter { it.llegadaTarde }
                .maxByOrNull { it.fecha + " " + it.hora }
                ?.let { displayDateFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.fecha)) }
            
            // Verificar si excede el límite
            val excedeLimite = configuracionManager.contarTardanzas && 
                              tardanzasEsteMes > configuracionManager.limiteTardanzasMes
            
            EstadisticaTardanzas(
                dni = personal.dni,
                nombre = personal.nombre,
                tardanzasEsteMes = tardanzasEsteMes,
                tardanzasTotal = tardanzasTotal,
                ultimaTardanza = ultimaTardanza,
                excedeLimite = excedeLimite
            )
        }.sortedByDescending { it.tardanzasEsteMes }
    }
    
    fun getPersonalConMasTardanzas(personalList: List<Personal>, limite: Int = 5): List<EstadisticaTardanzas> {
        return getEstadisticasTardanzas(personalList)
            .filter { it.tardanzasTotal > 0 }
            .take(limite)
    }
    
    fun getPersonalQueExcedeLimite(personalList: List<Personal>): List<EstadisticaTardanzas> {
        return getEstadisticasTardanzas(personalList)
            .filter { it.excedeLimite }
    }
    
    fun getTardanzasDelMes(dni: String): Int {
        val mesActual = dateFormat.format(Date())
        return asistenciaManager.getRegistrosByDni(dni)
            .count { it.llegadaTarde && it.fecha.startsWith(mesActual) }
    }
    
    fun verificarLimiteAlcanzado(dni: String): Boolean {
        if (!configuracionManager.contarTardanzas) return false
        
        val tardanzasDelMes = getTardanzasDelMes(dni)
        return tardanzasDelMes >= configuracionManager.limiteTardanzasMes
    }
    
    fun getMensajeAlerta(dni: String, nombre: String): String? {
        if (!configuracionManager.notificarTardanzas) return null
        
        val tardanzasDelMes = getTardanzasDelMes(dni)
        val limite = configuracionManager.limiteTardanzasMes
        
        return when {
            tardanzasDelMes >= limite -> {
                "⚠️ LÍMITE EXCEDIDO\n$nombre ha superado el límite de $limite tardanzas este mes ($tardanzasDelMes tardanzas)"
            }
            tardanzasDelMes == limite - 1 -> {
                "⚠️ ADVERTENCIA\n$nombre está cerca del límite ($tardanzasDelMes/$limite tardanzas este mes)"
            }
            else -> null
        }
    }
}