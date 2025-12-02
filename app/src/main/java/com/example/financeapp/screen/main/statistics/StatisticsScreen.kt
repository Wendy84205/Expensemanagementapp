package com.example.financeapp.screen.main.statistics

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.components.BottomNavBar
import com.example.financeapp.screen.features.formatCurrency
import java.lang.Math.ceil
import kotlin.math.max
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    transactions: List<Transaction>,
    categoryViewModel: CategoryViewModel = viewModel()
) {
    var selectedTimeRange by remember { mutableStateOf("weekly") }
    var selectedDataType by remember { mutableStateOf("expense") }

    val languageViewModel = LocalLanguageViewModel.current

    val timeRanges = listOf("weekly", "monthly", "yearly")
    val dataTypes = listOf("income", "expense", "difference")

    // Màu sắc theo UI trong ảnh
    val backgroundColor = Color(0xFFF5F7FA)
    val cardColor = Color.White
    val primaryColor = Color(0xFF4A6FA5)
    val textPrimary = Color(0xFF333333)
    val textSecondary = Color(0xFF666666)
    val gridLineColor = Color(0xFFE0E0E0)
    val chartBarColor = Color(0xFF4A6FA5)
    val selectedBarColor = Color(0xFF2E8B57)
    val redColor = Color(0xFFE74C3C)
    val greenColor = Color(0xFF2ECC71)

    // Lấy danh mục từ ViewModel
    val categories by categoryViewModel.categories.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        languageViewModel.getTranslation("statistics"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = { BottomNavBar(navController = navController) },
        containerColor = backgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            // Phần chọn thời gian và loại dữ liệu
            item {
                TimeRangeSelector(
                    selectedTimeRange = selectedTimeRange,
                    timeRanges = timeRanges,
                    onTimeRangeSelected = { selectedTimeRange = it },
                    primaryColor = primaryColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
                DataTypeSelector(
                    selectedDataType = selectedDataType,
                    dataTypes = dataTypes,
                    onDataTypeSelected = { selectedDataType = it },
                    primaryColor = primaryColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            // Tổng quan
            item {
                TotalOverviewCard(
                    dataType = selectedDataType,
                    timeRange = selectedTimeRange,
                    transactions = transactions,
                    primaryColor = primaryColor,
                    textPrimary = textPrimary,
                    accentColor = primaryColor
                )
            }

            // Biểu đồ chi tiết
            item {
                DetailedChartSection(
                    dataType = selectedDataType,
                    timeRange = selectedTimeRange,
                    transactions = transactions,
                    categories = categories,
                    primaryColor = primaryColor,
                    accentColor = greenColor,
                    redColor = redColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    gridLineColor = gridLineColor,
                    chartBarColor = chartBarColor,
                    selectedBarColor = selectedBarColor
                )
            }

            // So sánh cùng kỳ
            item {
                ComparisonSection(
                    dataType = selectedDataType,
                    timeRange = selectedTimeRange,
                    transactions = transactions,
                    primaryColor = primaryColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            // Phân tích theo danh mục
            item {
                CategoryAnalysisSection(
                    dataType = selectedDataType,
                    transactions = transactions,
                    categoryViewModel = categoryViewModel,
                    categories = categories,
                    primaryColor = primaryColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    backgroundColor = backgroundColor
                )
            }
        }
    }
}

@Composable
private fun TimeRangeSelector(
    selectedTimeRange: String,
    timeRanges: List<String>,
    onTimeRangeSelected: (String) -> Unit,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val languageViewModel = LocalLanguageViewModel.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                languageViewModel.getTranslation("time_range"),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                timeRanges.forEach { range ->
                    TimeRangeChip(
                        text = languageViewModel.getTranslation("time_range_$range"),
                        isSelected = range == selectedTimeRange,
                        onClick = { onTimeRangeSelected(range) },
                        primaryColor = primaryColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRangeChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val backgroundColor = if (isSelected) primaryColor else Color.Transparent
    val textColor = if (isSelected) Color.White else textSecondary
    val borderColor = if (isSelected) primaryColor else Color(0xFFE2E8F0)

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun DataTypeSelector(
    selectedDataType: String,
    dataTypes: List<String>,
    onDataTypeSelected: (String) -> Unit,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val languageViewModel = LocalLanguageViewModel.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                languageViewModel.getTranslation("data_type"),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dataTypes.forEach { type ->
                    DataTypeChip(
                        text = languageViewModel.getTranslation("data_type_$type"),
                        isSelected = type == selectedDataType,
                        onClick = { onDataTypeSelected(type) },
                        primaryColor = primaryColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DataTypeChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val textColor = if (isSelected) primaryColor else textSecondary
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    val backgroundColor = if (isSelected) primaryColor.copy(0.1f) else Color.Transparent

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun TotalOverviewCard(
    dataType: String,
    timeRange: String,
    transactions: List<Transaction>,
    primaryColor: Color,
    textPrimary: Color,
    accentColor: Color
) {
    val languageViewModel = LocalLanguageViewModel.current

    val totalAmount = calculateCurrentAmount(dataType, timeRange, transactions)
    val comparisonText = "${languageViewModel.getTranslation("same_period_as")} ${getPreviousTimeRangeText(timeRange, languageViewModel)}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Tổng ${getDataTypeDisplayName(dataType, languageViewModel)}",
                fontSize = 16.sp,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                formatCurrency(totalAmount),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    comparisonText,
                    fontSize = 14.sp,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Info,
                    contentDescription = languageViewModel.getTranslation("info"),
                    tint = textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailedChartSection(
    dataType: String,
    timeRange: String,
    transactions: List<Transaction>,
    categories: List<com.example.financeapp.viewmodel.transaction.Category>,
    primaryColor: Color,
    accentColor: Color,
    redColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    gridLineColor: Color,
    chartBarColor: Color,
    selectedBarColor: Color
) {
    val languageViewModel = LocalLanguageViewModel.current

    // Lấy dữ liệu THẬT theo timeRange
    val chartData = getChartDataByTimeRange(dataType, timeRange, transactions)

    // Tính toán các chỉ số
    val currentPeriodTotal = chartData.sumOf { it.amount }
    val previousPeriodData = getPreviousPeriodData(dataType, timeRange, transactions)
    val previousPeriodTotal = previousPeriodData.sumOf { it.amount }

    // Fix: Tính phần trăm thay đổi đúng cách
    val percentageChange = if (previousPeriodTotal != 0.0) {
        ((currentPeriodTotal - previousPeriodTotal) / previousPeriodTotal * 100)
    } else if (currentPeriodTotal > 0) 100.0 else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Tiêu đề và tổng chi tiêu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (dataType) {
                        "income" -> "Tổng thu nhập"
                        "expense" -> "Tổng chi tiêu"
                        else -> "Tổng chênh lệch"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCurrencyCompact(currentPeriodTotal),
                        fontSize = 16.sp,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (chartData.isNotEmpty() && chartData.any { it.amount > 0 }) {
                        Text(
                            text = if (percentageChange >= 0) "▲ ${"%.1f".format(percentageChange)}%"
                            else "▼ ${"%.1f".format(-percentageChange)}%",
                            fontSize = 14.sp,
                            color = if (percentageChange >= 0) accentColor else redColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BIỂU ĐỒ CHI TIẾT
            if (chartData.isEmpty() || chartData.all { it.amount == 0.0 }) {
                NoDataPlaceholder(textSecondary = textSecondary)
            } else {
                DynamicChartVisualization(
                    chartData = chartData,
                    primaryColor = primaryColor,
                    accentColor = accentColor,
                    redColor = redColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    gridLineColor = gridLineColor,
                    chartBarColor = chartBarColor,
                    selectedBarColor = selectedBarColor,
                    timeRange = timeRange
                )
            }
        }
    }
}
@Composable
fun DynamicChartVisualization(
    chartData: List<ChartData>,
    primaryColor: Color,
    accentColor: Color,
    redColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    gridLineColor: Color,
    chartBarColor: Color,
    selectedBarColor: Color,
    timeRange: String
) {
    // Tìm giá trị lớn nhất trong dữ liệu thực tế
    val maxAmount = chartData.maxOfOrNull { it.amount } ?: 0.0

    // Tìm giá trị lớn nhất KHÔNG PHẢI ZERO để làm tròn
    val maxNonZeroAmount = chartData.filter { it.amount > 0 }.maxOfOrNull { it.amount } ?: 0.0

    // Làm tròn LÊN đến số đẹp (như 25K, 50K, 75K, 100K, 200K, etc)
    val roundedMaxAmount = calculateRoundedMaxValue(maxNonZeroAmount)

    // Tạo 5 bước đều nhau từ 0 đến roundedMaxAmount
    val ySteps = createYAxisSteps(roundedMaxAmount)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        // Trục Y với các giá trị tự động - SỐ LỚN ở TRÊN, 0 ở DƯỚI
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Hiển thị ĐẢO NGƯỢC: số lớn ở trên, 0 ở dưới
            ySteps.reversed().forEach { value ->
                Text(
                    text = formatYAxisLabel(value),
                    fontSize = 12.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // Biểu đồ chính
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, top = 8.dp, bottom = 32.dp, end = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartHeight = size.height
                val chartWidth = size.width

                // Tính toán kích thước cột dựa trên số lượng dữ liệu
                val columnWidth = chartWidth / chartData.size
                val spacing = columnWidth * 0.2f
                val actualColumnWidth = columnWidth - spacing

                // Vẽ đường lưới ngang
                ySteps.forEach { step ->
                    // Tính yPosition: 0 ở ĐÁY biểu đồ (chartHeight), roundedMaxAmount ở ĐỈNH (0)
                    // Sử dụng tỷ lệ ngược: step càng lớn, yPosition càng cao trên canvas
                    val yPosition = chartHeight * (1 - step.toFloat() / roundedMaxAmount.toFloat())

                    // Vẽ đường lưới ngang
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, yPosition),
                        end = Offset(chartWidth, yPosition),
                        strokeWidth = 1f
                    )
                }

                // Vẽ các cột biểu đồ - CỘT MỌC TỪ DƯỚI LÊN
                chartData.forEachIndexed { index, data ->
                    // Tính chiều cao cột: dựa trên tỷ lệ với roundedMaxAmount
                    val columnHeight = if (roundedMaxAmount > 0) {
                        (data.amount.toFloat() / roundedMaxAmount.toFloat()) * chartHeight
                    } else {
                        0f
                    }

                    // Vị trí x của cột
                    val xPosition = index * columnWidth + spacing / 2

                    // Vị trí y BẮT ĐẦU của cột (tính từ TRÊN xuống)
                    // chartHeight - columnHeight: 0 ở đỉnh, cột mọc xuống
                    val yStartPosition = chartHeight - columnHeight

                    // Màu cột: highlight cột cuối cùng
                    val barColor = if (index == chartData.size - 1) selectedBarColor else chartBarColor

                    // Vẽ cột với bo góc trên
                    if (columnHeight > 0) {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(xPosition, yStartPosition),
                            size = Size(actualColumnWidth, columnHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }

                    // Vẽ nhãn dưới cột (ngày/tháng)
                    drawContext.canvas.nativeCanvas.drawText(
                        data.label,
                        xPosition + actualColumnWidth / 2,
                        chartHeight + 20f, // Dưới đáy biểu đồ
                        Paint().apply {
                            color = android.graphics.Color.parseColor("#666666")
                            textSize = 12f
                            textAlign = Paint.Align.CENTER
                            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                        }
                    )

                    // Vẽ giá trị trên đầu cột nếu có dữ liệu
                    if (data.amount > 0 && columnHeight > 20f) {
                        drawContext.canvas.nativeCanvas.drawText(
                            formatCurrencyCompact(data.amount),
                            xPosition + actualColumnWidth / 2,
                            yStartPosition - 8f, // Giá trị ở trên đầu cột
                            Paint().apply {
                                color = android.graphics.Color.parseColor("#4A6FA5")
                                textSize = 10f
                                textAlign = Paint.Align.CENTER
                                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                            }
                        )
                    }
                }
            }
        }
    }
}

// Hàm tính giá trị làm tròn cho trục Y
private fun calculateRoundedMaxValue(maxValue: Double): Double {
    if (maxValue <= 0) return 100000.0 // Mặc định 100K nếu không có dữ liệu

    // Tìm scale factor
    val scale = when {
        maxValue >= 1000000 -> 1000000.0 // Triệu
        maxValue >= 1000 -> 1000.0 // Nghìn
        else -> 1.0
    }

    val scaledValue = maxValue / scale

    // Các số đẹp phổ biến
    val niceNumbers = when {
        scale == 1000.0 -> listOf(25.0, 50.0, 75.0, 100.0, 150.0, 200.0, 250.0, 500.0, 750.0, 1000.0)
        scale == 1000000.0 -> listOf(0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0)
        else -> listOf(10.0, 20.0, 50.0, 100.0, 200.0, 500.0, 1000.0)
    }

    // Tìm số đẹp đầu tiên LỚN HƠN scaledValue
    return (niceNumbers.firstOrNull { it > scaledValue } ?: (scaledValue * 1.2)) * scale
}

// Tạo các bước trục Y từ 0 đến maxValue
private fun createYAxisSteps(maxValue: Double): List<Double> {
    // Tạo 5 bước đều nhau từ 0 đến maxValue
    return List(5) { i ->
        maxValue * i / 4.0 // 0, 0.25, 0.5, 0.75, 1.0
    }
}

// Format nhãn trục Y
private fun formatYAxisLabel(value: Double): String {
    return when {
        value >= 1000000 -> String.format("%.1fM", value / 1000000)
        value >= 1000 -> String.format("%.0fK", value / 1000)
        else -> String.format("%.0f", value)
    }
}

@Composable
private fun NoDataPlaceholder(textSecondary: Color) {
    val languageViewModel = LocalLanguageViewModel.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊",
                fontSize = 32.sp
            )
            Text(
                languageViewModel.getTranslation("no_data"),
                fontSize = 16.sp,
                color = textSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                languageViewModel.getTranslation("no_transactions_time_period"),
                fontSize = 14.sp,
                color = textSecondary
            )
        }
    }
}

@Composable
private fun ComparisonSection(
    dataType: String,
    timeRange: String,
    transactions: List<Transaction>,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val languageViewModel = LocalLanguageViewModel.current

    val (currentData, previousData) = when (timeRange) {
        "weekly" -> {
            val currentWeekData = calculateCurrentWeekAmount(dataType, transactions)
            val previousWeekData = calculatePreviousWeekAmount(dataType, transactions)
            Pair(currentWeekData, previousWeekData)
        }
        "monthly" -> {
            val currentMonthData = calculateCurrentMonthAmount(dataType, transactions)
            val previousMonthData = calculatePreviousMonthAmount(dataType, transactions)
            Pair(currentMonthData, previousMonthData)
        }
        "yearly" -> {
            // So sánh năm nay vs năm trước
            val currentYearData = calculateCurrentYearAmount(dataType, transactions)
            val previousYearData = calculatePreviousYearAmount(dataType, transactions)
            Pair(currentYearData, previousYearData)
        }
        else -> Pair(0.0, 0.0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                getComparisonTitle(timeRange, languageViewModel),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            ComparisonDataRow(
                getCurrentPeriodLabel(timeRange, languageViewModel),
                currentData,
                textPrimary = textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            ComparisonDataRow(
                getPreviousPeriodLabel(timeRange, languageViewModel),
                previousData,
                textPrimary = textPrimary
            )
        }
    }
}

@Composable
private fun ComparisonDataRow(label: String, amount: Double, textPrimary: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 16.sp,
            color = textPrimary
        )

        Text(
            formatCurrency(amount),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textPrimary
        )
    }
}

@Composable
private fun CategoryAnalysisSection(
    dataType: String,
    transactions: List<Transaction>,
    categoryViewModel: CategoryViewModel,
    categories: List<com.example.financeapp.viewmodel.transaction.Category>,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    backgroundColor: Color
) {
    val languageViewModel = LocalLanguageViewModel.current

    // Lấy top 5 danh mục với tên thay vì ID
    val topCategories = getTopCategoriesWithAmount(dataType, transactions, categories)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                languageViewModel.getTranslation("category_analysis"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (topCategories.isEmpty()) {
                NoDataPlaceholder(textSecondary = textSecondary)
            } else {
                CategoryAnalysisContent(topCategories, textPrimary, textSecondary)
            }
        }
    }
}

@Composable
private fun CategoryAnalysisContent(
    categories: List<CategoryAmount>,
    textPrimary: Color,
    textSecondary: Color
) {
    // Hiển thị tiêu đề
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Danh mục",
            fontSize = 14.sp,
            color = textSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            "Số tiền",
            fontSize = 14.sp,
            color = textSecondary,
            fontWeight = FontWeight.Medium
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Hiển thị từng danh mục
    categories.forEach { category ->
        CategoryAnalysisRow(category, textPrimary)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun CategoryAnalysisRow(category: CategoryAmount, textPrimary: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            category.name,
            fontSize = 16.sp,
            color = textPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Text(
            formatCurrency(category.amount),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textPrimary
        )
    }
}

// ==================== HÀM TIỆN ÍCH ====================

// Data classes
data class ChartData(
    val amount: Double,
    val label: String
)

data class CategoryAmount(
    val name: String,
    val amount: Double
)

// Lấy dữ liệu biểu đồ theo timeRange - CẬP NHẬT: tháng lấy 6 tháng
private fun getChartDataByTimeRange(
    dataType: String,
    timeRange: String,
    transactions: List<Transaction>
): List<ChartData> {
    return when (timeRange) {
        "weekly" -> getLastNDaysData(dataType, transactions, 7) // 7 ngày gần nhất
        "monthly" -> getLast6MonthsData(dataType, transactions) // 6 tháng gần nhất
        "yearly" -> getYearlyComparisonData(dataType, transactions) // Năm nay và năm trước
        else -> getLastNDaysData(dataType, transactions, 7)
    }
}

// Lấy dữ liệu N ngày gần nhất
private fun getLastNDaysData(
    dataType: String,
    transactions: List<Transaction>,
    days: Int
): List<ChartData> {
    val result = mutableListOf<ChartData>()
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    for (i in days - 1 downTo 0) {
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -i)
        val date = calendar.time
        val dateKey = dateFormat.format(date)

        val dayTransactions = transactions.filter { transaction ->
            try {
                val transactionDate = parseDate(transaction.date)
                isSameDay(transactionDate, date)
            } catch (e: Exception) {
                false
            }
        }

        val amount = calculateAmountForDataType(dayTransactions, dataType)
        result.add(ChartData(amount, dateKey))
    }

    return result
}

// Lấy dữ liệu 6 tháng gần nhất - MỚI
private fun getLast6MonthsData(
    dataType: String,
    transactions: List<Transaction>
): List<ChartData> {
    val result = mutableListOf<ChartData>()
    val calendar = Calendar.getInstance()
    val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())

    // Đặt về đầu tháng hiện tại
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    for (i in 0..5) { // 6 tháng gần nhất (0: tháng hiện tại, 5: 5 tháng trước)
        val monthCalendar = calendar.clone() as Calendar
        monthCalendar.add(Calendar.MONTH, -i)

        val startOfMonth = monthCalendar.time
        monthCalendar.add(Calendar.MONTH, 1)
        monthCalendar.add(Calendar.DAY_OF_MONTH, -1)
        val endOfMonth = monthCalendar.time

        val monthTransactions = transactions.filter { transaction ->
            try {
                val transactionDate = parseDate(transaction.date)
                transactionDate in startOfMonth..endOfMonth
            } catch (e: Exception) {
                false
            }
        }

        val amount = calculateAmountForDataType(monthTransactions, dataType)
        val monthLabel = SimpleDateFormat("MM/yy", Locale.getDefault()).format(startOfMonth)
        result.add(0, ChartData(amount, monthLabel)) // Thêm vào đầu để đúng thứ tự
    }

    return result.reversed() // Đảo ngược để tháng cũ nhất ở trước
}

// Lấy dữ liệu năm nay và năm trước cho so sánh - MỚI
private fun getYearlyComparisonData(
    dataType: String,
    transactions: List<Transaction>
): List<ChartData> {
    val result = mutableListOf<ChartData>()
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val previousYear = currentYear - 1

    // Dữ liệu năm nay
    val currentYearTransactions = transactions.filter { transaction ->
        val transactionDate = parseDate(transaction.date)
        val transCalendar = Calendar.getInstance().apply { time = transactionDate }
        transCalendar.get(Calendar.YEAR) == currentYear
    }
    val currentYearAmount = calculateAmountForDataType(currentYearTransactions, dataType)
    result.add(ChartData(currentYearAmount, "Năm nay"))

    // Dữ liệu năm trước
    val previousYearTransactions = transactions.filter { transaction ->
        val transactionDate = parseDate(transaction.date)
        val transCalendar = Calendar.getInstance().apply { time = transactionDate }
        transCalendar.get(Calendar.YEAR) == previousYear
    }
    val previousYearAmount = calculateAmountForDataType(previousYearTransactions, dataType)
    result.add(ChartData(previousYearAmount, "Năm trước"))

    return result
}

// Lấy dữ liệu kỳ trước để so sánh
private fun getPreviousPeriodData(
    dataType: String,
    timeRange: String,
    transactions: List<Transaction>
): List<ChartData> {
    return when (timeRange) {
        "weekly" -> getPreviousWeekData(dataType, transactions)
        "monthly" -> getPrevious6MonthsData(dataType, transactions)
        "yearly" -> getYearBeforeLastData(dataType, transactions)
        else -> emptyList()
    }
}

// Lấy dữ liệu tuần trước
private fun getPreviousWeekData(
    dataType: String,
    transactions: List<Transaction>
): List<ChartData> {
    val result = mutableListOf<ChartData>()
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.WEEK_OF_YEAR, -1)
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    for (i in 6 downTo 0) {
        val dayCalendar = calendar.clone() as Calendar
        dayCalendar.add(Calendar.DAY_OF_YEAR, i - 6)
        val date = dayCalendar.time
        val dateKey = dateFormat.format(date)

        val dayTransactions = transactions.filter { transaction ->
            try {
                val transactionDate = parseDate(transaction.date)
                isSameDay(transactionDate, date)
            } catch (e: Exception) {
                false
            }
        }

        val amount = calculateAmountForDataType(dayTransactions, dataType)
        result.add(ChartData(amount, dateKey))
    }

    return result
}

// Lấy dữ liệu 6 tháng trước đó - MỚI
private fun getPrevious6MonthsData(
    dataType: String,
    transactions: List<Transaction>
): List<ChartData> {
    val result = mutableListOf<ChartData>()
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, -6) // Bắt đầu từ 6 tháng trước
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    for (i in 0..5) {
        val monthCalendar = calendar.clone() as Calendar
        monthCalendar.add(Calendar.MONTH, i)

        val startOfMonth = monthCalendar.time
        monthCalendar.add(Calendar.MONTH, 1)
        monthCalendar.add(Calendar.DAY_OF_MONTH, -1)
        val endOfMonth = monthCalendar.time

        val monthTransactions = transactions.filter { transaction ->
            try {
                val transactionDate = parseDate(transaction.date)
                transactionDate in startOfMonth..endOfMonth
            } catch (e: Exception) {
                false
            }
        }

        val amount = calculateAmountForDataType(monthTransactions, dataType)
        val monthLabel = SimpleDateFormat("MM/yy", Locale.getDefault()).format(startOfMonth)
        result.add(ChartData(amount, monthLabel))
    }

    return result
}

// Lấy dữ liệu năm trước nữa - MỚI
private fun getYearBeforeLastData(
    dataType: String,
    transactions: List<Transaction>
): List<ChartData> {
    val result = mutableListOf<ChartData>()
    val calendar = Calendar.getInstance()
    val yearBeforeLast = calendar.get(Calendar.YEAR) - 2

    // Dữ liệu 2 năm trước
    val yearBeforeLastTransactions = transactions.filter { transaction ->
        val transactionDate = parseDate(transaction.date)
        val transCalendar = Calendar.getInstance().apply { time = transactionDate }
        transCalendar.get(Calendar.YEAR) == yearBeforeLast
    }
    val yearBeforeLastAmount = calculateAmountForDataType(yearBeforeLastTransactions, dataType)
    result.add(ChartData(yearBeforeLastAmount, "2 năm trước"))

    return result
}

// Làm tròn lên số đẹp gần nhất
private fun roundToNearestNiceNumber(value: Double): Double {
    if (value <= 0) return 100.0

    var scaledValue = value
    var scaleFactor = 1.0

    // Nếu giá trị >= 10, chia cho 10 cho đến khi < 10
    while (scaledValue >= 10.0) {
        scaledValue /= 10.0
        scaleFactor *= 10.0
    }

    // Nếu giá trị < 1 và > 0, nhân cho 10 cho đến khi >= 1
    while (scaledValue < 1.0 && scaledValue > 0) {
        scaledValue *= 10.0
        scaleFactor /= 10.0
    }

    // Làm tròn lên số đẹp gần nhất: 1, 2, 5, hoặc 10
    val niceFraction = when {
        scaledValue <= 1.0 -> 1.0
        scaledValue <= 2.0 -> 2.0
        scaledValue <= 5.0 -> 5.0
        else -> 10.0
    }

    return niceFraction * scaleFactor
}

// Tạo các bước đẹp cho trục Y - CẬP NHẬT: tạo các bước đều nhau
private fun generateNiceYSteps(maxValue: Double): List<Double> {
    val steps = mutableListOf<Double>()

    // Tạo 5 bước từ 0 đến maxValue
    for (i in 0..4) {
        steps.add(maxValue * i / 4)
    }

    return steps
}

// Format giá trị trục Y
private fun formatYAxisValue(value: Double, maxValue: Double): String {
    return when {
        maxValue >= 1000000 -> String.format("%.1fM", value / 1000000)
        maxValue >= 1000 -> String.format("%.0fK", value / 1000)
        else -> String.format("%.0f", value)
    }
}

// Kiểm tra hai ngày có cùng ngày không
private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

// Các hàm tiện ích
private fun parseDate(dateString: String): Date {
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString) ?: Date()
    } catch (e: Exception) {
        Date()
    }
}

