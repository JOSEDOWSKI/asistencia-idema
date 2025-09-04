package com.asistencia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.asistencia.app.ui.empleados.EmpleadosScreen
import com.asistencia.app.ui.theme.AsistenciaAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AsistenciaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AsistenciaApp()
                }
            }
        }
    }
}

@Composable
fun AsistenciaApp() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "empleados") {
        composable("empleados") {
            EmpleadosScreen(
                onNavigateToScanner = { navController.navigate("scanner") },
                onNavigateToReports = { navController.navigate("reports") }
            )
        }
        // TODO: Agregar más rutas para scanner y reports
    }
}
