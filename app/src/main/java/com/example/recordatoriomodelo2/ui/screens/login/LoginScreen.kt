package com.example.recordatoriomodelo2.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatoriomodelo2.ui.TasksViewModel
import android.accounts.Account
import com.google.android.gms.auth.UserRecoverableAuthException
import android.widget.Toast

data class ClassroomCourse(
    val id: String,
    val name: String,
    val section: String? = null,
    val description: String? = null
)

data class ClassroomTask(
    val id: String,
    val title: String,
    val description: String? = null,
    val dueDate: String? = null,
    val creationTime: String? = null
)

private const val GOOGLE_CLIENT_ID_WEB = "939841424668-6q07p835rkmnnbg2j5ttq0k6p35q9s6h.apps.googleusercontent.com"

@Composable
fun LoginScreen(
    navController: NavHostController,
    user: String,
    onUserChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    errorMessage: String?,
    onLoginClick: () -> Unit,
    onGoogleSignInResult: (Boolean, String?) -> Unit,
    isGoogleSignInSuccess: Boolean,
    googleSignInError: String?,
    classroomCourses: List<ClassroomCourse> = emptyList(),
    showCourseDialog: Boolean = false,
    onCoursesLoaded: ((List<ClassroomCourse>) -> Unit)? = null,
    onSelectCourse: ((ClassroomCourse) -> Unit)? = null,
    onDismissCourseDialog: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // --- INICIO: Autenticación con Google ---
        // Este bloque se encarga de recibir el resultado del intent de Google Sign-In
        // Aquí se obtiene la cuenta seleccionada y el email del usuario
        Log.d("GoogleSignIn", "Entró al launcher, resultCode: ${result.resultCode}, data: ${result.data}")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d("GoogleSignIn", "Cuenta seleccionada: ${account.email}")
            // Validar dominio del correo (puedes comentar o eliminar si ya no se usa)
            val email = account.email ?: ""
            if (!email.endsWith("@autonoma.edu.pe", ignoreCase = true)) {
                Toast.makeText(context, "Solo se permiten correos institucionales @autonoma.edu.pe", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            Toast.makeText(context, "Login exitoso: $email", Toast.LENGTH_SHORT).show()
            // Navegación tras login exitoso
            navController.navigate("home") { launchSingleTop = true }
            // --- FIN: Autenticación con Google ---

            // --- INICIO: Obtención de cursos desde Google Classroom ---
            // Aquí se obtiene el access token real de Google para acceder a la API de Classroom
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val scope = "oauth2:https://www.googleapis.com/auth/classroom.courses.readonly"
                    val googleAccount = account.account
                    if (googleAccount != null) {
                        val accessToken = GoogleAuthUtil.getToken(context, googleAccount, scope)
                        if (accessToken != null) {
                            // Llamada a la API REST de Classroom para obtener los cursos del usuario
                            val url = URL("https://classroom.googleapis.com/v1/courses")
                            val conn = url.openConnection() as HttpURLConnection
                            conn.requestMethod = "GET"
                            conn.setRequestProperty("Authorization", "Bearer $accessToken")
                            val responseCode = conn.responseCode
                            val response = conn.inputStream.bufferedReader().readText()
                            Log.d("ClassroomAPI", "Response code: $responseCode\n$response")
                            // Parseo de cursos recibidos en formato JSON
                            val courses = mutableListOf<ClassroomCourse>()
                            val json = JSONObject(response)
                            val coursesArray = json.optJSONArray("courses") ?: JSONArray()
                            for (i in 0 until coursesArray.length()) {
                                val obj = coursesArray.getJSONObject(i)
                                courses.add(
                                    ClassroomCourse(
                                        id = obj.optString("id"),
                                        name = obj.optString("name"),
                                        section = obj.optString("section"),
                                        description = obj.optString("description")
                                    )
                                )
                            }
                            Log.d("ClassroomAPI", "Cursos obtenidos: $courses")
                            // Callback para mostrar los cursos en la UI
                            onCoursesLoaded?.invoke(courses)
                        } else {
                            Log.e("ClassroomAPI", "No se pudo obtener el access token")
                        }
                    } else {
                        Log.e("ClassroomAPI", "No se pudo obtener el objeto Account de GoogleSignInAccount")
                    }
                } catch (e: Exception) {
                    Log.e("ClassroomAPI", "Error al llamar a Classroom: ${e.localizedMessage}")
                }
            }
            // --- FIN: Obtención de cursos desde Google Classroom ---
            onGoogleSignInResult(true, null)
        } catch (e: Exception) {
            Log.e("GoogleSignIn", "Error al obtener cuenta: ${e.localizedMessage}", e)
            onGoogleSignInResult(false, e.localizedMessage)
        }
    }

    val tasksViewModel: TasksViewModel = viewModel()

    // Estado para mostrar tareas del curso seleccionado
    var selectedCourseTasks by remember { mutableStateOf<List<ClassroomTask>>(emptyList()) }
    var showTasksDialog by remember { mutableStateOf(false) }
    var selectedCourseName by remember { mutableStateOf("") }
    // Estado para controlar la navegación manual tras importar tareas o cerrar el diálogo
    var navigateToHome by remember { mutableStateOf(false) }

    // Scopes completos para Classroom
    val allScopes = "oauth2:https://www.googleapis.com/auth/classroom.courses.readonly https://www.googleapis.com/auth/classroom.coursework.me.readonly https://www.googleapis.com/auth/classroom.coursework.students.readonly"
    // Launcher para el intent de consentimiento
    var pendingConsent by remember { mutableStateOf<Intent?>(null) }
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // El usuario concedió el permiso, puedes intentar de nuevo si lo deseas
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(
                        Scope("https://www.googleapis.com/auth/classroom.courses.readonly"),
                        Scope("https://www.googleapis.com/auth/classroom.coursework.me.readonly"),
                        Scope("https://www.googleapis.com/auth/classroom.coursework.students.readonly")
                    )
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInClient.signOut().addOnCompleteListener {
                    launcher.launch(googleSignInClient.signInIntent)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Iniciar sesión con Google")
        }
        if (!googleSignInError.isNullOrEmpty()) {
            Text(
                text = googleSignInError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
    if (showCourseDialog && classroomCourses.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { onDismissCourseDialog?.invoke() },
            title = { Text("Selecciona un curso de Classroom") },
            text = {
                Column {
                    classroomCourses.forEach { course ->
                        Button(
                            onClick = {
                                onSelectCourse?.invoke(course)
                                selectedCourseName = course.name
                                // Petición para obtener tareas del curso seleccionado
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val account = GoogleSignIn.getLastSignedInAccount(context)
                                        val googleAccount = account?.account
                                        val scope = allScopes
                                        if (googleAccount != null) {
                                            try {
                                                val accessToken = GoogleAuthUtil.getToken(context, googleAccount, scope)
                                                val url = URL("https://classroom.googleapis.com/v1/courses/${course.id}/courseWork")
                                                val conn = url.openConnection() as HttpURLConnection
                                                conn.requestMethod = "GET"
                                                conn.setRequestProperty("Authorization", "Bearer $accessToken")
                                                val responseCode = conn.responseCode
                                                var response = ""
                                                try {
                                                    response = conn.inputStream.bufferedReader().readText()
                                                } catch (e: Exception) {
                                                    Log.e("ClassroomAPI", "Error leyendo el cuerpo de la respuesta: ${e.localizedMessage}")
                                                }
                                                Log.d("ClassroomAPI", "Tareas response code: $responseCode\n$response")
                                                try {
                                                    val tasks = mutableListOf<ClassroomTask>()
                                                    val json = JSONObject(response)
                                                    val tasksArray = json.optJSONArray("courseWork") ?: JSONArray()
                                                    for (i in 0 until tasksArray.length()) {
                                                        val obj = tasksArray.getJSONObject(i)
                                                        // Procesar la fecha de vencimiento correctamente
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
                                                            ClassroomTask(
                                                                id = obj.optString("id"),
                                                                title = obj.optString("title"),
                                                                description = obj.optString("description"),
                                                                dueDate = dueDateString,
                                                                creationTime = obj.optString("creationTime")
                                                            )
                                                        )
                                                    }
                                                    Log.d("ClassroomAPI", "Tareas obtenidas: $tasks")
                                                    selectedCourseTasks = tasks
                                                    showTasksDialog = true
                                                    if (tasks.isEmpty()) {
                                                        Log.d("ClassroomAPI", "No hay tareas en este curso.")
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("ClassroomAPI", "Error de parsing de tareas: ${e.localizedMessage}")
                                                    Log.e("ClassroomAPI", "Respuesta recibida: $response")
                                                }
                                            } catch (e: UserRecoverableAuthException) {
                                                pendingConsent = e.intent
                                            }
                                        } else {
                                            Log.e("ClassroomAPI", "No se pudo obtener el objeto Account de GoogleSignInAccount para tareas")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ClassroomAPI", "Error al obtener tareas: ${e.localizedMessage}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(course.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = { onDismissCourseDialog?.invoke() }) { Text("Cancelar") }
            }
        )
    }
    // Lanzar el intent de consentimiento si es necesario
    LaunchedEffect(pendingConsent) {
        pendingConsent?.let {
            consentLauncher.launch(it)
            pendingConsent = null
        }
    }
    // Diálogo para mostrar tareas del curso seleccionado
    if (showTasksDialog) {
        AlertDialog(
            onDismissRequest = {
                showTasksDialog = false
                navigateToHome = true
            },
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
                    Button(onClick = {
                        // Importar todas las tareas a Room
                        tasksViewModel.importarTareasClassroom(selectedCourseTasks, selectedCourseName)
                        showTasksDialog = false
                        onDismissCourseDialog?.invoke()
                        navigateToHome = true
                    }) { Text("Importar todas") }
                }
            },
            dismissButton = {
                Button(onClick = {
                    showTasksDialog = false
                    navigateToHome = true
                }) { Text("Cerrar") }
            }
        )
    }
                    // Navegación tras importar tareas o cerrar el diálogo
                if (navigateToHome) {
                    LaunchedEffect(Unit) {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
} 