private fun calculateAmountForDataType(transactions: List<Transaction>, dataType: String): Double {
    return when (dataType) {
        "income" -> transactions.filter { it.isIncome }.sumOf { it.amount.toDouble() }
        "expense" -> transactions.filter { !it.isIncome }.sumOf { it.amount.toDouble() }
        "difference" -> {
            val income = transactions.filter { it.isIncome }.sumOf { it.amount.toDouble() }
            val expense = transactions.filter { !it.isIncome }.sumOf { it.amount.toDouble() }
            income - expense
        }
        else -> 0.0
    }
}

// Các hàm tính toán số tiền theo từng kỳ
private fun calculateCurrentWeekAmount(dataType: String, transactions: List<Transaction>): Double {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    val startOfWeek = calendar.time
    calendar.add(Calendar.DAY_OF_WEEK, 6)
    val endOfWeek = calendar.time

    val weekTransactions = transactions.filter { transaction ->
        val transactionDate = parseDate(transaction.date)
        transactionDate in startOfWeek..endOfWeek
    }

    return calculateAmountForDataType(weekTransactions, dataType)
}

private fun calculatePreviousWeekAmount(dataType: String, transactions: List<Transaction>): Double {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.WEEK_OF_YEAR, -1)
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    val startOfWeek = calendar.time
    calendar.add(Calendar.DAY_OF_WEEK, 6)
    val endOfWeek = calendar.time

    val weekTransactions = transactions.filter { transaction ->
        val transactionDate = parseDate(transaction.date)
        transactionDate in startOfWeek..endOfWeek
    }

    return calculateAmountForDataType(weekTransactions, dataType)
}

