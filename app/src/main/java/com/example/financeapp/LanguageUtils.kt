package com.example.financeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun rememberLanguageText(key: String, defaultText: String = key): String {
    val languageViewModel = LocalLanguageViewModel.current
    return remember(key, languageViewModel.getCurrentLanguageCode()) {
        languageViewModel.getTranslation(key, "vi") ?: defaultText
    }
}

@Composable
fun getTranslatedText(key: String): String {
    val languageViewModel = LocalLanguageViewModel.current
    return languageViewModel.getTranslation(key, "vi")
}