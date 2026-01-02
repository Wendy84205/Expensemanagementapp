package com.example.financeapp.utils.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.financeapp.MainActivity
import com.example.financeapp.R

/**
 * Helper class để quản lý thông báo notification trong ứng dụng
 * Xử lý tạo channel, kiểm tra permission và hiển thị notification
 */
object NotificationHelper {

    // ==================== CONSTANTS ====================

    /** ID của notification channel cho thông báo chung */
    const val CHANNEL_ID_GENERAL = "wendyai_channel"

    /** ID của notification channel cho cảnh báo khẩn cấp */
    const val CHANNEL_ID_ALERTS = "wendyai_alerts_channel"

    /** ID của notification channel cho AI Butler */
    const val CHANNEL_ID_AI = "wendy_ai_channel"

    /** Tên các channels */
    private const val CHANNEL_NAME_GENERAL = "Wendy AI Finance"
    private const val CHANNEL_NAME_ALERTS = "Cảnh báo tài chính"
    private const val CHANNEL_NAME_AI = "Wendy AI Thông minh"

    /** Mô tả channels */
    private const val CHANNEL_DESC_GENERAL = "Thông báo từ ứng dụng Wendy AI Finance"
    private const val CHANNEL_DESC_ALERTS = "Cảnh báo vượt ngân sách và chi tiêu"
    private const val CHANNEL_DESC_AI = "Thông báo thông minh từ AI"

    // ==================== NOTIFICATION CHANNELS ====================

