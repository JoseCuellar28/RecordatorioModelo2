package com.example.recordatoriomodelo2.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.navigation.NavHostController
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatoriomodelo2.viewmodel.AuthViewModel
import androidx.compose.ui.platform.LocalContext
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    Log.d("RegisterScreen", "=== INICIO RegisterScreen Composable ===")
    
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellidoPaterno by remember { mutableStateOf("") }
    var apellidoMaterno by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }

    var showSuccessMessage by remember { mutableStateOf(false) }
    
    // Estados de validación
    var emailError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }
    
    // Funciones de validación
    fun validateEmail(email: String): String {
        return when {
            email.isEmpty() -> ""
            !email.contains("@") -> "El correo debe contener @"
            !email.contains(".") -> "El correo debe contener un punto"
            !Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matcher(email).matches() -> "Formato de correo inválido"
            else -> ""
        }
    }
    
    fun validatePhone(phone: String): String {
        return when {
            phone.isEmpty() -> ""
            phone.length != 9 -> "El teléfono debe tener 9 dígitos"
            !phone.startsWith("9") -> "El teléfono debe empezar con 9"
            !phone.all { it.isDigit() } -> "El teléfono solo debe contener números"
            else -> ""
        }
    }
    

    
    // Validaciones en tiempo real
    LaunchedEffect(email) {
        emailError = validateEmail(email)
    }
    
    LaunchedEffect(phone) {
        phoneError = validatePhone(phone)
    }
    

    
    val uiState by authViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    Log.d("RegisterScreen", "Estado actual - isLoading: ${uiState.isLoading}, successMessage: ${uiState.successMessage}, errorMessage: ${uiState.errorMessage}, showSuccessMessage: $showSuccessMessage")
    
    // Log adicional para verificar campos y estado del botón
    val fullName = "$nombre $apellidoPaterno $apellidoMaterno".trim()
    val isButtonEnabled = !uiState.isLoading && 
                         nombre.isNotEmpty() && 
                         apellidoPaterno.isNotEmpty() && 
                         apellidoMaterno.isNotEmpty() && 
                         email.isNotEmpty() && 
                         institution.isNotEmpty() &&
                         emailError.isEmpty() &&
                         (phone.isEmpty() || phoneError.isEmpty()) // Teléfono opcional
    Log.d("RegisterScreen", "Campos: nombre='$nombre', apellidoPaterno='$apellidoPaterno', apellidoMaterno='$apellidoMaterno', email='$email', phone='$phone', institution='$institution'")
    Log.d("RegisterScreen", "Botón habilitado: $isButtonEnabled (isLoading=${uiState.isLoading})")

    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Título
        Text(
            text = "Crear Cuenta",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Completa los datos para registrarte",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Campos del formulario
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = apellidoPaterno,
            onValueChange = { apellidoPaterno = it },
            label = { Text("Apellido Paterno") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = apellidoMaterno,
            onValueChange = { apellidoMaterno = it },
            label = { Text("Apellido Materno") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            isError = emailError.isNotEmpty(),
            supportingText = if (emailError.isNotEmpty()) {
                { Text(emailError, color = MaterialTheme.colorScheme.error) }
            } else null
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Teléfono (opcional)") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            isError = phoneError.isNotEmpty(),
            supportingText = if (phoneError.isNotEmpty()) {
                { Text(phoneError, color = MaterialTheme.colorScheme.error) }
            } else null
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = institution,
            onValueChange = { institution = it },
            label = { Text("Institución") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        

        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Botón de registro
        Button(
            onClick = {
                Log.d("RegisterScreen", "=== BOTÓN REGISTRO PRESIONADO ===")
                Log.d("RegisterScreen", "Datos: email=$email, nombre=$nombre, apellidoPaterno=$apellidoPaterno, apellidoMaterno=$apellidoMaterno, phone=$phone, institution=$institution")
                
                authViewModel.clearMessages()
                showSuccessMessage = false
                Log.d("RegisterScreen", "Mensajes limpiados, showSuccessMessage=false")
                
                authViewModel.registerUserWithTemporaryPassword(
                    context = context,
                    email = email,
                    nombre = nombre,
                    apellidoPaterno = apellidoPaterno,
                    apellidoMaterno = apellidoMaterno,
                    phone = phone,
                    institution = institution,
                    onSuccess = {
                        Log.d("RegisterScreen", "=== CALLBACK onSuccess EJECUTADO ===")
                        // Manejar éxito directamente aquí
                        showSuccessMessage = true
                        Log.d("RegisterScreen", "showSuccessMessage=true, iniciando navegación en 3 segundos")
                        
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            Log.d("RegisterScreen", "Esperando 3 segundos...")
                            kotlinx.coroutines.delay(3000)
                            Log.d("RegisterScreen", "3 segundos completados, navegando a login")
                            authViewModel.clearMessages()
                            showSuccessMessage = false
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                            Log.d("RegisterScreen", "Navegación completada")
                        }
                    }
                )
                Log.d("RegisterScreen", "registerUserWithTemporaryPassword llamado")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = isButtonEnabled
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Registrarse", fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Enlace para ir al login
        TextButton(onClick = { 
            navController.navigate("login") {
                popUpTo("register") { inclusive = true }
            }
        }) {
            Text("¿Ya tienes cuenta? Inicia sesión")
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
        
        // Mostrar mensaje de éxito cuando se registra correctamente
        if (showSuccessMessage) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = "Se envió contraseña segura al correo registrado",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}