package com.example.financeapp.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.financeapp.LocalLanguageViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AccountSettingsViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set

    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            isLoading = true
            message = ""
            val user = FirebaseAuth.getInstance().currentUser
            try {
                val update = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user?.updateProfile(update)?.await()
                if (email != user?.email) {
                    user?.updateEmail(email)?.await()
                }
                message = "success"
            } catch (e: Exception) {
                message = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            isLoading = true
            message = ""
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
                message = "reset_success"
            } catch (e: Exception) {
                message = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun sendEmailVerification() {
        viewModelScope.launch {
            isLoading = true
            message = ""
            try {
                FirebaseAuth.getInstance().currentUser?.sendEmailVerification()?.await()
                message = "verify_email_sent"
            } catch (e: Exception) {
                message = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun clearMessage() {
        message = ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    navController: NavController,
    viewModel: AccountSettingsViewModel = AccountSettingsViewModel()
) {
    val user = FirebaseAuth.getInstance().currentUser
    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    val phone = user?.phoneNumber ?: ""

    val languageViewModel = LocalLanguageViewModel.current

    // Colors
    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White
    val textColor = Color(0xFF333333)
    val subtitleColor = Color(0xFF666666)
    val warningColor = Color(0xFFF44336)
    val successColor = Color(0xFF4CAF50)

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                title = "Thông tin tài khoản",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = backgroundColor,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thông tin cá nhân
            item {
                SettingsCard(title = "Thông tin cá nhân") {
                    // Tên
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "Tên đầy đủ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFDDDDDD),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = primaryColor
                            ),
                            singleLine = true
                        )
                    }

                    Divider(color = Color(0xFFEEEEEE))

                    // Email
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "Email",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFDDDDDD),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = primaryColor
                            ),
                            singleLine = true
                        )
                    }

                    Divider(color = Color(0xFFEEEEEE))

                    // Số điện thoại (chỉ đọc)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "Số điện thoại",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = if (phone.isNotBlank()) phone else "Chưa có",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFDDDDDD),
                                unfocusedTextColor = subtitleColor,
                                disabledTextColor = subtitleColor
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // Thao tác tài khoản
            item {
                SettingsCard(title = "Thao tác tài khoản") {
                    // Lưu thay đổi
                    Button(
                        onClick = { viewModel.updateProfile(name, email) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .height(48.dp),
                        enabled = !viewModel.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = Color(0xFFCCCCCC)
                        )
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đang lưu...", color = Color.White)
                        } else {
                            Text("LƯU THAY ĐỔI", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Đặt lại mật khẩu
                    OutlinedButton(
                        onClick = { viewModel.resetPassword(email) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textColor
                        )
                    ) {
                        Icon(
                            Icons.Default.LockReset,
                            contentDescription = "Đặt lại mật khẩu",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ĐẶT LẠI MẬT KHẨU")
                    }

                    // Tìm và thay thế phần xác thực email này:
                    if (user?.isEmailVerified == false) {
                        OutlinedButton(
                            onClick = { viewModel.sendEmailVerification() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .height(48.dp)
                                .border(
                                    width = 1.dp,
                                    color = warningColor,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = warningColor
                            )
                        ) {
                            Icon(
                                Icons.Default.MarkEmailRead,
                                contentDescription = "Xác thực email",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("XÁC THỰC EMAIL")
                        }
                    }
                }
            }

            // Thông tin hệ thống
            item {
                SettingsCard(title = "Thông tin hệ thống") {
                    // User ID
                    InfoRow(
                        title = "ID người dùng",
                        value = user?.uid ?: "Không có",
                        color = subtitleColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    // Nhà cung cấp
                    InfoRow(
                        title = "Nhà cung cấp",
                        value = getProviderName(user?.providerId ?: "unknown"),
                        color = subtitleColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    // Trạng thái email
                    InfoRow(
                        title = "Xác thực email",
                        value = if (user?.isEmailVerified == true) "Đã xác thực" else "Chưa xác thực",
                        valueColor = if (user?.isEmailVerified == true) successColor else warningColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    // Ngày tạo
                    InfoRow(
                        title = "Ngày tạo",
                        value = user?.metadata?.creationTimestamp?.formatDate() ?: "Không rõ",
                        color = subtitleColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    // Lần đăng nhập cuối
                    InfoRow(
                        title = "Lần đăng nhập cuối",
                        value = user?.metadata?.lastSignInTimestamp?.formatDate() ?: "Không rõ",
                        color = subtitleColor
                    )
                }
            }
        }

        // Hiển thị Snackbar khi có message
        LaunchedEffect(viewModel.message) {
            if (viewModel.message.isNotEmpty()) {
                val text = when (viewModel.message) {
                    "success" -> "Cập nhật thông tin thành công"
                    "reset_success" -> "Đã gửi email đặt lại mật khẩu"
                    "verify_email_sent" -> "Đã gửi email xác thực"
                    else -> "Lỗi: ${viewModel.message}"
                }
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = text,
                    duration = SnackbarDuration.Short
                )
                if (snackbarResult == SnackbarResult.Dismissed) {
                    viewModel.clearMessage()
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
private fun InfoRow(
    title: String,
    value: String,
    color: Color = Color(0xFF333333),
    valueColor: Color = color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color(0xFF333333),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp
        )
        Text(
            value,
            fontSize = 15.sp,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun getProviderName(providerId: String): String {
    return when (providerId) {
        "password" -> "Email & Mật khẩu"
        "google.com" -> "Google"
        "facebook.com" -> "Facebook"
        "phone" -> "Số điện thoại"
        else -> providerId
    }
}

private fun Long.formatDate(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}