    /**
     * Tạo tất cả notification channels
     */
    fun createAllChannels(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 1. Channel cho thông báo chung
                createNotificationChannel(
                    context = context,
                    channelId = CHANNEL_ID_GENERAL,
                    channelName = CHANNEL_NAME_GENERAL,
                    channelDescription = CHANNEL_DESC_GENERAL,
                    importance = NotificationManager.IMPORTANCE_HIGH,
                    enableSound = true,
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    enableVibration = true,
                    vibrationPattern = longArrayOf(100, 200, 100, 200),
                    enableLights = true,
                    lightColor = ContextCompat.getColor(context, R.color.purple_500)
                )

                // 2. Channel cho cảnh báo khẩn cấp
                createNotificationChannel(
                    context = context,
                    channelId = CHANNEL_ID_ALERTS,
                    channelName = CHANNEL_NAME_ALERTS,
                    channelDescription = CHANNEL_DESC_ALERTS,
                    importance = NotificationManager.IMPORTANCE_HIGH,
                    enableSound = true,
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    enableVibration = true,
                    vibrationPattern = longArrayOf(0, 500, 250, 500),
                    enableLights = true,
                    lightColor = android.graphics.Color.RED
                )

                // 3. Channel cho AI Butler
                createNotificationChannel(
                    context = context,
                    channelId = CHANNEL_ID_AI,
                    channelName = CHANNEL_NAME_AI,
                    channelDescription = CHANNEL_DESC_AI,
                    importance = NotificationManager.IMPORTANCE_HIGH,
                    enableSound = true,
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    enableVibration = true,
                    vibrationPattern = longArrayOf(0, 300, 200, 300),
                    enableLights = true,
                    lightColor = android.graphics.Color.BLUE
                )
            }
        } catch (e: Exception) {
            // Không xử lý exception
        }
    }

    /**
     * Tạo một notification channel (chỉ Android O+)
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String,
        importance: Int,
        enableSound: Boolean,
        soundUri: Uri?,
        enableVibration: Boolean,
        vibrationPattern: LongArray?,
        enableLights: Boolean,
        lightColor: Int
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            // Kiểm tra xem channel đã tồn tại chưa
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                // Tạo channel mới
                val channel = NotificationChannel(channelId, channelName, importance)
                channel.description = channelDescription

                // Cấu hình âm thanh
                if (enableSound && soundUri != null) {
                    // Tạo AudioAttributes
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()

                    channel.setSound(soundUri, audioAttributes)
                } else {
                    channel.setSound(null, null)
                }

                // Cấu hình rung
                channel.enableVibration(enableVibration)
                if (enableVibration && vibrationPattern != null) {
                    channel.vibrationPattern = vibrationPattern
                }

                // Cấu hình đèn LED
                channel.enableLights(enableLights)
                if (enableLights) {
                    channel.lightColor = lightColor
                }

                // Lock screen visibility - QUAN TRỌNG: Hiển thị trên màn hình khóa
                channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC

                // Show badge
                channel.setShowBadge(true)

                // Hiển thị trên màn hình khóa
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    channel.setAllowBubbles(true)
                }

                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            // Không xử lý exception
        }
    }

    /**
     * Tạo notification channel (compatibility method)
     */
    fun createChannel(context: Context) {
        createAllChannels(context)
    }

    // ==================== PERMISSION CHECKING ====================

    /**
     * Kiểm tra permission notification
     */
    fun hasNotificationPermission(context: Context): Boolean {
        // Android 13+ cần POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    // ==================== NOTIFICATION METHODS ====================

    /**
     * Hiển thị notification với âm thanh và rung
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = CHANNEL_ID_GENERAL,
        autoCancel: Boolean = true,
        priority: Int = NotificationCompat.PRIORITY_HIGH,
        enableSound: Boolean = true,
        enableVibration: Boolean = true,
        notificationId: Int = System.currentTimeMillis().toInt()
    ): Boolean {
        return try {
            // 1. Kiểm tra permission
            if (!hasNotificationPermission(context)) {
                // Vẫn tiếp tục, có thể hiển thị được trên một số device
            }

            // 2. Đảm bảo channels đã được tạo
            createAllChannels(context)

            // 3. Tạo PendingIntent để mở app khi click notification
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra("from_notification", true)
            intent.putExtra("notification_title", title)
            intent.putExtra("notification_message", message)
            intent.putExtra("notification_channel", channelId)

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 4. Xây dựng notification
            val builder = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(autoCancel)
                .setPriority(priority)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setContentIntent(pendingIntent) // Mở app khi click
                .setWhen(System.currentTimeMillis()) // Thời gian hiện tại
                .setShowWhen(true) // Hiển thị thời gian
                .setColor(ContextCompat.getColor(context, R.color.purple_500)) // Màu accent

            // 5. Thêm small icon
            try {
                // Thử lấy icon từ drawable
                val iconResId = R.drawable.ic_notification_wendy
                if (iconResId != 0) {
                    builder.setSmallIcon(iconResId)
                } else {
                    // Fallback icon
                    builder.setSmallIcon(R.drawable.ic_logo_wendy_ai)
                }
            } catch (e: Exception) {
                // Fallback icon
                builder.setSmallIcon(R.drawable.ic_logo_wendy_ai)
            }

            // 6. Cấu hình âm thanh và rung (chỉ Android < O mới cần set ở đây)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                if (enableSound) {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    builder.setSound(soundUri)
                }

                if (enableVibration) {
                    val vibrationPattern = when(channelId) {
                        CHANNEL_ID_ALERTS -> longArrayOf(0, 500, 250, 500)
                        else -> longArrayOf(0, 300, 200, 300)
                    }
                    builder.setVibrate(vibrationPattern)
                }
            }

            // 7. Thêm style cho notification dài
            if (message.length > 50) {
                val bigTextStyle = NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle(title)
                    .setSummaryText("Wendy AI Finance")
                builder.setStyle(bigTextStyle)
            }

            // 8. Hiển thị notification
            val notificationManager = NotificationManagerCompat.from(context)

            notificationManager.notify(notificationId, builder.build())
            true

        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Hiển thị notification cảnh báo vượt ngân sách (có âm thanh báo động)
     */
    fun showBudgetAlertNotification(
        context: Context,
        categoryName: String,
        spentAmount: Double,
        budgetAmount: Double,
        exceededAmount: Double
    ): Boolean {
        return try {
            val title = "⚠️ VƯỢT NGÂN SÁCH: $categoryName"
            val message = """
                |Bạn đã vượt ngân sách!
                |Đã chi: ${formatCurrency(spentAmount)}
                |Ngân sách: ${formatCurrency(budgetAmount)}
                |Vượt quá: ${formatCurrency(exceededAmount)}
            """.trimMargin()

            showNotification(
                context = context,
                title = title,
                message = message,
                channelId = CHANNEL_ID_ALERTS,
                priority = NotificationCompat.PRIORITY_MAX,
                enableSound = true,
                enableVibration = true
            )

        } catch (e: Exception) {
            false
        }
    }

    /**
     * Hiển thị notification cảnh báo sắp vượt ngân sách
     */
    fun showBudgetWarningNotification(
        context: Context,
        categoryName: String,
        spentAmount: Double,
        budgetAmount: Double,
        percentage: Int
    ): Boolean {
        return try {
            val title = "📊 SẮP VƯỢT NGÂN SÁCH: $categoryName"
            val message = """
                |$categoryName đã dùng $percentage% ngân sách
                |Đã chi: ${formatCurrency(spentAmount)}
                |Ngân sách: ${formatCurrency(budgetAmount)}
                |Còn lại: ${formatCurrency(budgetAmount - spentAmount)}
            """.trimMargin()

            showNotification(
                context = context,
                title = title,
                message = message,
                channelId = CHANNEL_ID_GENERAL,
                priority = NotificationCompat.PRIORITY_HIGH,
                enableSound = true,
                enableVibration = true
            )

        } catch (e: Exception) {
            false
        }
    }

    /**
     * Hiển thị notification từ AI Butler
     */
    fun showAINotification(
        context: Context,
        title: String,
        message: String,
        showSound: Boolean = true
    ): Boolean {
        return try {
            showNotification(
                context = context,
                title = "🤖 $title",
                message = message,
                channelId = CHANNEL_ID_AI,
                priority = NotificationCompat.PRIORITY_DEFAULT,
                enableSound = showSound,
                enableVibration = false
            )

        } catch (e: Exception) {
            false
        }
    }

    /**
     * Hiển thị notification cho recurring expense
     */
    fun showRecurringExpenseNotification(
        context: Context,
        expenseTitle: String,
        amount: Double,
        frequency: String
    ): Boolean {
        return try {
            val title = "🔄 Đã tạo giao dịch định kỳ"
            val message = """
                |$expenseTitle: ${formatCurrency(amount)}
                |Tần suất: $frequency
                |Đã được thêm vào danh sách giao dịch
            """.trimMargin()

            showNotification(
                context = context,
                title = title,
                message = message,
                channelId = CHANNEL_ID_GENERAL,
                priority = NotificationCompat.PRIORITY_DEFAULT,
                enableSound = true,
                enableVibration = false
            )

        } catch (e: Exception) {
            false
        }
    }

    /**
     * Hiển thị notification cho savings goal
     */
    fun showSavingsNotification(
        context: Context,
        goalName: String,
        progress: Int,
        remainingDays: Long,
        remainingAmount: Double
    ): Boolean {
        return try {
            val title = if (remainingDays <= 7) {
                "⏰ $goalName SẮP ĐẾN HẠN!"
            } else {
                "💰 $goalName - Tiến độ: $progress%"
            }

            val message = if (remainingDays > 0) {
                """
                |Còn $remainingDays ngày
                |Cần thêm: ${formatCurrency(remainingAmount)}
                |Tiến độ: $progress%
                """.trimMargin()
            } else {
                """
                |Mục tiêu đã đến hạn!
                |Cần hoàn thành: ${formatCurrency(remainingAmount)}
                |Tiến độ: $progress%
                """.trimMargin()
            }

            showNotification(
                context = context,
                title = title,
                message = message,
                channelId = CHANNEL_ID_AI,
                priority = NotificationCompat.PRIORITY_HIGH,
                enableSound = true,
                enableVibration = remainingDays <= 3
            )

        } catch (e: Exception) {
            false
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Kiểm tra xem notifications có được bật trong hệ thống không
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Xóa tất cả notification
     */
    fun cancelAllNotifications(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (e: Exception) {
            // Không xử lý exception
        }
    }

    /**
     * Xóa notification theo ID
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (e: Exception) {
            // Không xử lý exception
        }
    }

    /**
     * Format currency in VND format
     */
    private fun formatCurrency(amount: Double): String {
        return try {
            val formatter = java.text.NumberFormat.getInstance(java.util.Locale.getDefault())
            "${formatter.format(amount)}đ"
        } catch (e: Exception) {
            "${amount.toInt()}đ"
        }
    }

    /**
     * Debug: In thông tin channels
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    fun debugChannels(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            val channels = notificationManager.notificationChannels
        } catch (e: Exception) {
            // Không xử lý exception
        }
    }
}