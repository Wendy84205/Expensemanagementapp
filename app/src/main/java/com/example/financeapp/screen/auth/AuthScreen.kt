package com.example.financeapp.screen.auth

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
        targetValue = if (isLoading) 8.dp else 16.dp,
        animationSpec = tween(durationMillis = 300)
    )

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F4C75), Color(0xFF2E8B57))
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

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthHeader(isLoading)
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(cardElevation, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoginForm(
                        email = email,
                        password = password,
                        passwordVisible = passwordVisible,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
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

                    SocialLoginSection(
                        isLoading = isLoading,
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

                    SignUpRedirect(onSignUpClick = { navController.navigate("register") })
                }
            }
        }

        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                onDismiss = { showForgotPasswordDialog = false },
                authViewModel = authViewModel
            )
        }
    }
}

@Composable
private fun AuthHeader(isLoading: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("WENDY AI", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Đăng nhập ngay nào ✨", color = Color.White.copy(alpha = 0.9f))
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFED8936)
            )
        }
    }
}

@Composable
private fun LoginForm(
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPassword: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email", color = Color(0xFF718096)) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color(0xFF2D3748)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Mật khẩu", color = Color(0xFF718096)) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color(0xFF2D3748)),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFF0F4C75)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPassword) {
                Text("Quên mật khẩu?", color = Color(0xFFED8936))
            }
        }

        errorMessage?.let {
            Text(it, color = Color(0xFFED5536), textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C75)),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && isValidEmail(email)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            else Text("Đăng nhập", color = Color.White)
        }
    }
}

@Composable
private fun SocialLoginSection(
    isLoading: Boolean,
    onGoogleLogin: () -> Unit,
    onFacebookLogin: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Hoặc đăng nhập bằng", fontSize = 14.sp, color = Color(0xFF718096))
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onGoogleLogin,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) { Text("Google", color = Color(0xFF0F4C75)) }
            OutlinedButton(
                onClick = onFacebookLogin,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) { Text("Facebook", color = Color(0xFF0F4C75)) }
        }
    }
}

@Composable
private fun SignUpRedirect(onSignUpClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Text("Chưa có tài khoản? ", color = Color(0xFF2D3748))
        TextButton(onClick = onSignUpClick) {
            Text("Đăng ký ngay", color = Color(0xFFED8936))
        }
    }
}

@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        confirmButton = {
            TextButton(onClick = {
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
            }) { Text("Gửi", color = Color(0xFFED8936)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) } },
        title = { Text("Quên mật khẩu", color = Color(0xFF2D3748)) },
        text = {
            Column {
                Text("Nhập email để nhận liên kết đặt lại mật khẩu.", color = Color(0xFF718096))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color(0xFF718096)) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF2D3748)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0F4C75)
                    )
                }
                message?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = if (it.contains("đã", true)) Color(0xFF2E8B57) else Color(0xFFED5536))
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFFF5F7FA)
    )
}

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
