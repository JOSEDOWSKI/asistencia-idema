package com.asistencia.app.ui.empleados

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.asistencia.app.database.Empleado
import com.asistencia.app.database.TipoHorario
import com.asistencia.app.viewmodels.EmpleadosViewModel
import com.asistencia.app.viewmodels.EmpleadosUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmpleadosScreen(
    viewModel: EmpleadosViewModel = hiltViewModel(),
    onNavigateToScanner: () -> Unit = {},
    onNavigateToReports: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val empleados by viewModel.empleados.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyActive by remember { mutableStateOf(true) }

    LaunchedEffect(searchQuery) {
        viewModel.buscarEmpleados(searchQuery)
    }

    LaunchedEffect(showOnlyActive) {
        viewModel.cambiarFiltroActivo(showOnlyActive)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👥 Gestión de Empleados") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar empleado")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Estadísticas
            EstadisticasCard(uiState)
            
            // Filtros
            FiltrosSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                showOnlyActive = showOnlyActive,
                onShowOnlyActiveChange = { showOnlyActive = it }
            )
            
            // Lista de empleados
            EmpleadosList(
                empleados = empleados,
                onEmpleadoClick = { /* TODO: Navegar a detalles */ },
                onToggleEstado = { empleado ->
                    viewModel.cambiarEstadoEmpleado(empleado.id, !empleado.activo)
                },
                onDeleteEmpleado = { empleado ->
                    viewModel.eliminarEmpleado(empleado)
                }
            )
        }

        // Diálogo de agregar empleado
        if (showAddDialog) {
            AddEmpleadoDialog(
                onDismiss = { showAddDialog = false },
                onEmpleadoAdded = { dni, nombres, apellidos, tipoHorario ->
                    viewModel.agregarEmpleado(dni, nombres, apellidos, tipoHorario)
                    showAddDialog = false
                }
            )
        }

        // Snackbar para mensajes
        LaunchedEffect(uiState.mensaje, uiState.error) {
            uiState.mensaje?.let {
                // Mostrar mensaje de éxito
                viewModel.limpiarMensajes()
            }
            uiState.error?.let {
                // Mostrar mensaje de error
                viewModel.limpiarMensajes()
            }
        }
    }
}

@Composable
fun EstadisticasCard(uiState: EmpleadosUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Estadísticas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EstadisticaItem("Total", uiState.totalEmpleados.toString())
                EstadisticaItem("Activos", uiState.empleadosActivos.toString())
                EstadisticaItem("Flexibles", uiState.empleadosFlexibles.toString())
                EstadisticaItem("Regulares", uiState.empleadosRegulares.toString())
            }
        }
    }
}

@Composable
fun EstadisticaItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FiltrosSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showOnlyActive: Boolean,
    onShowOnlyActiveChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("🔍 Buscar empleados") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Filtro de estado
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showOnlyActive,
                    onCheckedChange = onShowOnlyActiveChange
                )
                Text(
                    text = "Solo empleados activos",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun EmpleadosList(
    empleados: List<Empleado>,
    onEmpleadoClick: (Empleado) -> Unit,
    onToggleEstado: (Empleado) -> Unit,
    onDeleteEmpleado: (Empleado) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (empleados.isEmpty()) {
            item {
                EmptyState()
            }
        } else {
            items(empleados) { empleado ->
                EmpleadoItem(
                    empleado = empleado,
                    onClick = { onEmpleadoClick(empleado) },
                    onToggleEstado = { onToggleEstado(empleado) },
                    onDelete = { onDeleteEmpleado(empleado) }
                )
            }
        }
    }
}

@Composable
fun EmpleadoItem(
    empleado: Empleado,
    onClick: () -> Unit,
    onToggleEstado: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (empleado.activo) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = empleado.nombres.firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Información del empleado
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${empleado.nombres} ${empleado.apellidos}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "DNI: ${empleado.dni}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (empleado.tipoHorario == TipoHorario.FLEXIBLE) 
                            Icons.Default.Schedule 
                        else 
                            Icons.Default.Work,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (empleado.tipoHorario == TipoHorario.FLEXIBLE) 
                            "Horario Flexible" 
                        else 
                            "Horario Regular",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Estado
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (empleado.activo) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = if (empleado.activo) "Activo" else "Inactivo",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = if (empleado.activo) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Menú de opciones
            IconButton(onClick = onToggleEstado) {
                Icon(
                    imageVector = if (empleado.activo) 
                        Icons.Default.Block 
                    else 
                        Icons.Default.Check,
                    contentDescription = "Cambiar estado",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No hay empleados registrados",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Toca el botón + para agregar el primer empleado",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun AddEmpleadoDialog(
    onDismiss: () -> Unit,
    onEmpleadoAdded: (String, String, String, TipoHorario) -> Unit
) {
    var dni by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var tipoHorario by remember { mutableStateOf(TipoHorario.REGULAR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("➕ Agregar Empleado") },
        text = {
            Column {
                OutlinedTextField(
                    value = dni,
                    onValueChange = { dni = it },
                    label = { Text("DNI") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nombres,
                    onValueChange = { nombres = it },
                    label = { Text("Nombres") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it },
                    label = { Text("Apellidos") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de tipo de horario
                Text(
                    text = "Tipo de Horario:",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row {
                    RadioButton(
                        selected = tipoHorario == TipoHorario.REGULAR,
                        onClick = { tipoHorario = TipoHorario.REGULAR }
                    )
                    Text(
                        text = "Regular",
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = tipoHorario == TipoHorario.FLEXIBLE,
                        onClick = { tipoHorario = TipoHorario.FLEXIBLE }
                    )
                    Text(
                        text = "Flexible",
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (dni.isNotBlank() && nombres.isNotBlank() && apellidos.isNotBlank()) {
                        onEmpleadoAdded(dni, nombres, apellidos, tipoHorario)
                    }
                }
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
