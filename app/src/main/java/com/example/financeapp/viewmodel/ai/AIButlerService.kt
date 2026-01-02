package com.example.financeapp.viewmodel.ai

import android.app.Application
import androidx.core.app.NotificationCompat
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
            return false
        }

        isRunning = true

        NotificationHelper.createChannel(application)

        sendWelcomeNotification()

        startPeriodicChecks()

        return true
    }

    fun stop() {
        isRunning = false
        periodicCheckJob?.cancel()
        periodicCheckJob = null
    }

    fun isServiceRunning(): Boolean = isRunning

    // ==================== PERIODIC CHECKING ====================

    private fun startPeriodicChecks() {
        periodicCheckJob?.cancel()
        periodicCheckJob = serviceScope.launch {
            forceCheckNow()

            while (isRunning) {
                try {
                    delay(CHECK_INTERVAL_MS)
                    forceCheckNow()
                } catch (e: Exception) {
                    delay(MIN_CHECK_INTERVAL)
                }
            }
        }
    }

    // ==================== CHECKING METHODS ====================

    private suspend fun checkBudgetExceeded() {
        try {
            val budgets = withContext(Dispatchers.Main) {
                budgetViewModel.budgets.value.filter {
                    it.isActive &&
                            it.isOverBudget &&
                            isBudgetInCurrentPeriod(it)
                }
            }

            if (budgets.isNotEmpty()) {
                val categoryNames = budgets.mapNotNull { budget ->
                    withContext(Dispatchers.Main) {
                        categoryViewModel.categories.value.find { it.id == budget.categoryId }?.name
                    }
                }.distinct().joinToString(", ")

                if (categoryNames.isNotEmpty() && notificationPreferences.canSendBudgetNotification()) {
                    val exceededAmount = budgets.first().spentAmount - budgets.first().amount

                    sendNotification(
                        "Vượt ngân sách",
                        "Bạn đã vượt ngân sách cho: $categoryNames\n" +
                                "Vượt quá: ${formatCurrency(exceededAmount)}\n" +
                                "Hãy kiểm soát chi tiêu!"
                    )
                }
            }

        } catch (e: Exception) {
        }
    }

    private suspend fun checkBudgetWarning() {
        try {
            val allBudgets = withContext(Dispatchers.Main) {
                budgetViewModel.budgets.value.filter {
                    it.isActive &&
                            isBudgetInCurrentPeriod(it) &&
                            it.amount > 0
                }
            }

            val warningBudgets = allBudgets.filter { budget ->
                val spentRatio = budget.spentAmount / budget.amount
                spentRatio >= 0.8 && spentRatio < 1.0
            }

            if (warningBudgets.isNotEmpty()) {
                val topBudgets = warningBudgets.sortedByDescending {
                    it.spentAmount / it.amount
                }.take(3)

                val message = buildString {
                    append("Các ngân sách sắp vượt:\n")
                    topBudgets.forEach { budget ->
                        val categoryName = withContext(Dispatchers.Main) {
                            categoryViewModel.categories.value.find { it.id == budget.categoryId }?.name
                                ?: "Không xác định"
                        }
                        val percentage = (budget.spentAmount / budget.amount * 100).toInt()
                        val remaining = budget.amount - budget.spentAmount

                        append("• $categoryName: $percentage% (còn ${formatCurrency(remaining)})\n")
                    }
                    append("\nHãy kiểm soát chi tiêu để không vượt ngân sách!")
                }

                if (notificationPreferences.canSendBudgetNotification()) {
                    sendNotification(
                        "Ngân sách sắp vượt",
                        message
                    )
                }
            }

        } catch (e: Exception) {
        }
    }

    private suspend fun checkLargeTransaction() {
        try {
            val recentTransactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value
                    .filter { !it.isIncome }
                    .sortedByDescending { parseDate(it.date) }
                    .take(5)
            }

            val largeTransactions = recentTransactions.filter { it.amount > LARGE_TRANSACTION_THRESHOLD }

            if (largeTransactions.isNotEmpty() && notificationPreferences.canSendTransactionNotification()) {
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
        }
    }

    private suspend fun checkNoTransactionToday() {
        try {
            val today = getTodayDate()
            val todayTransactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value.filter { it.date == today }
            }

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

            if (todayTransactions.isEmpty() && currentHour >= TRANSACTION_CHECK_HOUR) {
                sendNotification(
                    "Nhắc nhở ghi chép",
                    "Bạn chưa ghi nhận giao dịch nào hôm nay. Hãy cập nhật để theo dõi chi tiêu tốt hơn!"
                )
            }

        } catch (e: Exception) {
        }
    }

    private suspend fun checkMonthlySummary() {
        try {
            val calendar = Calendar.getInstance()
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            if (dayOfMonth >= (lastDayOfMonth - 2)) {
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
        }
    }

    fun scheduleBackgroundWorker() {
        try {
            AIButlerWorker.schedule(application)
        } catch (e: Exception) {
        }
    }

    fun stopBackgroundWorker() {
        try {
            AIButlerWorker.cancel(application)
        } catch (e: Exception) {
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
                NotificationHelper.showNotification(
                    context = application,
                    title = title,
                    message = message,
                    channelId = NotificationHelper.CHANNEL_ID_ALERTS,
                    priority = NotificationCompat.PRIORITY_HIGH
                )
            } else {
                NotificationHelper.showAINotification(
                    application,
                    title,
                    message
                )
            }

        } catch (e: Exception) {
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
            true
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
            false
        }
    }

    fun forceCheckNow() {
        serviceScope.launch {
            executeAllChecks()
        }
    }

    private suspend fun executeAllChecks() {
        try {
            delay(2000)

            checkBudgetExceeded()
            checkBudgetWarning()
            checkLargeTransaction()
            checkNoTransactionToday()
            checkMonthlySummary()

        } catch (e: Exception) {
        }
    }

    private suspend fun checkAndSendNotifications() {
        val now = System.currentTimeMillis()

        if (now - lastCheckTime < MIN_CHECK_INTERVAL) {
            return
        }

        lastCheckTime = now

        if (!NotificationHelper.hasNotificationPermission(application)) {
            return
        }

        if (!notificationPreferences.canSendAINotification()) {
            return
        }

        executeAllChecks()
    }
}