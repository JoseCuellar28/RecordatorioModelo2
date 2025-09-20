package com.example.recordatoriomodelo2.data.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

@Serializable
data class ImgBBResponse(
    val data: ImgBBData? = null,
    val success: Boolean = false,
    val error: ImgBBError? = null
)

@Serializable
data class ImgBBData(
    val id: String = "",
    val title: String = "",
    val url_viewer: String = "",
    val url: String = "",
    val display_url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val size: Int = 0,
    val time: String = "",
    val expiration: String = "",
    val image: ImgBBImage? = null,
    val thumb: ImgBBThumb? = null,
    val medium: ImgBBMedium? = null,
    val delete_url: String = ""
)

@Serializable
data class ImgBBImage(
    val filename: String = "",
    val name: String = "",
    val mime: String = "",
    val extension: String = "",
    val url: String = ""
)

@Serializable
data class ImgBBThumb(
    val filename: String = "",
    val name: String = "",
    val mime: String = "",
    val extension: String = "",
    val url: String = ""
)

@Serializable
data class ImgBBMedium(
    val filename: String = "",
    val name: String = "",
    val mime: String = "",
    val extension: String = "",
    val url: String = ""
)

@Serializable
data class ImgBBError(
    val message: String = "",
    val code: Int = 0
)

class ImgBBService {
    companion object {
        private const val API_KEY = "8023fa076114ff375f9f5d8793366fe3"
        private const val BASE_URL = "https://api.imgbb.com/1/upload"
        private const val TAG = "ImgBBService"
        private const val MAX_IMAGE_SIZE = 200 // píxeles
        private const val JPEG_QUALITY = 80 // calidad de compresión
    }

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Sube una imagen a ImgBB y retorna la URL
     */
    suspend fun uploadImage(imageUri: Uri, context: Context): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Iniciando subida de imagen a ImgBB")
                
                // 1. Comprimir y convertir imagen a Base64
                val base64Image = compressImageToBase64(imageUri, context)
                Log.d(TAG, "Imagen comprimida, tamaño Base64: ${base64Image.length} caracteres")
                
                // 2. Realizar petición a ImgBB
                Log.d(TAG, "=== ENVIANDO PETICIÓN A IMGBB ===")
                Log.d(TAG, "URL: $BASE_URL")
                Log.d(TAG, "API Key: ${API_KEY.take(10)}...")
                
                val response: ImgBBResponse = httpClient.submitForm(
                    url = BASE_URL,
                    formParameters = parameters {
                        append("key", API_KEY)
                        append("image", base64Image)
                        append("name", "profile_${System.currentTimeMillis()}")
                    }
                ).body()
                
                // 3. Procesar respuesta
                Log.d(TAG, "=== RESPUESTA DE IMGBB ===")
                Log.d(TAG, "response.success: ${response.success}")
                Log.d(TAG, "response.data: ${response.data}")
                Log.d(TAG, "response.error: ${response.error}")
                
                if (response.success && response.data != null) {
                    val imageUrl = response.data.display_url
                    Log.d(TAG, "=== IMAGEN SUBIDA EXITOSAMENTE ===")
                    Log.d(TAG, "URL completa: $imageUrl")
                    Log.d(TAG, "URL viewer: ${response.data.url_viewer}")
                    Log.d(TAG, "URL directa: ${response.data.url}")
                    Result.success(imageUrl)
                } else {
                    val errorMessage = response.error?.message ?: "Error desconocido al subir imagen"
                    Log.e(TAG, "=== ERROR EN RESPUESTA DE IMGBB ===")
                    Log.e(TAG, "Error message: $errorMessage")
                    Log.e(TAG, "Error code: ${response.error?.code}")
                    Result.failure(Exception(errorMessage))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error al subir imagen a ImgBB", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Comprime una imagen y la convierte a Base64
     */
    private suspend fun compressImageToBase64(imageUri: Uri, context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Cargar imagen desde URI
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) {
                    throw Exception("No se pudo cargar la imagen")
                }

                // 2. Calcular nuevas dimensiones manteniendo aspecto
                val originalWidth = originalBitmap.width
                val originalHeight = originalBitmap.height
                val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

                val (newWidth, newHeight) = if (aspectRatio > 1) {
                    // Imagen horizontal
                    MAX_IMAGE_SIZE to (MAX_IMAGE_SIZE / aspectRatio).toInt()
                } else {
                    // Imagen vertical o cuadrada
                    (MAX_IMAGE_SIZE * aspectRatio).toInt() to MAX_IMAGE_SIZE
                }

                // 3. Redimensionar imagen
                val scaledBitmap = Bitmap.createScaledBitmap(
                    originalBitmap,
                    newWidth,
                    newHeight,
                    true
                )

                // 4. Comprimir a JPEG
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                val byteArray = outputStream.toByteArray()

                // 5. Convertir a Base64
                val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                // 6. Limpiar memoria
                originalBitmap.recycle()
                scaledBitmap.recycle()
                outputStream.close()

                Log.d(TAG, "Imagen procesada: ${originalWidth}x${originalHeight} -> ${newWidth}x${newHeight}, ${byteArray.size} bytes")

                base64String
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar imagen", e)
                throw Exception("Error al procesar imagen: ${e.message}")
            }
        }
    }

    /**
     * Cierra el cliente HTTP
     */
    fun close() {
        httpClient.close()
    }
}