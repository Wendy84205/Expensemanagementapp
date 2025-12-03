package com.example.financeapp.viewmodel.ai

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.financeapp.FinanceApp
import com.example.financeapp.data.models.isOverBudget
import com.example.financeapp.utils.notification.NotificationHelper
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
 * AI Butler Service - Quản gia thông minh chạy background
 * Service kiểm tra định kỳ và gửi thông báo nhắc nhở về tài chính
 * Chạy độc lập với AIViewModel (không tương tác trực tiếp với user)
 */
class AIButlerService(private val application: Application) {

    companion object {
        private const val TAG = "AIButlerService"

        /** Khoảng thời gian kiểm tra định kỳ (mỗi giờ) */
        private const val CHECK_INTERVAL_MS = 3600000L

        /** Khoảng thời gian tối thiểu giữa các lần kiểm tra (5 phút) */
        private const val MIN_CHECK_INTERVAL = 300000L

        /** Giờ bắt đầu kiểm tra giao dịch trong ngày (18:00) */
        private const val TRANSACTION_CHECK_HOUR = 18
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

    // ==================== SERVICE STATE ====================

    /** Trạng thái service đang chạy */
    private var isRunning = false

    /** Thời gian kiểm tra lần cuối */
    private var lastCheckTime = 0L

    // ==================== COROUTINE SCOPE ====================

    /** Coroutine scope riêng cho service */
    private val serviceScope = CoroutineScope(
        Dispatchers.Main + SupervisorJob()
    )

    /** Alias cho serviceScope để dễ sử dụng */
    private val viewModelScope = serviceScope

    // ==================== SERVICE LIFECYCLE ====================

    /**
     * Khởi động service - sẽ chạy định kỳ để kiểm tra và gửi thông báo
     */
    fun start() {
        if (isRunning) {
            Log.d(TAG, "Service đã đang chạy")
            return
        }

        isRunning = true
        Log.d(TAG, "AI Butler Service đã khởi động")

        // Tạo notification channel
        NotificationHelper.createChannel(application)

        // Gửi thông báo chào mừng để user biết AI đang hoạt động
        viewModelScope.launch {
            delay(2000) // Đợi 2 giây sau khi app khởi động
            if (isNotificationsEnabled(application)) {
                NotificationHelper.showNotification(
                    application,
                    "Chào mừng!",
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

    // ==================== PERIODIC CHECKING ====================

    /**
     * Bắt đầu kiểm tra định kỳ
     */
    private fun startPeriodicChecks() {
        viewModelScope.launch {
            while (isRunning) {
                try {
                    checkAndSendNotifications()
                    delay(CHECK_INTERVAL_MS) // Đợi 1 giờ
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
        if (now - lastCheckTime < MIN_CHECK_INTERVAL) {
            return
        }

        lastCheckTime = now

        // Kiểm tra quyền thông báo
        if (!isNotificationsEnabled(application)) {
            Log.d(TAG, "Thông báo đã bị tắt")
            return
        }

        // Kiểm tra các điều kiện
        checkBudgetExceeded()
        checkLargeTransaction()
        checkNoTransactionToday()
        checkMonthlySummary()

        Log.d(TAG, "Đã hoàn thành kiểm tra định kỳ")
    }

    // ==================== CHECKING METHODS ====================

    /**
     * Kiểm tra ngân sách vượt quá
     */
    private suspend fun checkBudgetExceeded() {
        try {
            val budgets = budgetViewModel.budgets.value.filter { it.isActive && it.isOverBudget }

            if (budgets.isNotEmpty()) {
                val categoryNames = budgets.mapNotNull { budget ->
                    categoryViewModel.categories.value.find { it.id == budget.categoryId }?.name
                }.joinToString(", ")

                if (categoryNames.isNotEmpty()) {
                    sendNotification(
                        "Ngân sách vượt quá",
                        "Bạn đã vượt ngân sách cho: $categoryNames. Hãy kiểm soát chi tiêu!"
                    )
                    Log.d(TAG, "Phát hiện ngân sách vượt: $categoryNames")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kiểm tra ngân sách vượt: ${e.message}")
        }
    }

    /**
     * Kiểm tra giao dịch lớn (> 1 triệu)
     */
    private suspend fun checkLargeTransaction() {
        try {
            val recentTransactions = transactionViewModel.transactions.value
                .filter { !it.isIncome }
                .sortedByDescending { parseDate(it.date) }
                .take(5)

            val largeTransactions = recentTransactions.filter { it.amount > 1000000 }

            if (largeTransactions.isNotEmpty()) {
                val latest = largeTransactions.first()
                sendNotification(
                    "Giao dịch lớn",
                    "Bạn vừa chi ${formatCurrency(latest.amount)} cho '${latest.title}'. Hãy kiểm tra lại!"
                )
                Log.d(TAG, "Phát hiện giao dịch lớn: ${latest.title} - ${formatCurrency(latest.amount)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kiểm tra giao dịch lớn: ${e.message}")
        }
    }

    /**
     * Kiểm tra chưa có giao dịch hôm nay (sau 18:00)
     */
    private suspend fun checkNoTransactionToday() {
        try {
            val today = getTodayDate()
            val todayTransactions = transactionViewModel.transactions.value
                .filter { it.date == today }

            val calendar = Calendar.getInstance()
            // Nếu chưa có giao dịch nào hôm nay và đã qua 18h
            if (todayTransactions.isEmpty() && calendar.get(Calendar.HOUR_OF_DAY) >= TRANSACTION_CHECK_HOUR) {
                sendNotification(
                    "Nhắc nhở",
                    "Bạn chưa ghi nhận giao dịch nào hôm nay. Hãy cập nhật để theo dõi chi tiêu tốt hơn!"
                )
                Log.d(TAG, "Chưa có giao dịch hôm nay (sau 18:00)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kiểm tra giao dịch hôm nay: ${e.message}")
        }
    }

    /**
     * Kiểm tra và gửi tổng kết tháng (vào ngày cuối tháng)
     */
    private suspend fun checkMonthlySummary() {
        try {
            val calendar = Calendar.getInstance()
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            // Gửi tổng kết vào ngày cuối tháng
            if (dayOfMonth == lastDayOfMonth) {
                val currentMonthTransactions = transactionViewModel.transactions.value
                    .filter { isInCurrentMonth(it.date) }

                val totalIncome = currentMonthTransactions.filter { it.isIncome }.sumOf { it.amount }
                val totalExpense = currentMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }
                val savings = totalIncome - totalExpense

                val message = buildString {
                    append("Tổng kết tháng:\n")
                    append("Thu nhập: ${formatCurrency(totalIncome)}\n")
                    append("Chi tiêu: ${formatCurrency(totalExpense)}\n")
                    append("Tiết kiệm: ${formatCurrency(savings)}")
                }

                sendNotification("Tổng kết tháng", message)
                Log.d(TAG, "Đã gửi tổng kết tháng")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kiểm tra tổng kết tháng: ${e.message}")
        }
    }

    // ==================== NOTIFICATION METHODS ====================

    /**
     * Gửi thông báo
     * @param title Tiêu đề thông báo
     * @param message Nội dung thông báo
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

    // ==================== UTILITY METHODS ====================

    /**
     * Kiểm tra xem thông báo có được bật không
     * @param context Context của ứng dụng
     * @return true nếu thông báo được bật
     */
    private fun isNotificationsEnabled(context: Context): Boolean {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getBoolean("notifications_enabled", true)
    }

    /**
     * Định dạng tiền tệ
     * @param amount Số tiền cần định dạng
     * @return Chuỗi tiền tệ đã định dạng
     */
    private fun formatCurrency(amount: Double): String {
        return "%,.0f".format(amount) + "đ"
    }

    /**
     * Parse ngày từ string
     * @param dateString Chuỗi ngày (dd/MM/yyyy)
     * @return Date object
     */
    private fun parseDate(dateString: String): Date {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    /**
     * Lấy ngày hiện tại dạng dd/MM/yyyy
     * @return Ngày hiện tại
     */
    private fun getTodayDate(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    /**
     * Kiểm tra xem ngày có trong tháng hiện tại không
     * @param dateString Chuỗi ngày cần kiểm tra
     * @return true nếu trong tháng hiện tại
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
}