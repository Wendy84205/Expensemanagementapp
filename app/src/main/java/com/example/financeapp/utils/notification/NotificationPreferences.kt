package com.example.financeapp.utils.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// ==================== DATASTORE EXTENSION ====================

/**
 * Extension property để truy cập DataStore cho notification preferences
 */
private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences"
)

// ==================== NOTIFICATION PREFERENCES CLASS ====================

/**
 * Class quản lý cài đặt notification sử dụng DataStore
 * Lưu trữ và truy xuất các preference liên quan đến notification
 */
class NotificationPreferences(private val context: Context) {

    // ==================== COMPANION OBJECT ====================

    companion object {
        /**
         * Key cho preference "notifications_enabled"
         * Giá trị mặc định là true (notification được bật)
         */
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

        /**
         * Key cho preference "ai_notifications_enabled" (cho thông báo từ AI)
         */
        val AI_NOTIFICATIONS_ENABLED = booleanPreferencesKey("ai_notifications_enabled")

        /**
         * Key cho preference "budget_notifications_enabled" (cho thông báo ngân sách)
         */
        val BUDGET_NOTIFICATIONS_ENABLED = booleanPreferencesKey("budget_notifications_enabled")

        /**
         * Key cho preference "transaction_notifications_enabled" (cho thông báo giao dịch)
         */
        val TRANSACTION_NOTIFICATIONS_ENABLED = booleanPreferencesKey("transaction_notifications_enabled")
    }

    // ==================== FLOW PROPERTIES ====================

