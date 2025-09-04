package com.asistencia.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.asistencia.app.database.Empleado
import com.asistencia.app.database.TipoEvento
import com.asistencia.app.database.TipoHorario
import com.asistencia.app.database.Ausencia
import com.asistencia.app.database.TipoAusencia
import com.asistencia.app.repository.AsistenciaRepository
import com.asistencia.app.utils.HorarioUtils
import com.asistencia.app.utils.PinManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EmpleadoDetalleActivity : AppCompatActivity() {
    
    private lateinit var repository: AsistenciaRepository
    private var empleado: Empleado? = null
    private var fotoActual: String? = null
    
    // UI Components
    private lateinit var ivFotoEmpleado: ImageView
    private lateinit var tvNombreEmpleado: TextView
    private lateinit var tvDniEmpleado: TextView
    private lateinit var tvEstadoEmpleado: TextView
    private lateinit var tvHorarioEmpleado: TextView
    private lateinit var tvUltimoRegistro: TextView
    private lateinit var tvEstadisticas: TextView
    private lateinit var btnEditarEmpleado: Button
    private lateinit var btnCambiarFoto: Button
    private lateinit var btnEliminarEmpleado: Button
    private lateinit var btnActivarDesactivar: Button
    private lateinit var cardEstadisticas: androidx.cardview.widget.CardView
    private lateinit var cardHorario: androidx.cardview.widget.CardView
    private lateinit var cardRegistros: androidx.cardview.widget.CardView
    private lateinit var calendarView: com.asistencia.app.ui.CalendarView
    private lateinit var cardCalendario: androidx.cardview.widget.CardView
    private lateinit var btnEditarHorario: Button
    private lateinit var btnGestionarAusencias: Button
    
    companion object {
        const val EXTRA_EMPLEADO_ID = "empleado_id"
        const val EXTRA_EMPLEADO_DNI = "empleado_dni"
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val GALLERY_PERMISSION_REQUEST = 101
        private const val CAMERA_REQUEST = 102
        private const val GALLERY_REQUEST = 103
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empleado_detalle)
        
        repository = AsistenciaRepository(this)
        
        setupViews()
        setupClickListeners()
        loadEmpleadoData()
        
        supportActionBar?.title = "👤 Detalle de Empleado"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        PinManager.updateLastActivity(this)
    }
    
    private fun setupViews() {
        ivFotoEmpleado = findViewById(R.id.iv_foto_empleado)
        tvNombreEmpleado = findViewById(R.id.tv_nombre_empleado)
        tvDniEmpleado = findViewById(R.id.tv_dni_empleado)
        tvEstadoEmpleado = findViewById(R.id.tv_estado_empleado)
        tvHorarioEmpleado = findViewById(R.id.tv_horario_empleado)
        tvUltimoRegistro = findViewById(R.id.tv_ultimo_registro)
        tvEstadisticas = findViewById(R.id.tv_estadisticas)
        btnEditarEmpleado = findViewById(R.id.btn_editar_empleado)
        btnCambiarFoto = findViewById(R.id.btn_cambiar_foto)
        btnEliminarEmpleado = findViewById(R.id.btn_eliminar_empleado)
        btnActivarDesactivar = findViewById(R.id.btn_activar_desactivar)
        cardEstadisticas = findViewById(R.id.card_estadisticas)
        cardHorario = findViewById(R.id.card_horario)
        cardRegistros = findViewById(R.id.card_registros)
        calendarView = findViewById(R.id.calendar_view)
        cardCalendario = findViewById(R.id.card_calendario)
        btnEditarHorario = findViewById(R.id.btn_editar_horario)
        btnGestionarAusencias = findViewById(R.id.btn_gestionar_ausencias)
    }
    
    private fun setupClickListeners() {
        btnEditarEmpleado.setOnClickListener { editarEmpleado() }
        btnCambiarFoto.setOnClickListener { mostrarOpcionesFoto() }
        btnEliminarEmpleado.setOnClickListener { confirmarEliminacion() }
        btnActivarDesactivar.setOnClickListener { cambiarEstadoEmpleado() }
        btnEditarHorario.setOnClickListener { editarHorario() }
        btnGestionarAusencias.setOnClickListener { gestionarAusencias() }
    }
    
    private fun loadEmpleadoData() {
        val empleadoId = intent.getStringExtra(EXTRA_EMPLEADO_ID)
        val empleadoDni = intent.getStringExtra(EXTRA_EMPLEADO_DNI)
        
        if (empleadoId == null && empleadoDni == null) {
            Toast.makeText(this, "Error: No se especificó el empleado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                empleado = if (empleadoId != null) {
                    repository.getEmpleadoById(empleadoId)
                } else {
                    repository.getEmpleadoByDni(empleadoDni!!)
                }
                
                if (empleado == null) {
                    Toast.makeText(this@EmpleadoDetalleActivity, "Empleado no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                
                mostrarDatosEmpleado()
                cargarFotoEmpleado()
                cargarEstadisticas()
                cargarUltimoRegistro()
                cargarCalendario()
                configurarCalendario()
                
            } catch (e: Exception) {
                Toast.makeText(this@EmpleadoDetalleActivity, "Error al cargar empleado: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun mostrarDatosEmpleado() {
        empleado?.let { emp ->
            tvNombreEmpleado.text = "${emp.nombres} ${emp.apellidos}"
            tvDniEmpleado.text = "DNI: ${emp.dni}"
            
            // Estado del empleado
            val estado = if (emp.activo) "✅ Activo" else "❌ Inactivo"
            tvEstadoEmpleado.text = estado
            
            // Configurar botón de activar/desactivar
            btnActivarDesactivar.text = if (emp.activo) "❌ Desactivar" else "✅ Activar"
            
            // Horario del empleado
            mostrarHorarioEmpleado(emp)
        }
    }
    
    private fun mostrarHorarioEmpleado(emp: Empleado) {
        val horarioText = when {
            emp.tipoHorario == TipoHorario.FLEXIBLE -> {
                "🕐 Horario Flexible\n" +
                "📅 Configurado por días\n" +
                "🍽️ Refrigerio configurado"
            }
            else -> {
                "🕐 Horario Regular\n" +
                "📅 Entrada: ${emp.horaEntradaRegular ?: "No configurado"}\n" +
                "📅 Salida: ${emp.horaSalidaRegular ?: "No configurado"}"
            }
        }
        tvHorarioEmpleado.text = horarioText
    }
    
    private fun cargarFotoEmpleado() {
        empleado?.let { emp ->
            // Cargar foto desde almacenamiento local
            val fotoPath = getFotoPath(emp.id)
            if (fotoPath != null && File(fotoPath).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(fotoPath)
                    ivFotoEmpleado.setImageBitmap(bitmap)
                    fotoActual = fotoPath
                } catch (e: Exception) {
                    // Si hay error, mostrar foto por defecto
                    ivFotoEmpleado.setImageResource(R.drawable.ic_person_placeholder)
                }
            } else {
                ivFotoEmpleado.setImageResource(R.drawable.ic_person_placeholder)
            }
        }
    }
    
    private fun cargarEstadisticas() {
        empleado?.let { emp ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val fecha = HorarioUtils.getCurrentDateString()
                    val registrosHoy = repository.getRegistrosByEmpleadoAndFecha(emp.id, fecha)
                    
                    val estadisticas = buildString {
                        append("📊 ESTADÍSTICAS DE HOY\n\n")
                        append("📅 Registros: ${registrosHoy.size}\n")
                        
                        val entrada = registrosHoy.find { it.tipoEvento == TipoEvento.ENTRADA_TURNO }
                        val salida = registrosHoy.find { it.tipoEvento == TipoEvento.SALIDA_TURNO }
                        
                        if (entrada != null) {
                            append("✅ Entrada: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entrada.timestampDispositivo))}\n")
                        } else {
                            append("❌ Sin entrada registrada\n")
                        }
                        
                        if (salida != null) {
                            append("✅ Salida: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(salida.timestampDispositivo))}\n")
                        } else {
                            append("⏳ Sin salida registrada\n")
                        }
                        
                        // Calcular horas trabajadas
                        if (entrada != null && salida != null) {
                            val horasTrabajadas = (salida.timestampDispositivo - entrada.timestampDispositivo) / (1000 * 60 * 60)
                            append("⏱️ Horas trabajadas: $horasTrabajadas horas\n")
                        }
                    }
                    
                    tvEstadisticas.text = estadisticas
                    
                } catch (e: Exception) {
                    tvEstadisticas.text = "❌ Error al cargar estadísticas"
                }
            }
        }
    }
    
    private fun cargarUltimoRegistro() {
        empleado?.let { emp ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val fecha = HorarioUtils.getCurrentDateString()
                    val registrosHoy = repository.getRegistrosByEmpleadoAndFecha(emp.id, fecha)
                    
                    if (registrosHoy.isNotEmpty()) {
                        val ultimoRegistro = registrosHoy.maxByOrNull { it.timestampDispositivo }
                        val tipoEvento = when (ultimoRegistro?.tipoEvento) {
                            TipoEvento.ENTRADA_TURNO -> "Entrada"
                            TipoEvento.SALIDA_TURNO -> "Salida"
                            TipoEvento.SALIDA_REFRIGERIO -> "Salida Refrigerio"
                            TipoEvento.ENTRADA_POST_REFRIGERIO -> "Entrada Post Refrigerio"
                            else -> "Otro"
                        }
                        
                        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ultimoRegistro!!.timestampDispositivo))
                        tvUltimoRegistro.text = "🕐 Último registro: $tipoEvento a las $hora"
                    } else {
                        tvUltimoRegistro.text = "📝 Sin registros hoy"
                    }
                    
                } catch (e: Exception) {
                    tvUltimoRegistro.text = "❌ Error al cargar último registro"
                }
            }
        }
    }
    
    private fun editarEmpleado() {
        empleado?.let { emp ->
            val intent = Intent(this, EmpleadosActivityMejorado::class.java).apply {
                putExtra("EDITAR_EMPLEADO", true)
                putExtra("EMPLEADO_ID", emp.id)
            }
            startActivity(intent)
        }
    }
    
    private fun mostrarOpcionesFoto() {
        val opciones = arrayOf("📷 Tomar Foto", "🖼️ Seleccionar de Galería")
        
        AlertDialog.Builder(this)
            .setTitle("📸 Cambiar Foto")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> tomarFoto()
                    1 -> seleccionarDeGaleria()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun tomarFoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            return
        }
        
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val fotoFile = crearArchivoFoto()
        fotoFile?.let { file ->
            val fotoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri)
            startActivityForResult(intent, CAMERA_REQUEST)
        }
    }
    
    private fun seleccionarDeGaleria() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), GALLERY_PERMISSION_REQUEST)
            return
        }
        
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST)
    }
    
    private fun crearArchivoFoto(): File? {
        empleado?.let { emp ->
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "EMPLEADO_${emp.id}_$timeStamp.jpg"
            val storageDir = File(filesDir, "fotos_empleados")
            
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            
            return File(storageDir, imageFileName)
        }
        return null
    }
    
    private fun getFotoPath(empleadoId: String): String? {
        val storageDir = File(filesDir, "fotos_empleados")
        if (storageDir.exists()) {
            val archivos = storageDir.listFiles { file -> file.name.startsWith("EMPLEADO_${empleadoId}_") }
            return archivos?.maxByOrNull { it.lastModified() }?.absolutePath
        }
        return null
    }
    
    private fun guardarFoto(bitmap: Bitmap): String? {
        empleado?.let { emp ->
            val fotoFile = crearArchivoFoto()
            fotoFile?.let { file ->
                try {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    
                    // Actualizar foto en la base de datos
                    CoroutineScope(Dispatchers.IO).launch {
                        val empleadoActualizado = emp.copy(fotoPath = file.absolutePath)
                        repository.updateEmpleado(empleadoActualizado)
                    }
                    
                    return file.absolutePath
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al guardar foto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return null
    }
    
    private fun confirmarEliminacion() {
        empleado?.let { emp ->
            AlertDialog.Builder(this)
                .setTitle("🗑️ Eliminar Empleado")
                .setMessage("¿Está seguro de eliminar a ${emp.nombres} ${emp.apellidos}?\n\nEsta acción no se puede deshacer.")
                .setPositiveButton("Eliminar") { _, _ ->
                    eliminarEmpleado(emp)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
    
    private fun eliminarEmpleado(emp: Empleado) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                repository.deleteEmpleado(emp)
                Toast.makeText(this@EmpleadoDetalleActivity, "✅ Empleado eliminado", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@EmpleadoDetalleActivity, "❌ Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun cambiarEstadoEmpleado() {
        empleado?.let { emp ->
            val nuevoEstado = !emp.activo
            val accion = if (nuevoEstado) "activar" else "desactivar"
            
            AlertDialog.Builder(this)
                .setTitle(if (nuevoEstado) "✅ Activar Empleado" else "❌ Desactivar Empleado")
                .setMessage("¿Está seguro de $accion a ${emp.nombres} ${emp.apellidos}?")
                .setPositiveButton(if (nuevoEstado) "Activar" else "Desactivar") { _, _ ->
                    actualizarEstadoEmpleado(emp, nuevoEstado)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
    
    private fun actualizarEstadoEmpleado(emp: Empleado, activo: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val empleadoActualizado = emp.copy(activo = activo)
                repository.updateEmpleado(empleadoActualizado)
                empleado = empleadoActualizado
                
                val mensaje = if (activo) "✅ Empleado activado" else "❌ Empleado desactivado"
                Toast.makeText(this@EmpleadoDetalleActivity, mensaje, Toast.LENGTH_SHORT).show()
                
                mostrarDatosEmpleado()
                
            } catch (e: Exception) {
                Toast.makeText(this@EmpleadoDetalleActivity, "❌ Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST -> {
                    // La foto ya se guardó en el archivo
                    cargarFotoEmpleado()
                    Toast.makeText(this, "✅ Foto tomada exitosamente", Toast.LENGTH_SHORT).show()
                }
                GALLERY_REQUEST -> {
                    data?.data?.let { uri ->
                        try {
                            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            val fotoPath = guardarFoto(bitmap)
                            if (fotoPath != null) {
                                ivFotoEmpleado.setImageBitmap(bitmap)
                                fotoActual = fotoPath
                                Toast.makeText(this, "✅ Foto seleccionada exitosamente", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "❌ Error al procesar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    tomarFoto()
                } else {
                    Toast.makeText(this, "❌ Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
                }
            }
            GALLERY_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    seleccionarDeGaleria()
                } else {
                    Toast.makeText(this, "❌ Permiso de galería requerido", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun cargarCalendario() {
        empleado?.let { emp ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Obtener registros del mes actual
                    val calendar = Calendar.getInstance()
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH) + 1
                    val fechaInicio = String.format("%04d-%02d-01", year, month)
                    val fechaFin = String.format("%04d-%02d-%02d", year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    
                    val registros = repository.getRegistrosByEmpleadoAndRango(emp.id, fechaInicio, fechaFin)
                    val ausencias = repository.getAusenciasByEmpleadoAndRango(emp.id, fechaInicio, fechaFin)
                    
                    calendarView.setData(registros, ausencias)
                    
                } catch (e: Exception) {
                    Toast.makeText(this@EmpleadoDetalleActivity, "Error al cargar calendario: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun configurarCalendario() {
        calendarView.setOnDateSelectedListener { date ->
            mostrarDetalleDia(date)
        }
        
        calendarView.setOnDateLongClickListener { date ->
            mostrarOpcionesDia(date)
        }
    }
    
    private fun mostrarDetalleDia(date: Calendar) {
        CoroutineScope(Dispatchers.Main).launch {
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
            val registros = repository.getRegistrosByEmpleadoAndFecha(empleado!!.id, dateString)
            val ausencia = repository.getAusenciaByEmpleadoAndFecha(empleado!!.id, dateString)
        
        val mensaje = buildString {
            append("📅 ${SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale.getDefault()).format(date.time)}\n\n")
            
            if (ausencia != null) {
                append("🏷️ AUSENCIA REGISTRADA\n")
                append("📋 Tipo: ${getTipoAusenciaText(ausencia.tipo)}\n")
                append("📝 Motivo: ${ausencia.motivo}\n")
                if (ausencia.descripcion != null) {
                    append("📄 Descripción: ${ausencia.descripcion}\n")
                }
            } else if (registros.isNotEmpty()) {
                append("✅ DÍA TRABAJADO\n")
                append("📊 Registros: ${registros.size}\n\n")
                
                registros.forEach { registro ->
                    val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(registro.timestampDispositivo))
                    val tipo = when (registro.tipoEvento) {
                        TipoEvento.ENTRADA_TURNO -> "Entrada"
                        TipoEvento.SALIDA_TURNO -> "Salida"
                        TipoEvento.SALIDA_REFRIGERIO -> "Salida Refrigerio"
                        TipoEvento.ENTRADA_POST_REFRIGERIO -> "Entrada Post Refrigerio"
                        else -> "Otro"
                    }
                    append("🕐 $tipo: $hora\n")
                }
            } else {
                append("📝 Sin registros para este día")
            }
        }
        
        AlertDialog.Builder(this@EmpleadoDetalleActivity)
            .setTitle("📅 Detalle del Día")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
        }
    }
    
    private fun mostrarOpcionesDia(date: Calendar) {
        CoroutineScope(Dispatchers.Main).launch {
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
            val ausencia = repository.getAusenciaByEmpleadoAndFecha(empleado!!.id, dateString)
        
        val opciones = if (ausencia != null) {
            arrayOf("✏️ Editar Ausencia", "🗑️ Eliminar Ausencia")
        } else {
            arrayOf("📝 Registrar Ausencia")
        }
        
        AlertDialog.Builder(this@EmpleadoDetalleActivity)
            .setTitle("📅 Opciones del Día")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> if (ausencia != null) editarAusencia(ausencia) else registrarAusencia(date)
                    1 -> if (ausencia != null) eliminarAusencia(ausencia)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
        }
    }
    
    private fun registrarAusencia(date: Calendar) {
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
        mostrarDialogoAusencia(dateString, null)
    }
    
    private fun editarAusencia(ausencia: Ausencia) {
        mostrarDialogoAusencia(ausencia.fecha, ausencia)
    }
    
    private fun eliminarAusencia(ausencia: Ausencia) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Eliminar Ausencia")
            .setMessage("¿Está seguro de eliminar esta ausencia?")
            .setPositiveButton("Eliminar") { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        repository.deleteAusencia(ausencia)
                        Toast.makeText(this@EmpleadoDetalleActivity, "✅ Ausencia eliminada", Toast.LENGTH_SHORT).show()
                        cargarCalendario()
                    } catch (e: Exception) {
                        Toast.makeText(this@EmpleadoDetalleActivity, "❌ Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun mostrarDialogoAusencia(fecha: String, ausenciaExistente: Ausencia?) {
        val dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_ausencia, null)
        val spinnerTipo = dialogView.findViewById<Spinner>(R.id.spinner_tipo_ausencia)
        val etMotivo = dialogView.findViewById<EditText>(R.id.et_motivo_ausencia)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.et_descripcion_ausencia)
        
        // Configurar spinner
        val tipos = TipoAusencia.values().map { getTipoAusenciaText(it) }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapter
        
        // Cargar datos existentes si es edición
        if (ausenciaExistente != null) {
            spinnerTipo.setSelection(ausenciaExistente.tipo.ordinal)
            etMotivo.setText(ausenciaExistente.motivo)
            etDescripcion.setText(ausenciaExistente.descripcion)
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (ausenciaExistente != null) "✏️ Editar Ausencia" else "📝 Registrar Ausencia")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, which ->
                val tipo = TipoAusencia.values()[spinnerTipo.selectedItemPosition]
                val motivo = etMotivo.text.toString()
                val descripcion = etDescripcion.text.toString()
                
                if (motivo.isBlank()) {
                    Toast.makeText(this, "❌ El motivo es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val ausencia = if (ausenciaExistente != null) {
                    ausenciaExistente.copy(
                        tipo = tipo,
                        motivo = motivo,
                        descripcion = if (descripcion.isBlank()) null else descripcion,
                        fechaActualizacion = System.currentTimeMillis()
                    )
                } else {
                    Ausencia(
                        empleadoId = empleado!!.id,
                        fecha = fecha,
                        tipo = tipo,
                        motivo = motivo,
                        descripcion = if (descripcion.isBlank()) null else descripcion
                    )
                }
                
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        if (ausenciaExistente != null) {
                            repository.updateAusencia(ausencia)
                        } else {
                            repository.insertAusencia(ausencia)
                        }
                        Toast.makeText(this@EmpleadoDetalleActivity, "✅ Ausencia guardada", Toast.LENGTH_SHORT).show()
                        cargarCalendario()
                    } catch (e: Exception) {
                        Toast.makeText(this@EmpleadoDetalleActivity, "❌ Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun getTipoAusenciaText(tipo: TipoAusencia): String {
        return when (tipo) {
            TipoAusencia.JUSTIFICACION -> "Justificación"
            TipoAusencia.DESCANSO_MEDICO -> "Descanso Médico"
            TipoAusencia.VACACIONES -> "Vacaciones"
            TipoAusencia.PERMISO_PERSONAL -> "Permiso Personal"
            TipoAusencia.SUSPENSION -> "Suspensión"
            TipoAusencia.OTRO -> "Otro"
        }
    }
    
    private fun editarHorario() {
        empleado?.let { emp ->
            val intent = Intent(this, EmpleadosActivityMejorado::class.java).apply {
                putExtra("EDITAR_EMPLEADO", true)
                putExtra("EMPLEADO_ID", emp.id)
            }
            startActivity(intent)
        }
    }
    
    private fun gestionarAusencias() {
        // Mostrar vista de gestión de ausencias
        Toast.makeText(this, "📅 Gestión de ausencias en desarrollo", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
