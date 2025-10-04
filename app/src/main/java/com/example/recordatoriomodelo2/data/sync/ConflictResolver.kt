package com.example.recordatoriomodelo2.data.sync

import android.util.Log
import com.example.recordatoriomodelo2.data.local.TaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Resuelve conflictos de sincronización entre datos locales y remotos
 */
class ConflictResolver {
    
    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts: StateFlow<List<SyncConflict>> = _conflicts.asStateFlow()
    
    companion object {
        private const val TAG = "ConflictResolver"
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        @Volatile
        private var INSTANCE: ConflictResolver? = null
        
        fun getInstance(): ConflictResolver {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConflictResolver().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Detecta y resuelve conflictos entre tareas locales y remotas
     */
    fun resolveTaskConflicts(
        localTasks: List<TaskEntity>,
        remoteTasks: List<TaskEntity>
    ): ConflictResolutionResult {
        val conflicts = mutableListOf<SyncConflict>()
        val resolvedTasks = mutableListOf<TaskEntity>()
        val tasksToDelete = mutableListOf<TaskEntity>()
        
        // Crear mapas para búsqueda eficiente
        val localTasksMap = localTasks.associateBy { it.id }
        val remoteTasksMap = remoteTasks.associateBy { it.id }
        
        // Detectar conflictos en tareas existentes en ambos lados
        for (localTask in localTasks) {
            val remoteTask = remoteTasksMap[localTask.id]
            
            if (remoteTask != null) {
                val conflict = detectConflict(localTask, remoteTask)
                if (conflict != null) {
                    conflicts.add(conflict)
                    // Resolver automáticamente según la estrategia
                    val resolved = resolveConflictAutomatically(conflict)
                    resolvedTasks.add(resolved)
                } else {
                    // No hay conflicto, usar la versión más reciente
                    resolvedTasks.add(getMostRecentTask(localTask, remoteTask))
                }
            } else {
                // Tarea solo existe localmente
                resolvedTasks.add(localTask)
            }
        }
        
        // Agregar tareas que solo existen remotamente
        for (remoteTask in remoteTasks) {
            if (!localTasksMap.containsKey(remoteTask.id)) {
                resolvedTasks.add(remoteTask)
            }
        }
        
        // Actualizar lista de conflictos
        _conflicts.value = conflicts
        
        Log.d(TAG, "Conflictos detectados: ${conflicts.size}")
        Log.d(TAG, "Tareas resueltas: ${resolvedTasks.size}")
        
        return ConflictResolutionResult(
            resolvedTasks = resolvedTasks,
            conflicts = conflicts,
            tasksToDelete = tasksToDelete
        )
    }
    
    /**
     * Detecta si hay conflicto entre dos versiones de la misma tarea
     */
    private fun detectConflict(localTask: TaskEntity, remoteTask: TaskEntity): SyncConflict? {
        val differences = mutableListOf<String>()
        
        // Comparar campos importantes
        if (localTask.title != remoteTask.title) {
            differences.add("título")
        }
        if (localTask.subject != remoteTask.subject) {
            differences.add("materia")
        }
        if (localTask.dueDate != remoteTask.dueDate) {
            differences.add("fecha de vencimiento")
        }
        if (localTask.isCompleted != remoteTask.isCompleted) {
            differences.add("estado de completado")
        }
        if (localTask.reminderAt != remoteTask.reminderAt) {
            differences.add("recordatorio")
        }
        
        return if (differences.isNotEmpty()) {
            val conflictType = when {
                differences.contains("título") || differences.contains("materia") -> ConflictType.CONTENT_MODIFIED
                else -> ConflictType.CONTENT_MODIFIED // Por ahora usar CONTENT_MODIFIED para todos los casos
            }
            
            SyncConflict(
                taskId = localTask.id,
                localTask = localTask,
                remoteTask = remoteTask,
                conflictType = conflictType,
                differences = differences,
                timestamp = System.currentTimeMillis()
            )
        } else {
            null
        }
    }
    
    /**
     * Resuelve un conflicto automáticamente según estrategias predefinidas
     */
    private fun resolveConflictAutomatically(conflict: SyncConflict): TaskEntity {
        return when (conflict.conflictType) {
            ConflictType.CONTENT_MODIFIED -> {
                // Para cambios de contenido, usar la versión más reciente
                Log.d(TAG, "Resolviendo conflicto de contenido: usando versión más reciente")
                getMostRecentTask(conflict.localTask, conflict.remoteTask)
            }
            
            ConflictType.DELETED_LOCALLY -> {
                // Si fue eliminado localmente, mantener eliminado
                Log.d(TAG, "Resolviendo conflicto: tarea eliminada localmente")
                conflict.localTask
            }
            
            ConflictType.DELETED_REMOTELY -> {
                // Si fue eliminado remotamente, mantener eliminado
                Log.d(TAG, "Resolviendo conflicto: tarea eliminada remotamente")
                conflict.remoteTask
            }
            
            ConflictType.CREATION_CONFLICT -> {
                // Para conflictos de creación, usar la versión más reciente
                Log.d(TAG, "Resolviendo conflicto de creación: usando versión más reciente")
                getMostRecentTask(conflict.localTask, conflict.remoteTask)
            }
        }
    }
    
    /**
     * Obtiene la tarea más reciente basándose en la fecha de creación
     */
    private fun getMostRecentTask(localTask: TaskEntity, remoteTask: TaskEntity): TaskEntity {
        val localDate = parseDate(localTask.createdAt)
        val remoteDate = parseDate(remoteTask.createdAt)
        
        return when {
            localDate == null && remoteDate != null -> remoteTask
            remoteDate == null && localDate != null -> localTask
            localDate != null && remoteDate != null -> {
                if (localDate.after(remoteDate)) localTask else remoteTask
            }
            else -> localTask // Por defecto, mantener local
        }
    }
    
    /**
     * Parsea una fecha en formato string
     */
    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null
        
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            Log.w(TAG, "Error al parsear fecha: $dateString", e)
            null
        }
    }
    
