package com.example.recordatoriomodelo2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val dueDate: String,
    val isCompleted: Boolean = false,
    val createdAt: String = "",
    val reminderAt: String? = null,
    val classroomId: String? = null
) 