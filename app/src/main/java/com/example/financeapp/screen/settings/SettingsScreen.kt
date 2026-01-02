package com.example.financeapp.screen.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.utils.notification.NotificationHelper
import com.example.financeapp.viewmodel.savings.SavingsViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import kotlinx.coroutines.launch

/**
 * SettingsScreen - Màn hình cài đặt ứng dụng
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onSignOut: () -> Unit,
    savingsViewModel: SavingsViewModel
) {
    // ==================== INITIALIZATION ====================
    val languageViewModel = LocalLanguageViewModel.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ==================== COLORS ====================
    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val warningColor = Color(0xFFFF9800)
    val errorColor = Color(0xFFF44336)

    // ==================== STATE MANAGEMENT ====================
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPermissionInfo by remember { mutableStateOf(false) }
    val currentLanguageName = languageViewModel.getCurrentLanguageName()

    // 🔔 SIMPLIFIED NOTIFICATION STATE
    var notificationsEnabled by remember {
        mutableStateOf(checkSystemNotificationPermission(context))
    }

    // 🔔 PERMISSION LAUNCHER
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsEnabled = isGranted

        if (isGranted) {
            // Tạo notification channel và gửi thông báo chào mừng
            coroutineScope.launch {
                NotificationHelper.createChannel(context)

                NotificationHelper.showNotification(
                    context = context,
                    title = languageViewModel.getTranslation("notifications"),
                    message = languageViewModel.getTranslation("notifications_on")
                )
            }
        } else {
            // Hiển thị dialog thông tin khi user từ chối
            showPermissionInfo = true
        }
    }

    // ==================== UI - SCAFFOLD ====================
    Scaffold(
        topBar = {
            SettingsTopAppBar(
                title = languageViewModel.getTranslation("settings"),
                onBackClick = { navController.popBackStack() },
                languageViewModel = languageViewModel
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==================== SECTION 1: ACCOUNT ====================
            item {
                SettingsSectionCard(
                    title = languageViewModel.getTranslation("account"),
                    languageViewModel = languageViewModel
                ) {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = languageViewModel.getTranslation("personal_info"),
                        subtitle = languageViewModel.getTranslation("manage_account_info"),
                        onClick = { navController.navigate("account_settings") },
                        iconColor = primaryColor,
                        languageViewModel = languageViewModel
                    )
                }
            }

            // ==================== SECTION 2: APPLICATION SETTINGS ====================
            item {
                SettingsSectionCard(
                    title = languageViewModel.getTranslation("application_settings"),
                    languageViewModel = languageViewModel
                ) {
                    // 1. NOTIFICATION SWITCH (SIMPLE VERSION)
                    SettingsSwitchItem(
                        icon = Icons.Default.Notifications,
                        title = languageViewModel.getTranslation("notifications"),
                        subtitle = if (notificationsEnabled)
                            languageViewModel.getTranslation("notifications_on")
                        else
                            languageViewModel.getTranslation("notifications_off"),
                        checked = notificationsEnabled,
                        onCheckedChange = { newState ->
                            if (newState) {
                                // User wants to enable notifications
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    // Android 13+: Request runtime permission
                                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    // Android < 13: Just enable notifications
                                    notificationsEnabled = true
                                    coroutineScope.launch {
                                        NotificationHelper.createChannel(context)
                                    }
                                }
                            } else {
                                // User wants to disable notifications
                                notificationsEnabled = false
                            }
                        },
                        primaryColor = primaryColor,
                        languageViewModel = languageViewModel
                    )

                    Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                    // 2. LANGUAGE SETTINGS
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = languageViewModel.getTranslation("language"),
                        subtitle = currentLanguageName,
                        onClick = { navController.navigate("language_settings") },
                        iconColor = primaryColor,
                        languageViewModel = languageViewModel
                    )

                    Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                    // 3. EXTENSIONS
                    SettingsItem(
                        icon = Icons.Default.Extension,
                        title = languageViewModel.getTranslation("extensions"),
                        subtitle = languageViewModel.getTranslation("extra_tools_like_ai_calendar_scan"),
                        onClick = { navController.navigate("extensions") },
                        iconColor = primaryColor,
                        languageViewModel = languageViewModel
                    )
                }
            }

            // ==================== SECTION 3: SUPPORT ====================
            item {
                SettingsSectionCard(
                    title = languageViewModel.getTranslation("support"),
                    languageViewModel = languageViewModel
                ) {
                    SettingsItem(
                        icon = Icons.Default.Help,
                        title = languageViewModel.getTranslation("help"),
                        subtitle = languageViewModel.getTranslation("frequently_asked_questions"),
                        onClick = { navController.navigate("help") },
                        iconColor = primaryColor,
                        languageViewModel = languageViewModel
                    )

                    Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = languageViewModel.getTranslation("about_app"),
                        subtitle = "${languageViewModel.getTranslation("version")} 1.0.0",
                        onClick = { showAboutDialog = true },
                        iconColor = primaryColor,
                        languageViewModel = languageViewModel
                    )
                }
            }

            // ==================== SECTION 4: ACCOUNT ACTIONS ====================
            item {
                SettingsSectionCard(
                    title = languageViewModel.getTranslation("account"),
                    languageViewModel = languageViewModel
                ) {
                    SettingsItem(
                        icon = Icons.Default.Logout,
                        title = languageViewModel.getTranslation("logout"),
                        subtitle = languageViewModel.getTranslation("logout_account"),
                        onClick = { showLogoutDialog = true },
                        isWarning = true,
                        iconColor = errorColor,
                        languageViewModel = languageViewModel
                    )
                }
            }

            // ==================== SECTION 5: APP INFO FOOTER ====================
            item {
                AppInfoFooter(languageViewModel = languageViewModel)
            }
        }
    }

    // ==================== DIALOGS ====================

    // About Dialog
    if (showAboutDialog) {
        AboutAppDialog(
            onDismiss = { showAboutDialog = false },
            primaryColor = primaryColor,
            languageViewModel = languageViewModel
        )
    }

    // Logout Dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                onSignOut()
            },
            onDismiss = { showLogoutDialog = false },
            languageViewModel = languageViewModel
        )
    }

    // Permission Info Dialog
    if (showPermissionInfo) {
        PermissionInfoDialog(
            onDismiss = { showPermissionInfo = false },
            onOpenSettings = {
                showPermissionInfo = false
                openNotificationSettings(context)
            },
            languageViewModel = languageViewModel
        )
    }
}

// ==================== CORE LOGIC FUNCTIONS ====================

/**
 * Kiểm tra quyền thông báo hệ thống (đơn giản hóa)
 */
