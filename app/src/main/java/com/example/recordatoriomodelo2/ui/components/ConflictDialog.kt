package com.example.recordatoriomodelo2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.recordatoriomodelo2.data.local.TaskEntity
import com.example.recordatoriomodelo2.data.sync.ConflictResolution
import com.example.recordatoriomodelo2.data.sync.ConflictType
import com.example.recordatoriomodelo2.data.sync.SyncConflict
import java.text.SimpleDateFormat
import java.util.*

/**
 * Diálogo para mostrar y resolver conflictos de sincronización
 */
@Composable
fun ConflictDialog(
    conflicts: List<SyncConflict>,
    onResolveConflict: (String, ConflictResolution, TaskEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Conflictos de Sincronización",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${conflicts.size} conflicto(s) detectado(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lista de conflictos
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conflicts) { conflict ->
                        ConflictItem(
                            conflict = conflict,
                            onResolve = { resolution, mergedTask ->
                                onResolveConflict(conflict.id, resolution, mergedTask)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botones de acción global
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            conflicts.forEach { conflict ->
                                onResolveConflict(conflict.id, ConflictResolution.PREFER_LOCAL, null)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mantener Todo Local")
                    }
                    
                    Button(
                        onClick = {
                            conflicts.forEach { conflict ->
                                onResolveConflict(conflict.id, ConflictResolution.PREFER_REMOTE, null)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mantener Todo Remoto")
                    }
                }
            }
        }
    }
}

/**
 * Item individual de conflicto
 */
@Composable
private fun ConflictItem(
    conflict: SyncConflict,
    onResolve: (ConflictResolution, TaskEntity?) -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (conflict.conflictType) {
                ConflictType.CONTENT_MODIFIED -> MaterialTheme.colorScheme.errorContainer
                ConflictType.DELETED_LOCALLY -> MaterialTheme.colorScheme.warningContainer
                ConflictType.DELETED_REMOTELY -> MaterialTheme.colorScheme.primaryContainer
                ConflictType.CREATION_CONFLICT -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header del conflicto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = conflict.localTask.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = getConflictTypeDescription(conflict.conflictType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { showDetails = !showDetails }) {
                    Icon(
                        imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showDetails) "Ocultar detalles" else "Mostrar detalles"
                    )
                }
            }
            
            // Detalles expandibles
            if (showDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                
                ConflictDetails(
                    localTask = conflict.localTask,
                    remoteTask = conflict.remoteTask,
                    conflictType = conflict.conflictType
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Botones de resolución
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onResolve(ConflictResolution.PREFER_LOCAL, null) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Local", maxLines = 1)
                }
                
                OutlinedButton(
                    onClick = { onResolve(ConflictResolution.PREFER_REMOTE, null) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Remoto", maxLines = 1)
                }
                
                if (conflict.conflictType == ConflictType.CONTENT_MODIFIED) {
                    Button(
                        onClick = {
                            // Crear tarea fusionada automáticamente
                            val mergedTask = createMergedTask(conflict.localTask, conflict.remoteTask)
                            onResolve(ConflictResolution.MERGE_CONTENT, mergedTask)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fusionar", maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Muestra los detalles del conflicto
 */
@Composable
private fun ConflictDetails(
    localTask: TaskEntity,
    remoteTask: TaskEntity,
    conflictType: ConflictType
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    Column {
        Text(
            text = "Detalles del Conflicto:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Comparación lado a lado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Versión local
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Local",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    TaskDetails(task = localTask, dateFormat = dateFormat)
                }
            }
            
            // Versión remota
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Remoto",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    TaskDetails(task = remoteTask, dateFormat = dateFormat)
                }
            }
        }
    }
}

/**
 * Muestra los detalles de una tarea
 */
@Composable
private fun TaskDetails(
    task: TaskEntity,
    dateFormat: SimpleDateFormat
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        if (task.subject.isNotBlank()) {
            Text(
                text = "Materia: ${task.subject}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "Estado: ${if (task.isCompleted) "Completada" else "Pendiente"}",
            style = MaterialTheme.typography.bodySmall,
            color = if (task.isCompleted) Color.Green else Color(0xFFFF9800)
        )
        
        if (task.dueDate.isNotBlank()) {
            Text(
                text = "Vence: ${task.dueDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "Modificado: ${dateFormat.format(Date(task.createdAt))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Obtiene la descripción del tipo de conflicto
 */
private fun getConflictTypeDescription(type: ConflictType): String {
    return when (type) {
        ConflictType.CONTENT_MODIFIED -> "Contenido modificado"
        ConflictType.DELETED_LOCALLY -> "Eliminado localmente"
        ConflictType.DELETED_REMOTELY -> "Eliminado remotamente"
        ConflictType.CREATION_CONFLICT -> "Conflicto de creación"
    }
}

/**
 * Crea una tarea fusionada automáticamente
 */
private fun createMergedTask(localTask: TaskEntity, remoteTask: TaskEntity): TaskEntity {
    return localTask.copy(
        // Mantener el título más reciente
        title = if (localTask.createdAt > remoteTask.createdAt) localTask.title else remoteTask.title,
        // Combinar materias si son diferentes
        subject = if (localTask.subject != remoteTask.subject) {
            "${localTask.subject} / ${remoteTask.subject}".trim()
        } else {
            localTask.subject
        },
        // Mantener el estado de completado si alguna está completada
        isCompleted = localTask.isCompleted || remoteTask.isCompleted,
        // Usar la fecha de vencimiento más temprana
        dueDate = if (localTask.dueDate.isNotBlank() && remoteTask.dueDate.isNotBlank()) {
            if (localTask.dueDate <= remoteTask.dueDate) localTask.dueDate else remoteTask.dueDate
        } else {
            localTask.dueDate.ifBlank { remoteTask.dueDate }
        },
        // Usar el timestamp más reciente
        createdAt = maxOf(localTask.createdAt, remoteTask.createdAt)
    )
}

// Color de advertencia personalizado
@get:Composable
val ColorScheme.warningContainer: Color
    get() = Color(0xFFFFF3CD)