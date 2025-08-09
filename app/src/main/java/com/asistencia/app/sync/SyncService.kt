package com.asistencia.app.sync

import android.content.Context
import androidx.work.*
import com.asistencia.app.database.AsistenciaDatabase
import com.asistencia.app.database.EstadoSync
import com.asistencia.app.database.RegistroAsistencia
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// API Interface
interface AsistenciaApi {
    @POST("api/v1/attendance")
    suspend fun syncAttendance(@Body request: SyncRequest): SyncResponse
    
    @GET("api/v1/employees")
    suspend fun getEmployees(@Query("updated_since") updatedSince: Long? = null): EmployeesResponse
}

// Data classes para API
data class SyncRequest(
    val registros: List<RegistroSync>,
    val deviceId: String,
    val idempotencyKey: String
)

data class RegistroSync(
    val id: String,
    val empleadoId: String,
    val fecha: String,
    val tipoEvento: String,
    val timestampDispositivo: Long,
    val deviceId: String,
    val modoLectura: String,
    val rawCode: String,
    val gpsLat: Double?,
    val gpsLon: Double?,
    val nota: String?,
    val marcasCalculoJson: String?
)

data class SyncResponse(
    val success: Boolean,
    val serverTime: Long,
    val processedRecords: List<String>,
    val errors: List<SyncError>
)

data class SyncError(
    val recordId: String,
    val error: String
)

data class EmployeesResponse(
    val employees: List<EmpleadoSync>,
    val serverTime: Long
)

data class EmpleadoSync(
    val id: String,
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val area: String?,
    val activo: Boolean,
    val tipoHorario: String,
    val horaEntradaRegular: String?,
    val horaSalidaRegular: String?,
    val refrigerioInicioRegular: String?,
    val refrigerioFinRegular: String?,
    val horarioFlexibleJson: String?,
    val refrigerioFlexibleJson: String?
)

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val database = AsistenciaDatabase.getDatabase(context)
    private val gson = Gson()
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dispositivo = database.dispositivoDao().getDispositivo(getDeviceId())
            
            if (dispositivo?.apiEndpoint.isNullOrEmpty() || dispositivo?.apiToken.isNullOrEmpty()) {
                return@withContext Result.success()
            }
            
            val api = createApiService(dispositivo!!.apiEndpoint!!, dispositivo.apiToken!!)
            
            // Sincronizar registros pendientes
            val registrosPendientes = database.registroAsistenciaDao().getRegistrosPendientesSync()
            
            if (registrosPendientes.isNotEmpty()) {
                val result = syncRegistros(api, registrosPendientes, dispositivo.deviceId)
                if (!result) {
                    return@withContext Result.retry()
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private suspend fun syncRegistros(
        api: AsistenciaApi,
        registros: List<RegistroAsistencia>,
        deviceId: String
    ): Boolean {
        return try {
            val registrosSync = registros.map { registro ->
                RegistroSync(
                    id = registro.id,
                    empleadoId = registro.empleadoId,
                    fecha = registro.fecha,
                    tipoEvento = registro.tipoEvento.name,
                    timestampDispositivo = registro.timestampDispositivo,
                    deviceId = registro.deviceId,
                    modoLectura = registro.modoLectura.name,
                    rawCode = registro.rawCode,
                    gpsLat = registro.gpsLat,
                    gpsLon = registro.gpsLon,
                    nota = registro.nota,
                    marcasCalculoJson = registro.marcasCalculoJson
                )
            }
            
            val request = SyncRequest(
                registros = registrosSync,
                deviceId = deviceId,
                idempotencyKey = "${deviceId}_${System.currentTimeMillis()}"
            )
            
            val response = api.syncAttendance(request)
            
            if (response.success) {
                // Actualizar registros como enviados
                response.processedRecords.forEach { recordId ->
                    database.registroAsistenciaDao().markAsSynced(recordId, response.serverTime)
                }
                
                // Actualizar skew del dispositivo
                val skew = System.currentTimeMillis() - response.serverTime
                database.dispositivoDao().updateSkew(deviceId, skew)
                
                // Marcar registros con error
                response.errors.forEach { error ->
                    database.registroAsistenciaDao().updateEstadoSync(error.recordId, EstadoSync.ERROR)
                }
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            // Marcar todos los registros como error
            registros.forEach { registro ->
                database.registroAsistenciaDao().updateEstadoSync(registro.id, EstadoSync.ERROR)
            }
            false
        }
    }
    
    private fun createApiService(endpoint: String, token: String): AsistenciaApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(endpoint)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(AsistenciaApi::class.java)
    }
    
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            applicationContext.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
}

class SyncManager(private val context: Context) {
    
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "sync_attendance",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }
    
    fun forceSyncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(syncRequest)
    }
    
    fun cancelSync() {
        WorkManager.getInstance(context).cancelUniqueWork("sync_attendance")
    }
}