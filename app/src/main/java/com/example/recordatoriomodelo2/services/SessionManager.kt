package com.example.recordatoriomodelo2.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Servicio para gestionar sesiones de usuario multi-dispositivo
 * Maneja autenticación, persistencia de sesión y validación de estado
 */
class SessionManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SessionManager"
        private const val PREFS_NAME = "session_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_LAST_LOGIN = "last_login"
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_AUTO_LOGIN_ENABLED = "auto_login_enabled"
        private const val SESSIONS_COLLECTION = "sessions"
        
        @Volatile
        private var INSTANCE: SessionManager? = null
        
        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Estados de sesión
    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    sealed class AuthState {
        object Checking : AuthState()
        object Authenticated : AuthState()
        object NotAuthenticated : AuthState()
        data class Error(val message: String) : AuthState()
    }
    
    data class SessionInfo(
        val userId: String,
        val email: String,
        val deviceId: String,
        val lastActivity: String,
        val isActive: Boolean
    )
    
    init {
        // Configurar listener de estado de autenticación
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            
            if (user != null) {
                Log.d(TAG, "Usuario autenticado: ${user.uid}")
                _authState.value = AuthState.Authenticated
                updateSessionInfo(user)
            } else {
                Log.d(TAG, "Usuario no autenticado")
                _authState.value = AuthState.NotAuthenticated
                clearSessionInfo()
            }
        }
    }
    
    /**
     * Verifica si el usuario está autenticado
     */
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Obtiene el usuario actual
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    /**
     * Obtiene el ID del usuario actual
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Verifica si el auto-login está habilitado
     */
    fun isAutoLoginEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, true)
    }
    
    /**
     * Habilita o deshabilita el auto-login
     */
    fun setAutoLoginEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN_ENABLED, enabled).apply()
        Log.d(TAG, "Auto-login ${if (enabled) "habilitado" else "deshabilitado"}")
    }
    
    /**
     * Actualiza la información de sesión en Firestore
     */
    private fun updateSessionInfo(user: FirebaseUser) {
        try {
            val deviceId = getDeviceId()
            val currentTime = getCurrentTimestamp()
            
            val sessionInfo = mapOf(
                "userId" to user.uid,
                "email" to (user.email ?: ""),
                "deviceId" to deviceId,
                "lastActivity" to currentTime,
                "isActive" to true,
                "loginTime" to currentTime
            )
            
            // Actualizar información local
            prefs.edit().apply {
                putString(KEY_USER_ID, user.uid)
                putString(KEY_EMAIL, user.email ?: "")
                putString(KEY_LAST_LOGIN, currentTime)
                putString(KEY_SESSION_TOKEN, generateSessionToken())
                apply()
            }
            
            // Actualizar información en Firestore
            firestore.collection(SESSIONS_COLLECTION)
                .document("${user.uid}_$deviceId")
                .set(sessionInfo)
                .addOnSuccessListener {
                    Log.d(TAG, "Información de sesión actualizada en Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al actualizar información de sesión", e)
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar información de sesión", e)
        }
    }
    
    /**
     * Limpia la información de sesión
     */
    private fun clearSessionInfo() {
        val deviceId = getDeviceId()
        val userId = prefs.getString(KEY_USER_ID, null)
        
        // Marcar sesión como inactiva en Firestore
        if (userId != null) {
            firestore.collection(SESSIONS_COLLECTION)
                .document("${userId}_$deviceId")
                .update("isActive", false, "logoutTime", getCurrentTimestamp())
                .addOnSuccessListener {
                    Log.d(TAG, "Sesión marcada como inactiva en Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al marcar sesión como inactiva", e)
                }
        }
        
        // Limpiar información local
        prefs.edit().clear().apply()
        Log.d(TAG, "Información de sesión local limpiada")
    }
    
    /**
     * Cierra la sesión del usuario
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            clearSessionInfo()
            auth.signOut()
            _authState.value = AuthState.NotAuthenticated
            _currentUser.value = null
            Log.d(TAG, "Sesión cerrada exitosamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar sesión", e)
            Result.failure(e)
        }
    }
    
    /**
     * Valida que el usuario actual tenga acceso a un recurso específico
     */
    fun validateUserAccess(resourceUserId: String): Boolean {
        val currentUserId = getCurrentUserId()
        return currentUserId != null && currentUserId == resourceUserId
    }
    
    /**
     * Obtiene las sesiones activas del usuario actual
     */
    suspend fun getActiveSessions(): Result<List<SessionInfo>> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))
            
            val querySnapshot = firestore.collection(SESSIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val sessions = querySnapshot.documents.mapNotNull { document ->
                try {
                    SessionInfo(
                        userId = document.getString("userId") ?: "",
                        email = document.getString("email") ?: "",
                        deviceId = document.getString("deviceId") ?: "",
                        lastActivity = document.getString("lastActivity") ?: "",
                        isActive = document.getBoolean("isActive") ?: false
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error al parsear información de sesión", e)
                    null
                }
            }
            
            Log.d(TAG, "Sesiones activas obtenidas: ${sessions.size}")
            Result.success(sessions)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener sesiones activas", e)
            Result.failure(e)
        }
    }
    
    /**
     * Revoca una sesión específica
     */
    suspend fun revokeSession(deviceId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("Usuario no autenticado"))
            
            firestore.collection(SESSIONS_COLLECTION)
                .document("${userId}_$deviceId")
                .update("isActive", false, "revokedTime", getCurrentTimestamp())
                .await()
            
            Log.d(TAG, "Sesión revocada para dispositivo: $deviceId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al revocar sesión", e)
            Result.failure(e)
        }
    }
    
    /**
     * Genera un ID único para el dispositivo
     */
    private fun getDeviceId(): String {
        val savedDeviceId = prefs.getString("device_id", null)
        return savedDeviceId ?: run {
            val newDeviceId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newDeviceId).apply()
            newDeviceId
        }
    }
    
    /**
     * Genera un token de sesión único
     */
    private fun generateSessionToken(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Obtiene el timestamp actual formateado
     */
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }
    
    /**
     * Verifica la validez de la sesión actual
     */
    suspend fun validateCurrentSession(): Result<Boolean> {
        return try {
            val user = getCurrentUser() ?: return Result.success(false)
            val deviceId = getDeviceId()
            
            val document = firestore.collection(SESSIONS_COLLECTION)
                .document("${user.uid}_$deviceId")
                .get()
                .await()
            
            val isValid = document.exists() && document.getBoolean("isActive") == true
            Log.d(TAG, "Validación de sesión: $isValid")
            Result.success(isValid)
        } catch (e: Exception) {
            Log.e(TAG, "Error al validar sesión", e)
            Result.failure(e)
        }
    }
}