private fun checkSystemNotificationPermission(context: Context): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            // Android < 13: Không cần runtime permission
            true
        }
    } catch (e: Exception) {
        true // Mặc định true nếu có lỗi
    }
}

/**
 * Mở cài đặt thông báo hệ thống
 */
private fun openNotificationSettings(context: Context) {
    try {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", context.applicationInfo.uid)
                }
                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.parse("package:${context.packageName}")
                }
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e2: Exception) {
            // Không thể mở cài đặt
        }
    }
}

// ==================== UI COMPONENTS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = languageViewModel.getTranslation("back"),
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
private fun SettingsSectionCard(
    title: String? = null,
    languageViewModel: LanguageViewModel,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isWarning: Boolean = false,
    iconColor: Color = Color(0xFF2196F3),
    languageViewModel: LanguageViewModel
) {
    val titleColor = if (isWarning) Color(0xFFF44336) else Color(0xFF333333)
    val finalIconColor = if (isWarning) Color(0xFFF44336) else iconColor

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(finalIconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = finalIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = titleColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    lineHeight = 18.sp
                )
            }

            // Chevron indicator
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF999999),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    primaryColor: Color,
    languageViewModel: LanguageViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(primaryColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 18.sp
            )
        }

        // Switch
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = primaryColor,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFC3C7CF),
                uncheckedThumbColor = Color.White
            )
        )
    }
}

@Composable
private fun AppInfoFooter(languageViewModel: LanguageViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = languageViewModel.getTranslation("footer_copyright"),
            color = Color(0xFF666666),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${languageViewModel.getTranslation("version")} 1.0.0",
            color = Color(0xFF999999),
            fontSize = 11.sp
        )
    }
}

// ==================== DIALOG COMPONENTS ====================

@Composable
private fun AboutAppDialog(
    onDismiss: () -> Unit,
    primaryColor: Color,
    languageViewModel: LanguageViewModel
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = languageViewModel.getTranslation("close").uppercase(),
                    color = primaryColor,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(primaryColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = languageViewModel.getTranslation("finance_app"),
                    tint = primaryColor,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                text = languageViewModel.getTranslation("about_app"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column {
                Text(
                    text = languageViewModel.getTranslation("personal_finance_app"),
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Version info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = languageViewModel.getTranslation("version"),
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "1.0.0",
                        color = Color(0xFF333333),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Release date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = languageViewModel.getTranslation("release_date"),
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "2024",
                        color = Color(0xFF333333),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    val errorColor = Color(0xFFF44336)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = errorColor
                )
            ) {
                Text(
                    text = languageViewModel.getTranslation("logout").uppercase(),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = languageViewModel.getTranslation("cancel").uppercase(),
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(errorColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = languageViewModel.getTranslation("logout"),
                    tint = errorColor,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                text = languageViewModel.getTranslation("logout"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Text(
                text = languageViewModel.getTranslation("confirm_logout"),
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun PermissionInfoDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    val warningColor = Color(0xFFFF9800)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onOpenSettings,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = warningColor
                )
            ) {
                Text(
                    text = languageViewModel.getTranslation("open_settings").uppercase(),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = languageViewModel.getTranslation("cancel").uppercase(),
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(warningColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = warningColor,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                text = languageViewModel.getTranslation("notification_permission_denied"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column {
                Text(
                    text = languageViewModel.getTranslation("permission_denied_message"),
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}