package com.example.financeapp.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.financeapp.screen.main.dashboard.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesDataStore(private val context: Context) {

    companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AVATAR = stringPreferencesKey("user_avatar")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    suspend fun saveUser(
        id: String,
        email: String?,
        name: String?,
        avatar: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = id
            prefs[USER_EMAIL] = email ?: ""
            prefs[USER_NAME] = name ?: ""
            prefs[USER_AVATAR] = avatar ?: ""
            prefs[IS_LOGGED_IN] = true
        }
    }

    val userFlow: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        if (prefs[IS_LOGGED_IN] == true) {
            UserSession(
                id = prefs[USER_ID] ?: "",
                email = prefs[USER_EMAIL] ?: "",
                name = prefs[USER_NAME] ?: "",
                avatar = prefs[USER_AVATAR] ?: ""
            )
        } else null
    }

    suspend fun clearUser() {
        context.dataStore.edit { it.clear() }
    }
}
