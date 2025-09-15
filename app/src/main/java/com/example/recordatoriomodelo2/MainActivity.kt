package com.example.recordatoriomodelo2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.recordatoriomodelo2.ui.AppNavigation
import androidx.navigation.NavHostController
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatoriomodelo2.ui.screens.login.LoginViewModel
import com.example.recordatoriomodelo2.ui.screens.login.LoginEvent
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.Button
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun LoginScreenSimple(navController: NavHostController) {
    // Pantalla mínima para pruebas
    // Puedes restaurar la lógica de login después
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Button(onClick = {
            navController.navigate(com.example.recordatoriomodelo2.ui.Screen.Tasks.route)
        }) {
            Text("Ir a Tareas")
        }
    }
}