package com.example.recordatoriomodelo2.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recordatoriomodelo2.data.sync.SyncStatus
import com.example.recordatoriomodelo2.data.sync.SyncState
import com.example.recordatoriomodelo2.ui.TasksViewModel

/**
 * Componente que muestra indicadores visuales del estado de sincronización
 */
@Composable
fun SyncIndicator(
    uiState: TasksViewModel.TasksUiState,
    onRetrySync: () -> Unit = {},
    onForceSync: () -> Unit = {},
    onShowConflicts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val syncStatus = uiState.syncStatus
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = syncStatus.statusColor.toComposeColor().copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono y estado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SyncIcon(syncStatus = syncStatus)
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = syncStatus.statusMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = syncStatus.statusColor.toComposeColor()
                    )
                    
                    if (syncStatus.isOnline) {
                        Text(
                            text = "Tareas sincronizadas: ${uiState.syncedTasksCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (syncStatus.lastSyncTime != null) {
                        Text(
                            text = "Última sincronización: ${formatSyncTime(syncStatus.lastSyncTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (uiState.lastError != null) {
                        Text(
                            text = uiState.lastError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    if (uiState.pendingConflicts.isNotEmpty()) {
                        Text(
                            text = "${uiState.pendingConflicts.size} conflicto(s) pendiente(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Botones de acción
            Row {
                if (uiState.pendingConflicts.isNotEmpty()) {
                    IconButton(
                        onClick = onShowConflicts,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Ver conflictos",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                if (syncStatus.hasError) {
                    IconButton(
                        onClick = onRetrySync,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reintentar sincronización",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (!syncStatus.isLoading) {
                    IconButton(
                        onClick = onForceSync,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Forzar sincronización",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Icono animado que representa el estado de sincronización
 */
@Composable
private fun SyncIcon(syncStatus: SyncStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(syncStatus.statusColor.toComposeColor().copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        when (syncStatus.state) {
            SyncState.CONNECTING -> {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = "Conectando",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                    tint = syncStatus.statusColor.toComposeColor()
                )
            }
            SyncState.SYNCING -> {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = "Sincronizando",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                    tint = syncStatus.statusColor.toComposeColor()
                )
            }
            SyncState.SYNCED -> {
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = "Sincronizado",
                    modifier = Modifier.size(24.dp),
                    tint = syncStatus.statusColor.toComposeColor()
                )
            }
            SyncState.ERROR -> {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = "Error de sincronización",
                    modifier = Modifier.size(24.dp),
                    tint = syncStatus.statusColor.toComposeColor()
                )
            }
            SyncState.OFFLINE -> {
                Icon(
                    Icons.Default.WifiOff,
                    contentDescription = "Sin conexión",
                    modifier = Modifier.size(24.dp),
                    tint = syncStatus.statusColor.toComposeColor()
                )
            }
            SyncState.IDLE -> {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = "En espera",
                    modifier = Modifier.size(24.dp),
                    tint = syncStatus.statusColor.toComposeColor()
                )
            }
        }
    }
}

/**
 * Convierte SyncStatusColor a Color de Compose
 */
private fun com.example.recordatoriomodelo2.data.sync.SyncStatusColor.toComposeColor(): Color {
    return when (this) {
        com.example.recordatoriomodelo2.data.sync.SyncStatusColor.SUCCESS -> Color(0xFF10B981)
        com.example.recordatoriomodelo2.data.sync.SyncStatusColor.WARNING -> Color(0xFFF59E0B)
        com.example.recordatoriomodelo2.data.sync.SyncStatusColor.ERROR -> Color(0xFFEF4444)
        com.example.recordatoriomodelo2.data.sync.SyncStatusColor.LOADING -> Color(0xFF3B82F6)
        com.example.recordatoriomodelo2.data.sync.SyncStatusColor.NEUTRAL -> Color(0xFF6B7280)
    }
}

/**
 * Formatea el tiempo de sincronización para mostrar
 */
private fun formatSyncTime(date: java.util.Date): String {
    val now = System.currentTimeMillis()
    val diff = now - date.time
    
    return when {
        diff < 60_000 -> "Hace menos de 1 minuto"
        diff < 3600_000 -> "Hace ${diff / 60_000} minutos"
        diff < 86400_000 -> "Hace ${diff / 3600_000} horas"
        else -> "Hace más de 1 día"
    }
}

/**
 * Indicador compacto para la barra superior
 */
@Composable
fun CompactSyncIndicator(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SyncIcon(syncStatus = syncStatus)
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = when (syncStatus.state) {
                SyncState.CONNECTING -> "Conectando..."
                SyncState.SYNCING -> "Sincronizando..."
                SyncState.SYNCED -> "Sincronizado"
                SyncState.ERROR -> "Error"
                SyncState.OFFLINE -> "Sin conexión"
                SyncState.IDLE -> "En espera"
            },
            style = MaterialTheme.typography.bodySmall,
            color = syncStatus.statusColor.toComposeColor()
        )
    }
}