private fun calculateCurrentMonthAmount(dataType: String, transactions: List<Transaction>): Double {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val startOfMonth = calendar.time
    calendar.add(Calendar.MONTH, 1)
    calendar.add(Calendar.DAY_OF_MONTH, -1)
    val endOfMonth = calendar.time

    val monthTransactions = transactions.filter { transaction ->
        val transactionDate = parseDate(transaction.date)
        transactionDate in startOfMonth..endOfMonth
    }

    return calculateAmountForDataType(monthTransactions, dataType)
}

private fun calculatePreviousMonthAmount(dataType: String, transactions: List<Transaction>): Double {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, -1)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val startOfMonth = calendar.time
    calendar.add(Calendar.MONTH, 1)
    calendar.add(Calendar.DAY_OF_MONTH, -1)
    val endOfMonth = calendar.time

    val monthTransactions = transactions.filter { transaction ->
        val transactionDate = parseDate(transaction.date)
        transactionDate in startOfMonth..endOfMonth
    }

    return calculateAmountForDataType(monthTransactions, dataType)
}

private fun calculateCurrentYearAmount(dataType: String, transactions: List<Transaction>): Double {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.MONTH, Calendar.JANUARY)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val startOfYear = calendar.time
    calendar.set(Calendar.MONTH, Calendar.DECEMBER)
    calendar.set(Calendar.DAY_OF_MONTH, 31)
    val endOfYear = calendar.time

    val yearTransactions = transactions.filter { transaction ->
        val transactionDate = parseDate(transaction.date)
        transactionDate in startOfYear..endOfYear
    }

    return calculateAmountForDataType(yearTransactions, dataType)
}

