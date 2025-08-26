package com.asistencia.app.utils

/**
 * Utility class for handling DNI (Peruvian National ID) validation and formatting
 */
object DniUtils {
    
    /**
     * Validates if a string is a valid DNI number (8 digits)
     */
    fun isValidDni(dni: String): Boolean {
        return dni.matches(Regex("^[0-9]{8}$"))
    }
    
    /**
     * Formats a DNI string with proper formatting (e.g., "12345678" -> "12,345,678")
     */
    fun formatDni(dni: String): String {
        return try {
            val digits = dni.replace("[^0-9]".toRegex(), "")
            digits.reversed().chunked(3).joinToString(",").reversed()
        } catch (e: Exception) {
            dni
        }
    }
    
    /**
     * Extracts a DNI number from a scanned barcode or text
     */
    fun extractDniFromBarcode(barcodeText: String): String? {
        // Try to find 8 consecutive digits
        val regex = Regex("\\b\\d{8}\\b")
        return regex.find(barcodeText)?.value
    }
    
    /**
     * Validates if a DNI has a valid verification digit (dígito verificador)
     * This implements the MOD11 algorithm used in Peru
     */
    fun isValidDniWithVerificationDigit(dni: String): Boolean {
        if (dni.length != 9) return false
        
        val numberPart = dni.substring(0, 8)
        val verificationDigit = dni[8]
        
        // Calculate the expected verification digit
        val factors = intArrayOf(3, 2, 7, 6, 5, 4, 3, 2)
        var sum = 0
        
        for (i in 0 until 8) {
            val digit = numberPart[i].toString().toInt()
            sum += digit * factors[i]
        }
        
        val remainder = sum % 11
        val calculatedDigit = when (val result = 11 - remainder) {
            11 -> '0'
            10 -> 'K'
            else -> result.toString()[0]
        }
        
        return verificationDigit.equals(calculatedDigit, ignoreCase = true)
    }
    
    /**
     * Sanitizes a DNI string by removing any non-digit characters
     */
    fun sanitizeDni(dni: String): String {
        return dni.replace("[^0-9]".toRegex(), "")
    }
}
