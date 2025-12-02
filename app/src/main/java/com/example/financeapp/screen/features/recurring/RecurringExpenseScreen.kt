package com.example.financeapp.screen.features.recurring

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.model.RecurringExpense
import com.example.financeapp.model.RecurringFrequency
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import androidx.core.graphics.toColorInt
import com.example.financeapp.screen.features.formatCurrency
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseScreen(
    navController: NavController,
    recurringExpenseViewModel: RecurringExpenseViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
) {
    val recurringExpenses by recurringExpenseViewModel.recurringExpenses.collectAsState()
    val isLoading by recurringExpenseViewModel.isLoading.collectAsState()

    // Colors
    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White
    val textColor = Color(0xFF333333)
    val subtitleColor = Color(0xFF666666)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<RecurringExpense?>(null) }

    LaunchedEffect(Unit) {
        recurringExpenseViewModel.loadRecurringExpenses()
    }

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                title = "Chi tiêu định kỳ",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_recurring_expense") },
                containerColor = primaryColor,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Thêm chi tiêu định kỳ",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            if (isLoading) {
                LoadingState(primaryColor = primaryColor)
            } else {
                SimpleStatsCard(expenses = recurringExpenses, primaryColor = primaryColor)
                Spacer(modifier = Modifier.height(16.dp))
                ExpenseList(
                    expenses = recurringExpenses,
                    primaryColor = primaryColor,
                    onEdit = { expense ->
                        navController.navigate("edit_recurring_expense/${expense.id}")
                    },
                    onToggleStatus = { expense ->
                        selectedExpense = expense
                        showStatusDialog = true
                    },
                    onDelete = { expense ->
                        selectedExpense = expense
                        showDeleteDialog = true
                    },
                    onAddClick = {
                        navController.navigate("add_recurring_expense")
                    }
                )
            }
        }
    }

    // Dialog xác nhận xóa
    if (showDeleteDialog && selectedExpense != null) {
        SimpleDeleteDialog(
            expense = selectedExpense!!,
            onConfirm = {
                recurringExpenseViewModel.deleteRecurringExpense(selectedExpense!!.id)
                showDeleteDialog = false
                selectedExpense = null
            },
            onDismiss = {
                showDeleteDialog = false
                selectedExpense = null
            }
        )
    }

    // Dialog thay đổi trạng thái
    if (showStatusDialog && selectedExpense != null) {
        StatusDialog(
            expense = selectedExpense!!,
            onToggle = {
                recurringExpenseViewModel.toggleRecurringExpense(selectedExpense!!.id)
                showStatusDialog = false
                selectedExpense = null
            },
            onDismiss = {
                showStatusDialog = false
                selectedExpense = null
            }
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
private fun LoadingState(primaryColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = primaryColor,
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun SimpleStatsCard(
    expenses: List<RecurringExpense>,
    primaryColor: Color
) {
    val activeExpenses = expenses.count { it.isActive }
    val totalAmount = expenses.filter { it.isActive }.sumOf { it.amount }

    val totalMonthly = expenses
        .filter { it.isActive && it.getFrequencyEnum() == RecurringFrequency.MONTHLY }
        .sumOf { it.amount }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "Tổng chi tiêu",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        formatCurrency(totalAmount),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "$activeExpenses",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        "đang hoạt động",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chi hàng tháng
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Chi hàng tháng:",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
                Text(
                    formatCurrency(totalMonthly),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
            }
        }
    }
}

@Composable
private fun ExpenseList(
    expenses: List<RecurringExpense>,
    primaryColor: Color,
    onEdit: (RecurringExpense) -> Unit,
    onToggleStatus: (RecurringExpense) -> Unit,
    onDelete: (RecurringExpense) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (expenses.isEmpty()) {
            EmptyExpenseState(onAddClick = onAddClick, primaryColor = primaryColor)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses) { expense ->
                    SimpleExpenseCard(
                        expense = expense,
                        primaryColor = primaryColor,
                        onEdit = { onEdit(expense) },
                        onToggleStatus = { onToggleStatus(expense) },
                        onDelete = { onDelete(expense) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun SimpleExpenseCard(
    expense: RecurringExpense,
    primaryColor: Color,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Format next occurrence
    val nextDate = try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = sdf.parse(expense.nextOccurrence)
        val outputFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        expense.nextOccurrence
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Có thể thêm navigation chi tiết */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header với danh mục và menu
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = CenterVertically) {
                    // Icon danh mục
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                parseColor(expense.categoryColor).copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            expense.categoryIcon,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            expense.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            "${expense.category} • ${expense.wallet}",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color(0xFF666666)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .width(180.dp)
                            .background(Color.White)
                    ) {
                        // Chỉnh sửa
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Chỉnh sửa",
                                        color = Color(0xFF333333),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            modifier = Modifier.height(42.dp)
                        )

                        Divider(
                            color = Color(0xFFEEEEEE),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Tạm dừng / Kích hoạt
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        if (expense.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        if (expense.isActive) "Tạm dừng" else "Kích hoạt",
                                        color = Color(0xFF333333),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                onToggleStatus()
                            },
                            modifier = Modifier.height(42.dp)
                        )

                        Divider(
                            color = Color(0xFFEEEEEE),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Xóa
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFF44336),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Xóa",
                                        color = Color(0xFFF44336),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            modifier = Modifier.height(42.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thông tin số tiền và tần suất
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        formatCurrency(expense.amount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        expense.getFrequencyEnum().displayName,
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "Tiếp theo: $nextDate",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        "Đã tạo: ${expense.totalGenerated} lần",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Status indicator
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        if (expense.isActive) primaryColor.copy(alpha = 0.3f)
                        else Color(0xFF999999).copy(alpha = 0.3f),
                        RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            if (expense.isActive) primaryColor else Color(0xFF999999),
                            RoundedCornerShape(3.dp)
                        )
                )
            }

            // Ghi chú (nếu có)
            expense.description?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null,
                        tint = Color(0xFF666666),
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        note,
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyExpenseState(
    onAddClick: () -> Unit,
    primaryColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Autorenew,
            contentDescription = "Không có chi tiêu định kỳ",
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Chưa có chi tiêu định kỳ nào",
            color = Color(0xFF666666),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Thêm chi tiêu định kỳ để quản lý chi tiêu tự động",
            color = Color(0xFF999999),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor
            ),
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth(0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("THÊM CHI TIÊU", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SimpleDeleteDialog(
    expense: RecurringExpense,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("XÓA", color = Color(0xFFF44336), fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("HỦY", color = Color(0xFF666666), fontWeight = FontWeight.Medium)
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFFF44336).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Cảnh báo",
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                "Xóa chi tiêu định kỳ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Bạn có chắc muốn xóa \"${expense.title}\"?",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Hành động này không thể hoàn tác.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun StatusDialog(
    expense: RecurringExpense,
    onToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    val actionText = if (expense.isActive) "Tạm dừng" else "Kích hoạt"
    val icon = if (expense.isActive) Icons.Default.Pause else Icons.Default.PlayArrow
    val primaryColor = Color(0xFF2196F3)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onToggle
            ) {
                Text(actionText.uppercase(), color = primaryColor, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
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
                    icon,
                    contentDescription = actionText,
                    tint = primaryColor,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                actionText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "Bạn có chắc muốn ${actionText.lowercase()} \"${expense.title}\"?",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun parseColor(colorString: String): Color {
    return try {
        val color = colorString.toColorInt()
        Color(color)
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}