private fun calculatePreviousYearAmount(dataType: String, transactions: List<Transaction>): Double {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.YEAR, -1)
    calendar.set(Calendar.MONTH, Calendar.JANUARY)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val startOfYear = calendar.time
    calendar.set(Calendar.MONTH, Calendar.DECEMBER)
    calendar.set(Calendar.DAY_OF_MONTH, 31)
    val endOfYear = calendar.time

    val yearTransactions = transactions.filter { transaction ->
        val transactionDate = parseDate(transaction.date)
        transactionDate in startOfYear..endOfYear
    }

    return calculateAmountForDataType(yearTransactions, dataType)
}

private fun calculateCurrentAmount(dataType: String, timeRange: String, transactions: List<Transaction>): Double {
    return when (timeRange) {
        "weekly" -> calculateCurrentWeekAmount(dataType, transactions)
        "monthly" -> calculateCurrentMonthAmount(dataType, transactions)
        "yearly" -> calculateCurrentYearAmount(dataType, transactions)
        else -> 0.0
    }
}

// Lấy top 5 danh mục với tên thay vì ID - MỚI
private fun getTopCategoriesWithAmount(
    dataType: String,
    transactions: List<Transaction>,
    categories: List<com.example.financeapp.viewmodel.transaction.Category>
): List<CategoryAmount> {
    val filteredTransactions = when (dataType) {
        "income" -> transactions.filter { it.isIncome }
        "expense" -> transactions.filter { !it.isIncome }
        else -> transactions
    }

    // Nhóm theo category và tính tổng
    val categoryTotals = filteredTransactions
        .groupBy { it.category }
        .map { (categoryId, trans) ->
            // Tìm tên danh mục từ danh sách categories
            val categoryName = categories
                .find { it.id == categoryId }
                ?.name ?: "Không xác định"

            CategoryAmount(
                name = categoryName,
                amount = trans.sumOf { it.amount.toDouble() }
            )
        }
        .sortedByDescending { it.amount }
        .take(5) // Lấy top 5

    return categoryTotals
}

