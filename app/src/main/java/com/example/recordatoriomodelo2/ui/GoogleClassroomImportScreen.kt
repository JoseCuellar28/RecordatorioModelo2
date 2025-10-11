package com.example.recordatoriomodelo2.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.recordatoriomodelo2.data.local.TaskEntity
import com.example.recordatoriomodelo2.firebase.FirebaseManager
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Data classes para Google Classroom
data class ClassroomCourse(
    val id: String,
    val name: String,
    val section: String? = null,
    val descriptionHeading: String? = null,
    val room: String? = null,
    val ownerId: String? = null,
    val creationTime: String? = null,
    val updateTime: String? = null,
    val enrollmentCode: String? = null,
    val courseState: String? = null,
    val alternateLink: String? = null
)

data class ClassroomTask(
    val id: String,
    val title: String,
    val description: String? = null,
    val materials: List<String>? = null,
    val state: String? = null,
    val alternateLink: String? = null,
    val creationTime: String? = null,
    val updateTime: String? = null,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val maxPoints: Double? = null,
    val workType: String? = null,
    val courseId: String,
    val courseName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleClassroomImportScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: TasksViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(false) }
    var courses by remember { mutableStateOf<List<ClassroomCourse>>(emptyList()) }
    var selectedCourse by remember { mutableStateOf<ClassroomCourse?>(null) }
    var courseTasks by remember { mutableStateOf<List<ClassroomTask>>(emptyList()) }
    var selectedTasks by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCourseSelection by remember { mutableStateOf(true) }
    var isGoogleAuthenticated by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(context) != null) }
    
    // Launcher para autenticación con Google
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(Exception::class.java)
            if (account != null) {
                isGoogleAuthenticated = true
                Toast.makeText(context, "Conectado con Google exitosamente", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Error al autenticar con Google: ${e.localizedMessage}")
            Toast.makeText(context, "Error al conectar con Google: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
    
    // Función para iniciar autenticación con Google
    fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/classroom.courses.readonly"),
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/classroom.coursework.students.readonly")
            )
            .build()
        
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }
    
    // Función para cambiar cuenta de Google
    fun changeGoogleAccount() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/classroom.courses.readonly"),
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/classroom.coursework.students.readonly")
            )
            .build()
        
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            // Resetear estados
            courses = emptyList()
            courseTasks = emptyList()
            selectedTasks = emptySet()
            showCourseSelection = true
            isGoogleAuthenticated = false
            // Lanzar nueva autenticación
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }
    
    // Función para obtener cursos de Google Classroom
    fun loadGoogleClassroomCourses() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                isLoading = true
                errorMessage = null
                
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    isGoogleAuthenticated = false
                    isLoading = false
                    return@launch
                }
                
                val token = GoogleAuthUtil.getToken(
                    context,
                    account.account!!,
                    "oauth2:https://www.googleapis.com/auth/classroom.courses.readonly"
                )
                
                val url = URL("https://classroom.googleapis.com/v1/courses")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    val coursesArray = jsonResponse.optJSONArray("courses") ?: JSONArray()
                    
                    val coursesList = mutableListOf<ClassroomCourse>()
                    for (i in 0 until coursesArray.length()) {
                        val courseJson = coursesArray.getJSONObject(i)
                        coursesList.add(
                            ClassroomCourse(
                                id = courseJson.getString("id"),
                                name = courseJson.getString("name"),
                                section = courseJson.optString("section"),
                                descriptionHeading = courseJson.optString("descriptionHeading"),
                                room = courseJson.optString("room"),
                                ownerId = courseJson.optString("ownerId"),
                                creationTime = courseJson.optString("creationTime"),
                                updateTime = courseJson.optString("updateTime"),
                                enrollmentCode = courseJson.optString("enrollmentCode"),
                                courseState = courseJson.optString("courseState"),
                                alternateLink = courseJson.optString("alternateLink")
                            )
                        )
                    }
                    courses = coursesList
                } else {
                    errorMessage = "Error al obtener cursos: $responseCode"
                }
                
                isLoading = false
            } catch (e: Exception) {
                Log.e("GoogleClassroom", "Error al cargar cursos", e)
                errorMessage = "Error: ${e.message}"
                isLoading = false
            }
        }
    }
    
    // Función para obtener tareas de un curso específico
    fun loadCourseTasks(courseId: String, courseName: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                isLoading = true
                errorMessage = null
                
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    isGoogleAuthenticated = false
                    isLoading = false
                    return@launch
                }
                
                val token = GoogleAuthUtil.getToken(
                    context,
                    account.account!!,
                    "oauth2:https://www.googleapis.com/auth/classroom.coursework.students.readonly"
                )
                
                val url = URL("https://classroom.googleapis.com/v1/courses/$courseId/courseWork")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    val courseWorkArray = jsonResponse.optJSONArray("courseWork") ?: JSONArray()
                    
                    val tasksList = mutableListOf<ClassroomTask>()
                    for (i in 0 until courseWorkArray.length()) {
                        val taskJson = courseWorkArray.getJSONObject(i)
                        val dueDate = taskJson.optJSONObject("dueDate")
                        val dueTime = taskJson.optJSONObject("dueTime")
                        
                        tasksList.add(
                            ClassroomTask(
                                id = taskJson.getString("id"),
                                title = taskJson.getString("title"),
                                description = taskJson.optString("description"),
                                state = taskJson.optString("state"),
                                alternateLink = taskJson.optString("alternateLink"),
                                creationTime = taskJson.optString("creationTime"),
                                updateTime = taskJson.optString("updateTime"),
                                dueDate = if (dueDate != null) {
                                    "${dueDate.optInt("year")}-${String.format("%02d", dueDate.optInt("month"))}-${String.format("%02d", dueDate.optInt("day"))}"
                                } else null,
                                dueTime = if (dueTime != null) {
                                    "${String.format("%02d", dueTime.optInt("hours", 0))}:${String.format("%02d", dueTime.optInt("minutes", 0))}"
                                } else null,
                                maxPoints = taskJson.optDouble("maxPoints"),
                                workType = taskJson.optString("workType"),
                                courseId = courseId,
                                courseName = courseName
                            )
                        )
                    }
                    courseTasks = tasksList
                    showCourseSelection = false
                } else {
                    errorMessage = "Error al obtener tareas: $responseCode"
                }
                
                isLoading = false
            } catch (e: Exception) {
                Log.e("GoogleClassroom", "Error al cargar tareas", e)
                errorMessage = "Error: ${e.message}"
                isLoading = false
            }
        }
    }
    
    // Función para importar tareas seleccionadas
    fun importSelectedTasks() {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                val tasksToImport = courseTasks.filter { selectedTasks.contains(it.id) }
                var importedCount = 0
                
                for (task in tasksToImport) {
                    // Verificar si la tarea ya existe por classroomId
                    val existingTasks = viewModel.getAllTasks()
                    val taskExists = existingTasks.any { it.classroomId == task.id }
                    
                    if (!taskExists) {
                        val taskEntity = TaskEntity(
                            title = task.title,
                            subject = task.courseName,
                            dueDate = task.dueDate ?: "",
                            isCompleted = false,
                            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                            reminderAt = null,
                            classroomId = task.id,
                            userId = FirebaseManager.getCurrentUser()?.uid
                        )
                        
                        viewModel.insertTaskFromClassroom(
                            title = task.title,
                            subject = task.courseName,
                            dueDate = task.dueDate ?: "",
                            classroomId = task.id,
                            reminderAt = task.dueDate
                        )
                        importedCount++
                    }
                }
                
                isLoading = false
                
                // Mostrar mensaje de éxito
                Toast.makeText(
                    context,
                    "Se importaron $importedCount tareas exitosamente",
                    Toast.LENGTH_LONG
                ).show()
                
                // Navegar de vuelta a la pantalla de tareas
                navController.popBackStack()
                
            } catch (e: Exception) {
                Log.e("GoogleClassroom", "Error al importar tareas", e)
                errorMessage = "Error al importar: ${e.message}"
                isLoading = false
            }
        }
    }
    
    // Cargar cursos al iniciar la pantalla (solo si está autenticado)
    LaunchedEffect(isGoogleAuthenticated) {
        if (isGoogleAuthenticated) {
            loadGoogleClassroomCourses()
        }
    }
    
    // Manejar el botón de atrás del sistema
    BackHandler(enabled = !showCourseSelection) {
        // Si estamos en la pantalla de seleccionar tareas, volver a seleccionar cursos
        showCourseSelection = true
        selectedTasks = emptySet()
        courseTasks = emptyList()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Verificar si está autenticado con Google
        if (!isGoogleAuthenticated) {
            // Pantalla de autenticación con Google
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Conectar con Google Classroom",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = androidx.compose.ui.graphics.Color(0xFFEF4444),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Para importar tareas desde Google Classroom, necesitas conectar tu cuenta de Google.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { signInWithGoogle() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF4285F4)
                    )
                ) {
                    Text(
                        text = "Conectar con Google",
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Text(
                        text = "Cancelar",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // Pantalla principal de importación
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showCourseSelection) "Selecciona una clase" else "Seleccionar Tareas",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = androidx.compose.ui.graphics.Color(0xFF4285F4)
                )
            
            Row {
                // Botón para cambiar cuenta de Google
                IconButton(
                    onClick = { changeGoogleAccount() }
                ) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = "Cambiar cuenta",
                        tint = androidx.compose.ui.graphics.Color(0xFF4285F4)
                    )
                }
                
                // Botón de cerrar
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        Icons.Filled.ExitToApp,
                        contentDescription = "Cerrar",
                        tint = androidx.compose.ui.graphics.Color(0xFF1E293B)
                    )
                }
            }
        }
        
        // Contenido principal
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cargando...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            if (showCourseSelection) {
                                loadGoogleClassroomCourses()
                            } else {
                                selectedCourse?.let { course ->
                                    loadCourseTasks(course.id, course.name)
                                }
                            }
                        }
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        } else if (showCourseSelection) {
            // Mostrar lista de cursos
            if (courses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron cursos",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(courses) { course ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCourse = course
                                    loadCourseTasks(course.id, course.name)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!course.section.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Sección: ${course.section}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                if (!course.room.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Aula: ${course.room}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Mostrar lista de tareas del curso seleccionado
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Información del curso seleccionado
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFF3F4F6)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = selectedCourse?.name ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = androidx.compose.ui.graphics.Color(0xFF1E293B)
                        )
                        Text(
                            text = "${courseTasks.size} tareas disponibles",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color(0xFF6B7280)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lista de tareas con checkboxes
                if (courseTasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron tareas en este curso",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(courseTasks) { task ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedTasks.contains(task.id)) {
                                        androidx.compose.ui.graphics.Color(0xFFE0F2FE)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTasks = if (selectedTasks.contains(task.id)) {
                                                selectedTasks - task.id
                                            } else {
                                                selectedTasks + task.id
                                            }
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedTasks.contains(task.id),
                                        onCheckedChange = { checked ->
                                            selectedTasks = if (checked) {
                                                selectedTasks + task.id
                                            } else {
                                                selectedTasks - task.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = androidx.compose.ui.graphics.Color(0xFF0EA5E9)
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!task.description.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = task.description!!,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                maxLines = 2
                                            )
                                        }
                                        if (!task.dueDate.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Fecha límite: ${task.dueDate}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = androidx.compose.ui.graphics.Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Botón de importar
                    if (selectedTasks.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFF10B981)
                            )
                        ) {
                            Button(
                                onClick = { importSelectedTasks() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            ) {
                                Text(
                                    text = "Importar ${selectedTasks.size} tarea${if (selectedTasks.size > 1) "s" else ""}",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
        } // Cierre del bloque else
    }
}