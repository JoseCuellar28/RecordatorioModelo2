package com.example.recordatoriomodelo2.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatoriomodelo2.services.ConnectivityService

/**
 * Barra de estado que se muestra cuando la aplicación está en modo offline
 */
@Composable
fun OfflineStatusBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectivityService = remember { ConnectivityService.getInstance(context) }
    val isConnected by connectivityService.isConnected.collectAsStateWithLifecycle()
    
    // Mostrar la barra solo cuando no hay conexión
    AnimatedVisibility(
        visible = !isConnected,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD32F2F)) // Rojo material
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Sin conexión",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Modo offline",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Versión compacta de la barra de estado offline
 */
@Composable
fun CompactOfflineStatusBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectivityService = remember { ConnectivityService.getInstance(context) }
    val isConnected by connectivityService.isConnected.collectAsStateWithLifecycle()
    
    AnimatedVisibility(
        visible = !isConnected,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD32F2F))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Sin conexión",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = "Offline",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}