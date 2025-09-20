package com.example.recordatoriomodelo2.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatoriomodelo2.ui.TasksViewModel
import com.example.recordatoriomodelo2.data.local.TaskEntity
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Calendar
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.geometry.Offset
import android.widget.Toast
import com.example.recordatoriomodelo2.ui.screens.login.PruebaAuthScreen
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock

import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

data class UserProfile(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val institution: String = ""
)

// Estado global simple para el usuario (puede migrarse a ViewModel luego)
var userProfile by mutableStateOf(UserProfile())

sealed class Screen(val route: String) {
    object SelectorAuth : Screen("selector_auth")
    object Login : Screen("login")
    object Register : Screen("register")
    object FirstLogin : Screen("first_login")
    object GoogleLogin : Screen("google_login")
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object AddTask : Screen("add_task?taskId={taskId}") {
        fun createRoute(taskId: Int?) = if (taskId != null) "add_task?taskId=$taskId" else "add_task"
    }
    object Profile : Screen("profile")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.SelectorAuth.route) { SelectorAuthScreen(navController) }
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.GoogleLogin.route) {
            val viewModel: com.example.recordatoriomodelo2.ui.screens.login.LoginViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            com.example.recordatoriomodelo2.ui.screens.login.LoginScreen(
                navController = navController,
                user = state.user,
                onUserChange = { viewModel.onEvent(com.example.recordatoriomodelo2.ui.screens.login.LoginEvent.UserChanged(it)) },
                password = state.password,
                onPasswordChange = { viewModel.onEvent(com.example.recordatoriomodelo2.ui.screens.login.LoginEvent.PasswordChanged(it)) },
                errorMessage = state.errorMessage,
                onLoginClick = { viewModel.onEvent(com.example.recordatoriomodelo2.ui.screens.login.LoginEvent.LoginClicked) },
                onGoogleSignInResult = { success, error -> viewModel.onEvent(com.example.recordatoriomodelo2.ui.screens.login.LoginEvent.GoogleSignInResult(success, error)) },
                isGoogleSignInSuccess = state.isGoogleSignInSuccess,
                googleSignInError = state.googleSignInError,
                classroomCourses = state.classroomCourses,
                showCourseDialog = state.showCourseDialog,
                onCoursesLoaded = { viewModel.onEvent(com.example.recordatoriomodelo2.ui.screens.login.LoginEvent.ClassroomCoursesLoaded(it)) },
                onSelectCourse = { viewModel.onEvent(com.example.recordatoriomodelo2.ui.screens.login.LoginEvent.SelectCourse(it)) },
                onDismissCourseDialog = { viewModel.onEvent(com.example.recordatoriomodelo2.ui.screens.login.LoginEvent.DismissCourseDialog) }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Tasks.route) {
            TasksScreen(navController)
        }
        composable(
            route = Screen.AddTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.IntType; defaultValue = -1 })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId")?.takeIf { it != -1 }
            AddTaskScreen(navController, taskId)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
        composable(Screen.Register.route) {
            com.example.recordatoriomodelo2.ui.screens.auth.RegisterScreen(navController)
        }
        composable(Screen.FirstLogin.route) {
            com.example.recordatoriomodelo2.ui.screens.auth.FirstLoginScreen(navController)
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    val viewModel: TasksViewModel = viewModel()
    val tasks by viewModel.tasksOrdered.collectAsState(initial = emptyList())
    val context = LocalContext.current
    
    // Estado para el diálogo de importación
    var showImportDialog by remember { mutableStateOf(false) }
    var classroomCourses by remember { mutableStateOf<List<com.example.recordatoriomodelo2.ui.screens.login.ClassroomCourse>>(emptyList()) }
    var showCourseDialog by remember { mutableStateOf(false) }
    var selectedCourseTasks by remember { mutableStateOf<List<com.example.recordatoriomodelo2.ui.screens.login.ClassroomTask>>(emptyList()) }
    var showTasksDialog by remember { mutableStateOf(false) }
    var selectedCourseName by remember { mutableStateOf("") }
    
    // Scopes para Classroom
    val allScopes = "oauth2:https://www.googleapis.com/auth/classroom.courses.readonly https://www.googleapis.com/auth/classroom.coursework.me.readonly https://www.googleapis.com/auth/classroom.coursework.students.readonly"
    
    // Launcher para Google Sign-In
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // Obtener cursos de Classroom
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val googleAccount = account.account
                    val scope = allScopes
                    if (googleAccount != null) {
                        try {
                            val accessToken = GoogleAuthUtil.getToken(context, googleAccount, scope)
                            val url = URL("https://classroom.googleapis.com/v1/courses")
                            val conn = url.openConnection() as HttpURLConnection
                            conn.requestMethod = "GET"
                            conn.setRequestProperty("Authorization", "Bearer $accessToken")
                            val responseCode = conn.responseCode
                            val response = conn.inputStream.bufferedReader().readText()
                            
                            val courses = mutableListOf<com.example.recordatoriomodelo2.ui.screens.login.ClassroomCourse>()
                            val json = JSONObject(response)
                            val coursesArray = json.optJSONArray("courses") ?: JSONArray()
                            for (i in 0 until coursesArray.length()) {
                                val obj = coursesArray.getJSONObject(i)
                                courses.add(
                                    com.example.recordatoriomodelo2.ui.screens.login.ClassroomCourse(
                                        id = obj.optString("id"),
                                        name = obj.optString("name"),
                                        section = obj.optString("section"),
                                        description = obj.optString("description")
                                    )
                                )
                            }
                            classroomCourses = courses
                            showCourseDialog = true
                        } catch (e: Exception) {
                            Log.e("ClassroomAPI", "Error al obtener cursos: ${e.localizedMessage}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ClassroomAPI", "Error: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Error: ${e.localizedMessage}")
        }
    }
    
    var showProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFFFFFFF)) // Fondo blanco puro
            .padding(16.dp)
    ) {
        // Header con información del usuario
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)), // Azul marino
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "¡Bienvenido!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        if (userProfile.fullName.isNotBlank()) {
                            Text(
                                text = userProfile.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                            )
                        }
                        Text(
                            text = "Gestor de Tareas Académicas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color(0xFFCBD5E1) // Gris claro para subtítulo
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate(Screen.Profile.route) }
                    ) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = "Editar perfil",
                            tint = androidx.compose.ui.graphics.Color.White // Blanco puro
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Estadísticas rápidas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard(
                        title = "Total Tareas",
                        value = tasks.size.toString(),
                        icon = Icons.Filled.List,
                        color = androidx.compose.ui.graphics.Color.White,
                        iconColor = androidx.compose.ui.graphics.Color.White,
                        bgColor = androidx.compose.ui.graphics.Color(0xFF1E293B)
                    )
                    StatCard(
                        title = "Completadas",
                        value = tasks.count { it.isCompleted }.toString(),
                        icon = Icons.Filled.CheckCircle,
                        color = androidx.compose.ui.graphics.Color.White,
                        iconColor = androidx.compose.ui.graphics.Color.White,
                        bgColor = androidx.compose.ui.graphics.Color(0xFF1E293B)
                    )
                    StatCard(
                        title = "Pendientes",
                        value = tasks.count { !it.isCompleted }.toString(),
                        icon = Icons.Filled.Info,
                        color = androidx.compose.ui.graphics.Color.White,
                        iconColor = androidx.compose.ui.graphics.Color.White,
                        bgColor = androidx.compose.ui.graphics.Color(0xFF1E293B)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        // Botones de acción principales
        Text(
            text = "Acciones",
            style = MaterialTheme.typography.titleLarge,
            color = androidx.compose.ui.graphics.Color(0xFF1E293B), // Azul marino
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Botón para ver tareas
        ActionButton(
            title = "Ver Mis Tareas",
            subtitle = "Gestionar tareas existentes",
            icon = Icons.Filled.List,
            onClick = { navController.navigate(Screen.Tasks.route) },
            modifier = Modifier.fillMaxWidth(),
            bgColor = androidx.compose.ui.graphics.Color(0xFFF3F4F6), // Gris claro
            iconColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Azul marino
            textColor = androidx.compose.ui.graphics.Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Botón para crear tarea manual
        ActionButton(
            title = "Crear Tarea Manual",
            subtitle = "Agregar nueva tarea",
            icon = Icons.Filled.Add,
            onClick = { navController.navigate(Screen.AddTask.createRoute(null)) },
            modifier = Modifier.fillMaxWidth(),
            bgColor = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
            iconColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
            textColor = androidx.compose.ui.graphics.Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Botón para importar de Classroom
        ActionButton(
            title = "Importar de Classroom",
            subtitle = "Sincronizar tareas de Google Classroom",
            icon = Icons.Filled.Info,
            onClick = {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                if (account == null) {
                    // Mostrar mensaje de advertencia
                    Toast.makeText(context, "Debes iniciar sesión con Google para importar tareas de Classroom", Toast.LENGTH_LONG).show()
                } else {
                    showImportDialog = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            bgColor = androidx.compose.ui.graphics.Color(0xFFEF4444), // Rojo vibrante
            iconColor = androidx.compose.ui.graphics.Color.White,
            textColor = androidx.compose.ui.graphics.Color.White
        )
        Spacer(modifier = Modifier.height(32.dp))
        // Elimino el botón rectangular de cerrar sesión
        // Agrego botón flotante circular en la esquina inferior izquierda
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart
    ) {
        Button(
            onClick = {
                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
                val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                googleSignInClient.signOut().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                        navController.navigate("selector_auth") { launchSingleTop = true }
                    } else {
                        Toast.makeText(context, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFEF4444)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(24.dp)
                .height(36.dp)
                .widthIn(min = 120.dp, max = 160.dp)
        ) {
            Text("Cerrar sesión", color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp)
        }
    }
    
    // Diálogo de confirmación para importar
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF), // Fondo blanco puro
            titleContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Texto azul marino
            textContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Texto azul marino
            title = { Text("Importar de Google Classroom") },
            text = { Text("¿Deseas importar tareas desde Google Classroom?") },
            confirmButton = {
                Button(
                    onClick = {
                        showImportDialog = false
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(
                                Scope("https://www.googleapis.com/auth/classroom.courses.readonly"),
                                Scope("https://www.googleapis.com/auth/classroom.coursework.me.readonly"),
                                Scope("https://www.googleapis.com/auth/classroom.coursework.students.readonly")
                            )
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        launcher.launch(googleSignInClient.signInIntent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFEF4444) // Rojo vibrante
                    )
                ) {
                    Text("Importar", color = androidx.compose.ui.graphics.Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showImportDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B) // Azul marino
                    )
                ) {
                    Text("Cancelar", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        )
    }
    
    // Diálogo de selección de curso
    if (showCourseDialog && classroomCourses.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showCourseDialog = false },
            containerColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF), // Fondo blanco puro
            titleContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Texto azul marino
            textContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Texto azul marino
            title = { Text("Selecciona un curso") },
            text = {
                Column {
                    classroomCourses.forEach { course ->
                        Button(
                            onClick = {
                                selectedCourseName = course.name
                                // Obtener tareas del curso
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val account = GoogleSignIn.getLastSignedInAccount(context)
                                        val googleAccount = account?.account
                                        if (googleAccount != null) {
                                            val accessToken = GoogleAuthUtil.getToken(context, googleAccount, allScopes)
                                            val url = URL("https://classroom.googleapis.com/v1/courses/${course.id}/courseWork")
                                            val conn = url.openConnection() as HttpURLConnection
                                            conn.requestMethod = "GET"
                                            conn.setRequestProperty("Authorization", "Bearer $accessToken")
                                            val response = conn.inputStream.bufferedReader().readText()
                                            
                                            val tasks = mutableListOf<com.example.recordatoriomodelo2.ui.screens.login.ClassroomTask>()
                                            val json = JSONObject(response)
                                            val tasksArray = json.optJSONArray("courseWork") ?: JSONArray()
                                            for (i in 0 until tasksArray.length()) {
                                                val obj = tasksArray.getJSONObject(i)
                                                val dueDateObj = obj.optJSONObject("dueDate")
                                                val dueDateString = if (dueDateObj != null) {
                                                    val year = dueDateObj.optInt("year", 0)
                                                    val month = dueDateObj.optInt("month", 0)
                                                    val day = dueDateObj.optInt("day", 0)
                                                    if (year > 0 && month > 0 && day > 0) {
                                                        String.format("%02d/%02d/%04d", day, month, year)
                                                    } else {
                                                        ""
                                                    }
                                                } else {
                                                    ""
                                                }
                                                
                                                tasks.add(
                                                    com.example.recordatoriomodelo2.ui.screens.login.ClassroomTask(
                                                        id = obj.optString("id"),
                                                        title = obj.optString("title"),
                                                        description = obj.optString("description"),
                                                        dueDate = dueDateString,
                                                        creationTime = obj.optString("creationTime")
                                                    )
                                                )
                                            }
                                            selectedCourseTasks = tasks
                                            showTasksDialog = true
                                            showCourseDialog = false
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ClassroomAPI", "Error al obtener tareas: ${e.localizedMessage}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFFEF4444) // Rojo vibrante
                            )
                        ) {
                            Text(course.name, color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(
                    onClick = { showCourseDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B) // Azul marino
                    )
                ) { 
                    Text("Cancelar", color = androidx.compose.ui.graphics.Color.White) 
                }
            }
        )
    }
    
    // Diálogo de tareas del curso
    if (showTasksDialog) {
        AlertDialog(
            onDismissRequest = { showTasksDialog = false },
            containerColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF), // Fondo blanco puro
            titleContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Texto azul marino
            textContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Texto azul marino
            title = { Text("Tareas de $selectedCourseName") },
            text = {
                if (selectedCourseTasks.isEmpty()) {
                    Text("No hay tareas en este curso.")
                } else {
                    Column {
                        selectedCourseTasks.forEach { task ->
                            Text("• ${task.title}", modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            },
            confirmButton = {
                if (selectedCourseTasks.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.importarTareasClassroom(selectedCourseTasks, selectedCourseName)
                            showTasksDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFEF4444) // Rojo vibrante
                        )
                    ) { 
                        Text("Importar todas", color = androidx.compose.ui.graphics.Color.White) 
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { showTasksDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B) // Azul marino
                    )
                ) { 
                    Text("Cerrar", color = androidx.compose.ui.graphics.Color.White) 
                }
            }
        )
    }

    // Botón flotante de logout en la esquina inferior derecha
    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = {
                navController.navigate(Screen.SelectorAuth.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = androidx.compose.ui.graphics.Color(0xFFEF4444), // Rojo vibrante
            contentColor = androidx.compose.ui.graphics.Color.White
        ) {
            Icon(
                Icons.Filled.ExitToApp,
                contentDescription = "Cerrar sesión",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    iconColor: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bgColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
    iconColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF1E293B),
    textColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF1E293B)
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = iconColor.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SelectorAuthScreen(navController: NavHostController) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val email = account.email ?: ""
            // Eliminada la validación de dominio para permitir cualquier tester
            android.widget.Toast.makeText(context, "Login exitoso: $email", android.widget.Toast.LENGTH_SHORT).show()
            navController.navigate("home") { launchSingleTop = true }
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Error en login: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo o título de la app
            Text(
                text = "Gestor de Tareas",
                style = MaterialTheme.typography.headlineLarge,
                color = androidx.compose.ui.graphics.Color(0xFF6366F1), // Indigo vibrante
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Sincroniza con Google Classroom",
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color(0xFF6B7280), // Gris moderno
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            // Botón para Google
            Button(
                onClick = {
                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(
                            com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/classroom.courses.readonly"),
                            com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/classroom.coursework.me.readonly"),
                            com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/classroom.coursework.students.readonly")
                        )
                        .build()
                    val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                    googleSignInClient.signOut().addOnCompleteListener {
                        launcher.launch(googleSignInClient.signInIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF10B981), // Verde vibrante
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar sesión con Google")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Botón para login tradicional
            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.Login.route)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = androidx.compose.ui.graphics.Color(0xFF059669), // Verde
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    androidx.compose.ui.graphics.Color(0xFF059669)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Email,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar sesión con Email")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Botón para uso sin Google
            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SelectorAuth.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = androidx.compose.ui.graphics.Color(0xFF6366F1), // Indigo vibrante
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    androidx.compose.ui.graphics.Color(0xFF6366F1)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continuar sin Google")
                }
            }
        }
    }
}

@Composable
fun LoginScreen(navController: NavHostController) {
    val authViewModel: com.example.recordatoriomodelo2.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val uiState by authViewModel.uiState.collectAsState()
    
    // Navegar según el estado del login
    LaunchedEffect(uiState.isLoggedIn, uiState.isFirstLogin) {
        if (uiState.isLoggedIn) {
            if (uiState.isFirstLogin) {
                navController.navigate(Screen.FirstLogin.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Título
            Text(
                text = "Iniciar Sesión",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { 
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Email,
                        contentDescription = null
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                leadingIcon = { 
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) 
                                Icons.Default.Lock 
                            else 
                                Icons.Default.Lock,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) 
                    androidx.compose.ui.text.input.VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    authViewModel.clearMessages()
                    authViewModel.signIn(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading && email.isNotEmpty() && password.isNotEmpty()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Iniciar Sesión", fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botón para ir al registro
            TextButton(
                onClick = { 
                    navController.navigate(Screen.Register.route)
                }
            ) {
                Text("¿No tienes cuenta? Regístrate")
            }
            
            // Botón para restablecer contraseña
            TextButton(
                onClick = {
                    if (email.isNotEmpty()) {
                        authViewModel.sendPasswordResetEmail(email)
                    }
                }
            ) {
                Text("¿Olvidaste tu contraseña?")
            }
            
            // Mostrar mensajes de error o éxito
            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            uiState.successMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
fun TasksScreen(navController: NavHostController) {
    val viewModel: TasksViewModel = viewModel()
    val tasks by viewModel.tasksOrdered.collectAsState(initial = emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header con título y botones
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mis Tareas",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = androidx.compose.ui.graphics.Color(0xFFEF4444) // Rojo vibrante
            )
            Row {
                // Botón para limpiar fechas (solo si hay tareas)
                if (tasks.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.limpiarFechasExistentes() }
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Limpiar fechas",
                            tint = androidx.compose.ui.graphics.Color(0xFF1E293B) // Azul marino
                        )
                    }
                }
                // Botón de cerrar sesión
                IconButton(
                    onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Tasks.route) { inclusive = true }
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = "Cerrar sesión",
                        tint = androidx.compose.ui.graphics.Color(0xFF1E293B) // Azul marino
                    )
                }
            }
        }
        
        // Contenido principal
        if (tasks.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay tareas",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toca el botón + para crear tu primera tarea",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Lista de tareas
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(tasks) { task ->
                    TaskCard(
                        task = task,
                        onTaskClick = { navController.navigate(Screen.AddTask.createRoute(task.id)) },
                        onCheckboxClick = { viewModel.toggleCompleted(task) },
                        onDeleteClick = { viewModel.deleteTask(task) }
                    )
                }
            }
        }
        
        // Floating Action Button
        FloatingActionButton(
            onClick = { navController.navigate(Screen.AddTask.createRoute(null)) },
            modifier = Modifier
                .align(Alignment.End)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Añadir tarea",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    onTaskClick: () -> Unit,
    onCheckboxClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // Paleta proporcionada
    val colorMorado = androidx.compose.ui.graphics.Color(0xFF511F73)
    val colorVerdeAgua = androidx.compose.ui.graphics.Color(0xFF26A699)
    val colorAmarillo = androidx.compose.ui.graphics.Color(0xFFF2BB32)
    val colorNaranja = androidx.compose.ui.graphics.Color(0xFFF29F32)
    val colorRojo = androidx.compose.ui.graphics.Color(0xFFF25C4D)
    val coloresCiclicos = listOf(colorMorado, colorVerdeAgua, colorAmarillo, colorNaranja, colorRojo)

    // Asignar color cíclicamente según el id de la tarea
    val colorFondo = coloresCiclicos[task.id % coloresCiclicos.size]

    // Texto blanco en morado, verde agua y rojo; negro en amarillo y naranja
    val textoClaro = androidx.compose.ui.graphics.Color.White
    val textoOscuro = androidx.compose.ui.graphics.Color.Black
    val colorTexto = when (colorFondo) {
        colorMorado, colorVerdeAgua, colorRojo -> textoClaro
        else -> textoOscuro
    }
    val colorIcono = colorTexto
    val colorDelete = androidx.compose.ui.graphics.Color(0xFFEF4444)
    val colorCheck = androidx.compose.ui.graphics.Color(0xFF1E293B)

    val rojoVibrante = androidx.compose.ui.graphics.Color(0xFFEF4444)
    val azulMarino = androidx.compose.ui.graphics.Color(0xFF1E293B)

    // Título de la tarea: rojo vibrante y negrita
    val estiloTitulo = MaterialTheme.typography.titleMedium.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        color = androidx.compose.ui.graphics.Color.White
    )

    // Texto de vencimiento: blanco con sombra si fondo es rojo, rojo vibrante si fondo es claro
    val esFondoRojo = colorFondo == colorRojo
    val colorVencimiento = when {
        esFondoRojo -> androidx.compose.ui.graphics.Color.White
        else -> rojoVibrante
    }
    val estiloVencimiento = MaterialTheme.typography.bodySmall.copy(
        color = colorVencimiento
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorFondo
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onCheckboxClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = azulMarino,
                    uncheckedColor = azulMarino,
                    checkmarkColor = azulMarino
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = task.title,
                    style = estiloTitulo
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = colorIcono,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = colorTexto.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = azulMarino
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Creada: ${task.createdAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorTexto.copy(alpha = 0.7f)
                        )
                    }
                    if (!task.dueDate.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = azulMarino
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            if (esFondoRojo) {
                                Box {
                                    Text(
                                        text = "Vence: ${task.dueDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
                                        modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                                    )
                                    Text(
                                        text = "Vence: ${task.dueDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.ui.graphics.Color.White
                                    )
                                }
                            } else {
                                Text(
                                    text = "Vence: ${task.dueDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = rojoVibrante
                                )
                            }
                        }
                    }
                    if (!task.reminderAt.isNullOrEmpty() && task.reminderAt != task.dueDate) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = azulMarino
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Recordatorio: ${task.reminderAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorIcono
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = { onDeleteClick() }
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar tarea",
                    tint = rojoVibrante
                )
            }
        }
    }
}

