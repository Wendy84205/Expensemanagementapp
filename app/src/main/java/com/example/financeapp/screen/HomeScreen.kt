package com.example.financeapp.screen

import android.graphics.Paint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
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
import com.example.financeapp.Transaction
import com.example.financeapp.components.BottomNavBar
import com.example.financeapp.rememberLanguageText
import kotlin.math.abs
import java.util.*

enum class ChartType { LINE, COLUMN, PIE }

@Composable
fun HomeScreen(
    navController: NavController,
    onAddTransaction: () -> Unit,
    currentUser: UserSession?,
    transactions: List<Transaction>
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF5F7FA))
    )

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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { HeaderSection(currentUser) }
                item { TrendChartCard(transactions) }
            }
        }
    }
}

@Composable
private fun HeaderSection(currentUser: UserSession?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                rememberLanguageText("greeting"),
                color = Color(0xFF718096),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                currentUser?.name ?: rememberLanguageText("user"),
                color = Color(0xFF2D3748),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFE2E8F0), CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentUser?.name?.firstOrNull()?.toString() ?: "U",
                color = Color(0xFF2D3748),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun TrendChartCard(transactions: List<Transaction>) {
    var chartType by remember { mutableStateOf(ChartType.LINE) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        rememberLanguageText("financial_trend"),
                        color = Color(0xFF2D3748),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "${rememberLanguageText("month")} ${getCurrentMonth()}",
                        color = Color(0xFF718096),
                        fontSize = 12.sp
                    )
                }
                ChartTypeSelector(chartType) { chartType = it }
            }

            Spacer(Modifier.height(16.dp))
            MonthlySummary(transactions)
            Spacer(Modifier.height(16.dp))

            // Biểu đồ với kích thước cố định
            when (chartType) {
                ChartType.LINE -> AnimatedLineChart(transactions)
                ChartType.COLUMN -> AnimatedColumnChart(transactions)
                ChartType.PIE -> AnimatedPieChart(transactions)
            }
        }
    }
}

