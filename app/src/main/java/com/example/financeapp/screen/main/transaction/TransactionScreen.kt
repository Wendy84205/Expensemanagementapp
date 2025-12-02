package com.example.financeapp.screen

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
import com.example.financeapp.viewmodel.Category
import com.example.financeapp.viewmodel.CategoryViewModel
import com.example.financeapp.data.Transaction
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
    val transactions by transactionViewModel.transactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val errorMessage by transactionViewModel.errorMessage.collectAsState()
    val successMessage by transactionViewModel.successMessage.collectAsState()

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
    LaunchedEffect(transactions.size) {
        transactionViewModel.refreshTransactions()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Lấy text đa ngôn ngữ
    val transactionBookText = rememberLanguageText("transaction_book")
    val addTransactionText = rememberLanguageText("add_transaction")
    val financialOverviewText = rememberLanguageText("financial_overview")
    val monthText = rememberLanguageText("month")
    val totalIncomeText = rememberLanguageText("total_income")
    val totalExpenseText = rememberLanguageText("total_expense")
    val differenceText = rememberLanguageText("difference")
    val monthlyIncomeText = rememberLanguageText("monthly_income")
    val monthlyExpenseText = rememberLanguageText("monthly_expense")
    val transactionCountText = rememberLanguageText("transaction_count")
    val noNoteText = rememberLanguageText("no_note")
    val editText = rememberLanguageText("edit")
    val deleteText = rememberLanguageText("delete")
    val noTransactionsText = rememberLanguageText("no_transactions")
    val noTransactionsDescriptionText = rememberLanguageText("no_transactions_description")
    val deleteTransactionText = rememberLanguageText("delete_transaction")
    val deleteConfirmationText = rememberLanguageText("delete_confirmation")
    val confirmDeleteText = rememberLanguageText("confirm_delete")
    val cancelText = rememberLanguageText("cancel")

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
                containerColor = colors.warningColor,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(12.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = addTransactionText, tint = colors.onPrimary)
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
                        transactionBookText,
                        color = colors.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { navController.navigate("calendar") },
                        modifier = Modifier
                            .size(42.dp)
                            .background(colors.surface, CircleShape)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Calendar",
                            tint = colors.textPrimary
                        )
                    }
                }

                Spacer(Modifier.height(0.dp))

                // Summary Card
                TransactionSummaryCard(
                    transactions = transactions,
                    transactionViewModel = transactionViewModel,
                    totalIncomeText = totalIncomeText,
                    totalExpenseText = totalExpenseText,
                    differenceText = differenceText,
                    monthlyIncomeText = monthlyIncomeText,
                    monthlyExpenseText = monthlyExpenseText,
                    transactionCountText = transactionCountText,
                    monthText = monthText,
                    financialOverviewText = financialOverviewText
                )

                Spacer(Modifier.height(20.dp))

                // Hiển thị giao dịch hoặc Empty State
                if (transactions.isEmpty()) {
                    EmptyTransactionState(
                        noTransactionsText = noTransactionsText,
                        noTransactionsDescriptionText = noTransactionsDescriptionText
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        val groupedTransactions = transactions.groupBy { it.date }
                            .toList()
                            .sortedByDescending { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.first) }

                        groupedTransactions.forEach { (date, dailyTransactions) ->
                            item {
                                Text(
                                    formatDateHeader(date),
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            dailyTransactions.forEach { transaction ->
                                item {
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
                                            },
                                            noNoteText = noNoteText,
                                            editText = editText,
                                            deleteText = deleteText,
                                            deleteTransactionText = deleteTransactionText,
                                            deleteConfirmationText = deleteConfirmationText,
                                            confirmDeleteText = confirmDeleteText,
                                            cancelText = cancelText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------- Composables khác -----------------------------

@Composable
private fun TransactionSummaryCard(
    transactions: List<Transaction>,
    transactionViewModel: TransactionViewModel,
    totalIncomeText: String,
    totalExpenseText: String,
    differenceText: String,
    monthlyIncomeText: String,
    monthlyExpenseText: String,
    transactionCountText: String,
    monthText: String,
    financialOverviewText: String
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

    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH) + 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Tình hình thu chi", // Hardcode như trong hình
                    color = Color(0xFF2D3748),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tháng $currentMonth", // Hardcode như trong hình
                    color = Color(0xFF718096),
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(16.dp))

            // Layout 3 cột ngang giống hình
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cột 1: Chi tiêu
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Chi tiêu", // Hardcode như trong hình
                        color = Color(0xFF718096),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${formatCurrency(totalExpense)}",
                        color = Color(0xFFED8936),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Cột 2: Thu nhập
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Thu nhập", // Hardcode như trong hình
                        color = Color(0xFF718096),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${formatCurrency(totalIncome)}",
                        color = Color(0xFF2E8B57),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Cột 3: Tổng
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Tổng", // Hardcode như trong hình
                        color = Color(0xFF718096),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${formatCurrency(difference)}",
                        color = if(difference >= 0) Color(0xFF2E8B57) else Color(0xFFED8936),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionState(noTransactionsText: String, noTransactionsDescriptionText: String) {
    val colors = getAppColors()
    Box(
        modifier = Modifier.fillMaxSize().padding(vertical = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
            Text("💸", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(noTransactionsText, color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(noTransactionsDescriptionText, color = colors.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        }
    }
}


@Composable
fun TransactionListItem(
    transaction: Transaction,
    categories: List<Category>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    noNoteText: String,
    editText: String,
    deleteText: String,
    deleteTransactionText: String,
    deleteConfirmationText: String,
    confirmDeleteText: String,
    cancelText: String
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val colors = getAppColors()
    // ✅ Sửa: Tìm category bằng id hoặc name
    val categoryName = categories.find { 
        it.id == transaction.categoryId || 
        it.id == transaction.category || 
        it.name.equals(transaction.category, ignoreCase = true)
    }?.name ?: transaction.category.ifBlank { "Không xác định" }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(deleteTransactionText, fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = { Text("$deleteConfirmationText $categoryName?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text(confirmDeleteText, color = colors.warningColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(cancelText, color = colors.primary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = colors.cardColor
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardColor)
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
                        if(transaction.isIncome) colors.incomeColor.copy(alpha = 0.2f) 
                        else colors.expenseColor.copy(alpha = 0.2f), 
                        CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if(transaction.isIncome) "↑" else "↓",
                    color = if(transaction.isIncome) colors.incomeColor else colors.expenseColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(categoryName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.textPrimary)
                Text(
                    "${transaction.wallet} • ${if(transaction.description.isNotBlank()) transaction.description else noNoteText}",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if(transaction.isIncome) "+" else "-") + formatCurrency(transaction.amount),
                    color = if(transaction.isIncome) colors.incomeColor else colors.expenseColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Icon(Icons.Default.Edit, contentDescription = editText, tint = colors.primary, modifier = Modifier.size(20.dp).clickable { onClick() })
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Delete, contentDescription = deleteText, tint = colors.warningColor, modifier = Modifier.size(20.dp).clickable { showDeleteDialog = true })
                }
            }
        }
    }
}

private fun formatDateHeader(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date!!)
    } catch (e: Exception) { dateString }
}
