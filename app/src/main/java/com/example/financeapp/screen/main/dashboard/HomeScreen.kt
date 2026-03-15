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

import com.example.financeapp.components.theme.getAppColors
import com.example.financeapp.components.ui.AIInsightWidget
import com.example.financeapp.components.ui.ModernBalanceCard
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.CalendarMonth
import com.example.financeapp.data.models.isOverBudget
import com.example.financeapp.data.models.Budget
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.components.utils.formatCurrency as formatVND

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
    val colors = getAppColors()
    val budgets by budgetViewModel.budgets.collectAsState()
    val savingsGoals by savingsViewModel.savingsGoals.collectAsState()
    val totalIncome by savingsViewModel.totalIncome.collectAsState()
    val totalExpense by savingsViewModel.totalExpense.collectAsState()
    val availableSavings by savingsViewModel.availableSavings.collectAsState()
    val isLoading by savingsViewModel.isLoading.collectAsState()

    val currentMonth = remember { getCurrentMonthYear() }
    val currentMonthTransactions = remember(transactions, currentMonth) {
        transactions.filter {
            val transactionMonth = formatMonthYear(parseDate(it.date))
            transactionMonth == currentMonth
        }
    }

    val monthlySpent = totalExpense.toFloat()
    val monthlyIncome = totalIncome.toFloat()
    val totalBalance = transactions.sumOf { if (it.isIncome) it.amount else -it.amount }

    // Mock AI Insight (In real app, fetch from AIViewModel)
    val aiInsight = remember(monthlySpent, monthlyIncome) {
        if (monthlySpent > monthlyIncome * 0.8f) {
            "Bạn đã tiêu hết 80% thu nhập tháng này. Wendy khuyên bạn nên hạn chế mua sắm không thiết yếu."
        } else if (monthlyIncome > 0) {
            "Bạn đang quản lý tài chính rất tốt! Hãy xem xét gửi thêm ${formatVND(availableSavings.toFloat())} vào mục tiêu tiết kiệm."
        } else {
            "Hoan nghênh bạn! Hãy bắt đầu ghi chép chi tiêu đầu tiên để Wendy có thể hỗ trợ bạn tốt nhất."
        }
    }

    val last7DaysData = remember(transactions) { getLast7DaysSpending(transactions) }
    val total7DaysSpent = last7DaysData.sumOf { it.second.toDouble() }.toFloat()
    val recentTransactions = remember(transactions) {
        transactions.sortedByDescending { parseDate(it.date) }.take(5)
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController = navController) },
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = colors.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                ModernHeader(currentUser = currentUser, onCalendarClick = onCalendarClick)
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    AIInsightWidget(
                        insight = aiInsight,
                        modifier = Modifier.padding(vertical = 8.dp),
                        onClick = { navController.navigate("chat_ai") }
                    )
                    
                    ModernBalanceCard(
                        totalBalance = totalBalance,
                        monthlyIncome = monthlyIncome,
                        monthlyExpense = monthlySpent,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Giao dịch gần đây",
                    actionText = "Xem tất cả",
                    onActionClick = { navController.navigate("transactions") }
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (recentTransactions.isEmpty()) {
                            NoTransactionsPlaceholder(onAddTransaction)
                        } else {
                            recentTransactions.forEachIndexed { index, transaction ->
                                TransactionItem(transaction = transaction)
                                if (index < recentTransactions.size - 1) {
                                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Xu hướng chi tiêu")
                SpendingChartCard(
                    chartData = last7DaysData,
                    totalSpent = total7DaysSpent
                )
            }

            // 🔻 NGÂN SÁCH – ĐẶT Ở CUỐI CÙNG
            item {
                BudgetOverviewCard(
                    navController = navController,
                    budgets = budgets
                )
            }

            // Danh sách ngân sách dạng thanh ngang
            item {
                BudgetListSection(
                    budgets = budgets,
                    categories = categoryViewModel.categories.collectAsState().value
                )
            }
        }
    }
}

@Composable
fun ModernHeader(currentUser: com.example.financeapp.data.models.User?, onCalendarClick: () -> Unit) {
    val colors = getAppColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Chào mừng trở lại,",
                color = colors.textSecondary,
                fontSize = 14.sp
            )
            Text(
                text = currentUser?.name ?: "Bạn",
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = getAppColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null) {
            Text(
                text = actionText,
                color = colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onActionClick?.invoke() }
            )
        }
    }
}


