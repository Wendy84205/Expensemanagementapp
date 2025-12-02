package com.example.financeapp.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aiPrefs by preferencesDataStore(name = "ai_prefs")

class AIDataStore(private val context: Context) {
    companion object {
        private val KEY_AI_NOTIFICATIONS = booleanPreferencesKey("ai_notifications_enabled")
    }

    val aiNotificationsEnabled: Flow<Boolean> =
        context.aiPrefs.data.map { prefs -> prefs[KEY_AI_NOTIFICATIONS] ?: true }

    suspend fun setAiNotificationsEnabled(enabled: Boolean) {
        context.aiPrefs.edit { prefs -> prefs[KEY_AI_NOTIFICATIONS] = enabled }
    }
}
