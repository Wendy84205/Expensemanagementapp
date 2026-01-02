package com.example.financeapp.screen.main.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import com.example.financeapp.components.ui.BottomNavBar
import com.example.financeapp.rememberLanguageText
import com.example.financeapp.viewmodel.savings.SavingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    navController: NavController,
    onAddTransaction: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    transactionViewModel: TransactionViewModel = viewModel(),
    budgetViewModel: BudgetViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    savingsViewModel: SavingsViewModel
) {
    val allTransactions by transactionViewModel.transactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val errorMessage by transactionViewModel.errorMessage.collectAsState()
    val successMessage by transactionViewModel.successMessage.collectAsState()

    // State cho bộ lọc
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedDateFilter by remember { mutableStateOf("Tháng này") }

    // Lọc transactions
    val filteredTransactions = remember(allTransactions, selectedDateFilter) {
        allTransactions.filter { transaction ->
            when (selectedDateFilter) {
                "Hôm nay" -> isToday(transaction.date)
                "Hôm qua" -> isYesterday(transaction.date)
                "Tuần này" -> isThisWeek(transaction.date)
                "Tháng này" -> isThisMonth(transaction.date)
                "Tháng trước" -> isLastMonth(transaction.date)
                "Năm nay" -> isThisYear(transaction.date)
                else -> true
            }
        }.sortedByDescending {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.date) ?: Date()
        }
    }

    // Tính toán summary
    val (totalIncome, totalExpense) = remember(filteredTransactions) {
        val income = filteredTransactions.filter { it.isIncome }.sumOf { it.amount }
        val expense = filteredTransactions.filter { !it.isIncome }.sumOf { it.amount }
        Pair(income, expense)
    }

    // Reload data khi vào màn hình
    LaunchedEffect(Unit) {
        transactionViewModel.refreshTransactions()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Snackbar handling
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            transactionViewModel.clearError()
        }
    }

    // Colors
    val primaryBlue = Color(0xFF3B82F6)
    val lightBlue = Color(0xFFEFF6FF)
    val green = Color(0xFF10B981)
    val red = Color(0xFFEF4444)
    val background = Color(0xFFF8FAFC)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = primaryBlue,
                shape = CircleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch", tint = Color.White)
            }
        },
        bottomBar = { BottomNavBar(navController = navController) },
        containerColor = background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(padding)
                .padding(bottom = 16.dp)
        ) {
            // Header đơn giản
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Giao dịch",
                        color = Color(0xFF1E293B),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredTransactions.size} giao dịch",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
                IconButton(
                    onClick = { navController.navigate("calendar") },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFEFF6FF), CircleShape)
                        .clip(CircleShape)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Lịch",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Summary Cards - Hiển thị đơn giản
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Thu nhập",
                    amount = totalIncome,
                    color = green,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Chi tiêu",
                    amount = totalExpense,
                    color = red,
                    modifier = Modifier.weight(1f)
                )
            }

            // Filter chip hiện tại
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                FilterChip(
                    selected = true,
                    onClick = { showFilterSheet = true },
                    label = { Text(selectedDateFilter) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            // Danh sách giao dịch
            if (filteredTransactions.isEmpty()) {
                EmptyTransactionState(onAddTransaction = onAddTransaction)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            TransactionListItem( // ← SỬ DỤNG HÀM CÓ DIALOG
                                transaction = transaction,
                                categories = categories,
                                onClick = { onTransactionClick(transaction) },
                                onDelete = {
                                    transactionViewModel.deleteTransaction(
                                        transactionId = transaction.id,
                                        budgetViewModel = budgetViewModel
                                    )
                                },
                                primaryBlue = primaryBlue,
                                green = green,
                                red = red
                            )
                        }
                    }
                }
            }
        }

        // Filter Sheet
        if (showFilterSheet) {
            SimpleFilterSheet(
                onDismiss = { showFilterSheet = false },
                selectedFilter = selectedDateFilter,
                onFilterSelected = { selectedDateFilter = it },
                primaryBlue = primaryBlue
            )
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF64748B),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatVND(amount.toFloat()),
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SimpleTransactionItem(
    transaction: Transaction,
    categories: List<Category>,
    onClick: () -> Unit,
    primaryBlue: Color,
    green: Color,
    red: Color
) {
    val categoryName = categories.find {
        it.id == transaction.categoryId || it.name.equals(transaction.category, ignoreCase = true)
    }?.name ?: transaction.category.ifBlank { "Khác" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon đơn giản
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (transaction.isIncome) green.copy(alpha = 0.1f) else red.copy(alpha = 0.1f),
                        CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (transaction.isIncome) green else red,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Thông tin chính
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryName,
                    color = Color(0xFF1E293B),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = transaction.date,
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }

            // Số tiền
            Text(
                text = "${if (transaction.isIncome) "+" else "-"}${formatVND(transaction.amount.toFloat())}",
                color = if (transaction.isIncome) green else red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyTransactionState(onAddTransaction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Chưa có giao dịch",
            color = Color(0xFF1E293B),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Thêm giao dịch đầu tiên để bắt đầu theo dõi",
            color = Color(0xFF64748B),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAddTransaction,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
        ) {
            Text("Thêm giao dịch đầu tiên")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleFilterSheet(
    onDismiss: () -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    primaryBlue: Color
) {
    val filters = listOf(
        "Hôm nay",
        "Hôm qua",
        "Tuần này",
        "Tháng này",
        "Tháng trước",
        "Năm nay",
        "Tất cả"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Lọc theo thời gian",
                color = Color(0xFF1E293B),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            filters.forEach { filter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFilterSelected(filter) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = filter,
                        color = if (selectedFilter == filter) primaryBlue else Color(0xFF475569),
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Text("Áp dụng")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
@Composable
fun TransactionListItem(
    transaction: Transaction,
    categories: List<Category>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    primaryBlue: Color,
    green: Color,
    red: Color
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Tìm category
    val categoryName = categories.find {
        it.id == transaction.categoryId ||
                it.id == transaction.category ||
                it.name.equals(transaction.category, ignoreCase = true)
    }?.name ?: transaction.category.ifBlank { "Không xác định" }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Xoá giao dịch",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Hiển thị thông tin giao dịch
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEFF6FF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (transaction.isIncome) Color(0xFFC8E6C9) else Color(0xFFFFCC80),
                                        CircleShape
                                    )
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (transaction.isIncome) "↑" else "↓",
                                    color = if (transaction.isIncome) green else red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    categoryName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1E3A8A)
                                )
                                Text(
                                    transaction.date,
                                    color = primaryBlue,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                (if (transaction.isIncome) "+" else "-") + formatVND(transaction.amount.toFloat()),
                                color = if (transaction.isIncome) green else red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Bạn có chắc chắn muốn xoá giao dịch này?",
                        color = Color(0xFF718096),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEFF6FF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Quay lại",
                            color = primaryBlue,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDelete()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Xoá",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        )
    }

    // Card hiển thị giao dịch
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (transaction.isIncome) green.copy(alpha = 0.1f) else red.copy(alpha = 0.1f),
                        CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (transaction.isIncome) green else red,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    categoryName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    "${transaction.date} • ${transaction.wallet}",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (transaction.isIncome) "+" else "-") + formatVND(transaction.amount.toFloat()),
                    color = if (transaction.isIncome) green else red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Sửa",
                        tint = primaryBlue,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onClick() }
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Xoá",
                        tint = red,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { showDeleteDialog = true }
                    )
                }
            }
        }
    }
}


