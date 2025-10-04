package com.example.recordatoriomodelo2.data.sync

import com.example.recordatoriomodelo2.data.local.TaskEntity
import java.util.Date

/**
 * Estados de sincronización para la UI
 */
data class SyncStatus(
    val state: SyncState,
    val lastSyncTime: Date? = null,
    val syncedItemsCount: Int = 0,
    val pendingItemsCount: Int = 0,
    val errorMessage: String? = null,
    val isOnline: Boolean = true
) {
    
    /**
     * Indica si la sincronización está en progreso
     */
    val isLoading: Boolean
        get() = state == SyncState.CONNECTING || state == SyncState.SYNCING
    
    /**
     * Indica si hay un error activo
     */
    val hasError: Boolean
        get() = state == SyncState.ERROR && errorMessage != null
    
    /**
     * Indica si está completamente sincronizado
     */
    val isSynced: Boolean
        get() = state == SyncState.SYNCED && pendingItemsCount == 0
    
    /**
     * Mensaje descriptivo del estado actual
     */
    val statusMessage: String
        get() = when (state) {
            SyncState.IDLE -> "Listo para sincronizar"
            SyncState.CONNECTING -> "Conectando..."
            SyncState.SYNCING -> "Sincronizando $syncedItemsCount elementos..."
            SyncState.SYNCED -> when {
                pendingItemsCount > 0 -> "Sincronizado - $pendingItemsCount pendientes"
                else -> "Sincronizado - ${syncedItemsCount} elementos"
            }
            SyncState.ERROR -> errorMessage ?: "Error de sincronización"
            SyncState.OFFLINE -> "Sin conexión - modo offline"
        }
    
    /**
     * Color sugerido para el indicador de estado
     */
    val statusColor: SyncStatusColor
        get() = when {
            hasError -> SyncStatusColor.ERROR
            state == SyncState.OFFLINE -> SyncStatusColor.WARNING
            isLoading -> SyncStatusColor.LOADING
            isSynced -> SyncStatusColor.SUCCESS
            else -> SyncStatusColor.NEUTRAL
        }
}

/**
 * Colores para los indicadores de estado
 */
enum class SyncStatusColor {
    SUCCESS,    // Verde - Todo bien
    WARNING,    // Amarillo - Advertencia
    ERROR,      // Rojo - Error
    LOADING,    // Azul - Cargando
    NEUTRAL     // Gris - Estado neutro
}

/**
 * Estados de sincronización
 */
enum class SyncState {
    IDLE,           // Sin actividad
    CONNECTING,     // Conectando a Firestore
    SYNCING,        // Sincronizando datos
    SYNCED,         // Sincronizado exitosamente
    ERROR,          // Error en sincronización
    OFFLINE         // Sin conexión
}

/**
 * Información de conflicto de sincronización
 */
data class SyncConflict(
    val taskId: Int,
    val localTask: TaskEntity,
    val remoteTask: TaskEntity,
    val conflictType: ConflictType,
    val differences: List<String>,
    val timestamp: Long
) {
    val id: String get() = "${taskId}_${timestamp}"
}

/**
 * Tipos de conflicto
 */
enum class ConflictType {
    CONTENT_MODIFIED,   // Contenido modificado en ambos lados
    DELETED_LOCALLY,    // Eliminado localmente pero modificado remotamente
    DELETED_REMOTELY,   // Eliminado remotamente pero modificado localmente
    CREATION_CONFLICT   // Creado en ambos lados con diferentes datos
}

/**
 * Estrategias de resolución de conflictos
 */
enum class ConflictResolution {
    PREFER_LOCAL,       // Priorizar versión local
    PREFER_REMOTE,      // Priorizar versión remota
    PREFER_NEWEST,      // Priorizar la más reciente
    MERGE_CONTENT,      // Intentar fusionar contenido
    ASK_USER           // Preguntar al usuario
}