@Composable
fun getSubjectColor(subject: String): androidx.compose.ui.graphics.Color {
    return when (subject.lowercase()) {
        "matemáticas", "matematicas", "math" -> androidx.compose.ui.graphics.Color(0xFFE91E63) // Rosa
        "ciencias", "science" -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Verde
        "historia" -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Naranja
        "literatura", "español", "espanol", "lengua" -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Azul
        "física", "fisica", "physics" -> androidx.compose.ui.graphics.Color(0xFF9C27B0) // Púrpura
        "química", "quimica", "chemistry" -> androidx.compose.ui.graphics.Color(0xFF00BCD4) // Cyan
        "inglés", "ingles", "english" -> androidx.compose.ui.graphics.Color(0xFFFF5722) // Rojo-Naranja
        "geografía", "geografia", "geography" -> androidx.compose.ui.graphics.Color(0xFF795548) // Marrón
        "arte", "art" -> androidx.compose.ui.graphics.Color(0xFF607D8B) // Azul Gris
        "música", "musica", "music" -> androidx.compose.ui.graphics.Color(0xFF8BC34A) // Verde Claro
        else -> MaterialTheme.colorScheme.primary // Color por defecto
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(navController: NavHostController, taskId: Int?) {
    val viewModel: TasksViewModel = viewModel()
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val editingTask = tasks.find { it.id == taskId }
    var title by remember { mutableStateOf(editingTask?.title ?: "") }
    var subject by remember { mutableStateOf(editingTask?.subject ?: "") }
    var error by remember { mutableStateOf("") }
    var reminderAt by remember { mutableStateOf(editingTask?.reminderAt ?: "") }
    val scope = rememberCoroutineScope()
    val isEditing = editingTask != null
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempDate by remember { mutableStateOf("") }
    val context = LocalContext.current
    var timePickerDialog: TimePickerDialog? by remember { mutableStateOf(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFFFFFFF)), // Fondo blanco puro
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                if (isEditing) "Editar tarea" else "Nueva tarea",
                style = MaterialTheme.typography.headlineSmall,
                color = androidx.compose.ui.graphics.Color(0xFF1E293B), // Azul marino
                modifier = Modifier.padding(bottom = 24.dp)
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título", color = androidx.compose.ui.graphics.Color(0xFF1E293B)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Materia", color = androidx.compose.ui.graphics.Color(0xFF1E293B)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showDatePicker = true },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFEF4444), // Rojo vibrante
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(bottom = 24.dp)
                ) {
                    Text(if (reminderAt.isEmpty()) "Agregar recordatorio" else "Recordatorio: $reminderAt")
                }
            }
            if (error.isNotEmpty()) {
                Text(error, color = androidx.compose.ui.graphics.Color(0xFFEF4444), modifier = Modifier.padding(bottom = 8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFF3F4F6), // Gris claro
                        contentColor = androidx.compose.ui.graphics.Color(0xFF1E293B)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        if (title.isBlank() || subject.isBlank()) {
                            error = "Completa todos los campos"
                        } else {
                            scope.launch {
                                if (isEditing) {
                                    viewModel.updateTask(editingTask!!.copy(title = title, subject = subject, reminderAt = reminderAt))
                                } else {
                                    viewModel.insertTask(title, subject, reminderAt)
                                }
                                navController.popBackStack()
                            }
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Azul marino
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isEditing) "Actualizar" else "Guardar")
                }
            }
        }
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null) {
                                val cal = Calendar.getInstance().apply { timeInMillis = millis }
                                tempDate = String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
                                showDatePicker = false
                                showTimePicker = true
                            } else {
                                showDatePicker = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFEF4444), // Rojo vibrante
                            contentColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) { Text("OK") }
                },
                dismissButton = {
                    Button(
                        onClick = { showDatePicker = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Azul marino
                            contentColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) { Text("Cancelar") }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF), // Fondo blanco
                    titleContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B), // Azul marino
                    headlineContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                    weekdayContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                    subheadContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                    selectedDayContainerColor = androidx.compose.ui.graphics.Color(0xFFEF4444), // Rojo vibrante
                    selectedDayContentColor = androidx.compose.ui.graphics.Color.White,
                    dayContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                    todayContentColor = androidx.compose.ui.graphics.Color(0xFFEF4444)
                )
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF), // Fondo blanco puro
                        titleContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                        headlineContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                        weekdayContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                        subheadContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                        selectedDayContainerColor = androidx.compose.ui.graphics.Color(0xFFEF4444),
                        selectedDayContentColor = androidx.compose.ui.graphics.Color.White,
                        dayContentColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                        todayContentColor = androidx.compose.ui.graphics.Color(0xFFEF4444)
                    )
                )
            }
        }
        if (showTimePicker) {
            // Usar TimePickerDialog clásico de Android
            LaunchedEffect(Unit) {
                val now = Calendar.getInstance()
                timePickerDialog = TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val tempHour = String.format("%02d:%02d", hour, minute)
                        reminderAt = "$tempDate $tempHour"
                        showTimePicker = false
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog?.setOnDismissListener { showTimePicker = false }
                timePickerDialog?.show()
            }
        }
    }
} 

