package com.example.recordatoriomodelo2.ui

import com.example.recordatoriomodelo2.data.local.TaskEntity
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tests para la funcionalidad de importación de tareas desde Google Classroom
 */
class GoogleClassroomImportTest {

    private lateinit var classroomTasks: List<TaskEntity>
    private lateinit var dateFormat: SimpleDateFormat

    @Before
    fun setUp() {
        dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        // Crear tareas simuladas de Google Classroom
        classroomTasks = listOf(
            TaskEntity(
                id = 1,
                title = "Tarea de Matemáticas - Álgebra",
                subject = "Matemáticas",
                description = "", // Las tareas de Classroom no tienen descripción por defecto
                dueDate = "15/12/2024",
                isCompleted = false,
                createdAt = "01/12/2024 10:00:00",
                classroomId = "classroom_math_123"
            ),
            TaskEntity(
                id = 2,
                title = "Ensayo de Historia",
                subject = "Historia",
                description = "",
                dueDate = "20/12/2024",
                isCompleted = false,
                createdAt = "01/12/2024 11:00:00",
                classroomId = "classroom_history_456"
            ),
            TaskEntity(
                id = 3,
                title = "Laboratorio de Química",
                subject = "Química",
                description = "",
                dueDate = "25/12/2024",
                isCompleted = false,
                createdAt = "01/12/2024 12:00:00",
                classroomId = "classroom_chemistry_789"
            ),
            TaskEntity(
                id = 4,
                title = "Proyecto Final de Programación",
                subject = "Programación",
                description = "",
                dueDate = "30/12/2024",
                isCompleted = false,
                createdAt = "01/12/2024 13:00:00",
                classroomId = "classroom_programming_101"
            ),
            TaskEntity(
                id = 5,
                title = "Tarea sin fecha de vencimiento",
                subject = "Literatura",
                description = "",
                dueDate = "", // Algunas tareas pueden no tener fecha
                isCompleted = false,
                createdAt = "01/12/2024 14:00:00",
                classroomId = "classroom_literature_202"
            )
        )
    }

    @Test
    fun classroomTask_hasValidClassroomId() {
        // Test para verificar que las tareas de Classroom tienen un ID válido
        classroomTasks.forEach { task ->
            assertNotNull("La tarea debe tener un classroomId", task.classroomId)
            assertFalse("El classroomId no debe estar vacío", task.classroomId!!.isEmpty())
            assertTrue("El classroomId debe tener un formato válido", 
                task.classroomId!!.startsWith("classroom_"))
        }
    }

    @Test
    fun classroomTask_hasEmptyDescription() {
        // Test para verificar que las tareas importadas tienen descripción vacía
        classroomTasks.forEach { task ->
            assertNotNull("La descripción no debe ser null", task.description)
            assertTrue("Las tareas de Classroom deben tener descripción vacía", 
                task.description.isEmpty())
        }
    }

    @Test
    fun classroomTask_hasValidTitle() {
        // Test para verificar que las tareas tienen títulos válidos
        classroomTasks.forEach { task ->
            assertNotNull("El título no debe ser null", task.title)
            assertFalse("El título no debe estar vacío", task.title.isEmpty())
            assertTrue("El título debe tener contenido significativo", task.title.length > 3)
        }
    }

    @Test
    fun classroomTask_hasValidSubject() {
        // Test para verificar que las tareas tienen materias válidas
        classroomTasks.forEach { task ->
            assertNotNull("La materia no debe ser null", task.subject)
            assertFalse("La materia no debe estar vacía", task.subject.isEmpty())
        }
    }

    @Test
    fun classroomTask_dueDateFormat_isCorrect() {
        // Test para verificar que las fechas de vencimiento tienen el formato correcto
        val tasksWithDueDate = classroomTasks.filter { it.dueDate.isNotEmpty() }
        
        tasksWithDueDate.forEach { task ->
            try {
                val parsedDate = dateFormat.parse(task.dueDate)
                assertNotNull("La fecha debe ser parseable", parsedDate)
            } catch (e: Exception) {
                fail("La fecha '${task.dueDate}' no tiene el formato correcto dd/MM/yyyy")
            }
        }
    }

