package com.example.financeapp.utils.work

import android.content.Context
import android.util.Log
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
        private const val TAG = "AIButlerWorker"
        private const val WORK_NAME = "ai_butler_periodic_work"
        private const val REPEAT_INTERVAL_HOURS = 6L // Kiểm tra mỗi 6 giờ
        private const val FLEX_INTERVAL_HOURS = 1L
        private const val END_OF_MONTH_DAYS = 3
        private const val EVENING_START_HOUR = 18
        private const val EVENING_END_HOUR = 20
        private const val LARGE_TRANSACTION_THRESHOLD = 1000000.0

        @JvmStatic
        fun schedule(context: Context): Boolean {
            return try {
                Log.i(TAG, "Bắt đầu lên lịch AI Butler Worker...")

                // Constraints nhẹ hơn để có thể chạy thường xuyên
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(false) // Cho phép chạy ngay cả khi pin thấp
                    .setRequiresCharging(false) // Không cần sạc
                    .build()

                Log.d(TAG, "Constraints: Network=CONNECTED")

                val workRequest = PeriodicWorkRequestBuilder<AIButlerWorker>(
                    REPEAT_INTERVAL_HOURS, TimeUnit.HOURS,
                    FLEX_INTERVAL_HOURS, TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .addTag("finance_monitoring")
                    .addTag("ai_butler")
                    .build()

                Log.d(TAG, "Đã tạo work request: interval=$REPEAT_INTERVAL_HOURS hours")

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )

                Log.i(TAG, "AI Butler Worker đã được lên lịch thành công")
                true

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi lên lịch worker", e)
                false
            }
        }

        @JvmStatic
        fun cancel(context: Context): Boolean {
            return try {
                Log.i(TAG, "Bắt đầu hủy lịch trình AI Butler Worker...")
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.i(TAG, "AI Butler Worker đã bị hủy thành công")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi hủy worker", e)
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

                Log.d(TAG, "Worker scheduled status: $isScheduled")
                isScheduled
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi kiểm tra trạng thái worker", e)
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
                Log.i(TAG, "Đã tự động khởi động worker")
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
            Log.i(TAG, "AI Butler Worker bắt đầu thực thi...")

            // ĐỢI để ViewModel load dữ liệu
            delay(3000)

            val success = checkAndSendNotifications()

            if (success) {
                Log.i(TAG, "AI Butler Worker hoàn thành thành công")
                Result.success()
            } else {
                Log.w(TAG, "AI Butler Worker có cảnh báo")
                Result.success() // Vẫn thành công để tiếp tục chạy
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi nghiêm trọng trong AI Butler Worker", e)
            Result.retry() // Thử lại sau
        }
    }

    private suspend fun checkAndSendNotifications(): Boolean {
        return try {
            Log.d(TAG, "Bắt đầu kiểm tra điều kiện thông báo...")

            val context = applicationContext

            // 1. Kiểm tra permission notification
            if (!NotificationHelper.hasNotificationPermission(context)) {
                Log.w(TAG, "Không có quyền notification, bỏ qua")
                return false
            }

            // 2. Kiểm tra notification preferences
            if (!notificationPreferences.canSendAINotification()) {
                Log.d(TAG, "Notification AI đã bị tắt trong cài đặt")
                return false
            }

            // 3. Đảm bảo notification channel được tạo
            NotificationHelper.createChannel(context)

            // 4. Kiểm tra các điều kiện tài chính
            checkFinancialConditions()

            Log.i(TAG, "Hoàn thành kiểm tra và gửi thông báo")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra và gửi notification", e)
            false
        }
    }

    private suspend fun checkFinancialConditions() {
        try {
            Log.d(TAG, "Bắt đầu kiểm tra điều kiện tài chính...")

            // 1. Kiểm tra và cập nhật dữ liệu trước
            awaitDataLoad()

            // 2. Kiểm tra ngân sách vượt quá
            checkBudgetExceeded()

            // 3. Kiểm tra ngân sách sắp vượt (>80%)
            checkBudgetWarning()

            // 4. Kiểm tra nếu là cuối tháng
            if (isEndOfMonth()) {
                sendMonthlySummaryNotification()
            }

            // 5. Kiểm tra nếu là giờ buổi tối
            if (isEveningTime()) {
                sendDailyReminder()
            }

            // 6. Kiểm tra giao dịch lớn
            checkLargeTransactions()

            // 7. Kiểm tra giao dịch hôm nay
            checkTodayTransactions()

            Log.d(TAG, "Hoàn thành kiểm tra tất cả điều kiện tài chính")

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra điều kiện tài chính", e)
        }
    }

    /**
     * Đợi dữ liệu được load
     */
    private suspend fun awaitDataLoad() {
        Log.d(TAG, "Đang đợi dữ liệu load...")

        // Đợi 2 giây để dữ liệu load
        delay(2000)

        // Kiểm tra xem có dữ liệu không
        val budgetCount = withContext(Dispatchers.Main) {
            budgetViewModel.budgets.value.size
        }
        val transactionCount = withContext(Dispatchers.Main) {
            transactionViewModel.transactions.value.size
        }

        Log.d(TAG, "Dữ liệu đã load: $budgetCount budgets, $transactionCount transactions")
    }

    /**
     * Kiểm tra ngân sách vượt quá
     */
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

            Log.d(TAG, "Tìm thấy ${budgets.size} ngân sách vượt quá (active & trong kỳ)")

            if (budgets.isNotEmpty()) {
                Log.i(TAG, "Phát hiện ${budgets.size} ngân sách vượt quá")

                // Sửa: sử dụng list để xử lý category names
                val categoryDetails = budgets.mapNotNull { budget ->
                    withContext(Dispatchers.IO) {
                        val categoryName = getCategoryName(budget.categoryId)
                        val exceededAmount = budget.spent - budget.amount
                        categoryName?.let { name ->
                            "• $name: Vượt ${formatCurrency(exceededAmount)}"
                        }
                    }
                }.joinToString("\n")

                if (categoryDetails.isNotEmpty() && notificationPreferences.canSendBudgetNotification()) {
                    sendNotification(
                        title = "VƯỢT NGÂN SÁCH!",
                        message = "Có ${budgets.size} ngân sách đã vượt:\n$categoryDetails\n\nHãy kiểm soát chi tiêu ngay!"
                    )
                    Log.d(TAG, "Đã gửi thông báo vượt ngân sách")
                }
            } else {
                Log.d(TAG, "Không có ngân sách nào vượt quá")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra ngân sách vượt quá", e)
        }
    }

    /**
     * Kiểm tra ngân sách sắp vượt (>80%)
     */
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

            // Tìm ngân sách >80% và chưa vượt
            val warningBudgets = allBudgets.filter { budget ->
                val spentRatio = budget.spent / budget.amount
                spentRatio >= 0.8 && spentRatio < 1.0
            }

            Log.d(TAG, "Tìm thấy ${warningBudgets.size} ngân sách >80%")

            if (warningBudgets.isNotEmpty()) {
                Log.i(TAG, "Phát hiện ${warningBudgets.size} ngân sách sắp vượt (>80%)")

                // Sửa: sử dụng list để xử lý chi tiết
                val warningDetails = warningBudgets.take(3).mapNotNull { budget ->
                    withContext(Dispatchers.IO) {
                        val categoryName = getCategoryName(budget.categoryId)
                        val percentage = (budget.spent / budget.amount * 100).toInt()
                        val remaining = budget.amount - budget.spent
                        categoryName?.let { name ->
                            "• $name: $percentage% (còn ${formatCurrency(remaining)})"
                        }
                    }
                }.joinToString("\n")

                if (warningDetails.isNotEmpty() && notificationPreferences.canSendBudgetNotification()) {
                    sendNotification(
                        title = "Ngân sách sắp vượt!",
                        message = "Các ngân sách sắp đạt giới hạn:\n$warningDetails\n\nHãy kiểm soát chi tiêu!"
                    )
                    Log.d(TAG, "Đã gửi cảnh báo ngân sách sắp vượt")
                }
            } else {
                Log.d(TAG, "Không có ngân sách nào sắp vượt (>80%)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra cảnh báo ngân sách", e)
            Log.e(TAG, "Chi tiết lỗi: ${e.message}")
        }
    }

    /**
     * Kiểm tra giao dịch lớn
     */
    private suspend fun checkLargeTransactions() {
        try {
            Log.d(TAG, "Kiểm tra giao dịch lớn...")

            val recentTransactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value
                    .filter { !it.isIncome }
                    .sortedByDescending { parseDate(it.date) }
                    .take(10)
            }

            // Tìm giao dịch lớn trong 24h
            val now = System.currentTimeMillis()
            val oneDayAgo = now - (24 * 60 * 60 * 1000)

            val largeTransactions = recentTransactions.filter { transaction ->
                val transactionTime = parseDate(transaction.date).time
                transaction.amount >= LARGE_TRANSACTION_THRESHOLD &&
                        transactionTime > oneDayAgo
            }

            if (largeTransactions.isNotEmpty() && notificationPreferences.canSendTransactionNotification()) {
                Log.i(TAG, "Phát hiện ${largeTransactions.size} giao dịch lớn trong 24h")

                val latest = largeTransactions.first()
                // Sửa: sử dụng async để lấy category name
                val categoryName = withContext(Dispatchers.IO) {
                    getCategoryName(latest.categoryId) ?: latest.category
                }

                sendNotification(
                    title = "Giao dịch lớn phát hiện",
                    message = "Bạn vừa chi ${formatCurrency(latest.amount)} cho '${latest.title}' ($categoryName).\nHãy kiểm tra lại chi tiêu!"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra giao dịch lớn", e)
        }
    }

    /**
     * Kiểm tra giao dịch hôm nay
     */
    private suspend fun checkTodayTransactions() {
        try {
            Log.d(TAG, "Kiểm tra giao dịch hôm nay...")

            val today = getTodayDate()
            val todayTransactions = withContext(Dispatchers.Main) {
                transactionViewModel.transactions.value.filter { it.date == today }
            }

            val totalTodaySpending = todayTransactions
                .filter { !it.isIncome }
                .sumOf { it.amount }

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

            // Nhắc nhở nếu sau 18h và chưa có giao dịch
            if (todayTransactions.isEmpty() && currentHour >= EVENING_START_HOUR) {
                Log.i(TAG, "Chưa có giao dịch nào hôm nay (sau $EVENING_START_HOUR giờ)")

                // Sửa: Kiểm tra nếu canSendReminderNotification tồn tại, nếu không dùng canSendAINotification
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

            // Cảnh báo nếu chi tiêu hôm nay quá cao
            if (totalTodaySpending > 500000 && notificationPreferences.canSendBudgetNotification()) {
                Log.i(TAG, "Chi tiêu hôm nay cao: ${formatCurrency(totalTodaySpending)}")

                sendNotification(
                    title = "Chi tiêu hôm nay",
                    message = "Bạn đã chi ${formatCurrency(totalTodaySpending)} hôm nay.\nHãy theo dõi để không vượt ngân sách!"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra giao dịch hôm nay", e)
        }
    }

    /**
     * Gửi thông báo tổng kết tháng
     */
    private suspend fun sendMonthlySummaryNotification() {
        try {
            Log.d(TAG, "Chuẩn bị gửi tổng kết tháng...")

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

                val success = NotificationHelper.showNotification(
                    context = context,
                    title = "Tổng kết tháng",
                    message = message
                )

                if (success) {
                    Log.i(TAG, "Đã gửi thông báo tổng kết tháng")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi thông báo tổng kết tháng", e)
        }
    }

    /**
     * Gửi thông báo nhắc nhở hàng ngày
     */
    private suspend fun sendDailyReminder() {
        try {
            Log.d(TAG, "Chuẩn bị gửi nhắc nhở hàng ngày...")

            val context = applicationContext

            if (!NotificationHelper.hasNotificationPermission(context)) {
                return
            }

            // Sửa: Kiểm tra nếu canSendReminderNotification tồn tại
            val canRemind = try {
                notificationPreferences.canSendReminderNotification()
            } catch (e: NoSuchMethodError) {
                notificationPreferences.canSendAINotification()
            }

            if (canRemind) {
                val success = NotificationHelper.showNotification(
                    context = context,
                    title = "Nhắc nhở tài chính",
                    message = "Đừng quên ghi chép các giao dịch hôm nay để quản lý chi tiêu tốt hơn!"
                )

                if (success) {
                    Log.i(TAG, "Đã gửi thông báo nhắc nhở hàng ngày")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi thông báo nhắc nhở", e)
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
            Log.d(TAG, "Đã gửi notification: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi thông báo: $title", e)
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
            Log.e(TAG, "Lỗi kiểm tra budget period", e)
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
            Log.e(TAG, "Lỗi kiểm tra tháng: $dateString", e)
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
            Log.e(TAG, "Lỗi parse date: $dateString", e)
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