private fun formatVND(amount: Float): String {
    return if (amount >= 1000000000) {
        val ty = amount / 1000000000
        String.format("%,.1f tỷ", ty).replace(",", ".")
    } else if (amount >= 1000000) {
        val trieu = amount / 1000000
        String.format("%,.0f triệu", trieu).replace(",", ".")
    } else {
        String.format("%,.0f đ", amount).replace(",", ".")
    }
}


private fun isToday(dateString: String): Boolean = isDateInRange(dateString, 0)
private fun isYesterday(dateString: String): Boolean = isDateInRange(dateString, -1)

private fun isDateInRange(dateString: String, daysOffset: Int): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val transactionDate = sdf.parse(dateString) ?: return false
        val targetDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysOffset) }
        dateString == sdf.format(targetDate.time)
    } catch (e: Exception) {
        false
    }
}

private fun isThisWeek(dateString: String): Boolean {
    // Giữ nguyên logic cũ, đơn giản hóa
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val transactionDate = sdf.parse(dateString) ?: return false
    val calendar = Calendar.getInstance().apply { time = transactionDate }
    val today = Calendar.getInstance()

    val weekStart = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_WEEK, today.get(Calendar.DAY_OF_WEEK) * -1 + 1)
    }
    val weekEnd = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_WEEK, 7 - today.get(Calendar.DAY_OF_WEEK))
    }

    return !calendar.before(weekStart) && !calendar.after(weekEnd)
}

private fun isThisMonth(dateString: String): Boolean {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val transactionDate = sdf.parse(dateString) ?: return false
    val calendar = Calendar.getInstance().apply { time = transactionDate }
    val today = Calendar.getInstance()

    return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
}

private fun isLastMonth(dateString: String): Boolean {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val transactionDate = sdf.parse(dateString) ?: return false
    val calendar = Calendar.getInstance().apply { time = transactionDate }
    val today = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }

    return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
}

private fun isThisYear(dateString: String): Boolean {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val transactionDate = sdf.parse(dateString) ?: return false
    val calendar = Calendar.getInstance().apply { time = transactionDate }
    val today = Calendar.getInstance()

    return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
}