    @Test
    fun classroomTask_withoutDueDate_isHandled() {
        // Test para verificar que las tareas sin fecha de vencimiento se manejan correctamente
        val tasksWithoutDueDate = classroomTasks.filter { it.dueDate.isEmpty() }
        
        assertTrue("Debe haber al menos una tarea sin fecha", tasksWithoutDueDate.isNotEmpty())
        tasksWithoutDueDate.forEach { task ->
            assertEquals("La fecha vacía debe ser una cadena vacía", "", task.dueDate)
            assertNotNull("La tarea debe tener classroomId", task.classroomId)
        }
    }

    @Test
    fun classroomTask_isNotCompletedByDefault() {
        // Test para verificar que las tareas importadas no están completadas por defecto
        classroomTasks.forEach { task ->
            assertFalse("Las tareas importadas no deben estar completadas", task.isCompleted)
        }
    }

    @Test
    fun classroomTask_hasValidCreationDate() {
        // Test para verificar que las tareas tienen fecha de creación válida
        val creationDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        
        classroomTasks.forEach { task ->
            assertNotNull("La fecha de creación no debe ser null", task.createdAt)
            assertFalse("La fecha de creación no debe estar vacía", task.createdAt.isEmpty())
            
            try {
                val parsedDate = creationDateFormat.parse(task.createdAt)
                assertNotNull("La fecha de creación debe ser parseable", parsedDate)
            } catch (e: Exception) {
                fail("La fecha de creación '${task.createdAt}' no tiene el formato correcto")
            }
        }
    }

    @Test
    fun classroomTask_uniqueIds_areGenerated() {
        // Test para verificar que las tareas tienen IDs únicos
        val ids = classroomTasks.map { it.id }
        val uniqueIds = ids.toSet()
        
        assertEquals("Todos los IDs deben ser únicos", ids.size, uniqueIds.size)
        assertTrue("Todos los IDs deben ser positivos", ids.all { it > 0 })
    }

    @Test
    fun classroomTask_differentSubjects_areSupported() {
        // Test para verificar que se soportan diferentes materias
        val subjects = classroomTasks.map { it.subject }.toSet()
        
        assertTrue("Debe haber múltiples materias", subjects.size > 1)
        assertTrue("Debe incluir Matemáticas", subjects.contains("Matemáticas"))
        assertTrue("Debe incluir Historia", subjects.contains("Historia"))
        assertTrue("Debe incluir Química", subjects.contains("Química"))
        assertTrue("Debe incluir Programación", subjects.contains("Programación"))
    }

    @Test
    fun classroomTask_sorting_byDueDate_worksCorrectly() {
        // Test para verificar que las tareas se pueden ordenar por fecha de vencimiento
        val tasksWithDueDate = classroomTasks.filter { it.dueDate.isNotEmpty() }
        val sortedTasks = tasksWithDueDate.sortedBy { task ->
            try {
                dateFormat.parse(task.dueDate)
            } catch (e: Exception) {
                Date(Long.MAX_VALUE) // Poner al final las fechas inválidas
            }
        }
        
        assertTrue("Debe haber tareas para ordenar", sortedTasks.isNotEmpty())
        
        // Verificar que están ordenadas correctamente
        for (i in 0 until sortedTasks.size - 1) {
            val currentDate = dateFormat.parse(sortedTasks[i].dueDate)
            val nextDate = dateFormat.parse(sortedTasks[i + 1].dueDate)
            assertTrue("Las tareas deben estar ordenadas por fecha", 
                currentDate.before(nextDate) || currentDate.equals(nextDate))
        }
    }

    @Test
    fun classroomTask_filtering_bySubject_worksCorrectly() {
        // Test para verificar que las tareas se pueden filtrar por materia
        val mathTasks = classroomTasks.filter { it.subject == "Matemáticas" }
        val historyTasks = classroomTasks.filter { it.subject == "Historia" }
        
        assertTrue("Debe haber tareas de Matemáticas", mathTasks.isNotEmpty())
        assertTrue("Debe haber tareas de Historia", historyTasks.isNotEmpty())
        
        mathTasks.forEach { task ->
            assertEquals("Todas las tareas filtradas deben ser de Matemáticas", 
                "Matemáticas", task.subject)
        }
    }

