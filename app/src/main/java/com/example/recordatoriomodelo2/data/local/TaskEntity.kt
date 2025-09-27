package com.example.recordatoriomodelo2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = arrayOf("firebaseUid"),
        childColumns = arrayOf("userId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val dueDate: String,
    val isCompleted: Boolean = false,
    val createdAt: String = "",
    val reminderAt: String? = null,
    val classroomId: String? = null,
    val userId: String? = null // Firebase UID del usuario propietario
)