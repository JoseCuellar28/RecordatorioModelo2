package com.example.recordatoriomodelo2.data.repository

import android.util.Log
import com.example.recordatoriomodelo2.data.local.TaskDao
import com.example.recordatoriomodelo2.data.local.TaskEntity
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
 * para las tareas del usuario.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    
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
                                doc.toObject(TaskEntity::class.java)?.copy(
                                    id = doc.getLong("id")?.toInt() ?: 0
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error convirtiendo documento a TaskEntity", e)
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
     * Inserta una nueva tarea tanto en Room como en Firestore
     */
    suspend fun insertTask(task: TaskEntity): Result<Long> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
            
            // Asegurar que la tarea tenga el userId correcto
            val taskWithUser = task.copy(userId = currentUser.uid)
            
            // 1. Insertar en Room primero (para respuesta rápida)
            val localId = taskDao.insertTask(taskWithUser)
            val taskWithId = taskWithUser.copy(id = localId.toInt())
            
            // 2. Insertar en Firestore (sincronización)
            val firestoreData = taskToFirestoreMap(taskWithId)
            firestore.collection(TASKS_COLLECTION)
                .document(generateTaskDocumentId(taskWithId))
                .set(firestoreData)
                .await()
            
            Log.d(TAG, "Tarea insertada exitosamente - ID local: $localId")
            Result.success(localId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error insertando tarea", e)
            Result.failure(e)
        }
    }
    
    /**
     * Actualiza una tarea tanto en Room como en Firestore
     */
    suspend fun updateTask(task: TaskEntity): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
            
            // Asegurar que la tarea pertenezca al usuario actual
            if (task.userId != currentUser.uid) {
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
            
            Log.d(TAG, "Tarea actualizada exitosamente - ID: ${task.id}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando tarea", e)
            Result.failure(e)
        }
    }
    
    /**
     * Elimina una tarea tanto de Room como de Firestore
     */
    suspend fun deleteTask(task: TaskEntity): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
            
            // Verificar autorización
            if (task.userId != currentUser.uid) {
                return Result.failure(Exception("No autorizado para eliminar esta tarea"))
            }
            
            // 1. Eliminar de Room
            taskDao.deleteTask(task)
            
            // 2. Eliminar de Firestore
            firestore.collection(TASKS_COLLECTION)
                .document(generateTaskDocumentId(task))
                .delete()
                .await()
            
            Log.d(TAG, "Tarea eliminada exitosamente - ID: ${task.id}")
            Result.success(Unit)
            
        } catch (e: Exception) {
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
        val firestoreTaskIds = firestoreTasks.map { it.id }.toSet()
        val localOnlyTasks = localTasks.filter { it.id !in firestoreTaskIds }
        
        return (firestoreTasks + localOnlyTasks).sortedWith(
            compareBy<TaskEntity> { it.isCompleted }.thenByDescending { it.createdAt }
        )
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
}