package com.asistencia.app.utils

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileWriter
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class ReporteEmailSender(private val context: Context) {
    
    private val gson = Gson()
    
    data class ReporteDiario(
        val fecha: String,
        val totalRegistros: Int,
        val empleadosPresentes: Int,
        val tardanzas: Int,
        val registros: List<Map<String, String>>,
        val estadisticas: Map<String, Any>
    )
    
    interface EmailCallback {
        fun onSuccess(message: String)
        fun onError(error: String)
    }
    
    fun enviarReporteDiario(fecha: String, callback: EmailCallback) {
        EnviarEmailTask(fecha, callback).execute()
    }
    
    private inner class EnviarEmailTask(
        private val fecha: String,
        private val callback: EmailCallback
    ) : AsyncTask<Void, Void, Pair<Boolean, String>>() {
        
        override fun doInBackground(vararg params: Void?): Pair<Boolean, String> {
            return try {
                // 1. Generar reporte
                val reporte = generarReporteDiario(fecha)
                
                // 2. Verificar configuración
                val config = EmailConfigManager.loadEmailConfig(context)
                val destinatarios = EmailConfigManager.getDestinatariosActivos(context)
                
                if (!EmailConfigManager.isConfigComplete(context)) {
                    return Pair(false, "Configuración de email incompleta")
                }
                
                if (destinatarios.isEmpty()) {
                    return Pair(false, "No hay destinatarios configurados")
                }
                
                // 3. Configurar propiedades de email
                val props = Properties()
                props.put("mail.smtp.auth", "true")
                props.put("mail.smtp.starttls.enable", "true")
                props.put("mail.smtp.host", config.smtpServer)
                props.put("mail.smtp.port", config.smtpPort.toString())
                props.put("mail.smtp.ssl.trust", config.smtpServer)
                
                // 4. Crear sesión
                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(config.emailFrom, config.password)
                    }
                })
                
                // 5. Crear mensaje
                val message = MimeMessage(session)
                message.setFrom(InternetAddress(config.emailFrom))
                
                // Agregar destinatarios
                destinatarios.forEach { destinatario ->
                    message.addRecipient(Message.RecipientType.TO, InternetAddress(destinatario.email))
                }
                
                message.subject = "📊 Reporte Diario de Asistencia - $fecha"
                
                // 6. Crear contenido del email
                val multipart = MimeMultipart()
                
                // Contenido HTML
                val htmlContent = generarContenidoHTML(reporte)
                val htmlPart = MimeBodyPart()
                htmlPart.setContent(htmlContent, "text/html; charset=utf-8")
                multipart.addBodyPart(htmlPart)
                
                // Adjuntar CSV
                val csvFile = generarArchivoCSV(reporte)
                if (csvFile.exists()) {
                    val attachmentPart = MimeBodyPart()
                    attachmentPart.attachFile(csvFile)
                    attachmentPart.fileName = "reporte_asistencia_$fecha.csv"
                    multipart.addBodyPart(attachmentPart)
                }
                
                // Adjuntar Excel
                val excelFile = generarArchivoExcel(reporte)
                if (excelFile.exists()) {
                    val excelAttachmentPart = MimeBodyPart()
                    excelAttachmentPart.attachFile(excelFile)
                    excelAttachmentPart.fileName = "reporte_asistencia_$fecha.xlsx"
                    multipart.addBodyPart(excelAttachmentPart)
                }
                
                message.setContent(multipart)
                
                // 7. Enviar email
                Transport.send(message)
                
                Pair(true, "Reporte enviado exitosamente a ${destinatarios.size} destinatarios")
                
            } catch (e: Exception) {
                Log.e("ReporteEmailSender", "Error enviando email", e)
                Pair(false, "Error enviando email: ${e.message}")
            }
        }
        
        override fun onPostExecute(result: Pair<Boolean, String>) {
            if (result.first) {
                callback.onSuccess(result.second)
            } else {
                callback.onError(result.second)
            }
        }
    }
    
    private fun generarReporteDiario(fecha: String): ReporteDiario {
        // Cargar registros del día
        val sharedPreferences = context.getSharedPreferences("RegistrosApp", Context.MODE_PRIVATE)
        val registrosJson = sharedPreferences.getString("registros_list", "[]")
        val type = object : TypeToken<List<Map<String, String>>>() {}.type
        val todosRegistros: List<Map<String, String>> = gson.fromJson(registrosJson, type) ?: emptyList()
        
        // Filtrar registros del día
        val registrosDelDia = todosRegistros.filter { it["fecha"] == fecha }
        
        // Calcular estadísticas
        val empleadosUnicos = registrosDelDia.map { it["dni"] ?: "" }.distinct().filter { it.isNotEmpty() }
        val tardanzas = registrosDelDia.count { it["estado"]?.contains("TARDANZA") == true }
        
        val estadisticas = mapOf(
            "empleadosUnicos" to empleadosUnicos.size,
            "registrosEntrada" to registrosDelDia.count { it["tipoEvento"]?.contains("ENTRADA") == true },
            "registrosSalida" to registrosDelDia.count { it["tipoEvento"]?.contains("SALIDA") == true },
            "registrosRefrigerio" to registrosDelDia.count { it["tipoEvento"]?.contains("REFRIGERIO") == true }
        )
        
        return ReporteDiario(
            fecha = fecha,
            totalRegistros = registrosDelDia.size,
            empleadosPresentes = empleadosUnicos.size,
            tardanzas = tardanzas,
            registros = registrosDelDia,
            estadisticas = estadisticas
        )
    }
    
    private fun generarContenidoHTML(reporte: ReporteDiario): String {
        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(reporte.fecha) ?: Date()
        )
        
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                .header { background-color: #1a8a7a; color: white; padding: 20px; text-align: center; }
                .stats { display: flex; justify-content: space-around; margin: 20px 0; }
                .stat-box { background-color: #f5f5f5; padding: 15px; border-radius: 8px; text-align: center; }
                .stat-number { font-size: 24px; font-weight: bold; color: #1a8a7a; }
                .registros { margin: 20px 0; }
                .registro { background-color: #f9f9f9; padding: 10px; margin: 5px 0; border-left: 4px solid #1a8a7a; }
                .footer { background-color: #f5f5f5; padding: 15px; text-align: center; margin-top: 20px; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>📊 Reporte Diario de Asistencia</h1>
                <h2>$fechaFormateada</h2>
            </div>
            
            <div class="stats">
                <div class="stat-box">
                    <div class="stat-number">${reporte.totalRegistros}</div>
                    <div>Total Registros</div>
                </div>
                <div class="stat-box">
                    <div class="stat-number">${reporte.empleadosPresentes}</div>
                    <div>Empleados Presentes</div>
                </div>
                <div class="stat-box">
                    <div class="stat-number">${reporte.tardanzas}</div>
                    <div>Tardanzas</div>
                </div>
            </div>
            
            <div class="registros">
                <h3>📋 Detalle de Registros</h3>
                ${generarTablaRegistros(reporte.registros)}
            </div>
            
            <div class="footer">
                <p>📧 Este reporte fue generado automáticamente por el Sistema de Asistencia</p>
                <p>📎 Adjuntos: CSV y Excel con datos detallados</p>
                <p>🕐 Generado el: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}</p>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
    
    private fun generarTablaRegistros(registros: List<Map<String, String>>): String {
        if (registros.isEmpty()) {
            return "<p>No hay registros para mostrar</p>"
        }
        
        val tabla = StringBuilder()
        tabla.append("""
            <table style="width: 100%; border-collapse: collapse; margin: 10px 0;">
                <thead>
                    <tr style="background-color: #1a8a7a; color: white;">
                        <th style="padding: 10px; border: 1px solid #ddd;">Empleado</th>
                        <th style="padding: 10px; border: 1px solid #ddd;">Evento</th>
                        <th style="padding: 10px; border: 1px solid #ddd;">Hora</th>
                        <th style="padding: 10px; border: 1px solid #ddd;">Estado</th>
                    </tr>
                </thead>
                <tbody>
        """.trimIndent())
        
        registros.forEach { registro ->
            val nombre = registro["nombre"] ?: "N/A"
            val evento = registro["tipoEvento"] ?: "N/A"
            val hora = registro["hora"] ?: "N/A"
            val estado = registro["estado"] ?: "N/A"
            
            tabla.append("""
                <tr style="border-bottom: 1px solid #ddd;">
                    <td style="padding: 8px; border: 1px solid #ddd;">$nombre</td>
                    <td style="padding: 8px; border: 1px solid #ddd;">$evento</td>
                    <td style="padding: 8px; border: 1px solid #ddd;">$hora</td>
                    <td style="padding: 8px; border: 1px solid #ddd;">$estado</td>
                </tr>
            """.trimIndent())
        }
        
        tabla.append("</tbody></table>")
        return tabla.toString()
    }
    
    private fun generarArchivoCSV(reporte: ReporteDiario): File {
        val csvFile = File(context.cacheDir, "reporte_asistencia_${reporte.fecha}.csv")
        
        try {
            FileWriter(csvFile).use { writer ->
                // Encabezados
                writer.append("Empleado,DNI,Evento,Fecha,Hora,Estado,Ubicación\n")
                
                // Datos
                reporte.registros.forEach { registro ->
                    val nombre = registro["nombre"] ?: ""
                    val dni = registro["dni"] ?: ""
                    val evento = registro["tipoEvento"] ?: ""
                    val fecha = registro["fecha"] ?: ""
                    val hora = registro["hora"] ?: ""
                    val estado = registro["estado"] ?: ""
                    val ubicacion = registro["ubicacion"] ?: ""
                    
                    writer.append("$nombre,$dni,$evento,$fecha,$hora,$estado,$ubicacion\n")
                }
            }
        } catch (e: Exception) {
            Log.e("ReporteEmailSender", "Error generando CSV", e)
        }
        
        return csvFile
    }
    
    private fun generarArchivoExcel(reporte: ReporteDiario): File {
        val excelFile = File(context.cacheDir, "reporte_asistencia_${reporte.fecha}.xlsx")
        
        try {
            val workbook = XSSFWorkbook()
            
            // Crear hoja de registros
            val sheet = workbook.createSheet("Registros de Asistencia")
            
            // Crear estilos
            val headerStyle = workbook.createCellStyle()
            val headerFont = workbook.createFont()
            headerFont.setBold(true)
            headerFont.setColor(IndexedColors.WHITE.index)
            headerStyle.setFont(headerFont)
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.index)
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
            headerStyle.setBorderBottom(BorderStyle.THIN)
            headerStyle.setBorderTop(BorderStyle.THIN)
            headerStyle.setBorderRight(BorderStyle.THIN)
            headerStyle.setBorderLeft(BorderStyle.THIN)
            
            val dataStyle = workbook.createCellStyle()
            dataStyle.setBorderBottom(BorderStyle.THIN)
            dataStyle.setBorderTop(BorderStyle.THIN)
            dataStyle.setBorderRight(BorderStyle.THIN)
            dataStyle.setBorderLeft(BorderStyle.THIN)
            
            // Crear encabezados
            val headerRow = sheet.createRow(0)
            val headers = listOf("Empleado", "DNI", "Evento", "Fecha", "Hora", "Estado", "Ubicación")
            
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.setCellStyle(headerStyle)
            }
            
            // Agregar datos
            reporte.registros.forEachIndexed { index, registro ->
                val row = sheet.createRow(index + 1)
                
                val nombre = registro["nombre"] ?: ""
                val dni = registro["dni"] ?: ""
                val evento = registro["tipoEvento"] ?: ""
                val fecha = registro["fecha"] ?: ""
                val hora = registro["hora"] ?: ""
                val estado = registro["estado"] ?: ""
                val ubicacion = registro["ubicacion"] ?: ""
                
                row.createCell(0).setCellValue(nombre)
                row.createCell(1).setCellValue(dni)
                row.createCell(2).setCellValue(evento)
                row.createCell(3).setCellValue(fecha)
                row.createCell(4).setCellValue(hora)
                row.createCell(5).setCellValue(estado)
                row.createCell(6).setCellValue(ubicacion)
                
                // Aplicar estilo a todas las celdas de la fila
                (0..6).forEach { cellIndex ->
                    row.getCell(cellIndex).setCellStyle(dataStyle)
                }
            }
            
            // Crear hoja de estadísticas
            val statsSheet = workbook.createSheet("Estadísticas")
            val statsRow = statsSheet.createRow(0)
            val metricCell = statsRow.createCell(0)
            val valueCell = statsRow.createCell(1)
            metricCell.setCellValue("Métrica")
            valueCell.setCellValue("Valor")
            metricCell.setCellStyle(headerStyle)
            valueCell.setCellStyle(headerStyle)
            
            val statsData = listOf(
                "Total de Registros" to reporte.totalRegistros.toString(),
                "Empleados Presentes" to reporte.empleadosPresentes.toString(),
                "Tardanzas" to reporte.tardanzas.toString(),
                "Fecha del Reporte" to reporte.fecha,
                "Registros de Entrada" to (reporte.estadisticas["registrosEntrada"]?.toString() ?: "0"),
                "Registros de Salida" to (reporte.estadisticas["registrosSalida"]?.toString() ?: "0"),
                "Registros de Refrigerio" to (reporte.estadisticas["registrosRefrigerio"]?.toString() ?: "0")
            )
            
            statsData.forEachIndexed { index, (metrica, valor) ->
                val row = statsSheet.createRow(index + 1)
                val metricDataCell = row.createCell(0)
                val valueDataCell = row.createCell(1)
                metricDataCell.setCellValue(metrica)
                valueDataCell.setCellValue(valor)
                metricDataCell.setCellStyle(dataStyle)
                valueDataCell.setCellStyle(dataStyle)
            }
            
            // Autoajustar columnas
            (0..6).forEach { sheet.autoSizeColumn(it) }
            (0..1).forEach { statsSheet.autoSizeColumn(it) }
            
            // Guardar archivo
            FileOutputStream(excelFile).use { fileOut ->
                workbook.write(fileOut)
            }
            workbook.close()
            
        } catch (e: Exception) {
            Log.e("ReporteEmailSender", "Error generando Excel", e)
        }
        
        return excelFile
    }
}
