package com.asistencia.app

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PersonalActivity : AppCompatActivity() {
    
    private lateinit var personalAdapter: PersonalAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private var personalList = mutableListOf<Personal>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal)
        
        sharedPreferences = getSharedPreferences("AsistenciaApp", Context.MODE_PRIVATE)
        
        setupViews()
        setupRecyclerView()
        loadPersonalData()
    }
    
    private fun setupViews() {
        findViewById<Button>(R.id.btnAgregarPersonal).setOnClickListener {
            showAddPersonalDialog()
        }
    }
    
    private fun setupRecyclerView() {
        try {
            personalAdapter = PersonalAdapter { personal ->
                showPersonalOptionsDialog(personal)
            }
            
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewPersonal)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = personalAdapter
        } catch (e: Exception) {
            Toast.makeText(this, "Error al configurar la lista: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun loadPersonalData() {
        try {
            val personalJson = sharedPreferences.getString("personal_list", "[]")
            val type = object : TypeToken<List<Personal>>() {}.type
            personalList = gson.fromJson(personalJson, type) ?: mutableListOf()
            personalAdapter.submitList(personalList.toList())
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
            personalList = mutableListOf()
            personalAdapter.submitList(personalList.toList())
        }
    }
    
    private fun savePersonalData() {
        val personalJson = gson.toJson(personalList)
        sharedPreferences.edit().putString("personal_list", personalJson).apply()
    }
    
    private fun showAddPersonalDialog() {
        showPersonalDialogMejorado(null)
    }
    
    private fun showPersonalDialogMejorado(personalExistente: Personal?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_personal_mejorado, null)
        
        // Referencias a elementos del diálogo
        val etDni = dialogView.findViewById<EditText>(R.id.etDni)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombre)
        
        // Switch de tipo de horario
        val switchTipoHorario = dialogView.findViewById<Switch>(R.id.switchTipoHorario)
        val tvTipoHorarioDescripcion = dialogView.findViewById<TextView>(R.id.tvTipoHorarioDescripcion)
        
        // Elementos de horario fijo
        val etHoraEntrada = dialogView.findViewById<EditText>(R.id.etHoraEntrada)
        val etHoraSalida = dialogView.findViewById<EditText>(R.id.etHoraSalida)
        
        // Elementos de horario flexible
        val layoutHorarioFlexible = dialogView.findViewById<LinearLayout>(R.id.layoutHorarioFlexible)
        val etHoraEntradaBase = dialogView.findViewById<EditText>(R.id.etHoraEntradaBase)
        val etHoraSalidaBase = dialogView.findViewById<EditText>(R.id.etHoraSalidaBase)
        val btnAplicarLunesViernes = dialogView.findViewById<Button>(R.id.btnAplicarLunesViernes)
        val btnAplicarLunesSabado = dialogView.findViewById<Button>(R.id.btnAplicarLunesSabado)
        
        // Campos individuales por día
        val etLunesEntrada = dialogView.findViewById<EditText>(R.id.etLunesEntrada)
        val etLunesSalida = dialogView.findViewById<EditText>(R.id.etLunesSalida)
        val etMartesEntrada = dialogView.findViewById<EditText>(R.id.etMartesEntrada)
        val etMartesSalida = dialogView.findViewById<EditText>(R.id.etMartesSalida)
        val etMiercolesEntrada = dialogView.findViewById<EditText>(R.id.etMiercolesEntrada)
        val etMiercolesSalida = dialogView.findViewById<EditText>(R.id.etMiercolesSalida)
        val etJuevesEntrada = dialogView.findViewById<EditText>(R.id.etJuevesEntrada)
        val etJuevesSalida = dialogView.findViewById<EditText>(R.id.etJuevesSalida)
        val etViernesEntrada = dialogView.findViewById<EditText>(R.id.etViernesEntrada)
        val etViernesSalida = dialogView.findViewById<EditText>(R.id.etViernesSalida)
        val etSabadoEntrada = dialogView.findViewById<EditText>(R.id.etSabadoEntrada)
        val etSabadoSalida = dialogView.findViewById<EditText>(R.id.etSabadoSalida)
        
        // Elementos de refrigerio
        val cbTieneRefrigerio = dialogView.findViewById<CheckBox>(R.id.cbTieneRefrigerio)
        val layoutRefrigerio = dialogView.findViewById<LinearLayout>(R.id.layoutRefrigerio)
        val etInicioRefrigerio = dialogView.findViewById<EditText>(R.id.etInicioRefrigerio)
        val etFinRefrigerio = dialogView.findViewById<EditText>(R.id.etFinRefrigerio)
        val tvDuracionRefrigerio = dialogView.findViewById<TextView>(R.id.tvDuracionRefrigerio)
        
        // Elementos de resumen
        val tvHorasTotales = dialogView.findViewById<TextView>(R.id.tvHorasTotales)
        val tvHorasRefrigerio = dialogView.findViewById<TextView>(R.id.tvHorasRefrigerio)
        val tvHorasTrabajadas = dialogView.findViewById<TextView>(R.id.tvHorasTrabajadas)
        
        // Configurar Switch de tipo de horario
        switchTipoHorario.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Horario flexible activado
                layoutHorarioFlexible.visibility = LinearLayout.VISIBLE
                tvTipoHorarioDescripcion.text = "✅ Horario Flexible: Configuración individual por día"
                Toast.makeText(this, "✅ Horario Flexible: Podrás configurar cada día individualmente", Toast.LENGTH_SHORT).show()
            } else {
                // Horario regular activado
                layoutHorarioFlexible.visibility = LinearLayout.GONE
                tvTipoHorarioDescripcion.text = "📅 Horario Regular: Lunes a Viernes con mismo horario"
                Toast.makeText(this, "📅 Horario Regular: Mismo horario de lunes a viernes", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Configurar botón de aplicar Lunes a Viernes
        btnAplicarLunesViernes.setOnClickListener {
            val entradaBase = etHoraEntradaBase.text.toString().trim()
            val salidaBase = etHoraSalidaBase.text.toString().trim()
            
            if (entradaBase.isEmpty() || salidaBase.isEmpty()) {
                Toast.makeText(this, "Complete los horarios base primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!validarFormatoHora(entradaBase) || !validarFormatoHora(salidaBase)) {
                Toast.makeText(this, "Formato de hora inválido. Use HH:mm", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Aplicar horarios de Lunes a Viernes
            etLunesEntrada.setText(entradaBase)
            etLunesSalida.setText(salidaBase)
            etMartesEntrada.setText(entradaBase)
            etMartesSalida.setText(salidaBase)
            etMiercolesEntrada.setText(entradaBase)
            etMiercolesSalida.setText(salidaBase)
            etJuevesEntrada.setText(entradaBase)
            etJuevesSalida.setText(salidaBase)
            etViernesEntrada.setText(entradaBase)
            etViernesSalida.setText(salidaBase)
            
            Toast.makeText(this, "✅ Horario aplicado de Lunes a Viernes", Toast.LENGTH_SHORT).show()
        }
        
        // Configurar botón de aplicar Lunes a Sábado
        btnAplicarLunesSabado.setOnClickListener {
            val entradaBase = etHoraEntradaBase.text.toString().trim()
            val salidaBase = etHoraSalidaBase.text.toString().trim()
            
            if (entradaBase.isEmpty() || salidaBase.isEmpty()) {
                Toast.makeText(this, "Complete los horarios base primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!validarFormatoHora(entradaBase) || !validarFormatoHora(salidaBase)) {
                Toast.makeText(this, "Formato de hora inválido. Use HH:mm", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Aplicar horarios de Lunes a Sábado
            etLunesEntrada.setText(entradaBase)
            etLunesSalida.setText(salidaBase)
            etMartesEntrada.setText(entradaBase)
            etMartesSalida.setText(salidaBase)
            etMiercolesEntrada.setText(entradaBase)
            etMiercolesSalida.setText(salidaBase)
            etJuevesEntrada.setText(entradaBase)
            etJuevesSalida.setText(salidaBase)
            etViernesEntrada.setText(entradaBase)
            etViernesSalida.setText(salidaBase)
            etSabadoEntrada.setText(entradaBase)
            etSabadoSalida.setText(salidaBase)
            
            Toast.makeText(this, "✅ Horario aplicado de Lunes a Sábado", Toast.LENGTH_SHORT).show()
        }
        
        // Configurar eventos de refrigerio
        configurarEventosDialogo(
            cbTieneRefrigerio, layoutRefrigerio, etHoraEntrada, etHoraSalida,
            etInicioRefrigerio, etFinRefrigerio, tvDuracionRefrigerio,
            tvHorasTotales, tvHorasRefrigerio, tvHorasTrabajadas
        )
        
        // Si es edición, cargar datos existentes
        personalExistente?.let { personal ->
            etDni.setText(personal.dni)
            etDni.isEnabled = false
            etNombre.setText(personal.nombre)
            etHoraEntrada.setText(personal.horaEntrada)
            etHoraSalida.setText(personal.horaSalida)
            
            // Configurar el switch según el tipo de horario
            switchTipoHorario.isChecked = personal.tipoHorario == "VARIABLE"
            if (personal.tipoHorario == "VARIABLE") {
                layoutHorarioFlexible.visibility = LinearLayout.VISIBLE
                etHoraEntradaBase.setText(personal.horaEntrada)
                etHoraSalidaBase.setText(personal.horaSalida)
                
                // Cargar horarios individuales por día si existen
                etLunesEntrada.setText(personal.horaEntrada)
                etLunesSalida.setText(personal.horaSalida)
                etMartesEntrada.setText(personal.horaEntrada)
                etMartesSalida.setText(personal.horaSalida)
                etMiercolesEntrada.setText(personal.horaEntrada)
                etMiercolesSalida.setText(personal.horaSalida)
                etJuevesEntrada.setText(personal.horaEntrada)
                etJuevesSalida.setText(personal.horaSalida)
                etViernesEntrada.setText(personal.horaEntrada)
                etViernesSalida.setText(personal.horaSalida)
                etSabadoEntrada.setText(personal.horaEntrada)
                etSabadoSalida.setText(personal.horaSalida)
            } else {
                layoutHorarioFlexible.visibility = LinearLayout.GONE
            }
            
            cbTieneRefrigerio.isChecked = personal.tieneRefrigerio
            if (personal.tieneRefrigerio) {
                etInicioRefrigerio.setText(personal.inicioRefrigerio)
                etFinRefrigerio.setText(personal.finRefrigerio)
            }
            
            actualizarResumenHoras(
                etHoraEntrada, etHoraSalida, cbTieneRefrigerio,
                etInicioRefrigerio, etFinRefrigerio,
                tvHorasTotales, tvHorasRefrigerio, tvHorasTrabajadas
            )
        }
        
        // Mostrar diálogo
        AlertDialog.Builder(this)
            .setTitle(if (personalExistente == null) "Agregar Personal" else "Editar Personal")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                guardarPersonalConTipoHorario(
                    etDni, etNombre, switchTipoHorario,
                    etHoraEntrada, etHoraSalida,
                    cbTieneRefrigerio, etInicioRefrigerio, etFinRefrigerio,
                    personalExistente,
                    // Campos individuales por día
                    etLunesEntrada, etLunesSalida,
                    etMartesEntrada, etMartesSalida,
                    etMiercolesEntrada, etMiercolesSalida,
                    etJuevesEntrada, etJuevesSalida,
                    etViernesEntrada, etViernesSalida,
                    etSabadoEntrada, etSabadoSalida
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showPersonalOptionsDialog(personal: Personal) {
        AlertDialog.Builder(this)
            .setTitle("Opciones para ${personal.nombre}")
            .setItems(arrayOf("Ver Historial", "Editar Datos", "Eliminar")) { _, which ->
                when (which) {
                    0 -> showHistorialDialog(personal)
                    1 -> showEditPersonalDialog(personal)
                    2 -> showDeletePersonalDialog(personal)
                }
            }
            .show()
    }
    
    private fun showEditPersonalDialog(personal: Personal) {
        // Usar el diálogo mejorado para edición
        showPersonalDialogMejorado(personal)
    }
    
    private fun showHistorialDialog(personal: Personal) {
        val asistenciaManager = AsistenciaManager(this)
        val registros = asistenciaManager.getRegistrosByDni(personal.dni)
        
        if (registros.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Historial de ${personal.nombre}")
                .setMessage("No hay registros de asistencia para este empleado.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        // Crear el texto del historial de forma simple
        val historialText = StringBuilder()
        historialText.append("📊 Historial de Asistencia\n")
        historialText.append("👤 ${personal.nombre} (DNI: ${personal.dni})\n")
        historialText.append("⏰ Horario: ${personal.getDescripcionHorariosCompleta()}\n\n")
        
        registros.forEach { registro ->
            val emoji = if (registro.tipo == "ENTRADA") "📥" else "📤"
            val estado = if (registro.llegadaTarde) " ⚠️ TARDE" else ""
            
            historialText.append("$emoji ${registro.tipo}\n")
            historialText.append("📅 ${registro.diaSemana}, ${registro.fecha}\n")
            historialText.append("🕐 ${registro.hora}$estado\n\n")
        }
        
        // Crear ScrollView para el diálogo
        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            text = historialText.toString()
            setPadding(50, 30, 50, 30)
            textSize = 14f
        }
        scrollView.addView(textView)
        
        AlertDialog.Builder(this)
            .setTitle("Historial de Asistencia")
            .setView(scrollView)
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    private fun showDeletePersonalDialog(personal: Personal) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Personal")
            .setMessage("¿Está seguro de eliminar a ${personal.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                personalList.removeAll { it.dni == personal.dni }
                savePersonalData()
                personalAdapter.submitList(personalList.toList())
                Toast.makeText(this, "❌ Personal eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showHorariosPorDiaDialog(personal: Personal) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_horarios_por_dia, null)
            
            // Referencias a elementos del diálogo
            val tvNombreEmpleado = dialogView.findViewById<TextView>(R.id.tvNombreEmpleado)
            val tvDniEmpleado = dialogView.findViewById<TextView>(R.id.tvDniEmpleado)
            val switchHorarioVariable = dialogView.findViewById<Switch>(R.id.switchHorarioVariable)
            val layoutHorarioFijo = dialogView.findViewById<LinearLayout>(R.id.layoutHorarioFijo)
            val layoutHorarioVariable = dialogView.findViewById<LinearLayout>(R.id.layoutHorarioVariable)
            val tvResumenSemanal = dialogView.findViewById<TextView>(R.id.tvResumenSemanal)
            
            // Verificar que los elementos se encontraron correctamente
            if (switchHorarioVariable == null) {
                Toast.makeText(this, "Error: No se pudo encontrar el switch de horario variable", Toast.LENGTH_LONG).show()
                return
            }
            
            // Configurar información del empleado
            tvNombreEmpleado.text = personal.nombre
            tvDniEmpleado.text = "DNI: ${personal.dni}"
            
            // Configurar switch de horario variable
            switchHorarioVariable.isChecked = personal.tipoHorario == "VARIABLE"
            
            // Configurar visibilidad inicial
            if (personal.tipoHorario == "VARIABLE") {
                layoutHorarioFijo.visibility = LinearLayout.GONE
                layoutHorarioVariable.visibility = LinearLayout.VISIBLE
            } else {
                layoutHorarioFijo.visibility = LinearLayout.VISIBLE
                layoutHorarioVariable.visibility = LinearLayout.GONE
            }
            
            // Configurar eventos del switch
            switchHorarioVariable.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    layoutHorarioFijo.visibility = LinearLayout.GONE
                    layoutHorarioVariable.visibility = LinearLayout.VISIBLE
                } else {
                    layoutHorarioFijo.visibility = LinearLayout.VISIBLE
                    layoutHorarioVariable.visibility = LinearLayout.GONE
                }
            }
            
            // Configurar horario fijo
            val etHoraEntradaFijo = dialogView.findViewById<EditText>(R.id.etHoraEntradaFijo)
            val etHoraSalidaFijo = dialogView.findViewById<EditText>(R.id.etHoraSalidaFijo)
            val cbRefrigerioFijo = dialogView.findViewById<CheckBox>(R.id.cbRefrigerioFijo)
            val etInicioRefrigerioFijo = dialogView.findViewById<EditText>(R.id.etInicioRefrigerioFijo)
            val etFinRefrigerioFijo = dialogView.findViewById<EditText>(R.id.etFinRefrigerioFijo)
            
            // Cargar datos de horario fijo
            etHoraEntradaFijo.setText(personal.horaEntrada)
            etHoraSalidaFijo.setText(personal.horaSalida)
            cbRefrigerioFijo.isChecked = personal.tieneRefrigerio
            etInicioRefrigerioFijo.setText(personal.inicioRefrigerio)
            etFinRefrigerioFijo.setText(personal.finRefrigerio)
            
            // Configurar días de la semana para horario variable
            val diasSemana = listOf(
                Triple("Lunes", R.id.layoutLunes, "lunes"),
                Triple("Martes", R.id.layoutMartes, "martes"),
                Triple("Miércoles", R.id.layoutMiercoles, "miercoles"),
                Triple("Jueves", R.id.layoutJueves, "jueves"),
                Triple("Viernes", R.id.layoutViernes, "viernes"),
                Triple("Sábado", R.id.layoutSabado, "sabado"),
                Triple("Domingo", R.id.layoutDomingo, "domingo")
            )
            
            val diasConfigurados = mutableMapOf<String, DiaConfiguracion>()
            
            // Configurar cada día
            diasSemana.forEach { (nombreDia, layoutId, claveDia) ->
                val diaLayout = dialogView.findViewById<LinearLayout>(layoutId)
                if (diaLayout != null) {
                    configurarDiaHorario(diaLayout, nombreDia, claveDia, diasConfigurados, personal)
                }
            }
            
            // Configurar eventos del switch (actualizado)
            switchHorarioVariable.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    layoutHorarioFijo.visibility = LinearLayout.GONE
                    layoutHorarioVariable.visibility = LinearLayout.VISIBLE
                    Toast.makeText(this, "Modo: Horario Variable por Día", Toast.LENGTH_SHORT).show()
                } else {
                    layoutHorarioFijo.visibility = LinearLayout.VISIBLE
                    layoutHorarioVariable.visibility = LinearLayout.GONE
                    Toast.makeText(this, "Modo: Horario Fijo", Toast.LENGTH_SHORT).show()
                }
                actualizarResumenSemanal(tvResumenSemanal, isChecked, personal, diasConfigurados)
            }
            
            // Actualizar resumen inicial
            actualizarResumenSemanal(tvResumenSemanal, switchHorarioVariable.isChecked, personal, diasConfigurados)
            
            // Mostrar diálogo completo
            AlertDialog.Builder(this)
                .setTitle("⏰ Horarios por Día - ${personal.nombre}")
                .setView(dialogView)
                .setPositiveButton("💾 Guardar") { _, _ ->
                    guardarHorariosPorDia(
                        personal, switchHorarioVariable.isChecked,
                        etHoraEntradaFijo, etHoraSalidaFijo, cbRefrigerioFijo,
                        etInicioRefrigerioFijo, etFinRefrigerioFijo,
                        diasConfigurados
                    )
                }
                .setNeutralButton("📋 Copiar Horario") { _, _ ->
                    mostrarOpcionesCopiarHorario(personal, diasConfigurados)
                }
                .setNegativeButton("Cancelar", null)
                .show()
                
        } catch (e: Exception) {
            Toast.makeText(this, "Error al mostrar diálogo de horarios: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun configurarDiaHorario(
        diaLayout: LinearLayout,
        nombreDia: String,
        claveDia: String,
        diasConfigurados: MutableMap<String, DiaConfiguracion>,
        personal: Personal
    ) {
        // Referencias a elementos del día
        val cbDiaActivo = diaLayout.findViewById<CheckBox>(R.id.cbDiaActivo)
        val tvNombreDia = diaLayout.findViewById<TextView>(R.id.tvNombreDia)
        val layoutHorarios = diaLayout.findViewById<LinearLayout>(R.id.layoutHorarios)
        val etEntrada = diaLayout.findViewById<EditText>(R.id.etEntrada)
        val etSalida = diaLayout.findViewById<EditText>(R.id.etSalida)
        val cbTieneRefrigerio = diaLayout.findViewById<CheckBox>(R.id.cbTieneRefrigerio)
        val layoutRefrigerio = diaLayout.findViewById<LinearLayout>(R.id.layoutRefrigerio)
        val etInicioRefrigerio = diaLayout.findViewById<EditText>(R.id.etInicioRefrigerio)
        val etFinRefrigerio = diaLayout.findViewById<EditText>(R.id.etFinRefrigerio)
        val tvHorasCalculadas = diaLayout.findViewById<TextView>(R.id.tvHorasCalculadas)
        
        // Configurar nombre del día
        tvNombreDia.text = nombreDia
        
        // Cargar datos existentes
        val horarioDia = personal.getHorarioDia(claveDia)
        cbDiaActivo.isChecked = horarioDia.activo
        etEntrada.setText(horarioDia.entrada)
        etSalida.setText(horarioDia.salida)
        
        // Configurar refrigerio
        cbTieneRefrigerio.isChecked = horarioDia.tieneRefrigerio
        etInicioRefrigerio.setText(horarioDia.inicioRefrigerio)
        etFinRefrigerio.setText(horarioDia.finRefrigerio)
        
        // Crear configuración del día
        val diaConfig = DiaConfiguracion(
            cbDiaActivo = cbDiaActivo,
            tvHorasCalculadas = tvHorasCalculadas,
            layoutHorarios = layoutHorarios,
            etEntrada = etEntrada,
            etSalida = etSalida,
            cbTieneRefrigerio = cbTieneRefrigerio,
            layoutRefrigerio = layoutRefrigerio,
            etInicioRefrigerio = etInicioRefrigerio,
            etFinRefrigerio = etFinRefrigerio
        )
        
        diasConfigurados[claveDia] = diaConfig
        
        // Configurar eventos
        configurarEventosDiaHorario(diaConfig)
        
        // Actualizar visibilidad inicial
        layoutHorarios.visibility = if (cbDiaActivo.isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
        layoutRefrigerio.visibility = if (cbTieneRefrigerio.isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
        
        // Calcular horas iniciales
        actualizarHorasCalculadasDia(diaConfig)
    }
    
    private fun configurarEventosDiaHorario(config: DiaConfiguracion) {
        // Activar/desactivar día
        config.cbDiaActivo.setOnCheckedChangeListener { _, isChecked ->
            config.layoutHorarios.visibility = if (isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
            actualizarHorasCalculadasDia(config)
        }
        
        // Mostrar/ocultar refrigerio
        config.cbTieneRefrigerio.setOnCheckedChangeListener { _, isChecked ->
            config.layoutRefrigerio.visibility = if (isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
            actualizarHorasCalculadasDia(config)
        }
        
        // TextWatcher para actualizar cálculos
        val textWatcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                actualizarHorasCalculadasDia(config)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        
        config.etEntrada.addTextChangedListener(textWatcher)
        config.etSalida.addTextChangedListener(textWatcher)
        config.etInicioRefrigerio.addTextChangedListener(textWatcher)
        config.etFinRefrigerio.addTextChangedListener(textWatcher)
    }
    
    private fun actualizarHorasCalculadasDia(config: DiaConfiguracion) {
        if (!config.cbDiaActivo.isChecked) {
            config.tvHorasCalculadas.text = "❌ No trabaja"
            config.tvHorasCalculadas.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            return
        }
        
        try {
            val entrada = config.etEntrada.text.toString()
            val salida = config.etSalida.text.toString()
            
            if (entrada.isEmpty() || salida.isEmpty()) {
                config.tvHorasCalculadas.text = "⚠️ Configurar"
                config.tvHorasCalculadas.setTextColor(ContextCompat.getColor(this, R.color.warning_color))
                return
            }
            
            val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val entradaTime = formato.parse(entrada)
            val salidaTime = formato.parse(salida)
            
            if (entradaTime != null && salidaTime != null) {
                var totalMinutos = ((salidaTime.time - entradaTime.time) / (1000 * 60)).toInt()
                
                // Descontar refrigerio si está configurado
                if (config.cbTieneRefrigerio.isChecked) {
                    val inicioRef = config.etInicioRefrigerio.text.toString()
                    val finRef = config.etFinRefrigerio.text.toString()
                    
                    if (inicioRef.isNotEmpty() && finRef.isNotEmpty()) {
                        val inicioRefTime = formato.parse(inicioRef)
                        val finRefTime = formato.parse(finRef)
                        
                        if (inicioRefTime != null && finRefTime != null) {
                            val minutosRef = ((finRefTime.time - inicioRefTime.time) / (1000 * 60)).toInt()
                            totalMinutos -= minutosRef
                        }
                    }
                }
                
                if (totalMinutos > 0) {
                    val horas = totalMinutos / 60
                    val minutos = totalMinutos % 60
                    config.tvHorasCalculadas.text = "✅ ${horas}h ${minutos}m"
                    config.tvHorasCalculadas.setTextColor(ContextCompat.getColor(this, R.color.success_color))
                } else {
                    config.tvHorasCalculadas.text = "⚠️ Inválido"
                    config.tvHorasCalculadas.setTextColor(ContextCompat.getColor(this, R.color.error_color))
                }
            } else {
                config.tvHorasCalculadas.text = "⚠️ Formato"
                config.tvHorasCalculadas.setTextColor(ContextCompat.getColor(this, R.color.error_color))
            }
        } catch (e: Exception) {
            config.tvHorasCalculadas.text = "❌ Error"
            config.tvHorasCalculadas.setTextColor(ContextCompat.getColor(this, R.color.error_color))
        }
    }
    
    private fun actualizarResumenSemanal(
        tvResumen: TextView,
        esHorarioVariable: Boolean,
        personal: Personal,
        diasConfigurados: Map<String, DiaConfiguracion>
    ) {
        if (!esHorarioVariable) {
            val (horas, minutos) = personal.calcularHorasTrabajadasConRefrigerio()
            tvResumen.text = "📊 Horario fijo: ${horas}h ${minutos}m diarios"
            tvResumen.setTextColor(ContextCompat.getColor(this, R.color.primary_color))
            return
        }
        
        var totalMinutosSemana = 0
        var diasActivos = 0
        
        diasConfigurados.forEach { (_, config) ->
            if (config.cbDiaActivo.isChecked) {
                diasActivos++
                
                val entrada = config.etEntrada.text.toString()
                val salida = config.etSalida.text.toString()
                
                if (entrada.isNotEmpty() && salida.isNotEmpty()) {
                    try {
                        val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        val entradaTime = formato.parse(entrada)
                        val salidaTime = formato.parse(salida)
                        
                        if (entradaTime != null && salidaTime != null) {
                            var minutosDelDia = ((salidaTime.time - entradaTime.time) / (1000 * 60)).toInt()
                            
                            // Descontar refrigerio
                            if (config.cbTieneRefrigerio.isChecked) {
                                val inicioRef = config.etInicioRefrigerio.text.toString()
                                val finRef = config.etFinRefrigerio.text.toString()
                                
                                if (inicioRef.isNotEmpty() && finRef.isNotEmpty()) {
                                    val inicioRefTime = formato.parse(inicioRef)
                                    val finRefTime = formato.parse(finRef)
                                    
                                    if (inicioRefTime != null && finRefTime != null) {
                                        val minutosRef = ((finRefTime.time - inicioRefTime.time) / (1000 * 60)).toInt()
                                        minutosDelDia -= minutosRef
                                    }
                                }
                            }
                            
                            if (minutosDelDia > 0) {
                                totalMinutosSemana += minutosDelDia
                            }
                        }
                    } catch (e: Exception) {
                        // Ignorar errores de formato
                    }
                }
            }
        }
        
        val horasSemana = totalMinutosSemana / 60
        val minutosSemana = totalMinutosSemana % 60
        
        tvResumen.text = "📊 Total semanal: ${horasSemana}h ${minutosSemana}m ($diasActivos días activos)"
        tvResumen.setTextColor(ContextCompat.getColor(this, R.color.success_color))
    }  
  private fun guardarHorariosPorDia(
        personal: Personal,
        esHorarioVariable: Boolean,
        etHoraEntradaFijo: EditText,
        etHoraSalidaFijo: EditText,
        cbRefrigerioFijo: CheckBox,
        etInicioRefrigerioFijo: EditText,
        etFinRefrigerioFijo: EditText,
        diasConfigurados: Map<String, DiaConfiguracion>
    ) {
        try {
            val personalActualizado = if (esHorarioVariable) {
                // Validar que al menos un día esté activo
                val diasActivos = diasConfigurados.values.count { it.cbDiaActivo.isChecked }
                if (diasActivos == 0) {
                    Toast.makeText(this, "Debe configurar al menos un día de trabajo", Toast.LENGTH_SHORT).show()
                    return
                }
                
                // Crear horario semanal
                val horarioSemanal = HorarioSemanal(
                    lunes = crearHorarioDiaDesdeConfig(diasConfigurados["lunes"]),
                    martes = crearHorarioDiaDesdeConfig(diasConfigurados["martes"]),
                    miercoles = crearHorarioDiaDesdeConfig(diasConfigurados["miercoles"]),
                    jueves = crearHorarioDiaDesdeConfig(diasConfigurados["jueves"]),
                    viernes = crearHorarioDiaDesdeConfig(diasConfigurados["viernes"]),
                    sabado = crearHorarioDiaDesdeConfig(diasConfigurados["sabado"]),
                    domingo = crearHorarioDiaDesdeConfig(diasConfigurados["domingo"])
                )
                
                personal.copy(
                    tipoHorario = "VARIABLE",
                    horarioSemanal = horarioSemanal
                )
            } else {
                // Horario fijo
                val horaEntrada = etHoraEntradaFijo.text.toString().trim()
                val horaSalida = etHoraSalidaFijo.text.toString().trim()
                val tieneRefrigerio = cbRefrigerioFijo.isChecked
                val inicioRefrigerio = etInicioRefrigerioFijo.text.toString().trim()
                val finRefrigerio = etFinRefrigerioFijo.text.toString().trim()
                
                if (horaEntrada.isEmpty() || horaSalida.isEmpty()) {
                    Toast.makeText(this, "Complete los horarios de entrada y salida", Toast.LENGTH_SHORT).show()
                    return
                }
                
                if (!validarFormatoHora(horaEntrada) || !validarFormatoHora(horaSalida)) {
                    Toast.makeText(this, "Formato de hora inválido. Use HH:mm", Toast.LENGTH_SHORT).show()
                    return
                }
                
                val minutosRefrigerio = if (tieneRefrigerio && inicioRefrigerio.isNotEmpty() && finRefrigerio.isNotEmpty()) {
                    if (!validarFormatoHora(inicioRefrigerio) || !validarFormatoHora(finRefrigerio)) {
                        Toast.makeText(this, "Formato de hora de refrigerio inválido", Toast.LENGTH_SHORT).show()
                        return
                    }
                    
                    if (!validarRefrigerioEnHorario(horaEntrada, horaSalida, inicioRefrigerio, finRefrigerio)) {
                        Toast.makeText(this, "El refrigerio debe estar dentro del horario laboral", Toast.LENGTH_SHORT).show()
                        return
                    }
                    
                    try {
                        val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        val inicioTime = formato.parse(inicioRefrigerio)
                        val finTime = formato.parse(finRefrigerio)
                        
                        if (inicioTime != null && finTime != null) {
                            val minutos = ((finTime.time - inicioTime.time) / (1000 * 60)).toInt()
                            if (minutos <= 0) {
                                Toast.makeText(this, "El horario de refrigerio es inválido", Toast.LENGTH_SHORT).show()
                                return
                            }
                            minutos
                        } else 60
                    } catch (e: Exception) {
                        60
                    }
                } else 60
                
                personal.copy(
                    tipoHorario = "FIJO",
                    horaEntrada = horaEntrada,
                    horaSalida = horaSalida,
                    tieneRefrigerio = tieneRefrigerio,
                    inicioRefrigerio = if (tieneRefrigerio) inicioRefrigerio else "12:00",
                    finRefrigerio = if (tieneRefrigerio) finRefrigerio else "13:00",
                    minutosRefrigerio = minutosRefrigerio
                )
            }
            
            // Actualizar en la lista
            val index = personalList.indexOfFirst { it.dni == personal.dni }
            if (index != -1) {
                personalList[index] = personalActualizado
                savePersonalData()
                personalAdapter.submitList(personalList.toList())
                
                val tipoHorario = if (esHorarioVariable) "variable" else "fijo"
                Toast.makeText(this, "✅ Horarios actualizados: ${personal.nombre} ($tipoHorario)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar horarios: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun crearHorarioDiaDesdeConfig(config: DiaConfiguracion?): HorarioDia {
        if (config == null || !config.cbDiaActivo.isChecked) {
            return HorarioDia(activo = false)
        }
        
        val entrada = config.etEntrada.text.toString().trim()
        val salida = config.etSalida.text.toString().trim()
        
        if (entrada.isEmpty() || salida.isEmpty()) {
            return HorarioDia(activo = false)
        }
        
        val tieneRefrigerio = config.cbTieneRefrigerio.isChecked
        val inicioRefrigerio = config.etInicioRefrigerio.text.toString().trim().ifEmpty { "12:00" }
        val finRefrigerio = config.etFinRefrigerio.text.toString().trim().ifEmpty { "13:00" }
        
        // Calcular minutos de refrigerio
        val minutosRefrigerio = if (tieneRefrigerio && inicioRefrigerio.isNotEmpty() && finRefrigerio.isNotEmpty()) {
            try {
                val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val inicioTime = formato.parse(inicioRefrigerio)
                val finTime = formato.parse(finRefrigerio)
                
                if (inicioTime != null && finTime != null) {
                    val minutos = ((finTime.time - inicioTime.time) / (1000 * 60)).toInt()
                    if (minutos > 0) minutos else 60
                } else 60
            } catch (e: Exception) {
                60
            }
        } else 60
        
        return HorarioDia(
            entrada = entrada,
            salida = salida,
            activo = true,
            esHorarioPartido = false,
            tieneRefrigerio = tieneRefrigerio,
            inicioRefrigerio = inicioRefrigerio,
            finRefrigerio = finRefrigerio,
            minutosRefrigerio = minutosRefrigerio,
            aplicarCompensacion = true
        )
    }
    
    private fun mostrarOpcionesCopiarHorario(
        personal: Personal,
        diasConfigurados: Map<String, DiaConfiguracion>
    ) {
        AlertDialog.Builder(this)
            .setTitle("📋 Copiar Horario")
            .setMessage("Seleccione una opción para copiar horarios:")
            .setPositiveButton("📅 Copiar Lunes a todos") { _, _ ->
                copiarHorarioLunesATodos(diasConfigurados)
            }
            .setNeutralButton("🔄 Copiar horario fijo") { _, _ ->
                copiarHorarioFijoATodos(personal, diasConfigurados)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun copiarHorarioLunesATodos(diasConfigurados: Map<String, DiaConfiguracion>) {
        val configLunes = diasConfigurados["lunes"]
        if (configLunes == null || !configLunes.cbDiaActivo.isChecked) {
            Toast.makeText(this, "Configure primero el horario del lunes", Toast.LENGTH_SHORT).show()
            return
        }
        
        val entrada = configLunes.etEntrada.text.toString()
        val salida = configLunes.etSalida.text.toString()
        val tieneRefrigerio = configLunes.cbTieneRefrigerio.isChecked
        val inicioRef = configLunes.etInicioRefrigerio.text.toString()
        val finRef = configLunes.etFinRefrigerio.text.toString()
        
        if (entrada.isEmpty() || salida.isEmpty()) {
            Toast.makeText(this, "Complete el horario del lunes primero", Toast.LENGTH_SHORT).show()
            return
        }
        
        diasConfigurados.forEach { (dia, config) ->
            if (dia != "lunes" && dia != "domingo") { // No copiar al domingo
                config.cbDiaActivo.isChecked = true
                config.etEntrada.setText(entrada)
                config.etSalida.setText(salida)
                config.cbTieneRefrigerio.isChecked = tieneRefrigerio
                config.etInicioRefrigerio.setText(inicioRef)
                config.etFinRefrigerio.setText(finRef)
                
                // Actualizar visibilidad
                config.layoutHorarios.visibility = LinearLayout.VISIBLE
                config.layoutRefrigerio.visibility = if (tieneRefrigerio) LinearLayout.VISIBLE else LinearLayout.GONE
                
                actualizarHorasCalculadasDia(config)
            }
        }
        
        Toast.makeText(this, "✅ Horario del lunes copiado a todos los días laborales", Toast.LENGTH_SHORT).show()
    }
    
    private fun copiarHorarioFijoATodos(
        personal: Personal,
        diasConfigurados: Map<String, DiaConfiguracion>
    ) {
        diasConfigurados.forEach { (dia, config) ->
            if (dia != "domingo") { // No configurar domingo por defecto
                config.cbDiaActivo.isChecked = true
                config.etEntrada.setText(personal.horaEntrada)
                config.etSalida.setText(personal.horaSalida)
                config.cbTieneRefrigerio.isChecked = personal.tieneRefrigerio
                config.etInicioRefrigerio.setText(personal.inicioRefrigerio)
                config.etFinRefrigerio.setText(personal.finRefrigerio)
                
                // Actualizar visibilidad
                config.layoutHorarios.visibility = LinearLayout.VISIBLE
                config.layoutRefrigerio.visibility = if (personal.tieneRefrigerio) LinearLayout.VISIBLE else LinearLayout.GONE
                
                actualizarHorasCalculadasDia(config)
            }
        }
        
        Toast.makeText(this, "✅ Horario fijo copiado a todos los días laborales", Toast.LENGTH_SHORT).show()
    }
    
    // Clase auxiliar para manejar la configuración de cada día
    private data class DiaConfiguracion(
        val cbDiaActivo: CheckBox,
        val tvHorasCalculadas: TextView,
        val layoutHorarios: LinearLayout,
        val etEntrada: EditText,
        val etSalida: EditText,
        val cbTieneRefrigerio: CheckBox,
        val layoutRefrigerio: LinearLayout,
        val etInicioRefrigerio: EditText,
        val etFinRefrigerio: EditText
    )    private
 fun configurarEventosDialogo(
        cbTieneRefrigerio: CheckBox,
        layoutRefrigerio: LinearLayout,
        etHoraEntrada: EditText,
        etHoraSalida: EditText,
        etInicioRefrigerio: EditText,
        etFinRefrigerio: EditText,
        tvDuracionRefrigerio: TextView,
        tvHorasTotales: TextView,
        tvHorasRefrigerio: TextView,
        tvHorasTrabajadas: TextView
    ) {
        // Mostrar/ocultar configuración de refrigerio
        cbTieneRefrigerio.setOnCheckedChangeListener { _, isChecked ->
            layoutRefrigerio.visibility = if (isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
            actualizarResumenHoras(
                etHoraEntrada, etHoraSalida, cbTieneRefrigerio,
                etInicioRefrigerio, etFinRefrigerio,
                tvHorasTotales, tvHorasRefrigerio, tvHorasTrabajadas
            )
        }
        
        // TextWatcher para actualizar cálculos en tiempo real
        val textWatcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                actualizarDuracionRefrigerio(etInicioRefrigerio, etFinRefrigerio, tvDuracionRefrigerio)
                actualizarResumenHoras(
                    etHoraEntrada, etHoraSalida, cbTieneRefrigerio,
                    etInicioRefrigerio, etFinRefrigerio,
                    tvHorasTotales, tvHorasRefrigerio, tvHorasTrabajadas
                )
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        
        etHoraEntrada.addTextChangedListener(textWatcher)
        etHoraSalida.addTextChangedListener(textWatcher)
        etInicioRefrigerio.addTextChangedListener(textWatcher)
        etFinRefrigerio.addTextChangedListener(textWatcher)
    }
    
    private fun actualizarDuracionRefrigerio(
        etInicioRefrigerio: EditText,
        etFinRefrigerio: EditText,
        tvDuracionRefrigerio: TextView
    ) {
        try {
            val inicio = etInicioRefrigerio.text.toString()
            val fin = etFinRefrigerio.text.toString()
            
            if (inicio.isNotEmpty() && fin.isNotEmpty()) {
                val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val inicioTime = formato.parse(inicio)
                val finTime = formato.parse(fin)
                
                if (inicioTime != null && finTime != null) {
                    val diferenciaMs = finTime.time - inicioTime.time
                    val minutos = (diferenciaMs / (1000 * 60)).toInt()
                    
                    if (minutos > 0) {
                        val horas = minutos / 60
                        val mins = minutos % 60
                        tvDuracionRefrigerio.text = if (horas > 0) {
                            "⏱️ Duración: ${horas}h ${mins}m"
                        } else {
                            "⏱️ Duración: ${mins} minutos"
                        }
                    } else {
                        tvDuracionRefrigerio.text = "⚠️ Horario inválido"
                    }
                } else {
                    tvDuracionRefrigerio.text = "⏱️ Duración: --"
                }
            } else {
                tvDuracionRefrigerio.text = "⏱️ Duración: --"
            }
        } catch (e: Exception) {
            tvDuracionRefrigerio.text = "⚠️ Error en horario"
        }
    }
    
    private fun actualizarResumenHoras(
        etHoraEntrada: EditText,
        etHoraSalida: EditText,
        cbTieneRefrigerio: CheckBox,
        etInicioRefrigerio: EditText,
        etFinRefrigerio: EditText,
        tvHorasTotales: TextView,
        tvHorasRefrigerio: TextView,
        tvHorasTrabajadas: TextView
    ) {
        try {
            val entrada = etHoraEntrada.text.toString()
            val salida = etHoraSalida.text.toString()
            
            if (entrada.isNotEmpty() && salida.isNotEmpty()) {
                val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val entradaTime = formato.parse(entrada)
                val salidaTime = formato.parse(salida)
                
                if (entradaTime != null && salidaTime != null) {
                    val totalMinutos = ((salidaTime.time - entradaTime.time) / (1000 * 60)).toInt()
                    val horasTotal = totalMinutos / 60
                    val minutosTotal = totalMinutos % 60
                    
                    tvHorasTotales.text = "⏰ Horario: $entrada - $salida (${horasTotal}h ${minutosTotal}m)"
                    
                    if (cbTieneRefrigerio.isChecked) {
                        val inicioRef = etInicioRefrigerio.text.toString()
                        val finRef = etFinRefrigerio.text.toString()
                        
                        if (inicioRef.isNotEmpty() && finRef.isNotEmpty()) {
                            val inicioRefTime = formato.parse(inicioRef)
                            val finRefTime = formato.parse(finRef)
                            
                            if (inicioRefTime != null && finRefTime != null) {
                                val minutosRef = ((finRefTime.time - inicioRefTime.time) / (1000 * 60)).toInt()
                                val horasRef = minutosRef / 60
                                val minsRef = minutosRef % 60
                                
                                tvHorasRefrigerio.text = if (horasRef > 0) {
                                    "🍽️ Refrigerio: $inicioRef - $finRef (${horasRef}h ${minsRef}m)"
                                } else {
                                    "🍽️ Refrigerio: $inicioRef - $finRef (${minsRef}m)"
                                }
                                
                                val minutosTrabajos = totalMinutos - minutosRef
                                val horasTrabajo = minutosTrabajos / 60
                                val minsTrabajo = minutosTrabajos % 60
                                
                                tvHorasTrabajadas.text = "💼 Horas trabajadas: ${horasTrabajo}h ${minsTrabajo}m"
                            } else {
                                tvHorasRefrigerio.text = "🍽️ Refrigerio: Horario inválido"
                                tvHorasTrabajadas.text = "💼 Horas trabajadas: ${horasTotal}h ${minutosTotal}m"
                            }
                        } else {
                            tvHorasRefrigerio.text = "🍽️ Refrigerio: No configurado"
                            tvHorasTrabajadas.text = "💼 Horas trabajadas: ${horasTotal}h ${minutosTotal}m"
                        }
                    } else {
                        tvHorasRefrigerio.text = "🍽️ Refrigerio: No configurado"
                        tvHorasTrabajadas.text = "💼 Horas trabajadas: ${horasTotal}h ${minutosTotal}m"
                    }
                } else {
                    tvHorasTotales.text = "⏰ Horario: Formato inválido"
                    tvHorasRefrigerio.text = "🍽️ Refrigerio: --"
                    tvHorasTrabajadas.text = "💼 Horas trabajadas: --"
                }
            } else {
                tvHorasTotales.text = "⏰ Horario: No configurado"
                tvHorasRefrigerio.text = "🍽️ Refrigerio: --"
                tvHorasTrabajadas.text = "💼 Horas trabajadas: --"
            }
        } catch (e: Exception) {
            tvHorasTotales.text = "⏰ Horario: Error"
            tvHorasRefrigerio.text = "🍽️ Refrigerio: Error"
            tvHorasTrabajadas.text = "💼 Horas trabajadas: Error"
        }
    }
    
    private fun guardarPersonalConRefrigerio(
        etDni: EditText,
        etNombre: EditText,
        etHoraEntrada: EditText,
        etHoraSalida: EditText,
        cbTieneRefrigerio: CheckBox,
        etInicioRefrigerio: EditText,
        etFinRefrigerio: EditText,
        personalExistente: Personal?
    ) {
        val dni = etDni.text.toString().trim()
        val nombre = etNombre.text.toString().trim()
        val horaEntrada = etHoraEntrada.text.toString().trim()
        val horaSalida = etHoraSalida.text.toString().trim()
        
        if (dni.isEmpty() || nombre.isEmpty() || horaEntrada.isEmpty() || horaSalida.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Verificar DNI duplicado solo en modo agregar
        if (personalExistente == null && personalList.any { it.dni == dni }) {
            Toast.makeText(this, "Ya existe una persona con ese DNI", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validar formato de horarios
        if (!validarFormatoHora(horaEntrada) || !validarFormatoHora(horaSalida)) {
            Toast.makeText(this, "Formato de hora inválido. Use HH:mm", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Configurar refrigerio
        val tieneRefrigerio = cbTieneRefrigerio.isChecked
        var inicioRefrigerio = "12:00"
        var finRefrigerio = "13:00"
        var minutosRefrigerio = 60
        
        if (tieneRefrigerio) {
            inicioRefrigerio = etInicioRefrigerio.text.toString().trim()
            finRefrigerio = etFinRefrigerio.text.toString().trim()
            
            if (inicioRefrigerio.isEmpty() || finRefrigerio.isEmpty()) {
                Toast.makeText(this, "Configure el horario de refrigerio", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (!validarFormatoHora(inicioRefrigerio) || !validarFormatoHora(finRefrigerio)) {
                Toast.makeText(this, "Formato de hora de refrigerio inválido", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Calcular duración del refrigerio
            try {
                val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val inicioTime = formato.parse(inicioRefrigerio)
                val finTime = formato.parse(finRefrigerio)
                
                if (inicioTime != null && finTime != null) {
                    minutosRefrigerio = ((finTime.time - inicioTime.time) / (1000 * 60)).toInt()
                    if (minutosRefrigerio <= 0) {
                        Toast.makeText(this, "El horario de refrigerio es inválido", Toast.LENGTH_SHORT).show()
                        return
                    }
                    
                    // Validar que el refrigerio esté dentro del horario laboral
                    if (!validarRefrigerioEnHorario(horaEntrada, horaSalida, inicioRefrigerio, finRefrigerio)) {
                        Toast.makeText(this, "El refrigerio debe estar dentro del horario laboral", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error en el formato de horario de refrigerio", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        val personal = Personal(
            dni = dni,
            nombre = nombre,
            tipoHorario = "FIJO",
            horaEntrada = horaEntrada,
            horaSalida = horaSalida,
            tieneRefrigerio = tieneRefrigerio,
            inicioRefrigerio = inicioRefrigerio,
            finRefrigerio = finRefrigerio,
            minutosRefrigerio = minutosRefrigerio
        )
        
        // Guardar o actualizar
        if (personalExistente == null) {
            personalList.add(personal)
            Toast.makeText(this, "✅ Personal agregado: $nombre", Toast.LENGTH_SHORT).show()
        } else {
            val index = personalList.indexOfFirst { it.dni == dni }
            if (index != -1) {
                personalList[index] = personal
                Toast.makeText(this, "✅ Personal actualizado: $nombre", Toast.LENGTH_SHORT).show()
            }
        }
        
        savePersonalData()
        personalAdapter.submitList(personalList.toList())
    }
    
    private fun validarFormatoHora(hora: String): Boolean {
        return try {
            val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            formato.isLenient = false
            formato.parse(hora) != null
        } catch (e: Exception) {
            false
        }
    }
    
    private fun validarRefrigerioEnHorario(
        horaEntrada: String,
        horaSalida: String,
        inicioRefrigerio: String,
        finRefrigerio: String
    ): Boolean {
        return try {
            val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val entradaTime = formato.parse(horaEntrada)?.time ?: 0
            val salidaTime = formato.parse(horaSalida)?.time ?: 0
            val inicioRefTime = formato.parse(inicioRefrigerio)?.time ?: 0
            val finRefTime = formato.parse(finRefrigerio)?.time ?: 0
            
            // El refrigerio debe estar completamente dentro del horario laboral
            inicioRefTime >= entradaTime && finRefTime <= salidaTime
        } catch (e: Exception) {
            false
        }
    }
    
    private fun guardarPersonalConTipoHorario(
        etDni: EditText,
        etNombre: EditText,
        switchTipoHorario: Switch,
        etHoraEntrada: EditText,
        etHoraSalida: EditText,
        cbTieneRefrigerio: CheckBox,
        etInicioRefrigerio: EditText,
        etFinRefrigerio: EditText,
        personalExistente: Personal?,
        // Campos individuales por día
        etLunesEntrada: EditText, etLunesSalida: EditText,
        etMartesEntrada: EditText, etMartesSalida: EditText,
        etMiercolesEntrada: EditText, etMiercolesSalida: EditText,
        etJuevesEntrada: EditText, etJuevesSalida: EditText,
        etViernesEntrada: EditText, etViernesSalida: EditText,
        etSabadoEntrada: EditText, etSabadoSalida: EditText
    ) {
        val dni = etDni.text.toString().trim()
        val nombre = etNombre.text.toString().trim()
        val horaEntrada = etHoraEntrada.text.toString().trim()
        val horaSalida = etHoraSalida.text.toString().trim()
        
        if (dni.isEmpty() || nombre.isEmpty() || horaEntrada.isEmpty() || horaSalida.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Verificar DNI duplicado solo en modo agregar
        if (personalExistente == null && personalList.any { it.dni == dni }) {
            Toast.makeText(this, "Ya existe una persona con ese DNI", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validar formato de horarios
        if (!validarFormatoHora(horaEntrada) || !validarFormatoHora(horaSalida)) {
            Toast.makeText(this, "Formato de hora inválido. Use HH:mm", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Configurar refrigerio
        val tieneRefrigerio = cbTieneRefrigerio.isChecked
        var inicioRefrigerio = "12:00"
        var finRefrigerio = "13:00"
        var minutosRefrigerio = 60
        
        if (tieneRefrigerio) {
            inicioRefrigerio = etInicioRefrigerio.text.toString().trim()
            finRefrigerio = etFinRefrigerio.text.toString().trim()
            
            if (inicioRefrigerio.isEmpty() || finRefrigerio.isEmpty()) {
                Toast.makeText(this, "Configure el horario de refrigerio", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (!validarFormatoHora(inicioRefrigerio) || !validarFormatoHora(finRefrigerio)) {
                Toast.makeText(this, "Formato de hora de refrigerio inválido", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Calcular duración del refrigerio
            try {
                val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val inicioTime = formato.parse(inicioRefrigerio)
                val finTime = formato.parse(finRefrigerio)
                
                if (inicioTime != null && finTime != null) {
                    minutosRefrigerio = ((finTime.time - inicioTime.time) / (1000 * 60)).toInt()
                    if (minutosRefrigerio <= 0) {
                        Toast.makeText(this, "El horario de refrigerio es inválido", Toast.LENGTH_SHORT).show()
                        return
                    }
                    
                    // Validar que el refrigerio esté dentro del horario laboral
                    if (!validarRefrigerioEnHorario(horaEntrada, horaSalida, inicioRefrigerio, finRefrigerio)) {
                        Toast.makeText(this, "El refrigerio debe estar dentro del horario laboral", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error en el formato de horario de refrigerio", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        // Determinar tipo de horario según el Switch
        val tipoHorario = if (switchTipoHorario.isChecked) "VARIABLE" else "FIJO"
        
        // Crear horario semanal si es variable
        val horarioSemanal = if (tipoHorario == "VARIABLE") {
            // Crear horario base para todos los días laborales
            val horarioBase = HorarioDia(
                entrada = horaEntrada,
                salida = horaSalida,
                activo = true,
                esHorarioPartido = false,
                tieneRefrigerio = tieneRefrigerio,
                inicioRefrigerio = inicioRefrigerio,
                finRefrigerio = finRefrigerio,
                minutosRefrigerio = minutosRefrigerio,
                aplicarCompensacion = true
            )
            
            HorarioSemanal(
                lunes = horarioBase,
                martes = horarioBase,
                miercoles = horarioBase,
                jueves = horarioBase,
                viernes = horarioBase,
                sabado = HorarioDia(activo = false), // Sábado inactivo por defecto
                domingo = HorarioDia(activo = false)  // Domingo inactivo por defecto
            )
        } else {
            HorarioSemanal() // Horario semanal vacío para horario fijo
        }
        
        val personal = Personal(
            dni = dni,
            nombre = nombre,
            tipoHorario = tipoHorario,
            horaEntrada = horaEntrada,
            horaSalida = horaSalida,
            tieneRefrigerio = tieneRefrigerio,
            inicioRefrigerio = inicioRefrigerio,
            finRefrigerio = finRefrigerio,
            minutosRefrigerio = minutosRefrigerio,
            horarioSemanal = horarioSemanal
        )
        
        // Guardar o actualizar
        if (personalExistente == null) {
            personalList.add(personal)
            val tipoTexto = if (tipoHorario == "VARIABLE") "horario flexible" else "horario fijo"
            Toast.makeText(this, "✅ Personal agregado: $nombre ($tipoTexto)", Toast.LENGTH_SHORT).show()
        } else {
            val index = personalList.indexOfFirst { it.dni == dni }
            if (index != -1) {
                personalList[index] = personal
                val tipoTexto = if (tipoHorario == "VARIABLE") "horario flexible" else "horario fijo"
                Toast.makeText(this, "✅ Personal actualizado: $nombre ($tipoTexto)", Toast.LENGTH_SHORT).show()
            }
        }
        
        savePersonalData()
        personalAdapter.submitList(personalList.toList())
    }
}