@Composable
private fun ChartTypeSelector(currentType: ChartType, onTypeChanged: (ChartType) -> Unit) {
    Row(
        modifier = Modifier
            .background(Color(0xFFF5F7FA), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        val types = listOf(
            ChartType.LINE to Icons.Default.ShowChart,
            ChartType.COLUMN to Icons.Default.BarChart,
            ChartType.PIE to Icons.Default.PieChart
        )
        types.forEach { (type, icon) ->
            Box(
                modifier = Modifier
                    .background(if (currentType == type) Color(0xFF0F4C75) else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { onTypeChanged(type) }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = type.name,
                    tint = if (currentType == type) Color.White else Color(0xFF718096),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthlySummary(transactions: List<Transaction>) {
    val currentMonthTransactions = transactions.filter {
        it.date.endsWith("/${getCurrentMonthYear()}")
    }

    val totalIncome = currentMonthTransactions.filter { it.isIncome }.sumOf { it.amount }.toFloat()
    val totalExpense = currentMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }.toFloat()
    val netAmount = totalIncome - totalExpense

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F7FA), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Hiển thị số tiền chính - bố cục đơn giản hơn
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FinanceItem(
                title = rememberLanguageText("income"),
                amount = totalIncome,
                color = Color(0xFF2E8B57),
                icon = "↑"
            )
            FinanceItem(
                title = rememberLanguageText("spending"),
                amount = totalExpense,
                color = Color(0xFFED8936),
                icon = "↓"
            )
            FinanceItem(
                title = rememberLanguageText("total"),
                amount = netAmount,
                color = if (netAmount >= 0) Color(0xFF2E8B57) else Color(0xFFED8936),
                icon = if (netAmount >= 0) "↗" else "↘"
            )
        }
    }
}

@Composable
private fun FinanceItem(title: String, amount: Float, color: Color, icon: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = icon,
                color = color,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = title,
                color = Color(0xFF718096),
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = formatCurrencyCompact(amount),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun AnimatedLineChart(transactions: List<Transaction>) {
    val groupedData = getLast7DaysData(transactions)

    if (groupedData.isEmpty()) {
        PlaceholderChart("No data available")
        return
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { animatedProgress.animateTo(1f, animationSpec = tween(1000)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val values = groupedData.map { it.second }
            val maxY = values.maxOrNull() ?: 1f
            val minY = values.minOrNull() ?: 0f
            val rangeY = (maxY - minY).takeIf { it != 0f } ?: 1f

            val paddingTop = 40f
            val paddingBottom = 30f
            val chartHeight = size.height - paddingTop - paddingBottom
            val stepX = (size.width - 40f) / (groupedData.size - 1)

            // Vẽ grid lines
            for (i in 0..4) {
                val y = paddingTop + chartHeight * (1 - i / 4f)
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(20f, y),
                    end = Offset(size.width - 20f, y),
                    strokeWidth = 1f
                )
            }

            // Vẽ đường line với animation đơn giản
            for (i in 0 until values.size - 1) {
                val x1 = 20f + i * stepX
                val y1 = paddingTop + chartHeight - ((values[i] - minY) / rangeY * chartHeight)
                val x2 = 20f + (i + 1) * stepX
                val y2 = paddingTop + chartHeight - ((values[i + 1] - minY) / rangeY * chartHeight)

                // Vẽ đoạn đường với animation
                val currentY2 = y1 + (y2 - y1) * animatedProgress.value

                drawLine(
                    color = Color(0xFF0F4C75),
                    start = Offset(x1, y1),
                    end = Offset(x2, currentY2),
                    strokeWidth = 3f
                )

                // Vẽ điểm
                drawCircle(
                    color = Color(0xFF0F4C75),
                    center = Offset(x1, y1),
                    radius = 4f
                )

                // Vẽ điểm cuối cùng
                if (i == values.size - 2) {
                    drawCircle(
                        color = Color(0xFF0F4C75),
                        center = Offset(x2, y2),
                        radius = 4f
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedColumnChart(transactions: List<Transaction>) {
    val groupedData = getLast7DaysData(transactions)

    if (groupedData.isEmpty()) {
        PlaceholderChart("No data available")
        return
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { animatedProgress.animateTo(1f, animationSpec = tween(1000)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val values = groupedData.map { it.second }
            val maxValue = maxOf(values.maxOrNull() ?: 1f, 1f)

            val paddingTop = 40f
            val paddingBottom = 30f
            val chartHeight = size.height - paddingTop - paddingBottom
            val columnWidth = (size.width - 60f) / groupedData.size
            val spacing = 8f

            // Vẽ grid lines
            for (i in 0..4) {
                val y = paddingTop + chartHeight * (1 - i / 4f)
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(20f, y),
                    end = Offset(size.width - 20f, y),
                    strokeWidth = 1f
                )
            }

            // Vẽ các cột
            values.forEachIndexed { index, value ->
                val columnHeight = (value / maxValue * chartHeight) * animatedProgress.value
                val x = 30f + index * (columnWidth + spacing)
                val y = paddingTop + chartHeight - columnHeight

                drawRoundRect(
                    color = if (value >= 0) Color(0xFF2E8B57) else Color(0xFFED8936),
                    topLeft = Offset(x, y),
                    size = Size(columnWidth, columnHeight),
                    cornerRadius = CornerRadius(x = 4f, y = 4f)
                )

                // Hiển thị giá trị trên cột (đơn giản hóa)
                if (value != 0f && columnHeight > 20f) {
                    drawContext.canvas.nativeCanvas.drawText(
                        formatCurrencyCompact(value),
                        x + columnWidth / 2,
                        y - 8f,
                        Paint().apply {
                            color = android.graphics.Color.parseColor("#2D3748")
                            textSize = 18f
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedPieChart(transactions: List<Transaction>) {
    val categoryData = transactions.filter { !it.isIncome }
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amount }.toFloat() }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    if (categoryData.isEmpty()) {
        PlaceholderChart("No expense data")
        return
    }

    val totalExpense = categoryData.sumOf { it.second.toDouble() }.toFloat()
    val colors = listOf(
        Color(0xFF0F4C75),
        Color(0xFF2E8B57),
        Color(0xFFED8936),
        Color(0xFFF56565),
        Color(0xFF48BB78)
    )

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { animatedProgress.animateTo(1f, animationSpec = tween(1000)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Biểu đồ tròn
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = 0f
                    val radius = size.minDimension * 0.35f
                    val center = Offset(size.width / 2, size.height / 2)

                    categoryData.forEachIndexed { index, (_, amount) ->
                        val sweepAngle = (amount / totalExpense) * 360f * animatedProgress.value
                        drawArc(
                            color = colors.getOrElse(index) { Color.Gray },
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                        startAngle += sweepAngle
                    }
                }
            }

            // Legend - bố cục dọc
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                categoryData.forEachIndexed { index, (category, amount) ->
                    PieLegendItem(
                        category = category,
                        amount = amount,
                        color = colors.getOrElse(index) { Color.Gray },
                        percentage = (amount / totalExpense * 100f).toInt()
                    )
                    if (index < categoryData.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PieLegendItem(
    category: String,
    amount: Float,
    color: Color,
    percentage: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2D3748),
                maxLines = 1
            )
            Text(
                text = "${formatCurrencyCompact(amount)} • $percentage%",
                fontSize = 11.sp,
                color = Color(0xFF718096)
            )
        }
    }
}

@Composable
private fun PlaceholderChart(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFFF5F7FA), RoundedCornerShape(12.dp)),
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
                color = Color(0xFF718096),
                fontSize = 14.sp
            )
        }
    }
}

private fun getLast7DaysData(transactions: List<Transaction>): List<Pair<String, Float>> {
    // Lấy dữ liệu 7 ngày gần nhất và convert Double to Float
    return transactions
        .groupBy { it.date }
        .mapValues { (_, list) ->
            list.sumOf { if (it.isIncome) it.amount else -it.amount }.toFloat()
        }
        .toList()
        .sortedBy { it.first }
        .takeLast(7)
}

// Hàm format tiền compact
fun formatCurrencyCompact(amount: Float): String {
    return when {
        abs(amount) >= 1000000 -> String.format("%,.1fM", amount / 1000000)
        abs(amount) >= 1000 -> String.format("%,.0fK", amount / 1000)
        else -> String.format("%,.0f", amount)
    }.replace(",", ".")
}

private fun getCurrentMonthYear(): String {
    val calendar = Calendar.getInstance()
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    return String.format("%02d/%d", month, year)
}

private fun getCurrentMonth(): String {
    val calendar = Calendar.getInstance()
    return (calendar.get(Calendar.MONTH) + 1).toString()
}

data class UserSession(
    val id: String,
    val email: String,
    val name: String,
    val avatar: String
)