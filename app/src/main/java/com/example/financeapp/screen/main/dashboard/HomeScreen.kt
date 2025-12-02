package com.example.financeapp.screen

import android.graphics.Paint
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
import com.example.financeapp.components.BottomNavBar
import com.example.financeapp.formatCurrency
import com.example.financeapp.rememberLanguageText
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max

@Composable
fun HomeScreen(
    navController: NavController,
    onAddTransaction: () -> Unit,
    currentUser: UserSession?,
    transactions: List<Transaction>
) {
    // Tạo các state cho dữ liệu động
    var selectedFundId by remember { mutableStateOf<String?>(null) }
    var monthlySpendingLimit by remember { mutableStateOf(0f) }

    // Lấy danh sách quỹ tiết kiệm (giả lập - thay bằng data thực từ database)
    val savingFunds = remember {
        listOf(
            SavingFund(
                id = "fund_001",
                name = "Quỹ tiết kiệm chung",
                description = "Quỹ dành cho các khoản chi tiêu hàng ngày",
                balance = 0f,
                targetAmount = null,
                targetDate = null,
                color = null
            )
        )
    }

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
            .sumOf { it.amount.toDouble() }
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
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HeaderSection(
                        currentUser = currentUser,
                        monthlySpent = monthlySpent
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
                        last7DaysData = last7DaysData,
                        monthlySpent = monthlySpent
                    )
                }
                item {
                    SpendingLimitCard(
                        savingFunds = savingFunds,
                        monthlySpendingLimit = monthlySpendingLimit,
                        monthlySpent = monthlySpent,
                        onSelectFund = { fund ->
                            selectedFundId = fund?.id
                            // Cập nhật hạn mức chi tiêu dựa trên quỹ
                            monthlySpendingLimit = fund?.balance ?: 0f
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    currentUser: UserSession?,
    monthlySpent: Float
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
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFFE2E8F0), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser?.name?.firstOrNull()?.toString()?.uppercase() ?: "U",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        // Số tiền đã chi trong tháng
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = rememberLanguageText("monthly_spending_title"),
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatCurrency(monthlySpent),
                        color = Color(0xFF0F172A),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rememberLanguageText("view_details"),
                        color = Color(0xFF3B82F6),
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { /* Xem chi tiết */ }
                    )
                }
                Text(
                    text = rememberLanguageText("spent_this_month"),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
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
        // SỬA LỖI Ở ĐÂY: thay vì dùng items(), dùng forEach hoặc for loop
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
                Text(
                    text = transaction.category,
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.date,
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
                if (transaction.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
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
                    text = rememberLanguageText("overview"),
                    color = Color(0xFF0F172A),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${rememberLanguageText("this_month")} ${formatCurrency(monthlySpent)}",
                    color = Color(0xFF3B82F6),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        // Navigate to statistics
                        // navController.navigate("statistics")
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
private fun SpendingLimitCard(
    savingFunds: List<SavingFund>,
    monthlySpendingLimit: Float,
    monthlySpent: Float,
    onSelectFund: (SavingFund?) -> Unit
) {
    val spendingPercentage = remember(monthlySpendingLimit, monthlySpent) {
        if (monthlySpendingLimit > 0) {
            (monthlySpent / monthlySpendingLimit * 100).coerceAtMost(100f)
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
                    text = rememberLanguageText("spending_limit"),
                    color = Color(0xFF0F172A),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (monthlySpendingLimit > 0) {
                    Text(
                        text = "${spendingPercentage.toInt()}%",
                        color = if (spendingPercentage > 80) Color(0xFFEF4444) else Color(0xFF10B981),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (monthlySpendingLimit > 0) {
                // Hiển thị progress bar nếu đã đặt hạn mức
                SpendingProgressBar(
                    spent = monthlySpent,
                    limit = monthlySpendingLimit,
                    percentage = spendingPercentage
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Mở dialog chọn quỹ hoặc tạo mới
                        onSelectFund(savingFunds.firstOrNull())
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = rememberLanguageText("create_or_select_fund_for_limit"),
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = rememberLanguageText("limit_description"),
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (savingFunds.isNotEmpty()) {
                                onSelectFund(savingFunds.first())
                            } else {
                                // Mở dialog tạo quỹ mới
                                onSelectFund(null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = rememberLanguageText("select_or_create_fund"),
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
private fun SpendingProgressBar(
    spent: Float,
    limit: Float,
    percentage: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Đã chi: ${formatCurrency(spent)}",
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
            Text(
                text = "Hạn mức: ${formatCurrency(limit)}",
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

// Hàm helper cho chuỗi ngôn ngữ
@Composable
fun rememberLanguageText(key: String): String {
    return when (key) {
        "greeting" -> "Xin chào"
        "user" -> "Người dùng"
        "monthly_spending_title" -> "Số tiền bạn đã chi trong tháng"
        "view_details" -> "Xem chi tiết"
        "view_all" -> "Xem tất cả"
        "spent_this_month" -> "Số tiền đã chi tiêu trong tháng này"
        "classification_by_type" -> "Chi theo phân loại"
        "create_or_select_fund" -> "Tạo hoặc lựa chọn quỹ tiết kiệm"
        "fund_description" -> "để chúng tôi giúp bạn quản lý tài chính hiệu quả"
        "balance" -> "Số dư"
        "recent_transactions" -> "Giao dịch gần đây"
        "income" -> "Thu"
        "expense" -> "Chi"
        "no_recent_transactions" -> "Bạn chưa có giao dịch gần đây"
        "create_transaction" -> "Tạo giao dịch"
        "overview" -> "Tổng quan"
        "this_month" -> "Tháng này"
        "spending_limit" -> "Hạn mức chi tiêu"
        "create_or_select_fund_for_limit" -> "Tạo hoặc lựa chọn quỹ tiết kiệm"
        "limit_description" -> "để chúng tôi tính toán hạn mức chi tiêu"
        "select_or_create_fund" -> "Lựa chọn / Tạo quỹ tiết kiệm"
        "no_chart_data" -> "Chưa có dữ liệu biểu đồ"
        else -> key
    }
}

// Data classes
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