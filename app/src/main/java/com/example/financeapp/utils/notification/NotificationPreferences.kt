package com.example.financeapp.utils.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

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
        // Keys
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val AI_NOTIFICATIONS_ENABLED = booleanPreferencesKey("ai_notifications_enabled")
        private val BUDGET_NOTIFICATIONS_ENABLED = booleanPreferencesKey("budget_notifications_enabled")
        private val TRANSACTION_NOTIFICATIONS_ENABLED = booleanPreferencesKey("transaction_notifications_enabled")
        private val REMINDER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("reminder_notifications_enabled")
        private val MONTHLY_SUMMARY_ENABLED = booleanPreferencesKey("monthly_summary_enabled")
    }

    // ==================== FLOW PROPERTIES ====================

    val notificationsEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[NOTIFICATIONS_ENABLED] ?: true }

    val aiNotificationsEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[AI_NOTIFICATIONS_ENABLED] ?: true }

    val budgetNotificationsEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[BUDGET_NOTIFICATIONS_ENABLED] ?: true }

    val transactionNotificationsEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[TRANSACTION_NOTIFICATIONS_ENABLED] ?: true }

    val reminderNotificationsEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[REMINDER_NOTIFICATIONS_ENABLED] ?: true }

    val monthlySummaryEnabled: Flow<Boolean> = context.notificationDataStore.data
        .map { preferences -> preferences[MONTHLY_SUMMARY_ENABLED] ?: true }

    // ==================== SUSPEND SETTER METHODS ====================

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setAINotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[AI_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setBudgetNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[BUDGET_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setTransactionNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[TRANSACTION_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setReminderNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[REMINDER_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setMonthlySummaryEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[MONTHLY_SUMMARY_ENABLED] = enabled
        }
    }

    // ==================== NON-SUSPEND SETTER METHODS (cho UI) ====================

    fun setNotificationsEnabledBlocking(enabled: Boolean) {
        runBlocking {
            setNotificationsEnabled(enabled)
        }
    }

    fun setAINotificationsEnabledBlocking(enabled: Boolean) {
        runBlocking {
            setAINotificationsEnabled(enabled)
        }
    }

    fun setBudgetNotificationsEnabledBlocking(enabled: Boolean) {
        runBlocking {
            setBudgetNotificationsEnabled(enabled)
        }
    }

    fun setTransactionNotificationsEnabledBlocking(enabled: Boolean) {
        runBlocking {
            setTransactionNotificationsEnabled(enabled)
        }
    }

    fun setReminderNotificationsEnabledBlocking(enabled: Boolean) {
        runBlocking {
            setReminderNotificationsEnabled(enabled)
        }
    }

    fun setMonthlySummaryEnabledBlocking(enabled: Boolean) {
        runBlocking {
            setMonthlySummaryEnabled(enabled)
        }
    }

    // ==================== SUSPEND CHECKING METHODS ====================

    suspend fun areNotificationsEnabled(): Boolean {
        return context.notificationDataStore.data
            .map { it[NOTIFICATIONS_ENABLED] ?: true }
            .first()
    }

    suspend fun canSendAINotification(): Boolean {
        return context.notificationDataStore.data
            .map {
                val generalEnabled = it[NOTIFICATIONS_ENABLED] ?: true
                val aiEnabled = it[AI_NOTIFICATIONS_ENABLED] ?: true
                generalEnabled && aiEnabled
            }
            .first()
    }

    suspend fun canSendBudgetNotification(): Boolean {
        return context.notificationDataStore.data
            .map {
                val generalEnabled = it[NOTIFICATIONS_ENABLED] ?: true
                val budgetEnabled = it[BUDGET_NOTIFICATIONS_ENABLED] ?: true
                generalEnabled && budgetEnabled
            }
            .first()
    }

    suspend fun canSendTransactionNotification(): Boolean {
        return context.notificationDataStore.data
            .map {
                val generalEnabled = it[NOTIFICATIONS_ENABLED] ?: true
                val transactionEnabled = it[TRANSACTION_NOTIFICATIONS_ENABLED] ?: true
                generalEnabled && transactionEnabled
            }
            .first()
    }

    suspend fun canSendReminderNotification(): Boolean {
        return context.notificationDataStore.data
            .map {
                val generalEnabled = it[NOTIFICATIONS_ENABLED] ?: true
                val reminderEnabled = it[REMINDER_NOTIFICATIONS_ENABLED] ?: true
                generalEnabled && reminderEnabled
            }
            .first()
    }

    suspend fun canSendMonthlySummary(): Boolean {
        return context.notificationDataStore.data
            .map {
                val generalEnabled = it[NOTIFICATIONS_ENABLED] ?: true
                val summaryEnabled = it[MONTHLY_SUMMARY_ENABLED] ?: true
                generalEnabled && summaryEnabled
            }
            .first()
    }

    // ==================== NON-SUSPEND CHECKING METHODS (cho Worker) ====================

    /**
     * NON-SUSPEND VERSION - Có thể gọi trực tiếp trong Worker
     */
    fun canSendAINotificationSync(): Boolean {
        return runBlocking {
            canSendAINotification()
        }
    }

    fun canSendBudgetNotificationSync(): Boolean {
        return runBlocking {
            canSendBudgetNotification()
        }
    }

    fun canSendTransactionNotificationSync(): Boolean {
        return runBlocking {
            canSendTransactionNotification()
        }
    }

    fun canSendReminderNotificationSync(): Boolean {
        return runBlocking {
            canSendReminderNotification()
        }
    }

    fun canSendMonthlySummarySync(): Boolean {
        return runBlocking {
            canSendMonthlySummary()
        }
    }

    fun areNotificationsEnabledSync(): Boolean {
        return runBlocking {
            areNotificationsEnabled()
        }
    }

    // ==================== GETTER METHODS (blocking) ====================

    fun getBudgetNotificationsEnabled(): Boolean {
        return runBlocking {
            context.notificationDataStore.data
                .map { it[BUDGET_NOTIFICATIONS_ENABLED] ?: true }
                .first()
        }
    }

    fun getTransactionNotificationsEnabled(): Boolean {
        return runBlocking {
            context.notificationDataStore.data
                .map { it[TRANSACTION_NOTIFICATIONS_ENABLED] ?: true }
                .first()
        }
    }

    fun getReminderNotificationsEnabled(): Boolean {
        return runBlocking {
            context.notificationDataStore.data
                .map { it[REMINDER_NOTIFICATIONS_ENABLED] ?: true }
                .first()
        }
    }

    fun getAINotificationsEnabled(): Boolean {
        return runBlocking {
            context.notificationDataStore.data
                .map { it[AI_NOTIFICATIONS_ENABLED] ?: true }
                .first()
        }
    }

    // ==================== BATCH OPERATIONS ====================

    suspend fun enableAllNotifications() {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = true
            preferences[AI_NOTIFICATIONS_ENABLED] = true
            preferences[BUDGET_NOTIFICATIONS_ENABLED] = true
            preferences[TRANSACTION_NOTIFICATIONS_ENABLED] = true
            preferences[REMINDER_NOTIFICATIONS_ENABLED] = true
            preferences[MONTHLY_SUMMARY_ENABLED] = true
        }
    }

    suspend fun disableAllNotifications() {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = false
            preferences[AI_NOTIFICATIONS_ENABLED] = false
            preferences[BUDGET_NOTIFICATIONS_ENABLED] = false
            preferences[TRANSACTION_NOTIFICATIONS_ENABLED] = false
            preferences[REMINDER_NOTIFICATIONS_ENABLED] = false
            preferences[MONTHLY_SUMMARY_ENABLED] = false
        }
    }

    suspend fun resetToDefaults() {
        enableAllNotifications()
    }

    // ==================== DEBUG METHODS ====================

    suspend fun getAllPreferences(): Map<String, Boolean> {
        return context.notificationDataStore.data.map { preferences ->
            mapOf(
                "notifications_enabled" to (preferences[NOTIFICATIONS_ENABLED] ?: true),
                "ai_notifications_enabled" to (preferences[AI_NOTIFICATIONS_ENABLED] ?: true),
                "budget_notifications_enabled" to (preferences[BUDGET_NOTIFICATIONS_ENABLED] ?: true),
                "transaction_notifications_enabled" to (preferences[TRANSACTION_NOTIFICATIONS_ENABLED] ?: true),
                "reminder_notifications_enabled" to (preferences[REMINDER_NOTIFICATIONS_ENABLED] ?: true),
                "monthly_summary_enabled" to (preferences[MONTHLY_SUMMARY_ENABLED] ?: true)
            )
        }.first()
    }

    fun getAllPreferencesSync(): Map<String, Boolean> {
        return runBlocking {
            getAllPreferences()
        }
    }

    /**
     * Phương thức tiện ích để kiểm tra nhanh trạng thái
     */
    fun printAllPreferences() {
        val prefs = getAllPreferencesSync()
        println("=== NOTIFICATION PREFERENCES ===")
        prefs.forEach { (key, value) ->
            println("$key: $value")
        }
        println("================================")
    }
}