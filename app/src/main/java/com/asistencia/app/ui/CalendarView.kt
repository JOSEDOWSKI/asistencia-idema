package com.asistencia.app.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.asistencia.app.R
import com.asistencia.app.database.Ausencia
import com.asistencia.app.database.RegistroAsistencia
import java.text.SimpleDateFormat
import java.util.*

class CalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {
    
    private lateinit var gridLayout: GridLayout
    private lateinit var monthYearText: TextView
    private lateinit var prevMonthButton: TextView
    private lateinit var nextMonthButton: TextView
    
    private var currentDate = Calendar.getInstance()
    private var selectedDate: Calendar? = null
    private var onDateSelectedListener: ((Calendar) -> Unit)? = null
    private var onDateLongClickListener: ((Calendar) -> Unit)? = null
    
    private var registros: List<RegistroAsistencia> = emptyList()
    private var ausencias: List<Ausencia> = emptyList()
    
    init {
        initView()
    }
    
    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.view_calendar, this, true)
        
        gridLayout = findViewById(R.id.calendar_grid)
        monthYearText = findViewById(R.id.month_year_text)
        prevMonthButton = findViewById(R.id.prev_month_button)
        nextMonthButton = findViewById(R.id.next_month_button)
        
        setupClickListeners()
        updateCalendar()
    }
    
    private fun setupClickListeners() {
        prevMonthButton.setOnClickListener {
            currentDate.add(Calendar.MONTH, -1)
            updateCalendar()
        }
        
        nextMonthButton.setOnClickListener {
            currentDate.add(Calendar.MONTH, 1)
            updateCalendar()
        }
    }
    
    fun setOnDateSelectedListener(listener: (Calendar) -> Unit) {
        onDateSelectedListener = listener
    }
    
    fun setOnDateLongClickListener(listener: (Calendar) -> Unit) {
        onDateLongClickListener = listener
    }
    
    fun setData(registros: List<RegistroAsistencia>, ausencias: List<Ausencia>) {
        this.registros = registros
        this.ausencias = ausencias
        updateCalendar()
    }
    
    private fun updateCalendar() {
        updateMonthYearText()
        updateCalendarGrid()
    }
    
    private fun updateMonthYearText() {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearText.text = formatter.format(currentDate.time)
    }
    
    private fun updateCalendarGrid() {
        gridLayout.removeAllViews()
        
        // Agregar headers de días
        val daysOfWeek = arrayOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
        daysOfWeek.forEach { day ->
            val dayHeader = createDayHeader(day)
            gridLayout.addView(dayHeader)
        }
        
        // Obtener el primer día del mes
        val firstDayOfMonth = Calendar.getInstance().apply {
            time = currentDate.time
            set(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Obtener el último día del mes
        val lastDayOfMonth = Calendar.getInstance().apply {
            time = currentDate.time
            set(Calendar.DAY_OF_MONTH, currentDate.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        
        // Agregar espacios vacíos para días antes del primer día del mes
        val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1
        repeat(firstDayOfWeek) {
            val emptyDay = createEmptyDay()
            gridLayout.addView(emptyDay)
        }
        
        // Agregar días del mes
        val currentDay = Calendar.getInstance().apply {
            time = firstDayOfMonth.time
        }
        
        while (currentDay.before(lastDayOfMonth) || currentDay.equals(lastDayOfMonth)) {
            val dayView = createDayView(currentDay)
            gridLayout.addView(dayView)
            currentDay.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    
    private fun createDayHeader(day: String): TextView {
        return TextView(context).apply {
            text = day
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = android.view.Gravity.CENTER
            setPadding(8, 16, 8, 16)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
    }
    
    private fun createEmptyDay(): View {
        return View(context).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 80
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
    }
    
    private fun createDayView(date: Calendar): TextView {
        val dayView = TextView(context).apply {
            text = date.get(Calendar.DAY_OF_MONTH).toString()
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setPadding(8, 16, 8, 16)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 80
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
        
        // Configurar colores según el estado del día
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
        val hasRegistros = registros.any { it.fecha == dateString }
        val ausencia = ausencias.find { it.fecha == dateString }
        val isToday = isToday(date)
        val isSelected = selectedDate?.let { isSameDay(it, date) } ?: false
        
        when {
            ausencia != null -> {
                // Día con ausencia
                dayView.setBackgroundColor(getAusenciaColor(ausencia.tipo))
                dayView.setTextColor(Color.WHITE)
                dayView.text = "${date.get(Calendar.DAY_OF_MONTH)}\n${getAusenciaIcon(ausencia.tipo)}"
            }
            hasRegistros -> {
                // Día trabajado
                dayView.setBackgroundColor(Color.parseColor("#E74C3C"))
                dayView.setTextColor(Color.WHITE)
                dayView.text = "${date.get(Calendar.DAY_OF_MONTH)}\n✅"
            }
            isToday -> {
                // Día actual
                dayView.setBackgroundColor(Color.parseColor("#3498DB"))
                dayView.setTextColor(Color.WHITE)
                dayView.text = "${date.get(Calendar.DAY_OF_MONTH)}\n📅"
            }
            isSelected -> {
                // Día seleccionado
                dayView.setBackgroundColor(Color.parseColor("#F39C12"))
                dayView.setTextColor(Color.WHITE)
            }
            else -> {
                // Día normal
                dayView.setBackgroundColor(Color.TRANSPARENT)
                dayView.setTextColor(Color.BLACK)
            }
        }
        
        // Configurar click listeners
        dayView.setOnClickListener {
            selectedDate = date
            onDateSelectedListener?.invoke(date)
            updateCalendar()
        }
        
        dayView.setOnLongClickListener {
            onDateLongClickListener?.invoke(date)
            true
        }
        
        return dayView
    }
    
    private fun getAusenciaColor(tipo: com.asistencia.app.database.TipoAusencia): Int {
        return when (tipo) {
            com.asistencia.app.database.TipoAusencia.JUSTIFICACION -> Color.parseColor("#27AE60")
            com.asistencia.app.database.TipoAusencia.DESCANSO_MEDICO -> Color.parseColor("#E67E22")
            com.asistencia.app.database.TipoAusencia.VACACIONES -> Color.parseColor("#9B59B6")
            com.asistencia.app.database.TipoAusencia.PERMISO_PERSONAL -> Color.parseColor("#F39C12")
            com.asistencia.app.database.TipoAusencia.SUSPENSION -> Color.parseColor("#E74C3C")
            com.asistencia.app.database.TipoAusencia.OTRO -> Color.parseColor("#95A5A6")
        }
    }
    
    private fun getAusenciaIcon(tipo: com.asistencia.app.database.TipoAusencia): String {
        return when (tipo) {
            com.asistencia.app.database.TipoAusencia.JUSTIFICACION -> "📝"
            com.asistencia.app.database.TipoAusencia.DESCANSO_MEDICO -> "🏥"
            com.asistencia.app.database.TipoAusencia.VACACIONES -> "🏖️"
            com.asistencia.app.database.TipoAusencia.PERMISO_PERSONAL -> "👤"
            com.asistencia.app.database.TipoAusencia.SUSPENSION -> "⚠️"
            com.asistencia.app.database.TipoAusencia.OTRO -> "❓"
        }
    }
    
    private fun isToday(date: Calendar): Boolean {
        val today = Calendar.getInstance()
        return isSameDay(date, today)
    }
    
    private fun isSameDay(date1: Calendar, date2: Calendar): Boolean {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
                date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH)
    }
}
