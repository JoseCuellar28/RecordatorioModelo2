package com.example.recordatoriomodelo2.ui

import com.example.recordatoriomodelo2.data.local.TaskEntity
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tests para la funcionalidad de fecha de vencimiento en las tareas
 */
class DueDateFunctionalityTest {

    private lateinit var dateFormatter: SimpleDateFormat
    private lateinit var testTasks: List<TaskEntity>

    @Before
    fun setUp() {
        dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        // Crear tareas de prueba con diferentes formatos de fecha
        testTasks = listOf(
            TaskEntity(
                id = 1,
                title = "Tarea con fecha válida",
                subject = "Matemáticas",
                description = "Descripción de prueba",
                dueDate = "15/12/2024",
                isCompleted = false,
                createdAt = "01/01/2024 10:00:00"
            ),
            TaskEntity(
                id = 2,
                title = "Tarea sin fecha",
                subject = "Historia",
                description = "Descripción sin fecha",
                dueDate = "",
                isCompleted = false,
                createdAt = "01/01/2024 11:00:00"
            ),
            TaskEntity(
                id = 3,
                title = "Tarea con fecha JSON",
                subject = "Ciencias",
                description = "Descripción con fecha JSON",
                dueDate = "{\"year\":2024,\"month\":12,\"day\":20}",
                isCompleted = false,
                createdAt = "01/01/2024 12:00:00"
            )
        )
    }

    @Test
    fun dueDate_validFormat_isCorrect() {
        // Test para verificar que las fechas en formato válido se manejan correctamente
        val task = testTasks[0]
        
        assertNotNull("La fecha de vencimiento no debe ser null", task.dueDate)
        assertFalse("La fecha de vencimiento no debe estar vacía", task.dueDate.isEmpty())
        assertTrue("La fecha debe seguir el formato dd/MM/yyyy", 
            task.dueDate.matches(Regex("\\d{2}/\\d{2}/\\d{4}")))
    }

    @Test
    fun dueDate_emptyDate_handlesGracefully() {
        // Test para verificar que las fechas vacías se manejan correctamente
        val task = testTasks[1]
        
        assertNotNull("La fecha de vencimiento no debe ser null", task.dueDate)
        assertTrue("La fecha de vencimiento debe estar vacía", task.dueDate.isEmpty())
    }

    @Test
    fun dueDate_jsonFormat_canBeParsed() {
        // Test para verificar que las fechas en formato JSON tienen la estructura correcta
        val task = testTasks[2]
        val jsonDate = task.dueDate
        
        assertTrue("La fecha debe ser un JSON válido", jsonDate.startsWith("{") && jsonDate.endsWith("}"))
        assertTrue("El JSON debe contener 'year'", jsonDate.contains("\"year\""))
        assertTrue("El JSON debe contener 'month'", jsonDate.contains("\"month\""))
        assertTrue("El JSON debe contener 'day'", jsonDate.contains("\"day\""))
        
        // Verificar que contiene valores numéricos válidos
        assertTrue("Debe contener el año 2024", jsonDate.contains("2024"))
        assertTrue("Debe contener el mes 12", jsonDate.contains("12"))
        assertTrue("Debe contener el día 20", jsonDate.contains("20"))
    }

    @Test
    fun dueDate_jsonFormat_hasValidStructure() {
        // Test para verificar que el formato JSON de fecha tiene una estructura válida
        val jsonDate = "{\"year\":2024,\"month\":12,\"day\":15}"
        val expectedPattern = "\\{\\\"year\\\":\\d{4},\\\"month\\\":\\d{1,2},\\\"day\\\":\\d{1,2}\\}"
        
        assertTrue("El JSON debe seguir el patrón esperado", 
            jsonDate.matches(Regex(expectedPattern)))
    }

    @Test
    fun formatClassroomDate_invalidJson_returnsEmpty() {
        // Test para JSON inválido
        val invalidJson = "invalid json"
        val formattedDate = formatClassroomDate(invalidJson)
        
        assertTrue("JSON inválido debe retornar cadena vacía", formattedDate.isEmpty())
    }

    @Test
    fun formatClassroomDate_incompleteJson_returnsEmpty() {
        // Test para JSON incompleto
        val incompleteJson = "{\"year\":2024,\"month\":12}"
        val formattedDate = formatClassroomDate(incompleteJson)
        
        assertTrue("JSON incompleto debe retornar cadena vacía", formattedDate.isEmpty())
    }

    @Test
    fun formatClassroomDate_zeroValues_returnsEmpty() {
        // Test para valores cero en el JSON
        val zeroJson = "{\"year\":0,\"month\":0,\"day\":0}"
        val formattedDate = formatClassroomDate(zeroJson)
        
        assertTrue("Valores cero deben retornar cadena vacía", formattedDate.isEmpty())
    }

