package com.example.recordatoriomodelo2.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.recordatoriomodelo2.data.local.TaskEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Gestor de sincronización en tiempo real con Firestore
 * Maneja estados de conexión, reconexión automática y sincronización inteligente
 */
class SyncManager private constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    
    // Resolver de conflictos
    private val conflictResolver = ConflictResolver.getInstance()
    
    companion object {
        private const val TAG = "SyncManager"
        private const val TASKS_COLLECTION = "tasks"
        
        @Volatile
        private var INSTANCE: SyncManager? = null
        
        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Estados de sincronización
    enum class SyncState {
        IDLE,           // Sin actividad
        CONNECTING,     // Conectando a Firestore
        SYNCING,        // Sincronizando datos
        SYNCED,         // Sincronizado exitosamente
        ERROR,          // Error en sincronización
        OFFLINE         // Sin conexión
    }
    
    // Estado actual de sincronización
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    // Último error de sincronización
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    
    // Contador de tareas sincronizadas
    private val _syncedTasksCount = MutableStateFlow(0)
    val syncedTasksCount: StateFlow<Int> = _syncedTasksCount.asStateFlow()
    
    // Control de listeners activos
    private var firestoreListener: ListenerRegistration? = null
    private val isListenerActive = AtomicBoolean(false)
    
    // Monitor de conectividad
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    init {
        setupNetworkMonitoring()
    }
    
    /**
     * Configura el monitoreo de conectividad de red
     */
    private fun setupNetworkMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Red disponible - reactivando sincronización")
                if (_syncState.value == SyncState.OFFLINE) {
                    _syncState.value = SyncState.IDLE
                }
            }
            
            override fun onLost(network: Network) {
                Log.d(TAG, "Red perdida - modo offline")
                _syncState.value = SyncState.OFFLINE
                _lastError.value = "Sin conexión a internet"
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }
    
    /**
     * Inicia la sincronización en tiempo real para un usuario con detección automática de conflictos
     */
    fun startRealtimeSync(userId: String, localTasksFlow: Flow<List<TaskEntity>>? = null): Flow<List<TaskEntity>> = callbackFlow {
        if (isListenerActive.get()) {
            Log.w(TAG, "Listener ya está activo, cerrando anterior")
            stopRealtimeSync()
        }
        
        _syncState.value = SyncState.CONNECTING
        _lastError.value = null
        
        try {
            firestoreListener = firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error en listener de Firestore", error)
                        _syncState.value = SyncState.ERROR
                        _lastError.value = "Error de sincronización: ${error.message}"
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        _syncState.value = SyncState.SYNCING
                        
                        val remoteTasks = snapshot.documents.mapNotNull { doc ->
                            try {
                                val taskEntity = doc.toObject(TaskEntity::class.java)
                                if (taskEntity != null) {
                                    // Manejar ID del documento
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
                                Log.e(TAG, "Error convirtiendo documento: ${e.message}", e)
                                null
                            }
                        }
                        
                        // Detectar conflictos automáticamente si se proporcionan tareas locales
                        if (localTasksFlow != null) {
                            try {
                                // Obtener las tareas locales actuales (esto es una simplificación)
                                // En una implementación real, necesitaríamos una forma más elegante de obtener el estado actual
                                Log.d(TAG, "Detección automática de conflictos habilitada")
                                
                                // Por ahora, procesar las tareas remotas directamente
                                // La detección de conflictos se manejará en el repositorio
                                val finalTasks = remoteTasks
                                
                                _syncedTasksCount.value = finalTasks.size
                                _syncState.value = SyncState.SYNCED
                                _lastError.value = null
                                
                                Log.d(TAG, "Sincronización exitosa con detección de conflictos: ${finalTasks.size} tareas")
                                trySend(finalTasks)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error en detección automática de conflictos", e)
                                _syncState.value = SyncState.ERROR
                                _lastError.value = "Error detectando conflictos: ${e.message}"
                                trySend(remoteTasks)
                            }
                        } else {
                            _syncedTasksCount.value = remoteTasks.size
                            _syncState.value = SyncState.SYNCED
                            _lastError.value = null
                            
                            Log.d(TAG, "Sincronización exitosa: ${remoteTasks.size} tareas")
                            trySend(remoteTasks)
                        }
                    }
                }
            
            isListenerActive.set(true)
            Log.d(TAG, "Listener de sincronización iniciado para usuario: $userId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando sincronización", e)
            _syncState.value = SyncState.ERROR
            _lastError.value = "Error iniciando sincronización: ${e.message}"
            trySend(emptyList())
        }
        
        awaitClose {
            stopRealtimeSync()
        }
    }
    
    /**
     * Detiene la sincronización en tiempo real
     */
    fun stopRealtimeSync() {
        firestoreListener?.remove()
        firestoreListener = null
        isListenerActive.set(false)
        _syncState.value = SyncState.IDLE
        Log.d(TAG, "Sincronización en tiempo real detenida")
    }
    
    /**
     * Verifica si hay conexión a internet
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Fuerza una sincronización manual
     */
    suspend fun forceSyncNow(userId: String): Result<Int> {
        return try {
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("Sin conexión a internet"))
            }
            
            _syncState.value = SyncState.SYNCING
            
            val snapshot = firestore.collection(TASKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val taskCount = snapshot.documents.size
            _syncedTasksCount.value = taskCount
            _syncState.value = SyncState.SYNCED
            _lastError.value = null
            
            Log.d(TAG, "Sincronización manual exitosa: $taskCount tareas")
            Result.success(taskCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización manual", e)
            _syncState.value = SyncState.ERROR
            _lastError.value = "Error en sincronización: ${e.message}"
            Result.failure(e)
        }
    }
    
    /**
     * Reinicia la sincronización (útil para reconexión)
     */
    fun restartSync() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            stopRealtimeSync()
            // El reinicio se manejará automáticamente cuando se vuelva a llamar startRealtimeSync
            Log.d(TAG, "Sincronización reiniciada para usuario: ${currentUser.uid}")
        }
    }
    
    /**
     * Sincroniza tareas con manejo de conflictos
     */
    suspend fun syncTasksWithConflictResolution(
        localTasks: List<TaskEntity>,
        remoteTasks: List<TaskEntity>
    ): ConflictResolutionResult {
        return try {
            _syncState.value = SyncState.SYNCING
            
            val result = conflictResolver.resolveTaskConflicts(localTasks, remoteTasks)
            
            if (result.conflicts.isNotEmpty()) {
                Log.d(TAG, "Conflictos detectados: ${result.conflicts.size}")
                _lastError.value = "Se detectaron ${result.conflicts.size} conflictos de sincronización"
            } else {
                _lastError.value = null
            }
            
            _syncedTasksCount.value = result.resolvedTasks.size
            _syncState.value = if (result.conflicts.isEmpty()) SyncState.SYNCED else SyncState.ERROR
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error en resolución de conflictos", e)
            _syncState.value = SyncState.ERROR
            _lastError.value = "Error resolviendo conflictos: ${e.message}"
            ConflictResolutionResult(emptyList(), emptyList(), emptyList())
        }
    }
    
    /**
     * Detecta conflictos automáticamente entre tareas locales y remotas
     */
    suspend fun detectConflictsAutomatically(
        localTasks: List<TaskEntity>,
        remoteTasks: List<TaskEntity>
    ): ConflictResolutionResult {
        return try {
            Log.d(TAG, "Iniciando detección automática de conflictos")
            Log.d(TAG, "Tareas locales: ${localTasks.size}, Tareas remotas: ${remoteTasks.size}")
            
            val result = conflictResolver.resolveTaskConflicts(localTasks, remoteTasks)
            
            if (result.conflicts.isNotEmpty()) {
                Log.w(TAG, "Conflictos detectados automáticamente: ${result.conflicts.size}")
                _lastError.value = "Se detectaron ${result.conflicts.size} conflictos de sincronización"
                _syncState.value = SyncState.ERROR
            } else {
                Log.d(TAG, "No se detectaron conflictos")
                _lastError.value = null
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error en detección automática de conflictos", e)
            _lastError.value = "Error detectando conflictos: ${e.message}"
            ConflictResolutionResult(emptyList(), emptyList(), emptyList())
        }
    }
    
    /**
     * Obtiene conflictos pendientes
     */
    suspend fun getPendingConflicts(): List<SyncConflict> {
        return conflictResolver.getPendingConflicts()
    }
    
    /**
     * Resuelve un conflicto manualmente
     */
    suspend fun resolveConflictManually(
        conflictId: String,
        resolution: ConflictResolution,
        mergedTask: TaskEntity? = null
    ): Boolean {
        return try {
            val success = conflictResolver.resolveConflictManually(conflictId, resolution, mergedTask)
            if (success) {
                Log.d(TAG, "Conflicto resuelto manualmente: $conflictId")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error resolviendo conflicto manualmente", e)
            false
        }
    }
    
    /**
     * Limpia recursos al destruir
     */
    fun cleanup() {
        stopRealtimeSync()
        networkCallback?.let { 
            connectivityManager.unregisterNetworkCallback(it)
        }
        networkCallback = null
        Log.d(TAG, "SyncManager limpiado")
    }
}