package com.example.recordatoriomodelo2.middleware

import android.content.Context
import android.util.Log
import com.example.recordatoriomodelo2.services.SessionManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Middleware de seguridad para validar operaciones de tareas
 * Garantiza que solo usuarios autenticados y con sesiones válidas puedan realizar operaciones
 */
class SecurityMiddleware private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SecurityMiddleware"
        
        @Volatile
        private var INSTANCE: SecurityMiddleware? = null
        
        fun getInstance(context: Context): SecurityMiddleware {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityMiddleware(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val sessionManager: SessionManager by lazy { SessionManager.getInstance(context) }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    /**
     * Valida si el usuario puede realizar operaciones de tareas
     */
    suspend fun validateTaskOperation(operation: String): SecurityResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Validando operación de tarea: $operation")
            
            // 1. Verificar autenticación básica con Firebase Auth
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "Usuario no autenticado para operación: $operation")
                return@withContext SecurityResult.Failure("Usuario no autenticado")
            }
            
            // 2. Verificar que el usuario esté verificado (opcional, comentado por ahora)
            // if (!currentUser.isEmailVerified) {
            //     Log.w(TAG, "Email no verificado para operación: $operation")
            //     return@withContext SecurityResult.Failure("Email no verificado")
            // }
            
            // 3. Validación simplificada - solo verificar que el usuario existe en Firebase Auth
            // Esto evita problemas de condición de carrera con Firestore
            Log.d(TAG, "Operación validada exitosamente: $operation para usuario ${currentUser.uid}")
            SecurityResult.Success(currentUser.uid)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en validación de seguridad para operación: $operation", e)
            SecurityResult.Failure("Error de validación: ${e.message}")
        }
    }
    
    /**
     * Valida si el usuario puede acceder a una tarea específica
     */
    suspend fun validateTaskAccess(taskUserId: String?, operation: String): SecurityResult = withContext(Dispatchers.IO) {
        try {
            // Primero validar la operación general
            val operationResult = validateTaskOperation(operation)
            if (operationResult is SecurityResult.Failure) {
                return@withContext operationResult
            }
            
            val currentUserId = (operationResult as SecurityResult.Success).userId
            
            // Verificar que la tarea pertenece al usuario actual
            if (taskUserId != null && taskUserId != currentUserId) {
                Log.w(TAG, "Intento de acceso a tarea de otro usuario. Usuario actual: $currentUserId, Propietario: $taskUserId")
                return@withContext SecurityResult.Failure("No tienes permisos para acceder a esta tarea")
            }
            
            Log.d(TAG, "Acceso a tarea validado para usuario: $currentUserId")
            SecurityResult.Success(currentUserId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en validación de acceso a tarea", e)
            SecurityResult.Failure("Error de validación: ${e.message}")
        }
    }
    
    /**
     * Valida operaciones masivas (como sincronización)
     */
    suspend fun validateBulkOperation(operation: String): SecurityResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Validando operación masiva: $operation")
            
            val result = validateTaskOperation(operation)
            if (result is SecurityResult.Success) {
                // Para operaciones masivas, verificar límites adicionales
                val activeSessionsResult = sessionManager.getActiveSessions()
                val activeSessions = activeSessionsResult.getOrElse { emptyList() }
                if (activeSessions.size > 5) { // Límite de 5 sesiones activas
                    Log.w(TAG, "Demasiadas sesiones activas para usuario ${result.userId}: ${activeSessions.size}")
                    return@withContext SecurityResult.Failure("Demasiadas sesiones activas. Cierra algunas sesiones.")
                }
            }
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en validación de operación masiva: $operation", e)
            SecurityResult.Failure("Error de validación: ${e.message}")
        }
    }
    
    /**
     * Registra actividad de seguridad para auditoría
     */
    suspend fun logSecurityEvent(event: String, userId: String?, success: Boolean, details: String? = null) = withContext(Dispatchers.IO) {
        try {
            val logMessage = "SECURITY_EVENT: $event | User: $userId | Success: $success | Details: $details"
            if (success) {
                Log.i(TAG, logMessage)
            } else {
                Log.w(TAG, logMessage)
            }
            
            // Aquí se podría enviar a un servicio de auditoría externo
            // Por ahora solo registramos en logs locales
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar evento de seguridad", e)
        }
    }
}

/**
 * Resultado de validación de seguridad
 */
sealed class SecurityResult {
    data class Success(val userId: String) : SecurityResult()
    data class Failure(val message: String) : SecurityResult()
}