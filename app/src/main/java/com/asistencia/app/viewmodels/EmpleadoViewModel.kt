package com.asistencia.app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.asistencia.app.EmpleadosActivityMejorado.EmpleadoSimple
import com.asistencia.app.data.EmpleadoRepository
import com.asistencia.app.utils.TimeUtils
import kotlinx.coroutines.launch

class EmpleadoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EmpleadoRepository(application)
    private val _empleados = MutableLiveData<List<EmpleadoSimple>>()
    private val _error = MutableLiveData<String>()
    private val _success = MutableLiveData<Boolean>()
    
    val empleados: LiveData<List<EmpleadoSimple>> = _empleados
    val error: LiveData<String> = _error
    val success: LiveData<Boolean> = _success
    
    init {
        cargarEmpleados()
    }
    
    fun cargarEmpleados() {
        viewModelScope.launch {
            try {
                val lista = repository.obtenerEmpleados().filter { it.activo }
                _empleados.value = lista
            } catch (e: Exception) {
                _error.value = "Error al cargar empleados: ${e.message}"
            }
        }
    }
    
    fun agregarEmpleado(empleado: EmpleadoSimple) {
        viewModelScope.launch {
            try {
                if (repository.agregarEmpleado(empleado)) {
                    _success.value = true
                    cargarEmpleados()
                } else {
                    _error.value = "Ya existe un empleado con el mismo DNI"
                }
            } catch (e: Exception) {
                _error.value = "Error al agregar empleado: ${e.message}"
            }
        }
    }
    
    fun actualizarEmpleado(empleado: EmpleadoSimple) {
        viewModelScope.launch {
            try {
                if (repository.actualizarEmpleado(empleado)) {
                    _success.value = true
                    cargarEmpleados()
                } else {
                    _error.value = "No se pudo actualizar el empleado"
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar empleado: ${e.message}"
            }
        }
    }
    
    fun eliminarEmpleado(dni: String) {
        viewModelScope.launch {
            try {
                if (repository.eliminarEmpleado(dni)) {
                    _success.value = true
                    cargarEmpleados()
                } else {
                    _error.value = "No se pudo eliminar el empleado"
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar empleado: ${e.message}"
            }
        }
    }
    
    fun buscarEmpleado(dni: String): EmpleadoSimple? {
        return repository.buscarEmpleadoPorDni(dni)
    }
    
    fun calcularHorasTrabajadas(
        checkIn: String,
        checkOut: String,
        horaEntrada: String,
        horaSalida: String,
        tieneRefrigerio: Boolean = false,
        horaInicioRefrigerio: String? = null,
        horaFinRefrigerio: String? = null
    ): Triple<String, Boolean, Boolean> {
        return TimeUtils.calculateWorkedHours(
            checkIn = checkIn,
            checkOut = checkOut,
            horaEntrada = horaEntrada,
            horaSalida = horaSalida,
            tieneRefrigerio = tieneRefrigerio,
            horaInicioRefrigerio = horaInicioRefrigerio,
            horaFinRefrigerio = horaFinRefrigerio
        )
    }
    
    fun validarHorario(
        horaEntrada: String,
        horaSalida: String,
        tieneRefrigerio: Boolean = false,
        horaInicioRefrigerio: String? = null,
        horaFinRefrigerio: String? = null
    ): Boolean {
        if (!TimeUtils.isValidTime(horaEntrada) || !TimeUtils.isValidTime(horaSalida)) {
            _error.value = "Formato de hora inválido"
            return false
        }
        
        if (TimeUtils.compareTimes(horaEntrada, horaSalida) >= 0) {
            _error.value = "La hora de salida debe ser posterior a la de entrada"
            return false
        }
        
        if (tieneRefrigerio) {
            if (horaInicioRefrigerio.isNullOrEmpty() || horaFinRefrigerio.isNullOrEmpty()) {
                _error.value = "Debe especificar el horario de refrigerio"
                return false
            }
            
            if (!TimeUtils.isValidTime(horaInicioRefrigerio) || !TimeUtils.isValidTime(horaFinRefrigerio)) {
                _error.value = "Formato de hora de refrigerio inválido"
                return false
            }
            
            if (TimeUtils.compareTimes(horaInicioRefrigerio, horaFinRefrigerio) >= 0) {
                _error.value = "La hora de fin de refrigerio debe ser posterior a la de inicio"
                return false
            }
            
            if (TimeUtils.compareTimes(horaInicioRefrigerio, horaEntrada) < 0 || 
                TimeUtils.compareTimes(horaFinRefrigerio, horaSalida) > 0) {
                _error.value = "El horario de refrigerio debe estar dentro del horario laboral"
                return false
            }
        }
        
        return true
    }
}
