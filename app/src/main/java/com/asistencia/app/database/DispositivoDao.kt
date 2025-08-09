package com.asistencia.app.database

import androidx.room.*

@Dao
interface DispositivoDao {
    
    @Query("SELECT * FROM dispositivo WHERE deviceId = :deviceId")
    suspend fun getDispositivo(deviceId: String): Dispositivo?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispositivo(dispositivo: Dispositivo)
    
    @Update
    suspend fun updateDispositivo(dispositivo: Dispositivo)
    
    @Query("UPDATE dispositivo SET skewMs = :skewMs WHERE deviceId = :deviceId")
    suspend fun updateSkew(deviceId: String, skewMs: Long)
    
    @Query("UPDATE dispositivo SET operadorPinHash = :pinHash WHERE deviceId = :deviceId")
    suspend fun updateOperadorPin(deviceId: String, pinHash: String)
    
    @Query("UPDATE dispositivo SET modoLectura = :modo WHERE deviceId = :deviceId")
    suspend fun updateModoLectura(deviceId: String, modo: ModoLectura)
    
    @Query("UPDATE dispositivo SET capturaUbicacion = :captura WHERE deviceId = :deviceId")
    suspend fun updateCapturaUbicacion(deviceId: String, captura: Boolean)
    
    @Query("UPDATE dispositivo SET apiEndpoint = :endpoint, apiToken = :token WHERE deviceId = :deviceId")
    suspend fun updateApiConfig(deviceId: String, endpoint: String?, token: String?)
}