@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    var tempName by remember { mutableStateOf(userProfile.fullName) }
    var tempEmail by remember { mutableStateOf(userProfile.email) }
    var tempPhone by remember { mutableStateOf(userProfile.phone) }
    var tempInstitution by remember { mutableStateOf(userProfile.institution) }
    val inicial = tempName.trim().takeIf { it.isNotEmpty() }?.firstOrNull()?.uppercase() ?: "U"
    val rojoVibrante = androidx.compose.ui.graphics.Color(0xFFEF4444)
    val azulMarino = androidx.compose.ui.graphics.Color(0xFF1E293B)
    val grisClaro = androidx.compose.ui.graphics.Color(0xFFF3F4F6)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFFFFFFF))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        // Círculo con inicial rojo
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = rojoVibrante,
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = inicial.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = androidx.compose.ui.graphics.Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Perfil de usuario", style = MaterialTheme.typography.headlineMedium, color = azulMarino)
        Spacer(modifier = Modifier.height(24.dp))
        ModernTextField(
            value = tempName,
            onValueChange = { tempName = it },
            label = "Nombre completo"
        )
        Spacer(modifier = Modifier.height(16.dp))
        ModernTextField(
            value = tempEmail,
            onValueChange = { tempEmail = it },
            label = "Correo electrónico"
        )
        Spacer(modifier = Modifier.height(16.dp))
        ModernTextField(
            value = tempPhone,
            onValueChange = { tempPhone = it },
            label = "Número de teléfono"
        )
        Spacer(modifier = Modifier.height(16.dp))
        ModernTextField(
            value = tempInstitution,
            onValueChange = { tempInstitution = it },
            label = "Centro de estudio"
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    userProfile = userProfile.copy(
                        fullName = tempName,
                        email = tempEmail,
                        phone = tempPhone,
                        institution = tempInstitution
                    )
                    navController.popBackStack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = rojoVibrante),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            ) {
                Text("Guardar", color = androidx.compose.ui.graphics.Color.White)
            }
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = azulMarino),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            ) {
                Text("Cancelar", color = androidx.compose.ui.graphics.Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botón para cambiar cuenta de Google
        Button(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                
                googleSignInClient.signOut().addOnCompleteListener {
                    // Limpiar el perfil del usuario
                    userProfile = UserProfile()
                    
                    // Mostrar mensaje de confirmación
                    Toast.makeText(context, "Sesión cerrada. Selecciona una nueva cuenta.", Toast.LENGTH_SHORT).show()
                    
                    // Navegar a la pantalla de login de Google
                    navController.navigate("google_login") {
                        popUpTo("selector_auth") { inclusive = false }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color(0xFF4285F4)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ) {
            Text(
                "Cambiar cuenta de Google",
                color = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}

@Composable
fun ModernTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    )
}
