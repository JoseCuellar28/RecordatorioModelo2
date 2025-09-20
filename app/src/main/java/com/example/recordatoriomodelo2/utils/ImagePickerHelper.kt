package com.example.recordatoriomodelo2.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ImagePickerHelper(
    private val context: Context,
    private val onImageSelected: (Uri?) -> Unit,
    private val onError: (String) -> Unit
) {
    private var tempImageUri: Uri? = null
    
    // Launcher para seleccionar imagen de galería
    private lateinit var galleryLauncher: ManagedActivityResultLauncher<String, Uri?>
    
    // Launcher para tomar foto con cámara
    private lateinit var cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>
    
    // Launcher para permisos
    private lateinit var permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
    
    fun initializeLaunchers(
        galleryLauncher: ManagedActivityResultLauncher<String, Uri?>,
        cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
        permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
    ) {
        this.galleryLauncher = galleryLauncher
        this.cameraLauncher = cameraLauncher
        this.permissionLauncher = permissionLauncher
    }
    
    fun selectFromGallery() {
        if (hasStoragePermission()) {
            galleryLauncher.launch("image/*")
        } else {
            requestStoragePermission()
        }
    }
    
    fun takePhoto() {
        if (hasCameraPermission()) {
            val imageUri = createImageUri()
            if (imageUri != null) {
                tempImageUri = imageUri
                cameraLauncher.launch(imageUri)
            } else {
                onError("Error al crear archivo temporal para la foto")
            }
        } else {
            requestCameraPermission()
        }
    }
    
    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }
    
    private fun requestCameraPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }
    
    private fun createImageUri(): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = File(context.cacheDir, "images")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            
            val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    fun handleGalleryResult(uri: Uri?) {
        if (uri != null) {
            val compressedUri = compressImage(uri)
            onImageSelected(compressedUri)
        } else {
            onError("No se seleccionó ninguna imagen")
        }
    }
    
    fun handleCameraResult(success: Boolean) {
        if (success && tempImageUri != null) {
            val compressedUri = compressImage(tempImageUri!!)
            onImageSelected(compressedUri)
        } else {
            onError("Error al tomar la foto")
        }
        tempImageUri = null
    }
    
    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            onError("Se necesitan permisos para acceder a las imágenes")
        }
    }
    
    private fun compressImage(uri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                // Corregir orientación
                val correctedBitmap = correctImageOrientation(uri, bitmap)
                
                // Redimensionar si es muy grande
                val resizedBitmap = resizeBitmap(correctedBitmap, 1024, 1024)
                
                // Comprimir
                val compressedFile = createCompressedFile(resizedBitmap)
                if (compressedFile != null) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        compressedFile
                    )
                } else {
                    uri
                }
            } else {
                uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uri
        }
    }
    
    private fun correctImageOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            
            if (!matrix.isIdentity) {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }
    
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun createCompressedFile(bitmap: Bitmap): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "compressed_${timeStamp}.jpg"
            val file = File(context.cacheDir, fileName)
            
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Método para mostrar opciones de selección de imagen
    // Por simplicidad, usaremos galería por defecto
    // En una implementación completa, se podría mostrar un diálogo
    fun showImagePicker() {
        selectFromGallery()
    }
}

@Composable
fun rememberImagePickerHelper(
    onImageSelected: (Uri?) -> Unit,
    onError: (String) -> Unit
): ImagePickerHelper {
    val context = LocalContext.current
    
    val imagePickerHelper = remember {
        ImagePickerHelper(context, onImageSelected, onError)
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imagePickerHelper.handleGalleryResult(uri)
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        imagePickerHelper.handleCameraResult(success)
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        imagePickerHelper.handlePermissionResult(permissions)
    }
    
    LaunchedEffect(Unit) {
        imagePickerHelper.initializeLaunchers(galleryLauncher, cameraLauncher, permissionLauncher)
    }
    
    return imagePickerHelper
}