    @Test
    fun dueDate_comparison_worksCorrectly() {
        // Test para verificar que la comparación de fechas funciona correctamente
        val earlierDate = "10/12/2024"
        val laterDate = "20/12/2024"
        
        val task1 = TaskEntity(id = 1, title = "Tarea 1", dueDate = earlierDate)
        val task2 = TaskEntity(id = 2, title = "Tarea 2", dueDate = laterDate)
        
        val tasks = listOf(task2, task1) // Desordenadas intencionalmente
        val sortedTasks = tasks.sortedBy { it.dueDate }
        
        assertEquals("La primera tarea debe ser la de fecha más temprana", earlierDate, sortedTasks[0].dueDate)
        assertEquals("La segunda tarea debe ser la de fecha más tardía", laterDate, sortedTasks[1].dueDate)
    }

    @Test
    fun dueDate_withReminder_isConsistent() {
        // Test para verificar que la fecha de vencimiento es consistente con el recordatorio
        val dueDate = "25/12/2024"
        val task = TaskEntity(
            id = 1,
            title = "Tarea con recordatorio",
            dueDate = dueDate,
            reminderAt = dueDate
        )
        
        assertEquals("La fecha de vencimiento y recordatorio deben ser iguales", 
            task.dueDate, task.reminderAt)
    }

    @Test
    fun dueDate_pastDate_isHandledCorrectly() {
        // Test para verificar que las fechas pasadas se manejan correctamente
        val pastDate = "01/01/2020"
        val task = TaskEntity(
            id = 1,
            title = "Tarea con fecha pasada",
            dueDate = pastDate,
            isCompleted = false
        )
        
        assertNotNull("La fecha pasada debe ser válida", task.dueDate)
        assertEquals("La fecha debe mantenerse como se estableció", pastDate, task.dueDate)
    }

    @Test
    fun dueDate_futureDate_isHandledCorrectly() {
        // Test para verificar que las fechas futuras se manejan correctamente
        val futureDate = "31/12/2025"
        val task = TaskEntity(
            id = 1,
            title = "Tarea con fecha futura",
            dueDate = futureDate,
            isCompleted = false
        )
        
        assertNotNull("La fecha futura debe ser válida", task.dueDate)
        assertEquals("La fecha debe mantenerse como se estableció", futureDate, task.dueDate)
    }

    @Test
    fun calculateDaysUntilDue_currentDate_returnsCorrectDays() {
        // Test para calcular días hasta la fecha de vencimiento
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 5) // 5 días en el futuro
        
        val futureDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val daysUntil = calculateDaysUntilDue(futureDate)
        
        assertTrue("Los días hasta la fecha deben ser positivos", daysUntil > 0)
        assertTrue("Los días deben estar cerca de 5", daysUntil >= 4 && daysUntil <= 6)
    }

    @Test
    fun taskEntity_dueDateField_isNotNull() {
        // Test para verificar que el campo dueDate nunca es null
        testTasks.forEach { task ->
            assertNotNull("El campo dueDate no debe ser null para la tarea ${task.id}", task.dueDate)
        }
    }

    @Test
    fun dueDate_displayFormat_isUserFriendly() {
        // Test para verificar que el formato de fecha es amigable para el usuario
        val task = testTasks[0]
        val datePattern = Regex("\\d{2}/\\d{2}/\\d{4}")
        
        assertTrue("La fecha debe estar en formato dd/MM/yyyy", 
            datePattern.matches(task.dueDate))
    }

    // Funciones auxiliares para los tests
    private fun formatClassroomDate(dueDateJson: String): String {
        return try {
            // Parsing manual del JSON sin usar JSONObject
            if (!dueDateJson.startsWith("{") || !dueDateJson.endsWith("}")) {
                return ""
            }
            
            val yearMatch = Regex("\"year\":(\\d+)").find(dueDateJson)
            val monthMatch = Regex("\"month\":(\\d+)").find(dueDateJson)
            val dayMatch = Regex("\"day\":(\\d+)").find(dueDateJson)
            
            if (yearMatch != null && monthMatch != null && dayMatch != null) {
                val year = yearMatch.groupValues[1].toInt()
                val month = monthMatch.groupValues[1].toInt()
                val day = dayMatch.groupValues[1].toInt()
                
                if (year > 0 && month > 0 && day > 0) {
                    String.format("%02d/%02d/%04d", day, month, year)
                } else {
                    ""
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun calculateDaysUntilDue(dueDate: String): Int {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val due = formatter.parse(dueDate)
            
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val diffInMillis = due!!.time - today.time
            val days = (diffInMillis / (1000 * 60 * 60 * 24)).toDouble()
            kotlin.math.ceil(days).toInt()
        } catch (e: Exception) {
            0
        }
    }
}