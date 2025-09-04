package com.asistencia.app

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.asistencia.app.workers.EmailWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AsistenciaApplication : Application(), Configuration.Provider {
    
    override fun onCreate() {
        super.onCreate()
        // Configuración global de la aplicación
        
        // Inicializar WorkManager para envío automático
        inicializarEnvioAutomatico()
    }
    
    private fun inicializarEnvioAutomatico() {
        // Programar envío automático al iniciar la app
        EmailWorker.programarEnvioAutomatico(this)
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