    /**
     * Flow cho trạng thái bật/tắt notification chung
     * Giá trị mặc định: true (bật)
     */
    val notificationsEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true
        }

    /**
     * Flow cho trạng thái bật/tắt notification từ AI
     * Giá trị mặc định: true (bật)
     */
    val aiNotificationsEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences ->
            preferences[AI_NOTIFICATIONS_ENABLED] ?: true
        }

    /**
     * Flow cho trạng thái bật/tắt notification về ngân sách
     * Giá trị mặc định: true (bật)
     */
    val budgetNotificationsEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences ->
            preferences[BUDGET_NOTIFICATIONS_ENABLED] ?: true
        }

    /**
     * Flow cho trạng thái bật/tắt notification về giao dịch
     * Giá trị mặc định: true (bật)
     */
    val transactionNotificationsEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences ->
            preferences[TRANSACTION_NOTIFICATIONS_ENABLED] ?: true
        }

    /**
     * Flow cho trạng thái tất cả notification types
     * @return Map chứa trạng thái của tất cả loại notification
     */
    val allNotificationPreferences: Flow<Map<String, Boolean>> =
        context.notificationDataStore.data.map { preferences ->
            mapOf(
                "notifications_enabled" to (preferences[NOTIFICATIONS_ENABLED] ?: true),
                "ai_notifications_enabled" to (preferences[AI_NOTIFICATIONS_ENABLED] ?: true),
                "budget_notifications_enabled" to (preferences[BUDGET_NOTIFICATIONS_ENABLED] ?: true),
                "transaction_notifications_enabled" to (preferences[TRANSACTION_NOTIFICATIONS_ENABLED] ?: true)
            )
        }

    // ==================== SETTER METHODS ====================

    /**
     * Cài đặt trạng thái bật/tắt notification chung
     * @param enabled true để bật notification, false để tắt
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Cài đặt trạng thái bật/tắt notification từ AI
     * @param enabled true để bật, false để tắt
     */
    suspend fun setAINotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[AI_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Cài đặt trạng thái bật/tắt notification về ngân sách
     * @param enabled true để bật, false để tắt
     */
    suspend fun setBudgetNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[BUDGET_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Cài đặt trạng thái bật/tắt notification về giao dịch
     * @param enabled true để bật, false để tắt
     */
    suspend fun setTransactionNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[TRANSACTION_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Cài đặt tất cả notification preferences cùng lúc
     * @param settings Map chứa các cài đặt notification
     */
    suspend fun setAllNotificationPreferences(settings: Map<String, Boolean>) {
        context.notificationDataStore.edit { preferences ->
            settings.forEach { (key, value) ->
                when (key) {
                    "notifications_enabled" -> preferences[NOTIFICATIONS_ENABLED] = value
                    "ai_notifications_enabled" -> preferences[AI_NOTIFICATIONS_ENABLED] = value
                    "budget_notifications_enabled" -> preferences[BUDGET_NOTIFICATIONS_ENABLED] = value
                    "transaction_notifications_enabled" -> preferences[TRANSACTION_NOTIFICATIONS_ENABLED] = value
                }
            }
        }
    }

    // ==================== CHECKING METHODS ====================

    /**
     * Kiểm tra xem notification có được bật không (dùng cho synchronous check)
     * @return true nếu notification được bật
     */
    suspend fun areNotificationsEnabledSync(): Boolean {
        return try {
            context.notificationDataStore.data
                .map { it[NOTIFICATIONS_ENABLED] ?: true }
                .first()
        } catch (e: Exception) {
            true // Mặc định true nếu có lỗi
        }
    }

    /**
     * Kiểm tra xem có thể gửi notification từ AI không
     * @return true nếu cả notification chung và AI notification đều được bật
     */
    suspend fun canSendAINotification(): Boolean {
        return try {
            context.notificationDataStore.data
                .map {
                    val generalEnabled = it[NOTIFICATIONS_ENABLED] ?: true
                    val aiEnabled = it[AI_NOTIFICATIONS_ENABLED] ?: true
                    generalEnabled && aiEnabled
                }
                .first()
        } catch (e: Exception) {
            true // Mặc định true nếu có lỗi
        }
    }

    /**
     * Kiểm tra xem có thể gửi notification về ngân sách không
     * @return true nếu cả notification chung và budget notification đều được bật
     */
    suspend fun canSendBudgetNotification(): Boolean {
        return try {
            context.notificationDataStore.data
                .map {
                    val generalEnabled = it[NOTIFICATIONS_ENABLED] ?: true
                    val budgetEnabled = it[BUDGET_NOTIFICATIONS_ENABLED] ?: true
                    generalEnabled && budgetEnabled
                }
                .first()
        } catch (e: Exception) {
            true // Mặc định true nếu có lỗi
        }
    }

    /**
     * Kiểm tra xem có thể gửi notification về giao dịch không
     * @return true nếu cả notification chung và transaction notification đều được bật
     */
    suspend fun canSendTransactionNotification(): Boolean {
        return try {
            context.notificationDataStore.data
                .map {
                    val generalEnabled = it[NOTIFICATIONS_ENABLED] ?: true
                    val transactionEnabled = it[TRANSACTION_NOTIFICATIONS_ENABLED] ?: true
                    generalEnabled && transactionEnabled
                }
                .first()
        } catch (e: Exception) {
            true // Mặc định true nếu có lỗi
        }
    }

    // ==================== RESET METHODS ====================

    /**
     * Reset tất cả notification preferences về giá trị mặc định (true)
     */
    suspend fun resetToDefaults() {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = true
            preferences[AI_NOTIFICATIONS_ENABLED] = true
            preferences[BUDGET_NOTIFICATIONS_ENABLED] = true
            preferences[TRANSACTION_NOTIFICATIONS_ENABLED] = true
        }
    }

    /**
     * Tắt tất cả notification
     */
    suspend fun disableAllNotifications() {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = false
            preferences[AI_NOTIFICATIONS_ENABLED] = false
            preferences[BUDGET_NOTIFICATIONS_ENABLED] = false
            preferences[TRANSACTION_NOTIFICATIONS_ENABLED] = false
        }
    }

    /**
     * Bật tất cả notification
     */
    suspend fun enableAllNotifications() {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = true
            preferences[AI_NOTIFICATIONS_ENABLED] = true
            preferences[BUDGET_NOTIFICATIONS_ENABLED] = true
            preferences[TRANSACTION_NOTIFICATIONS_ENABLED] = true
        }
    }
}