package com.example.financeapp.screen.auth

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.auth.AuthViewModel
import com.example.financeapp.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    activity: Activity
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val googleSignInClient = remember { createGoogleSignInClient(context) }
    val callbackManager = remember { CallbackManager.Factory.create() }

    // Màu xanh dương và trắng
    val primaryBlue = Color(0xFF1E3A8A) // Xanh dương đậm
    val secondaryBlue = Color(0xFF2563EB) // Xanh dương sáng
    val accentOrange = Color(0xFFF97316) // Cam nhấn
    val white = Color.White
    val lightGray = Color(0xFFF8FAFC)
    val textGray = Color(0xFF64748B)
    val darkText = Color(0xFF0F172A)

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task, authViewModel) { success, message ->
                isLoading = false
                if (!success) errorMessage = message
            }
        } else {
            isLoading = false
            errorMessage = "Đăng nhập Google bị hủy"
        }
    }

    val cardElevation by animateDpAsState(
        targetValue = if (isLoading) 8.dp else 24.dp,
        animationSpec = tween(durationMillis = 300)
    )

    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            navController.navigate("home") {
                popUpTo("auth") { inclusive = true }
            }
        }
    }

    DisposableEffect(Unit) {
        val callback = object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                authViewModel.firebaseAuthWithFacebook(result.accessToken) { success, message ->
                    isLoading = false
                    if (!success) errorMessage = message
                }
            }

            override fun onCancel() {
                isLoading = false
                errorMessage = "Đăng nhập Facebook bị hủy"
            }

            override fun onError(error: FacebookException) {
                isLoading = false
                errorMessage = "Lỗi đăng nhập Facebook: ${error.message}"
            }
        }

        LoginManager.getInstance().registerCallback(callbackManager, callback)
        onDispose { LoginManager.getInstance().unregisterCallback(callbackManager) }
    }

    // UI với màu xanh dương và trắng
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
            Spacer(modifier = Modifier.height(40.dp))

            // Logo W với AI
            WLogoWithAI(
                primaryColor = primaryBlue,
                accentColor = accentOrange
            )

            Spacer(modifier = Modifier.height(24.dp))

            AuthHeaderWithTagline(isLoading, primaryBlue)
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
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoginForm(
                        email = email,
                        password = password,
                        passwordVisible = passwordVisible,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        primaryBlue = primaryBlue,
                        secondaryOrange = accentOrange,
                        darkText = darkText,
                        textGray = textGray,
                        onEmailChange = { email = it },
                        onPasswordChange = { password = it },
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        onLoginClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Vui lòng nhập email và mật khẩu"
                            } else if (!isValidEmail(email)) {
                                errorMessage = "Email không hợp lệ"
                            } else {
                                isLoading = true
                                errorMessage = null
                                authViewModel.signInWithEmail(email, password) { success, message ->
                                    isLoading = false
                                    if (!success) errorMessage = message
                                }
                            }
                        },
                        onForgotPassword = { showForgotPasswordDialog = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Divider(
                        color = Color(0xFFE2E8F0),
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SocialLoginSection(
                        isLoading = isLoading,
                        primaryBlue = primaryBlue,
                        textGray = textGray,
                        onGoogleLogin = {
                            isLoading = true
                            val signInIntent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        },
                        onFacebookLogin = {
                            isLoading = true
                            LoginManager.getInstance().logInWithReadPermissions(
                                activity,
                                listOf("email", "public_profile")
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SignUpRedirect(
                        primaryBlue = primaryBlue,
                        secondaryOrange = accentOrange,
                        textGray = textGray,
                        onSignUpClick = { navController.navigate("register") }
                    )
                }
            }

            // Feature highlights
            Spacer(modifier = Modifier.height(32.dp))
            FeatureHighlights(
                primaryBlue = primaryBlue,
                accentOrange = accentOrange
            )
        }

        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                onDismiss = { showForgotPasswordDialog = false },
                authViewModel = authViewModel,
                primaryBlue = primaryBlue,
                secondaryOrange = accentOrange
            )
        }
    }
}
@Composable
private fun WLogoWithAI(
    primaryColor: Color,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo_wendy_ai), // Tên file ảnh
            contentDescription = "WENDY AI Logo",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, primaryColor.copy(alpha = 0.2f), CircleShape),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Text với W và AI (giữ nguyên)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(
                        color = primaryColor,
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
                        color = accentColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )) {
                        append(" AI")
                    }
                }
            )

            Text(
                "Trợ lý AI quản lý tài chính thông minh",
                color = Color(0xFF475569),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun AuthHeaderWithTagline(isLoading: Boolean, primaryBlue: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Chào mừng bạn trở lại",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBlue,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Đăng nhập để tiếp tục quản lý chi tiêu với AI",
            color = Color(0xFF64748B),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
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
private fun LoginForm(
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    primaryBlue: Color,
    secondaryOrange: Color,
    darkText: Color,
    textGray: Color,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPassword: () -> Unit
) {
    Column {
        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = primaryBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Email tài khoản", color = textGray)
                }
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = darkText),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryBlue,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                cursorColor = primaryBlue,
                focusedTextColor = darkText,
                unfocusedTextColor = darkText
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = primaryBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mật khẩu", color = textGray)
                }
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = darkText),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = primaryBlue
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryBlue,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                cursorColor = primaryBlue,
                focusedTextColor = darkText,
                unfocusedTextColor = darkText
            )
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = onForgotPassword,
                colors = ButtonDefaults.textButtonColors(contentColor = secondaryOrange)
            ) {
                Text("Quên mật khẩu?", fontWeight = FontWeight.Medium)
            }
        }

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
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Nút đăng nhập với gradient
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && isValidEmail(email),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryBlue, Color(0xFF2563EB))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
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
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Đăng nhập",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialLoginSection(
    isLoading: Boolean,
    primaryBlue: Color,
    textGray: Color,
    onGoogleLogin: () -> Unit,
    onFacebookLogin: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Hoặc đăng nhập nhanh với",
            fontSize = 14.sp,
            color = textGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Google button
            OutlinedButton(
                onClick = onGoogleLogin,
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = primaryBlue,
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google")
                }
            }

            // Facebook button
            OutlinedButton(
                onClick = onFacebookLogin,
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = primaryBlue,
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Facebook",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Facebook")
                }
            }
        }
    }
}

