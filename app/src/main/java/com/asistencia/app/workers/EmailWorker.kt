package com.asistencia.app.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.asistencia.app.utils.EmailConfigManager
import com.asistencia.app.utils.ReporteEmailSender
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class EmailWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "EmailWorker"
        private const val WORK_NAME = "email_automatico"
        
        // Programar envío automático
        fun programarEnvioAutomatico(context: Context) {
            val config = EmailConfigManager.loadEmailConfig(context)
            
            if (!config.enviarAutomatico) {
                Log.d(TAG, "Envío automático desactivado")
                return
            }
            
            // Calcular próxima hora de envío
            val proximaHora = calcularProximaHoraEnvio(config.horaEnvio)
            
            // Crear constraints
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            // Crear request
            val request = PeriodicWorkRequestBuilder<EmailWorker>(
                1, TimeUnit.DAYS // Repetir cada día
            )
                .setConstraints(constraints)
                .setInitialDelay(proximaHora, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()
            
            // Programar trabajo
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
            
            Log.d(TAG, "Envío automático programado para: ${config.horaEnvio}")
        }
        
        // Cancelar envío automático
        fun cancelarEnvioAutomatico(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_NAME)
            Log.d(TAG, "Envío automático cancelado")
        }
        
        // Calcular próxima hora de envío
        private fun calcularProximaHoraEnvio(horaEnvio: String): Long {
            val calendar = Calendar.getInstance()
            val ahora = calendar.timeInMillis
            
            // Parsear hora de envío (formato HH:MM)
            val partes = horaEnvio.split(":")
            val hora = partes[0].toInt()
            val minuto = partes[1].toInt()
            
            // Configurar hora de envío para hoy
            calendar.set(Calendar.HOUR_OF_DAY, hora)
            calendar.set(Calendar.MINUTE, minuto)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            val horaEnvioHoy = calendar.timeInMillis
            
            // Si ya pasó la hora de hoy, programar para mañana
            return if (horaEnvioHoy > ahora) {
                horaEnvioHoy - ahora
            } else {
                // Mañana a la misma hora
                horaEnvioHoy + (24 * 60 * 60 * 1000) - ahora
            }
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Iniciando envío automático de reporte")
            
            // Verificar configuración
            val config = EmailConfigManager.loadEmailConfig(applicationContext)
            if (!config.enviarAutomatico) {
                Log.d(TAG, "Envío automático desactivado")
                return Result.success()
            }
            
            // Verificar si es día laborable
            val calendar = Calendar.getInstance()
            val diaSemana = calendar.get(Calendar.DAY_OF_WEEK)
            
            // Solo enviar de lunes a viernes (2=DOMINGO, 2=LUNES, ..., 7=SÁBADO)
            if (diaSemana == Calendar.SUNDAY || diaSemana == Calendar.SATURDAY) {
                Log.d(TAG, "No es día laborable, saltando envío")
                return Result.success()
            }
            
            // Verificar si hay destinatarios
            val destinatarios = EmailConfigManager.getDestinatariosActivos(applicationContext)
            if (destinatarios.isEmpty()) {
                Log.d(TAG, "No hay destinatarios configurados")
                return Result.success()
            }
            
            // Obtener fecha actual
            val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            
            // Enviar reporte
            val emailSender = ReporteEmailSender(applicationContext)
            var enviadoExitosamente = false
            
            emailSender.enviarReporteDiario(fechaActual, object : ReporteEmailSender.EmailCallback {
                override fun onSuccess(message: String) {
                    Log.d(TAG, "Reporte enviado exitosamente: $message")
                    enviadoExitosamente = true
                }
                
                override fun onError(error: String) {
                    Log.e(TAG, "Error enviando reporte: $error")
                    enviadoExitosamente = false
                }
            })
            
            // Esperar un poco para que se complete el envío
            kotlinx.coroutines.delay(5000)
            
            if (enviadoExitosamente) {
                Log.d(TAG, "Envío automático completado exitosamente")
                Result.success()
            } else {
                Log.e(TAG, "Error en envío automático")
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en EmailWorker", e)
            Result.failure()
        }
    }
}
