package com.example.recordatoriomodelo2.firebase

import com.example.recordatoriomodelo2.services.EmailService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*
import android.util.Log
import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object FirebaseManager {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // Verificar disponibilidad de Google Play Services
    fun checkGooglePlayServices(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        
        return when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.d("FirebaseManager", "Google Play Services está disponible")
                true
            }
            ConnectionResult.SERVICE_MISSING -> {
                Log.w("FirebaseManager", "Google Play Services no está instalado")
                false
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                Log.w("FirebaseManager", "Google Play Services necesita actualización")
                false
            }
            ConnectionResult.SERVICE_DISABLED -> {
                Log.w("FirebaseManager", "Google Play Services está deshabilitado")
                false
            }
            else -> {
                Log.w("FirebaseManager", "Google Play Services no está disponible: $resultCode")
                false
            }
        }
    }
    
    // Obtener usuario actual
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    // Verificar si el usuario está autenticado
    fun isUserLoggedIn(): Boolean = getCurrentUser() != null
    
    // Registrar usuario con email y contraseña
    suspend fun registerUser(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        institution: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                // Crear perfil del usuario en Firestore
                createUserProfile(user.uid, fullName, email, phone, institution)
                
                // Enviar email de verificación
                user.sendEmailVerification().await()
                
                Result.success(user)
            } else {
                Result.failure(Exception("Error al crear usuario"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "=== ERROR EN registerUserWithTemporaryPassword ===")
            Log.e("FirebaseManager", "Excepción: ${e.message}")
            Log.e("FirebaseManager", "Tipo de excepción: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    // Iniciar sesión con email y contraseña
    suspend fun signInUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d("FirebaseManager", "=== INICIO signInUser ===")
            Log.d("FirebaseManager", "Email: $email")
            
            // Primero intentar autenticación normal con Firebase Auth
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                
                if (user != null) {
                    Log.d("FirebaseManager", "Autenticación exitosa con Firebase Auth")
                    return Result.success(user)
                }
            } catch (authException: Exception) {
                Log.d("FirebaseManager", "Fallo autenticación Firebase Auth: ${authException.message}")
                
                // Si falla la autenticación normal, verificar si es una contraseña temporal
                try {
                    Log.d("FirebaseManager", "Verificando contraseña temporal en Firestore...")
                    
                    // Buscar usuario por email en Firestore
                    val userQuery = firestore.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .await()
                    
                    if (!userQuery.isEmpty) {
                        val userDoc = userQuery.documents.first()
                        val hasTemporaryPassword = userDoc.getBoolean("hasTemporaryPassword") ?: false
                        val temporaryPassword = userDoc.getString("temporaryPassword")
                        
                        Log.d("FirebaseManager", "Usuario encontrado - hasTemporaryPassword: $hasTemporaryPassword")
                        
                        if (hasTemporaryPassword && temporaryPassword == password) {
                            Log.d("FirebaseManager", "Contraseña temporal válida, autenticando con contraseña temporal...")
                            
                            // Autenticar con la contraseña temporal almacenada
                            val tempResult = auth.signInWithEmailAndPassword(email, temporaryPassword).await()
                            val tempUser = tempResult.user
                            
                            if (tempUser != null) {
                                Log.d("FirebaseManager", "Autenticación exitosa con contraseña temporal")
                                return Result.success(tempUser)
                            }
                        } else {
                            Log.d("FirebaseManager", "Contraseña temporal no válida o usuario sin contraseña temporal")
                        }
                    } else {
                        Log.d("FirebaseManager", "Usuario no encontrado en Firestore")
                    }
                } catch (firestoreException: Exception) {
                    Log.e("FirebaseManager", "Error verificando contraseña temporal: ${firestoreException.message}")
                }
                
                // Si llegamos aquí, tanto la autenticación normal como la temporal fallaron
                throw authException
            }
            
            Result.failure(Exception("Error al iniciar sesión"))
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error en signInUser: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Cerrar sesión
    fun signOut() {
        auth.signOut()
    }
    
    // Enviar email para restablecer contraseña
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Reenviar email de verificación
    suspend fun resendVerificationEmail(): Result<Unit> {
        return try {
            val user = getCurrentUser()
            if (user != null) {
                user.sendEmailVerification().await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Usuario no autenticado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Crear perfil del usuario en Firestore
    private suspend fun createUserProfile(
        userId: String,
        fullName: String,
        email: String,
        phone: String,
        institution: String
    ): Result<Unit> {
        return try {
            val userProfile = hashMapOf(
                "fullName" to fullName,
                "email" to email,
                "phone" to phone,
                "institution" to institution,
                "createdAt" to Date(),
                "isEmailVerified" to false,
                "isActive" to true
            )
            
            firestore.collection("users")
                .document(userId)
                .set(userProfile)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Obtener perfil del usuario desde Firestore
    suspend fun getUserProfile(userId: String): Result<Map<String, Any>> {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                Result.success(document.data ?: emptyMap())
            } else {
                Result.failure(Exception("Perfil de usuario no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Actualizar perfil del usuario
    suspend fun updateUserProfile(
        userId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Generar contraseña temporal
    fun generateTemporaryPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }
    
    // Registrar usuario con contraseña temporal
    suspend fun registerUserWithTemporaryPassword(
        email: String,
        fullName: String,
        phone: String,
        institution: String
    ): Result<Pair<FirebaseUser, String>> {
        Log.d("FirebaseManager", "=== INICIO registerUserWithTemporaryPassword ===")
        Log.d("FirebaseManager", "Parámetros: email=$email, fullName=$fullName, phone=$phone, institution=$institution")
        
        return try {
            val temporaryPassword = generateTemporaryPassword()
            Log.d("FirebaseManager", "Contraseña temporal generada: $temporaryPassword")
            
            Log.d("FirebaseManager", "Creando usuario en Firebase Auth...")
            val result = auth.createUserWithEmailAndPassword(email, temporaryPassword).await()
            val user = result.user
            Log.d("FirebaseManager", "Usuario creado en Firebase Auth: ${user?.uid}")
            
            if (user != null) {
                Log.d("FirebaseManager", "Creando perfil de usuario en Firestore...")
                // Crear perfil con contraseña temporal
                val userProfile = hashMapOf(
                    "fullName" to fullName,
                    "email" to email,
                    "phone" to phone,
                    "institution" to institution,
                    "createdAt" to Date(),
                    "isEmailVerified" to false,
                    "isActive" to true,
                    "hasTemporaryPassword" to true,
                    "temporaryPassword" to temporaryPassword,
                    "isFirstLogin" to true
                )
                
                firestore.collection("users")
                    .document(user.uid)
                    .set(userProfile)
                    .await()
                Log.d("FirebaseManager", "Perfil de usuario creado en Firestore")
                
                // Enviar email de verificación
                Log.d("FirebaseManager", "Enviando email de verificación...")
                user.sendEmailVerification().await()
                Log.d("FirebaseManager", "Email de verificación enviado")
                
                // Enviar correo con contraseña temporal
                Log.d("FirebaseManager", "Enviando correo con contraseña temporal...")
                EmailService.sendTemporaryPasswordEmail(email, fullName, temporaryPassword)
                    .onSuccess {
                        Log.d("FirebaseManager", "Correo con contraseña temporal enviado exitosamente")
                    }
                    .onFailure { emailException ->
                        // Log del error pero no fallar el registro
                        Log.e("FirebaseManager", "Error enviando correo: ${emailException.message}")
                        println("Error enviando correo: ${emailException.message}")
                    }
                
                Log.d("FirebaseManager", "=== REGISTRO COMPLETADO EXITOSAMENTE ===")
                Result.success(Pair(user, temporaryPassword))
            } else {
                Log.e("FirebaseManager", "Error: usuario es null después de la creación")
                Result.failure(Exception("Error al crear usuario"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Cambiar contraseña temporal por una permanente
    suspend fun changeTemporaryPassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
            
            user.updatePassword(newPassword).await()
            
            // Actualizar los campos relacionados con contraseña temporal
            val userRef = firestore.collection("users").document(user.uid)
            val updates = hashMapOf<String, Any>(
                "isFirstLogin" to false,
                "hasTemporaryPassword" to false,
                "temporaryPassword" to ""
            )
            userRef.update(updates).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFirstLogin(): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
            
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            val isFirstLogin = userDoc.getBoolean("isFirstLogin") ?: true
            
            Result.success(isFirstLogin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}