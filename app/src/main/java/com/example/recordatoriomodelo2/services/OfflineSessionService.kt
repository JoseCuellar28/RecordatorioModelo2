package com.example.recordatoriomodelo2.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Servicio para manejar sesiones offline usando almacenamiento local seguro
 */
class OfflineSessionService private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "OfflineSessionService"
        private const val PREFS_NAME = "offline_session_prefs"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_LAST_LOGIN_TIME = "last_login_time"
        private const val KEY_IS_OFFLINE_SESSION = "is_offline_session"
        private const val KEY_CACHED_TASKS = "cached_tasks"
        
        @Volatile
        private var INSTANCE: OfflineSessionService? = null
        
        fun getInstance(context: Context): OfflineSessionService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfflineSessionService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creando EncryptedSharedPreferences, usando SharedPreferences normal", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    private val gson = Gson()
    
    /**
     * Datos de sesión offline
     */
    data class OfflineSession(
        val userEmail: String,
        val userId: String,
        val userName: String,
        val lastLoginTime: Long,
        val isOfflineSession: Boolean = true
    )
    
    /**
     * Guarda la sesión del usuario para acceso offline
     */
    fun saveUserSession(
        userEmail: String,
        userId: String,
        userName: String
    ) {
        try {
            with(sharedPreferences.edit()) {
                putString(KEY_USER_EMAIL, userEmail)
                putString(KEY_USER_ID, userId)
                putString(KEY_USER_NAME, userName)
                putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
                putBoolean(KEY_IS_OFFLINE_SESSION, false) // Inicialmente online
                apply()
            }
            Log.d(TAG, "Sesión guardada para usuario: $userEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando sesión de usuario", e)
        }
    }
    
    /**
     * Obtiene la sesión offline guardada
     */
    fun getOfflineSession(): OfflineSession? {
        return try {
            val userEmail = sharedPreferences.getString(KEY_USER_EMAIL, null)
            val userId = sharedPreferences.getString(KEY_USER_ID, null)
            val userName = sharedPreferences.getString(KEY_USER_NAME, null)
            val lastLoginTime = sharedPreferences.getLong(KEY_LAST_LOGIN_TIME, 0L)
            
            if (userEmail != null && userId != null && userName != null && lastLoginTime > 0) {
                OfflineSession(
                    userEmail = userEmail,
                    userId = userId,
                    userName = userName,
                    lastLoginTime = lastLoginTime,
                    isOfflineSession = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo sesión offline", e)
            null
        }
    }
    
    /**
     * Verifica si hay una sesión válida guardada
     */
    fun hasValidOfflineSession(): Boolean {
        val session = getOfflineSession()
        if (session == null) return false
        
        // Verificar que la sesión no sea muy antigua (ej: 30 días)
        val thirtyDaysInMillis = 30L * 24L * 60L * 60L * 1000L
        val isSessionRecent = (System.currentTimeMillis() - session.lastLoginTime) < thirtyDaysInMillis
        
        return isSessionRecent
    }
    
    /**
     * Marca la sesión actual como offline
     */
    fun setOfflineMode(isOffline: Boolean) {
        try {
            sharedPreferences.edit()
                .putBoolean(KEY_IS_OFFLINE_SESSION, isOffline)
                .apply()
            Log.d(TAG, "Modo offline establecido: $isOffline")
        } catch (e: Exception) {
            Log.e(TAG, "Error estableciendo modo offline", e)
        }
    }
    
    /**
     * Verifica si la sesión actual está en modo offline
     */
    fun isInOfflineMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_OFFLINE_SESSION, false)
    }
    
    /**
     * Guarda tareas en caché para acceso offline
     */
    fun cacheTasks(tasks: List<Map<String, Any>>) {
        try {
            val tasksJson = gson.toJson(tasks)
            sharedPreferences.edit()
                .putString(KEY_CACHED_TASKS, tasksJson)
                .apply()
            Log.d(TAG, "Tareas guardadas en caché: ${tasks.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando tareas en caché", e)
        }
    }
    
    /**
     * Obtiene las tareas guardadas en caché
     */
    fun getCachedTasks(): List<Map<String, Any>> {
        return try {
            val tasksJson = sharedPreferences.getString(KEY_CACHED_TASKS, null)
            if (tasksJson != null) {
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                gson.fromJson(tasksJson, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo tareas en caché", e)
            emptyList()
        }
    }
    
    /**
     * Limpia la sesión offline
     */
    fun clearOfflineSession() {
        try {
            sharedPreferences.edit().clear().apply()
            Log.d(TAG, "Sesión offline limpiada")
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando sesión offline", e)
        }
    }
    
    /**
     * Actualiza el tiempo de último acceso
     */
    fun updateLastAccessTime() {
        try {
            sharedPreferences.edit()
                .putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando tiempo de último acceso", e)
        }
    }
}