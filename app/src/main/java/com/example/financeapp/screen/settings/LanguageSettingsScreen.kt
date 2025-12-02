package com.example.financeapp.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

// ===============================
//        COLOR PALETTE
// ===============================
private val Navy = Color(0xFF0F4C75)
private val NavyLight = Color(0xFF3282B8)
private val TextPrimary = Color(0xFF2D3748)
private val TextSecondary = Color(0xFF718096)
private val Accent = Color(0xFFED8936)
private val AccentSoft = Color(0xFFFFF4E8)

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
    val infoText = "Thay đổi ngôn ngữ sẽ áp dụng cho toàn bộ ứng dụng."

    // Gradient Navy
    val gradient = Brush.verticalGradient(
        colors = listOf(Navy, NavyLight)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        screenTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {

                // ===============================
                //      CARD: CURRENT LANGUAGE
                // ===============================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(20.dp),
                            clip = true
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = currentLanguageText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = languageViewModel.getCurrentLanguageName(),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = Accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===============================
                //      CARD: LANGUAGE OPTIONS
                // ===============================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(20.dp),
                            clip = true
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            chooseLanguageText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            availableLanguages.forEach { language ->
                                LanguageOption(
                                    languageName = language.name,
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
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===============================
                //      CARD: ACTION BUTTONS
                // ===============================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(20.dp),
                            clip = true
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    languageViewModel.changeLanguage("vi")
                                    context.saveLanguageToPreferences("vi")

                                    scope.launch {
                                        snackbarHostState.showSnackbar(languageResetText)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Accent
                                )
                            ) {
                                Text(resetText, fontWeight = FontWeight.Medium)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "$languageSavedText: ${languageViewModel.getCurrentLanguageName()}"
                                        )
                                        navController.popBackStack()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) {
                                Text(saveText, fontWeight = FontWeight.Medium, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ===============================
                //      CARD: INFORMATION
                // ===============================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(20.dp),
                            clip = true
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "💡 $screenTitle",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = infoText,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    languageName: String,
    languageCode: String,
    isSelected: Boolean,
    onLanguageSelected: () -> Unit
) {
    Card(
        onClick = onLanguageSelected,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(14.dp),
                clip = true
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AccentSoft else Color.White
        ),
        border = if (isSelected)
            BorderStroke(2.dp, Accent)
        else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {
                Text(
                    text = languageName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Text(
                    text = getLanguageNativeName(languageCode),
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onLanguageSelected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Accent,
                    unselectedColor = TextSecondary
                )
            )
        }
    }
}

private fun getLanguageNativeName(code: String): String {
    return when (code) {
        "vi" -> "Tiếng Việt"
        "en" -> "English"
        "ja" -> "日本語"
        "ko" -> "한국어"
        "zh" -> "中文"
        "es" -> "Español"
        "fr" -> "Français"
        else -> code
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
