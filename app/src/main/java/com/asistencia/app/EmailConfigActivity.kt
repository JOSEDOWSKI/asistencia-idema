package com.asistencia.app

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.asistencia.app.utils.EmailConfig
import com.asistencia.app.utils.EmailConfigManager
import com.asistencia.app.utils.EmailDestinatario
import com.asistencia.app.utils.PinManager
import com.asistencia.app.workers.EmailWorker
import java.util.Properties

class EmailConfigActivity : AppCompatActivity() {
    
    private lateinit var etSmtpServer: EditText
    private lateinit var etSmtpPort: EditText
    private lateinit var etEmailFrom: EditText
    private lateinit var etPassword: EditText
    private lateinit var switchSSL: Switch
    private lateinit var switchEnvioAutomatico: Switch
    private lateinit var etHoraEnvio: EditText
    private lateinit var btnTestConexion: Button
    private lateinit var btnGuardarConfig: Button
    private lateinit var btnAgregarDestinatario: Button
    private lateinit var destinatariosList: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_config)
        
        setupViews()
        loadCurrentConfiguration()
        
        supportActionBar?.title = "📧 Configuración de Email"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Registrar actividad para el sistema de PIN
        PinManager.updateLastActivity(this)
    }
    
    private fun setupViews() {
        etSmtpServer = findViewById(R.id.et_smtp_server)
        etSmtpPort = findViewById(R.id.et_smtp_port)
        etEmailFrom = findViewById(R.id.et_email_from)
        etPassword = findViewById(R.id.et_password)
        switchSSL = findViewById(R.id.switch_ssl)
        switchEnvioAutomatico = findViewById(R.id.switch_envio_automatico)
        etHoraEnvio = findViewById(R.id.et_hora_envio)
        btnTestConexion = findViewById(R.id.btn_test_conexion_email)
        btnGuardarConfig = findViewById(R.id.btn_guardar_config_email)
        btnAgregarDestinatario = findViewById(R.id.btn_agregar_destinatario)
        destinatariosList = findViewById(R.id.destinatarios_list)
        
        // Configurar campo de contraseña
        etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        
        // Configurar campo de hora
        etHoraEnvio.hint = "HH:MM (ej: 18:00)"
        
        // Setup listeners
        btnTestConexion.setOnClickListener { testearConexionEmail() }
        btnGuardarConfig.setOnClickListener { guardarConfiguracionEmail() }
        btnAgregarDestinatario.setOnClickListener { mostrarDialogoAgregarDestinatario() }
        
        // Configurar switch de envío automático
        switchEnvioAutomatico.setOnCheckedChangeListener { _, isChecked ->
            etHoraEnvio.isEnabled = isChecked
        }
    }
    
    private fun loadCurrentConfiguration() {
        val config = EmailConfigManager.loadEmailConfig(this)
        
        etSmtpServer.setText(config.smtpServer)
        etSmtpPort.setText(config.smtpPort.toString())
        etEmailFrom.setText(config.emailFrom)
        etPassword.setText(config.password)
        switchSSL.isChecked = config.useSSL
        switchEnvioAutomatico.isChecked = config.enviarAutomatico
        etHoraEnvio.setText(config.horaEnvio)
        
        etHoraEnvio.isEnabled = config.enviarAutomatico
        
        // Cargar destinatarios
        cargarDestinatarios()
    }
    
    private fun cargarDestinatarios() {
        destinatariosList.removeAllViews()
        
        val destinatarios = EmailConfigManager.loadDestinatarios(this)
        
        if (destinatarios.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No hay destinatarios configurados\n\nToca 'Agregar Destinatario' para comenzar"
                textSize = 14f
                setPadding(16, 16, 16, 16)
                setTextColor(android.graphics.Color.GRAY)
                gravity = android.view.Gravity.CENTER
            }
            destinatariosList.addView(emptyText)
        } else {
            destinatarios.forEach { destinatario ->
                val destinatarioView = crearDestinatarioView(destinatario)
                destinatariosList.addView(destinatarioView)
            }
        }
    }
    
    private fun crearDestinatarioView(destinatario: EmailDestinatario): View {
        val cardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundResource(R.drawable.item_background)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }
        
        val nombreText = TextView(this).apply {
            text = "👤 ${destinatario.nombre}"
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val emailText = TextView(this).apply {
            text = "📧 ${destinatario.email}"
            textSize = 14f
            setTextColor(android.graphics.Color.GRAY)
        }
        
        val tipoText = TextView(this).apply {
            text = "🏷️ ${destinatario.tipo.capitalize()}"
            textSize = 12f
            setTextColor(android.graphics.Color.GRAY)
        }
        
        val estadoText = TextView(this).apply {
            text = if (destinatario.activo) "✅ Activo" else "❌ Inactivo"
            textSize = 12f
            setTextColor(if (destinatario.activo) android.graphics.Color.GREEN else android.graphics.Color.RED)
        }
        
        val btnEliminar = Button(this).apply {
            text = "🗑️ Eliminar"
            textSize = 12f
            setBackgroundResource(R.drawable.button_secondary_selector)
            setTextColor(android.graphics.Color.RED)
            setOnClickListener {
                confirmarEliminarDestinatario(destinatario)
            }
        }
        
        cardView.addView(nombreText)
        cardView.addView(emailText)
        cardView.addView(tipoText)
        cardView.addView(estadoText)
        cardView.addView(btnEliminar)
        
        return cardView
    }
    
    private fun confirmarEliminarDestinatario(destinatario: EmailDestinatario) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Eliminar Destinatario")
            .setMessage("¿Está seguro de eliminar a ${destinatario.nombre} (${destinatario.email})?")
            .setPositiveButton("Eliminar") { _, _ ->
                EmailConfigManager.removeDestinatario(this, destinatario.email)
                cargarDestinatarios()
                Toast.makeText(this, "✅ Destinatario eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun mostrarDialogoAgregarDestinatario() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        val etNombre = EditText(this).apply {
            hint = "Nombre completo"
            inputType = InputType.TYPE_CLASS_TEXT
            textSize = 16f
        }
        dialogLayout.addView(etNombre)
        
        val etEmail = EditText(this).apply {
            hint = "Correo electrónico"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            textSize = 16f
        }
        dialogLayout.addView(etEmail)
        
        val etTipo = EditText(this).apply {
            hint = "Tipo (empleador, supervisor, etc.)"
            inputType = InputType.TYPE_CLASS_TEXT
            textSize = 16f
        }
        dialogLayout.addView(etTipo)
        
        AlertDialog.Builder(this)
            .setTitle("👤 Agregar Destinatario")
            .setView(dialogLayout)
            .setPositiveButton("Agregar") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val tipo = etTipo.text.toString().trim()
                
                if (nombre.isNotEmpty() && email.isNotEmpty()) {
                    val destinatario = EmailDestinatario(
                        nombre = nombre,
                        email = email,
                        tipo = if (tipo.isNotEmpty()) tipo else "empleador"
                    )
                    
                    EmailConfigManager.addDestinatario(this, destinatario)
                    cargarDestinatarios()
                    Toast.makeText(this, "✅ Destinatario agregado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "❌ Complete todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun testearConexionEmail() {
        val smtpServer = etSmtpServer.text.toString().trim()
        val smtpPort = etSmtpPort.text.toString().toIntOrNull() ?: 587
        val emailFrom = etEmailFrom.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val useSSL = switchSSL.isChecked
        
        if (smtpServer.isEmpty() || emailFrom.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "❌ Complete todos los campos primero", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Mostrar progreso
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("🔧 Probando Conexión")
            .setMessage("Verificando configuración SMTP...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        // Ejecutar test en background
        Thread {
            try {
                val props = Properties()
                props.put("mail.smtp.auth", "true")
                props.put("mail.smtp.starttls.enable", "true")
                props.put("mail.smtp.host", smtpServer)
                props.put("mail.smtp.port", smtpPort.toString())
                props.put("mail.smtp.ssl.trust", smtpServer)
                
                // Configuración adicional para diferentes tipos de servidores
                if (useSSL) {
                    if (smtpPort == 465) {
                        props.put("mail.smtp.socketFactory.port", "465")
                        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                        props.put("mail.smtp.socketFactory.fallback", "false")
                    }
                }
                
                val session = javax.mail.Session.getInstance(props, object : javax.mail.Authenticator() {
                    override fun getPasswordAuthentication(): javax.mail.PasswordAuthentication {
                        return javax.mail.PasswordAuthentication(emailFrom, password)
                    }
                })
                
                val message = javax.mail.internet.MimeMessage(session)
                message.setFrom(javax.mail.internet.InternetAddress(emailFrom))
                message.setRecipients(javax.mail.Message.RecipientType.TO, javax.mail.internet.InternetAddress.parse(emailFrom))
                message.subject = "🧪 Test de Configuración - App Asistencia"
                message.setText("""
                    ✅ CONFIGURACIÓN SMTP EXITOSA
                    
                    Servidor: $smtpServer:$smtpPort
                    Email: $emailFrom
                    SSL: ${if (useSSL) "Activado" else "Desactivado"}
                    
                    Este es un email de prueba para verificar que la configuración SMTP funciona correctamente.
                    
                    🕐 Enviado: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
                """.trimIndent())
                
                javax.mail.Transport.send(message)
                
                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog.Builder(this@EmailConfigActivity)
                        .setTitle("✅ Conexión Exitosa")
                        .setMessage("""
                            🎉 ¡Configuración SMTP correcta!
                            
                            ✅ Servidor: $smtpServer:$smtpPort
                            ✅ Email: $emailFrom
                            ✅ SSL: ${if (useSSL) "Activado" else "Desactivado"}
                            
                            Se envió un email de prueba a $emailFrom
                            Revisa tu bandeja de entrada para confirmar.
                        """.trimIndent())
                        .setPositiveButton("OK", null)
                        .show()
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog.Builder(this@EmailConfigActivity)
                        .setTitle("❌ Error de Conexión")
                        .setMessage("""
                            Error al conectar con el servidor SMTP:
                            
                            ❌ ${e.message}
                            
                            🔧 Verifica:
                            • Servidor SMTP correcto
                            • Puerto correcto
                            • Email y contraseña válidos
                            • Conexión a internet
                            • Configuración SSL
                            
                            💡 Sugerencias:
                            • Prueba sin SSL (puerto 25)
                            • Verifica contraseña de aplicación
                            • Comprueba configuración del servidor
                        """.trimIndent())
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }.start()
    }
    
    private fun guardarConfiguracionEmail() {
        val smtpServer = etSmtpServer.text.toString().trim()
        val smtpPort = etSmtpPort.text.toString().toIntOrNull() ?: 587
        val emailFrom = etEmailFrom.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val useSSL = switchSSL.isChecked
        val enviarAutomatico = switchEnvioAutomatico.isChecked
        val horaEnvio = etHoraEnvio.text.toString().trim()
        
        if (smtpServer.isEmpty() || emailFrom.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "❌ Complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }
        
        val config = EmailConfig(
            smtpServer = smtpServer,
            smtpPort = smtpPort,
            emailFrom = emailFrom,
            password = password,
            useSSL = useSSL,
            enviarAutomatico = enviarAutomatico,
            horaEnvio = if (horaEnvio.isNotEmpty()) horaEnvio else "18:00"
        )
        
        EmailConfigManager.saveEmailConfig(this, config)
        
        // Si el envío automático está activado, programarlo
        if (config.enviarAutomatico) {
            EmailWorker.programarEnvioAutomatico(this)
        }
        
        Toast.makeText(this, "✅ Configuración guardada", Toast.LENGTH_SHORT).show()
        
        mostrarResumenConfiguracion(config)
    }
    
    private fun mostrarResumenConfiguracion(config: EmailConfig) {
        val destinatarios = EmailConfigManager.loadDestinatarios(this)
        
        val mensaje = """
            ✅ CONFIGURACIÓN DE EMAIL GUARDADA
            
            📧 Servidor SMTP: ${config.smtpServer}:${config.smtpPort}
            📤 Email desde: ${config.emailFrom}
            🔒 SSL: ${if (config.useSSL) "Activado" else "Desactivado"}
            
            📋 Destinatarios: ${destinatarios.size}
            ${destinatarios.take(3).joinToString("\n") { "• ${it.nombre} (${it.email})" }}
            ${if (destinatarios.size > 3) "\n... y ${destinatarios.size - 3} más" else ""}
            
            ⏰ Envío automático: ${if (config.enviarAutomatico) "Activado a las ${config.horaEnvio}" else "Desactivado"}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Configuración Guardada")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
