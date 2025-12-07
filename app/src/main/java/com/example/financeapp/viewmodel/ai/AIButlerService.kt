package com.example.financeapp.viewmodel.ai

import android.app.Application
import android.util.Log
import com.example.financeapp.FinanceApp
import com.example.financeapp.data.models.isOverBudget
import com.example.financeapp.utils.notification.NotificationHelper
import com.example.financeapp.utils.notification.NotificationPreferences
import com.example.financeapp.utils.work.AIButlerWorker
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * AI Butler Service - Quản gia thông minh chạy background
 */
class AIButlerService(private val application: Application) {

    companion object {
        private const val TAG = "AIButlerService"
        private const val CHECK_INTERVAL_MS = 3600000L
        private const val MIN_CHECK_INTERVAL = 300000L
        private const val TRANSACTION_CHECK_HOUR = 18
        private const val LARGE_TRANSACTION_THRESHOLD = 1000000.0
    }

    // ==================== DEPENDENCIES ====================

    private val transactionViewModel: TransactionViewModel by lazy {
        (application as FinanceApp).transactionViewModel
    }

    private val budgetViewModel: BudgetViewModel by lazy {
        (application as FinanceApp).budgetViewModel
    }

    private val categoryViewModel by lazy {
        (application as FinanceApp).categoryViewModel
    }

    private val notificationPreferences: NotificationPreferences by lazy {
        NotificationPreferences(application)
    }

    // ==================== SERVICE STATE ====================

    private var isRunning = false
    private var lastCheckTime = 0L
    private var periodicCheckJob: Job? = null

    // ==================== COROUTINE SCOPE ====================

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ==================== SERVICE LIFECYCLE ====================

    fun start(): Boolean {
        if (isRunning) {
            Log.w(TAG, "Service đã đang chạy")
            return false
        }

        isRunning = true
        Log.i(TAG, "AI Butler Service đã khởi động")

        // Tạo notification channel
        NotificationHelper.createChannel(application)

        // Gửi thông báo chào mừng
        sendWelcomeNotification()

        // Bắt đầu kiểm tra định kỳ
        startPeriodicChecks()

        return true
    }

    fun stop() {
        isRunning = false
        periodicCheckJob?.cancel()
        periodicCheckJob = null
        Log.i(TAG, "AI Butler Service đã dừng")
    }

    fun isServiceRunning(): Boolean = isRunning

    // ==================== PERIODIC CHECKING ====================

    private fun startPeriodicChecks() {
        periodicCheckJob?.cancel()
        periodicCheckJob = serviceScope.launch {
            Log.d(TAG, "Bắt đầu kiểm tra định kỳ...")

            // Kiểm tra ngay lập tức khi khởi động
            forceCheckNow()

            // Lặp kiểm tra định kỳ
            while (isRunning) {
                try {
                    delay(CHECK_INTERVAL_MS)
                    forceCheckNow()
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi trong quá trình kiểm tra định kỳ", e)
                    delay(MIN_CHECK_INTERVAL)
                }
            }
        }
    }

    // ==================== CHECKING METHODS ====================

