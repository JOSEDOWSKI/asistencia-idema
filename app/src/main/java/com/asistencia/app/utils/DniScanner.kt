package com.asistencia.app.utils

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.io.IOException

class DniScanner(private val activity: AppCompatActivity) {
    private val multiFormatReader = MultiFormatReader()
    private var onDniScanned: ((String) -> Unit)? = null
    
    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScan()
        } else {
            onDniScanned?.invoke("") // Return empty string if permission denied
        }
    }
    
    private val takePictureLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? android.graphics.Bitmap
            imageBitmap?.let { processImage(it) }
        } else {
            onDniScanned?.invoke("") // Return empty string if no image captured
        }
    }
    
    fun scanDni(onResult: (String) -> Unit) {
        onDniScanned = onResult
        
        // Check camera permission
        when {
            ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startScan()
            }
            activity.shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                // Show explanation dialog
                android.app.AlertDialog.Builder(activity)
                    .setTitle("Permiso de cámara requerido")
                    .setMessage("Necesitamos acceso a la cámara para escanear el DNI")
                    .setPositiveButton("Aceptar") { _, _ ->
                        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun startScan() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
            takePictureLauncher.launch(takePictureIntent)
        } else {
            onDniScanned?.invoke("") // Return empty string if no camera app found
        }
    }
    
    private fun processImage(bitmap: android.graphics.Bitmap) {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            
            try {
                val result = multiFormatReader.decode(binaryBitmap)
                result.text?.let { dni ->
                    onDniScanned?.invoke(dni)
                    return
                }
            } catch (e: Exception) {
                // No se pudo decodificar el código de barras
            }
            
            onDniScanned?.invoke("") // No se encontró ningún código de barras
        } catch (e: Exception) {
            e.printStackTrace()
            onDniScanned?.invoke("") // Error al procesar la imagen
        }
    }
    
    private fun extractDni(text: String): String {
        // Try to find 8 consecutive digits (Peruvian DNI format)
        val regex = "\\b\\d{8}\\b".toRegex()
        return regex.find(text)?.value ?: ""
    }
    
    fun onDestroy() {
        // No es necesario limpiar nada para ZXing
    }
}
