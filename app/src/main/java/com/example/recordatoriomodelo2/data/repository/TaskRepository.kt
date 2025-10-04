package com.example.recordatoriomodelo2.data.repository

import android.content.Context
import android.util.Log
import com.example.recordatoriomodelo2.data.local.TaskDao
import com.example.recordatoriomodelo2.data.local.TaskEntity
import com.example.recordatoriomodelo2.services.SessionManager
import com.example.recordatoriomodelo2.middleware.SecurityMiddleware
import com.example.recordatoriomodelo2.middleware.SecurityResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    
    companion object {
        private const val TAG = "TaskRepository"
        private const val TASKS_COLLECTION = "tasks"
    }
    
    /**
     * Obtiene las tareas del usuario actual con sincronización en tiempo real
     * Combina datos locales (Room) con datos remotos (Firestore)
     */
    fun getTasksForCurrentUser(): Flow<List<TaskEntity>> {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            // Combinar datos locales con sincronización en tiempo real de Firestore
            combine(
                getLocalTasks(currentUser.uid),
                getFirestoreTasks(currentUser.uid)
            ) { localTasks, firestoreTasks ->
                // Sincronizar automáticamente las diferencias
                syncTasksInBackground(localTasks, firestoreTasks, currentUser.uid)
                
                // Retornar las tareas más actualizadas (priorizar Firestore)
                mergeTaskLists(localTasks, firestoreTasks)
            }
        } else {
            flowOf(emptyList())
        }
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
            
            // 1. Eliminar de Room
            taskDao.deleteTask(task)
            
            // 2. Eliminar de Firestore
            firestore.collection(TASKS_COLLECTION)
                .document(generateTaskDocumentId(task))
                .delete()
                .await()
            
            securityMiddleware.logSecurityEvent("DELETE_TASK_SUCCESS", userId, true, "Task ID: ${task.id}")
            Log.d(TAG, "Tarea eliminada exitosamente - ID: ${task.id}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            securityMiddleware.logSecurityEvent("DELETE_TASK_ERROR", task.userId, false, e.message)
            Log.e(TAG, "Error eliminando tarea", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sincroniza tareas en segundo plano
     */
    private suspend fun syncTasksInBackground(
        localTasks: List<TaskEntity>,
        firestoreTasks: List<TaskEntity>,
        userId: String
    ) {
        try {
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
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización en segundo plano", e)
        }
    }
    
    /**
     * Combina listas de tareas priorizando datos de Firestore
     */
    private fun mergeTaskLists(localTasks: List<TaskEntity>, firestoreTasks: List<TaskEntity>): List<TaskEntity> {
        Log.d(TAG, "=== MERGE TASK LISTS ===")
        Log.d(TAG, "Tareas locales: ${localTasks.size}")
        localTasks.forEach { task ->
            Log.d(TAG, "Local: ID=${task.id}, Title='${task.title}', UserId=${task.userId}")
        }
        
        Log.d(TAG, "Tareas de Firestore: ${firestoreTasks.size}")
        firestoreTasks.forEach { task ->
            Log.d(TAG, "Firestore: ID=${task.id}, Title='${task.title}', UserId=${task.userId}")
        }
        
        val firestoreTaskIds = firestoreTasks.map { it.id }.toSet()
        val localOnlyTasks = localTasks.filter { it.id !in firestoreTaskIds }
        
        Log.d(TAG, "Tareas solo locales (no en Firestore): ${localOnlyTasks.size}")
        localOnlyTasks.forEach { task ->
            Log.d(TAG, "Solo local: ID=${task.id}, Title='${task.title}'")
        }
        
        val mergedTasks = (firestoreTasks + localOnlyTasks).sortedWith(
            compareBy<TaskEntity> { it.isCompleted }.thenByDescending { it.createdAt }
        )
        
        Log.d(TAG, "Total tareas combinadas: ${mergedTasks.size}")
        Log.d(TAG, "=== FIN MERGE TASK LISTS ===")
        
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