    private suspend fun checkBudgetExceeded() {
        try {
            Log.d(TAG, "Kiểm tra ngân sách vượt quá...")

            val budgets = withContext(Dispatchers.Main) {
                budgetViewModel.budgets.value.filter {
                    it.isActive &&
                            it.isOverBudget &&
                            isBudgetInCurrentPeriod(it)
                }
            }

            if (budgets.isNotEmpty()) {
                Log.i(TAG, "Phát hiện ${budgets.size} ngân sách vượt quá")

                val categoryNames = budgets.mapNotNull { budget ->
                    withContext(Dispatchers.Main) {
                        categoryViewModel.categories.value.find { it.id == budget.categoryId }?.name
                    }
                }.distinct().joinToString(", ")

                if (categoryNames.isNotEmpty() && notificationPreferences.canSendBudgetNotification()) {
                    val exceededAmount = budgets.first().spent - budgets.first().amount

                    sendNotification(
                        "Vượt ngân sách",
                        "Bạn đã vượt ngân sách cho: $categoryNames\n" +
                                "Vượt quá: ${formatCurrency(exceededAmount)}\n" +
                                "Hãy kiểm soát chi tiêu!"
                    )
                    Log.d(TAG, "Đã gửi thông báo vượt ngân sách: $categoryNames")
                }
            } else {
                Log.d(TAG, "Không có ngân sách nào vượt quá")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra ngân sách vượt quá", e)
        }
    }

    private suspend fun checkBudgetWarning() {
        try {
            Log.d(TAG, "Kiểm tra ngân sách sắp vượt...")

            // Lấy tất cả budget active trong kỳ
            val allBudgets = withContext(Dispatchers.Main) {
                budgetViewModel.budgets.value.filter {
                    it.isActive &&
                            isBudgetInCurrentPeriod(it) &&
                            it.amount > 0
                }
            }

            Log.d(TAG, "Đang kiểm tra ${allBudgets.size} ngân sách active...")

            // DEBUG: In thông tin từng budget
            allBudgets.forEach { budget ->
                val spentRatio = budget.spent / budget.amount
                val percentage = (spentRatio * 100).toInt()
                Log.d(TAG,
                    "Budget ${budget.categoryId}: " +
                            "amount=${budget.amount}, " +
                            "spent=${budget.spent}, " +
                            "spentAmount=${budget.spentAmount}, " +
                            "ratio=$spentRatio ($percentage%), " +
                            "isOverBudget=${budget.isOverBudget}"
                )
            }

            // Tìm ngân sách >80% và CHƯA vượt quá
            val warningBudgets = allBudgets.filter { budget ->
                val spentRatio = budget.spent / budget.amount
                spentRatio >= 0.8 && spentRatio < 1.0
            }

            Log.d(TAG, "Tìm thấy ${warningBudgets.size} ngân sách >80% và chưa vượt")

            if (warningBudgets.isNotEmpty()) {
                Log.i(TAG, "Phát hiện ${warningBudgets.size} ngân sách sắp vượt (>80%)")

                // Lấy 3 ngân sách có tỷ lệ cao nhất
                val topBudgets = warningBudgets.sortedByDescending {
                    it.spent / it.amount
                }.take(3)

                // Tạo message chi tiết
                val message = buildString {
                    append("Các ngân sách sắp vượt:\n")
                    topBudgets.forEach { budget ->
                        val categoryName = withContext(Dispatchers.Main) {
                            categoryViewModel.categories.value.find { it.id == budget.categoryId }?.name
                                ?: "Không xác định"
                        }
                        val percentage = (budget.spent / budget.amount * 100).toInt()
                        val remaining = budget.amount - budget.spent

                        append("• $categoryName: $percentage% (còn ${formatCurrency(remaining)})\n")
                    }
                    append("\nHãy kiểm soát chi tiêu để không vượt ngân sách!")
                }

                if (notificationPreferences.canSendBudgetNotification()) {
                    sendNotification(
                        "Ngân sách sắp vượt",
                        message
                    )
                    Log.d(TAG, "Đã gửi cảnh báo ngân sách sắp vượt")
                }
            } else {
                Log.d(TAG, "Không có ngân sách nào sắp vượt (>80%)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra cảnh báo ngân sách", e)
            Log.e(TAG, "Chi tiết lỗi: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun checkLargeTransaction() {
        try {
            Log.d(TAG, "Kiểm tra giao dịch lớn...")

            val recentTransactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value
                    .filter { !it.isIncome }
                    .sortedByDescending { parseDate(it.date) }
                    .take(5)
            }

            val largeTransactions = recentTransactions.filter { it.amount > LARGE_TRANSACTION_THRESHOLD }

            if (largeTransactions.isNotEmpty() && notificationPreferences.canSendTransactionNotification()) {
                Log.i(TAG, "Phát hiện ${largeTransactions.size} giao dịch lớn")

                val latest = largeTransactions.first()
                val categoryName = withContext(Dispatchers.Main) {
                    categoryViewModel.categories.value
                        .find { it.id == latest.categoryId }?.name ?: latest.category
                }

                sendNotification(
                    "Giao dịch lớn",
                    "Bạn vừa chi ${formatCurrency(latest.amount)} cho '${latest.title}' ($categoryName). Hãy kiểm tra lại!"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra giao dịch lớn", e)
        }
    }

    private suspend fun checkNoTransactionToday() {
        try {
            Log.d(TAG, "Kiểm tra giao dịch hôm nay...")

            val today = getTodayDate()
            val todayTransactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value.filter { it.date == today }
            }

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

            if (todayTransactions.isEmpty() && currentHour >= TRANSACTION_CHECK_HOUR) {
                Log.i(TAG, "Chưa có giao dịch nào hôm nay (sau $TRANSACTION_CHECK_HOUR giờ)")

                sendNotification(
                    "Nhắc nhở ghi chép",
                    "Bạn chưa ghi nhận giao dịch nào hôm nay. Hãy cập nhật để theo dõi chi tiêu tốt hơn!"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra giao dịch hôm nay", e)
        }
    }

    private suspend fun checkMonthlySummary() {
        try {
            Log.d(TAG, "Kiểm tra tổng kết tháng...")

            val calendar = Calendar.getInstance()
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            if (dayOfMonth >= (lastDayOfMonth - 2)) {
                Log.i(TAG, "Đang trong 3 ngày cuối tháng, kiểm tra tổng kết")

                val currentMonthTransactions = withContext(Dispatchers.Main) {
                    transactionViewModel.transactions.value.filter { isInCurrentMonth(it.date) }
                }

                if (currentMonthTransactions.isNotEmpty()) {
                    val totalIncome = currentMonthTransactions
                        .filter { it.isIncome }
                        .sumOf { it.amount }
                    val totalExpense = currentMonthTransactions
                        .filter { !it.isIncome }
                        .sumOf { it.amount }
                    val savings = totalIncome - totalExpense

                    val message = """
                        Tổng kết tháng:
                        • Thu nhập: ${formatCurrency(totalIncome)}
                        • Chi tiêu: ${formatCurrency(totalExpense)}
                        • Tiết kiệm: ${formatCurrency(savings)}
                        
                        ${if (savings > 0) "Tuyệt vời! Bạn đang tiết kiệm được tiền."
                    else if (savings < 0) "Cần xem xét lại chi tiêu tháng sau."
                    else "Chi tiêu cân bằng với thu nhập."}
                    """.trimIndent()

                    sendNotification("Tổng kết tháng", message)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra tổng kết tháng", e)
        }
    }

    // Thêm vào AIButlerService.kt
    fun scheduleBackgroundWorker() {
        try {
            Log.i(TAG, "Đang lên lịch background worker...")

            // Lên lịch worker với WorkManager
            AIButlerWorker.schedule(application)

            Log.i(TAG, "Đã lên lịch background worker thành công")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lên lịch background worker", e)
        }
    }

    fun stopBackgroundWorker() {
        try {
            Log.i(TAG, "Đang dừng background worker...")
            AIButlerWorker.cancel(application)
            Log.i(TAG, "Đã dừng background worker thành công")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi dừng background worker", e)
        }
    }

    // ==================== NOTIFICATION METHODS ====================

    private fun sendWelcomeNotification() {
        serviceScope.launch {
            delay(2000)

            if (NotificationHelper.hasNotificationPermission(application) &&
                notificationPreferences.canSendAINotification()) {
                NotificationHelper.showNotification(
                    application,
                    "Chào mừng đến với WendyAI",
                    "AI Butler đã sẵn sàng. Tôi sẽ nhắc nhở bạn về tài chính!"
                )
            }
        }
    }

    private fun sendNotification(title: String, message: String) {
        try {
            if (title.contains("vượt", ignoreCase = true) ||
                title.contains("VƯỢT", ignoreCase = true)) {
                NotificationHelper.showBudgetAlertNotification(
                    application,
                    title,
                    message
                )
            } else {
                NotificationHelper.showAINotification(
                    application,
                    title,
                    message
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi thông báo: $title", e)
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Kiểm tra budget có đang trong kỳ hiện tại không
     */
    private fun isBudgetInCurrentPeriod(budget: com.example.financeapp.data.models.Budget): Boolean {
        return try {
            val today = LocalDate.now()
            (today.isAfter(budget.startDate) || today.isEqual(budget.startDate)) &&
                    (today.isBefore(budget.endDate) || today.isEqual(budget.endDate))
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kiểm tra budget period", e)
            true // Nếu lỗi, giả sử đang trong kỳ
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            val formatter = java.text.NumberFormat.getInstance(Locale.getDefault())
            "${formatter.format(amount)}đ"
        } catch (e: Exception) {
            "${amount.toInt()}đ"
        }
    }

    private fun parseDate(dateString: String): Date {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi parse date: $dateString", e)
            Date()
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    private fun isInCurrentMonth(dateString: String): Boolean {
        return try {
            val transactionDate = parseDate(dateString)
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            calendar.time = transactionDate
            val transactionMonth = calendar.get(Calendar.MONTH)
            val transactionYear = calendar.get(Calendar.YEAR)

            currentMonth == transactionMonth && currentYear == transactionYear
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kiểm tra tháng: $dateString", e)
            false
        }
    }

    fun forceCheckNow() {
        serviceScope.launch {
            Log.i(TAG, "Buộc kiểm tra ngay lập tức...")
            executeAllChecks()
        }
    }

    private suspend fun executeAllChecks() {
        try {
            Log.d(TAG, "Thực hiện tất cả các kiểm tra...")

            // Đợi để đảm bảo dữ liệu đã load
            delay(2000)

            checkBudgetExceeded()
            checkBudgetWarning()
            checkLargeTransaction()
            checkNoTransactionToday()
            checkMonthlySummary()

            Log.i(TAG, "Đã hoàn thành tất cả kiểm tra định kỳ")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi thực hiện các kiểm tra", e)
        }
    }

    private suspend fun checkAndSendNotifications() {
        val now = System.currentTimeMillis()

        if (now - lastCheckTime < MIN_CHECK_INTERVAL) {
            Log.d(TAG, "Bỏ qua kiểm tra, vừa kiểm tra gần đây")
            return
        }

        lastCheckTime = now
        Log.d(TAG, "Bắt đầu kiểm tra điều kiện thông báo")

        // Kiểm tra quyền thông báo
        if (!NotificationHelper.hasNotificationPermission(application)) {
            Log.w(TAG, "Không có quyền thông báo, bỏ qua kiểm tra")
            return
        }

        // Kiểm tra notification preferences
        if (!notificationPreferences.canSendAINotification()) {
            Log.w(TAG, "Thông báo AI đã bị tắt trong cài đặt")
            return
        }

        // Thực hiện các kiểm tra
        executeAllChecks()
    }
}