// utils/invoice/OCRProcessor.kt
package com.example.financeapp.utils.invoice

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

class OCRProcessor {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(bitmap: Bitmap): String {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result: Text = textRecognizer.process(inputImage).await()

            if (result.text.isNotEmpty()) {
                Log.d("OCRProcessor", "OCR Result: ${result.text}")
                result.text
            } else {
                throw Exception("Không tìm thấy văn bản trong ảnh")
            }
        } catch (e: Exception) {
            Log.e("OCRProcessor", "OCR Error: ${e.message}")
            throw e
        }
    }

    suspend fun processImageFromUri(uri: Uri, context: android.content.Context): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                processImage(bitmap)
            } else {
                throw Exception("Không thể đọc ảnh từ URI")
            }
        } catch (e: Exception) {
            Log.e("OCRProcessor", "Error processing URI: ${e.message}")
            throw e
        }
    }

    suspend fun processImageFromFile(file: File): String {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                processImage(bitmap)
            } else {
                throw Exception("Không thể đọc ảnh từ file")
            }
        } catch (e: Exception) {
            Log.e("OCRProcessor", "Error processing file: ${e.message}")
            throw e
        }
    }
}