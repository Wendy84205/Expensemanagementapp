package com.example.financeapp.screen

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.financeapp.components.BottomNavBar
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.NotificationManagerCompat
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.rememberLanguageText

// 🎨 Màu theo gói ý 1
private val Navy = Color(0xFF0F4C75)
private val SoftGray = Color(0xFFF5F7FA)
private val TextDark = Color(0xFF2D3748)
private val TextLight = Color(0xFF718096)
private val AccentOrange = Color(0xFFED8936)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onSignOut: () -> Unit
) {
    val languageViewModel = LocalLanguageViewModel.current
    val context = LocalContext.current

    var showAboutDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    val currentLanguageName = languageViewModel.getCurrentLanguageName()

    // 🔔 State cho thông báo - kiểm tra trạng thái hiện tại
    var notificationsEnabled by remember {
        mutableStateOf(areNotificationsEnabled(context))
    }

    // 🔔 Launcher để request permission (cho Android 13+)
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsEnabled = isGranted
        if (!isGranted) {
            // Nếu không được cấp quyền, mở cài đặt
            openNotificationSettings(context)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        rememberLanguageText("settings"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = { BottomNavBar(navController) },
        containerColor = SoftGray
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {

            // 🔷 CARD 1 — Settings List với Notification Toggle
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = rememberLanguageText("account"),
                    subtitle = rememberLanguageText("manage_personal_info"),
                    onClick = { navController.navigate("account_settings") }
                )
                Divider(color = SoftGray)

                // 🔔 NOTIFICATION TOGGLE - QUAN TRỌNG
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = rememberLanguageText("notifications"),
                    subtitle = if (notificationsEnabled) "Đã bật • Nhận thông báo" else "Đã tắt • Bật để nhận thông báo",
                    checked = notificationsEnabled,
                    onCheckedChange = { newState ->
                        if (newState) {
                            // Khi người dùng BẬT thông báo
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // Android 13+ cần request permission
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                // Android 12 trở xuống - mở cài đặt thông báo
                                openNotificationSettings(context)
                                // Cập nhật state sau khi mở cài đặt
                                notificationsEnabled = areNotificationsEnabled(context)
                            }
                        } else {
                            // Khi người dùng TẮT thông báo - mở cài đặt để tắt
                            openNotificationSettings(context)
                            // Cập nhật state
                            notificationsEnabled = false
                        }
                    }
                )
                Divider(color = SoftGray)

                SettingsItem(
                    icon = Icons.Default.Language,
                    title = rememberLanguageText("language"),
                    subtitle = currentLanguageName,
                    onClick = { navController.navigate("language_settings") }
                )
                Divider(color = SoftGray)

                SettingsItem(
                    icon = Icons.Default.Extension,
                    title = rememberLanguageText("extensions"),
                    subtitle = rememberLanguageText("extra_tools_like_ai_calendar_scan"),
                    onClick = { navController.navigate("extensions") }
                )
                Divider(color = SoftGray)

                SettingsItem(
                    icon = Icons.Default.Help,
                    title = rememberLanguageText("help_support"),
                    subtitle = rememberLanguageText("faq"),
                    onClick = { navController.navigate("help") }
                )
                Divider(color = SoftGray)

                SettingsItem(
                    icon = Icons.Default.Info,
                    title = rememberLanguageText("about_app"),
                    subtitle = "${rememberLanguageText("version")} 1.0.0",
                    onClick = { showAboutDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔶 CARD 2 — Logout
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = rememberLanguageText("sign_out"),
                    subtitle = rememberLanguageText("logout_account"),
                    onClick = onSignOut,
                    isWarning = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Finance App © 2025 • v1.0.0",
                color = TextLight,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontSize = 12.sp
            )
        }

        if (showAboutDialog) {
            AboutAppDialog(onDismiss = { showAboutDialog = false })
        }

        if (showNotificationPermissionDialog) {
            NotificationPermissionDialog(
                onDismiss = { showNotificationPermissionDialog = false },
                onGrantPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    showNotificationPermissionDialog = false
                },
                onOpenSettings = {
                    openNotificationSettings(context)
                    showNotificationPermissionDialog = false
                }
            )
        }
    }
}

// 🔔 DIALOG YÊU CẦU QUYỀN THÔNG BÁO
@Composable
fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🔔 Cho phép thông báo",
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        },
        text = {
            Column {
                Text(
                    "Để nhận cảnh báo quan trọng về tài chính, vui lòng cho phép thông báo:",
                    color = TextLight,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "• Cảnh báo vượt ngân sách",
                    color = TextLight,
                    fontSize = 14.sp
                )
                Text(
                    "• Nhắc nhở chi tiêu định kỳ",
                    color = TextLight,
                    fontSize = 14.sp
                )
                Text(
                    "• Phân tích tài chính hàng tuần",
                    color = TextLight,
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("BỎ QUA", color = TextLight)
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    TextButton(onClick = onGrantPermission) {
                        Text("CHO PHÉP", color = Navy, fontWeight = FontWeight.Medium)
                    }
                } else {
                    TextButton(onClick = onOpenSettings) {
                        Text("CÀI ĐẶT", color = Navy, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// 🔔 KIỂM TRA QUYỀN THÔNG BÁO
private fun areNotificationsEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.areNotificationsEnabled()
    } else {
        // Trên Android cũ, sử dụng NotificationManagerCompat
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
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

// 🎯 THÊM VÀO AndroidManifest.xml
/*
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
*/

// Các composable cũ giữ nguyên...
@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), clip = true),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isWarning: Boolean = false
) {
    val titleColor = if (isWarning) Color(0xFFE53E3E) else TextDark
    val iconColor = if (isWarning) Color(0xFFE53E3E) else Navy

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = titleColor, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(subtitle, fontSize = 14.sp, color = TextLight)
            }

            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFA0AEC0))
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Navy, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextDark, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(subtitle, fontSize = 14.sp, color = TextLight)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Navy,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFC3C7CF),
                uncheckedThumbColor = Color.White
            )
        )
    }
}

@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Navy,
                    modifier = Modifier.size(50.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Wendy AI",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Ứng dụng quản lý tài chính cá nhân thông minh với AI hỗ trợ.",
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = TextLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Simple features list
                FeatureRow(icon = Icons.Default.Savings, text = "Theo dõi chi tiêu thông minh")
                FeatureRow(icon = Icons.Default.Analytics, text = "Phân tích tài chính AI")
                FeatureRow(icon = Icons.Default.Security, text = "Bảo mật dữ liệu")

                Spacer(modifier = Modifier.height(16.dp))

                // Version info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Phiên bản",
                        color = TextLight,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "1.0.0",
                        color = TextDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Đóng")
            }
        }
    )
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Navy,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = TextDark,
            fontSize = 14.sp
        )
    }
}