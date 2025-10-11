package com.example.recordatoriomodelo2

import com.example.recordatoriomodelo2.data.local.TaskEntity
import org.junit.Test
import org.junit.Assert.*

/**
 * Simple unit tests for basic functionality
 */
class SimpleUnitTest {
    
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    
    @Test
    fun taskEntity_creation_isCorrect() {
        val task = TaskEntity(
            id = 1,
            title = "Test Task",
            subject = "Math",
            dueDate = "2024-01-15",
            isCompleted = false,
            createdAt = "2024-01-01",
            reminderAt = null,
            classroomId = null,
            userId = "user123"
        )
        
        assertEquals(1, task.id)
        assertEquals("Test Task", task.title)
        assertEquals("Math", task.subject)
        assertEquals("2024-01-15", task.dueDate)
        assertEquals(false, task.isCompleted)
        assertEquals("2024-01-01", task.createdAt)
        assertEquals(null, task.reminderAt)
        assertEquals(null, task.classroomId)
        assertEquals("user123", task.userId)
    }
    
    @Test
    fun taskEntity_defaultValues_areCorrect() {
        val task = TaskEntity()
        
        assertEquals(0, task.id)
        assertEquals("", task.title)
        assertEquals("", task.subject)
        assertEquals("", task.dueDate)
        assertEquals(false, task.isCompleted)
        assertEquals("", task.createdAt)
        assertEquals(null, task.reminderAt)
        assertEquals(null, task.classroomId)
        assertEquals(null, task.userId)
    }
    
    @Test
    fun taskEntity_completedTask_isCorrect() {
        val task = TaskEntity(
            title = "Completed Task",
            isCompleted = true
        )
        
        assertEquals("Completed Task", task.title)
        assertEquals(true, task.isCompleted)
    }
    
    @Test
    fun string_isEmpty_isCorrect() {
        val emptyString = ""
        val nonEmptyString = "Hello"
        
        assertTrue(emptyString.isEmpty())
        assertFalse(nonEmptyString.isEmpty())
    }
    
    @Test
    fun string_length_isCorrect() {
        val testString = "Hello World"
        assertEquals(11, testString.length)
    }
}