@Composable
private fun BudgetOverviewCard(
    navController: NavController,
    budgets: List<Budget>
) {
    val colors = getAppColors()
    val now = remember { java.time.LocalDate.now() }
    val activeBudgets = remember(budgets) {
        budgets.filter { budget ->
            budget.isActive &&
                    !now.isBefore(budget.startDate) &&
                    !now.isAfter(budget.endDate)
        }
    }

    val totalBudget = activeBudgets.sumOf { it.amount }.toFloat()
    val totalSpent = activeBudgets.sumOf { it.spentAmount }.toFloat()
    val remaining = (totalBudget - totalSpent).coerceAtLeast(0f)
    val usagePercent = if (totalBudget > 0f) {
        (totalSpent / totalBudget * 100f).coerceIn(0f, 100f)
    } else 0f

    val exceededCount = activeBudgets.count { it.isOverBudget }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        text = "Ngân sách tháng này",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (activeBudgets.isNotEmpty()) {
                        Text(
                            text = "${activeBudgets.size} ngân sách đang hoạt động",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "Chưa có ngân sách nào",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = if (activeBudgets.isNotEmpty()) "Quản lý" else "Tạo ngân sách",
                    color = colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        navController.navigate(if (activeBudgets.isNotEmpty()) "budgets" else "add_budget")
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (activeBudgets.isEmpty()) {
                Text(
                    text = "Hãy thêm ngân sách để quản lí tiền tốt hơn.",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = usagePercent / 100f,
                        animationSpec = tween(durationMillis = 1000),
                        label = "budgetUsageProgress"
                    )

                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 10.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2
                            val center = Offset(size.width / 2, size.height / 2)

                            drawArc(
                                color = colors.background,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            drawArc(
                                brush = if (usagePercent >= 80) colors.expenseGradient else colors.accentGradient,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress.coerceIn(0f, 1f),
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${usagePercent.toInt()}%",
                                color = colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = formatVND(remaining),
                            color = colors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Số dư ngân sách còn lại",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )

                        if (totalBudget > 0f) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.secondary.copy(alpha = 0.1f),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "Đã dùng ${formatVND(totalSpent)} / ${formatVND(totalBudget)}",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (exceededCount > 0) {
                            Text(
                                text = "$exceededCount ngân sách đã vượt giới hạn",
                                color = colors.expense,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetListSection(
    budgets: List<Budget>,
    categories: List<Category>
) {
    val colors = getAppColors()
    val now = remember { java.time.LocalDate.now() }
    val activeBudgets = remember(budgets, categories) {
        budgets.filter { budget ->
            budget.isActive &&
                    !now.isBefore(budget.startDate) &&
                    !now.isAfter(budget.endDate)
        }.sortedByDescending { it.spentAmount / (it.amount.takeIf { a -> a > 0 } ?: 1.0) }
    }

    if (activeBudgets.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        SectionHeader(title = "Ngân sách hiện tại")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            activeBudgets.forEach { budget ->
                BudgetHorizontalItem(
                    budget = budget,
                    categoryName = categories.find { it.id == budget.categoryId }?.name ?: "Danh mục",
                    colors = colors
                )
            }
        }
    }
}

@Composable
private fun BudgetHorizontalItem(
    budget: Budget,
    categoryName: String,
    colors: com.example.financeapp.components.theme.AppColors
) {
    val total = budget.amount.toFloat()
    val spent = budget.spentAmount.toFloat()
    val progress = if (total > 0f) (spent / total).coerceIn(0f, 1f) else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "budgetItemProgress"
    )

    val barColor = when {
        progress < 0.7f -> colors.income
        progress < 0.9f -> colors.secondary
        else -> colors.expense
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryName,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatVND(spent)} / ${formatVND(total)}",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }

            Text(
                text = "${(progress * 100).toInt()}%",
                color = barColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.background, RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .height(6.dp)
                    .background(barColor, RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction) {
    val colors = getAppColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (transaction.isIncome) colors.secondary.copy(0.1f) else colors.expense.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (transaction.isIncome) colors.secondary else colors.expense,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = transaction.title.ifBlank { transaction.category.ifBlank { "Giao dịch" } },
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.date,
                    color = colors.textMuted,
                    fontSize = 12.sp
                )
            }
        }

        Text(
            text = (if (transaction.isIncome) "+" else "-") + formatVND(transaction.amount.toFloat()),
            color = if (transaction.isIncome) colors.income else colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
private fun NoTransactionsPlaceholder(onAddTransaction: () -> Unit) {
    val colors = getAppColors()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(24.dp)
    ) {
        Text("Chưa có giao dịch", color = colors.textSecondary)
        TextButton(onClick = onAddTransaction) {
            Text("Thêm ngay", color = colors.primary)
        }
    }
}

@Composable
private fun SpendingChartCard(
    chartData: List<Pair<String, Float>>,
    totalSpent: Float
) {
    val colors = getAppColors()
    val displayChartData = remember(chartData) {
        if (chartData.size == 7) chartData else listOf(
            "T2" to 0f, "T3" to 0f, "T4" to 0f, "T5" to 0f, "T6" to 0f, "T7" to 0f, "CN" to 0f
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Phân tích tuần",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng chi tiêu",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )

                Text(
                    text = formatVND(totalSpent),
                    color = colors.expense,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            ColumnChart(chartData = displayChartData)
        }
    }
}


@Composable
private fun ColumnChart(chartData: List<Pair<String, Float>>) {
    val maxValue = max(chartData.maxOfOrNull { it.second } ?: 1f, 1f)
    
    // Entrance animation
    var animationTriggered by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "chartAnim"
    )
    
    LaunchedEffect(Unit) {
        animationTriggered = true
    }

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
                    val barHeight = (value / maxValue) * maxBarHeight * animatedProgress
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