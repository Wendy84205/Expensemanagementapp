package com.example.financeapp.screen.features.savings

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.data.models.SavingsGoal
import com.example.financeapp.screen.features.formatCurrency
import com.example.financeapp.viewmodel.savings.SavingsViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddSavingsGoalScreen(
    navController: NavController,
    goalId: String = "" // Thêm parameter này để nhận goalId
) {
    val viewModel: SavingsViewModel = viewModel()
    val auth = Firebase.auth
    val currentUser by remember(auth) {
        derivedStateOf { auth.currentUser }
    }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // State cho form
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var selectedColor by remember { mutableStateOf(0) }
    var selectedIcon by remember { mutableStateOf(0) }
    var showSuccess by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var addMoneyAmount by remember { mutableStateOf("") }
    var addMoneyError by remember { mutableStateOf<String?>(null) }

    // Validation states
    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val addSuccess by viewModel.addSuccess.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()
    val deleteSuccess by viewModel.deleteSuccess.collectAsState()

    // Thêm state để theo dõi chế độ và current goal
    val isEditMode = remember { mutableStateOf(goalId.isNotEmpty()) }
    val currentGoal = remember { mutableStateOf<SavingsGoal?>(null) }
    val savingsGoals by viewModel.savingsGoals.collectAsState()

    val colors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Green
        Color(0xFFF59E0B), // Yellow
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF14B8A6), // Teal
        Color(0xFFF97316)  // Orange
    )

    val icons = listOf(
        "💰", "🏠", "🚗", "✈️", "💻", "📱", "🎓", "🏥",
        "🎁", "💍", "📚", "🎮", "🎸", "🏀", "🍽️", "🛍️"
    )

    // Load dữ liệu nếu là chế độ chỉnh sửa
    LaunchedEffect(goalId) {
        if (goalId.isNotEmpty()) {
            val goal = viewModel.getGoalById(goalId)
            goal?.let {
                currentGoal.value = it
                name = it.name
                targetAmount = it.targetAmount.toString()
                description = it.description
                selectedColor = it.color
                selectedIcon = it.icon
                deadline = if (it.deadline > 0) it.deadline else null
            }
        }
    }

    // Xử lý thành công
    LaunchedEffect(addSuccess) {
        if (addSuccess) {
            showSuccess = true
            viewModel.resetAddSuccess()
        }
    }

    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            showSuccess = true
            viewModel.resetUpdateSuccess()
        }
    }

    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) {
            viewModel.resetDeleteSuccess()
            navController.popBackStack()
        }
    }

    // Auto focus vào tên khi mới vào màn hình
    LaunchedEffect(Unit) {
        if (!isEditMode.value) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    // Kiểm tra đăng nhập
    if (currentUser == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    Icons.Default.Login,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(60.dp)
                )
                Text(
                    "Vui lòng đăng nhập để ${if (isEditMode.value) "chỉnh sửa" else "thêm"} mục tiêu",
                    fontSize = 16.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
        return
    }

    // Success animation
    if (showSuccess) {
        LaunchedEffect(Unit) {
            delay(1500)
            navController.popBackStack()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .height(280.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(
                        visible = showSuccess,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        if (isEditMode.value) "Cập nhật thành công!" else "Thành công!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        if (isEditMode.value) "Mục tiêu đã được cập nhật" else "Mục tiêu đã được tạo",
                        fontSize = 16.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF3B82F6)
                    )
                }
            }
        }
        return
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Xác nhận xóa",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc chắn muốn xóa mục tiêu '${name}'?\nHành động này không thể hoàn tác.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        coroutineScope.launch {
                            viewModel.deleteSavingsGoal(goalId)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Xóa", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Hủy")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Add money dialog
    if (showAddMoneyDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddMoneyDialog = false
                addMoneyAmount = ""
                addMoneyError = null
            },
            title = {
                Text(
                    text = "Thêm tiền vào mục tiêu",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Mục tiêu: ${name}",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = addMoneyAmount,
                        onValueChange = {
                            addMoneyAmount = it.replace(Regex("[^\\d]"), "")
                            addMoneyError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Nhập số tiền", color = Color(0xFF94A3B8))
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedBorderColor = if (addMoneyError != null) Color(0xFFEF4444) else Color(0xFF3B82F6),
                            unfocusedBorderColor = if (addMoneyError != null) Color(0xFFEF4444) else Color(0xFFE2E8F0),
                            cursorColor = Color(0xFF3B82F6)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFF64748B))
                        },
                        isError = addMoneyError != null,
                        supportingText = {
                            if (addMoneyError != null) {
                                Text(
                                    text = addMoneyError ?: "",
                                    color = Color(0xFFDC2626),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )

                    if (currentGoal.value != null) {
                        val current = currentGoal.value!!.currentAmount
                        val target = currentGoal.value!!.targetAmount
                        val remaining = target - current

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Hiện tại: ${formatCurrency(current.toDouble())}",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Cần thêm: ${formatCurrency(remaining.toDouble())}",
                                fontSize = 13.sp,
                                color = Color(0xFF3B82F6),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = addMoneyAmount.toLongOrNull()
                        if (amount == null || amount <= 0) {
                            addMoneyError = "Vui lòng nhập số tiền hợp lệ"
                        } else {
                            coroutineScope.launch {
                                viewModel.addToSavingsGoal(goalId, amount)
                                showAddMoneyDialog = false
                                addMoneyAmount = ""
                                // Reload goal data
                                viewModel.loadSavingsGoals()
                            }
                        }
                    },
                    enabled = addMoneyAmount.isNotEmpty() && addMoneyAmount.toLongOrNull() ?: 0 > 0
                ) {
                    Text("Thêm tiền", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddMoneyDialog = false
                        addMoneyAmount = ""
                        addMoneyError = null
                    }
                ) {
                    Text("Hủy")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Hàm validation
    fun validateForm(): Boolean {
        var isValid = true

        // Validate tên
        if (name.isEmpty()) {
            nameError = "Vui lòng nhập tên mục tiêu"
            isValid = false
        } else if (name.length > 50) {
            nameError = "Tên quá dài (tối đa 50 ký tự)"
            isValid = false
        } else {
            nameError = null
        }

        // Validate số tiền
        if (targetAmount.isEmpty()) {
            amountError = "Vui lòng nhập số tiền mục tiêu"
            isValid = false
        } else {
            val amount = targetAmount.toLongOrNull()
            if (amount == null || amount <= 0) {
                amountError = "Số tiền phải lớn hơn 0"
                isValid = false
            } else if (amount > 1_000_000_000_000L) {
                amountError = "Số tiền quá lớn"
                isValid = false
            } else {
                amountError = null
            }
        }

        return isValid
    }

    // Hàm format số tiền
    fun formatCurrencyInput(input: String): String {
        return if (input.isEmpty()) {
            ""
        } else {
            try {
                val number = input.toLong()
                val formatter = java.text.DecimalFormat("#,###")
                formatter.format(number)
            } catch (e: Exception) {
                input
            }
        }
    }

    // Hàm xử lý lưu
    fun handleSave() {
        if (validateForm()) {
            if (isEditMode.value && goalId.isNotEmpty()) {
                // Chế độ chỉnh sửa
                val updatedFields = mutableMapOf<String, Any>()

                updatedFields["name"] = name
                updatedFields["targetAmount"] = targetAmount.toLong()
                updatedFields["description"] = description
                updatedFields["color"] = selectedColor
                updatedFields["icon"] = selectedIcon
                updatedFields["deadline"] = deadline ?: 0L

                coroutineScope.launch {
                    viewModel.updateGoalFields(goalId, updatedFields)
                }
            } else {
                // Chế độ tạo mới
                val goal = SavingsGoal(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    targetAmount = targetAmount.toLong(),
                    currentAmount = 0L,
                    deadline = deadline ?: 0L,
                    category = "Personal",
                    userId = currentUser!!.uid,
                    color = selectedColor,
                    icon = selectedIcon,
                    description = description,
                    progress = 0f,
                    isCompleted = false,
                    monthlyContribution = 0L,
                    startDate = System.currentTimeMillis(),
                    isActive = true
                )

                coroutineScope.launch {
                    viewModel.addSavingsGoal(goal)
                }
            }
        }
    }

    // Hàm format tiền tệ
    fun formatCurrency(amount: Double): String {
        return try {
            java.text.NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)
        } catch (e: Exception) {
            amount.toString()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isEditMode.value) "Chỉnh sửa mục tiêu" else "Tạo mục tiêu mới",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF475569)
                        )
                    }
                },
                actions = {
                    if (isEditMode.value) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Nút thêm tiền
                            IconButton(
                                onClick = { showAddMoneyDialog = true }
                            ) {
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = "Thêm tiền",
                                    tint = Color(0xFF10B981)
                                )
                            }

                            // Nút xóa
                            IconButton(
                                onClick = { showDeleteDialog = true }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Xóa",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hiển thị thông tin tiến độ nếu đang chỉnh sửa
            if (isEditMode.value && currentGoal.value != null) {
                val goal = currentGoal.value!!
                val progress = goal.calculateProgress()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tiến độ hiện tại",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "${progress.toInt()}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    progress >= 100 -> Color(0xFF10B981)
                                    progress > 70 -> Color(0xFF3B82F6)
                                    else -> Color(0xFFF59E0B)
                                }
                            )
                        }

                        LinearProgressIndicator(
                            progress = progress / 100,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = when {
                                progress >= 100 -> Color(0xFF10B981)
                                progress > 70 -> Color(0xFF3B82F6)
                                progress > 50 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            },
                            trackColor = Color(0xFFE2E8F0)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatCurrency(goal.currentAmount.toDouble()),
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = formatCurrency(goal.targetAmount.toDouble()),
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Nút thêm tiền nhanh
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val quickAmounts = listOf(50000L, 100000L, 200000L, 500000L)
                            quickAmounts.forEach { amount ->
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.addToSavingsGoal(goalId, amount)
                                            // Reload data
                                            viewModel.loadSavingsGoals()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF3B82F6)
                                    )
                                ) {
                                    Text("+${formatCurrency(amount.toDouble())}")
                                }
                            }
                        }
                    }
                }
            }

            // Error message từ viewModel
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                error?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEE2E2)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = it,
                                fontSize = 14.sp,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Card chính chứa form
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(16.dp),
                            clip = true
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Tên mục tiêu
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Tên mục tiêu *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    nameError = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                placeholder = {
                                    Text(
                                        "Ví dụ: Mua xe máy, Du lịch Đà Nẵng...",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B),
                                    focusedBorderColor = if (nameError != null) Color(0xFFEF4444) else Color(0xFF3B82F6),
                                    unfocusedBorderColor = if (nameError != null) Color(0xFFEF4444) else Color(0xFFE2E8F0),
                                    cursorColor = Color(0xFF3B82F6),
                                    errorBorderColor = Color(0xFFEF4444),
                                    errorTextColor = Color(0xFFDC2626),
                                    focusedLabelColor = Color.Transparent,
                                    unfocusedLabelColor = Color.Transparent,
                                    focusedPlaceholderColor = Color(0xFF94A3B8),
                                    unfocusedPlaceholderColor = Color(0xFF94A3B8)
                                ),
                                singleLine = true,
                                maxLines = 1,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Label,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                isError = nameError != null,
                                supportingText = {
                                    if (nameError != null) {
                                        Text(
                                            text = nameError ?: "",
                                            color = Color(0xFFDC2626),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            )
                        }

                        // Mục tiêu số tiền
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Mục tiêu số tiền *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                            OutlinedTextField(
                                value = formatCurrencyInput(targetAmount),
                                onValueChange = { newValue ->
                                    val cleanedValue = newValue.replace(Regex("[^\\d]"), "")
                                    targetAmount = cleanedValue
                                    amountError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "Nhập số tiền",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B),
                                    focusedBorderColor = if (amountError != null) Color(0xFFEF4444) else Color(0xFF3B82F6),
                                    unfocusedBorderColor = if (amountError != null) Color(0xFFEF4444) else Color(0xFFE2E8F0),
                                    cursorColor = Color(0xFF3B82F6),
                                    errorBorderColor = Color(0xFFEF4444),
                                    errorTextColor = Color(0xFFDC2626),
                                    focusedLabelColor = Color.Transparent,
                                    unfocusedLabelColor = Color.Transparent,
                                    focusedPlaceholderColor = Color(0xFF94A3B8),
                                    unfocusedPlaceholderColor = Color(0xFF94A3B8)
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                singleLine = true,
                                maxLines = 1,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                trailingIcon = {
                                    if (targetAmount.isNotEmpty()) {
                                        Text(
                                            "VND",
                                            fontSize = 14.sp,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                },
                                isError = amountError != null,
                                supportingText = {
                                    if (amountError != null) {
                                        Text(
                                            text = amountError ?: "",
                                            color = Color(0xFFDC2626),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            )
                        }

                        // Mô tả
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Mô tả",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    "(không bắt buộc)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                placeholder = {
                                    Text(
                                        "Thêm ghi chú về mục tiêu của bạn...",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B),
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    cursorColor = Color(0xFF3B82F6),
                                    focusedLabelColor = Color.Transparent,
                                    unfocusedLabelColor = Color.Transparent,
                                    focusedPlaceholderColor = Color(0xFF94A3B8),
                                    unfocusedPlaceholderColor = Color(0xFF94A3B8)
                                ),
                                maxLines = 4
                            )
                        }

                        // Chọn màu
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Chọn màu sắc",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                colors.forEachIndexed { index, color ->
                                    val isSelected = selectedColor == index
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                selectedColor = index
                                            }
                                            .shadow(
                                                elevation = if (isSelected) 8.dp else 2.dp,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Chọn icon
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Chọn biểu tượng",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B)
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.height(150.dp)
                            ) {
                                items(icons) { icon ->
                                    val index = icons.indexOf(icon)
                                    val isSelected = selectedIcon == index
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) colors[selectedColor].copy(alpha = 0.1f)
                                                else Color(0xFFF8FAFC)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) colors[selectedColor]
                                                else Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedIcon = index
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            icon,
                                            fontSize = 24.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Hạn chót
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Hạn chót",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    "(không bắt buộc)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

                            OutlinedTextField(
                                value = deadline?.let { dateFormat.format(Date(it)) } ?: "",
                                onValueChange = { /* Handle date picker */ },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "Chọn ngày hết hạn",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B),
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    cursorColor = Color(0xFF3B82F6),
                                    focusedLabelColor = Color.Transparent,
                                    unfocusedLabelColor = Color.Transparent,
                                    focusedPlaceholderColor = Color(0xFF94A3B8),
                                    unfocusedPlaceholderColor = Color(0xFF94A3B8)
                                ),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            // TODO: Hiển thị date picker
                                            // deadline = selectedDate
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = "Chọn ngày",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                }
                            )

                            if (deadline != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Đã chọn: ${dateFormat.format(Date(deadline!!))}",
                                        fontSize = 13.sp,
                                        color = Color(0xFF3B82F6),
                                        fontWeight = FontWeight.Medium
                                    )
                                    TextButton(
                                        onClick = { deadline = null }
                                    ) {
                                        Text(
                                            "Xóa",
                                            fontSize = 13.sp,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Button hành động
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nút lưu/cập nhật
                    Button(
                        onClick = { handleSave() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6),
                            disabledContainerColor = Color(0xFF94A3B8)
                        ),
                        enabled = name.isNotEmpty() && targetAmount.isNotEmpty() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isEditMode.value) Icons.Default.Save else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Text(
                                    if (isEditMode.value) "Cập nhật mục tiêu" else "Tạo mục tiêu",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Nút thêm tiền (chỉ hiển thị khi chỉnh sửa)
                    if (isEditMode.value) {
                        OutlinedButton(
                            onClick = { showAddMoneyDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF10B981),
                                containerColor = Color.White
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 2.dp
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981)
                                )
                                Text(
                                    "Thêm tiền vào mục tiêu",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }

                // Hướng dẫn
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF0F9FF)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF0EA5E9),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Mẹo nhỏ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0EA5E9)
                            )
                        }

                        Text(
                            "• Đặt tên mục tiêu rõ ràng để dễ theo dõi\n" +
                                    "• Chọn màu và biểu tượng phù hợp để phân biệt\n" +
                                    "• Thiết lập hạn chót để có động lực tiết kiệm\n" +
                                    "• Bạn có thể chỉnh sửa mục tiêu bất kỳ lúc nào",
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}