package com.example.recordatoriomodelo2.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.recordatoriomodelo2.data.local.AppDatabase
import com.example.recordatoriomodelo2.data.local.TaskEntity
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

class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val db = Room.databaseBuilder(
        app,
        AppDatabase::class.java,
        "tasks-db"
    )
    .fallbackToDestructiveMigration()
    .build()
    private val taskDao = db.taskDao()

    val tasks: Flow<List<TaskEntity>> = taskDao.getTasks()

    val tasksOrdered: Flow<List<TaskEntity>> = tasks.map { list ->
        list.sortedWith(compareBy<TaskEntity> { it.isCompleted }.thenByDescending { it.createdAt })
    }

    fun insertTask(title: String, subject: String, reminderAt: String?) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val now = dateFormat.format(Date())
            val task = TaskEntity(title = title, subject = subject, dueDate = "", createdAt = now, reminderAt = reminderAt)
            val id = taskDao.insertTask(task)
            if (!reminderAt.isNullOrEmpty()) {
                scheduleTaskReminder(getApplication(), title, subject, reminderAt)
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task)
            if (!task.reminderAt.isNullOrEmpty()) {
                scheduleTaskReminder(getApplication(), task.title, task.subject, task.reminderAt!!)
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }

    fun toggleCompleted(task: TaskEntity) {
        updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    fun importarTareasClassroom(tasks: List<com.example.recordatoriomodelo2.ui.screens.login.ClassroomTask>, subject: String) {
        viewModelScope.launch {
            for (task in tasks) {
                // Evitar duplicados por classroomId
                val exists = taskDao.getTaskByClassroomId(task.id) != null
                if (!exists) {
                    val entity = TaskEntity(
                        title = task.title,
                        subject = subject,
                        dueDate = task.dueDate ?: "",
                        isCompleted = false,
                        createdAt = formatCreationTime(task.creationTime),
                        reminderAt = task.dueDate,
                        classroomId = task.id
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
            val todasLasTareas = taskDao.getTasks().first()
            for (tarea in todasLasTareas) {
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