private fun getTimeRangeText(timeRange: String, languageViewModel: LanguageViewModel): String {
    return when (timeRange) {
        "weekly" -> languageViewModel.getTranslation("this_week")
        "monthly" -> languageViewModel.getTranslation("this_month")
        "yearly" -> languageViewModel.getTranslation("this_year")
        else -> ""
    }
}

private fun getPreviousTimeRangeText(timeRange: String, languageViewModel: LanguageViewModel): String {
    return when (timeRange) {
        "weekly" -> languageViewModel.getTranslation("last_week")
        "monthly" -> languageViewModel.getTranslation("last_month")
        "yearly" -> languageViewModel.getTranslation("last_year")
        else -> ""
    }
}

private fun getComparisonTitle(timeRange: String, languageViewModel: LanguageViewModel): String {
    return when (timeRange) {
        "yearly" -> "So sánh năm nay với năm trước"
        else -> getTimeRangeText(timeRange, languageViewModel).replaceFirstChar { it.uppercase() }
    }
}

private fun getDataTypeDisplayName(dataType: String, languageViewModel: LanguageViewModel): String {
    return when (dataType) {
        "income" -> "thu nhập"
        "expense" -> "chi tiêu"
        "difference" -> "chênh lệch"
        else -> ""
    }
}

private fun formatCurrencyCompact(amount: Double): String {
    return when {
        amount >= 1000000 -> String.format("%.1fM", amount / 1000000)
        amount >= 1000 -> String.format("%.0fK", amount / 1000)
        else -> String.format("%.0f", amount)
    }
}

// Hàm mới để tạo nhãn cho phần so sánh
private fun getCurrentPeriodLabel(timeRange: String, languageViewModel: LanguageViewModel): String {
    return when (timeRange) {
        "weekly" -> "Tuần này"
        "monthly" -> "Tháng này"
        "yearly" -> "Năm nay"
        else -> ""
    }
}

private fun getPreviousPeriodLabel(timeRange: String, languageViewModel: LanguageViewModel): String {
    return when (timeRange) {
        "weekly" -> "Tuần trước"
        "monthly" -> "Tháng trước"
        "yearly" -> "Năm trước"
        else -> ""
    }
}

// Extension để kiểm tra range của Date
private operator fun Date.rangeTo(other: Date) = object : ClosedRange<Date> {
    override val start = this@rangeTo
    override val endInclusive = other
}