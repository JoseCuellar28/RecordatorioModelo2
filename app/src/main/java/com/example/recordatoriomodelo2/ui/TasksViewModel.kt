package com.example.recordatoriomodelo2.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.recordatoriomodelo2.data.local.AppDatabase
import com.example.recordatoriomodelo2.data.local.TaskEntity
import com.example.recordatoriomodelo2.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import androidx.room.Room
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // Tareas con sincronización en tiempo real
    val tasks: Flow<List<TaskEntity>> = taskRepository.getTasksForCurrentUser()

    val tasksOrdered: Flow<List<TaskEntity>> = tasks.map { list ->
        list.sortedWith(compareBy<TaskEntity> { it.isCompleted }.thenByDescending { it.createdAt })
    }

    /**
     * Estados de sincronización para mostrar indicadores visuales
     */
    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        object Success : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }

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
                val result = FirebaseManager.syncFirebaseUserToLocal(userDao)
                if (result.isSuccess) {
                    val currentUser = FirebaseManager.getCurrentUser()
                    _currentUserId.value = currentUser?.uid
                    android.util.Log.d("TasksViewModel", "Usuario sincronizado: ${currentUser?.uid}")
                } else {
                    android.util.Log.e("TasksViewModel", "Error al sincronizar usuario: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("TasksViewModel", "Error en syncCurrentUser", e)
            }
        }
    }

    fun insertTask(title: String, subject: String, reminderAt: String?) {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId == null) {
                Log.e("TasksViewModel", "No hay usuario autenticado para crear tarea")
                _syncStatus.value = SyncStatus.Error("Usuario no autenticado")
                return@launch
            }
            
            try {
                _syncStatus.value = SyncStatus.Syncing
                
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
                
                // Insertar con sincronización automática
                taskRepository.insertTask(task)
                
                if (!reminderAt.isNullOrEmpty()) {
                    scheduleTaskReminder(getApplication(), title, subject, reminderAt)
                }
                
                _syncStatus.value = SyncStatus.Success
                Log.d("TasksViewModel", "Tarea insertada y sincronizada: $title")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error al insertar tarea", e)
                _syncStatus.value = SyncStatus.Error("Error al crear tarea: ${e.message}")
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus.Syncing
                
                // Actualizar con sincronización automática
                taskRepository.updateTask(task)
                
                if (!task.reminderAt.isNullOrEmpty()) {
                    scheduleTaskReminder(getApplication(), task.title, task.subject, task.reminderAt!!)
                }
                
                _syncStatus.value = SyncStatus.Success
                Log.d("TasksViewModel", "Tarea actualizada y sincronizada: ${task.title}")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error al actualizar tarea", e)
                _syncStatus.value = SyncStatus.Error("Error al actualizar tarea: ${e.message}")
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus.Syncing
                
                // Eliminar con sincronización automática
                taskRepository.deleteTask(task)
                
                _syncStatus.value = SyncStatus.Success
                Log.d("TasksViewModel", "Tarea eliminada y sincronizada: ${task.title}")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error al eliminar tarea", e)
                _syncStatus.value = SyncStatus.Error("Error al eliminar tarea: ${e.message}")
            }
        }
    }

    fun toggleCompleted(task: TaskEntity) {
        updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    /**
     * Fuerza la sincronización manual de todas las tareas
     * La sincronización se realiza automáticamente a través del Flow de getTasksForCurrentUser()
     */
    fun forceSyncTasks() {
        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus.Syncing
                // La sincronización se maneja automáticamente en el repositorio
                // Solo necesitamos actualizar el estado
                _syncStatus.value = SyncStatus.Success
                Log.d("TasksViewModel", "Sincronización manual completada")
            } catch (e: Exception) {
                Log.e("TasksViewModel", "Error en sincronización manual", e)
                _syncStatus.value = SyncStatus.Error("Error de sincronización: ${e.message}")
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
}