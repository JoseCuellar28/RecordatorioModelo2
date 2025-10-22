package com.example.recordatoriomodelo2.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueDate ASC")
    fun getTasksByUser(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE classroomId = :classroomId LIMIT 1")
    suspend fun getTaskByClassroomId(classroomId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE classroomId = :classroomId AND userId = :userId LIMIT 1")
    suspend fun getTaskByClassroomIdAndUser(classroomId: String, userId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Int): TaskEntity?
}