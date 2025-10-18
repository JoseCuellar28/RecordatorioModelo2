package com.example.recordatoriomodelo2.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatoriomodelo2.firebase.FirebaseManager
import com.example.recordatoriomodelo2.ui.UserProfile
import com.example.recordatoriomodelo2.ui.userProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val institution: String = "",
    val fullNameError: String = "",
    val emailError: String = "",
    val phoneError: String = "",
    val institutionError: String = "",
    val hasChanges: Boolean = false
)

class ProfileEditViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()
    
    private var originalProfile: UserProfile = userProfile
    
    init {
        loadCurrentProfile()
    }
    
    private fun loadCurrentProfile() {
        _uiState.value = _uiState.value.copy(
            fullName = userProfile.fullName,
            email = userProfile.email,
            phone = userProfile.phone,
            institution = userProfile.institution
        )
        originalProfile = userProfile
    }
    
    fun updateFullName(fullName: String) {
        _uiState.value = _uiState.value.copy(
            fullName = fullName,
            fullNameError = "",
            hasChanges = checkForChanges(fullName = fullName)
        )
    }
    
    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = "",
            hasChanges = checkForChanges(email = email)
        )
    }
    
    fun updatePhone(phone: String) {
        _uiState.value = _uiState.value.copy(
            phone = phone,
            phoneError = "",
            hasChanges = checkForChanges(phone = phone)
        )
    }
    
    fun updateInstitution(institution: String) {
        _uiState.value = _uiState.value.copy(
            institution = institution,
            institutionError = "",
            hasChanges = checkForChanges(institution = institution)
        )
    }
    
    private fun checkForChanges(
        fullName: String = _uiState.value.fullName,
        email: String = _uiState.value.email,
        phone: String = _uiState.value.phone,
        institution: String = _uiState.value.institution
    ): Boolean {
        return fullName.trim() != originalProfile.fullName ||
                email.trim() != originalProfile.email ||
                phone.trim() != originalProfile.phone ||
                institution.trim() != originalProfile.institution
    }
    
    fun validateFields(): Boolean {
        val currentState = _uiState.value
        var isValid = true
        var fullNameError = ""
        var emailError = ""
        var phoneError = ""
        var institutionError = ""
        
        // Validar nombre completo
        if (currentState.fullName.trim().isEmpty()) {
            fullNameError = "El nombre es obligatorio"
            isValid = false
        } else if (currentState.fullName.trim().length < 2) {
            fullNameError = "El nombre debe tener al menos 2 caracteres"
            isValid = false
        } else if (!currentState.fullName.trim().matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$".toRegex())) {
            fullNameError = "El nombre solo puede contener letras y espacios"
            isValid = false
        }
        
        // Validar email
        val emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        if (currentState.email.trim().isEmpty()) {
            emailError = "El email es obligatorio"
            isValid = false
        } else if (!currentState.email.trim().matches(emailPattern.toRegex())) {
            emailError = "Formato de email inválido"
            isValid = false
        }
        
        // Validar teléfono (opcional pero si se proporciona debe ser válido)
        if (currentState.phone.trim().isNotEmpty()) {
            val phonePattern = "^[+]?[0-9\\s\\-()]{8,20}$"
            if (!currentState.phone.trim().matches(phonePattern.toRegex())) {
                phoneError = "Formato de teléfono inválido"
                isValid = false
            }
        }
        
        // Validar institución
        if (currentState.institution.trim().isEmpty()) {
            institutionError = "La institución es obligatoria"
            isValid = false
        } else if (currentState.institution.trim().length < 3) {
            institutionError = "La institución debe tener al menos 3 caracteres"
            isValid = false
        }
        
        _uiState.value = _uiState.value.copy(
            fullNameError = fullNameError,
            emailError = emailError,
            phoneError = phoneError,
            institutionError = institutionError
        )
        
        return isValid
    }
    
    fun saveProfile(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!validateFields()) {
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                val currentUser = FirebaseManager.getCurrentUser()
                if (currentUser != null) {
                    val currentState = _uiState.value
                    val updates = mapOf(
                        "fullName" to currentState.fullName.trim(),
                        "email" to currentState.email.trim(),
                        "phone" to currentState.phone.trim(),
                        "institution" to currentState.institution.trim(),
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                    
                    val result = FirebaseManager.updateUserProfile(currentUser.uid, updates)
                    
                    if (result.isSuccess) {
                        // Actualizar estado global
                        userProfile = userProfile.copy(
                            fullName = currentState.fullName.trim(),
                            email = currentState.email.trim(),
                            phone = currentState.phone.trim(),
                            institution = currentState.institution.trim()
                        )
                        
                        // Actualizar perfil original para futuras comparaciones
                        originalProfile = userProfile
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            hasChanges = false
                        )
                        
                        onSuccess()
                    } else {
                        val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido"
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                        onError(errorMsg)
                    }
                } else {
                    val errorMsg = "Usuario no autenticado"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Error inesperado: ${e.message}"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
                onError(errorMsg)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
    
    fun resetToOriginal() {
        _uiState.value = _uiState.value.copy(
            fullName = originalProfile.fullName,
            email = originalProfile.email,
            phone = originalProfile.phone,
            institution = originalProfile.institution,
            fullNameError = "",
            emailError = "",
            phoneError = "",
            institutionError = "",
            hasChanges = false,
            errorMessage = null
        )
    }
    
    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val currentUser = FirebaseManager.getCurrentUser()
                if (currentUser != null) {
                    val result = FirebaseManager.getUserProfile(currentUser.uid)
                    if (result.isSuccess) {
                        val profileData = result.getOrNull()
                        if (profileData != null) {
                            val updatedProfile = UserProfile(
                                fullName = profileData["fullName"] as? String ?: "",
                                email = profileData["email"] as? String ?: "",
                                phone = profileData["phone"] as? String ?: "",
                                institution = profileData["institution"] as? String ?: "",
                                profileImageUrl = profileData["profileImageUrl"] as? String ?: ""
                            )
                            
                            // Actualizar estado global
                            userProfile = updatedProfile
                            originalProfile = updatedProfile
                            
                            // Actualizar UI state
                            _uiState.value = _uiState.value.copy(
                                fullName = updatedProfile.fullName,
                                email = updatedProfile.email,
                                phone = updatedProfile.phone,
                                institution = updatedProfile.institution,
                                hasChanges = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al cargar perfil: ${e.message}"
                )
            }
        }
    }
}