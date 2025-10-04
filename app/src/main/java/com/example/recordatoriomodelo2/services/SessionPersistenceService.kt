package com.example.recordatoriomodelo2.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * Servicio de persistencia de sesión que maneja el auto-login seguro
 * y la persistencia de sesiones entre dispositivos
 */
class SessionPersistenceService private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SessionPersistence"
        private const val PREFS_NAME = "secure_session_prefs"
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LAST_LOGIN = "last_login"
        private const val KEY_AUTO_LOGIN_ENABLED = "auto_login_enabled"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_SESSION_EXPIRY = "session_expiry"
        private const val SESSION_DURATION_MS = 30L * 24 * 60 * 60 * 1000 // 30 días
        
        @Volatile
        private var INSTANCE: SessionPersistenceService? = null
        
        fun getInstance(context: Context): SessionPersistenceService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionPersistenceService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    private val encryptedPrefs: SharedPreferences by lazy {
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
    
    /**
     * Configura la persistencia de sesión después de un login exitoso
     */
    suspend fun configureSessionPersistence(userId: String, enableAutoLogin: Boolean = true): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Configurando persistencia de sesión para usuario: $userId")
            
            // Generar token de sesión seguro
            val sessionToken = generateSecureSessionToken()
            val deviceId = getOrCreateDeviceId()
            val currentTime = System.currentTimeMillis()
            val expiryTime = currentTime + SESSION_DURATION_MS
            
            // Guardar en SharedPreferences encriptadas
            encryptedPrefs.edit().apply {
                putString(KEY_SESSION_TOKEN, sessionToken)
                putString(KEY_USER_ID, userId)
                putLong(KEY_LAST_LOGIN, currentTime)
                putBoolean(KEY_AUTO_LOGIN_ENABLED, enableAutoLogin)
                putString(KEY_DEVICE_ID, deviceId)
                putLong(KEY_SESSION_EXPIRY, expiryTime)
                apply()
            }
            
            // Registrar sesión en Firestore
            val sessionData = mapOf(
                "userId" to userId,
                "deviceId" to deviceId,
                "sessionToken" to sessionToken,
                "createdAt" to currentTime,
                "expiresAt" to expiryTime,
                "isActive" to true,
                "lastActivity" to currentTime,
                "deviceInfo" to getDeviceInfo()
            )
            
            firestore.collection("sessions")
                .document("${userId}_${deviceId}")
                .set(sessionData)
                .await()
            
            Log.d(TAG, "Persistencia de sesión configurada exitosamente")
            Result.success(sessionToken)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando persistencia de sesión", e)
            Result.failure(e)
        }
    }
    
    /**
     * Intenta realizar auto-login usando la sesión persistida
     */
    suspend fun attemptAutoLogin(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== INICIANDO ATTEMPT AUTO-LOGIN ===")
            
            // Verificar si auto-login está habilitado
            val autoLoginEnabled = encryptedPrefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, false)
            Log.d(TAG, "Auto-login habilitado: $autoLoginEnabled")
            
            if (!autoLoginEnabled) {
                Log.d(TAG, "Auto-login deshabilitado - retornando null")
                return@withContext Result.success(null)
            }
            
            // Obtener datos de sesión
            val userId = encryptedPrefs.getString(KEY_USER_ID, null)
            val sessionToken = encryptedPrefs.getString(KEY_SESSION_TOKEN, null)
            val deviceId = encryptedPrefs.getString(KEY_DEVICE_ID, null)
            val expiryTime = encryptedPrefs.getLong(KEY_SESSION_EXPIRY, 0)
            
            if (userId == null || sessionToken == null || deviceId == null) {
                Log.d(TAG, "Datos de sesión incompletos")
                return@withContext Result.success(null)
            }
            
            // Verificar expiración
            if (System.currentTimeMillis() > expiryTime) {
                Log.d(TAG, "Sesión expirada")
                clearSessionData()
                return@withContext Result.success(null)
            }
            
            // Validar sesión en Firestore
            val sessionDoc = firestore.collection("sessions")
                .document("${userId}_${deviceId}")
                .get()
                .await()
            
            if (!sessionDoc.exists()) {
                Log.d(TAG, "Sesión no encontrada en Firestore")
                clearSessionData()
                return@withContext Result.success(null)
            }
            
            val sessionData = sessionDoc.data
            val storedToken = sessionData?.get("sessionToken") as? String
            val isActive = sessionData?.get("isActive") as? Boolean ?: false
            
            if (storedToken != sessionToken || !isActive) {
                Log.d(TAG, "Token de sesión inválido o sesión inactiva")
                clearSessionData()
                return@withContext Result.success(null)
            }
            
            // Actualizar última actividad
            updateLastActivity(userId, deviceId)
            
            Log.d(TAG, "Auto-login exitoso para usuario: $userId")
            Result.success(userId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en auto-login", e)
            clearSessionData()
            Result.failure(e)
        }
    }
    
    /**
     * Actualiza la última actividad de la sesión
     */
    suspend fun updateLastActivity(userId: String, deviceId: String) = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Actualizar en SharedPreferences
            encryptedPrefs.edit()
                .putLong(KEY_LAST_LOGIN, currentTime)
                .apply()
            
            // Actualizar en Firestore
            firestore.collection("sessions")
                .document("${userId}_${deviceId}")
                .update("lastActivity", currentTime)
                .await()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando última actividad", e)
        }
    }
    
    /**
     * Limpia todos los datos de sesión
     */
    suspend fun clearSessionData() = withContext(Dispatchers.IO) {
        try {
            val userId = encryptedPrefs.getString(KEY_USER_ID, null)
            val deviceId = encryptedPrefs.getString(KEY_DEVICE_ID, null)
            
            // Limpiar SharedPreferences
            encryptedPrefs.edit().clear().apply()
            
            // Marcar sesión como inactiva en Firestore
            if (userId != null && deviceId != null) {
                firestore.collection("sessions")
                    .document("${userId}_${deviceId}")
                    .update("isActive", false)
                    .await()
            }
            
            Log.d(TAG, "Datos de sesión limpiados")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando datos de sesión", e)
        }
    }
    
    /**
     * Deshabilita el auto-login
     */
    fun disableAutoLogin() {
        encryptedPrefs.edit()
            .putBoolean(KEY_AUTO_LOGIN_ENABLED, false)
            .apply()
        Log.d(TAG, "Auto-login deshabilitado")
    }
    
    /**
     * Habilita el auto-login
     */
    fun enableAutoLogin() {
        encryptedPrefs.edit()
            .putBoolean(KEY_AUTO_LOGIN_ENABLED, true)
            .apply()
        Log.d(TAG, "Auto-login habilitado")
    }
    
    /**
     * Verifica si el auto-login está habilitado
     */
    fun isAutoLoginEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, false)
    }
    
    /**
     * Obtiene información de la sesión actual
     */
    fun getCurrentSessionInfo(): SessionInfo? {
        val userId = encryptedPrefs.getString(KEY_USER_ID, null)
        val deviceId = encryptedPrefs.getString(KEY_DEVICE_ID, null)
        val lastLogin = encryptedPrefs.getLong(KEY_LAST_LOGIN, 0)
        val expiryTime = encryptedPrefs.getLong(KEY_SESSION_EXPIRY, 0)
        val autoLoginEnabled = encryptedPrefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, false)
        
        return if (userId != null && deviceId != null) {
            SessionInfo(
                userId = userId,
                deviceId = deviceId,
                lastLogin = lastLogin,
                expiryTime = expiryTime,
                autoLoginEnabled = autoLoginEnabled,
                isExpired = System.currentTimeMillis() > expiryTime
            )
        } else null
    }
    
    /**
     * Genera un token de sesión seguro
     */
    private fun generateSecureSessionToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
    
    /**
     * Obtiene o crea un ID único del dispositivo
     */
    private fun getOrCreateDeviceId(): String {
        val existingId = encryptedPrefs.getString(KEY_DEVICE_ID, null)
        return existingId ?: run {
            val newId = UUID.randomUUID().toString()
            encryptedPrefs.edit()
                .putString(KEY_DEVICE_ID, newId)
                .apply()
            newId
        }
    }
    
    /**
     * Obtiene información básica del dispositivo
     */
    private fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "model" to android.os.Build.MODEL,
            "manufacturer" to android.os.Build.MANUFACTURER,
            "version" to android.os.Build.VERSION.RELEASE,
            "sdk" to android.os.Build.VERSION.SDK_INT.toString()
        )
    }
}

/**
 * Información de sesión
 */
data class SessionInfo(
    val userId: String,
    val deviceId: String,
    val lastLogin: Long,
    val expiryTime: Long,
    val autoLoginEnabled: Boolean,
    val isExpired: Boolean
)