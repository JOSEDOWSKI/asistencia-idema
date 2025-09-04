package com.asistencia.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroAsistenciaDao {
    
    @Query("SELECT * FROM registros_asistencia WHERE empleadoId = :empleadoId AND fecha = :fecha ORDER BY timestampDispositivo")
    suspend fun getRegistrosByEmpleadoAndFecha(empleadoId: String, fecha: String): List<RegistroAsistencia>
    
    @Query("SELECT * FROM registros_asistencia WHERE fecha = :fecha ORDER BY timestampDispositivo DESC")
    fun getRegistrosByFecha(fecha: String): Flow<List<RegistroAsistencia>>
    
    @Query("SELECT * FROM registros_asistencia WHERE empleadoId = :empleadoId ORDER BY timestampDispositivo DESC LIMIT 50")
    fun getRegistrosByEmpleado(empleadoId: String): Flow<List<RegistroAsistencia>>
    
    @Query("SELECT * FROM registros_asistencia WHERE estadoSync = :estado ORDER BY timestampDispositivo")
    suspend fun getRegistrosByEstadoSync(estado: EstadoSync): List<RegistroAsistencia>
    
    @Query("SELECT * FROM registros_asistencia WHERE estadoSync = 'PENDIENTE' ORDER BY timestampDispositivo")
    suspend fun getRegistrosPendientesSync(): List<RegistroAsistencia>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistro(registro: RegistroAsistencia)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistros(registros: List<RegistroAsistencia>)
    
    @Update
    suspend fun updateRegistro(registro: RegistroAsistencia)
    
    @Query("UPDATE registros_asistencia SET estadoSync = :estado WHERE id = :id")
    suspend fun updateEstadoSync(id: String, estado: EstadoSync)
    
    @Query("UPDATE registros_asistencia SET timestampServidor = :timestamp, estadoSync = 'ENVIADO' WHERE id = :id")
    suspend fun markAsSynced(id: String, timestamp: Long)
    
    @Delete
    suspend fun deleteRegistro(registro: RegistroAsistencia)
    
    @Query("DELETE FROM registros_asistencia WHERE fecha < :fechaLimite")
    suspend fun deleteRegistrosAntiguos(fechaLimite: String)
    
    // Verificar duplicados (mismo empleado, tipo evento y dentro de 30 segundos)
    @Query("""
        SELECT COUNT(*) FROM registros_asistencia 
        WHERE empleadoId = :empleadoId 
        AND tipoEvento = :tipoEvento 
        AND ABS(timestampDispositivo - :timestamp) < 30000
    """)
    suspend fun checkDuplicado(empleadoId: String, tipoEvento: TipoEvento, timestamp: Long): Int
    
    // Obtener el último registro del empleado en el día
    @Query("""
        SELECT * FROM registros_asistencia 
        WHERE empleadoId = :empleadoId AND fecha = :fecha 
        ORDER BY timestampDispositivo DESC 
        LIMIT 1
    """)
    suspend fun getUltimoRegistroDelDia(empleadoId: String, fecha: String): RegistroAsistencia?
    
    // Estadísticas
    @Query("SELECT COUNT(*) FROM registros_asistencia WHERE fecha = :fecha")
    suspend fun getCountRegistrosByFecha(fecha: String): Int
    
    @Query("SELECT COUNT(*) FROM registros_asistencia WHERE estadoSync = 'PENDIENTE'")
    suspend fun getCountRegistrosPendientes(): Int
    
    @Query("SELECT * FROM registros_asistencia WHERE empleadoId = :empleadoId AND fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY timestampDispositivo")
    suspend fun getRegistrosByEmpleadoAndRango(empleadoId: String, fechaInicio: String, fechaFin: String): List<RegistroAsistencia>
}