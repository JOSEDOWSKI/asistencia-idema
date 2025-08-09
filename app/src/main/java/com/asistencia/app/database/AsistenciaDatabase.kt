package com.asistencia.app.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [
        Empleado::class,
        RegistroAsistencia::class,
        Dispositivo::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AsistenciaDatabase : RoomDatabase() {
    
    abstract fun empleadoDao(): EmpleadoDao
    abstract fun registroAsistenciaDao(): RegistroAsistenciaDao
    abstract fun dispositivoDao(): DispositivoDao
    
    companion object {
        @Volatile
        private var INSTANCE: AsistenciaDatabase? = null
        
        fun getDatabase(context: Context): AsistenciaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AsistenciaDatabase::class.java,
                    "asistencia_database_v2"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migración desde la versión anterior
                // Crear las nuevas tablas
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS empleados (
                        id TEXT PRIMARY KEY NOT NULL,
                        dni TEXT NOT NULL,
                        nombres TEXT NOT NULL,
                        apellidos TEXT NOT NULL,
                        area TEXT,
                        activo INTEGER NOT NULL DEFAULT 1,
                        tipoHorario TEXT NOT NULL DEFAULT 'REGULAR',
                        horaEntradaRegular TEXT,
                        horaSalidaRegular TEXT,
                        refrigerioInicioRegular TEXT,
                        refrigerioFinRegular TEXT,
                        horarioFlexibleJson TEXT,
                        refrigerioFlexibleJson TEXT,
                        fechaCreacion INTEGER NOT NULL DEFAULT 0,
                        fechaActualizacion INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS registros_asistencia (
                        id TEXT PRIMARY KEY NOT NULL,
                        empleadoId TEXT NOT NULL,
                        fecha TEXT NOT NULL,
                        tipoEvento TEXT NOT NULL,
                        timestampDispositivo INTEGER NOT NULL,
                        timestampServidor INTEGER,
                        deviceId TEXT NOT NULL,
                        modoLectura TEXT NOT NULL,
                        rawCode TEXT NOT NULL,
                        gpsLat REAL,
                        gpsLon REAL,
                        nota TEXT,
                        estadoSync TEXT NOT NULL DEFAULT 'PENDIENTE',
                        marcasCalculoJson TEXT,
                        fechaCreacion INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(empleadoId) REFERENCES empleados(id) ON DELETE CASCADE
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS dispositivo (
                        deviceId TEXT PRIMARY KEY NOT NULL,
                        operadorPinHash TEXT,
                        skewMs INTEGER NOT NULL DEFAULT 0,
                        modoOperacion TEXT NOT NULL DEFAULT 'PUESTO_FIJO',
                        modoLectura TEXT NOT NULL DEFAULT 'QR',
                        capturaUbicacion INTEGER NOT NULL DEFAULT 0,
                        modoOffline INTEGER NOT NULL DEFAULT 1,
                        apiEndpoint TEXT,
                        apiToken TEXT,
                        fechaActualizacion INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // Migrar datos existentes si los hay
                try {
                    // Migrar personal a empleados
                    database.execSQL("""
                        INSERT INTO empleados (id, dni, nombres, apellidos, horaEntradaRegular, horaSalidaRegular, fechaCreacion, fechaActualizacion)
                        SELECT 
                            lower(hex(randomblob(16))),
                            dni,
                            nombre,
                            '',
                            horaEntrada,
                            horaSalida,
                            ${System.currentTimeMillis()},
                            ${System.currentTimeMillis()}
                        FROM personal
                    """)
                } catch (e: Exception) {
                    // Tabla personal no existe, continuar
                }
            }
        }
    }
}