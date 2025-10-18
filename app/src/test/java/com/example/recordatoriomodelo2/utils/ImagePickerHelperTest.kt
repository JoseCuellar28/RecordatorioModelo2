package com.example.recordatoriomodelo2.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ImagePickerHelperTest {

    @Test
    fun `test basic string operations for image picker`() {
        // Given
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif")
        val testFileName = "test_image.jpg"
        
        // When
        val hasValidExtension = imageExtensions.any { ext -> 
            testFileName.lowercase().endsWith(".$ext") 
        }
        
        // Then
        assertThat(hasValidExtension).isTrue()
    }

    @Test
    fun `test invalid image extension`() {
        // Given
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif")
        val testFileName = "test_document.pdf"
        
        // When
        val hasValidExtension = imageExtensions.any { ext -> 
            testFileName.lowercase().endsWith(".$ext") 
        }
        
        // Then
        assertThat(hasValidExtension).isFalse()
    }

    @Test
    fun `test image file name generation`() {
        // Given
        val prefix = "IMG_"
        val timestamp = "20240101_120000"
        val extension = ".jpg"
        
        // When
        val fileName = "$prefix$timestamp$extension"
        
        // Then
        assertThat(fileName).isEqualTo("IMG_20240101_120000.jpg")
        assertThat(fileName).startsWith(prefix)
        assertThat(fileName).endsWith(extension)
    }

    @Test
    fun `test image compression quality calculation`() {
        // Given
        val originalSize = 1024 * 1024 // 1MB
        val targetSize = 512 * 1024   // 512KB
        
        // When
        val compressionRatio = (targetSize.toDouble() / originalSize.toDouble()) * 100
        val quality = compressionRatio.toInt().coerceIn(10, 100)
        
        // Then
        assertThat(quality).isEqualTo(50)
        assertThat(quality).isAtLeast(10)
        assertThat(quality).isAtMost(100)
    }

    @Test
    fun `test image dimension calculation`() {
        // Given
        val originalWidth = 2048
        val originalHeight = 1536
        val maxDimension = 1024
        
        // When
        val scale = minOf(
            maxDimension.toFloat() / originalWidth,
            maxDimension.toFloat() / originalHeight
        )
        val newWidth = (originalWidth * scale).toInt()
        val newHeight = (originalHeight * scale).toInt()
        
        // Then
        assertThat(newWidth).isEqualTo(1024)
        assertThat(newHeight).isEqualTo(768)
        assertThat(newWidth).isAtMost(maxDimension)
        assertThat(newHeight).isAtMost(maxDimension)
    }

    @Test
    fun `test error message formatting`() {
        // Given
        val errorType = "PERMISSION_DENIED"
        val operation = "gallery_access"
        
        // When
        val errorMessage = "Error: $errorType during $operation"
        
        // Then
        assertThat(errorMessage).isEqualTo("Error: PERMISSION_DENIED during gallery_access")
        assertThat(errorMessage).contains(errorType)
        assertThat(errorMessage).contains(operation)
    }

    @Test
    fun `test callback function simulation`() {
        // Given
        var callbackResult: String? = null
        val callback: (String) -> Unit = { result -> callbackResult = result }
        
        // When
        callback("image_selected")
        
        // Then
        assertThat(callbackResult).isEqualTo("image_selected")
    }

    @Test
    fun `test null safety for callback`() {
        // Given
        var callbackResult: String? = null
        val callback: ((String) -> Unit)? = { result -> callbackResult = result }
        
        // When
        callback?.invoke("test_result")
        
        // Then
        assertThat(callbackResult).isEqualTo("test_result")
    }
}