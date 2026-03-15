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
import androidx.compose.material.icons.automirrored.filled.*
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

import com.example.financeapp.components.theme.getAppColors

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
    val colors = getAppColors()
    val allTransactions by transactionViewModel.transactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    
    var selectedDateFilter by remember { mutableStateOf("Tháng này") }
    var showFilterSheet by remember { mutableStateOf(false) }

    val filteredTransactions = remember(allTransactions, selectedDateFilter) {
        allTransactions.filter { transaction ->
            when (selectedDateFilter) {
                "Hôm nay" -> isToday(transaction.date)
                "Tuần này" -> isThisWeek(transaction.date)
                "Tháng này" -> isThisMonth(transaction.date)
                else -> true
            }
        }.sortedByDescending { it.date }
    }

    val (totalIncome, totalExpense) = remember(filteredTransactions) {
        val income = filteredTransactions.filter { it.isIncome }.sumOf { it.amount }
        val expense = filteredTransactions.filter { !it.isIncome }.sumOf { it.amount }
        Pair(income, expense)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("Giao dịch", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text(selectedDateFilter, fontSize = 12.sp, color = colors.textSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("calendar") }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Xem lịch", tint = colors.primary)
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = colors.primary)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = colors.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
        bottomBar = { BottomNavBar(navController = navController) },
        containerColor = colors.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Stats Row
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Thu nhập",
                    amount = totalIncome,
                    color = colors.income,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Chi tiêu",
                    amount = totalExpense,
                    color = colors.expense,
                    modifier = Modifier.weight(1f)
                )
            }

            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có giao dịch nào", color = colors.textMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        TransactionListItemModern(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction) }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            onDismiss = { showFilterSheet = false },
            onFilterSelected = { selectedDateFilter = it },
            navController = navController
        )
    }
}

@Composable
fun StatCard(label: String, amount: Double, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = getAppColors().surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = getAppColors().textSecondary)
            Text(formatVND(amount.toFloat()), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun TransactionListItemModern(transaction: Transaction, onClick: () -> Unit) {
    val colors = getAppColors()
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = getAppColors().surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (transaction.isIncome) colors.secondary.copy(0.1f) else colors.expense.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (transaction.isIncome) colors.secondary else colors.expense,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category.ifBlank { "Khác" }, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(transaction.date, fontSize = 12.sp, color = colors.textMuted)
            }
            
            Text(
                (if (transaction.isIncome) "+" else "-") + formatVND(transaction.amount.toFloat()),
                fontWeight = FontWeight.Bold,
                color = if (transaction.isIncome) colors.secondary else colors.textPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(onDismiss: () -> Unit, onFilterSelected: (String) -> Unit, navController: NavController) {
    val colors = getAppColors()
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp, start = 20.dp, end = 20.dp).fillMaxWidth()) {
            Text(
                "Lọc theo thời gian", 
                fontWeight = FontWeight.Bold, 
                fontSize = 20.sp, 
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            listOf(
                "Hôm nay" to Icons.Default.Today,
                "Tuần này" to Icons.Default.DateRange,
                "Tháng này" to Icons.Default.CalendarMonth,
                "Chọn từ lịch" to Icons.Default.Event,
                "Tất cả" to Icons.AutoMirrored.Filled.List
            ).forEach { (filter, icon) ->
                Surface(
                    onClick = { 
                        if (filter == "Chọn từ lịch") {
                            onDismiss()
                            navController.navigate("calendar")
                        } else {
                            onFilterSelected(filter)
                            onDismiss() 
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).background(colors.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(filter, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun formatVND(amount: Float): String {
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"))
    return "${formatter.format(amount.toLong())} đ"
}

private fun isToday(dateString: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val now = Calendar.getInstance()
        dateString == sdf.format(now.time)
    } catch (e: Exception) {
        false
    }
}

private fun isThisWeek(dateString: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = sdf.parse(dateString) ?: return false
        val cal = Calendar.getInstance().apply { time = date }
        val now = Calendar.getInstance()
        cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) && cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    } catch (e: Exception) {
        false
    }
}

private fun isThisMonth(dateString: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = sdf.parse(dateString) ?: return false
        val cal = Calendar.getInstance().apply { time = date }
        val now = Calendar.getInstance()
        cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    } catch (e: Exception) {
        false
    }
}