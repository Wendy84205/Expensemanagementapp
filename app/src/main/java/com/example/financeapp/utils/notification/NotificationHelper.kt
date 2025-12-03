package com.example.financeapp.utils.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Helper class để quản lý thông báo notification trong ứng dụng
 * Xử lý tạo channel, kiểm tra permission và hiển thị notification
 */
object NotificationHelper {

    // ==================== CONSTANTS ====================

    /** ID của notification channel */
    private const val CHANNEL_ID = "wendyai_notification_channel"

    /** Tên của notification channel */
    private const val CHANNEL_NAME = "WendyAI Notification"

    /** Mô tả của notification channel */
    private const val CHANNEL_DESCRIPTION = "Thông báo từ ứng dụng quản lý tài chính"

    /** ID cho notification (sử dụng timestamp để đảm bảo unique) */
    private var notificationId = System.currentTimeMillis().toInt()

    // ==================== NOTIFICATION CHANNEL ====================

    /**
     * Tạo notification channel (bắt buộc từ Android 8.0+)
     * Phải gọi method này trước khi hiển thị bất kỳ notification nào
     *
     * @param context Context của ứng dụng
     */
    fun createChannel(context: Context) {
        // Chỉ cần tạo channel từ Android 8.0 (API level 26) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

                // Kiểm tra xem channel đã tồn tại chưa
                val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (existingChannel == null) {
                    // Tạo channel mới
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH // Quan trọng, sẽ có âm thanh
                    )
                    channel.description = CHANNEL_DESCRIPTION

                    // Thiết lập các tính năng cho channel
                    channel.enableVibration(true) // Bật rung
                    channel.enableLights(true) // Bật đèn LED

                    notificationManager.createNotificationChannel(channel)
                }
            } catch (e: Exception) {
                // Log error (trong thực tế nên dùng Timber hoặc Log)
                e.printStackTrace()
            }
        }
    }

    // ==================== PERMISSION CHECKING ====================

    /**
     * Kiểm tra xem ứng dụng có quyền hiển thị notification không
     * Từ Android 13 (API level 33) trở lên cần runtime permission
     *
     * @param context Context của ứng dụng
     * @return true nếu có quyền hiển thị notification
     */
    fun hasNotificationPermission(context: Context): Boolean {
        // Android 13+ cần POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        // Android 12 trở xuống không cần runtime permission cho notification
        return true
    }

    /**
     * Kiểm tra và yêu cầu permission nếu cần (method này nên được gọi từ Activity/Fragment)
     *
     * @param context Context của ứng dụng (nên là Activity)
     * @param requestCode Mã yêu cầu permission
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestNotificationPermission(context: Context, requestCode: Int) {
        // Kiểm tra xem có cần request permission không
        if (!hasNotificationPermission(context)) {
            // Nên gọi ActivityCompat.requestPermissions từ Activity
            // Thực tế method này nên được triển khai trong Activity
        }
    }

    // ==================== NOTIFICATION DISPLAY ====================

    /**
     * Hiển thị notification
     *
     * @param context Context của ứng dụng
     * @param title Tiêu đề của notification
     * @param message Nội dung của notification
     * @param autoCancel Có tự động đóng khi user chạm vào không (mặc định: true)
     * @return true nếu notification được hiển thị thành công
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        autoCancel: Boolean = true
    ): Boolean {
        try {
            // 1. Kiểm tra permission
            if (!hasNotificationPermission(context)) {
                return false
            }

            // 2. Đảm bảo notification channel đã được tạo
            createChannel(context)

            // 3. Xây dựng notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(autoCancel)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)

            // 4. Thêm small icon (bắt buộc từ Android 5.0+)
            // Lưu ý: Trong thực tế cần thay ic_launcher_foreground bằng icon thật
            try {
                val smallIcon = context.applicationInfo.icon
                if (smallIcon != 0) {
                    builder.setSmallIcon(smallIcon)
                } else {
                    // Fallback icon
                    builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                }
            } catch (e: Exception) {
                // Fallback nếu không lấy được icon
                builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            }

            // 5. Thêm style cho notification dài
            if (message.length > 50) {
                val bigTextStyle = NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle(title)
                builder.setStyle(bigTextStyle)
            }

            // 6. Hiển thị notification
            val notificationManager = NotificationManagerCompat.from(context)

            // Kiểm tra xem notification có được bật không
            if (!notificationManager.areNotificationsEnabled()) {
                return false
            }

            // Tăng notification ID để mỗi notification là unique
            notificationId++
            if (notificationId > 1000000) {
                notificationId = 1
            }

            notificationManager.notify(notificationId, builder.build())
            return true

        } catch (e: SecurityException) {
            // Lỗi permission
            e.printStackTrace()
            return false
        } catch (e: Exception) {
            // Lỗi khác
            e.printStackTrace()
            return false
        }
    }

    /**
     * Hiển thị notification với nội dung chi tiết (dành cho AI assistant)
     *
     * @param context Context của ứng dụng
     * @param title Tiêu đề
     * @param message Nội dung chính
     * @param details Chi tiết bổ sung (hiển thị trong expanded view)
     * @return true nếu thành công
     */
    fun showDetailedNotification(
        context: Context,
        title: String,
        message: String,
        details: String
    ): Boolean {
        try {
            if (!hasNotificationPermission(context)) {
                return false
            }

            createChannel(context)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)

            // Thêm small icon
            try {
                val smallIcon = context.applicationInfo.icon
                if (smallIcon != 0) {
                    builder.setSmallIcon(smallIcon)
                } else {
                    builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                }
            } catch (e: Exception) {
                builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            }

            // Sử dụng BigTextStyle cho nội dung chi tiết
            val bigTextStyle = NotificationCompat.BigTextStyle()
                .bigText("$message\n\n$details")
                .setBigContentTitle(title)
            builder.setStyle(bigTextStyle)

            // Thêm action nếu cần
            // builder.addAction(...)

            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                return false
            }

            notificationId++
            notificationManager.notify(notificationId, builder.build())
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Xóa tất cả notification của ứng dụng
     *
     * @param context Context của ứng dụng
     */
    fun cancelAllNotifications(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancelAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Xóa notification theo ID
     *
     * @param context Context của ứng dụng
     * @param notificationId ID của notification cần xóa
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(notificationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Kiểm tra xem notifications có được bật trong hệ thống không
     *
     * @param context Context của ứng dụng
     * @return true nếu notification được bật
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.areNotificationsEnabled()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Lấy danh sách notification channels
     * (Chỉ dành cho debug/testing)
     *
     * @param context Context của ứng dụng
     * @return Danh sách channel IDs
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    fun getNotificationChannels(context: Context): List<String> {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.notificationChannels.map { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }
}