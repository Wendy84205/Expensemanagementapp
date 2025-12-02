package com.example.financeapp.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.rememberLanguageText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(navController: NavController, languageViewModel: LanguageViewModel) {
    val context = LocalContext.current
    val languageViewModel = LocalLanguageViewModel.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentLanguageCode by languageViewModel.languageCode.collectAsState()
    val availableLanguages = languageViewModel.getAvailableLanguages()

    val screenTitle = rememberLanguageText("language_settings")
    val currentLanguageText = rememberLanguageText("current_language")
    val chooseLanguageText = rememberLanguageText("choose_language")
    val languageSavedText = rememberLanguageText("language_saved")
    val languageResetText = rememberLanguageText("language_reset")
    val saveText = rememberLanguageText("save")
    val resetText = rememberLanguageText("reset")
    val infoText = rememberLanguageText("language_change_info")

    // Colors - Giữ nguyên phong cách HelpScreen
    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White
    val textColor = Color(0xFF333333)
    val subtitleColor = Color(0xFF666666)
    val accentColor = Color(0xFFED8936)
    val successColor = Color(0xFF4CAF50)

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                title = screenTitle,
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = backgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ngôn ngữ hiện tại
            item {
                SettingsCard(title = currentLanguageText) {
                    CurrentLanguageItem(
                        languageName = languageViewModel.getCurrentLanguageName(),
                        nativeName = getLanguageNativeName(currentLanguageCode),
                        primaryColor = primaryColor
                    )
                }
            }

            // Chọn ngôn ngữ
            item {
                SettingsCard(title = chooseLanguageText) {
                    availableLanguages.forEachIndexed { index, language ->
                        LanguageOptionItem(
                            languageName = language.name,
                            nativeName = getLanguageNativeName(language.code),
                            languageCode = language.code,
                            isSelected = currentLanguageCode == language.code,
                            onLanguageSelected = {
                                languageViewModel.changeLanguage(language.code)
                                context.saveLanguageToPreferences(language.code)

                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "$languageSavedText: ${language.name}"
                                    )
                                }
                            },
                            primaryColor = primaryColor
                        )

                        if (index < availableLanguages.size - 1) {
                            Divider(color = Color(0xFFEEEEEE))
                        }
                    }
                }
            }

            // Hành động
            item {
                SettingsCard(title = rememberLanguageText("actions")) {
                    ActionButtons(
                        onReset = {
                            languageViewModel.changeLanguage("vi")
                            context.saveLanguageToPreferences("vi")

                            scope.launch {
                                snackbarHostState.showSnackbar(languageResetText)
                            }
                        },
                        onSave = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "$languageSavedText: ${languageViewModel.getCurrentLanguageName()}"
                                )
                                navController.popBackStack()
                            }
                        },
                        resetText = resetText,
                        saveText = saveText,
                        primaryColor = primaryColor,
                        accentColor = accentColor
                    )
                }
            }

            // Thông tin
            item {
                SettingsCard(title = rememberLanguageText("information")) {
                    InfoTipItem(
                        text = infoText,
                        primaryColor = primaryColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopAppBar(
    title: String,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = rememberLanguageText("back"),
                    tint = Color(0xFF333333)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
private fun SettingsCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
                )
            }
            content()
        }
    }
}

@Composable
private fun CurrentLanguageItem(
    languageName: String,
    nativeName: String,
    primaryColor: Color = Color(0xFF2196F3)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(primaryColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                languageName,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                nativeName,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }

        Box(
            modifier = Modifier
                .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                rememberLanguageText("current"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = primaryColor
            )
        }
    }
}

@Composable
private fun LanguageOptionItem(
    languageName: String,
    nativeName: String,
    languageCode: String,
    isSelected: Boolean,
    onLanguageSelected: () -> Unit,
    primaryColor: Color = Color(0xFF2196F3)
) {
    Surface(
        onClick = onLanguageSelected,
        color = if (isSelected) primaryColor.copy(alpha = 0.05f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(primaryColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    getLanguageFlagEmoji(languageCode),
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    languageName,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    nativeName,
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onLanguageSelected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = primaryColor,
                    unselectedColor = Color(0xFFCCCCCC)
                )
            )
        }
    }
}

@Composable
private fun ActionButtons(
    onReset: () -> Unit,
    onSave: () -> Unit,
    resetText: String,
    saveText: String,
    primaryColor: Color = Color(0xFF2196F3),
    accentColor: Color = Color(0xFFED8936)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = accentColor
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 1.5.dp
            )
        ) {
            Icon(
                Icons.Default.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                resetText,
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                saveText,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun InfoTipItem(
    text: String,
    primaryColor: Color = Color(0xFF2196F3)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(primaryColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF333333),
            modifier = Modifier.weight(1f),
            lineHeight = 20.sp
        )
    }
}

private fun getLanguageNativeName(code: String): String {
    return when (code) {
        "vi" -> "Tiếng Việt"
        "en" -> "English"
        else -> code
    }
}

private fun getLanguageFlagEmoji(code: String): String {
    return when (code) {
        "vi" -> "🇻🇳"
        "en" -> "🇺🇸"
        else -> "🌐"
    }
}

private fun Context.saveLanguageToPreferences(languageCode: String) {
    val prefs = this.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    prefs.edit().putString("language_code", languageCode).apply()
}

private fun Context.getSavedLanguage(): String {
    val prefs = this.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    return prefs.getString("language_code", "vi") ?: "vi"
}