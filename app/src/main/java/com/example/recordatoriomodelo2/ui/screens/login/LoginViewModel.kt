package com.example.recordatoriomodelo2.ui.screens.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Estado de la pantalla de login
data class LoginState(
    val user: String = "",
    val password: String = "",
    val errorMessage: String? = null,
    val isGoogleSignInSuccess: Boolean = false,
    val googleSignInError: String? = null,
    val classroomCourses: List<ClassroomCourse> = emptyList(),
    val selectedCourse: ClassroomCourse? = null,
    val showCourseDialog: Boolean = false
)

// Eventos de la pantalla de login
sealed class LoginEvent {
    data class UserChanged(val value: String) : LoginEvent()
    data class PasswordChanged(val value: String) : LoginEvent()
    object LoginClicked : LoginEvent()
    data class GoogleSignInResult(val success: Boolean, val error: String? = null) : LoginEvent()
    data class ClassroomCoursesLoaded(val courses: List<ClassroomCourse>) : LoginEvent()
    data class SelectCourse(val course: ClassroomCourse) : LoginEvent()
    object DismissCourseDialog : LoginEvent()
}

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.UserChanged -> {
                _state.value = _state.value.copy(user = event.value, errorMessage = null)
            }
            is LoginEvent.PasswordChanged -> {
                _state.value = _state.value.copy(password = event.value, errorMessage = null)
            }
            is LoginEvent.LoginClicked -> {
                authenticate()
            }
            is LoginEvent.GoogleSignInResult -> {
                if (event.success) {
                    _state.value = _state.value.copy(isGoogleSignInSuccess = true, googleSignInError = null)
                } else {
                    _state.value = _state.value.copy(isGoogleSignInSuccess = false, googleSignInError = event.error)
                }
            }
            is LoginEvent.ClassroomCoursesLoaded -> {
                _state.value = _state.value.copy(
                    classroomCourses = event.courses,
                    showCourseDialog = true
                )
            }
            is LoginEvent.SelectCourse -> {
                _state.value = _state.value.copy(selectedCourse = event.course, showCourseDialog = false)
            }
            is LoginEvent.DismissCourseDialog -> {
                _state.value = _state.value.copy(showCourseDialog = false)
            }
        }
    }

    private fun authenticate() {
        val user = _state.value.user
        val password = _state.value.password
        if (user.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Por favor, complete todos los campos.")
            return
        }
        if (user == "admin" && password == "1234") {
            _state.value = _state.value.copy(errorMessage = null)
        } else {
            _state.value = _state.value.copy(errorMessage = "Usuario o contraseña incorrectos.")
        }
    }
} 