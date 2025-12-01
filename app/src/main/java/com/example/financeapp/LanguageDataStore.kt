package com.example.financeapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.languageDataStore by preferencesDataStore(name = "language_prefs")

class LanguageDataStore(private val context: Context) {
    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("selected_language")
    }

    val selectedLanguage: Flow<String> = context.languageDataStore.data.map { prefs ->
        prefs[LANGUAGE_KEY] ?: "Tiếng Việt"
    }

    suspend fun saveLanguage(language: String) {
        context.languageDataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language
        }
    }
}
