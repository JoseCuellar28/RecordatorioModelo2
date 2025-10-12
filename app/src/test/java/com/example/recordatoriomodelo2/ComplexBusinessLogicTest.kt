package com.example.recordatoriomodelo2

import com.example.recordatoriomodelo2.data.local.TaskEntity
import com.example.recordatoriomodelo2.ui.screens.login.ClassroomTask
import com.example.recordatoriomodelo2.ui.screens.login.ClassroomCourse
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.text.SimpleDateFormat
import java.util.*

/**
 * Complex unit tests for business logic and advanced validations
 */
class ComplexBusinessLogicTest {
    
    private lateinit var taskManager: TaskManager
    private lateinit var dateFormatter: SimpleDateFormat
    
    @Before
    fun setUp() {
        taskManager = TaskManager()
        dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    
    @Test
    fun taskManager_addMultipleTasks_maintainsCorrectOrder() {
        val task1 = TaskEntity(
            id = 1,
            title = "Task A",
            subject = "Math",
            dueDate = "2024-01-15",
            isCompleted = false
        )
        
        val task2 = TaskEntity(
            id = 2,
            title = "Task B", 
            subject = "Science",
            dueDate = "2024-01-10",
            isCompleted = false
        )
        
        val task3 = TaskEntity(
            id = 3,
            title = "Task C",
            subject = "History",
            dueDate = "2024-01-20",
            isCompleted = true
        )
        
        taskManager.addTask(task1)
        taskManager.addTask(task2)
        taskManager.addTask(task3)
        
        val sortedTasks = taskManager.getTasksSortedByDueDate()
        
        assertEquals(3, sortedTasks.size)
        assertEquals("Task B", sortedTasks[0].title) // 2024-01-10
        assertEquals("Task A", sortedTasks[1].title) // 2024-01-15
        assertEquals("Task C", sortedTasks[2].title) // 2024-01-20
    }
    
    @Test
    fun taskManager_filterCompletedTasks_returnsOnlyCompleted() {
        val completedTask = TaskEntity(
            id = 1,
            title = "Completed Task",
            isCompleted = true
        )
        
        val pendingTask = TaskEntity(
            id = 2,
            title = "Pending Task",
            isCompleted = false
        )
        
        taskManager.addTask(completedTask)
        taskManager.addTask(pendingTask)
        
        val completedTasks = taskManager.getCompletedTasks()
        val pendingTasks = taskManager.getPendingTasks()
        
        assertEquals(1, completedTasks.size)
        assertEquals(1, pendingTasks.size)
        assertEquals("Completed Task", completedTasks[0].title)
        assertEquals("Pending Task", pendingTasks[0].title)
    }
    
    @Test
    fun taskValidator_validateTaskData_detectsInvalidData() {
        val validator = TaskValidator()
        
        // Test valid task with future date
        val futureDate = getDateInDays(30)
        val validTask = TaskEntity(
            title = "Valid Task",
            subject = "Math",
            dueDate = futureDate
        )
        assertTrue(validator.isValidTask(validTask))
        
        // Test invalid tasks
        val emptyTitleTask = TaskEntity(title = "", subject = "Math", dueDate = futureDate)
        assertFalse(validator.isValidTask(emptyTitleTask))
        
        val invalidDateTask = TaskEntity(title = "Task", subject = "Math", dueDate = "invalid-date")
        assertFalse(validator.isValidTask(invalidDateTask))
        
        val pastDateTask = TaskEntity(title = "Task", subject = "Math", dueDate = "2020-01-01")
        assertFalse(validator.isValidTask(pastDateTask))
    }
    
    @Test
    fun classroomTaskConverter_convertToTaskEntity_preservesAllData() {
        val converter = ClassroomTaskConverter()
        
        val classroomTask = ClassroomTask(
            id = "classroom123",
            title = "Homework Assignment",
            description = "Complete exercises 1-10",
            dueDate = "2024-06-15",
            creationTime = "2024-01-01T10:00:00Z"
        )
        
        val course = ClassroomCourse(
            id = "course456",
            name = "Advanced Mathematics",
            section = "A"
        )
        
        val taskEntity = converter.convertToTaskEntity(classroomTask, course, "user123")
        
        assertEquals("Homework Assignment", taskEntity.title)
        assertEquals("Advanced Mathematics", taskEntity.subject)
        assertEquals("2024-06-15", taskEntity.dueDate)
        assertEquals("classroom123", taskEntity.classroomId)
        assertEquals("user123", taskEntity.userId)
        assertEquals(false, taskEntity.isCompleted)
        assertNotNull(taskEntity.createdAt)
    }
    
    @Test
    fun taskStatistics_calculateTaskMetrics_returnsCorrectStats() {
        val statistics = TaskStatistics()
        
        val tasks = listOf(
            TaskEntity(title = "Task 1", subject = "Math", isCompleted = true),
            TaskEntity(title = "Task 2", subject = "Math", isCompleted = false),
            TaskEntity(title = "Task 3", subject = "Science", isCompleted = true),
            TaskEntity(title = "Task 4", subject = "Science", isCompleted = true),
            TaskEntity(title = "Task 5", subject = "History", isCompleted = false)
        )
        
        val metrics = statistics.calculateMetrics(tasks)
        
        assertEquals(5, metrics.totalTasks)
        assertEquals(3, metrics.completedTasks)
        assertEquals(2, metrics.pendingTasks)
        assertEquals(60.0, metrics.completionPercentage, 0.1)
        
        // Test subject breakdown
        assertEquals(3, metrics.subjectBreakdown.size)
        assertEquals(2, metrics.subjectBreakdown["Math"])
        assertEquals(2, metrics.subjectBreakdown["Science"])
        assertEquals(1, metrics.subjectBreakdown["History"])
    }
    
    @Test
    fun dateUtility_calculateDaysUntilDue_returnsCorrectDays() {
        val dateUtility = DateUtility()
        
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        val nextWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        
        val tomorrowStr = dateFormatter.format(tomorrow.time)
        val nextWeekStr = dateFormatter.format(nextWeek.time)
        val yesterdayStr = dateFormatter.format(yesterday.time)
        
        assertEquals(1, dateUtility.calculateDaysUntilDue(tomorrowStr))
        assertEquals(7, dateUtility.calculateDaysUntilDue(nextWeekStr))
        assertEquals(-1, dateUtility.calculateDaysUntilDue(yesterdayStr))
    }
    
    @Test
    fun taskPriorityCalculator_calculatePriority_assignsCorrectPriority() {
        val calculator = TaskPriorityCalculator()
        
        val urgentTask = TaskEntity(
            title = "Urgent Task",
            dueDate = getTomorrowDate(),
            isCompleted = false
        )
        
        val normalTask = TaskEntity(
            title = "Normal Task", 
            dueDate = getDateInDays(7),
            isCompleted = false
        )
        
        val lowPriorityTask = TaskEntity(
            title = "Low Priority Task",
            dueDate = getDateInDays(30),
            isCompleted = false
        )
        
        val completedTask = TaskEntity(
            title = "Completed Task",
            dueDate = getTomorrowDate(),
            isCompleted = true
        )
        
        assertEquals(TaskPriority.HIGH, calculator.calculatePriority(urgentTask))
        assertEquals(TaskPriority.MEDIUM, calculator.calculatePriority(normalTask))
        assertEquals(TaskPriority.LOW, calculator.calculatePriority(lowPriorityTask))
        assertEquals(TaskPriority.NONE, calculator.calculatePriority(completedTask))
    }
    
    // Helper classes and methods for testing
    
    private fun getTomorrowDate(): String {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        return dateFormatter.format(tomorrow.time)
    }
    
    private fun getDateInDays(days: Int): String {
        val futureDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, days) }
        return dateFormatter.format(futureDate.time)
    }
    
    // Mock classes for testing business logic
    
    class TaskManager {
        private val tasks = mutableListOf<TaskEntity>()
        
        fun addTask(task: TaskEntity) {
            tasks.add(task)
        }
        
        fun getTasksSortedByDueDate(): List<TaskEntity> {
            return tasks.sortedBy { it.dueDate }
        }
        
        fun getCompletedTasks(): List<TaskEntity> {
            return tasks.filter { it.isCompleted }
        }
        
        fun getPendingTasks(): List<TaskEntity> {
            return tasks.filter { !it.isCompleted }
        }
    }
    
    class TaskValidator {
        fun isValidTask(task: TaskEntity): Boolean {
            if (task.title.isBlank()) return false
            if (!isValidDateFormat(task.dueDate)) return false
            if (isPastDate(task.dueDate)) return false
            return true
        }
        
        private fun isValidDateFormat(date: String): Boolean {
            return try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                true
            } catch (e: Exception) {
                false
            }
        }
        
        private fun isPastDate(date: String): Boolean {
            return try {
                val taskDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                
                // Get today's date at midnight for accurate comparison
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                
                taskDate?.before(today) ?: false
            } catch (e: Exception) {
                false
            }
        }
    }
    
    class ClassroomTaskConverter {
        fun convertToTaskEntity(classroomTask: ClassroomTask, course: ClassroomCourse, userId: String): TaskEntity {
            return TaskEntity(
                title = classroomTask.title,
                subject = course.name,
                dueDate = classroomTask.dueDate ?: "",
                isCompleted = false,
                createdAt = formatCreationTime(classroomTask.creationTime ?: ""),
                classroomId = classroomTask.id,
                userId = userId
            )
        }
        
        private fun formatCreationTime(isoTime: String): String {
            return if (isoTime.isNotEmpty()) {
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val date = inputFormat.parse(isoTime)
                    outputFormat.format(date ?: Date())
                } catch (e: Exception) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                }
            } else {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            }
        }
    }
    
    data class TaskMetrics(
        val totalTasks: Int,
        val completedTasks: Int,
        val pendingTasks: Int,
        val completionPercentage: Double,
        val subjectBreakdown: Map<String, Int>
    )
    
    class TaskStatistics {
        fun calculateMetrics(tasks: List<TaskEntity>): TaskMetrics {
            val total = tasks.size
            val completed = tasks.count { it.isCompleted }
            val pending = total - completed
            val percentage = if (total > 0) (completed.toDouble() / total) * 100 else 0.0
            
            val subjectBreakdown = tasks.groupBy { it.subject }.mapValues { it.value.size }
            
            return TaskMetrics(total, completed, pending, percentage, subjectBreakdown)
        }
    }
    
    class DateUtility {
        fun calculateDaysUntilDue(dueDate: String): Int {
            return try {
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val due = formatter.parse(dueDate)
                
                // Get today's date at midnight for accurate comparison
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
    
    enum class TaskPriority {
        NONE, LOW, MEDIUM, HIGH
    }
    
    class TaskPriorityCalculator {
        fun calculatePriority(task: TaskEntity): TaskPriority {
            if (task.isCompleted) return TaskPriority.NONE
            
            val dateUtility = DateUtility()
            val daysUntilDue = dateUtility.calculateDaysUntilDue(task.dueDate)
            
            return when {
                daysUntilDue <= 1 -> TaskPriority.HIGH
                daysUntilDue <= 7 -> TaskPriority.MEDIUM
                else -> TaskPriority.LOW
            }
        }
    }
}