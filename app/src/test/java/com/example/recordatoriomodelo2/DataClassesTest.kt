package com.example.recordatoriomodelo2

import com.example.recordatoriomodelo2.ui.screens.login.ClassroomCourse
import com.example.recordatoriomodelo2.ui.screens.login.ClassroomTask
import org.junit.Test
import org.junit.Assert.*

/**
 * Simple unit tests for data classes
 */
class DataClassesTest {
    
    @Test
    fun classroomCourse_creation_isCorrect() {
        val course = ClassroomCourse(
            id = "course123",
            name = "Mathematics",
            section = "A",
            description = "Advanced Mathematics Course"
        )
        
        assertEquals("course123", course.id)
        assertEquals("Mathematics", course.name)
        assertEquals("A", course.section)
        assertEquals("Advanced Mathematics Course", course.description)
    }
    
    @Test
    fun classroomCourse_withNullValues_isCorrect() {
        val course = ClassroomCourse(
            id = "course456",
            name = "Physics"
        )
        
        assertEquals("course456", course.id)
        assertEquals("Physics", course.name)
        assertEquals(null, course.section)
        assertEquals(null, course.description)
    }
    
    @Test
    fun classroomTask_creation_isCorrect() {
        val task = ClassroomTask(
            id = "task123",
            title = "Homework Assignment",
            description = "Complete exercises 1-10",
            dueDate = "2024-01-15",
            creationTime = "2024-01-01"
        )
        
        assertEquals("task123", task.id)
        assertEquals("Homework Assignment", task.title)
        assertEquals("Complete exercises 1-10", task.description)
        assertEquals("2024-01-15", task.dueDate)
        assertEquals("2024-01-01", task.creationTime)
    }
    
    @Test
    fun classroomTask_withNullValues_isCorrect() {
        val task = ClassroomTask(
            id = "task456",
            title = "Quiz"
        )
        
        assertEquals("task456", task.id)
        assertEquals("Quiz", task.title)
        assertEquals(null, task.description)
        assertEquals(null, task.dueDate)
        assertEquals(null, task.creationTime)
    }
    
    @Test
    fun classroomTask_equality_isCorrect() {
        val task1 = ClassroomTask(
            id = "task789",
            title = "Test Task"
        )
        
        val task2 = ClassroomTask(
            id = "task789",
            title = "Test Task"
        )
        
        assertEquals(task1, task2)
        assertEquals(task1.hashCode(), task2.hashCode())
    }
    
    @Test
    fun classroomCourse_equality_isCorrect() {
        val course1 = ClassroomCourse(
            id = "course789",
            name = "Chemistry"
        )
        
        val course2 = ClassroomCourse(
            id = "course789",
            name = "Chemistry"
        )
        
        assertEquals(course1, course2)
        assertEquals(course1.hashCode(), course2.hashCode())
    }
}