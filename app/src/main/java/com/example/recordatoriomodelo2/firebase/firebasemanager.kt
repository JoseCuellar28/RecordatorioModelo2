package com.example.recordatoriomodelo2.firebase

import com.example.recordatoriomodelo2.services.EmailService
import com.example.recordatoriomodelo2.data.services.ImgBBService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*
import android.util.Log
import android.content.Context
import android.net.Uri
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import androidx.core.graphics.drawable.DrawableCompat
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import com.example.recordatoriomodelo2.R

object FirebaseManager {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val imgBBService: ImgBBService = ImgBBService()
    
    private fun createDefaultImageUri(context: Context): Uri? {
        return try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.default_profile_image)
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                is VectorDrawable -> {
                    val bitmap = Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }
                else -> {
                    DrawableCompat.wrap(drawable!!).let { wrappedDrawable ->
                        val bitmap = Bitmap.createBitmap(
                            wrappedDrawable.intrinsicWidth,
                            wrappedDrawable.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        wrappedDrawable.setBounds(0, 0, canvas.width, canvas.height)
                        wrappedDrawable.draw(canvas)
                        bitmap
                    }
                }
            }
            
            // Guardar bitmap como archivo temporal
            val file = File(context.cacheDir, "default_profile_image.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            // Crear URI usando FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error creando URI de imagen por defecto: ${e.message}")
            null
        }
    }
    
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
    
    // Registrar usuario con contraseña temporal e imagen de perfil
    suspend fun registerUserWithTemporaryPassword(
        email: String,
        fullName: String,
        phone: String,
        institution: String,
        profileImageUri: Uri? = null,
        context: Context? = null
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
                
                // Determinar qué imagen usar (proporcionada o por defecto)
                val imageUriToUpload = profileImageUri ?: (context?.let { createDefaultImageUri(it) })
                
                // Si hay imagen (proporcionada o por defecto), subirla a ImgBB y actualizar Firestore
                if (imageUriToUpload != null && context != null) {
                    Log.d("FirebaseManager", "=== INICIANDO PROCESO DE SUBIDA DE IMAGEN ===")
                    Log.d("FirebaseManager", "imageUriToUpload: $imageUriToUpload")
                    Log.d("FirebaseManager", "profileImageUri original: $profileImageUri")
                    Log.d("FirebaseManager", "usando imagen por defecto: ${profileImageUri == null}")
                    Log.d("FirebaseManager", "context: $context")
                    Log.d("FirebaseManager", "user.uid: ${user.uid}")
                    
                    try {
                        Log.d("FirebaseManager", "Llamando a imgBBService.uploadImage...")
                        val uploadResult = imgBBService.uploadImage(imageUriToUpload, context)
                        Log.d("FirebaseManager", "Resultado de uploadImage: isSuccess=${uploadResult.isSuccess}")
                        
                        if (uploadResult.isSuccess) {
                            val imageUrl = uploadResult.getOrNull()!!
                            Log.d("FirebaseManager", "=== IMAGEN SUBIDA EXITOSAMENTE ===")
                            Log.d("FirebaseManager", "URL generada por ImgBB: $imageUrl")
                            
                            // Actualizar Firestore con la URL de la imagen
                            val imageUpdates = hashMapOf<String, Any>(
                                "profileImageUrl" to imageUrl,
                                "hasProfileImage" to true,
                                "updatedAt" to Date()
                            )
                            Log.d("FirebaseManager", "Actualizando Firestore con datos: $imageUpdates")
                            
                            firestore.collection("users")
                                .document(user.uid)
                                .update(imageUpdates)
                                .await()
                            Log.d("FirebaseManager", "=== URL DE IMAGEN ACTUALIZADA EN FIRESTORE ===")
                            Log.d("FirebaseManager", "Documento actualizado: users/${user.uid}")
                        } else {
                            val error = uploadResult.exceptionOrNull()
                            Log.e("FirebaseManager", "=== ERROR AL SUBIR IMAGEN ===")
                            Log.e("FirebaseManager", "Error: ${error?.message}")
                            Log.e("FirebaseManager", "Stack trace: ${error?.stackTraceToString()}")
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseManager", "=== ERROR INESPERADO AL SUBIR IMAGEN ===")
                        Log.e("FirebaseManager", "Error: ${e.message}")
                        Log.e("FirebaseManager", "Stack trace: ${e.stackTraceToString()}")
                        // No fallar el registro por error de imagen
                    }
                } else {
                    Log.d("FirebaseManager", "=== NO HAY IMAGEN PARA SUBIR ===")
                    Log.d("FirebaseManager", "profileImageUri es null: ${profileImageUri == null}")
                    Log.d("FirebaseManager", "context es null: ${context == null}")
                }
                
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
    
    // ===== MÉTODOS PARA MANEJO DE IMÁGENES DE PERFIL CON IMGBB =====
    
    /**
     * Sube una imagen de perfil a ImgBB
     * @param imageUri URI de la imagen a subir
     * @param context Contexto de la aplicación
     * @param userId ID del usuario (opcional, usa el usuario actual si no se proporciona)
     * @return Result con la URL de la imagen en ImgBB
     */
    suspend fun uploadProfileImage(imageUri: Uri, context: Context, userId: String? = null): Result<String> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
            val targetUserId = userId ?: user.uid
            
            Log.d("FirebaseManager", "=== INICIO uploadProfileImage con ImgBB ===")
            Log.d("FirebaseManager", "Usuario ID: $targetUserId")
            Log.d("FirebaseManager", "URI de imagen: $imageUri")
            
            // Subir imagen a ImgBB
            val uploadResult = imgBBService.uploadImage(imageUri, context)
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Error al subir imagen a ImgBB"))
            }
            
            val imageUrl = uploadResult.getOrNull()!!
            Log.d("FirebaseManager", "Imagen subida exitosamente a ImgBB: $imageUrl")
            
            Result.success(imageUrl)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error al subir imagen de perfil", e)
            Result.failure(e)
        }
    }
    
    /**
     * Actualiza la URL de la imagen de perfil en Firestore
     * @param imageUrl URL de la imagen en ImgBB
     * @param userId ID del usuario (opcional, usa el usuario actual si no se proporciona)
     * @return Result indicando éxito o fallo
     */
    suspend fun updateProfileImageUrl(imageUrl: String, userId: String? = null): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
            val targetUserId = userId ?: user.uid
            
            Log.d("FirebaseManager", "=== INICIO updateProfileImageUrl ===")
            Log.d("FirebaseManager", "Usuario ID: $targetUserId")
            Log.d("FirebaseManager", "URL de imagen: $imageUrl")
            
            val userRef = firestore.collection("users").document(targetUserId)
            val updates = hashMapOf<String, Any>(
                "profileImageUrl" to imageUrl,
                "hasProfileImage" to true,
                "updatedAt" to Date()
            )
            
            userRef.update(updates).await()
            Log.d("FirebaseManager", "URL de imagen actualizada en Firestore")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error al actualizar URL de imagen en Firestore", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sube una imagen de perfil a ImgBB y actualiza la URL en Firestore en una sola operación
     * @param imageUri URI de la imagen a subir
     * @param context Contexto de la aplicación
     * @param userId ID del usuario (opcional, usa el usuario actual si no se proporciona)
     * @return Result con la URL de la imagen en ImgBB
     */
    suspend fun uploadAndSetProfileImage(imageUri: Uri, context: Context, userId: String? = null): Result<String> {
        return try {
            Log.d("FirebaseManager", "=== INICIO uploadAndSetProfileImage con ImgBB ===")
            
            // Subir imagen a ImgBB
            val uploadResult = uploadProfileImage(imageUri, context, userId)
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Error al subir imagen"))
            }
            
            val imageUrl = uploadResult.getOrNull()!!
            
            // Actualizar URL en Firestore
            val updateResult = updateProfileImageUrl(imageUrl, userId)
            if (updateResult.isFailure) {
                return Result.failure(updateResult.exceptionOrNull() ?: Exception("Error al actualizar URL"))
            }
            
            Log.d("FirebaseManager", "Imagen de perfil subida a ImgBB y actualizada en Firestore exitosamente")
            Result.success(imageUrl)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error en uploadAndSetProfileImage", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene la URL de la imagen de perfil del usuario
     * @param userId ID del usuario (opcional, usa el usuario actual si no se proporciona)
     * @return Result con la URL de la imagen o null si no tiene
     */
    suspend fun getProfileImageUrl(userId: String? = null): Result<String?> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
            val targetUserId = userId ?: user.uid
            
            val userDoc = firestore.collection("users").document(targetUserId).get().await()
            val imageUrl = userDoc.getString("profileImageUrl")
            
            Result.success(imageUrl)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error al obtener URL de imagen de perfil", e)
            Result.failure(e)
        }
    }
    
    /**
     * Elimina la imagen de perfil del usuario (solo actualiza Firestore, ImgBB mantiene la imagen)
     * @param userId ID del usuario (opcional, usa el usuario actual si no se proporciona)
     * @return Result indicando éxito o fallo
     */
    suspend fun deleteProfileImage(userId: String? = null): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))
            val targetUserId = userId ?: user.uid
            
            Log.d("FirebaseManager", "=== INICIO deleteProfileImage ===")
            Log.d("FirebaseManager", "Usuario ID: $targetUserId")
            
            // Actualizar Firestore (ImgBB no permite eliminar imágenes con API gratuita)
            val userRef = firestore.collection("users").document(targetUserId)
            val updates = hashMapOf<String, Any>(
                "profileImageUrl" to "",
                "hasProfileImage" to false,
                "updatedAt" to Date()
            )
            userRef.update(updates).await()
            
            Log.d("FirebaseManager", "Imagen de perfil eliminada del perfil de usuario")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error al eliminar imagen de perfil", e)
            Result.failure(e)
        }
    }
}