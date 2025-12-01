package com.example.financeapp

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.financeapp.viewmodel.LanguageViewModel

val LocalLanguageViewModel = staticCompositionLocalOf<LanguageViewModel> {
    error("No LanguageViewModel provided!")
}