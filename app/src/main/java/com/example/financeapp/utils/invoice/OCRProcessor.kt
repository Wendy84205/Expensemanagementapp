// utils/invoice/OCRProcessor.kt
package com.example.financeapp.utils.invoice

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

class OCRProcessor {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(bitmap: Bitmap): String {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result: Text = textRecognizer.process(inputImage).await()

            if (result.text.isNotEmpty()) {
                result.text
            } else {
                throw Exception("Không tìm thấy văn bản trong ảnh")
            }
        } catch (e: Exception) {
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
            throw e
        }
    }
}