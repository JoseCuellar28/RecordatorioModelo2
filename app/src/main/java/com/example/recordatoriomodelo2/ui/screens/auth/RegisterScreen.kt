package com.example.recordatoriomodelo2.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.AccountCircle
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
import com.example.recordatoriomodelo2.viewmodel.AuthViewModelFactory
import androidx.compose.ui.platform.LocalContext
import java.util.regex.Pattern
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import android.net.Uri
import android.widget.Toast
import com.example.recordatoriomodelo2.utils.rememberImagePickerHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavHostController
) {
    Log.d("RegisterScreen", "=== INICIO RegisterScreen Composable ===")
    
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context)
    )
    var email by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellidoPaterno by remember { mutableStateOf("") }
    var apellidoMaterno by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Helper para selección de imagen
    val imagePickerHelper = rememberImagePickerHelper(
        onImageSelected = { uri ->
            selectedImageUri = uri
        },
        onError = { error ->
            // Manejar error de selección de imagen
            // Por ahora solo lo logueamos
            android.util.Log.e("RegisterScreen", "Error al seleccionar imagen: $error")
        }
    )
    
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
    
    Log.d("RegisterScreen", "Estado actual - isLoading: ${uiState.isLoading}, successMessage: ${uiState.successMessage}, errorMessage: ${uiState.errorMessage}")
    
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Componente de selección de foto de perfil
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clickable { imagePickerHelper.showImagePicker() },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Seleccionar foto",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
                
                // Icono de cámara en la esquina
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .clickable { imagePickerHelper.showImagePicker() },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Cambiar foto",
                            modifier = Modifier.padding(6.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Toca para agregar foto de perfil",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
                Log.d("RegisterScreen", "Mensajes limpiados")
                
                authViewModel.registerUserWithTemporaryPassword(
                    context = context,
                    email = email,
                    nombre = nombre,
                    apellidoPaterno = apellidoPaterno,
                    apellidoMaterno = apellidoMaterno,
                    phone = phone,
                    institution = institution,
                    profileImageUri = selectedImageUri,
                    onSuccess = {
                        Log.d("RegisterScreen", "=== CALLBACK onSuccess EJECUTADO ===")
                        // Mostrar Toast de éxito
                        Toast.makeText(
                            context,
                            "Contraseña temporal enviada exitosamente al correo",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            Log.d("RegisterScreen", "Esperando 2 segundos...")
                            kotlinx.coroutines.delay(2000)
                            Log.d("RegisterScreen", "2 segundos completados, navegando a login")
                            authViewModel.clearMessages()
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
        
        // Mostrar mensajes de error
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
        
        // Mostrar mensajes de éxito
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
                    color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

    }
}