    /**
     * Resuelve un conflicto manualmente con la estrategia especificada
     */
    fun resolveConflictManually(
        conflictId: String,
        resolution: ConflictResolution,
        mergedTask: TaskEntity? = null
    ): Boolean {
        val currentConflicts = _conflicts.value.toMutableList()
        val conflict = currentConflicts.find { "${it.taskId}_${it.timestamp}" == conflictId }
            ?: return false
        
        val resolvedTask = when (resolution) {
            ConflictResolution.PREFER_LOCAL -> conflict.localTask
            ConflictResolution.PREFER_REMOTE -> conflict.remoteTask
            ConflictResolution.MERGE_CONTENT -> mergedTask ?: mergeTasksManually(conflict.localTask, conflict.remoteTask)
            ConflictResolution.PREFER_NEWEST -> getMostRecentTask(conflict.localTask, conflict.remoteTask)
            ConflictResolution.ASK_USER -> conflict.localTask // Por defecto usar local si se pide al usuario
        }
        
        // Remover el conflicto resuelto
        currentConflicts.removeAll { "${it.taskId}_${it.timestamp}" == conflictId }
        _conflicts.value = currentConflicts
        
        Log.d(TAG, "Conflicto resuelto manualmente: $conflictId con estrategia $resolution")
        
        return true
    }
    
    /**
     * Combina dos tareas manualmente (estrategia de merge)
     */
    private fun mergeTasksManually(localTask: TaskEntity, remoteTask: TaskEntity): TaskEntity {
        // Estrategia de merge: combinar lo mejor de ambas versiones
        return localTask.copy(
            title = if (localTask.title.isNotEmpty()) localTask.title else remoteTask.title,
            subject = if (localTask.subject.isNotEmpty()) localTask.subject else remoteTask.subject,
            dueDate = if (localTask.dueDate.isNotEmpty()) localTask.dueDate else remoteTask.dueDate,
            isCompleted = localTask.isCompleted || remoteTask.isCompleted, // Si cualquiera está completada
            reminderAt = localTask.reminderAt ?: remoteTask.reminderAt,
            createdAt = getMostRecentTask(localTask, remoteTask).createdAt
        )
    }
    
    /**
     * Obtiene la lista actual de conflictos pendientes
     */
    fun getPendingConflicts(): List<SyncConflict> {
        return _conflicts.value
    }
    
    /**
     * Limpia conflictos antiguos (más de 24 horas)
     */
    fun cleanOldConflicts() {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000) // 24 horas
        
        val currentConflicts = _conflicts.value
        val recentConflicts = currentConflicts.filter { it.timestamp > oneDayAgo }
        
        if (recentConflicts.size != currentConflicts.size) {
            _conflicts.value = recentConflicts
            Log.d(TAG, "Limpiados ${currentConflicts.size - recentConflicts.size} conflictos antiguos")
        }
    }
}

/**
 * Resultado de la resolución de conflictos
 */
data class ConflictResolutionResult(
    val resolvedTasks: List<TaskEntity>,
    val conflicts: List<SyncConflict>,
    val tasksToDelete: List<TaskEntity>
)