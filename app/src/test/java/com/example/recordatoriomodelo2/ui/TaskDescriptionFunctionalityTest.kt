package com.example.recordatoriomodelo2.ui

import com.example.recordatoriomodelo2.data.local.TaskEntity
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Tests para la funcionalidad de descripción completa en las tareas
 */
class TaskDescriptionFunctionalityTest {

    private lateinit var testTasks: List<TaskEntity>

    @Before
    fun setUp() {
        // Crear tareas de prueba con diferentes tipos de descripción
        testTasks = listOf(
            TaskEntity(
                id = 1,
                title = "Tarea con descripción completa",
                subject = "Matemáticas",
                description = "Esta es una descripción completa y detallada de la tarea que incluye múltiples líneas y información específica sobre lo que se debe hacer.",
                dueDate = "15/12/2024",
                isCompleted = false,
                createdAt = "01/01/2024 10:00:00"
            ),
            TaskEntity(
                id = 2,
                title = "Tarea con descripción vacía",
                subject = "Historia",
                description = "",
                dueDate = "20/12/2024",
                isCompleted = false,
                createdAt = "01/01/2024 11:00:00"
            ),
            TaskEntity(
                id = 3,
                title = "Tarea con descripción corta",
                subject = "Ciencias",
                description = "Descripción breve",
                dueDate = "25/12/2024",
                isCompleted = false,
                createdAt = "01/01/2024 12:00:00"
            ),
            TaskEntity(
                id = 4,
                title = "Tarea con descripción muy larga",
                subject = "Literatura",
                description = "Esta es una descripción extremadamente larga que contiene múltiples párrafos y mucha información detallada. " +
                        "Incluye instrucciones específicas, referencias bibliográficas, y criterios de evaluación. " +
                        "También puede contener enlaces, fechas importantes, y otros detalles relevantes para la tarea. " +
                        "El propósito es probar cómo el sistema maneja descripciones extensas y si mantiene la integridad de los datos.",
                dueDate = "30/12/2024",
                isCompleted = false,
                createdAt = "01/01/2024 13:00:00"
            ),
            TaskEntity(
                id = 5,
                title = "Tarea de Classroom sin descripción",
                subject = "Programación",
                description = "", // Las tareas de Classroom no tienen descripción por defecto
                dueDate = "05/01/2025",
                isCompleted = false,
                createdAt = "01/01/2024 14:00:00",
                classroomId = "classroom_123"
            )
        )
    }

    @Test
    fun taskDescription_notNull_isAlwaysValid() {
        // Test para verificar que el campo description nunca es null
        testTasks.forEach { task ->
            assertNotNull("La descripción no debe ser null para la tarea ${task.id}", task.description)
        }
    }

    @Test
    fun taskDescription_emptyDescription_isHandledCorrectly() {
        // Test para verificar que las descripciones vacías se manejan correctamente
        val taskWithEmptyDescription = testTasks[1]
        
        assertNotNull("La descripción no debe ser null", taskWithEmptyDescription.description)
        assertTrue("La descripción debe estar vacía", taskWithEmptyDescription.description.isEmpty())
        assertEquals("La descripción vacía debe ser una cadena vacía", "", taskWithEmptyDescription.description)
    }

    @Test
    fun taskDescription_fullDescription_isStoredCorrectly() {
        // Test para verificar que las descripciones completas se almacenan correctamente
        val taskWithFullDescription = testTasks[0]
        
        assertNotNull("La descripción no debe ser null", taskWithFullDescription.description)
        assertFalse("La descripción no debe estar vacía", taskWithFullDescription.description.isEmpty())
        assertTrue("La descripción debe contener texto significativo", 
            taskWithFullDescription.description.length > 10)
        assertTrue("La descripción debe contener palabras clave", 
            taskWithFullDescription.description.contains("descripción") || 
            taskWithFullDescription.description.contains("tarea"))
    }

    @Test
    fun taskDescription_shortDescription_isValid() {
        // Test para verificar que las descripciones cortas son válidas
        val taskWithShortDescription = testTasks[2]
        
        assertNotNull("La descripción no debe ser null", taskWithShortDescription.description)
        assertFalse("La descripción no debe estar vacía", taskWithShortDescription.description.isEmpty())
        assertTrue("La descripción corta debe ser válida", taskWithShortDescription.description.length > 0)
        assertEquals("La descripción debe coincidir", "Descripción breve", taskWithShortDescription.description)
    }

    @Test
    fun taskDescription_longDescription_isHandledCorrectly() {
        // Test para verificar que las descripciones largas se manejan correctamente
        val taskWithLongDescription = testTasks[3]
        
        assertNotNull("La descripción no debe ser null", taskWithLongDescription.description)
        assertFalse("La descripción no debe estar vacía", taskWithLongDescription.description.isEmpty())
        assertTrue("La descripción larga debe tener más de 100 caracteres", 
            taskWithLongDescription.description.length > 100)
        assertTrue("La descripción debe contener múltiples oraciones", 
            taskWithLongDescription.description.contains("."))
    }

    @Test
    fun taskDescription_classroomTask_hasEmptyDescription() {
        // Test para verificar que las tareas de Classroom tienen descripción vacía por defecto
        val classroomTask = testTasks[4]
        
        assertNotNull("La descripción no debe ser null", classroomTask.description)
        assertTrue("Las tareas de Classroom deben tener descripción vacía", classroomTask.description.isEmpty())
        assertNotNull("La tarea debe tener classroomId", classroomTask.classroomId)
        assertFalse("El classroomId no debe estar vacío", classroomTask.classroomId!!.isEmpty())
    }

