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
import androidx.compose.ui.graphics.Brush
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
import com.example.financeapp.components.BottomNavBar
import com.example.financeapp.getAppColors
import com.example.financeapp.rememberLanguageText
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
    categoryViewModel: CategoryViewModel = viewModel()
) {
    val allTransactions by transactionViewModel.transactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val errorMessage by transactionViewModel.errorMessage.collectAsState()
    val successMessage by transactionViewModel.successMessage.collectAsState()

    // State cho bộ lọc
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedDateFilter by remember { mutableStateOf("Tháng này") }

    // Danh sách date filter options (giữ nguyên tiếng Việt cho filter)
    val dateFilterOptions = listOf(
        "Hôm nay",
        "Hôm qua",
        "Tuần này",
        "Tháng này",
        "Tháng trước",
        "Năm nay",
        "Tất cả"
    )

    // Lọc transactions dựa trên các tiêu chí
    val filteredTransactions = remember(
        allTransactions,
        selectedDateFilter
    ) {
        allTransactions.filter { transaction ->
            val matchesDate = when (selectedDateFilter) {
                "Hôm nay" -> isToday(transaction.date)
                "Hôm qua" -> isYesterday(transaction.date)
                "Tuần này" -> isThisWeek(transaction.date)
                "Tháng này" -> isThisMonth(transaction.date)
                "Tháng trước" -> isLastMonth(transaction.date)
                "Năm nay" -> isThisYear(transaction.date)
                else -> true // "Tất cả"
            }

            matchesDate
        }
    }

    // ✅ Reload transactions khi vào màn hình để đảm bảo dữ liệu mới nhất
    LaunchedEffect(Unit) {
        transactionViewModel.refreshTransactions()
    }

    // ✅ Reload khi có transaction mới được thêm
    LaunchedEffect(Unit) {
        transactionViewModel.transactionAdded.collect {
            transactionViewModel.refreshTransactions()
        }
    }

    // ✅ Reload summary data khi transactions thay đổi
    LaunchedEffect(allTransactions.size) {
        transactionViewModel.refreshTransactions()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Snackbar hiển thị lỗi / thành công
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short) }
            transactionViewModel.clearError()
        }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short) }
            transactionViewModel.clearSuccessMessage()
        }
    }

    // ✅ Sử dụng MaterialTheme colors cho dark mode support
    val colors = getAppColors()
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(colors.background, colors.surface)
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = Color(0xFFED8936),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(12.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = rememberLanguageText("add_transaction"),
                    tint = Color.White
                )
            }
        },
        bottomBar = { BottomNavBar(navController = navController) },
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 0.dp)
            ) {
                // Header - Text bên trái, icon bên phải
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rememberLanguageText("transaction_screen_title"),
                        color = Color(0xFF2D3748),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { navController.navigate("calendar") },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White, CircleShape)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = rememberLanguageText("calendar"),
                            tint = Color(0xFF718096)
                        )
                    }
                }

                Spacer(Modifier.height(0.dp))

                // Summary Card - Cập nhật theo UI
                TransactionSummaryCard(
                    transactions = filteredTransactions
                )

                Spacer(Modifier.height(20.dp))

                // Row chứa icon filter và text hiển thị filter hiện tại
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hiển thị bộ lọc hiện tại
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = rememberLanguageText("filter"),
                            tint = Color(0xFF718096),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = selectedDateFilter,
                            color = Color(0xFF718096),
                            fontSize = 14.sp
                        )
                    }

                    // Icon filter để mở bottom sheet
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = rememberLanguageText("filter_options"),
                            tint = Color(0xFF718096)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Hiển thị giao dịch hoặc Empty State
                if (filteredTransactions.isEmpty()) {
                    EmptyTransactionState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        val groupedTransactions = filteredTransactions.groupBy { it.date }
                            .toList()
                            .sortedByDescending {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.first) ?: Date()
                            }

                        groupedTransactions.forEach { (date, dailyTransactions) ->
                            item {
                                Text(
                                    text = formatDateHeader(date),
                                    color = Color(0xFF718096),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(dailyTransactions) { transaction ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { it },
                                    exit = fadeOut(animationSpec = tween(500)) + slideOutVertically(animationSpec = tween(500)) { it }
                                ) {
                                    TransactionListItem(
                                        transaction = transaction,
                                        categories = categories,
                                        onClick = { onTransactionClick(transaction) },
                                        onDelete = {
                                            transactionViewModel.deleteTransaction(
                                                transactionId = transaction.id,
                                                budgetViewModel = budgetViewModel
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            showFilterSheet = showFilterSheet,
            onDismiss = { showFilterSheet = false },
            dateFilterOptions = dateFilterOptions,
            selectedDateFilter = selectedDateFilter,
            onFilterSelected = { filter ->
                selectedDateFilter = filter
                showFilterSheet = false
            }
        )
    }
}

// Filter Bottom Sheet composable riêng
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    showFilterSheet: Boolean,
    onDismiss: () -> Unit,
    dateFilterOptions: List<String>,
    selectedDateFilter: String,
    onFilterSelected: (String) -> Unit
) {
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color.White,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rememberLanguageText("filter_by_time"),
                        color = Color(0xFF2D3748),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = rememberLanguageText("close"),
                            tint = Color(0xFF718096)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Danh sách các tùy chọn lọc
                Column {
                    dateFilterOptions.forEach { filter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onFilterSelected(filter)
                                }
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDateFilter == filter,
                                onClick = {
                                    onFilterSelected(filter)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFFED8936)
                                )
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = filter,
                                color = Color(0xFF4A5568),
                                fontSize = 16.sp,
                                fontWeight = if (selectedDateFilter == filter) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // Divider giữa các item (trừ item cuối cùng)
                        if (filter != dateFilterOptions.last()) {
                            Divider(
                                color = Color(0xFFE2E8F0),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 48.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Nút áp dụng
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFED8936)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = rememberLanguageText("apply_filter"),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ----------------------------- Composables khác -----------------------------

@Composable
private fun TransactionSummaryCard(
    transactions: List<Transaction>
) {
    // ✅ Tính toán lại từ transactions list để đảm bảo luôn cập nhật
    val totalIncome = remember(transactions) {
        transactions.filter { it.isIncome }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { !it.isIncome }.sumOf { it.amount }
    }
    val difference = remember(transactions) {
        totalIncome - totalExpense
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Layout 2 hàng: Chi tiêu bên trái, Thu nhập bên phải
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tiền chi (bên trái)
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = rememberLanguageText("spending"),
                        color = Color(0xFF718096),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${formatCurrency(totalExpense.toLong())}",
                        color = Color(0xFFED8936),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tiền thu (bên phải)
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = rememberLanguageText("income"),
                        color = Color(0xFF718096),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${formatCurrency(totalIncome.toLong())}",
                        color = Color(0xFF2E8B57),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Dấu gạch ngang phân cách
            Spacer(Modifier.height(12.dp))
            Divider(
                color = Color(0xFFE2E8F0),
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // Tổng cộng (Chênh lệch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rememberLanguageText("total"),
                    color = Color(0xFF2D3748),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${if (difference >= 0) "+" else ""}${formatCurrency(difference.toLong())}",
                    color = if (difference >= 0) Color(0xFF2E8B57) else Color(0xFFED8936),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyTransactionState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(vertical = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Text("💸", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = rememberLanguageText("no_transactions"),
                color = Color(0xFF2D3748),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = rememberLanguageText("no_transactions_description"),
                color = Color(0xFF718096),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    categories: List<Category>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Tìm category
    val categoryName = categories.find {
        it.id == transaction.categoryId ||
                it.id == transaction.category ||
                it.name.equals(transaction.category, ignoreCase = true)
    }?.name ?: transaction.category.ifBlank { "Không xác định" }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = rememberLanguageText("delete_transaction"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748),
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
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
                                        if(transaction.isIncome) Color(0xFFC6F6D5) else Color(0xFFFED7D7),
                                        CircleShape
                                    )
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if(transaction.isIncome) "↑" else "↓",
                                    color = if(transaction.isIncome) Color(0xFF2E8B57) else Color(0xFFED8936),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    categoryName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF2D3748)
                                )
                                Text(
                                    transaction.date,
                                    color = Color(0xFF718096),
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                (if(transaction.isIncome) "+" else "-") + formatCurrency(transaction.amount.toLong()),
                                color = if(transaction.isIncome) Color(0xFF2E8B57) else Color(0xFFED8936),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "${rememberLanguageText("delete_confirmation")}?",
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
                            containerColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = rememberLanguageText("go_back"),
                            color = Color(0xFF4A5568),
                            fontSize = 16.sp
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
                            containerColor = Color(0xFFED8936)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = rememberLanguageText("delete"),
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if(transaction.isIncome) Color(0xFFC6F6D5) else Color(0xFFFED7D7),
                        CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if(transaction.isIncome) "↑" else "↓",
                    color = if(transaction.isIncome) Color(0xFF2E8B57) else Color(0xFFED8936),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    categoryName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF2D3748)
                )
                Text(
                    "${transaction.wallet} • ${if(transaction.description.isNotBlank()) transaction.description else rememberLanguageText("no_note")}",
                    color = Color(0xFF718096),
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if(transaction.isIncome) "+" else "-") + formatCurrency(transaction.amount.toLong()),
                    color = if(transaction.isIncome) Color(0xFF2E8B57) else Color(0xFFED8936),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = rememberLanguageText("edit"),
                        tint = Color(0xFF4A5568),
                        modifier = Modifier.size(20.dp).clickable { onClick() }
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = rememberLanguageText("delete"),
                        tint = Color(0xFFED8936),
                        modifier = Modifier.size(20.dp).clickable { showDeleteDialog = true }
                    )
                }
            }
        }
    }
}

