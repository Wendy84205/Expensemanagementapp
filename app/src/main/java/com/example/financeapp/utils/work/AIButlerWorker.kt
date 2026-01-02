package com.example.financeapp.utils.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.financeapp.FinanceApp
import com.example.financeapp.data.models.isOverBudget
import com.example.financeapp.utils.notification.NotificationHelper
import com.example.financeapp.utils.notification.NotificationPreferences
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Worker để chạy kiểm tra định kỳ trong background
 * Kiểm tra các điều kiện tài chính và gửi thông báo khi cần
 */
class AIButlerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "wendyai_butler_periodic_work"
        private const val REPEAT_INTERVAL_HOURS = 6L
        private const val FLEX_INTERVAL_HOURS = 1L
        private const val END_OF_MONTH_DAYS = 3
        private const val EVENING_START_HOUR = 18
        private const val EVENING_END_HOUR = 20
        private const val LARGE_TRANSACTION_THRESHOLD = 1000000.0

        @JvmStatic
        fun schedule(context: Context): Boolean {
            return try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresCharging(false)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<AIButlerWorker>(
                    REPEAT_INTERVAL_HOURS, TimeUnit.HOURS,
                    FLEX_INTERVAL_HOURS, TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .addTag("finance_monitoring")
                    .addTag("ai_butler")
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )

                true

            } catch (e: Exception) {
                false
            }
        }

        @JvmStatic
        fun cancel(context: Context): Boolean {
            return try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                true
            } catch (e: Exception) {
                false
            }
        }

        @JvmStatic
        fun isScheduled(context: Context): Boolean {
            return try {
                val workManager = WorkManager.getInstance(context)
                val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()

                val isScheduled = workInfos.any { workInfo ->
                    workInfo.state == androidx.work.WorkInfo.State.ENQUEUED ||
                            workInfo.state == androidx.work.WorkInfo.State.RUNNING
                }

                isScheduled
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Kiểm tra và khởi động Worker nếu cần
         */
        @JvmStatic
        fun checkAndStartWorker(context: Context) {
            if (!isScheduled(context)) {
                schedule(context)
            }
        }
    }

    // ==================== DEPENDENCIES ====================

    private val transactionViewModel: TransactionViewModel by lazy {
        (applicationContext as FinanceApp).transactionViewModel
    }

    private val budgetViewModel: BudgetViewModel by lazy {
        (applicationContext as FinanceApp).budgetViewModel
    }

    private val categoryViewModel: CategoryViewModel by lazy {
        (applicationContext as FinanceApp).categoryViewModel
    }

    private val notificationPreferences: NotificationPreferences by lazy {
        NotificationPreferences(applicationContext)
    }

    // ==================== WORKER EXECUTION ====================

    override suspend fun doWork(): Result {
        return try {
            delay(3000)

            val success = checkAndSendNotifications()

            if (success) {
                Result.success()
            } else {
                Result.success()
            }

        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkAndSendNotifications(): Boolean {
        return try {
            val context = applicationContext

            if (!NotificationHelper.hasNotificationPermission(context)) {
                return false
            }

            if (!notificationPreferences.canSendAINotification()) {
                return false
            }

            NotificationHelper.createChannel(context)

            checkFinancialConditions()

            true

        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkFinancialConditions() {
        try {
            awaitDataLoad()

            checkBudgetExceeded()

            checkBudgetWarning()

            if (isEndOfMonth()) {
                sendMonthlySummaryNotification()
            }

            if (isEveningTime()) {
                sendDailyReminder()
            }

            checkLargeTransactions()

            checkTodayTransactions()

        } catch (e: Exception) {
        }
    }

    /**
     * Đợi dữ liệu được load
     */
    private suspend fun awaitDataLoad() {
        delay(2000)
    }

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

    /**
     * Kiểm tra giao dịch lớn
     */
    private suspend fun checkLargeTransactions() {
        try {
            val recentTransactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value
                    .filter { !it.isIncome }
                    .sortedByDescending { parseDate(it.date) }
                    .take(10)
            }

            val now = System.currentTimeMillis()
            val oneDayAgo = now - (24 * 60 * 60 * 1000)

            val largeTransactions = recentTransactions.filter { transaction ->
                val transactionTime = parseDate(transaction.date).time
                transaction.amount >= LARGE_TRANSACTION_THRESHOLD &&
                        transactionTime > oneDayAgo
            }

            if (largeTransactions.isNotEmpty() && notificationPreferences.canSendTransactionNotification()) {
                val latest = largeTransactions.first()
                val categoryName = withContext(Dispatchers.IO) {
                    getCategoryName(latest.categoryId) ?: latest.category
                }

                sendNotification(
                    title = "Giao dịch lớn phát hiện",
                    message = "Bạn vừa chi ${formatCurrency(latest.amount)} cho '${latest.title}' ($categoryName).\nHãy kiểm tra lại chi tiêu!"
                )
            }

        } catch (e: Exception) {
        }
    }

    /**
     * Kiểm tra giao dịch hôm nay
     */
    private suspend fun checkTodayTransactions() {
        try {
            val today = getTodayDate()
            val todayTransactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value.filter { it.date == today }
            }

            val totalTodaySpending = todayTransactions
                .filter { !it.isIncome }
                .sumOf { it.amount }

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

            if (todayTransactions.isEmpty() && currentHour >= EVENING_START_HOUR) {
                val canRemind = try {
                    notificationPreferences.canSendReminderNotification()
                } catch (e: NoSuchMethodError) {
                    notificationPreferences.canSendAINotification()
                }

                if (canRemind) {
                    sendNotification(
                        title = "Nhắc nhở ghi chép",
                        message = "Bạn chưa ghi nhận giao dịch nào hôm nay!\nHãy cập nhật để theo dõi chi tiêu chính xác."
                    )
                }
            }

            if (totalTodaySpending > 500000 && notificationPreferences.canSendBudgetNotification()) {
                sendNotification(
                    title = "Chi tiêu hôm nay",
                    message = "Bạn đã chi ${formatCurrency(totalTodaySpending)} hôm nay.\nHãy theo dõi để không vượt ngân sách!"
                )
            }

        } catch (e: Exception) {
        }
    }

    /**
     * Gửi thông báo tổng kết tháng
     */
    private suspend fun sendMonthlySummaryNotification() {
        try {
            val context = applicationContext

            if (!NotificationHelper.hasNotificationPermission(context)) {
                return
            }

            val currentMonthTransactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value
                    .filter { isInCurrentMonth(it.date) }
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

                NotificationHelper.showNotification(
                    context = context,
                    title = "Tổng kết tháng",
                    message = message
                )
            }

        } catch (e: Exception) {
        }
    }

    /**
     * Gửi thông báo nhắc nhở hàng ngày
     */
    private suspend fun sendDailyReminder() {
        try {
            val context = applicationContext

            if (!NotificationHelper.hasNotificationPermission(context)) {
                return
            }

            val canRemind = try {
                notificationPreferences.canSendReminderNotification()
            } catch (e: NoSuchMethodError) {
                notificationPreferences.canSendAINotification()
            }

            if (canRemind) {
                NotificationHelper.showNotification(
                    context = context,
                    title = "Nhắc nhở tài chính",
                    message = "Đừng quên ghi chép các giao dịch hôm nay để quản lý chi tiêu tốt hơn!"
                )
            }

        } catch (e: Exception) {
        }
    }

    /**
     * Gửi thông báo
     */
    private fun sendNotification(title: String, message: String) {
        try {
            NotificationHelper.showNotification(
                context = applicationContext,
                title = title,
                message = message
            )
        } catch (e: Exception) {
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Lấy tên danh mục
     */
    private suspend fun getCategoryName(categoryId: String): String? {
        return withContext(Dispatchers.Main) {
            categoryViewModel.categories.value.find { it.id == categoryId }?.name
        }
    }

    /**
     * Kiểm tra budget có đang trong kỳ hiện tại không
     */
    private fun isBudgetInCurrentPeriod(budget: com.example.financeapp.data.models.Budget): Boolean {
        return try {
            val today = LocalDate.now()
            (today.isAfter(budget.startDate) || today.isEqual(budget.startDate)) &&
                    (today.isBefore(budget.endDate) || today.isEqual(budget.endDate))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kiểm tra xem có phải cuối tháng không
     */
    private fun isEndOfMonth(): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return today >= (lastDay - END_OF_MONTH_DAYS + 1)
    }

    /**
     * Kiểm tra xem có phải giờ buổi tối không
     */
    private fun isEveningTime(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour in EVENING_START_HOUR..EVENING_END_HOUR
    }

    /**
     * Kiểm tra xem ngày có trong tháng hiện tại không
     */
    private fun isInCurrentMonth(dateString: String): Boolean {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val transactionDate = LocalDate.parse(dateString, formatter)
            val today = LocalDate.now()
            transactionDate.year == today.year && transactionDate.month == today.month
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Parse ngày từ string
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
     */
    private fun getTodayDate(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    /**
     * Định dạng tiền tệ
     */
    private fun formatCurrency(amount: Double): String {
        return try {
            val formatter = java.text.NumberFormat.getInstance(Locale.getDefault())
            "${formatter.format(amount)}đ"
        } catch (e: Exception) {
            "${amount.toInt()}đ"
        }
    }
}