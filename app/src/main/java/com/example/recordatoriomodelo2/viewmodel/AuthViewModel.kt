package com.example.recordatoriomodelo2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatoriomodelo2.firebase.FirebaseManager
import com.example.recordatoriomodelo2.services.EmailService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import android.util.Log
import android.content.Context

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val temporaryPassword: String? = null,
    val showPasswordDialog: Boolean = false,
    val isFirstLogin: Boolean = false
)

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

class AuthViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        // No verificar automáticamente el estado de autenticación
        // La aplicación siempre debe iniciar en la pantalla de Login
    }
    
    private fun checkAuthState() {
        _uiState.value = _uiState.value.copy(
            isLoggedIn = FirebaseManager.isUserLoggedIn()
        )
    }
    
    // Validar email
    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(false, "El email es requerido")
        }
        
        if (!email.contains("@")) {
            return ValidationResult(false, "El email debe contener @")
        }
        
        if (!email.contains(".")) {
            return ValidationResult(false, "El email debe contener un punto")
        }
        
        return ValidationResult(true)
    }
    
    // Validar contraseña
    fun validatePassword(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult(false, "La contraseña es requerida")
        }
        
        if (password.length < 6) {
            return ValidationResult(false, "La contraseña debe tener al menos 6 caracteres")
        }
        
        return ValidationResult(true)
    }
    
    // Validar nombre completo
    fun validateFullName(fullName: String): ValidationResult {
        if (fullName.isBlank()) {
            return ValidationResult(false, "El nombre completo es requerido")
        }
        
        if (fullName.length < 2) {
            return ValidationResult(false, "El nombre debe tener al menos 2 caracteres")
        }
        
        return ValidationResult(true)
    }
    
    // Validar nombre individual
    fun validateNombre(nombre: String): ValidationResult {
        if (nombre.isBlank()) {
            return ValidationResult(false, "El nombre es requerido")
        }
        
        if (nombre.length < 2) {
            return ValidationResult(false, "El nombre debe tener al menos 2 caracteres")
        }
        
        if (!nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$"))) {
            return ValidationResult(false, "El nombre solo puede contener letras")
        }
        
        return ValidationResult(true)
    }
    
    // Validar apellido paterno
    fun validateApellidoPaterno(apellidoPaterno: String): ValidationResult {
        if (apellidoPaterno.isBlank()) {
            return ValidationResult(false, "El apellido paterno es requerido")
        }
        
        if (apellidoPaterno.length < 2) {
            return ValidationResult(false, "El apellido paterno debe tener al menos 2 caracteres")
        }
        
        if (!apellidoPaterno.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$"))) {
            return ValidationResult(false, "El apellido paterno solo puede contener letras")
        }
        
        return ValidationResult(true)
    }
    
    // Validar apellido materno
    fun validateApellidoMaterno(apellidoMaterno: String): ValidationResult {
        if (apellidoMaterno.isBlank()) {
            return ValidationResult(false, "El apellido materno es requerido")
        }
        
        if (apellidoMaterno.length < 2) {
            return ValidationResult(false, "El apellido materno debe tener al menos 2 caracteres")
        }
        
        if (!apellidoMaterno.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$"))) {
            return ValidationResult(false, "El apellido materno solo puede contener letras")
        }
        
        return ValidationResult(true)
    }
    
    // Validar contraseña con requisitos de seguridad
    fun validateSecurePassword(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult(false, "La contraseña es requerida")
        }
        
        if (password.length < 8) {
            return ValidationResult(false, "La contraseña debe tener al menos 8 caracteres")
        }
        
        if (!password.any { it.isUpperCase() }) {
            return ValidationResult(false, "La contraseña debe tener al menos una mayúscula")
        }
        
        if (!password.any { it.isDigit() }) {
            return ValidationResult(false, "La contraseña debe tener al menos un número")
        }
        
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        if (!password.any { it in specialChars }) {
            return ValidationResult(false, "La contraseña debe tener al menos un carácter especial")
        }
        
        return ValidationResult(true)
    }
    
    // Validar confirmación de contraseña
    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationResult {
        if (confirmPassword.isBlank()) {
            return ValidationResult(false, "La confirmación de contraseña es requerida")
        }
        
        if (password != confirmPassword) {
            return ValidationResult(false, "Las contraseñas no coinciden")
        }
        
        return ValidationResult(true)
    }
    
    // Calcular fortaleza de contraseña
    fun calculatePasswordStrength(password: String): Int {
        var strength = 0
        
        if (password.length >= 8) strength++
        if (password.any { it.isLowerCase() }) strength++
        if (password.any { it.isUpperCase() }) strength++
        if (password.any { it.isDigit() }) strength++
        
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        if (password.any { it in specialChars }) strength++
        
        return strength
    }
    
    // Validar teléfono
    fun validatePhone(phone: String): ValidationResult {
        // El teléfono es opcional, si está vacío es válido
        if (phone.isBlank()) {
            return ValidationResult(true)
        }
        
        if (phone.length != 9) {
            return ValidationResult(false, "El teléfono debe tener 9 dígitos")
        }
        
        if (!phone.startsWith("9")) {
            return ValidationResult(false, "El teléfono debe empezar con 9")
        }
        
        if (!phone.all { it.isDigit() }) {
            return ValidationResult(false, "El teléfono solo debe contener números")
        }
        
        return ValidationResult(true)
    }
    
    // Validar institución
    fun validateInstitution(institution: String): ValidationResult {
        if (institution.isBlank()) {
            return ValidationResult(false, "La institución es requerida")
        }
        
        return ValidationResult(true)
    }
    
    // Iniciar sesión
    fun signIn(email: String, password: String) {
        val emailValidation = validateEmail(email)
        if (!emailValidation.isValid) {
            _uiState.value = _uiState.value.copy(errorMessage = emailValidation.errorMessage)
            return
        }
        
        val passwordValidation = validatePassword(password)
        if (!passwordValidation.isValid) {
            _uiState.value = _uiState.value.copy(errorMessage = passwordValidation.errorMessage)
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        Log.d("AuthViewModel", "Estado actualizado: isLoading=true, errorMessage=null")
        
        viewModelScope.launch {
            FirebaseManager.signInUser(email, password)
                .onSuccess { user ->
                    // Verificar si es el primer login
                    FirebaseManager.isFirstLogin()
                        .onSuccess { isFirstLogin ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                isFirstLogin = isFirstLogin,
                                successMessage = "Inicio de sesión exitoso"
                            )
                        }
                        .onFailure {
                            // Si falla la verificación, asumir que no es primer login
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                isFirstLogin = false,
                                successMessage = "Inicio de sesión exitoso"
                            )
                        }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = getErrorMessage(exception)
                    )
                }
        }
    }
    
    // Registrar usuario
    fun registerUser(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        institution: String
    ) {
        // Validar todos los campos
        val emailValidation = validateEmail(email)
        if (!emailValidation.isValid) {
            _uiState.value = _uiState.value.copy(errorMessage = emailValidation.errorMessage)
            return
        }
        
        val passwordValidation = validatePassword(password)
        if (!passwordValidation.isValid) {
            _uiState.value = _uiState.value.copy(errorMessage = passwordValidation.errorMessage)
            return
        }
        
        val nameValidation = validateFullName(fullName)
        if (!nameValidation.isValid) {
            Log.d("AuthViewModel", "Validación nombre FALLÓ: ${nameValidation.errorMessage}")
            _uiState.value = _uiState.value.copy(errorMessage = nameValidation.errorMessage)
            return
        }
        Log.d("AuthViewModel", "Validación nombre: OK")
        
        // Validar teléfono solo si no está vacío (es opcional)
        if (phone.isNotEmpty()) {
            val phoneValidation = validatePhone(phone)
            if (!phoneValidation.isValid) {
                Log.d("AuthViewModel", "Validación teléfono FALLÓ: ${phoneValidation.errorMessage}")
                _uiState.value = _uiState.value.copy(errorMessage = phoneValidation.errorMessage)
                return
            }
            Log.d("AuthViewModel", "Validación teléfono: OK")
        } else {
            Log.d("AuthViewModel", "Teléfono vacío (opcional): OK")
        }
        
        val institutionValidation = validateInstitution(institution)
        if (!institutionValidation.isValid) {
            _uiState.value = _uiState.value.copy(errorMessage = institutionValidation.errorMessage)
            return
        }
        
        // Limpiar mensajes previos y establecer estado de carga
        Log.d("AuthViewModel", "Estableciendo estado de carga...")
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )
        Log.d("AuthViewModel", "Estado actualizado: isLoading=true, mensajes limpiados")
        
        viewModelScope.launch {
            FirebaseManager.registerUser(email, password, fullName, phone, institution)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Registro exitoso. Verifica tu email para activar la cuenta."
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = getErrorMessage(exception)
                    )
                }
        }
    }
    
    // Registrar usuario con contraseña temporal
    fun registerUserWithTemporaryPassword(
        context: Context,
        email: String,
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        phone: String,
        institution: String,
        onSuccess: (() -> Unit)? = null
    ) {
        val fullName = "$nombre $apellidoPaterno $apellidoMaterno".trim()
        Log.d("AuthViewModel", "=== INICIO registerUserWithTemporaryPassword ===")
        Log.d("AuthViewModel", "Parámetros: email=$email, nombre=$nombre, apellidoPaterno=$apellidoPaterno, apellidoMaterno=$apellidoMaterno, phone=$phone, institution=$institution")
        Log.d("AuthViewModel", "Nombre completo construido: $fullName")
        Log.d("AuthViewModel", "onSuccess callback: ${if (onSuccess != null) "PRESENTE" else "NULL"}")
        
        // Verificar Google Play Services primero
        if (!FirebaseManager.checkGooglePlayServices(context)) {
            Log.e("AuthViewModel", "Google Play Services no está disponible")
            _uiState.value = _uiState.value.copy(
                errorMessage = "Google Play Services no está disponible. Por favor, actualiza Google Play Services e intenta nuevamente."
            )
            return
        }
        Log.d("AuthViewModel", "Google Play Services: OK")
        
        // Validar campos requeridos
        val emailValidation = validateEmail(email)
        if (!emailValidation.isValid) {
            Log.d("AuthViewModel", "Validación email FALLÓ: ${emailValidation.errorMessage}")
            _uiState.value = _uiState.value.copy(errorMessage = emailValidation.errorMessage)
            return
        }
        Log.d("AuthViewModel", "Validación email: OK")
        
        val nameValidation = validateFullName(fullName)
        if (!nameValidation.isValid) {
            Log.d("AuthViewModel", "Validación nombre FALLÓ: ${nameValidation.errorMessage}")
            _uiState.value = _uiState.value.copy(errorMessage = nameValidation.errorMessage)
            return
        }
        Log.d("AuthViewModel", "Validación nombre: OK")
        
        val phoneValidation = validatePhone(phone)
        if (!phoneValidation.isValid) {
            Log.d("AuthViewModel", "Validación teléfono FALLÓ: ${phoneValidation.errorMessage}")
            _uiState.value = _uiState.value.copy(errorMessage = phoneValidation.errorMessage)
            return
        }
        Log.d("AuthViewModel", "Validación teléfono: OK")
        
        val institutionValidation = validateInstitution(institution)
        if (!institutionValidation.isValid) {
            Log.d("AuthViewModel", "Validación institución FALLÓ: ${institutionValidation.errorMessage}")
            _uiState.value = _uiState.value.copy(errorMessage = institutionValidation.errorMessage)
            return
        }
        Log.d("AuthViewModel", "Validación institución: OK")
        
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Iniciando corrutina, llamando a FirebaseManager...")
                FirebaseManager.registerUserWithTemporaryPassword(email, fullName, phone, institution)
                    .onSuccess { (user, temporaryPassword) ->
                        Log.d("AuthViewModel", "=== FirebaseManager onSuccess RECIBIDO ===")
                        Log.d("AuthViewModel", "Mensaje de éxito: Se envió contraseña segura al correo registrado")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Se envió contraseña segura al correo registrado"
                        )
                        Log.d("AuthViewModel", "Estado actualizado: isLoading=false, successMessage=Se envió contraseña segura al correo registrado")
                        // Llamar al callback de éxito si se proporciona
                        Log.d("AuthViewModel", "Invocando callback onSuccess...")
                        onSuccess?.invoke()
                        Log.d("AuthViewModel", "Callback onSuccess invocado")
                    }
                    .onFailure { exception ->
                        Log.d("AuthViewModel", "=== FirebaseManager onFailure RECIBIDO ===")
                        Log.d("AuthViewModel", "Error: ${getErrorMessage(exception)}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = getErrorMessage(exception)
                        )
                        Log.d("AuthViewModel", "Estado actualizado: isLoading=false, errorMessage=${getErrorMessage(exception)}")
                    }
                Log.d("AuthViewModel", "Llamada a FirebaseManager completada")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error inesperado en registerUserWithTemporaryPassword: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error inesperado: ${e.message}"
                )
            }
        }
    }
    
    // Cambiar contraseña temporal
    fun changeTemporaryPassword(newPassword: String) {
        val passwordValidation = validatePassword(newPassword)
        if (!passwordValidation.isValid) {
            _uiState.value = _uiState.value.copy(errorMessage = passwordValidation.errorMessage)
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            FirebaseManager.changeTemporaryPassword(newPassword)
                .onSuccess {
                    // Obtener información del usuario para enviar notificación
                    val currentUser = FirebaseManager.getCurrentUser()
                    currentUser?.let { user ->
                        FirebaseManager.getUserProfile(user.uid)
                            .onSuccess { profile ->
                                val fullName = profile["fullName"] as? String ?: ""
                                val email = profile["email"] as? String ?: user.email ?: ""
                                
                                // Enviar notificación por correo
                                EmailService.sendPasswordChangeNotification(email, fullName)
                                    .onFailure { emailException ->
                                        println("Error enviando notificación: ${emailException.message}")
                                    }
                            }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showPasswordDialog = false,
                        temporaryPassword = null,
                        successMessage = "Contraseña actualizada exitosamente"
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = getErrorMessage(exception)
                    )
                }
        }
    }
    
    // Enviar email de restablecimiento de contraseña
    fun sendPasswordResetEmail(email: String) {
        val emailValidation = validateEmail(email)
        if (!emailValidation.isValid) {
            _uiState.value = _uiState.value.copy(errorMessage = emailValidation.errorMessage)
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            FirebaseManager.sendPasswordResetEmail(email)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Email de restablecimiento enviado"
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = getErrorMessage(exception)
                    )
                }
        }
    }
    
    // Cerrar sesión
    fun signOut() {
        FirebaseManager.signOut()
        _uiState.value = _uiState.value.copy(
            isLoggedIn = false,
            successMessage = "Sesión cerrada exitosamente"
        )
    }
    
    // Limpiar mensajes
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
    
    // Cerrar diálogo de contraseña
    fun dismissPasswordDialog() {
        _uiState.value = _uiState.value.copy(
            showPasswordDialog = false,
            temporaryPassword = null
        )
    }
    
    // Obtener mensaje de error amigable
    private fun getErrorMessage(exception: Throwable): String {
        return when (exception.message) {
            "The email address is already in use by another account." -> 
                "Este email ya está registrado"
            "The password is invalid or the user does not have a password." -> 
                "Contraseña incorrecta"
            "There is no user record corresponding to this identifier. The user may have been deleted." -> 
                "Usuario no encontrado"
            "The email address is badly formatted." -> 
                "Formato de email inválido"
            "The given password is invalid. [ Password should be at least 6 characters ]" -> 
                "La contraseña debe tener al menos 6 caracteres"
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> 
                "Error de conexión. Verifica tu internet"
            else -> exception.message ?: "Error desconocido"
        }
    }
}