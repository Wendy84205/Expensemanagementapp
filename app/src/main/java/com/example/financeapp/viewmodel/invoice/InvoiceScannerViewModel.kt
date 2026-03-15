// viewmodel/invoice/InvoiceScannerViewModel.kt
package com.example.financeapp.viewmodel.invoice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeapp.data.models.Transaction
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService


class InvoiceScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(InvoiceScannerState())
    val state: StateFlow<InvoiceScannerState> = _state.asStateFlow()

    private var currentProcessingJob: Job? = null
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Capture image from camera
     */
    fun captureImage(
        imageCapture: ImageCapture?,
        context: Context,
        executor: ExecutorService
    ) {
        if (imageCapture == null) {
            _state.update { it.copy(error = "Camera chưa sẵn sàng") }
            return
        }

        _state.update { it.copy(isProcessing = true, progressStep = 0) }

        val outputDirectory = getOutputDirectory(context)
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    processImageFromFile(photoFile, context, isFromCamera = true)
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModelScope.launch {
                        _state.update {
                            it.copy(
                                isProcessing = false,
                                error = "Lỗi khi chụp ảnh: ${exception.message ?: "Không xác định"}"
                            )
                        }
                    }
                }
            }
        )
    }

    /**
     * Select and process image from gallery
     */
    fun selectFromGallery(context: Context, uri: Uri) {
        currentProcessingJob?.cancel()

        currentProcessingJob = viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progressStep = 0) }

            try {
                // Step 1: Loading image
                _state.update { it.copy(progressStep = 1) }
                delay(500)

                // Load image from URI
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = "Không thể đọc ảnh",
                            progressStep = 0
                        )
                    }
                    return@launch
                }

                // Step 2: Processing image
                _state.update { it.copy(progressStep = 2) }
                delay(500)

                // Perform OCR
                val ocrText = performOCR(bitmap)

                if (ocrText.isBlank()) {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = "Không tìm thấy văn bản trong ảnh",
                            progressStep = 0
                        )
                    }
                    return@launch
                }

                // Step 3: Parsing invoice
                _state.update { it.copy(progressStep = 3) }
                delay(500)

                // Parse invoice data
                val transaction = parseInvoiceData(ocrText)

                _state.update {
                    it.copy(
                        isProcessing = false,
                        scannedTransaction = transaction,
                        confidenceScore = calculateConfidence(ocrText, transaction),
                        rawOcrText = ocrText,
                        progressStep = 0
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        error = "Lỗi khi xử lý ảnh: ${e.message ?: "Không xác định"}",
                        progressStep = 0
                    )
                }
            }
        }
    }

    /**
     * Process image from file (for camera capture)
     */
    private fun processImageFromFile(file: File, context: Context, isFromCamera: Boolean) {
        currentProcessingJob?.cancel()

        currentProcessingJob = viewModelScope.launch {
            try {
                // Step 1: Loading image
                _state.update { it.copy(progressStep = 1) }
                delay(500)

                // Load image from file
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                if (bitmap == null) {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = "Không thể đọc ảnh",
                            progressStep = 0
                        )
                    }
                    return@launch
                }

                // Step 2: Processing image
                _state.update { it.copy(progressStep = 2) }
                delay(500)

                // Perform OCR
                val ocrText = performOCR(bitmap)

                if (ocrText.isBlank()) {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = "Không tìm thấy văn bản trong ảnh",
                            progressStep = 0
                        )
                    }
                    return@launch
                }

                // Step 3: Parsing invoice
                _state.update { it.copy(progressStep = 3) }
                delay(500)

                // Parse invoice data
                val transaction = parseInvoiceData(ocrText)

                _state.update {
                    it.copy(
                        isProcessing = false,
                        scannedTransaction = transaction,
                        confidenceScore = calculateConfidence(ocrText, transaction),
                        rawOcrText = ocrText,
                        originalImagePath = file.absolutePath,
                        isFromCamera = isFromCamera,
                        progressStep = 0
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        error = "Lỗi khi xử lý ảnh: ${e.message ?: "Không xác định"}",
                        progressStep = 0
                    )
                }
            }
        }
    }

    /**
     * Perform OCR on bitmap
     */
    private suspend fun performOCR(bitmap: Bitmap): String {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result: Text = textRecognizer.process(inputImage).await()
            result.text ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Parse invoice data from OCR text
     */
    private fun parseInvoiceData(ocrText: String): Transaction {
        val lines = ocrText.lines()

        // Extract merchant name
        val merchantName = extractMerchantName(lines) ?: "Hóa đơn không xác định"

        // Extract amount
        val amount = extractAmount(lines) ?: 0.0

        // Get current date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.format(Date())

        // Get day of week
        val calendar = Calendar.getInstance()
        val days = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
        val dayOfWeek = days[calendar.get(Calendar.DAY_OF_WEEK) - 1]

        // Auto-categorize
        val category = autoCategorize(merchantName)

        return Transaction(
            id = UUID.randomUUID().toString(),
            title = merchantName,
            amount = amount,
            date = date,
            dayOfWeek = dayOfWeek,
            category = category.name,
            categoryId = "",
            isIncome = false,
            group = "expense",
            wallet = "Ví chính",
            description = "Hóa đơn quét tự động\n$ocrText",
            categoryIcon = category.icon,
            categoryColor = category.color,
            createdAt = System.currentTimeMillis(),
            isAutoGenerated = false,
            recurringSourceId = ""
        )
    }

    /**
     * Extract merchant name from OCR lines
     */
    private fun extractMerchantName(lines: List<String>): String? {
        val patterns = listOf(
            Regex("(?i)(?:cửa hàng|shop|store|mua tại|tên đơn vị)[:：\\s]*([^\\n]+)"),
            Regex("(?i)(?:CỬA HÀNG|ĐƠN VỊ)[:：\\s]*([^\\n]+)"),
            Regex("(?i)^\\s*([A-ZÀ-Ỹ][A-ZÀ-Ỹ\\s]{2,})\\s*$")
        )

        for (pattern in patterns) {
            for (line in lines) {
                val match = pattern.find(line)
                if (match != null && match.groupValues.size > 1) {
                    return match.groupValues[1].trim()
                }
            }
        }

        // If no merchant found, try to find any capitalized line
        for (line in lines) {
            if (line.trim().matches(Regex("[A-ZÀ-Ỹ].*")) && line.length in 3..50) {
                return line.trim()
            }
        }

        return null
    }

    /**
     * Extract amount from OCR lines
     */
    private fun extractAmount(lines: List<String>): Double? {
        val patterns = listOf(
            Regex("(?i)(?:tổng cộng|thanh toán|total|thành tiền|tong cong)[:：\\s]*([\\d.,]+\\s*[₫đĐvndVND]?)"),
            Regex("([\\d.,]+\\s*[₫đĐ])"),
            Regex("(?i)VNĐ\\s*([\\d.,]+)"),
            Regex("(?i)đ\\s*([\\d.,]+)")
        )

        for (pattern in patterns) {
            for (line in lines) {
                val match = pattern.find(line)
                if (match != null) {
                    val amountStr = match.groupValues[1]
                        .replace(Regex("[₫đĐVNDvnd,]"), "")
                        .replace(".", "")
                        .trim()

                    return try {
                        amountStr.toDouble()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }

        return null
    }

    /**
     * Auto-categorize based on merchant name
     */
    private fun autoCategorize(merchantName: String): Category {
        val lowerName = merchantName.lowercase(Locale.getDefault())

        return when {
            lowerName.contains("coffee") || lowerName.contains("highlands") ||
                    lowerName.contains("trung nguyên") || lowerName.contains("phúc long") ||
                    lowerName.contains("starbucks") || lowerName.contains("quán ăn") ||
                    lowerName.contains("nhà hàng") || lowerName.contains("kfc") ||
                    lowerName.contains("lotteria") || lowerName.contains("pizza") ->
                Category("Ăn uống", "🍽️", "#4CAF50")

            lowerName.contains("circle k") || lowerName.contains("ministop") ||
                    lowerName.contains("family mart") || lowerName.contains("tiện lợi") ->
                Category("Tiện ích", "🏪", "#2196F3")

            lowerName.contains("vinmart") || lowerName.contains("coopmart") ||
                    lowerName.contains("big c") || lowerName.contains("siêu thị") ||
                    lowerName.contains("bách hóa") ->
                Category("Siêu thị", "🛒", "#FF9800")

            lowerName.contains("petrolimex") || lowerName.contains("shell") ||
                    lowerName.contains("total") || lowerName.contains("xăng") ->
                Category("Xăng xe", "⛽", "#9C27B0")

            lowerName.contains("pharmacity") || lowerName.contains("long châu") ||
                    lowerName.contains("nhà thuốc") || lowerName.contains("dược") ->
                Category("Sức khỏe", "💊", "#F44336")

            else -> Category("Mua sắm", "🛍️", "#9C27B0")
        }
    }

    /**
     * Calculate confidence score
     */
    private fun calculateConfidence(ocrText: String, transaction: Transaction): Float {
        var confidence = 0.3f // Base confidence

        // If merchant name was found
        if (transaction.title != "Hóa đơn không xác định") {
            confidence += 0.3f
        }

        // If amount was found
        if (transaction.amount > 0) {
            confidence += 0.25f
        }

        // If text contains common invoice keywords
        val invoiceKeywords = listOf("hóa đơn", "invoice", "tổng cộng", "thanh toán", "total", "₫", "đ")
        val containsKeywords = invoiceKeywords.any { keyword ->
            ocrText.lowercase(Locale.getDefault()).contains(keyword)
        }

        if (containsKeywords) {
            confidence += 0.15f
        }

        return confidence.coerceAtMost(0.95f)
    }

    /**
     * Process test invoice (for development)
     */
    fun processTestInvoice() {
        currentProcessingJob?.cancel()

        currentProcessingJob = viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progressStep = 0) }

            // Simulate processing steps
            repeat(4) { step ->
                delay(400)
                _state.update { it.copy(progressStep = step + 1) }
            }

            // Create test transaction
            val transaction = createTestTransaction()

            _state.update {
                it.copy(
                    isProcessing = false,
                    scannedTransaction = transaction,
                    confidenceScore = 0.92f,
                    rawOcrText = generateTestOCRText(),
                    progressStep = 0
                )
            }
        }
    }

    private fun createTestTransaction(): Transaction {
        val merchants = listOf(
            "Highlands Coffee" to "Ăn uống",
            "Circle K" to "Tiện ích",
            "VinMart+" to "Siêu thị",
            "Petrolimex" to "Xăng xe",
            "Pharmacity" to "Sức khỏe"
        )

        val random = Random()
        val randomIndex = random.nextInt(merchants.size)
        val (merchant, category) = merchants[randomIndex]

        // Format date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.format(Date())

        // Get day of week
        val calendar = Calendar.getInstance()
        val days = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
        val dayOfWeek = days[calendar.get(Calendar.DAY_OF_WEEK) - 1]

        // Generate random amount
        val amount = generateRandomAmount()

        return Transaction(
            id = UUID.randomUUID().toString(),
            title = merchant,
            amount = amount,
            date = date,
            dayOfWeek = dayOfWeek,
            category = category,
            categoryId = "",
            isIncome = false,
            group = "expense",
            wallet = "Ví chính",
            description = "Hóa đơn quét tự động từ: $merchant",
            categoryIcon = when(category) {
                "Ăn uống" -> "🍽️"
                "Tiện ích" -> "🏪"
                "Siêu thị" -> "🛒"
                "Xăng xe" -> "⛽"
                "Sức khỏe" -> "💊"
                else -> "🛍️"
            },
            categoryColor = when(category) {
                "Ăn uống" -> "#4CAF50"
                "Tiện ích" -> "#2196F3"
                "Siêu thị" -> "#FF9800"
                "Xăng xe" -> "#9C27B0"
                "Sức khỏe" -> "#F44336"
                else -> "#9C27B0"
            },
            createdAt = System.currentTimeMillis(),
            isAutoGenerated = false,
            recurringSourceId = ""
        )
    }

    private fun generateRandomAmount(): Double {
        val random = Random()
        val amounts = listOf(
            25000.0, 35000.0, 45000.0, 55000.0, 65000.0,
            75000.0, 120000.0, 180000.0, 250000.0, 320000.0,
            450000.0, 500000.0
        )
        return amounts[random.nextInt(amounts.size)]
    }

    private fun generateTestOCRText(): String {
        return """
            CỬA HÀNG: Highlands Coffee
            ĐỊA CHỈ: 123 Nguyễn Văn Linh, Quận 7, TP.HCM
            MÃ SỐ THUẾ: 0123456789
            SỐ HÓA ĐƠN: HD202400123
            NGÀY: ${SimpleDateFormat("dd/MM/yyyy").format(Date())}
            
            CHI TIẾT HÓA ĐƠN:
            Cà phê đen đá x2: 70,000đ
            Bánh mì sandwich: 50,000đ
            
            TỔNG CỘNG: 120,000đ
            THUẾ VAT: 12,000đ
            THÀNH TIỀN: 132,000đ
            
            CẢM ƠN QUÝ KHÁCH!
        """.trimIndent()
    }

    private fun getOutputDirectory(context: Context): File {
        val mediaDir = context.getExternalFilesDir(null)?.let {
            File(it, "Invoices").apply { mkdirs() }
        }
        return mediaDir ?: File(context.filesDir, "Invoices").apply { mkdirs() }
    }

    /**
     * Confirm use of scanned transaction
     */
    fun confirmUseTransaction() {
        _state.update { it.copy(shouldNavigateToAddTransaction = true) }
    }

    /**
     * Reset navigation flag
     */
    fun resetNavigationFlag() {
        _state.update { it.copy(shouldNavigateToAddTransaction = false) }
    }

    /**
     * Reset entire state
     */
    fun resetState() {
        currentProcessingJob?.cancel()
        _state.value = InvoiceScannerState()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

/**
 * State data class
 */
data class InvoiceScannerState(
    val isProcessing: Boolean = false,
    val progressStep: Int = 0,
    val scannedTransaction: Transaction? = null,
    val confidenceScore: Float = 0f,
    val rawOcrText: String? = null,
    val originalImagePath: String? = null,
    val isFromCamera: Boolean = false,
    val error: String? = null,
    val shouldNavigateToAddTransaction: Boolean = false
)

data class Category(
    val name: String,
    val icon: String,
    val color: String
)