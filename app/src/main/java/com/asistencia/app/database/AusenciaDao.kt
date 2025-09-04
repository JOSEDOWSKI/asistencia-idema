package com.asistencia.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AusenciaDao {
    
    @Query("SELECT * FROM ausencias WHERE empleadoId = :empleadoId ORDER BY fecha DESC")
    fun getAusenciasByEmpleado(empleadoId: String): Flow<List<Ausencia>>
    
    @Query("SELECT * FROM ausencias WHERE empleadoId = :empleadoId AND fecha = :fecha")
    suspend fun getAusenciaByEmpleadoAndFecha(empleadoId: String, fecha: String): Ausencia?
    
    @Query("SELECT * FROM ausencias WHERE empleadoId = :empleadoId AND fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY fecha")
    suspend fun getAusenciasByEmpleadoAndRango(empleadoId: String, fechaInicio: String, fechaFin: String): List<Ausencia>
    
    @Query("SELECT COUNT(*) FROM ausencias WHERE empleadoId = :empleadoId AND fecha BETWEEN :fechaInicio AND :fechaFin")
    suspend fun getCountAusenciasByEmpleadoAndRango(empleadoId: String, fechaInicio: String, fechaFin: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAusencia(ausencia: Ausencia)
    
    @Update
    suspend fun updateAusencia(ausencia: Ausencia)
    
    @Delete
    suspend fun deleteAusencia(ausencia: Ausencia)
    
    @Query("DELETE FROM ausencias WHERE empleadoId = :empleadoId AND fecha = :fecha")
    suspend fun deleteAusenciaByEmpleadoAndFecha(empleadoId: String, fecha: String)
    
    @Query("SELECT * FROM ausencias WHERE fecha = :fecha")
    suspend fun getAusenciasByFecha(fecha: String): List<Ausencia>
}