@Composable
private fun SignUpRedirect(
    primaryBlue: Color,
    secondaryOrange: Color,
    textGray: Color,
    onSignUpClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Chưa có tài khoản? ", color = textGray)
        TextButton(
            onClick = onSignUpClick,
            colors = ButtonDefaults.textButtonColors(contentColor = secondaryOrange)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Đăng ký ngay", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlights(
    primaryBlue: Color,
    accentOrange: Color
) {
    val features = listOf(
        Triple(Icons.Default.AutoGraph, "AI Phân tích", primaryBlue),
        Triple(Icons.Default.Savings, "Tiết kiệm", Color(0xFF16A34A)),
        Triple(Icons.Default.ShoppingCart, "Chi tiêu", accentOrange),
        Triple(Icons.Default.TrackChanges, "Mục tiêu", Color(0xFF7C3AED))
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Tính năng nổi bật",
            color = primaryBlue,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            features.forEach { (icon, text, color) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color.copy(alpha = 0.2f), CircleShape)
                                .border(2.dp, color.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text,
                            color = Color(0xFF1E293B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel,
    primaryBlue: Color,
    secondaryOrange: Color
) {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        confirmButton = {
            TextButton(
                onClick = {
                    if (email.isBlank() || !isValidEmail(email)) {
                        message = "Vui lòng nhập email hợp lệ"
                    } else {
                        isLoading = true
                        authViewModel.sendPasswordResetEmail(email) { success, msg ->
                            isLoading = false
                            message = msg
                            if (success) onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = secondaryOrange)
            ) {
                Text("Gửi liên kết", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color(0xFF64748B))
            }
        },
        title = {
            Text("Quên mật khẩu", color = Color(0xFF1E293B))
        },
        text = {
            Column {
                Text(
                    "Nhập email để nhận liên kết đặt lại mật khẩu. " +
                            "Trợ lý AI của chúng tôi sẽ hỗ trợ bạn thiết lập lại.",
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email của bạn", color = Color(0xFF64748B)) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = primaryBlue,
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
                message?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        it,
                        color = if (it.contains("đã", true)) Color(0xFF16A34A) else Color(0xFFDC2626),
                        fontSize = 14.sp
                    )
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        tonalElevation = 24.dp
    )
}

// Các hàm utility giữ nguyên
private fun createGoogleSignInClient(context: Context): GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    return GoogleSignIn.getClient(context, gso)
}

private fun handleGoogleSignInResult(
    task: Task<GoogleSignInAccount>,
    authViewModel: AuthViewModel,
    onResult: (Boolean, String?) -> Unit
) {
    try {
        val account = task.getResult(ApiException::class.java)
        val idToken = account.idToken
        if (idToken != null) {
            authViewModel.firebaseAuthWithGoogle(idToken, onResult)
        } else {
            onResult(false, "Không thể lấy ID token từ Google")
        }
    } catch (e: ApiException) {
        onResult(false, "Đăng nhập Google thất bại: ${e.statusCode}")
    }
}

private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    return email.matches(emailRegex.toRegex())
}