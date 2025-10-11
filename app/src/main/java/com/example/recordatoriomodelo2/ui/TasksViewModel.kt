package com.example.recordatoriomodelo2.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.recordatoriomodelo2.data.local.AppDatabase
import com.example.recordatoriomodelo2.data.local.TaskEntity
import com.example.recordatoriomodelo2.data.repository.TaskRepository
import com.example.recordatoriomodelo2.data.sync.SyncStatus
import com.example.recordatoriomodelo2.data.sync.SyncState
import com.example.recordatoriomodelo2.data.sync.SyncConflict
import com.example.recordatoriomodelo2.data.sync.ConflictResolution
import com.example.recordatoriomodelo2.data.sync.ConflictResolutionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import androidx.room.Room
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.recordatoriomodelo2.TaskReminderReceiver
import com.example.recordatoriomodelo2.firebase.FirebaseManager
import kotlinx.coroutines.flow.flowOf
import android.util.Log

class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val db = Room.databaseBuilder(
        app,
        AppDatabase::class.java,
        "tasks-db"
    )
    .fallbackToDestructiveMigration()
    .build()
    private val taskDao = db.taskDao()
    private val userDao = db.userDao()
    
    // Repositorio para manejo de sincronización con validaciones de seguridad
    private val taskRepository = TaskRepository(taskDao, app)

    // Estado del usuario actual
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // Estado de sincronización
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus(SyncState.IDLE))
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    // Error de sincronización
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()
    
    // Contador de tareas sincronizadas
    private val _syncedTasksCount = MutableStateFlow<Int>(0)
    val syncedTasksCount: StateFlow<Int> = _syncedTasksCount.asStateFlow()
    
    // Estado de conflictos
    private val _pendingConflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val pendingConflicts: StateFlow<List<SyncConflict>> = _pendingConflicts.asStateFlow()

    // Tareas con sincronización en tiempo real - solo se cargan cuando hay usuario autenticado
    val tasks: Flow<List<TaskEntity>> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            Log.d("TasksViewModel", "Cargando tareas para usuario: $userId")
            taskRepository.getTasksForCurrentUser()
        } else {
            Log.d("TasksViewModel", "No hay usuario autenticado, retornando lista vacía")
            flowOf(emptyList())
        }
    }

    val tasksOrdered: Flow<List<TaskEntity>> = tasks.map { list ->
        list.sortedWith(compareBy<TaskEntity> { it.isCompleted }.thenByDescending { it.createdAt })
    }

    // Estado combinado para UI
    val uiState: StateFlow<TasksUiState> = combine(
        syncStatus,
        syncError,
        syncedTasksCount,
        pendingConflicts
    ) { status, error, count, conflicts ->
        TasksUiState(
            syncStatus = status,
            lastError = error,
            syncedTasksCount = count,
            pendingConflicts = conflicts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState(
            syncStatus = SyncStatus(SyncState.IDLE),
            lastError = null,
            syncedTasksCount = 0,
            pendingConflicts = emptyList()
        )
    )

    /**
     * Estado de UI para mostrar información de sincronización
     */
    data class TasksUiState(
        val syncStatus: SyncStatus,
        val lastError: String?,
        val syncedTasksCount: Int,
        val pendingConflicts: List<SyncConflict> = emptyList()
    )

    init {
        // Sincronizar usuario al inicializar
        syncCurrentUser()
    }

    /**
     * Sincroniza el usuario actual de Firebase con la base de datos local
     */
    fun syncCurrentUser() {
        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus(SyncState.SYNCING)
                
                // Primero verificar si hay usuario autenticado
                val currentUser = FirebaseManager.getCurrentUser()
                if (currentUser == null) {
                    Log.w("TasksViewModel", "No hay usuario autenticado en Firebase")
                    _currentUserId.value = null
                    _syncStatus.value = SyncStatus(SyncState.ERROR, errorMessage = "Usuario no autenticado")
                    return@launch
                }
                
                Log.d("TasksViewModel", "Sincronizando usuario: ${currentUser.uid}")
                
                val result = FirebaseManager.syncFirebaseUserToLocal(userDao)
                if (result.isSuccess) {
                    _currentUserId.value = currentUser.uid
                    _syncStatus.value = SyncStatus(SyncState.SYNCED)
                    Log.d("TasksViewModel", "Usuario sincronizado exitosamente: ${currentUser.uid}")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido"
                    _syncStatus.value = SyncStatus(SyncState.ERROR, errorMessage = errorMsg)
                    Log.e("TasksViewModel", "Error al sincronizar usuario: $errorMsg")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus(SyncState.ERROR, errorMessage = e.message ?: "Error desconocido")
                Log.e("TasksViewModel", "Error en syncCurrentUser", e)
            }
        }
    }

    fun insertTask(title: String, subject: String, reminderAt: String?) {
        viewModelScope.launch {
            val userId = _currentUserId.value
            Log.d("TasksViewModel", "=== INICIO insertTask ===")
            Log.d("TasksViewModel", "Usuario actual: $userId")
            
            if (userId == null) {
                Log.e("TasksViewModel", "No hay usuario autenticado para crear tarea")
                return@launch
            }
            
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val now = dateFormat.format(Date())
                val task = TaskEntity(
                    title = title, 
                    subject = subject, 
                    dueDate = "", 
                    createdAt = now, 
                    reminderAt = reminderAt,
                    userId = userId
                )
                
                Log.d("TasksViewModel", "Tarea creada - userId: ${task.userId}, title: ${task.title}")
                
                // Insertar con sincronización automática usando el nuevo sistema
                val result = taskRepository.insertTask(task)
                
                if (result.isSuccess) {
                    Log.d("TasksViewModel", "Tarea insertada exitosamente en repository")
                } else {
                    Log.e("TasksViewModel", "Error al insertar en repository: ${result.exceptionOrNull()?.message}")
                }
                
                if (!reminderAt.isNullOrEmpty()) {
                    scheduleTaskReminder(getApplication(), title, subject, reminderAt)
                }
                
                Log.d("TasksViewModel", "Tarea insertada y sincronizada: $title")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error al insertar tarea", e)
            }
        }
    }

    fun insertTaskFromClassroom(title: String, subject: String, dueDate: String, classroomId: String, reminderAt: String? = null) {
        viewModelScope.launch {
            val userId = _currentUserId.value
            Log.d("TasksViewModel", "=== INICIO insertTaskFromClassroom ===")
            Log.d("TasksViewModel", "Usuario actual: $userId, classroomId: $classroomId")
            
            if (userId == null) {
                Log.e("TasksViewModel", "No hay usuario autenticado para crear tarea")
                return@launch
            }
            
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val now = dateFormat.format(Date())
                val task = TaskEntity(
                    title = title, 
                    subject = subject, 
                    dueDate = dueDate, 
                    createdAt = now, 
                    reminderAt = reminderAt,
                    classroomId = classroomId,
                    userId = userId
                )
                
                Log.d("TasksViewModel", "Tarea de Classroom creada - userId: ${task.userId}, classroomId: ${task.classroomId}, title: ${task.title}")
                
                // Insertar con sincronización automática usando el nuevo sistema
                val result = taskRepository.insertTask(task)
                
                if (result.isSuccess) {
                    Log.d("TasksViewModel", "Tarea de Classroom insertada exitosamente en repository")
                } else {
                    Log.e("TasksViewModel", "Error al insertar tarea de Classroom en repository: ${result.exceptionOrNull()?.message}")
                }
                
                if (!reminderAt.isNullOrEmpty()) {
                    scheduleTaskReminder(getApplication(), title, subject, reminderAt)
                }
                
                Log.d("TasksViewModel", "Tarea de Classroom insertada y sincronizada: $title")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error al insertar tarea de Classroom", e)
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                // Actualizar con sincronización automática usando el nuevo sistema
                taskRepository.updateTask(task)
                
                if (!task.reminderAt.isNullOrEmpty()) {
                    scheduleTaskReminder(getApplication(), task.title, task.subject, task.reminderAt!!)
                }
                
                Log.d("TasksViewModel", "Tarea actualizada y sincronizada: ${task.title}")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error al actualizar tarea", e)
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                // Eliminar con sincronización automática usando el nuevo sistema
                taskRepository.deleteTask(task)
                
                Log.d("TasksViewModel", "Tarea eliminada y sincronizada: ${task.title}")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error al eliminar tarea", e)
            }
        }
    }

    fun toggleCompleted(task: TaskEntity) {
        updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    /**
     * Fuerza la sincronización manual de todas las tareas usando el nuevo SyncManager
     */
    fun forceSyncTasks() {
        viewModelScope.launch {
            try {
                taskRepository.forceSyncNow()
                Log.d("TasksViewModel", "Sincronización manual iniciada")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error en sincronización manual", e)
            }
        }
    }

    /**
     * Reinicia la sincronización en tiempo real
     */
    fun restartSync() {
        viewModelScope.launch {
            try {
                taskRepository.restartSync()
                Log.d("TasksViewModel", "Sincronización reiniciada")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error al reiniciar sincronización", e)
            }
        }
    }

    fun importarTareasClassroom(tasks: List<com.example.recordatoriomodelo2.ui.screens.login.ClassroomTask>, subject: String) {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId == null) {
                android.util.Log.e("TasksViewModel", "No hay usuario autenticado para importar tareas")
                return@launch
            }
            
            for (task in tasks) {
                // Evitar duplicados por classroomId y usuario
                val exists = taskDao.getTaskByClassroomIdAndUser(task.id, userId) != null
                if (!exists) {
                    val entity = TaskEntity(
                        title = task.title,
                        subject = subject,
                        dueDate = task.dueDate ?: "",
                        isCompleted = false,
                        createdAt = formatCreationTime(task.creationTime),
                        reminderAt = task.dueDate,
                        classroomId = task.id,
                        userId = userId
                    )
                    taskDao.insertTask(entity)
                }
            }
        }
    }
    
    private fun formatCreationTime(creationTime: String?): String {
        if (creationTime.isNullOrEmpty()) return ""
        
        return try {
            // Intentar diferentes formatos ISO
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            var date: Date? = null
            
            // Formato 1: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
            try {
                val inputFormat1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                date = inputFormat1.parse(creationTime)
            } catch (e: Exception) {
                // Formato 2: yyyy-MM-dd'T'HH:mm:ss'Z'
                try {
                    val inputFormat2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    date = inputFormat2.parse(creationTime)
                } catch (e: Exception) {
                    // Formato 3: yyyy-MM-dd'T'HH:mm:ss
                    try {
                        val inputFormat3 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        date = inputFormat3.parse(creationTime)
                    } catch (e: Exception) {
                        // Si ninguno funciona, devolver como está
                        return creationTime
                    }
                }
            }
            
            if (date != null) {
                outputFormat.format(date)
            } else {
                creationTime
            }
        } catch (e: Exception) {
            // Si no se puede parsear, devolver como está
            creationTime
        }
    }
    
    // Función para limpiar fechas JSON existentes en la base de datos
    fun limpiarFechasExistentes() {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId == null) {
                android.util.Log.e("TasksViewModel", "No hay usuario autenticado para limpiar fechas")
                return@launch
            }
            
            val tareasDelUsuario = taskDao.getTasksByUser(userId).first()
            for (tarea in tareasDelUsuario) {
                var tareaActualizada = tarea
                
                // Limpiar fecha de vencimiento si es JSON
                if (tarea.dueDate.startsWith("{") && tarea.dueDate.contains("year")) {
                    val fechaFormateada = formatClassroomDate(tarea.dueDate)
                    tareaActualizada = tareaActualizada.copy(dueDate = fechaFormateada, reminderAt = fechaFormateada)
                }
                
                // Limpiar fecha de creación si es formato ISO
                if (tarea.createdAt.contains("T") && tarea.createdAt.contains("Z")) {
                    val fechaFormateada = formatCreationTime(tarea.createdAt)
                    tareaActualizada = tareaActualizada.copy(createdAt = fechaFormateada)
                }
                
                if (tareaActualizada != tarea) {
                    taskDao.updateTask(tareaActualizada)
                }
            }
        }
    }
    
    private fun formatClassroomDate(dueDateJson: String): String {
        return try {
            // Intentar parsear el JSON de la fecha
            val json = org.json.JSONObject(dueDateJson)
            val year = json.optInt("year", 0)
            val month = json.optInt("month", 0)
            val day = json.optInt("day", 0)
            
            if (year > 0 && month > 0 && day > 0) {
                String.format("%02d/%02d/%04d", day, month, year)
            } else {
                ""
            }
        } catch (e: Exception) {
            // Si no es JSON válido, devolver vacío
            ""
        }
    }

    private fun scheduleTaskReminder(context: Context, title: String, subject: String, reminderAt: String) {
        try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val time = dateFormat.parse(reminderAt)?.time ?: return
            android.util.Log.d("TaskReminder", "Programando recordatorio para: $reminderAt ($time)")
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("subject", subject)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (title + subject + reminderAt).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            android.util.Log.d("TaskReminder", "Recordatorio programado correctamente")
        } catch (e: Exception) {
            android.util.Log.e("TaskReminder", "Error al programar recordatorio", e)
        }
    }
    

    
    /**
     * Carga conflictos pendientes
     */
    fun loadPendingConflicts() {
        viewModelScope.launch {
            try {
                val conflicts = taskRepository.getPendingConflicts()
                _pendingConflicts.value = conflicts
                Log.d("TasksViewModel", "Conflictos cargados: ${conflicts.size}")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error cargando conflictos", e)
                _pendingConflicts.value = emptyList()
            }
        }
    }
    
    /**
     * Resuelve un conflicto manualmente
     */
    fun resolveConflict(
        conflictId: String,
        resolution: ConflictResolution,
        mergedTask: TaskEntity? = null
    ) {
        viewModelScope.launch {
            try {
                val success = taskRepository.resolveConflictManually(conflictId, resolution, mergedTask)
                if (success) {
                    // Recargar conflictos después de resolver uno
                    loadPendingConflicts()
                    Log.d("TasksViewModel", "Conflicto resuelto: $conflictId")
                } else {
                    Log.e("TasksViewModel", "Error resolviendo conflicto: $conflictId")
                }
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error resolviendo conflicto", e)
            }
        }
    }
    
    /**
     * Obtiene todas las tareas del usuario actual
     */
    suspend fun getAllTasks(): List<TaskEntity> {
        val userId = _currentUserId.value
        return if (userId != null) {
            taskDao.getTasksByUser(userId).first()
        } else {
            emptyList()
        }
    }

    /**
     * Sincroniza tareas con manejo de conflictos
     */
    fun syncWithConflictResolution() {
        viewModelScope.launch {
            try {
                val userId = _currentUserId.value
                if (userId == null) {
                    Log.e("TasksViewModel", "No hay usuario autenticado para sincronizar")
                    return@launch
                }
                
                // Obtener tareas locales
                val localTasks = taskDao.getTasksByUser(userId).first()
                
                // Obtener tareas remotas (esto debería implementarse en el repository)
                // Por ahora, usamos una lista vacía como placeholder
                val remoteTasks = emptyList<TaskEntity>()
                
                val result = taskRepository.syncTasksWithConflictResolution(localTasks, remoteTasks)
                
                if (result.conflicts.isNotEmpty()) {
                    _pendingConflicts.value = result.conflicts
                    Log.d("TasksViewModel", "Sincronización completada con ${result.conflicts.size} conflictos")
                } else {
                    _pendingConflicts.value = emptyList()
                    Log.d("TasksViewModel", "Sincronización completada sin conflictos")
                }
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error en sincronización con manejo de conflictos", e)
            }
        }
    }
}