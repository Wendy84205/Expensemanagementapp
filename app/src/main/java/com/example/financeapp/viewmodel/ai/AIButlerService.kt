package com.example.financeapp.viewmodel.ai

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.financeapp.FinanceApp
import com.example.financeapp.NotificationHelper
import com.example.financeapp.data.models.isOverBudget
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * AI Butler Service - Quản gia thông minh
 * Phân tích dữ liệu tài chính và gửi thông báo nhắc nhở user
 */
class AIButlerService(private val application: Application) {
    companion object {
        private const val TAG = "AIButlerService"
        private const val CHECK_INTERVAL_MS = 3600000L // Kiểm tra mỗi giờ
    }

    private val transactionViewModel: TransactionViewModel by lazy {
        (application as FinanceApp).transactionViewModel
    }



    private val budgetViewModel: BudgetViewModel by lazy {
        (application as FinanceApp).budgetViewModel
    }

    private var isRunning = false
    private var lastCheckTime = 0L

    /**
     * Bắt đầu service - sẽ chạy định kỳ để kiểm tra và gửi thông báo
     */
    fun start() {
        if (isRunning) {
            Log.d(TAG, "Service đã đang chạy")
            return
        }

        isRunning = true
        Log.d(TAG, "✅ AI Butler Service đã khởi động")

        // Tạo notification channel
        NotificationHelper.createChannel(application)

        // ✅ Gửi thông báo chào mừng để user biết AI đang hoạt động
        viewModelScope.launch {
            delay(2000) // Đợi 2 giây sau khi app khởi động
            if (isNotificationsEnabled(application)) {
                NotificationHelper.showNotification(
                    application,
                    "👋 Chào mừng!",
                    "AI Butler đã sẵn sàng. Tôi sẽ nhắc nhở bạn về tài chính!"
                )
            }
        }

        // Bắt đầu kiểm tra định kỳ
        startPeriodicChecks()
    }

    /**
     * Dừng service
     */
    fun stop() {
        isRunning = false
        Log.d(TAG, "AI Butler Service đã dừng")
    }

    /**
     * Kiểm tra định kỳ và gửi thông báo
     */
    private fun startPeriodicChecks() {
        viewModelScope.launch {
            while (isRunning) {
                try {
                    checkAndSendNotifications()
                    delay(CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi trong periodic check: ${e.message}", e)
                    delay(60000) // Đợi 1 phút nếu có lỗi
                }
            }
        }
    }

    /**
     * Kiểm tra các điều kiện và gửi thông báo
     */
    private suspend fun checkAndSendNotifications() {
        val now = System.currentTimeMillis()

        // Kiểm tra xem có nên gửi thông báo không (tránh spam)
        if (now - lastCheckTime < 300000) { // 5 phút
            return
        }

        lastCheckTime = now

        // Kiểm tra quyền thông báo
        if (!isNotificationsEnabled(application)) {
            Log.d(TAG, "Thông báo đã bị tắt")
            return
        }

        // Kiểm tra các điều kiện
        // Bỏ kiểm tra số dư ví
        checkBudgetExceeded()
        checkLargeTransaction()
        checkNoTransactionToday()
        checkMonthlySummary()
    }

    /**
     * Kiểm tra số dư thấp
     */
    private suspend fun checkLowBalance() { /* removed */ }

    /**
     * Kiểm tra ngân sách vượt quá
     */
    private suspend fun checkBudgetExceeded() {
        val budgets = budgetViewModel.budgets.value.filter { it.isActive && it.isOverBudget }

        if (budgets.isNotEmpty()) {
            val categoryNames = budgets.mapNotNull { budget ->
                // Lấy tên category từ categoryId
                val category = (application as FinanceApp).categoryViewModel.categories.value
                    .find { it.id == budget.categoryId }
                category?.name
            }.joinToString(", ")

            if (categoryNames.isNotEmpty()) {
                sendNotification(
                    "⚠️ Ngân sách vượt quá",
                    "Bạn đã vượt ngân sách cho: $categoryNames. Hãy kiểm soát chi tiêu!"
                )
            }
        }
    }

    /**
     * Kiểm tra giao dịch lớn
     */
    private suspend fun checkLargeTransaction() {
        val recentTransactions = transactionViewModel.transactions.value
            .filter { !it.isIncome }
            .sortedByDescending { parseDate(it.date) }
            .take(5)

        val largeTransactions = recentTransactions.filter { it.amount > 1000000 }

        if (largeTransactions.isNotEmpty()) {
            val latest = largeTransactions.first()
            sendNotification(
                "💸 Giao dịch lớn",
                "Bạn vừa chi ${formatCurrency(latest.amount)} cho '${latest.title}'. Hãy kiểm tra lại!"
            )
        }
    }

    /**
     * Kiểm tra chưa có giao dịch hôm nay
     */
    private suspend fun checkNoTransactionToday() {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val todayTransactions = transactionViewModel.transactions.value
            .filter { it.date == today }

        // Nếu chưa có giao dịch nào hôm nay và đã qua 18h
        val calendar = Calendar.getInstance()
        if (todayTransactions.isEmpty() && calendar.get(Calendar.HOUR_OF_DAY) >= 18) {
            sendNotification(
                "📝 Nhắc nhở",
                "Bạn chưa ghi nhận giao dịch nào hôm nay. Hãy cập nhật để theo dõi chi tiêu tốt hơn!"
            )
        }
    }

    /**
     * Kiểm tra tổng kết tháng
     */
    private suspend fun checkMonthlySummary() {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        // Gửi tổng kết vào ngày cuối tháng
        if (dayOfMonth == calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            val currentMonthTransactions = transactionViewModel.transactions.value
                .filter { isInCurrentMonth(it.date) }

            val totalIncome = currentMonthTransactions.filter { it.isIncome }.sumOf { it.amount }
            val totalExpense = currentMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val savings = totalIncome - totalExpense

            val message = buildString {
                append("📊 Tổng kết tháng:\n")
                append("Thu nhập: ${formatCurrency(totalIncome)}\n")
                append("Chi tiêu: ${formatCurrency(totalExpense)}\n")
                append("Tiết kiệm: ${formatCurrency(savings)}")
            }

            sendNotification("📈 Tổng kết tháng", message)
        }
    }

    /**
     * Gửi thông báo
     */
    private fun sendNotification(title: String, message: String) {
        try {
            if (isNotificationsEnabled(application)) {
                NotificationHelper.showNotification(application, title, message)
                Log.d(TAG, "Đã gửi thông báo: $title - $message")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi gửi thông báo: ${e.message}", e)
        }
    }

    /**
     * Kiểm tra xem thông báo có được bật không
     */
    private fun isNotificationsEnabled(context: Context): Boolean {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getBoolean("notifications_enabled", true)
    }

    /**
     * Format currency
     */
    private fun formatCurrency(amount: Double): String {
        return "%,.0f".format(amount) + "đ"
    }

    /**
     * Parse date
     */
    private fun parseDate(dateString: String): Date {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    /**
     * Kiểm tra xem có trong tháng hiện tại không
     */
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
            false
        }
    }

    // ✅ CoroutineScope cho service
    private val serviceScope = CoroutineScope(
        Dispatchers.Main + SupervisorJob()
    )

    // ✅ Alias để dễ sử dụng
    private val viewModelScope = serviceScope
}