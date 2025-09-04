package com.asistencia.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asistencia.app.database.Empleado
import com.asistencia.app.database.TipoHorario
import com.asistencia.app.repository.AsistenciaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmpleadosViewModel @Inject constructor(
    private val repository: AsistenciaRepository
) : ViewModel() {

    // Estados de la UI
    private val _uiState = MutableStateFlow(EmpleadosUiState())
    val uiState: StateFlow<EmpleadosUiState> = _uiState.asStateFlow()

    // Lista de empleados
    private val _empleados = MutableStateFlow<List<Empleado>>(emptyList())
    val empleados: StateFlow<List<Empleado>> = _empleados.asStateFlow()

    // Filtros
    private val _filtroActivo = MutableStateFlow(true)
    private val _filtroBusqueda = MutableStateFlow("")

    init {
        cargarEmpleados()
        configurarFiltros()
    }

    private fun configurarFiltros() {
        viewModelScope.launch {
            combine(
                repository.getAllEmpleados(),
                _filtroActivo,
                _filtroBusqueda
            ) { empleados, soloActivos, busqueda ->
                var empleadosFiltrados = empleados

                // Filtrar por estado activo
                if (soloActivos) {
                    empleadosFiltrados = empleadosFiltrados.filter { it.activo }
                }

                // Filtrar por búsqueda
                if (busqueda.isNotBlank()) {
                    empleadosFiltrados = empleadosFiltrados.filter { empleado ->
                        empleado.nombres.contains(busqueda, ignoreCase = true) ||
                        empleado.apellidos.contains(busqueda, ignoreCase = true) ||
                        empleado.dni.contains(busqueda, ignoreCase = true)
                    }
                }

                empleadosFiltrados
            }.collect { empleadosFiltrados ->
                _empleados.value = empleadosFiltrados
                actualizarEstadisticas(empleadosFiltrados)
            }
        }
    }

    private fun actualizarEstadisticas(empleados: List<Empleado>) {
        val total = empleados.size
        val activos = empleados.count { it.activo }
        val flexibles = empleados.count { it.tipoHorario == TipoHorario.FLEXIBLE }
        val regulares = empleados.count { it.tipoHorario == TipoHorario.REGULAR }

        _uiState.update { currentState ->
            currentState.copy(
                totalEmpleados = total,
                empleadosActivos = activos,
                empleadosFlexibles = flexibles,
                empleadosRegulares = regulares,
                isLoading = false
            )
        }
    }

    fun cargarEmpleados() {
        _uiState.update { it.copy(isLoading = true) }
        // Los empleados se cargan automáticamente a través del Flow
    }

    fun agregarEmpleado(
        dni: String,
        nombres: String,
        apellidos: String,
        tipoHorario: TipoHorario = TipoHorario.REGULAR,
        horaEntrada: String? = null,
        horaSalida: String? = null,
        horarioFlexibleJson: String? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val empleado = Empleado(
                    dni = dni,
                    nombres = nombres,
                    apellidos = apellidos,
                    tipoHorario = tipoHorario,
                    horaEntradaRegular = horaEntrada,
                    horaSalidaRegular = horaSalida,
                    horarioFlexibleJson = horarioFlexibleJson
                )

                repository.insertEmpleado(empleado)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        mensaje = "Empleado agregado exitosamente"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Error al agregar empleado: ${e.message}"
                    )
                }
            }
        }
    }

    fun actualizarEmpleado(empleado: Empleado) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                repository.updateEmpleado(empleado)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        mensaje = "Empleado actualizado exitosamente"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Error al actualizar empleado: ${e.message}"
                    )
                }
            }
        }
    }

    fun cambiarEstadoEmpleado(empleadoId: String, activo: Boolean) {
        viewModelScope.launch {
            try {
                if (activo) {
                    repository.activateEmpleado(empleadoId)
                } else {
                    repository.deactivateEmpleado(empleadoId)
                }
                _uiState.update { 
                    it.copy(
                        mensaje = "Estado del empleado actualizado"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Error al cambiar estado: ${e.message}"
                    )
                }
            }
        }
    }

    fun eliminarEmpleado(empleado: Empleado) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                repository.deleteEmpleado(empleado)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        mensaje = "Empleado eliminado exitosamente"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Error al eliminar empleado: ${e.message}"
                    )
                }
            }
        }
    }

    fun buscarEmpleados(query: String) {
        _filtroBusqueda.value = query
    }

    fun cambiarFiltroActivo(soloActivos: Boolean) {
        _filtroActivo.value = soloActivos
    }

    fun limpiarMensajes() {
        _uiState.update { 
            it.copy(
                mensaje = null,
                error = null
            )
        }
    }

    fun obtenerEmpleadoPorDni(dni: String): Empleado? {
        return empleados.value.find { it.dni == dni }
    }
}

// Estado de la UI
data class EmpleadosUiState(
    val isLoading: Boolean = false,
    val totalEmpleados: Int = 0,
    val empleadosActivos: Int = 0,
    val empleadosFlexibles: Int = 0,
    val empleadosRegulares: Int = 0,
    val mensaje: String? = null,
    val error: String? = null
)