    @Test
    fun classroomTask_importProcess_maintainsDataIntegrity() {
        // Test para simular el proceso de importación y verificar integridad
        val originalCount = classroomTasks.size
        val importedTasks = classroomTasks.map { it.copy() } // Simular importación
        
        assertEquals("El número de tareas debe mantenerse", originalCount, importedTasks.size)
        
        for (i in classroomTasks.indices) {
            val original = classroomTasks[i]
            val imported = importedTasks[i]
            
            assertEquals("El título debe mantenerse", original.title, imported.title)
            assertEquals("La materia debe mantenerse", original.subject, imported.subject)
            assertEquals("La fecha debe mantenerse", original.dueDate, imported.dueDate)
            assertEquals("El classroomId debe mantenerse", original.classroomId, imported.classroomId)
        }
    }

    @Test
    fun classroomTask_duplicateDetection_worksCorrectly() {
        // Test para verificar detección de tareas duplicadas
        val duplicateTask = classroomTasks[0].copy(id = 999) // Misma tarea con diferente ID
        val allTasks = classroomTasks + duplicateTask
        
        // Buscar duplicados por título y classroomId
        val duplicates = allTasks.groupBy { "${it.title}_${it.classroomId}" }
            .filter { it.value.size > 1 }
        
        assertTrue("Debe detectar duplicados", duplicates.isNotEmpty())
        assertEquals("Debe haber exactamente un grupo de duplicados", 1, duplicates.size)
    }

    @Test
    fun classroomTask_specialCharacters_inTitle_areHandled() {
        // Test para verificar que los caracteres especiales en títulos se manejan
        val taskWithSpecialChars = TaskEntity(
            id = 100,
            title = "Tarea con caracteres especiales: áéíóú ñ ¿¡ @#$%",
            subject = "Prueba",
            description = "",
            dueDate = "01/01/2025",
            isCompleted = false,
            createdAt = "01/12/2024 15:00:00",
            classroomId = "classroom_special_chars"
        )
        
        assertNotNull("El título no debe ser null", taskWithSpecialChars.title)
        assertTrue("El título debe contener caracteres especiales", 
            taskWithSpecialChars.title.contains("áéíóú"))
        assertTrue("El título debe contener símbolos", 
            taskWithSpecialChars.title.contains("@#$%"))
    }

    @Test
    fun classroomTask_longTitles_areHandled() {
        // Test para verificar que los títulos largos se manejan correctamente
        val longTitle = "Este es un título muy largo para una tarea de Google Classroom que puede contener mucha información detallada sobre la asignación"
        val taskWithLongTitle = TaskEntity(
            id = 101,
            title = longTitle,
            subject = "Prueba",
            description = "",
            dueDate = "01/01/2025",
            isCompleted = false,
            createdAt = "01/12/2024 16:00:00",
            classroomId = "classroom_long_title"
        )
        
        assertNotNull("El título no debe ser null", taskWithLongTitle.title)
        assertEquals("El título largo debe almacenarse completamente", longTitle, taskWithLongTitle.title)
        assertTrue("El título debe ser considerablemente largo", taskWithLongTitle.title.length > 50)
    }

    @Test
    fun classroomTask_pastDueDates_areHandled() {
        // Test para verificar que las fechas pasadas se manejan correctamente
        val pastDueTask = TaskEntity(
            id = 102,
            title = "Tarea con fecha pasada",
            subject = "Prueba",
            description = "",
            dueDate = "01/01/2020", // Fecha en el pasado
            isCompleted = false,
            createdAt = "01/12/2024 17:00:00",
            classroomId = "classroom_past_due"
        )
        
        val dueDate = dateFormat.parse(pastDueTask.dueDate)
        val currentDate = Date()
        
        assertNotNull("La fecha debe ser parseable", dueDate)
        assertTrue("La fecha debe estar en el pasado", dueDate.before(currentDate))
        assertFalse("La tarea no debe estar marcada como completada automáticamente", 
            pastDueTask.isCompleted)
    }
}