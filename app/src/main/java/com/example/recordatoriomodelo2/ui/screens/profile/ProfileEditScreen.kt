package com.example.recordatoriomodelo2.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.recordatoriomodelo2.firebase.FirebaseManager
import com.example.recordatoriomodelo2.ui.UserProfile
import com.example.recordatoriomodelo2.ui.userProfile
import com.example.recordatoriomodelo2.utils.rememberImagePickerHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    navController: NavHostController,
    viewModel: ProfileEditViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // Estados del formulario
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    
    // Estados de validación
    var fullNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }
    var institutionError by remember { mutableStateOf("") }
    
    // Estados de UI
    var isLoading by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    
    // Estado del ViewModel
    val uiState by viewModel.uiState.collectAsState()
    
    // Helper para selección de imagen
    val imagePickerHelper = rememberImagePickerHelper(
        onImageSelected = { uri ->
            uri?.let { 
                viewModel.updateProfileImage(it, context)
            }
        },
        onError = { error ->
            Toast.makeText(context, "Error al seleccionar imagen: $error", Toast.LENGTH_SHORT).show()
        }
    )
    
    // Sincronizar estados del formulario con ViewModel
    LaunchedEffect(uiState) {
        fullName = uiState.fullName
        email = uiState.email
        phone = uiState.phone
        institution = uiState.institution
    }
    
    // Colores del tema
    val rojoVibrante = Color(0xFFEF4444)
    val azulMarino = Color(0xFF1E293B)
    val grisClaro = Color(0xFFF3F4F6)
    val verdeExito = Color(0xFF10B981)
    
    // Función para validar campos (solo campos editables)
    fun validateFields(): Boolean {
        var isValid = true
        
        // Validar teléfono (opcional pero si se proporciona debe ser válido)
        if (phone.trim().isNotEmpty()) {
            val phonePattern = "^[+]?[0-9]{8,15}$"
            if (!phone.trim().matches(phonePattern.toRegex())) {
                phoneError = "Formato de teléfono inválido (8-15 dígitos)"
                isValid = false
            } else {
                phoneError = ""
            }
        } else {
            phoneError = ""
        }
        
        // Validar institución
        if (institution.trim().isEmpty()) {
            institutionError = "La institución es obligatoria"
            isValid = false
        } else if (institution.trim().length < 3) {
            institutionError = "La institución debe tener al menos 3 caracteres"
            isValid = false
        } else {
            institutionError = ""
        }
        
        return isValid
    }
    
    // Función para guardar cambios
    fun saveChanges() {
        if (validateFields()) {
            isLoading = true
            scope.launch {
                try {
                    val currentUser = FirebaseManager.getCurrentUser()
                    if (currentUser != null) {
                        val updates = mapOf(
                            "phone" to phone.trim(),
                            "institution" to institution.trim(),
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        )
                        
                        val result = FirebaseManager.updateUserProfile(currentUser.uid, updates)
                        
                        if (result.isSuccess) {
                            // Actualizar estado global (solo campos editables)
                            userProfile = userProfile.copy(
                                phone = phone.trim(),
                                institution = institution.trim()
                            )
                            
                            Toast.makeText(context, "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "Error al actualizar perfil: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }
    
    // Verificar si hay cambios (solo en campos editables)
    fun hasChanges(): Boolean {
        return uiState.hasChanges
    }
    
    // Diálogo de confirmación para descartar cambios
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Descartar cambios") },
            text = { Text("¿Estás seguro de que quieres descartar los cambios realizados?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Descartar", color = rojoVibrante)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header con botón de retroceso
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (hasChanges()) {
                        showDiscardDialog = true
                    } else {
                        navController.popBackStack()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = azulMarino
                )
            }
            
            Text(
                text = "Editar Perfil",
                style = MaterialTheme.typography.headlineSmall,
                color = azulMarino,
                fontWeight = FontWeight.Bold
            )
            
            // Espacio para balancear el layout
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Avatar del usuario
        val inicial = fullName.trim().takeIf { it.isNotEmpty() }?.firstOrNull()?.uppercase() ?: "U"
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .clickable { 
                    imagePickerHelper.showImagePicker()
                },
            contentAlignment = Alignment.Center
        ) {
            if (uiState.profileImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uiState.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = android.R.drawable.ic_menu_camera),
                    placeholder = painterResource(id = android.R.drawable.ic_menu_camera)
                )
            } else {
                // Mostrar inicial si no hay imagen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = rojoVibrante,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inicial,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Overlay de carga si se está subiendo imagen
            if (uiState.isUploadingImage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Toca para cambiar foto",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Formulario de edición
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = grisClaro),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Campo Nombre Completo (Solo lectura)
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { }, // No permite cambios
                    label = { Text("Nombre completo") },
                    leadingIcon = { 
                        Icon(Icons.Default.Person, contentDescription = null) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false, // Campo deshabilitado
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledLabelColor = Color.Gray,
                        disabledLeadingIconColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Campo Email (Solo lectura)
                OutlinedTextField(
                    value = email,
                    onValueChange = { }, // No permite cambios
                    label = { Text("Correo electrónico") },
                    leadingIcon = { 
                        Icon(Icons.Default.Email, contentDescription = null) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false, // Campo deshabilitado
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledLabelColor = Color.Gray,
                        disabledLeadingIconColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Campo Teléfono
                OutlinedTextField(
                    value = phone,
                    onValueChange = { 
                        phone = it
                        viewModel.updatePhone(it)
                        if (phoneError.isNotEmpty()) phoneError = ""
                    },
                    label = { Text("Número de teléfono") },
                    leadingIcon = { 
                        Icon(Icons.Default.Phone, contentDescription = null) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError.isNotEmpty(),
                    supportingText = if (phoneError.isNotEmpty()) {
                        { Text(phoneError, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Campo Institución
                OutlinedTextField(
                    value = institution,
                    onValueChange = { 
                        institution = it
                        viewModel.updateInstitution(it)
                        if (institutionError.isNotEmpty()) institutionError = ""
                    },
                    label = { Text("Centro de estudio *") },
                    leadingIcon = { 
                        Icon(Icons.Default.School, contentDescription = null) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = institutionError.isNotEmpty(),
                    supportingText = if (institutionError.isNotEmpty()) {
                        { Text(institutionError, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Texto de campos obligatorios
        Text(
            text = "* Centro de estudio es obligatorio",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Botón Cancelar
            OutlinedButton(
                onClick = {
                    if (hasChanges()) {
                        showDiscardDialog = true
                    } else {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, azulMarino)
            ) {
                Text(
                    "Cancelar",
                    color = azulMarino,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Botón Guardar
            Button(
                onClick = { saveChanges() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && hasChanges(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasChanges()) verdeExito else Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Guardar",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}