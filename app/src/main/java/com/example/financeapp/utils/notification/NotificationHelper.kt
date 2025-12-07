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
import android.util.Log

/**
 * Helper class để quản lý thông báo notification trong ứng dụng
 * Xử lý tạo channel, kiểm tra permission và hiển thị notification
 */
object NotificationHelper {

    // ==================== CONSTANTS ====================

    private const val TAG = "NotificationHelper"

    /** ID của notification channel cho thông báo chung */
    private const val CHANNEL_ID_GENERAL = "wendyai_channel"

    /** ID của notification channel cho cảnh báo khẩn cấp */
    private const val CHANNEL_ID_ALERTS = "wendyai_alerts_channel"

    /** ID của notification channel cho AI Butler */
    private const val CHANNEL_ID_AI = "wendy_ai_channel"

    /** Tên các channels */
    private const val CHANNEL_NAME_GENERAL = "Thông báo chung"
    private const val CHANNEL_NAME_ALERTS = "Cảnh báo tài chính"
    private const val CHANNEL_NAME_AI = "Wendy AI"

    /** Mô tả channels */
    private const val CHANNEL_DESC_GENERAL = "Thông báo từ ứng dụng quản lý tài chính"
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
                    importance = NotificationManager.IMPORTANCE_DEFAULT,
                    enableSound = true,
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    enableVibration = true,
                    vibrationPattern = longArrayOf(100, 200, 100, 200),
                    enableLights = true,
                    lightColor = android.graphics.Color.GREEN
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

                Log.d(TAG, "Đã tạo tất cả notification channels")
            } else {
                // Android < 8.0 không cần tạo channel
                Log.d(TAG, "Android version < O, không cần tạo channel")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tạo notification channels", e)
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

                // Lock screen visibility
                channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC

                // Show badge
                channel.setShowBadge(true)

                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Đã tạo channel: $channelName")
            } else {
                Log.d(TAG, "Channel $channelName đã tồn tại")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tạo channel $channelName", e)
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
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        enableSound: Boolean = true,
        enableVibration: Boolean = true
    ): Boolean {
        return try {
            // 1. Kiểm tra permission
            if (!hasNotificationPermission(context)) {
                Log.w(TAG, "Không có quyền notification")
                return false
            }

            // 2. Đảm bảo channels đã được tạo
            createAllChannels(context)

            // 3. Tạo PendingIntent để mở app khi click notification
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            // 4. Xây dựng notification
            val builder = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(autoCancel)
                .setPriority(priority)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent) // Mở app khi click
                .setWhen(System.currentTimeMillis()) // Thời gian hiện tại

            // 5. Thêm small icon
            try {
                // Sử dụng icon từ resources
                val iconResId = context.resources.getIdentifier(
                    "ic_notification",
                    "drawable",
                    context.packageName
                )
                if (iconResId != 0) {
                    builder.setSmallIcon(iconResId)
                } else {
                    // Fallback icon
                    builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                }
            } catch (e: Exception) {
                // Fallback icon
                builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            }

            // 6. Cấu hình âm thanh và rung (chỉ Android < O mới cần set ở đây)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                if (enableSound) {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    builder.setSound(soundUri)
                }

                if (enableVibration) {
                    val vibrationPattern = longArrayOf(0, 300, 200, 300)
                    builder.setVibrate(vibrationPattern)
                }
            }

            // 7. Thêm style cho notification dài
            if (message.length > 50) {
                val bigTextStyle = NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle(title)
                builder.setStyle(bigTextStyle)
            }

            // 8. Hiển thị notification
            val notificationManager = NotificationManagerCompat.from(context)

            if (!notificationManager.areNotificationsEnabled()) {
                Log.w(TAG, "Notifications bị tắt trong hệ thống")
                return false
            }

            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, builder.build())

            Log.d(TAG, "Đã hiển thị notification: $title")
            true

        } catch (e: SecurityException) {
            Log.e(TAG, "Lỗi permission khi hiển thị notification", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hiển thị notification", e)
            false
        }
    }

    /**
     * Hiển thị notification cảnh báo vượt ngân sách (có âm thanh báo động)
     */
    fun showBudgetAlertNotification(
        context: Context,
        title: String,
        message: String,
        details: String = ""
    ): Boolean {
        return try {
            val fullMessage = if (details.isNotEmpty()) "$message\n$details" else message

            showNotification(
                context = context,
                title = title,
                message = fullMessage,
                channelId = CHANNEL_ID_ALERTS,
                priority = NotificationCompat.PRIORITY_HIGH,
                enableSound = true,
                enableVibration = true
            )

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hiển thị budget alert", e)
            false
        }
    }

    /**
     * Hiển thị notification từ AI Butler
     */
    fun showAINotification(
        context: Context,
        title: String,
        message: String
    ): Boolean {
        return try {
            showNotification(
                context = context,
                title = title,
                message = message,
                channelId = CHANNEL_ID_AI,
                priority = NotificationCompat.PRIORITY_DEFAULT,
                enableSound = true,
                enableVibration = true
            )

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hiển thị AI notification", e)
            false
        }
    }

    /**
     * Simple notification method (cho backward compatibility)
     */
    fun showSimpleNotification(
        context: Context,
        title: String,
        message: String
    ): Boolean {
        return try {
            showNotification(
                context = context,
                title = title,
                message = message,
                channelId = CHANNEL_ID_GENERAL,
                enableSound = true,
                enableVibration = false // Không rung cho thông báo đơn giản
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hiển thị simple notification", e)
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
            Log.e(TAG, "Lỗi khi xóa notification", e)
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

            Log.d(TAG, "=== NOTIFICATION CHANNELS ===")
            channels.forEach { channel ->
                Log.d(TAG,
                    "Channel: ${channel.id}\n" +
                            "Name: ${channel.name}\n" +
                            "Importance: ${channel.importance}\n" +
                            "Sound: ${channel.sound}\n" +
                            "Vibration: ${channel.vibrationPattern?.joinToString()}\n" +
                            "Lights: ${channel.lightColor}"
                )
            }
            Log.d(TAG, "=== END CHANNELS ===")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi debug channels", e)
        }
    }
}