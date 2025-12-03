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
import com.example.financeapp.utils.notification.NotificationHelper
import com.example.financeapp.utils.notification.NotificationPreferences
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Worker để chạy kiểm tra định kỳ trong background
 * Kiểm tra các điều kiện tài chính và gửi thông báo khi cần
 * Chạy mỗi 12 giờ hoặc khi có mạng
 */
class AIButlerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AIButlerWorker"

        /**
         * Lên lịch worker để chạy định kỳ
         * @param context Context của ứng dụng
         */
        fun schedule(context: Context) {
            try {
                // Tạo work request: chạy mỗi 12 giờ, linh hoạt trong khoảng 1 giờ
                val workRequest = PeriodicWorkRequestBuilder<AIButlerWorker>(
                    12, TimeUnit.HOURS,  // Chạy mỗi 12 giờ
                    1, TimeUnit.HOURS    // Flex window: 1 giờ
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .build()

                // Enqueue work với unique name để tránh duplicate
                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                        "ai_butler_work",
                        ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                    )

                Log.d(TAG, "AI Butler Worker đã được lên lịch")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi lên lịch worker: ${e.message}")
            }
        }

        /**
         * Hủy lịch trình của worker
         * @param context Context của ứng dụng
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("ai_butler_work")
            Log.d(TAG, "AI Butler Worker đã bị hủy")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "AI Butler Worker bắt đầu chạy")

            // Kiểm tra và gửi notification
            checkAndSendNotifications()

            Log.d(TAG, "AI Butler Worker hoàn thành")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong AI Butler Worker: ${e.message}", e)
            Result.failure()
        }
    }

    /**
     * Kiểm tra các điều kiện và gửi thông báo
     */
    private suspend fun checkAndSendNotifications() {
        try {
            val context = applicationContext

            // Kiểm tra permission notification
            if (!NotificationHelper.hasNotificationPermission(context)) {
                Log.d(TAG, "Không có quyền notification, bỏ qua")
                return
            }

            // Kiểm tra notification preferences
            val notificationPrefs = NotificationPreferences(context)
            if (!notificationPrefs.areNotificationsEnabledSync()) {
                Log.d(TAG, "Notification đã bị tắt trong cài đặt app")
                return
            }

            // Đảm bảo notification channel được tạo
            NotificationHelper.createChannel(context)

            // Kiểm tra các điều kiện tài chính
            checkFinancialConditions()

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra và gửi notification: ${e.message}", e)
        }
    }

    /**
     * Kiểm tra các điều kiện tài chính
     */
    private suspend fun checkFinancialConditions() {
        // Lưu ý: Trong thực tế, bạn cần truy cập ViewModel để lấy dữ liệu
        // Ở đây chỉ là logic mẫu

        Log.d(TAG, "Đang kiểm tra điều kiện tài chính...")

        // Ví dụ: Kiểm tra nếu là cuối tháng
        if (isEndOfMonth()) {
            sendMonthlySummaryNotification()
        }

        // Ví dụ: Kiểm tra nếu là giờ cao điểm (18:00)
        if (isEveningTime()) {
            sendDailyReminder()
        }
    }

    /**
     * Gửi thông báo tổng kết tháng
     */
    private suspend fun sendMonthlySummaryNotification() {
        try {
            val context = applicationContext

            // Kiểm tra permission
            if (!NotificationHelper.hasNotificationPermission(context)) {
                return
            }

            // Tạo và hiển thị notification
            val success = NotificationHelper.showNotification(
                context = context,
                title = "📊 Tổng kết tháng",
                message = "Tháng này bạn đã chi tiêu thế nào? Hãy kiểm tra báo cáo tài chính!"
            )

            if (success) {
                Log.d(TAG, "Đã gửi thông báo tổng kết tháng")
            } else {
                Log.d(TAG, "Không thể gửi thông báo tổng kết tháng")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi thông báo tổng kết tháng: ${e.message}")
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

            val success = NotificationHelper.showNotification(
                context = context,
                title = "💡 Nhắc nhở tài chính",
                message = "Đừng quên ghi chép các giao dịch hôm nay để quản lý chi tiêu tốt hơn!"
            )

            if (success) {
                Log.d(TAG, "Đã gửi thông báo nhắc nhở hàng ngày")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi thông báo nhắc nhở: ${e.message}")
        }
    }

    /**
     * Kiểm tra xem có phải cuối tháng không
     * @return true nếu là ngày cuối tháng
     */
    private fun isEndOfMonth(): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Kiểm tra nếu là 3 ngày cuối tháng
        return today >= (lastDay - 2)
    }

    /**
     * Kiểm tra xem có phải giờ tối (18:00 - 20:00) không
     * @return true nếu là giờ tối
     */
    private fun isEveningTime(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // Kiểm tra nếu là giờ tối (18:00 - 20:00)
        return hour in 18..20
    }
}