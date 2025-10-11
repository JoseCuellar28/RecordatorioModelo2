package com.example.recordatoriomodelo2.data.repository

import android.content.Context
import android.util.Log
import com.example.recordatoriomodelo2.data.local.TaskDao
import com.example.recordatoriomodelo2.data.local.TaskEntity
import com.example.recordatoriomodelo2.data.sync.SyncManager
import com.example.recordatoriomodelo2.data.sync.SyncStatus
import com.example.recordatoriomodelo2.data.sync.SyncState
import com.example.recordatoriomodelo2.data.sync.ConflictResolutionResult
import com.example.recordatoriomodelo2.data.sync.SyncConflict
import com.example.recordatoriomodelo2.data.sync.ConflictResolution
import com.example.recordatoriomodelo2.services.SessionManager
import com.example.recordatoriomodelo2.middleware.SecurityMiddleware
import com.example.recordatoriomodelo2.middleware.SecurityResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositorio que maneja la sincronización entre Room (cache local) y Firestore (base de datos principal)
 * para las tareas del usuario con validaciones de seguridad multi-dispositivo.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    
    private val sessionManager: SessionManager by lazy { SessionManager.getInstance(context) }
    private val securityMiddleware: SecurityMiddleware by lazy { SecurityMiddleware.getInstance(context) }
    private val syncManager: SyncManager by lazy { SyncManager.getInstance(context) }
    
    // Registro temporal de tareas eliminadas recientemente para evitar resurrección
    private val recentlyDeletedTasks = mutableSetOf<Int>()
    private val deletionTimestamps = mutableMapOf<Int, Long>()
    
    companion object {
        private const val TAG = "🔍DEBUG_TaskRepo"
        private const val TASKS_COLLECTION = "tasks"
    }
    
    /**
     * Obtiene las tareas del usuario actual con sincronización en tiempo real mejorada
     * Combina datos locales (Room) con datos remotos (Firestore) usando SyncManager
     */
    fun getTasksForCurrentUser(): Flow<List<TaskEntity>> {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            Log.d(TAG, "🔄 Iniciando getTasksForCurrentUser para usuario: ${currentUser.uid}")
            
            // Usar el nuevo SyncManager para sincronización en tiempo real
            combine(
                getLocalTasks(currentUser.uid),
                syncManager.startRealtimeSync(currentUser.uid)
            ) { localTasks, firestoreTasks ->
                Log.d(TAG, "📊 COMBINE TRIGGERED:")
                Log.d(TAG, "  📱 Local tasks: ${localTasks.size} tareas")
                Log.d(TAG, "  ☁️ Firestore tasks: ${firestoreTasks.size} tareas")
                Log.d(TAG, "  🗑️ Recently deleted: ${recentlyDeletedTasks.size} tareas")
                
                // Log detalles de las tareas
                localTasks.forEach { task ->
                    Log.d(TAG, "  📱 Local: ID=${task.id}, title='${task.title}', deleted=${recentlyDeletedTasks.contains(task.id)}")
                }
                firestoreTasks.forEach { task ->
                    val isDeleted = recentlyDeletedTasks.contains(task.id)
                    Log.d(TAG, "  ☁️ Firestore: ID=${task.id}, title='${task.title}', deleted=$isDeleted")
                    
                    // ⚠️ ALERTA ESPECIAL: Tarea eliminada que aparece en Firestore
                    if (isDeleted) {
                        Log.w(TAG, "  ⚠️ PROBLEMA DETECTADO: Tarea eliminada ID=${task.id} aparece en Firestore!")
                        Log.w(TAG, "  ⚠️ Timestamp eliminación: ${deletionTimestamps[task.id]}")
                        Log.w(TAG, "  ⚠️ Tiempo transcurrido: ${System.currentTimeMillis() - (deletionTimestamps[task.id] ?: 0)}ms")
                    }
                }
                
                // Sincronizar automáticamente las diferencias
                syncTasksInBackground(localTasks, firestoreTasks, currentUser.uid)
                
                // Retornar las tareas más actualizadas (priorizar Firestore)
                val mergedTasks = mergeTaskLists(localTasks, firestoreTasks)
                Log.d(TAG, "  ✅ Merged result: ${mergedTasks.size} tareas")
                mergedTasks.forEach { task ->
                    Log.d(TAG, "  ✅ Final: ID=${task.id}, title='${task.title}'")
                }
                
                mergedTasks
            }
        } else {
            Log.w(TAG, "❌ Usuario no autenticado en getTasksForCurrentUser")
            flowOf(emptyList())
        }
    }
    
    /**
     * Obtiene el estado de sincronización actual
     */
    fun getSyncStatus(): StateFlow<SyncManager.SyncState> {
        return syncManager.syncState
    }
    
    /**
     * Obtiene el último error de sincronización
     */
    fun getLastSyncError(): StateFlow<String?> {
        return syncManager.lastError
    }
    
    /**
     * Obtiene el contador de tareas sincronizadas
     */
    fun getSyncedTasksCount(): StateFlow<Int> {
        return syncManager.syncedTasksCount
    }
    
    /**
     * Fuerza una sincronización manual
     */
    suspend fun forceSyncNow(): Result<Int> {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            syncManager.forceSyncNow(currentUser.uid)
        } else {
            Result.failure(Exception("Usuario no autenticado"))
        }
    }
    
    /**
     * Reinicia la sincronización (útil para reconexión)
     */
    fun restartSync() {
        syncManager.restartSync()
    }
    
    /**
     * Sincroniza tareas con manejo de conflictos
     */
    suspend fun syncTasksWithConflictResolution(
        localTasks: List<TaskEntity>,
        remoteTasks: List<TaskEntity>
    ): ConflictResolutionResult {
        return syncManager.syncTasksWithConflictResolution(localTasks, remoteTasks)
    }
    
    /**
     * Obtiene conflictos pendientes
     */
    suspend fun getPendingConflicts(): List<SyncConflict> {
        return syncManager.getPendingConflicts()
    }
    
    /**
     * Resuelve un conflicto manualmente
     */
    suspend fun resolveConflictManually(
        conflictId: String,
        resolution: ConflictResolution,
        mergedTask: TaskEntity? = null
    ): Boolean {
        return syncManager.resolveConflictManually(conflictId, resolution, mergedTask)
    }
    
    /**
     * Obtiene tareas locales de Room
     */
    private fun getLocalTasks(userId: String): Flow<List<TaskEntity>> {
        return taskDao.getTasksByUser(userId)
    }
    
    /**
     * Obtiene tareas de Firestore en tiempo real
     */
    private fun getFirestoreTasks(userId: String): Flow<List<TaskEntity>> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            listenerRegistration = firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error escuchando cambios en Firestore", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val tasks = snapshot.documents.mapNotNull { doc ->
                            try {
                                val taskEntity = doc.toObject(TaskEntity::class.java)
                                if (taskEntity != null) {
                                    // Intentar obtener el ID de diferentes formas
                                    val taskId = when {
                                        doc.contains("id") && doc.get("id") != null -> {
                                            when (val idValue = doc.get("id")) {
                                                is Long -> idValue.toInt()
                                                is Int -> idValue
                                                is String -> idValue.toIntOrNull() ?: taskEntity.id
                                                else -> taskEntity.id
                                            }
                                        }
                                        else -> taskEntity.id
                                    }
                                    
                                    taskEntity.copy(id = taskId)
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error convirtiendo documento a TaskEntity: ${e.message}", e)
                                null
                            }
                        }
                        Log.d(TAG, "Tareas recibidas de Firestore: ${tasks.size}")
                        trySend(tasks)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando listener de Firestore", e)
            trySend(emptyList())
        }
        
        awaitClose {
            listenerRegistration?.remove()
            Log.d(TAG, "Listener de Firestore removido")
        }
    }
    
    /**
     * Inserta una nueva tarea tanto en Room como en Firestore con validaciones de seguridad
     */
    suspend fun insertTask(task: TaskEntity): Result<Long> {
        return try {
            Log.d(TAG, "=== INICIO insertTask ===")
            Log.d(TAG, "Tarea recibida - userId: ${task.userId}, title: ${task.title}")
            
            // Validar operación con middleware de seguridad
            val securityResult = securityMiddleware.validateTaskOperation("INSERT_TASK")
            if (securityResult is SecurityResult.Failure) {
                securityMiddleware.logSecurityEvent("INSERT_TASK_DENIED", null, false, securityResult.message)
                return Result.failure(Exception(securityResult.message))
            }
            
            val userId = (securityResult as SecurityResult.Success).userId
            Log.d(TAG, "Usuario validado por SecurityMiddleware: $userId")
            
            // Asegurar que la tarea tenga el userId correcto
            val taskWithUser = task.copy(userId = userId)
            Log.d(TAG, "Tarea con userId asignado: ${taskWithUser.userId}")
            
            // 1. Insertar en Room primero (para respuesta rápida)
            val localId = taskDao.insertTask(taskWithUser)
            val taskWithId = taskWithUser.copy(id = localId.toInt())
            Log.d(TAG, "Tarea insertada en Room - ID: $localId, userId: ${taskWithId.userId}")
            
            // 2. Insertar en Firestore (sincronización)
            val firestoreData = taskToFirestoreMap(taskWithId)
            Log.d(TAG, "Datos para Firestore: $firestoreData")
            
            val documentId = generateTaskDocumentId(taskWithId)
            Log.d(TAG, "Document ID generado: $documentId")
            
            firestore.collection(TASKS_COLLECTION)
                .document(documentId)
                .set(firestoreData)
                .await()
            
            securityMiddleware.logSecurityEvent("INSERT_TASK_SUCCESS", userId, true, "Task ID: $localId")
            Log.d(TAG, "Tarea insertada exitosamente en Firestore - ID local: $localId, userId: $userId")
            Result.success(localId)
            
        } catch (e: Exception) {
            securityMiddleware.logSecurityEvent("INSERT_TASK_ERROR", null, false, e.message)
            Log.e(TAG, "Error insertando tarea", e)
            Result.failure(e)
        }
    }
    
    /**
     * Actualiza una tarea tanto en Room como en Firestore con validaciones de seguridad
     */
    suspend fun updateTask(task: TaskEntity): Result<Unit> {
        return try {
            // Validar acceso a la tarea específica
            val securityResult = securityMiddleware.validateTaskAccess(task.userId, "UPDATE_TASK")
            if (securityResult is SecurityResult.Failure) {
                securityMiddleware.logSecurityEvent("UPDATE_TASK_DENIED", task.userId, false, securityResult.message)
                return Result.failure(Exception(securityResult.message))
            }
            
            val userId = (securityResult as SecurityResult.Success).userId
            
            // Asegurar que la tarea pertenezca al usuario actual
            if (task.userId != userId) {
                securityMiddleware.logSecurityEvent("UPDATE_TASK_UNAUTHORIZED", userId, false, "Task belongs to: ${task.userId}")
                return Result.failure(Exception("No autorizado para actualizar esta tarea"))
            }
            
            // 1. Actualizar en Room
            taskDao.updateTask(task)
            
            // 2. Actualizar en Firestore
            val firestoreData = taskToFirestoreMap(task)
            firestore.collection(TASKS_COLLECTION)
                .document(generateTaskDocumentId(task))
                .set(firestoreData)
                .await()
            
            securityMiddleware.logSecurityEvent("UPDATE_TASK_SUCCESS", userId, true, "Task ID: ${task.id}")
            Log.d(TAG, "Tarea actualizada exitosamente - ID: ${task.id}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            securityMiddleware.logSecurityEvent("UPDATE_TASK_ERROR", task.userId, false, e.message)
            Log.e(TAG, "Error actualizando tarea", e)
            Result.failure(e)
        }
    }
    
    /**
     * Elimina una tarea tanto de Room como de Firestore con validaciones de seguridad
     */
    suspend fun deleteTask(task: TaskEntity): Result<Unit> {
        return try {
            // Validar acceso a la tarea específica
            val securityResult = securityMiddleware.validateTaskAccess(task.userId, "DELETE_TASK")
            if (securityResult is SecurityResult.Failure) {
                securityMiddleware.logSecurityEvent("DELETE_TASK_DENIED", task.userId, false, securityResult.message)
                return Result.failure(Exception(securityResult.message))
            }
            
            val userId = (securityResult as SecurityResult.Success).userId
            
            // Verificar autorización
            if (task.userId != userId) {
                securityMiddleware.logSecurityEvent("DELETE_TASK_UNAUTHORIZED", userId, false, "Task belongs to: ${task.userId}")
                return Result.failure(Exception("No autorizado para eliminar esta tarea"))
            }
            
            // Registrar la tarea como eliminada recientemente ANTES de eliminarla
            // Esto evita que el listener la "resucite" durante el proceso
            val currentTime = System.currentTimeMillis()
            recentlyDeletedTasks.add(task.id)
            deletionTimestamps[task.id] = currentTime
            
            // 🎯 NUEVA SOLUCIÓN: Marcar en SyncManager para filtrado inteligente
            Log.d(TAG, "🎯 LLAMANDO markTaskAsDeleted ANTES de eliminar de Firestore")
            syncManager.markTaskAsDeleted(task.id)
            Log.d(TAG, "✅ markTaskAsDeleted completado - tarea protegida contra reaparición")
            
            Log.d(TAG, "🗑️ INICIANDO ELIMINACIÓN:")
            Log.d(TAG, "  📝 Tarea: ID=${task.id}, title='${task.title}'")
            Log.d(TAG, "  ⏰ Timestamp: $currentTime")
            Log.d(TAG, "  📊 Recently deleted count: ${recentlyDeletedTasks.size}")
            Log.d(TAG, "  🔒 Registrada como eliminada recientemente")
            Log.d(TAG, "  🎯 Marcada en SyncManager para filtrado inteligente")
            
            // NUEVO ORDEN: Eliminar de Firestore PRIMERO
            // Esto evita que el listener reciba la tarea y la "resucite" 
            val firestoreDocId = generateTaskDocumentId(task)
            Log.d(TAG, "  ☁️ Eliminando de Firestore (doc: $firestoreDocId)...")
            firestore.collection(TASKS_COLLECTION)
                .document(firestoreDocId)
                .delete()
                .await()
            Log.d(TAG, "  ✅ Eliminada de Firestore exitosamente")
            
            // Luego eliminar de Room para actualizar la UI
            Log.d(TAG, "  📱 Eliminando de Room...")
            taskDao.deleteTask(task)
            Log.d(TAG, "  ✅ Eliminada de Room exitosamente")
            
            securityMiddleware.logSecurityEvent("DELETE_TASK_SUCCESS", userId, true, "Task ID: ${task.id}")
            Log.d(TAG, "Tarea eliminada exitosamente - ID: ${task.id}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            securityMiddleware.logSecurityEvent("DELETE_TASK_ERROR", task.userId, false, e.message)
            Log.e(TAG, "Error eliminando tarea", e)
            // Si falla la eliminación, remover del registro de eliminadas
            recentlyDeletedTasks.remove(task.id)
            deletionTimestamps.remove(task.id)
            Result.failure(e)
        }
    }
    
    /**
     * Sincroniza tareas en segundo plano con detección automática de conflictos
     */
    private suspend fun syncTasksInBackground(
        localTasks: List<TaskEntity>,
        firestoreTasks: List<TaskEntity>,
        userId: String
    ) {
        try {
            // Detectar conflictos automáticamente
            val conflictResult = syncManager.detectConflictsAutomatically(localTasks, firestoreTasks)
            
            if (conflictResult.conflicts.isNotEmpty()) {
                Log.w(TAG, "Conflictos detectados durante sincronización automática: ${conflictResult.conflicts.size}")
                // Los conflictos se manejarán en la UI a través del estado de sincronización
                return
            }
            
            // Si no hay conflictos, proceder con la sincronización normal
            // Encontrar tareas que están en Firestore pero no en Room
            val localTaskIds = localTasks.map { it.id }.toSet()
            val tasksToAddLocally = firestoreTasks.filter { it.id !in localTaskIds }
            
            // Agregar tareas faltantes a Room
            tasksToAddLocally.forEach { task ->
                try {
                    taskDao.insertTask(task)
                    Log.d(TAG, "Tarea sincronizada localmente: ${task.title}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error sincronizando tarea localmente", e)
                }
            }
            
            // Procesar tareas resueltas si las hay
            conflictResult.resolvedTasks.forEach { task ->
                try {
                    taskDao.updateTask(task)
                    Log.d(TAG, "Tarea resuelta actualizada: ${task.title}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error actualizando tarea resuelta", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización en segundo plano", e)
        }
    }
    
    /**
     * Combina las listas de tareas locales y remotas, priorizando las eliminaciones locales
     * y evitando la resurrección de tareas eliminadas recientemente
     */
    private fun mergeTaskLists(localTasks: List<TaskEntity>, firestoreTasks: List<TaskEntity>): List<TaskEntity> {
        Log.d(TAG, "🔄 MERGE TASK LISTS:")
        Log.d(TAG, "  📱 Local: ${localTasks.size} tareas")
        Log.d(TAG, "  ☁️ Firestore: ${firestoreTasks.size} tareas")
        Log.d(TAG, "  🗑️ Recently deleted: ${recentlyDeletedTasks.size} tareas")
        
        // Limpiar tareas eliminadas que ya han pasado suficiente tiempo (30 segundos)
        val currentTime = System.currentTimeMillis()
        val expiredDeletions = deletionTimestamps.filter { (_, timestamp) ->
            currentTime - timestamp > 30_000 // 30 segundos
        }.keys
        
        if (expiredDeletions.isNotEmpty()) {
            Log.d(TAG, "  🧹 Limpiando ${expiredDeletions.size} eliminaciones expiradas")
            expiredDeletions.forEach { taskId ->
                recentlyDeletedTasks.remove(taskId)
                deletionTimestamps.remove(taskId)
                Log.d(TAG, "    ✅ Expirada: ID=$taskId")
            }
        }
        
        // Crear un conjunto de IDs de tareas locales para búsqueda rápida
        val localTaskIds = localTasks.map { it.id }.toSet()
        Log.d(TAG, "  📋 Local IDs: $localTaskIds")
        
        // Filtrar tareas remotas para incluir solo las que existen localmente
        // y que no han sido eliminadas recientemente
        val filteredRemoteTasks = firestoreTasks.filter { remoteTask ->
            val existsLocally = localTaskIds.contains(remoteTask.id)
            val notRecentlyDeleted = !recentlyDeletedTasks.contains(remoteTask.id)
            val shouldInclude = existsLocally && notRecentlyDeleted
            
            Log.d(TAG, "  ☁️ Remote ID=${remoteTask.id}: existsLocally=$existsLocally, notRecentlyDeleted=$notRecentlyDeleted, include=$shouldInclude")
            shouldInclude
        }
        
        // Identificar tareas que solo existen localmente (no sincronizadas aún)
        val localOnlyTasks = localTasks.filter { localTask ->
            val notInFirestore = firestoreTasks.none { it.id == localTask.id }
            val notRecentlyDeleted = !recentlyDeletedTasks.contains(localTask.id)
            val shouldInclude = notInFirestore && notRecentlyDeleted
            
            Log.d(TAG, "  📱 Local ID=${localTask.id}: notInFirestore=$notInFirestore, notRecentlyDeleted=$notRecentlyDeleted, include=$shouldInclude")
            shouldInclude
        }
        
        Log.d(TAG, "  📊 Resultados del filtrado:")
        Log.d(TAG, "    ☁️ Filtered remote: ${filteredRemoteTasks.size}")
        Log.d(TAG, "    📱 Local only: ${localOnlyTasks.size}")
        Log.d(TAG, "    🗑️ Still recently deleted: ${recentlyDeletedTasks.size}")
        
        // Combinar: tareas remotas filtradas + tareas solo locales
        val mergedTasks = (filteredRemoteTasks + localOnlyTasks).distinctBy { it.id }
        
        Log.d(TAG, "  ✅ Lista final combinada: ${mergedTasks.size} tareas")
        mergedTasks.forEach { task ->
            Log.d(TAG, "    ✅ Final: ID=${task.id}, title='${task.title}'")
        }
        
        return mergedTasks
    }
    
    /**
     * Convierte TaskEntity a Map para Firestore
     */
    private fun taskToFirestoreMap(task: TaskEntity): Map<String, Any?> {
        return mapOf(
            "id" to task.id,
            "title" to task.title,
            "subject" to task.subject,
            "dueDate" to task.dueDate,
            "isCompleted" to task.isCompleted,
            "createdAt" to task.createdAt,
            "reminderAt" to task.reminderAt,
            "classroomId" to task.classroomId,
            "userId" to task.userId,
            "lastModified" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
    }
    
    /**
     * Genera un ID único para el documento de Firestore
     */
    private fun generateTaskDocumentId(task: TaskEntity): String {
        return "${task.userId}_${task.id}"
    }
    
    /**
     * Genera un ID único para el documento de Firestore
     */
    private fun generateFirestoreDocumentId(): String {
        return firestore.collection(TASKS_COLLECTION).document().id
    }
    
    // ==================== MÉTODOS DE VALIDACIÓN DE SEGURIDAD ====================
    
    /**
     * Valida que el usuario actual esté autenticado y tenga acceso a la operación
     */
    private suspend fun validateUserAccess(): Result<String> {
        return try {
            // Verificar autenticación básica
            val currentUser = sessionManager.getCurrentUser()
                ?: return Result.failure(SecurityException("Usuario no autenticado"))
            
            // Validar sesión activa
            val sessionValid = sessionManager.validateCurrentSession().getOrElse { false }
            if (!sessionValid) {
                return Result.failure(SecurityException("Sesión inválida o expirada"))
            }
            
            Log.d(TAG, "Acceso validado para usuario: ${currentUser.uid}")
            Result.success(currentUser.uid)
        } catch (e: Exception) {
            Log.e(TAG, "Error validando acceso de usuario", e)
            Result.failure(e)
        }
    }
    
    /**
     * Valida que el usuario tenga acceso a una tarea específica
     */
    private fun validateTaskAccess(task: TaskEntity, userId: String): Boolean {
        return task.userId?.let { taskUserId ->
            sessionManager.validateUserAccess(taskUserId) && taskUserId == userId
        } ?: false
    }
    
    /**
     * Versión segura de insertTask con validaciones adicionales
     */
    suspend fun insertTaskSecure(task: TaskEntity): Result<Unit> {
        return try {
            val userIdResult = validateUserAccess()
            if (userIdResult.isFailure) {
                return Result.failure(userIdResult.exceptionOrNull() ?: SecurityException("Acceso denegado"))
            }
            
            val userId = userIdResult.getOrThrow()
            val secureTask = task.copy(userId = userId) // Asegurar que el userId sea correcto
            
            insertTask(secureTask).map { Unit }
        } catch (e: Exception) {
            Log.e(TAG, "Error en inserción segura de tarea", e)
            Result.failure(e)
        }
    }
    
    /**
     * Versión segura de updateTask con validaciones adicionales
     */
    suspend fun updateTaskSecure(task: TaskEntity): Result<Unit> {
        return try {
            val userIdResult = validateUserAccess()
            if (userIdResult.isFailure) {
                return Result.failure(userIdResult.exceptionOrNull() ?: SecurityException("Acceso denegado"))
            }
            
            val userId = userIdResult.getOrThrow()
            
            // Validar que el usuario tenga acceso a esta tarea
            if (!validateTaskAccess(task, userId)) {
                return Result.failure(SecurityException("No tienes permisos para modificar esta tarea"))
            }
            
            updateTask(task)
        } catch (e: Exception) {
            Log.e(TAG, "Error en actualización segura de tarea", e)
            Result.failure(e)
        }
    }
    
    /**
     * Versión segura de deleteTask con validaciones adicionales
     */
    suspend fun deleteTaskSecure(task: TaskEntity): Result<Unit> {
        return try {
            val userIdResult = validateUserAccess()
            if (userIdResult.isFailure) {
                return Result.failure(userIdResult.exceptionOrNull() ?: SecurityException("Acceso denegado"))
            }
            
            val userId = userIdResult.getOrThrow()
            
            // Validar que el usuario tenga acceso a esta tarea
            if (!validateTaskAccess(task, userId)) {
                return Result.failure(SecurityException("No tienes permisos para eliminar esta tarea"))
            }
            
            deleteTask(task)
        } catch (e: Exception) {
            Log.e(TAG, "Error en eliminación segura de tarea", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene información de sesiones activas del usuario
     */
    suspend fun getActiveUserSessions(): Result<List<SessionManager.SessionInfo>> {
        return try {
            val userIdResult = validateUserAccess()
            if (userIdResult.isFailure) {
                return Result.failure(userIdResult.exceptionOrNull() ?: SecurityException("Acceso denegado"))
            }
            
            sessionManager.getActiveSessions()
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo sesiones activas", e)
            Result.failure(e)
        }
    }
    
    /**
     * Revoca una sesión específica
     */
    suspend fun revokeUserSession(deviceId: String): Result<Unit> {
        return try {
            val userIdResult = validateUserAccess()
            if (userIdResult.isFailure) {
                return Result.failure(userIdResult.exceptionOrNull() ?: SecurityException("Acceso denegado"))
            }
            
            sessionManager.revokeSession(deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error revocando sesión", e)
            Result.failure(e)
        }
    }
}