// Hàm định dạng ngày tháng
private fun formatDateHeader(dateString: String): String {
    return try {
        when (dateString) {
            "Hôm nay" -> "Hôm nay"
            "Hôm qua" -> "Hôm qua"
            else -> {
                val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                if (date != null) {
                    outputFormat.format(date)
                } else {
                    dateString
                }
            }
        }
    } catch (e: Exception) {
        dateString
    }
}

// Hàm helper để định dạng tiền tệ
private fun formatCurrency(amount: Long): String {
    return String.format("%,d", amount).replace(",", ".") + "đ"
}

// Hàm helper để kiểm tra ngày
private fun isToday(dateString: String): Boolean {
    return dateString == "Hôm nay" || isDateInRange(dateString, 0)
}

private fun isYesterday(dateString: String): Boolean {
    return dateString == "Hôm qua" || isDateInRange(dateString, -1)
}

private fun isThisWeek(dateString: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val transactionDate = sdf.parse(dateString) ?: return false
        val calendar = Calendar.getInstance()
        calendar.time = transactionDate

        val today = Calendar.getInstance()
        val weekStart = Calendar.getInstance()
        weekStart.add(Calendar.DAY_OF_WEEK, today.get(Calendar.DAY_OF_WEEK) * -1 + 1)
        val weekEnd = Calendar.getInstance()
        weekEnd.add(Calendar.DAY_OF_WEEK, 7 - today.get(Calendar.DAY_OF_WEEK))

        !calendar.before(weekStart) && !calendar.after(weekEnd)
    } catch (e: Exception) {
        false
    }
}

private fun isThisMonth(dateString: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val transactionDate = sdf.parse(dateString) ?: return false
        val calendar = Calendar.getInstance()
        calendar.time = transactionDate

        val today = Calendar.getInstance()

        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
    } catch (e: Exception) {
        false
    }
}

private fun isLastMonth(dateString: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val transactionDate = sdf.parse(dateString) ?: return false
        val calendar = Calendar.getInstance()
        calendar.time = transactionDate

        val today = Calendar.getInstance()
        today.add(Calendar.MONTH, -1)

        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
    } catch (e: Exception) {
        false
    }
}

private fun isThisYear(dateString: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val transactionDate = sdf.parse(dateString) ?: return false
        val calendar = Calendar.getInstance()
        calendar.time = transactionDate

        val today = Calendar.getInstance()

        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    } catch (e: Exception) {
        false
    }
}

private fun isDateInRange(dateString: String, daysOffset: Int): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val transactionDate = sdf.parse(dateString) ?: return false
        val targetDate = Calendar.getInstance()
        targetDate.add(Calendar.DAY_OF_YEAR, daysOffset)

        val targetDateFormat = sdf.format(targetDate.time)
        dateString == targetDateFormat
    } catch (e: Exception) {
        false
    }
}