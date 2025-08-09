package com.asistencia.app.repository

import android.content.Context
import com.asistencia.app.business.ReglasAsistenciaEngine
import com.asistencia.app.database.*
import com.asistencia.app.sync.SyncManager
import com.asistencia.app.utils.HorarioUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.util.*

class AsistenciaRepository(context: Context) {
    
    private val database = AsistenciaDatabase.getDatabase(context)
    private val empleadoDao = database.empleadoDao()
    private val registroDao = database.registroAsistenciaDao()
    private val dispositivoDao = database.dispositivoDao()
    private val reglasEngine = ReglasAsistenciaEngine()
    private val syncManager = SyncManager(context)
    private val gson = Gson()
    
    private val deviceId = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ANDROID_ID
    )
    
    // Empleados
    fun getAllEmpleadosActivos(): Flow<List<Empleado>> = empleadoDao.getAllEmpleadosActivos()
    
    fun getAllEmpleados(): Flow<List<Empleado>> = empleadoDao.getAllEmpleados()
    
    suspend fun getEmpleadoById(id: String): Empleado? = empleadoDao.getEmpleadoById(id)
    
    suspend fun getEmpleadoByDni(dni: String): Empleado? = empleadoDao.getEmpleadoByDni(dni)
    
    suspend fun insertEmpleado(empleado: Empleado) = empleadoDao.insertEmpleado(empleado)
    
    suspend fun updateEmpleado(empleado: Empleado) = empleadoDao.updateEmpleado(empleado)
    
    suspend fun deactivateEmpleado(id: String) = empleadoDao.deactivateEmpleado(id)
    
    suspend fun activateEmpleado(id: String) = empleadoDao.activateEmpleado(id)
    
    suspend fun deleteEmpleado(empleado: Empleado) = empleadoDao.deleteEmpleado(empleado)
    
    suspend fun getEmpleadoByDniIncludeInactive(dni: String): Empleado? = empleadoDao.getEmpleadoByDniIncludeInactive(dni)
    
    fun searchEmpleados(query: String): Flow<List<Empleado>> = empleadoDao.searchEmpleados(query)
    
    // Registros de asistencia
    fun getRegistrosByFecha(fecha: String): Flow<List<RegistroAsistencia>> = 
        registroDao.getRegistrosByFecha(fecha)
    
    fun getRegistrosByEmpleado(empleadoId: String): Flow<List<RegistroAsistencia>> = 
        registroDao.getRegistrosByEmpleado(empleadoId)
    
    suspend fun getRegistrosByEmpleadoAndFecha(empleadoId: String, fecha: String): List<RegistroAsistencia> = 
        registroDao.getRegistrosByEmpleadoAndFecha(empleadoId, fecha)
    
    // Registro principal de asistencia
    suspend fun registrarAsistencia(
        empleadoIdentificador: String, // Puede ser ID o DNI
        tipoEvento: TipoEvento,
        modoLectura: ModoLectura,
        rawCode: String,
        gpsLat: Double? = null,
        gpsLon: Double? = null,
        nota: String? = null
    ): ResultadoRegistro {
        
        try {
            // Buscar empleado por ID o DNI
            val empleado = getEmpleadoByDni(empleadoIdentificador) 
                ?: getEmpleadoById(empleadoIdentificador)
                ?: return ResultadoRegistro.Error("Empleado no encontrado")
            
            if (!empleado.activo) {
                return ResultadoRegistro.Error("Empleado inactivo")
            }
            
            val timestamp = System.currentTimeMillis()
            val fecha = HorarioUtils.getCurrentDateString()
            
            // Verificar duplicados
            val duplicados = registroDao.checkDuplicado(empleado.id, tipoEvento, timestamp)
            if (duplicados > 0) {
                return ResultadoRegistro.Error("Registro duplicado detectado")
            }
            
            // Obtener registros del día
            val registrosDelDia = registroDao.getRegistrosByEmpleadoAndFecha(empleado.id, fecha)
            
            // Validar y calcular usando el motor de reglas
            val validacion = reglasEngine.validarYCalcularRegistro(
                empleado, tipoEvento, timestamp, registrosDelDia
            )
            
            if (!validacion.esValido) {
                return ResultadoRegistro.Error(validacion.mensaje)
            }
            
            // Crear registro
            val registro = RegistroAsistencia(
                empleadoId = empleado.id,
                fecha = fecha,
                tipoEvento = tipoEvento,
                timestampDispositivo = timestamp,
                deviceId = deviceId,
                modoLectura = modoLectura,
                rawCode = rawCode,
                gpsLat = gpsLat,
                gpsLon = gpsLon,
                nota = nota,
                marcasCalculoJson = gson.toJson(validacion.marcasCalculo)
            )
            
            // Guardar en base de datos
            registroDao.insertRegistro(registro)
            
            // Programar sincronización
            syncManager.forceSyncNow()
            
            return ResultadoRegistro.Exito(
                mensaje = validacion.mensaje,
                empleado = empleado,
                registro = registro,
                proximoEvento = validacion.proximoEventoEsperado
            )
            
        } catch (e: Exception) {
            return ResultadoRegistro.Error("Error interno: ${e.message}")
        }
    }
    
    // Dispositivo
    suspend fun getDispositivo(): Dispositivo {
        return dispositivoDao.getDispositivo(deviceId) ?: run {
            val nuevoDispositivo = Dispositivo(deviceId = deviceId)
            dispositivoDao.insertDispositivo(nuevoDispositivo)
            nuevoDispositivo
        }
    }
    
    suspend fun updateDispositivo(dispositivo: Dispositivo) = dispositivoDao.updateDispositivo(dispositivo)
    
    suspend fun updateModoLectura(modo: ModoLectura) = 
        dispositivoDao.updateModoLectura(deviceId, modo)
    
    suspend fun updateCapturaUbicacion(captura: Boolean) = 
        dispositivoDao.updateCapturaUbicacion(deviceId, captura)
    
    suspend fun updateApiConfig(endpoint: String?, token: String?) = 
        dispositivoDao.updateApiConfig(deviceId, endpoint, token)
    
    // Sincronización
    fun iniciarSincronizacionPeriodica() = syncManager.schedulePeriodicSync()
    
    fun forzarSincronizacion() = syncManager.forceSyncNow()
    
    suspend fun getCountRegistrosPendientes(): Int = registroDao.getCountRegistrosPendientes()
    
    // Estadísticas y reportes
    suspend fun getEstadisticasDelDia(fecha: String): EstadisticasDia {
        val registros = registroDao.getRegistrosByFecha(fecha)
        // Implementar cálculo de estadísticas
        return EstadisticasDia(
            fecha = fecha,
            totalRegistros = 0,
            empleadosPresentes = 0,
            tardanzas = 0,
            registrosPendientesSync = getCountRegistrosPendientes()
        )
    }
    
    suspend fun calcularHorasTrabajadasEmpleado(empleadoId: String, fecha: String): Int {
        val registros = getRegistrosByEmpleadoAndFecha(empleadoId, fecha)
        return reglasEngine.calcularHorasTrabajadas(registros)
    }
    
    // Debug: Verificar si empleado existe
    suspend fun verificarEmpleadoExiste(dni: String): String {
        val empleadoPorDni = getEmpleadoByDni(dni)
        val empleadoPorId = getEmpleadoById(dni)
        
        return buildString {
            append("Búsqueda para: $dni\n")
            append("Por DNI: ${if (empleadoPorDni != null) "ENCONTRADO (${empleadoPorDni.nombres} ${empleadoPorDni.apellidos})" else "NO ENCONTRADO"}\n")
            append("Por ID: ${if (empleadoPorId != null) "ENCONTRADO (${empleadoPorId.nombres} ${empleadoPorId.apellidos})" else "NO ENCONTRADO"}\n")
            append("Total empleados activos: ${empleadoDao.getActiveEmployeeCount()}")
        }
    }
    
    // Determinar próximo evento esperado
    suspend fun determinarProximoEvento(empleadoId: String): TipoEvento? {
        val fecha = HorarioUtils.getCurrentDateString()
        val registros = getRegistrosByEmpleadoAndFecha(empleadoId, fecha)
        
        val eventosExistentes = registros.map { it.tipoEvento }.toSet()
        
        return when {
            !eventosExistentes.contains(TipoEvento.ENTRADA_TURNO) -> TipoEvento.ENTRADA_TURNO
            !eventosExistentes.contains(TipoEvento.SALIDA_REFRIGERIO) -> TipoEvento.SALIDA_REFRIGERIO
            !eventosExistentes.contains(TipoEvento.ENTRADA_POST_REFRIGERIO) -> TipoEvento.ENTRADA_POST_REFRIGERIO
            !eventosExistentes.contains(TipoEvento.SALIDA_TURNO) -> TipoEvento.SALIDA_TURNO
            else -> null
        }
    }
}

sealed class ResultadoRegistro {
    data class Exito(
        val mensaje: String,
        val empleado: Empleado,
        val registro: RegistroAsistencia,
        val proximoEvento: TipoEvento?
    ) : ResultadoRegistro()
    
    data class Error(val mensaje: String) : ResultadoRegistro()
}

data class EstadisticasDia(
    val fecha: String,
    val totalRegistros: Int,
    val empleadosPresentes: Int,
    val tardanzas: Int,
    val registrosPendientesSync: Int
)