    @Test
    fun taskDescription_specialCharacters_areHandledCorrectly() {
        // Test para verificar que los caracteres especiales se manejan correctamente
        val specialDescription = "Descripción con caracteres especiales: áéíóú, ñ, ¿¡, @#$%&*()[]{}|\\:;\"'<>,.?/~`"
        val taskWithSpecialChars = TaskEntity(
            id = 6,
            title = "Tarea con caracteres especiales",
            subject = "Prueba",
            description = specialDescription,
            dueDate = "01/01/2025",
            isCompleted = false,
            createdAt = "01/01/2024 15:00:00"
        )
        
        assertNotNull("La descripción no debe ser null", taskWithSpecialChars.description)
        assertEquals("La descripción con caracteres especiales debe mantenerse intacta", 
            specialDescription, taskWithSpecialChars.description)
        assertTrue("La descripción debe contener caracteres especiales", 
            taskWithSpecialChars.description.contains("áéíóú"))
    }

    @Test
    fun taskDescription_lineBreaks_arePreserved() {
        // Test para verificar que los saltos de línea se preservan
        val descriptionWithLineBreaks = "Primera línea\nSegunda línea\nTercera línea"
        val taskWithLineBreaks = TaskEntity(
            id = 7,
            title = "Tarea con saltos de línea",
            subject = "Prueba",
            description = descriptionWithLineBreaks,
            dueDate = "01/01/2025",
            isCompleted = false,
            createdAt = "01/01/2024 16:00:00"
        )
        
        assertNotNull("La descripción no debe ser null", taskWithLineBreaks.description)
        assertTrue("La descripción debe contener saltos de línea", 
            taskWithLineBreaks.description.contains("\n"))
        assertEquals("Los saltos de línea deben preservarse", 
            descriptionWithLineBreaks, taskWithLineBreaks.description)
    }

    @Test
    fun taskDescription_whitespace_isHandledCorrectly() {
        // Test para verificar que los espacios en blanco se manejan correctamente
        val descriptionWithWhitespace = "   Descripción con espacios al inicio y final   "
        val taskWithWhitespace = TaskEntity(
            id = 8,
            title = "Tarea con espacios",
            subject = "Prueba",
            description = descriptionWithWhitespace,
            dueDate = "01/01/2025",
            isCompleted = false,
            createdAt = "01/01/2024 17:00:00"
        )
        
        assertNotNull("La descripción no debe ser null", taskWithWhitespace.description)
        assertEquals("Los espacios deben preservarse", 
            descriptionWithWhitespace, taskWithWhitespace.description)
    }

    @Test
    fun taskDescription_maxLength_isReasonable() {
        // Test para verificar que la longitud máxima de descripción es razonable
        val veryLongDescription = "a".repeat(10000) // 10,000 caracteres
        val taskWithVeryLongDescription = TaskEntity(
            id = 9,
            title = "Tarea con descripción muy larga",
            subject = "Prueba",
            description = veryLongDescription,
            dueDate = "01/01/2025",
            isCompleted = false,
            createdAt = "01/01/2024 18:00:00"
        )
        
        assertNotNull("La descripción no debe ser null", taskWithVeryLongDescription.description)
        assertEquals("La descripción muy larga debe almacenarse completamente", 
            veryLongDescription, taskWithVeryLongDescription.description)
        assertEquals("La longitud debe ser exactamente 10,000 caracteres", 
            10000, taskWithVeryLongDescription.description.length)
    }

    @Test
    fun taskDescription_comparison_worksCorrectly() {
        // Test para verificar que la comparación de descripciones funciona correctamente
        val task1 = testTasks[0]
        val task2 = testTasks[1]
        
        assertNotEquals("Las descripciones deben ser diferentes", task1.description, task2.description)
        assertTrue("Una descripción debe ser más larga que la otra", 
            task1.description.length != task2.description.length)
    }

    @Test
    fun taskDescription_updateScenario_maintainsIntegrity() {
        // Test para simular actualización de descripción
        val originalTask = testTasks[0]
        val updatedDescription = "Descripción actualizada con nueva información"
        val updatedTask = originalTask.copy(description = updatedDescription)
        
        assertNotEquals("La descripción debe haber cambiado", 
            originalTask.description, updatedTask.description)
        assertEquals("La nueva descripción debe ser correcta", 
            updatedDescription, updatedTask.description)
        assertEquals("Otros campos deben mantenerse iguales", 
            originalTask.title, updatedTask.title)
    }

    @Test
    fun taskDescription_searchability_isSupported() {
        // Test para verificar que las descripciones son buscables
        val searchTerm = "descripción"
        val tasksWithSearchTerm = testTasks.filter { 
            it.description.lowercase().contains(searchTerm.lowercase()) 
        }
        
        assertTrue("Debe haber tareas que contengan el término de búsqueda", 
            tasksWithSearchTerm.isNotEmpty())
        tasksWithSearchTerm.forEach { task ->
            assertTrue("Cada tarea encontrada debe contener el término", 
                task.description.lowercase().contains(searchTerm.lowercase()))
        }
    }
}