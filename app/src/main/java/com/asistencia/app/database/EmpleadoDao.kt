package com.asistencia.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmpleadoDao {
    
    @Query("SELECT * FROM empleados WHERE activo = 1 ORDER BY apellidos, nombres")
    fun getAllEmpleadosActivos(): Flow<List<Empleado>>
    
    @Query("SELECT * FROM empleados ORDER BY apellidos, nombres")
    fun getAllEmpleados(): Flow<List<Empleado>>
    
    @Query("SELECT * FROM empleados WHERE id = :id")
    suspend fun getEmpleadoById(id: String): Empleado?
    
    @Query("SELECT * FROM empleados WHERE dni = :dni AND activo = 1")
    suspend fun getEmpleadoByDni(dni: String): Empleado?
    
    @Query("SELECT * FROM empleados WHERE dni = :dni")
    suspend fun getEmpleadoByDniIncludeInactive(dni: String): Empleado?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmpleado(empleado: Empleado)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmpleados(empleados: List<Empleado>)
    
    @Update
    suspend fun updateEmpleado(empleado: Empleado)
    
    @Query("UPDATE empleados SET activo = 0 WHERE id = :id")
    suspend fun deactivateEmpleado(id: String)
    
    @Query("UPDATE empleados SET activo = 1 WHERE id = :id")
    suspend fun activateEmpleado(id: String)
    
    @Delete
    suspend fun deleteEmpleado(empleado: Empleado)
    
    @Query("SELECT COUNT(*) FROM empleados WHERE activo = 1")
    suspend fun getActiveEmployeeCount(): Int
    
    @Query("SELECT * FROM empleados WHERE nombres LIKE '%' || :query || '%' OR apellidos LIKE '%' || :query || '%' OR dni LIKE '%' || :query || '%'")
    fun searchEmpleados(query: String): Flow<List<Empleado>>
}