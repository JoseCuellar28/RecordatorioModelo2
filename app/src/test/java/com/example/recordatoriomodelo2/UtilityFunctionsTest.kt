package com.example.recordatoriomodelo2

import org.junit.Test
import org.junit.Assert.*

/**
 * Simple unit tests for utility functions and basic operations
 */
class UtilityFunctionsTest {
    
    @Test
    fun string_isValidEmail_isCorrect() {
        val validEmail = "test@example.com"
        val invalidEmail = "invalid-email"
        val emptyEmail = ""
        
        assertTrue(isValidEmail(validEmail))
        assertFalse(isValidEmail(invalidEmail))
        assertFalse(isValidEmail(emptyEmail))
    }
    
    @Test
    fun string_isNotEmpty_isCorrect() {
        val nonEmptyString = "Hello"
        val emptyString = ""
        val whitespaceString = "   "
        
        assertTrue(isNotEmpty(nonEmptyString))
        assertFalse(isNotEmpty(emptyString))
        assertTrue(isNotEmpty(whitespaceString)) // Contains whitespace, so not empty
    }
    
    @Test
    fun string_trimAndValidate_isCorrect() {
        val stringWithSpaces = "  Hello World  "
        val emptyStringWithSpaces = "    "
        
        assertEquals("Hello World", trimAndValidate(stringWithSpaces))
        assertEquals("", trimAndValidate(emptyStringWithSpaces))
    }
    
    @Test
    fun date_isValidDateFormat_isCorrect() {
        val validDate = "2024-01-15"
        val invalidDate = "15-01-2024"
        val emptyDate = ""
        
        assertTrue(isValidDateFormat(validDate))
        assertFalse(isValidDateFormat(invalidDate))
        assertFalse(isValidDateFormat(emptyDate))
    }
    
    @Test
    fun list_isNotEmpty_isCorrect() {
        val nonEmptyList = listOf("item1", "item2")
        val emptyList = emptyList<String>()
        
        assertTrue(nonEmptyList.isNotEmpty())
        assertFalse(emptyList.isNotEmpty())
        assertEquals(2, nonEmptyList.size)
        assertEquals(0, emptyList.size)
    }
    
    @Test
    fun number_isPositive_isCorrect() {
        val positiveNumber = 5
        val negativeNumber = -3
        val zero = 0
        
        assertTrue(isPositive(positiveNumber))
        assertFalse(isPositive(negativeNumber))
        assertFalse(isPositive(zero))
    }
    
    // Helper functions for testing
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.length > 5
    }
    
    private fun isNotEmpty(text: String): Boolean {
        return text.isNotEmpty()
    }
    
    private fun trimAndValidate(text: String): String {
        return text.trim()
    }
    
    private fun isValidDateFormat(date: String): Boolean {
        return date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
    }
    
    private fun isPositive(number: Int): Boolean {
        return number > 0
    }
}