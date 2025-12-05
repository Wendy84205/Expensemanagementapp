package com.example.financeapp.screen.main.dashboard

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.components.ui.BottomNavBar
import com.example.financeapp.components.utils.formatCurrency
import com.example.financeapp.rememberLanguageText
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max

@Composable
fun HomeScreen(
    navController: NavController,
    onAddTransaction: () -> Unit,
    currentUser: com.example.financeapp.data.models.User?,
    transactions: List<Transaction>,
    onCalendarClick: () -> Unit,
    budgetViewModel: com.example.financeapp.viewmodel.budget.BudgetViewModel
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))
    )

    // Lấy tháng hiện tại
    val currentMonth = remember { getCurrentMonthYear() }

    // Lọc giao dịch tháng hiện tại
    val currentMonthTransactions = remember(transactions, currentMonth) {
        transactions.filter {
            val transactionDate = parseDate(it.date)
            val transactionMonth = formatMonthYear(transactionDate)
            transactionMonth == currentMonth
        }
    }

    // Tính tổng chi tiêu tháng hiện tại
    val monthlySpent = remember(currentMonthTransactions) {
        currentMonthTransactions
            .filter { !it.isIncome }
            .sumOf { it.amount }
            .toFloat()
    }

    // Tính tổng thu nhập tháng hiện tại
    val monthlyIncome = remember(currentMonthTransactions) {
        currentMonthTransactions
            .filter { it.isIncome }
            .sumOf { it.amount }
            .toFloat()
    }

    // Lấy 7 ngày gần nhất
    val last7DaysData = remember(transactions) {
        getLast7DaysSpending(transactions)
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController = navController) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HeaderSection(
                        currentUser = currentUser,
                        monthlySpent = monthlySpent,
                        monthlyIncome = monthlyIncome,
                        onCalendarClick = onCalendarClick
                    )
                }
                item {
                    RecentTransactionsCard(
                        navController = navController,
                        transactions = transactions,
                        onAddTransaction = onAddTransaction
                    )
                }
                item {
                    SpendingChartCard(
                        navController = navController,
                        last7DaysData = last7DaysData,
                        monthlySpent = monthlySpent
                    )
                }
                item {
                    BudgetCard(
                        navController = navController,
                        monthlySpent = monthlySpent,
                        budgetViewModel = budgetViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    currentUser: com.example.financeapp.data.models.User?,
    monthlySpent: Float,
    monthlyIncome: Float,
    onCalendarClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = rememberLanguageText("greeting"),
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = currentUser?.name ?: rememberLanguageText("user"),
                    color = Color(0xFF0F172A),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = onCalendarClick,
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFF3B82F6), CircleShape)
                    .clip(CircleShape)
            ) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = rememberLanguageText("calendar"),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Số tiền đã chi và thu trong tháng
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card thu nhập
            IncomeExpenseCard(
                title = rememberLanguageText("income_card_title"),
                amount = monthlyIncome,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )

            // Card chi tiêu
            IncomeExpenseCard(
                title = rememberLanguageText("expense_card_title"),
                amount = monthlySpent,
                color = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun IncomeExpenseCard(
    title: String,
    amount: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(amount.toDouble()),
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = rememberLanguageText("this_month"),
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun RecentTransactionsCard(
    navController: NavController,
    transactions: List<Transaction>,
    onAddTransaction: () -> Unit
) {
    // Lấy 5 giao dịch gần nhất
    val recentTransactions = remember(transactions) {
        transactions.sortedByDescending { parseDate(it.date) }.take(5)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rememberLanguageText("recent_transactions"),
                    color = Color(0xFF0F172A),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (recentTransactions.isNotEmpty()) {
                    Text(
                        text = rememberLanguageText("view_all"),
                        color = Color(0xFF3B82F6),
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            navController.navigate("transactions")
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Hai nút Thu và Chi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TransactionTypeButton(
                    text = rememberLanguageText("income"),
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = onAddTransaction
                )
                TransactionTypeButton(
                    text = rememberLanguageText("expense"),
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    onClick = onAddTransaction
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hiển thị danh sách giao dịch hoặc placeholder
            if (recentTransactions.isEmpty()) {
                NoTransactionsPlaceholder(onAddTransaction)
            } else {
                RecentTransactionsList(transactions = recentTransactions)
            }
        }
    }
}

@Composable
private fun TransactionTypeButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun NoTransactionsPlaceholder(onAddTransaction: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        Text(
            text = "📊",
            fontSize = 32.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = rememberLanguageText("no_recent_transactions"),
            color = Color(0xFF64748B),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddTransaction,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = rememberLanguageText("create_transaction"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RecentTransactionsList(transactions: List<Transaction>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        transactions.forEach { transaction ->
            TransactionItem(transaction = transaction)
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Hiển thị title thay vì category ID
                Text(
                    text = transaction.title.ifBlank {
                        transaction.description.ifBlank { rememberLanguageText("transaction") }
                    },
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${transaction.date} • ${transaction.dayOfWeek}",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
                if (transaction.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.description,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = if (transaction.isIncome) "+${formatCurrency(transaction.amount.toFloat())}"
                else "-${formatCurrency(transaction.amount.toFloat())}",
                color = if (transaction.isIncome) Color(0xFF10B981) else Color(0xFFEF4444),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SpendingChartCard(
    navController: NavController,
    last7DaysData: List<Pair<String, Float>>,
    monthlySpent: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rememberLanguageText("spending_7_days"),
                    color = Color(0xFF0F172A),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${rememberLanguageText("monthly_spent")} ${formatCurrency(monthlySpent)}",
                    color = Color(0xFF3B82F6),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        navController.navigate("statistics")
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Biểu đồ cột với dữ liệu thực
            SimpleColumnChart(last7DaysData)
        }
    }
}

@Composable
private fun SimpleColumnChart(data: List<Pair<String, Float>>) {
    if (data.isEmpty()) {
        PlaceholderChart(message = rememberLanguageText("no_chart_data"))
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartData = data.map { it.second }
            val days = data.map { it.first }

            val maxValue = max(chartData.maxOrNull() ?: 1f, 1f)
            val paddingTop = 20f
            val paddingBottom = 30f
            val chartHeight = size.height - paddingTop - paddingBottom
            val columnWidth = (size.width - 40f) / chartData.size
            val spacing = 4f

            // Vẽ các cột
            chartData.forEachIndexed { index, value ->
                val columnHeight = (value / maxValue * chartHeight)
                val x = 20f + index * (columnWidth + spacing)
                val y = paddingTop + chartHeight - columnHeight

                drawRoundRect(
                    color = Color(0xFF3B82F6),
                    topLeft = Offset(x, y),
                    size = Size(columnWidth, columnHeight),
                    cornerRadius = CornerRadius(x = 4f, y = 4f)
                )

                // Vẽ nhãn ngày
                drawContext.canvas.nativeCanvas.drawText(
                    days[index],
                    x + columnWidth / 2,
                    size.height - 10f,
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#64748B")
                        textSize = 12f
                        textAlign = Paint.Align.CENTER
                    }
                )
            }

            // Vẽ trục Y
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(15f, paddingTop),
                end = Offset(15f, paddingTop + chartHeight),
                strokeWidth = 2f
            )

            // Vẽ các mốc giá trị
            for (i in 0..4) {
                val y = paddingTop + chartHeight * (1 - i / 4f)
                val value = (maxValue * i / 4).toInt()

                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(15f, y),
                    end = Offset(size.width - 20f, y),
                    strokeWidth = 1f
                )

                drawContext.canvas.nativeCanvas.drawText(
                    formatCurrencyCompact(value.toFloat()),
                    8f,
                    y + 4f,
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#94A3B8")
                        textSize = 10f
                        textAlign = Paint.Align.RIGHT
                    }
                )
            }
        }
    }
}

@Composable
private fun BudgetCard(
    navController: NavController,
    monthlySpent: Float,
    budgetViewModel: com.example.financeapp.viewmodel.budget.BudgetViewModel
) {
    val currentMonth = remember { getCurrentMonthYear() }
    val budgets by budgetViewModel.budgets.collectAsState()

    // Lấy ngân sách tháng hiện tại
    val currentMonthBudget = remember(budgets) {
        budgets.find { budget ->
            val budgetMonth = formatLocalDateMonthYear(budget.startDate)
            budgetMonth == currentMonth && budget.isActive
        }
    }

    val budgetAmount = (currentMonthBudget?.amount ?: 0.0).toFloat()
    val spentAmount = (currentMonthBudget?.spent ?: 0.0).toFloat()
    val spendingPercentage = remember(budgetAmount, spentAmount) {
        if (budgetAmount > 0) {
            (spentAmount / budgetAmount * 100).coerceAtMost(100f)
        } else 0f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rememberLanguageText("budget_title"),
                    color = Color(0xFF0F172A),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (budgetAmount > 0) {
                    Text(
                        text = "${spendingPercentage.toInt()}%",
                        color = if (spendingPercentage > 80) Color(0xFFEF4444)
                        else if (spendingPercentage > 60) Color(0xFFF59E0B)
                        else Color(0xFF10B981),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (budgetAmount > 0) {
                // Hiển thị progress bar nếu đã đặt ngân sách
                BudgetProgressBar(
                    spent = spentAmount,
                    budget = budgetAmount,
                    percentage = spendingPercentage
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Card để thêm/chỉnh sửa ngân sách
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (currentMonthBudget != null) {
                            navController.navigate("edit_budget/${currentMonthBudget.id}")
                        } else {
                            navController.navigate("add_budget")
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = rememberLanguageText("manage_budget"),
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (budgetAmount > 0)
                                    "${rememberLanguageText("tracking_budget")} ${formatCurrency(budgetAmount.toDouble())}"
                                else rememberLanguageText("setup_budget_description"),
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = rememberLanguageText("view_details"),
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (currentMonthBudget != null) {
                                navController.navigate("edit_budget/${currentMonthBudget.id}")
                            } else {
                                navController.navigate("add_budget")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (budgetAmount > 0)
                                rememberLanguageText("view_details")
                            else
                                rememberLanguageText("setup_budget_button"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressBar(
    spent: Float,
    budget: Float,
    percentage: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${rememberLanguageText("spent")}: ${formatCurrency(spent)}",
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
            Text(
                text = "${rememberLanguageText("budget")}: ${formatCurrency(budget)}",
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .height(8.dp)
                    .background(
                        color = if (percentage > 80) Color(0xFFEF4444)
                        else if (percentage > 60) Color(0xFFF59E0B)
                        else Color(0xFF10B981),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        // Hiển thị số tiền còn lại
        val remaining = budget - spent
        if (remaining > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${rememberLanguageText("remaining")}: ${formatCurrency(remaining)}",
                color = Color(0xFF10B981),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        } else if (remaining < 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${rememberLanguageText("over_budget")}: ${formatCurrency(-remaining)}",
                color = Color(0xFFEF4444),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PlaceholderChart(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "📊",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Color(0xFF64748B),
                fontSize = 14.sp
            )
        }
    }
}

// ========== Hàm hỗ trợ ==========

// Thêm hàm này vào phần helper functions
private fun formatLocalDateMonthYear(localDate: java.time.LocalDate): String {
    return try {
        val month = localDate.monthValue
        val year = localDate.year
        "$month/$year"
    } catch (e: Exception) {
        "0/0"
    }
}

private fun getLast7DaysSpending(transactions: List<Transaction>): List<Pair<String, Float>> {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    val result = mutableListOf<Pair<String, Float>>()

    // Lấy 7 ngày gần nhất
    for (i in 6 downTo 0) {
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -i)
        val dateKey = dateFormat.format(calendar.time)

        // Tính tổng chi tiêu trong ngày
        val daySpending = transactions
            .filter {
                try {
                    val transactionDate = parseDate(it.date)
                    val compareDate = dateFormat.format(transactionDate)
                    compareDate == dateKey && !it.isIncome
                } catch (e: Exception) {
                    false
                }
            }
            .sumOf { it.amount.toDouble() }
            .toFloat()

        result.add(dateKey to daySpending)
    }

    return result
}

private fun parseDate(dateString: String): Date {
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString) ?: Date()
    } catch (e: Exception) {
        Date()
    }
}

private fun formatMonthYear(date: Date): String {
    val sdf = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    return sdf.format(date)
}

private fun getCurrentMonthYear(): String {
    val calendar = Calendar.getInstance()
    return formatMonthYear(calendar.time)
}

fun formatCurrency(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.0f₫", amount).replace(",", ".")
}

fun formatCurrencyCompact(amount: Float): String {
    return when {
        abs(amount) >= 1000000 -> String.format("%,.1fM", amount / 1000000)
        abs(amount) >= 1000 -> String.format("%,.0fK", amount / 1000)
        else -> String.format("%,.0f", amount)
    }.replace(",", ".")
}

// Data classes - GIỮ NGUYÊN
data class UserSession(
    val id: String,
    val email: String,
    val name: String,
    val avatar: String?
)

data class SavingFund(
    val id: String,
    val name: String,
    val description: String?,
    val balance: Float,
    val targetAmount: Float?,
    val targetDate: Date?,
    val color: String?
)