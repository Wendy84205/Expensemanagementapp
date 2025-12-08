package com.example.financeapp.screen.main.dashboard

import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notes
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
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
    budgetViewModel: com.example.financeapp.viewmodel.budget.BudgetViewModel,
    categoryViewModel: com.example.financeapp.viewmodel.transaction.CategoryViewModel
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
                        budgetViewModel = budgetViewModel,
                        categoryViewModel = categoryViewModel
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
    budgetViewModel: com.example.financeapp.viewmodel.budget.BudgetViewModel,
    categoryViewModel: com.example.financeapp.viewmodel.transaction.CategoryViewModel // Thêm CategoryViewModel
) {
    val budgets by budgetViewModel.budgets.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()

    // Lấy tất cả budget đã tạo
    val allBudgets = remember(budgets) {
        budgets.sortedByDescending { it.startDate }
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
                if (allBudgets.isNotEmpty()) {
                    Text(
                        text = "${allBudgets.size} ngân sách",
                        color = Color(0xFF3B82F6),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (allBudgets.isEmpty()) {
                // Hiển thị khi không có budget
                NoBudgetPlaceholder(navController = navController)
            } else {
                // Hiển thị danh sách tất cả budget
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hiển thị tất cả budgets
                    allBudgets.forEach { budget ->
                        // Tìm category tương ứng
                        val category = categories.find { it.id == budget.categoryId }
                        BudgetItemWithIcon(
                            budget = budget,
                            category = category,
                            navController = navController
                        )
                    }

                    // Nút thêm budget mới
                    Button(
                        onClick = {
                            navController.navigate("add_budget")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Thêm ngân sách mới",
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
private fun BudgetItemWithIcon(
    budget: com.example.financeapp.data.models.Budget,
    category: com.example.financeapp.viewmodel.transaction.Category?,
    navController: NavController
) {
    val budgetAmount = budget.amount.toFloat()
    val spentAmount = budget.spentAmount.toFloat()
    val spendingPercentage = remember(budgetAmount, spentAmount) {
        if (budgetAmount > 0) {
            (spentAmount / budgetAmount * 100).coerceAtMost(100f)
        } else 0f
    }

    val isExceeded = spendingPercentage >= 100
    val isWarning = spendingPercentage > 80 && spendingPercentage < 100
    val remaining = budgetAmount - spentAmount // <-- Định nghĩa biến remaining ở đây

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("edit_budget/${budget.id}")
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isExceeded -> Color(0xFFFFF8E1)
                !budget.isActive -> Color(0xFFF8F8F8)
                else -> Color(0xFFF8FAFC)
            }
        ),
        border = when {
            isExceeded -> BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f))
            !budget.isActive -> BorderStroke(1.dp, Color(0xFFDDDDDD))
            else -> null
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header với icon và tên category
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon category
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            parseColor(category?.color ?: "#3B82F6").copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        category?.icon ?: "💰",
                        fontSize = 16.sp,
                        color = parseColor(category?.color ?: "#3B82F6")
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Tên category
                    Text(
                        text = category?.name ?: "Ngân sách chung",
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Thông tin thời gian và chu kỳ
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${budget.startDate.dayOfMonth}/${budget.startDate.monthValue} - ${budget.endDate.dayOfMonth}/${budget.endDate.monthValue}",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Loại chu kỳ
                        val periodText = when (budget.periodType) {
                            com.example.financeapp.data.models.BudgetPeriodType.WEEK -> "Tuần"
                            com.example.financeapp.data.models.BudgetPeriodType.MONTH -> "Tháng"
                            com.example.financeapp.data.models.BudgetPeriodType.QUARTER -> "Quý"
                            com.example.financeapp.data.models.BudgetPeriodType.YEAR -> "Năm"
                            else -> "Tháng"
                        }

                        Text(
                            text = "/$periodText",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )

                        // Badge trạng thái
                        if (!budget.isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFDDDDDD), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Tạm dừng",
                                    color = Color(0xFF666666),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Phần trăm
                Text(
                    text = "${spendingPercentage.toInt()}%",
                    color = when {
                        isExceeded -> Color(0xFFEF4444)
                        isWarning -> Color(0xFFF59E0B)
                        !budget.isActive -> Color(0xFF94A3B8)
                        else -> Color(0xFF10B981)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thông tin số tiền
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Đã chi: ${formatCurrency(spentAmount)}",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Ngân sách: ${formatCurrency(budgetAmount)}",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Còn lại:",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatCurrency(remaining), // <-- Sử dụng biến remaining đã định nghĩa
                        color = when {
                            isExceeded -> Color(0xFFEF4444)
                            isWarning -> Color(0xFFF59E0B)
                            !budget.isActive -> Color(0xFF94A3B8)
                            else -> Color(0xFF10B981)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        when {
                            !budget.isActive -> Color(0xFFE2E8F0)
                            else -> Color(0xFFE2E8F0)
                        },
                        RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(spendingPercentage / 100f)
                        .height(6.dp)
                        .background(
                            color = when {
                                isExceeded -> Color(0xFFEF4444)
                                isWarning -> Color(0xFFF59E0B)
                                !budget.isActive -> Color(0xFF94A3B8)
                                else -> Color(0xFF10B981)
                            },
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }

            // Hiển thị cảnh báo nếu cần
            if (isExceeded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Vượt ngân sách: ${formatCurrency(-remaining)}",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            } else if (isWarning) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Đã chi hơn 80% ngân sách",
                    color = Color(0xFFF59E0B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Hiển thị ghi chú nếu có
            if (!budget.note.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = "Ghi chú",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(12.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = budget.note ?: "",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
// Hàm parse màu từ string (lấy từ BudgetScreen)
private fun parseColor(colorString: String): Color {
    return try {
        val color = colorString.toColorInt()
        Color(color)
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }
}

@Composable
private fun NoBudgetPlaceholder(navController: NavController) {
    // Giữ nguyên như trước
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💰",
            fontSize = 36.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Chưa có ngân sách nào",
            color = Color(0xFF0F172A),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Thiết lập ngân sách để quản lý chi tiêu hiệu quả hơn",
            color = Color(0xFF64748B),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                navController.navigate("add_budget")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Thiết lập ngân sách",
                fontSize = 14.sp,
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