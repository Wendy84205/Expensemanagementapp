package com.example.financeapp.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.LanguageViewModel
import com.example.financeapp.LocalLanguageViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// 🎨 Colors
private val Navy = Color(0xFF0F4C75)
private val AccentOrange = Color(0xFFED8936)
private val SoftGray = Color(0xFFF5F7FA)
private val TextDark = Color(0xFF2D3748)
private val TextLight = Color(0xFF718096)
private val AccentGreen = Color(0xFF2E8B57)

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

    // --- Texts từ LanguageViewModel ---
    val accountInfoText = languageViewModel.getTranslation("account_info")
    val backText = languageViewModel.getTranslation("back")
    val updatePersonalInfoText = languageViewModel.getTranslation("update_personal_info")
    val fullNameText = languageViewModel.getTranslation("full_name")
    val emailText = languageViewModel.getTranslation("email")
    val phoneNumberText = languageViewModel.getTranslation("phone_number")
    val readOnlyText = languageViewModel.getTranslation("read_only")
    val updateSuccessText = languageViewModel.getTranslation("update_success")
    val savingText = languageViewModel.getTranslation("saving")
    val saveChangesText = languageViewModel.getTranslation("save_changes")
    val systemInfoText = languageViewModel.getTranslation("system_info")
    val userIdText = languageViewModel.getTranslation("user_id")
    val notAvailableText = languageViewModel.getTranslation("not_available")
    val providerText = languageViewModel.getTranslation("provider")
    val emailVerificationText = languageViewModel.getTranslation("email_verification")
    val verifiedText = languageViewModel.getTranslation("verified")
    val notVerifiedText = languageViewModel.getTranslation("not_verified")
    val createdAtText = languageViewModel.getTranslation("created_at")
    val lastLoginText = languageViewModel.getTranslation("last_login")
    val unknownText = languageViewModel.getTranslation("unknown")
    val changePasswordText = languageViewModel.getTranslation("change_password")
    val resetPasswordText = languageViewModel.getTranslation("reset_password")
    val verifyEmailText = languageViewModel.getTranslation("verify_email")
    val resetSuccessText = languageViewModel.getTranslation("reset_success")
    val verifySentText = languageViewModel.getTranslation("verify_email_sent")
    val errorText = languageViewModel.getTranslation("error")

    val providerName = remember(user?.providerId) {
        getProviderName(user?.providerId ?: "unknown", languageViewModel)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(accountInfoText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = backText,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Navy)
            )
        },
        containerColor = SoftGray,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // -------- Card thông tin cá nhân --------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp), clip = true),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(updatePersonalInfoText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)

                    CustomOutlinedTextField(value = name, onValueChange = { name = it }, label = fullNameText)
                    CustomOutlinedTextField(value = email, onValueChange = { email = it }, label = emailText)

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {},
                        label = { Text("$phoneNumberText ($readOnlyText)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        readOnly = true,
                        colors = TextFieldDefaults.colors(
                            disabledTextColor = TextLight,
                            disabledLabelColor = TextLight,
                            disabledIndicatorColor = Color(0xFFE2E8F0)
                        )
                    )

                    // Buttons: Save, Change Password, Reset, Verify Email
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.updateProfile(name, email) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = !viewModel.isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, disabledContainerColor = SoftGray),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(savingText, color = Color.White)
                            } else {
                                Text(saveChangesText, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { viewModel.resetPassword(email) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(resetPasswordText, fontWeight = FontWeight.Bold)
                        }

                        if (user?.isEmailVerified == false) {
                            Button(
                                onClick = { viewModel.sendEmailVerification() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(verifyEmailText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // -------- Card thông tin hệ thống --------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp), clip = true),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(systemInfoText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)

                    AccountInfoRow(userIdText, user?.uid ?: notAvailableText)
                    AccountInfoRow(providerText, providerName)
                    AccountInfoRow(emailVerificationText, if (user?.isEmailVerified == true) verifiedText else notVerifiedText)
                    AccountInfoRow(createdAtText, user?.metadata?.creationTimestamp?.formatDate() ?: unknownText)
                    AccountInfoRow(lastLoginText, user?.metadata?.lastSignInTimestamp?.formatDate() ?: unknownText)
                }
            }
        }

        // Hiển thị Snackbar khi có message
        LaunchedEffect(viewModel.message) {
            if (viewModel.message.isNotEmpty()) {
                val text = when (viewModel.message) {
                    "success" -> updateSuccessText
                    "reset_success" -> resetSuccessText
                    "verify_email_sent" -> verifySentText
                    else -> "${errorText}: ${viewModel.message}"
                }
                snackbarHostState.showSnackbar(text)
                viewModel.clearMessage()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomOutlinedTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextLight) },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedLabelColor = Navy,
            unfocusedLabelColor = TextLight,
            focusedIndicatorColor = Navy,
            unfocusedIndicatorColor = Color(0xFFE2E8F0),
            cursorColor = Navy,
            focusedTextColor = TextDark,
            unfocusedTextColor = TextDark
        )
    )
}

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextLight, fontSize = 14.sp)
        Text(value, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun getProviderName(providerId: String, languageViewModel: LanguageViewModel): String {
    return when (providerId) {
        "password" -> languageViewModel.getTranslation("email_password")
        "google.com" -> languageViewModel.getTranslation("google")
        "facebook.com" -> languageViewModel.getTranslation("facebook")
        "phone" -> languageViewModel.getTranslation("phone")
        else -> providerId
    }
}

private fun Long.formatDate(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}
