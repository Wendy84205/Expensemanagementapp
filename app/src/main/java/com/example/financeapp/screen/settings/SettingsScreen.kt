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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.rememberLanguageText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onSignOut: () -> Unit
) {
    val languageViewModel = LocalLanguageViewModel.current
    val context = LocalContext.current

    // Colors
    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White
    val textColor = Color(0xFF333333)
    val subtitleColor = Color(0xFF666666)

    // State
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val currentLanguageName = languageViewModel.getCurrentLanguageName()

    // 🔔 State cho thông báo
    var notificationsEnabled by remember {
        mutableStateOf(areNotificationsEnabled(context))
    }

    // 🔔 Launcher để request permission
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsEnabled = isGranted
    }

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                title = "Cài đặt",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Phần thông tin cá nhân
            item {
                SettingsCard(title = "Tài khoản") {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Thông tin cá nhân",
                        subtitle = "Quản lý thông tin tài khoản",
                        onClick = { navController.navigate("account_settings") },
                        primaryColor = primaryColor
                    )
                }
            }

            // Phần cài đặt ứng dụng
            item {
                SettingsCard(title = "Cài đặt") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Notifications,
                        title = "Thông báo",
                        subtitle = if (notificationsEnabled) "Đã bật" else "Đã tắt",
                        checked = notificationsEnabled,
                        onCheckedChange = { newState ->
                            if (newState) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    openNotificationSettings(context)
                                }
                            } else {
                                openNotificationSettings(context)
                            }
                        },
                        primaryColor = primaryColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Ngôn ngữ",
                        subtitle = currentLanguageName,
                        onClick = { navController.navigate("language_settings") },
                        primaryColor = primaryColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    SettingsItem(
                        icon = Icons.Default.Extension,
                        title = "Tiện ích mở rộng",
                        subtitle = "AI, Lịch, Quét hóa đơn",
                        onClick = { navController.navigate("extensions") },
                        primaryColor = primaryColor
                    )
                }
            }

            // Phần hỗ trợ
            item {
                SettingsCard(title = "Hỗ trợ") {
                    SettingsItem(
                        icon = Icons.Default.Help,
                        title = "Trợ giúp",
                        subtitle = "Câu hỏi thường gặp",
                        onClick = { navController.navigate("help") },
                        primaryColor = primaryColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Về ứng dụng",
                        subtitle = "Phiên bản 1.0.0",
                        onClick = { showAboutDialog = true },
                        primaryColor = primaryColor
                    )
                }
            }

            // Phần đăng xuất
            item {
                SettingsCard(title = "Tài khoản") {
                    SettingsItem(
                        icon = Icons.Default.Logout,
                        title = "Đăng xuất",
                        subtitle = "Đăng xuất khỏi tài khoản",
                        onClick = { showLogoutDialog = true },
                        isWarning = true,
                        primaryColor = Color(0xFFF44336)
                    )
                }
            }

            // Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Finance App © 2025",
                        color = subtitleColor,
                        fontSize = 12.sp
                    )
                    Text(
                        "Phiên bản 1.0.0",
                        color = Color(0xFF999999),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    // About Dialog
    if (showAboutDialog) {
        AboutAppDialog(
            onDismiss = { showAboutDialog = false },
            primaryColor = primaryColor
        )
    }

    // Logout Dialog
    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                showLogoutDialog = false
                onSignOut()
            },
            onDismiss = { showLogoutDialog = false }
        )
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
                    contentDescription = "Quay lại",
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
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isWarning: Boolean = false,
    primaryColor: Color = Color(0xFF2196F3)
) {
    val titleColor = if (isWarning) Color(0xFFF44336) else Color(0xFF333333)
    val iconColor = if (isWarning) Color(0xFFF44336) else primaryColor

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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = titleColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF999999)
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
                icon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                subtitle,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }

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
private fun AboutAppDialog(
    onDismiss: () -> Unit,
    primaryColor: Color = Color(0xFF2196F3)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ĐÓNG", color = primaryColor, fontWeight = FontWeight.Medium)
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
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = "Ứng dụng tài chính",
                    tint = primaryColor,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                "Về ứng dụng",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    "Finance App - Ứng dụng quản lý tài chính cá nhân",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Phiên bản",
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    )
                    Text(
                        "1.0.0",
                        color = Color(0xFF333333),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Ngày phát hành",
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    )
                    Text(
                        "2025",
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
private fun LogoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = Color(0xFFF44336)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("ĐĂNG XUẤT", color = primaryColor, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("HỦY", color = Color(0xFF666666), fontWeight = FontWeight.Medium)
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
                    Icons.Default.Logout,
                    contentDescription = "Đăng xuất",
                    tint = primaryColor,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                "Đăng xuất",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                "Bạn có chắc muốn đăng xuất khỏi tài khoản?",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

// 🔔 KIỂM TRA QUYỀN THÔNG BÁO
private fun areNotificationsEnabled(context: Context): Boolean {
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

// 🔔 MỞ CÀI ĐẶT THÔNG BÁO
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
                    addCategory(Intent.CATEGORY_DEFAULT)
                    data = Uri.parse("package:${context.packageName}")
                }
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: mở cài đặt app chung
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}