package com.example.financeapp.screen.main.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.components.ui.BottomNavBar
import com.example.financeapp.viewmodel.savings.SavingsViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun HomeScreen(
    navController: NavController,
    onAddTransaction: () -> Unit,
    currentUser: com.example.financeapp.data.models.User?,
    transactions: List<Transaction>,
    onCalendarClick: () -> Unit,
    budgetViewModel: com.example.financeapp.viewmodel.budget.BudgetViewModel,
    categoryViewModel: com.example.financeapp.viewmodel.transaction.CategoryViewModel,
    savingsViewModel: SavingsViewModel
) {
    val savingsGoals by savingsViewModel.savingsGoals.collectAsState()
    val totalIncome by savingsViewModel.totalIncome.collectAsState()
    val totalExpense by savingsViewModel.totalExpense.collectAsState()
    val availableSavings by savingsViewModel.availableSavings.collectAsState()
    val isLoading by savingsViewModel.isLoading.collectAsState()

    var selectedTransactionTab by remember { mutableStateOf(0) }
    val currentMonth = remember { getCurrentMonthYear() }

    val currentMonthTransactions = remember(transactions, currentMonth) {
        transactions.filter {
            val transactionMonth = formatMonthYear(parseDate(it.date))
            transactionMonth == currentMonth
        }
    }

    val monthlySpent = if (totalExpense > 0) totalExpense.toFloat() else {
        remember(currentMonthTransactions) {
            currentMonthTransactions
                .filter { !it.isIncome }
                .sumOf { it.amount }
                .toFloat()
        }
    }

    val monthlyIncome = if (totalIncome > 0) totalIncome.toFloat() else {
        remember(currentMonthTransactions) {
            currentMonthTransactions
                .filter { it.isIncome }
                .sumOf { it.amount }
                .toFloat()
        }
    }

    // Lấy dữ liệu tháng trước để tính % thay đổi
    val previousMonthData = remember(transactions, currentMonth) {
        getPreviousMonthData(transactions, currentMonth)
    }

    val previousMonthIncome = previousMonthData.first
    val previousMonthExpense = previousMonthData.second

    val incomeChange = remember(monthlyIncome, previousMonthIncome) {
        calculateRealChangeText(currentAmount = monthlyIncome, previousAmount = previousMonthIncome)
    }

    val expenseChange = remember(monthlySpent, previousMonthExpense) {
        calculateRealChangeText(currentAmount = monthlySpent, previousAmount = previousMonthExpense)
    }

    val last7DaysData = remember(transactions) {
        getLast7DaysSpending(transactions)
    }

    val total7DaysSpent = remember(last7DaysData) {
        last7DaysData.sumOf { it.second.toDouble() }.toFloat()
    }

    val recentTransactions = remember(transactions) {
        transactions.sortedByDescending { parseDate(it.date) }.take(3)
    }

    LaunchedEffect(Unit) {
        savingsViewModel.loadSavingsGoals()
        savingsViewModel.calculateAutoSavings()
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController = navController) },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeaderSection(
                    currentUser = currentUser,
                    isLoading = isLoading
                )
            }

            item {
                IncomeExpenseSection(
                    monthlyIncome = monthlyIncome,
                    monthlySpent = monthlySpent,
                    incomeChange = incomeChange,
                    expenseChange = expenseChange,
                    availableSavings = availableSavings.toFloat()
                )
            }

            item {
                MonthlyGoalCard(
                    navController = navController,
                    monthlySpent = monthlySpent,
                    monthlyIncome = monthlyIncome,
                    savingsGoals = savingsGoals
                )
            }

            item {
                RecentTransactionsCard(
                    selectedTab = selectedTransactionTab,
                    onTabSelected = { selectedTransactionTab = it },
                    recentTransactions = recentTransactions,
                    onAddTransaction = onAddTransaction
                )
            }

            item {
                SpendingChartCard(
                    chartData = last7DaysData,
                    totalSpent = total7DaysSpent
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun HeaderSection(
    currentUser: com.example.financeapp.data.models.User?,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Xin chào,",
                color = Color(0xFF64748B),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${currentUser?.name ?: "Người dùng"}!",
                color = Color(0xFF0F172A),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            if (isLoading) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Đang tải dữ liệu...",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun IncomeExpenseSection(
    monthlyIncome: Float,
    monthlySpent: Float,
    incomeChange: String,
    expenseChange: String,
    availableSavings: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IncomeExpenseCard(
                title = "Thu nhập",
                amount = monthlyIncome,
                changeText = incomeChange,
                changeColor = Color(0xFF10B981),
                amountColor = Color(0xFF0F172A),
                modifier = Modifier.weight(1f)
            )
            IncomeExpenseCard(
                title = "Chi tiêu",
                amount = monthlySpent,
                changeText = expenseChange,
                changeColor = Color(0xFFEF4444),
                amountColor = Color(0xFF0F172A),
                modifier = Modifier.weight(1f)
            )
        }

        if (availableSavings > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF0F9FF),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Có thể tiết kiệm",
                            color = Color(0xFF0369A1),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatVND(availableSavings),
                            color = Color(0xFF0C4A6E),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Thêm vào mục tiêu →",
                        color = Color(0xFF0284C7),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            // TODO: Navigate to savings goals
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeExpenseCard(
    title: String,
    amount: Float,
    changeText: String,
    changeColor: Color,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(120.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Tiêu đề
            Text(
                text = title,
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Text(
                text = formatVND(amount),
                color = amountColor,
                fontSize = calculateFontSizeForAmount(amount),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = calculateLineHeightForAmount(amount)
            )

            // % thay đổi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(changeColor, CircleShape)
                )
                Text(
                    text = changeText,
                    color = changeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

private fun calculateFontSizeForAmount(amount: Float): androidx.compose.ui.unit.TextUnit {
    val formattedAmount = formatVND(amount)
    return when {
        formattedAmount.length > 15 -> 14.sp  // Rất dài
        formattedAmount.length > 12 -> 16.sp  // Dài
        formattedAmount.length > 10 -> 18.sp  // Trung bình
        formattedAmount.length > 8 -> 20.sp   // Ngắn
        else -> 22.sp                          // Rất ngắn
    }
}

private fun calculateLineHeightForAmount(amount: Float): androidx.compose.ui.unit.TextUnit {
    val fontSize = calculateFontSizeForAmount(amount)
    return when (fontSize) {
        14.sp -> 18.sp
        16.sp -> 20.sp
        18.sp -> 22.sp
        20.sp -> 24.sp
        else -> 26.sp
    }
}

@Composable
private fun MonthlyGoalCard(
    navController: NavController,
    monthlySpent: Float,
    monthlyIncome: Float,
    savingsGoals: List<com.example.financeapp.data.models.SavingsGoal>
) {
    val monthlySavingsNeeded = if (savingsGoals.isNotEmpty()) {
        val goalsWithDeadline = savingsGoals.filter { it.deadline > 0 }
        if (goalsWithDeadline.isNotEmpty()) {
            goalsWithDeadline.sumOf { it.getMonthlyNeeded() }.toFloat()
        } else {
            monthlyIncome * 0.2f
        }
    } else 0f

    val monthlyActualSavings = (monthlyIncome - monthlySpent).coerceAtLeast(0f)
    val monthlyProgress = if (monthlySavingsNeeded > 0) {
        (monthlyActualSavings / monthlySavingsNeeded * 100).coerceIn(0f, 100f)
    } else if (monthlyIncome > 0) {
        (monthlyActualSavings / monthlyIncome * 100)
    } else 0f

    val activeGoals = savingsGoals.filter { it.isActive && !it.isCompleted }.size

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tiến độ tiết kiệm",
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (savingsGoals.isNotEmpty()) {
                        Text(
                            text = "$activeGoals mục tiêu đang hoạt động",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = "Xem chi tiết",
                    color = Color(0xFF3B82F6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        navController.navigate("savings_goals")
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 12.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        drawArc(
                            color = Color(0xFFE2E8F0),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - radius,
                                center.y - radius
                            ),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        drawArc(
                            color = getProgressColor(monthlyProgress),
                            startAngle = -90f,
                            sweepAngle = 360f * monthlyProgress / 100,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - radius,
                                center.y - radius
                            ),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${monthlyProgress.toInt()}%",
                            color = Color(0xFF0F172A),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "tiến độ",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatVND(monthlyActualSavings),
                        color = Color(0xFF0F172A),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Tiết kiệm thực tế / tháng",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (monthlySavingsNeeded > 0) {
                        Text(
                            text = "Cần ${formatVND(monthlySavingsNeeded)} mỗi tháng",
                            color = Color(0xFF10B981),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(getProgressColor(monthlyProgress), CircleShape)
                            )
                            Text(
                                text = "Tiết kiệm ${monthlyProgress.toInt()}%",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFE2E8F0), CircleShape)
                            )
                            Text(
                                text = "Chi tiêu ${(100 - monthlyProgress).toInt()}%",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsCard(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    recentTransactions: List<Transaction>,
    onAddTransaction: () -> Unit
) {
    val filteredTransactions = remember(recentTransactions, selectedTab) {
        when (selectedTab) {
            0 -> recentTransactions
            1 -> recentTransactions.filter { it.isIncome }
            else -> recentTransactions.filter { !it.isIncome }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Giao dịch gần đây",
                color = Color(0xFF0F172A),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp)),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                TabButton(
                    text = "Tất cả",
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.weight(1f)
                )

                TabButton(
                    text = "Thu nhập",
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.weight(1f)
                )

                TabButton(
                    text = "Chi tiêu",
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelected(2) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (filteredTransactions.isEmpty()) {
                NoTransactionsPlaceholder(onAddTransaction)
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    filteredTransactions.forEachIndexed { index, transaction ->
                        TransactionItem(transaction = transaction)
                        if (index < filteredTransactions.size - 1) {
                            Divider(
                                color = Color(0xFFF1F5F9),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color.White else Color.Transparent
    val textColor = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .background(backgroundColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun TransactionItem(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = transaction.title.ifBlank {
                    transaction.description.ifBlank { "Giao dịch" }
                },
                color = Color(0xFF0F172A),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (transaction.isIncome) "Thu nhập" else "Chi tiêu",
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = if (transaction.isIncome) {
                formatVND(transaction.amount.toFloat())
            } else {
                "-${formatVND(transaction.amount.toFloat())}"
            },
            color = if (transaction.isIncome) Color(0xFF10B981) else Color(0xFF0F172A),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NoTransactionsPlaceholder(onAddTransaction: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Text(
            text = "Chưa có giao dịch nào",
            color = Color(0xFF64748B),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onAddTransaction,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF3B82F6),
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, Color(0xFF3B82F6))
        ) {
            Text(
                text = "Thêm giao dịch đầu tiên",
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SpendingChartCard(
    chartData: List<Pair<String, Float>>,
    totalSpent: Float
) {
    val displayChartData = remember(chartData) {
        if (chartData.size == 7) {
            chartData
        } else {
            listOf(
                "T2" to 0f,
                "T3" to 0f,
                "T4" to 0f,
                "T5" to 0f,
                "T6" to 0f,
                "T7" to 0f,
                "CN" to 0f
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Phân tích chi tiêu",
                color = Color(0xFF0F172A),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "7 ngày gần đây",
                color = Color(0xFF64748B),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng cộng",
                    color = Color(0xFF0F172A),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = formatVND(totalSpent),
                    color = Color(0xFF0F172A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            ColumnChart(chartData = displayChartData)
        }
    }
}

@Composable
private fun ColumnChart(chartData: List<Pair<String, Float>>) {
    val maxValue = max(chartData.maxOfOrNull { it.second } ?: 1f, 1f)
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = (size.width - 40.dp.toPx()) / chartData.size
                val maxBarHeight = size.height - 40.dp.toPx()

                // Vẽ grid lines
                for (i in 1..3) {
                    val gridY = (size.height - 40.dp.toPx()) * (i / 4f)
                    drawLine(
                        color = Color(0xFFF1F5F9),
                        start = Offset(20.dp.toPx(), gridY),
                        end = Offset(size.width - 20.dp.toPx(), gridY),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Vẽ các cột
                chartData.forEachIndexed { index, (_, value) ->
                    val barHeight = (value / maxValue) * maxBarHeight
                    val x = 20.dp.toPx() + index * barWidth
                    val y = size.height - 40.dp.toPx() - barHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4F46E5),
                                Color(0xFF3B82F6)
                            ),
                            startY = y,
                            endY = y + barHeight
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth - 6.dp.toPx(), barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            x = 4.dp.toPx(),
                            y = 4.dp.toPx()
                        )
                    )
                }

                // Vẽ trục Y
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(16.dp.toPx(), 12.dp.toPx()),
                    end = Offset(16.dp.toPx(), size.height - 40.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }

        // Hiển thị label ngày
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            chartData.forEach { (day, _) ->
                Text(
                    text = day,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun calculateRealChangeText(currentAmount: Float, previousAmount: Float): String {
    return if (previousAmount > 0) {
        val changePercent = ((currentAmount - previousAmount) / previousAmount * 100).toInt()
        if (changePercent >= 0) {
            "+$changePercent% so với tháng trước"
        } else {
            "$changePercent% so với tháng trước"
        }
    } else if (currentAmount > 0) {
        "+100% so với tháng trước"
    } else {
        "0% so với tháng trước"
    }
}

// HÀM LẤY DỮ LIỆU THÁNG TRƯỚC
private fun getPreviousMonthData(transactions: List<Transaction>, currentMonth: String): Pair<Float, Float> {
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    val currentDate = sdf.parse(currentMonth) ?: Date()

    calendar.time = currentDate
    calendar.add(Calendar.MONTH, -1)
    val previousMonth = sdf.format(calendar.time)

    var previousIncome = 0f
    var previousExpense = 0f

    transactions.forEach { transaction ->
        try {
            val transactionDate = parseDate(transaction.date)
            val transactionMonth = formatMonthYear(transactionDate)

            if (transactionMonth == previousMonth) {
                val amount = transaction.amount.toFloat()
                if (transaction.isIncome) {
                    previousIncome += amount
                } else {
                    previousExpense += amount
                }
            }
        } catch (_: Exception) {
            // Bỏ qua lỗi parse
        }
    }

    return Pair(previousIncome, previousExpense)
}

// CÁC HÀM HELPER KHÁC
private fun getProgressColor(progress: Float): Color {
    return when {
        progress >= 80 -> Color(0xFF10B981)
        progress >= 50 -> Color(0xFF3B82F6)
        progress >= 30 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
}

private fun getLast7DaysSpending(transactions: List<Transaction>): List<Pair<String, Float>> {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    val spendingByDate = mutableMapOf<String, Float>()

    transactions.forEach { transaction ->
        if (!transaction.isIncome) {
            try {
                val transactionDate = parseDate(transaction.date)
                val dateKey = dateFormat.format(transactionDate)
                val currentSpending = spendingByDate.getOrDefault(dateKey, 0f)
                spendingByDate[dateKey] = currentSpending + transaction.amount.toFloat()
            } catch (_: Exception) {
            }
        }
    }

    val result = mutableListOf<Pair<String, Float>>()
    val daysOfWeek = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")

    for (i in 6 downTo 0) {
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -i)

        val dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val dayOfWeek = if (dayOfWeekIndex == 0) "CN" else daysOfWeek[dayOfWeekIndex]
        val dateKey = dateFormat.format(calendar.time)
        val spending = spendingByDate.getOrDefault(dateKey, 0f)

        result.add(dayOfWeek to spending)
    }

    val sundayData = result.find { it.first == "CN" }
    val otherDaysData = result.filter { it.first != "CN" }

    val sortedOtherDays = otherDaysData.sortedBy {
        when (it.first) {
            "T2" -> 0
            "T3" -> 1
            "T4" -> 2
            "T5" -> 3
            "T6" -> 4
            "T7" -> 5
            else -> 6
        }
    }

    return if (sundayData != null) {
        sortedOtherDays + sundayData
    } else {
        sortedOtherDays
    }
}

private fun parseDate(dateString: String): Date {
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString) ?: Date()
    } catch (_: Exception) {
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

private fun com.example.financeapp.data.models.SavingsGoal.calculateProgress(): Float {
    return if (this.targetAmount > 0) {
        ((this.currentAmount / this.targetAmount) * 100).toFloat().coerceIn(0f, 100f)
    } else 0f
}

private fun com.example.financeapp.data.models.SavingsGoal.getMonthlyNeeded(): Double {
    if (this.deadline <= 0 || this.isCompleted) return 0.0
    val remaining = (this.targetAmount - this.currentAmount).coerceAtLeast(0L).toDouble()
    return remaining / this.deadline
}
data class UserSession(
    val id: String,
    val email: String,
    val name: String,
    val avatar: String?
)