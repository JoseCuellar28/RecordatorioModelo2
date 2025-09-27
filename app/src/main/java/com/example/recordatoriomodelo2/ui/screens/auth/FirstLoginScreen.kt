package com.example.recordatoriomodelo2.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import android.widget.Toast
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.recordatoriomodelo2.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Función para validar contraseña
private fun validatePassword(password: String): String {
    if (password.length < 8) return "La contraseña debe tener al menos 8 caracteres"
    if (!password.any { it.isUpperCase() }) return "Debe contener al menos una letra mayúscula"
    if (!password.any { it.isDigit() }) return "Debe contener al menos un número"
    if (!password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) return "Debe contener al menos un carácter especial"
    return ""
}

// Función para obtener la fuerza de la contraseña
private fun getPasswordStrength(password: String): Pair<String, Color> {
    var score = 0
    
    // Longitud
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    
    // Mayúsculas
    if (password.any { it.isUpperCase() }) score++
    
    // Minúsculas
    if (password.any { it.isLowerCase() }) score++
    
    // Números
    if (password.any { it.isDigit() }) score++
    
    // Caracteres especiales
    if (password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) score++
    
    return when (score) {
        0, 1 -> "Muy débil" to Color(0xFFD32F2F)
        2 -> "Débil" to Color(0xFFFF5722)
        3 -> "Regular" to Color(0xFFFF9800)
        4 -> "Fuerte" to Color(0xFF4CAF50)
        5, 6 -> "Muy fuerte" to Color(0xFF2E7D32)
        else -> "Muy débil" to Color(0xFFD32F2F)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstLoginScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }
    var userProfileImageUrl by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf("") }
    
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Obtener información del usuario actual
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userProfileImageUrl = document.getString("profileImageUrl")
                        userName = document.getString("fullName") ?: ""
                    }
                }
        }
    }
    
    // Validaciones en tiempo real
    LaunchedEffect(newPassword) {
        passwordError = validatePassword(newPassword)
    }
    
    LaunchedEffect(confirmPassword) {
        confirmPasswordError = if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
            "Las contraseñas no coinciden"
        } else ""
    }
    
    // Navegar a home cuando el cambio de contraseña sea exitoso
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage?.contains("actualizada") == true) {
            // Mostrar toast de confirmación
            Toast.makeText(context, "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show()
            
            // Esperar 2 segundos antes de navegar
            kotlinx.coroutines.delay(2000)
            
            navController.navigate("home") {
                popUpTo("first_login") { inclusive = true }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título
        Text(
            text = "Configuración de Contraseña",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Imagen de perfil del usuario
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (userProfileImageUrl != null && userProfileImageUrl!!.isNotEmpty()) {
                AsyncImage(
                    model = userProfileImageUrl,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Imagen por defecto si no hay imagen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Usuario",
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Nombre del usuario
        if (userName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bienvenido, $userName",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mensaje informativo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔐 Cambio de Contraseña Obligatorio",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Por seguridad, debes cambiar tu contraseña temporal por una permanente antes de continuar.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Campo nueva contraseña
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("Nueva Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                    Icon(
                        imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (newPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = passwordError.isNotEmpty(),
            supportingText = if (passwordError.isNotEmpty()) {
                { Text(passwordError, color = MaterialTheme.colorScheme.error) }
            } else null
        )
        
        // Barra de seguridad de contraseña
        if (newPassword.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            val (strengthText, strengthColor) = getPasswordStrength(newPassword)
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seguridad:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = strengthText,
                        fontSize = 12.sp,
                        color = strengthColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LinearProgressIndicator(
                    progress = when (strengthText) {
                        "Muy débil" -> 0.2f
                        "Débil" -> 0.4f
                        "Regular" -> 0.6f
                        "Fuerte" -> 0.8f
                        "Muy fuerte" -> 1.0f
                        else -> 0.0f
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = strengthColor,
                    trackColor = strengthColor.copy(alpha = 0.3f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Campo confirmar contraseña
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmar Nueva Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = confirmPasswordError.isNotEmpty(),
            supportingText = if (confirmPasswordError.isNotEmpty()) {
                { Text(confirmPasswordError, color = MaterialTheme.colorScheme.error) }
            } else null
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Requisitos de contraseña
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Requisitos de la contraseña:",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Mínimo 8 caracteres\n• Al menos una letra mayúscula\n• Al menos un número\n• Al menos un carácter especial (!@#$%^&*)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Botón cambiar contraseña
        Button(
            onClick = {
                authViewModel.clearMessages()
                authViewModel.changeTemporaryPassword(newPassword)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !uiState.isLoading && 
                    newPassword.isNotEmpty() && 
                    confirmPassword.isNotEmpty() &&
                    newPassword == confirmPassword &&
                    passwordError.isEmpty() &&
                    confirmPasswordError.isEmpty()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Cambiar Contraseña", fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mostrar mensajes de error
        uiState.errorMessage?.let { message ->
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
        

    }
}