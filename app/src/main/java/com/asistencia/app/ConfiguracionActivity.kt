package com.asistencia.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ConfiguracionActivity : AppCompatActivity() {
    
    private lateinit var configuracionManager: ConfiguracionManager
    private lateinit var tardanzasManager: TardanzasManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)
        
        configuracionManager = ConfiguracionManager(this)
        tardanzasManager = TardanzasManager(this)
        
        setupViews()
        loadCurrentConfiguration()
    }
    
    private fun setupViews() {
        // Tolerancia
        val seekBarTolerancia = findViewById<SeekBar>(R.id.seekBarTolerancia)
        val tvToleranciaValor = findViewById<TextView>(R.id.tvToleranciaValor)
        
        seekBarTolerancia.max = 60 // Máximo 60 minutos
        seekBarTolerancia.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvToleranciaValor.text = if (progress == 0) {
                    "Sin tolerancia (puntualidad estricta)"
                } else {
                    "$progress minutos de tolerancia"
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Límite de tardanzas
        val seekBarLimite = findViewById<SeekBar>(R.id.seekBarLimite)
        val tvLimiteValor = findViewById<TextView>(R.id.tvLimiteValor)
        
        seekBarLimite.max = 10 // Máximo 10 tardanzas por mes
        seekBarLimite.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvLimiteValor.text = if (progress == 0) {
                    "Sin límite de tardanzas"
                } else {
                    "$progress tardanzas máximo por mes"
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Switches
        val switchContarTardanzas = findViewById<Switch>(R.id.switchContarTardanzas)
        val switchNotificarTardanzas = findViewById<Switch>(R.id.switchNotificarTardanzas)
        
        // Botones
        findViewById<Button>(R.id.btnGuardarConfiguracion).setOnClickListener {
            guardarConfiguracion()
        }
        
        findViewById<Button>(R.id.btnRestaurarDefecto).setOnClickListener {
            restaurarConfiguracionDefecto()
        }
        
        findViewById<Button>(R.id.btnVerEstadisticas).setOnClickListener {
            mostrarEstadisticasTardanzas()
        }
    }
    
    private fun loadCurrentConfiguration() {
        findViewById<SeekBar>(R.id.seekBarTolerancia).progress = configuracionManager.toleranciaMinutos
        findViewById<SeekBar>(R.id.seekBarLimite).progress = configuracionManager.limiteTardanzasMes
        findViewById<Switch>(R.id.switchContarTardanzas).isChecked = configuracionManager.contarTardanzas
        findViewById<Switch>(R.id.switchNotificarTardanzas).isChecked = configuracionManager.notificarTardanzas
    }
    
    private fun guardarConfiguracion() {
        val tolerancia = findViewById<SeekBar>(R.id.seekBarTolerancia).progress
        val limite = findViewById<SeekBar>(R.id.seekBarLimite).progress
        val contarTardanzas = findViewById<Switch>(R.id.switchContarTardanzas).isChecked
        val notificarTardanzas = findViewById<Switch>(R.id.switchNotificarTardanzas).isChecked
        
        configuracionManager.toleranciaMinutos = tolerancia
        configuracionManager.limiteTardanzasMes = limite
        configuracionManager.contarTardanzas = contarTardanzas
        configuracionManager.notificarTardanzas = notificarTardanzas
        
        Toast.makeText(this, "✅ Configuración guardada", Toast.LENGTH_SHORT).show()
        
        // Mostrar resumen
        val resumen = StringBuilder()
        resumen.append("📋 Configuración actualizada:\n\n")
        resumen.append("⏰ ${configuracionManager.getDescripcionTolerancia()}\n")
        resumen.append("📊 ${configuracionManager.getDescripcionLimite()}\n")
        resumen.append("🔔 Notificaciones: ${if (notificarTardanzas) "Activadas" else "Desactivadas"}")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configuración Guardada")
            .setMessage(resumen.toString())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun restaurarConfiguracionDefecto() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Restaurar Configuración")
            .setMessage("¿Está seguro de restaurar la configuración por defecto?\n\n• Tolerancia: 15 minutos\n• Límite: 3 tardanzas por mes\n• Conteo y notificaciones activados")
            .setPositiveButton("Restaurar") { _, _ ->
                configuracionManager.toleranciaMinutos = 15
                configuracionManager.limiteTardanzasMes = 3
                configuracionManager.contarTardanzas = true
                configuracionManager.notificarTardanzas = true
                
                loadCurrentConfiguration()
                Toast.makeText(this, "✅ Configuración restaurada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun mostrarEstadisticasTardanzas() {
        // Obtener lista de personal
        val personalJson = getSharedPreferences("AsistenciaApp", MODE_PRIVATE)
            .getString("personal_list", "[]")
        val type = object : com.google.gson.reflect.TypeToken<List<Personal>>() {}.type
        val personalList: List<Personal> = com.google.gson.Gson().fromJson(personalJson, type) ?: emptyList()
        
        if (personalList.isEmpty()) {
            Toast.makeText(this, "No hay personal registrado para mostrar estadísticas", Toast.LENGTH_SHORT).show()
            return
        }
        
        val estadisticas = tardanzasManager.getEstadisticasTardanzas(personalList)
        val personalConProblemas = tardanzasManager.getPersonalQueExcedeLimite(personalList)
        
        val mensaje = StringBuilder()
        mensaje.append("📊 ESTADÍSTICAS DE TARDANZAS\n\n")
        
        if (personalConProblemas.isNotEmpty()) {
            mensaje.append("⚠️ PERSONAL QUE EXCEDE LÍMITE:\n")
            personalConProblemas.forEach { stat ->
                mensaje.append("• ${stat.nombre}: ${stat.tardanzasEsteMes} tardanzas este mes\n")
            }
            mensaje.append("\n")
        }
        
        mensaje.append("📈 TOP 5 CON MÁS TARDANZAS:\n")
        estadisticas.take(5).forEach { stat ->
            val indicador = if (stat.excedeLimite) "⚠️" else if (stat.tardanzasEsteMes > 0) "📊" else "✅"
            mensaje.append("$indicador ${stat.nombre}: ${stat.tardanzasEsteMes} este mes (${stat.tardanzasTotal} total)\n")
        }
        
        if (estadisticas.all { it.tardanzasTotal == 0 }) {
            mensaje.append("✅ ¡Excelente! No hay tardanzas registradas")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Estadísticas de Tardanzas")
            .setMessage(mensaje.toString())
            .setPositiveButton("Cerrar", null)
            .show()
    }
}