package com.example.financeapp.screen.auth

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financeapp.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Màu xanh dương và trắng theo yêu cầu
    val primaryBlue = Color(0xFF1E3A8A) // Xanh dương đậm
    val secondaryBlue = Color(0xFF2563EB) // Xanh dương sáng
    val accentOrange = Color(0xFFF97316) // Cam nhấn
    val white = Color.White
    val lightGray = Color(0xFFF8FAFC)
    val textGray = Color(0xFF64748B)
    val darkText = Color(0xFF0F172A)

    val cardElevation by animateDpAsState(
        targetValue = if (isLoading) 8.dp else 24.dp,
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            delay(2000)
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGray)
    ) {
        // Background decorative elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Decorative circles
                    drawCircle(
                        color = primaryBlue.copy(alpha = 0.05f),
                        radius = 250.dp.toPx(),
                        center = Offset(size.width * 0.8f, size.height * 0.1f)
                    )
                    drawCircle(
                        color = secondaryBlue.copy(alpha = 0.05f),
                        radius = 200.dp.toPx(),
                        center = Offset(size.width * 0.2f, size.height * 0.3f)
                    )
                    drawCircle(
                        color = accentOrange.copy(alpha = 0.05f),
                        radius = 150.dp.toPx(),
                        center = Offset(size.width * 0.7f, size.height * 0.8f)
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(white, CircleShape)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = primaryBlue
                    )
                }
            }

            RegisterHeader(isLoading, primaryBlue)

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(cardElevation, RoundedCornerShape(32.dp), clip = true),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = white,
                    contentColor = darkText
                ),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo W với AI
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .drawBehind {
                                // Vẽ chữ W cách điệu
                                drawCircle(
                                    color = primaryBlue.copy(alpha = 0.1f),
                                    radius = size.minDimension / 2,
                                    center = Offset(size.width / 2, size.height / 2)
                                )

                                val path = Path().apply {
                                    moveTo(size.width * 0.2f, size.height * 0.8f)
                                    lineTo(size.width * 0.35f, size.height * 0.2f)
                                    lineTo(size.width * 0.5f, size.height * 0.6f)
                                    lineTo(size.width * 0.65f, size.height * 0.2f)
                                    lineTo(size.width * 0.8f, size.height * 0.8f)
                                }

                                drawPath(
                                    path = path,
                                    color = primaryBlue,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Điểm AI
                                drawCircle(
                                    color = accentOrange,
                                    radius = 6.dp.toPx(),
                                    center = Offset(size.width * 0.8f, size.height * 0.3f)
                                )
                                drawCircle(
                                    color = accentOrange,
                                    radius = 6.dp.toPx(),
                                    center = Offset(size.width * 0.2f, size.height * 0.3f)
                                )
                            }
                            .clip(CircleShape)
                            .background(white)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tạo tài khoản mới",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryBlue,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Tham gia cùng trợ lý AI quản lý tài chính",
                        color = textGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Họ và tên
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = primaryBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Họ và tên", color = textGray)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = primaryBlue,
                            focusedTextColor = darkText,
                            unfocusedTextColor = darkText
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = primaryBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Email", color = textGray)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = primaryBlue,
                            focusedTextColor = darkText,
                            unfocusedTextColor = darkText
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password
                    CustomPasswordTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Mật khẩu",
                        enabled = !isLoading,
                        primaryBlue = primaryBlue,
                        darkText = darkText,
                        textGray = textGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password
                    CustomPasswordTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Xác nhận mật khẩu",
                        enabled = !isLoading,
                        primaryBlue = primaryBlue,
                        darkText = darkText,
                        textGray = textGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone optional
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = primaryBlue.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Số điện thoại (tùy chọn)", color = textGray)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = primaryBlue,
                            focusedTextColor = darkText,
                            unfocusedTextColor = darkText
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Password requirements
                    PasswordRequirements(password = password)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error message
                    errorMessage?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    it,
                                    color = Color(0xFFDC2626),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Success message
                    successMessage?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    it,
                                    color = Color(0xFF16A34A),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Register button
                    Button(
                        onClick = {
                            if (fullName.isBlank()) {
                                errorMessage = "Vui lòng nhập họ và tên"
                            } else if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Vui lòng nhập email và mật khẩu"
                            } else if (!isValidEmail(email)) {
                                errorMessage = "Email không hợp lệ"
                            } else if (password.length < 6) {
                                errorMessage = "Mật khẩu phải có ít nhất 6 ký tự"
                            } else if (password != confirmPassword) {
                                errorMessage = "Mật khẩu xác nhận không khớp"
                            } else {
                                isLoading = true
                                errorMessage = null
                                authViewModel.createUserWithEmail(email, password) { ok, msg ->
                                    isLoading = false
                                    if (ok) {
                                        successMessage = "✅ Tài khoản đã được tạo! Trợ lý AI đang chuẩn bị..."
                                        errorMessage = null
                                    } else {
                                        errorMessage = msg ?: "Đăng ký thất bại"
                                        successMessage = null
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && fullName.isNotBlank()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(primaryBlue, secondaryBlue)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = white,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = white,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Tạo tài khoản & Bắt đầu",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = white
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Terms and conditions
                    Text(
                        buildAnnotatedString {
                            append("Bằng việc đăng ký, bạn đồng ý với ")
                            withStyle(style = SpanStyle(
                                color = primaryBlue,
                                fontWeight = FontWeight.Medium
                            )) {
                                append("Điều khoản dịch vụ")
                            }
                            append(" và ")
                            withStyle(style = SpanStyle(
                                color = primaryBlue,
                                fontWeight = FontWeight.Medium
                            )) {
                                append("Chính sách bảo mật")
                            }
                        },
                        color = textGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(
                        color = Color(0xFFE2E8F0),
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Back to login
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Đã có tài khoản? Đăng nhập ngay",
                            color = primaryBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RegisterHeader(isLoading: Boolean, primaryBlue: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(
                    color = primaryBlue,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )) {
                    append("W")
                }
                withStyle(style = SpanStyle(
                    color = Color(0xFF0F172A),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )) {
                    append("ENDY")
                }
                withStyle(style = SpanStyle(
                    color = Color(0xFFF97316),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )) {
                    append(" AI")
                }
            },
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Trợ lý AI quản lý tài chính thông minh",
            color = Color(0xFF475569),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        if (isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFF97316),
                trackColor = Color(0xFFFDE68A)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    primaryBlue: Color,
    darkText: Color,
    textGray: Color
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = primaryBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, color = textGray)
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                    tint = primaryBlue
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryBlue,
            unfocusedBorderColor = Color(0xFFCBD5E1),
            cursorColor = primaryBlue,
            focusedTextColor = darkText,
            unfocusedTextColor = darkText
        )
    )
}

@Composable
fun PasswordRequirements(password: String) {
    val requirements = listOf(
        "Ít nhất 6 ký tự" to (password.length >= 6),
        "Có chữ cái" to password.any { it.isLetter() },
        "Có số" to password.any { it.isDigit() }
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Yêu cầu mật khẩu:",
            color = Color(0xFF475569),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        requirements.forEach { (requirement, met) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (met) Color(0xFF16A34A) else Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    requirement,
                    color = if (met) Color(0xFF16A34A) else Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    return email.matches(emailRegex.toRegex())
}