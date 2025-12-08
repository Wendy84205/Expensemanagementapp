package com.example.financeapp.screen.main.statistics

import android.graphics.Paint
import android.graphics.Typeface
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
import com.example.financeapp.components.ui.BottomNavBar
import com.example.financeapp.screen.features.formatCurrency
import com.example.financeapp.viewmodel.transaction.Category
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.xr.compose.testing.toDp
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.min
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.toArgb


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
                "${languageViewModel.getTranslation("total")} ${getDataTypeDisplayName(dataType, languageViewModel)}",
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
    categories: List<Category>,
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

    // Tính phần trăm thay đổi đúng cách
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
                        "income" -> languageViewModel.getTranslation("total_income")
                        "expense" -> languageViewModel.getTranslation("total_expense")
                        else -> languageViewModel.getTranslation("difference")
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
                            text = if (percentageChange >= 0)
                                "▲ ${"%.1f".format(percentageChange)}%"
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
                            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
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
                                typeface = Typeface.create("sans-serif", Typeface.BOLD)
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
    categories: List<Category>,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    backgroundColor: Color
) {
    val languageViewModel = LocalLanguageViewModel.current

    // Lấy dữ liệu phân loại danh mục
    val categoryData = getCategoryDataWithAmount(dataType, transactions, categories)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header với icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        languageViewModel.getTranslation("category_analysis"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    if (categoryData.isNotEmpty()) {
                        Text(
                            "${categoryData.size} danh mục",
                            fontSize = 12.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryData.isEmpty()) {
                NoDataPlaceholder(textSecondary = textSecondary)
            } else {
                CategoryAnalysisWithPieChart(
                    categoryData = categoryData,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    primaryColor = primaryColor
                )
            }
        }
    }
}
@Composable
private fun getCategoryDataWithAmount(
    dataType: String,
    transactions: List<Transaction>,
    categories: List<Category>
): List<CategoryAmount> {
    val languageViewModel = LocalLanguageViewModel.current

    val filteredTransactions = when (dataType) {
        "income" -> transactions.filter { it.isIncome }
        "expense" -> transactions.filter { !it.isIncome }
        "difference" -> {
            // For difference, we want to show both income and expense categories
            // but separated by type
            val incomeTransactions = transactions.filter { it.isIncome }
            val expenseTransactions = transactions.filter { !it.isIncome }

            // We'll show expense categories by default for difference view
            return getCategoryDataForTransactions(expenseTransactions, categories, languageViewModel)
        }
        else -> transactions.filter { !it.isIncome } // Mặc định là expense
    }

    return getCategoryDataForTransactions(filteredTransactions, categories, languageViewModel)
}

@Composable
private fun getCategoryDataForTransactions(
    transactions: List<Transaction>,
    categories: List<Category>,
    languageViewModel: LanguageViewModel
): List<CategoryAmount> {
    // Nhóm theo category và tính tổng
    val categoryMap = mutableMapOf<String, Double>()

    transactions.forEach { transaction ->
        val amount = transaction.amount.toDouble()
        val categoryId = transaction.category

        // Tìm tên danh mục
        val categoryName = categories
            .find { it.id == categoryId }
            ?.name ?: languageViewModel.getTranslation("unknown_category")

        categoryMap[categoryName] = categoryMap.getOrDefault(categoryName, 0.0) + amount
    }

    // Chuyển đổi thành list và sắp xếp
    return categoryMap.map { (name, amount) ->
        CategoryAmount(name = name, amount = amount)
    }
        .filter { it.amount > 0 } // Chỉ hiển thị danh mục có số tiền
        .sortedByDescending { it.amount }
        .take(8) // Giới hạn 8 danh mục để hiển thị đẹp
}

@Composable
private fun CategoryAnalysisWithPieChart(
    categoryData: List<CategoryAmount>,
    textPrimary: Color,
    textSecondary: Color,
    primaryColor: Color
) {
    var selectedCategoryIndex by remember { mutableStateOf(-1) }
    val pieChartColors = remember { getPieChartColors() }
    val totalAmount = categoryData.sumOf { it.amount }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp), // Tăng chiều cao lên
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cột bên trái: Danh sách danh mục cải tiến
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header cho danh sách
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Danh mục",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
                Text(
                    text = "Số tiền",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
            }

            // Danh sách với divider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                categoryData.forEachIndexed { index, category ->
                    EnhancedCategoryListItem(
                        category = category,
                        index = index,
                        isSelected = selectedCategoryIndex == index,
                        totalAmount = totalAmount,
                        color = pieChartColors[index % pieChartColors.size],
                        onCategorySelected = { idx ->
                            selectedCategoryIndex = if (selectedCategoryIndex == idx) -1 else idx
                        },
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        primaryColor = primaryColor
                    )

                    // Divider (trừ item cuối)
                    if (index < categoryData.size - 1) {
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = textSecondary.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            // Footer với tổng
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = primaryColor.copy(alpha = 0.05f)
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tổng cộng",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Text(
                        text = formatCurrency(totalAmount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
        }

        // Cột bên phải: Pie Chart
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            EnhancedInteractivePieChart(
                categoryData = categoryData,
                selectedIndex = selectedCategoryIndex,
                onSliceSelected = { index ->
                    selectedCategoryIndex = if (selectedCategoryIndex == index) -1 else index
                },
                pieChartColors = pieChartColors,
                primaryColor = primaryColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }
    }
}

@Composable
private fun EnhancedCategoryListItem(
    category: CategoryAmount,
    index: Int,
    isSelected: Boolean,
    totalAmount: Double,
    color: Color,
    onCategorySelected: (Int) -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    primaryColor: Color
) {
    val percentage = if (totalAmount > 0) {
        (category.amount / totalAmount * 100)
    } else {
        0.0
    }

    // Animation cho selection
    val animatedBackground by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 200)
    )

    val backgroundColor = if (isSelected) {
        color.copy(alpha = 0.1f * animatedBackground)
    } else {
        Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCategorySelected(index) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = if (isSelected) BorderStroke(
            1.dp,
            color.copy(alpha = 0.5f * animatedBackground)
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Số thứ tự và màu
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Tên danh mục và phần trăm
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = category.name,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Progress indicator nhỏ
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Progress bar nhỏ
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(color.copy(alpha = 0.2f), RoundedCornerShape(1.5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percentage.toFloat() / 100f)
                                .height(3.dp)
                                .background(color, RoundedCornerShape(1.5.dp))
                        )
                    }

                    Text(
                        text = "${"%.1f".format(percentage)}%",
                        fontSize = 10.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Số tiền
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatCurrencyCompact(category.amount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) primaryColor else textPrimary
                )
                Text(
                    text = "(${"%.1f".format(percentage)}%)",
                    fontSize = 10.sp,
                    color = textSecondary
                )
            }
        }
    }
}

@Composable
private fun EnhancedInteractivePieChart(
    categoryData: List<CategoryAmount>,
    selectedIndex: Int,
    onSliceSelected: (Int) -> Unit,
    pieChartColors: List<Color>,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val totalAmount = categoryData.sumOf { it.amount }

    // Animation cho pie chart (chỉ chạy 1 lần khi load)
    var animatedProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        // Chỉ animation lần đầu khi load
        animatedProgress = 0f
        for (i in 0..100) {
            animatedProgress = i / 100f
            delay(5) // Faster animation
        }
    }

    // Animation cho selection highlight
    val selectionAnimation by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // Tính toán slice được click
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val angle = calculateAngle(center, offset)

                        // Tìm slice tương ứng với góc này
                        var accumulatedAngle = 0f
                        categoryData.forEachIndexed { index, data ->
                            val sliceAngle = (data.amount / totalAmount * 360).toFloat()
                            if (angle >= accumulatedAngle && angle <= accumulatedAngle + sliceAngle) {
                                onSliceSelected(index)
                                return@detectTapGestures
                            }
                            accumulatedAngle += sliceAngle
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val chartSize = min(constraints.maxWidth, constraints.maxHeight)
        val density = LocalDensity.current

        Canvas(
            modifier = Modifier
                .size(chartSize.dp)
        ) {
            val size = size
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f
            val innerRadius = outerRadius * 0.4f // Lỗ nhỏ hơn cho rõ hơn
            var startAngle = -90f

            // Vẽ background circle nhẹ
            drawCircle(
                color = primaryColor.copy(alpha = 0.05f),
                radius = outerRadius,
                center = center
            )

            // Vẽ các slice - KHÔNG dùng animatedProgress ở đây nữa
            categoryData.forEachIndexed { index, data ->
                val sliceAngle = (data.amount / totalAmount * 360).toFloat()
                val isSelected = selectedIndex == index

                // Màu với độ trong suốt tùy theo selection
                val sliceColor = if (isSelected) {
                    // Màu đậm hơn khi selected
                    pieChartColors[index % pieChartColors.size].copy(alpha = 0.9f)
                } else {
                    // Màu nhạt hơn cho các slice không được chọn
                    pieChartColors[index % pieChartColors.size].copy(alpha = 0.6f)
                }

                // Độ dày stroke - dày hơn khi selected
                val strokeWidth = if (isSelected) {
                    // Animate stroke width khi selected
                    (outerRadius - innerRadius) + with(density) {
                        6.dp.toPx() * selectionAnimation
                    }
                } else {
                    outerRadius - innerRadius
                }

                // Vẽ slice
                drawArc(
                    color = sliceColor,
                    startAngle = startAngle,
                    sweepAngle = sliceAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = strokeWidth)
                )

                // Highlight effect cho slice được chọn
                if (isSelected) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.3f * selectionAnimation),
                        startAngle = startAngle,
                        sweepAngle = sliceAngle,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - outerRadius - 4.dp.toPx() * selectionAnimation,
                            center.y - outerRadius - 4.dp.toPx() * selectionAnimation
                        ),
                        size = Size(
                            (outerRadius + 4.dp.toPx() * selectionAnimation) * 2,
                            (outerRadius + 4.dp.toPx() * selectionAnimation) * 2
                        ),
                        style = Stroke(width = strokeWidth + 2.dp.toPx() * selectionAnimation)
                    )
                }

                // Vẽ đường phân cách giữa các slice
                if (sliceAngle > 0 && index < categoryData.size - 1) {
                    val separatorAngle = startAngle + sliceAngle
                    val rad = Math.toRadians(separatorAngle.toDouble())
                    val x1 = center.x + outerRadius * kotlin.math.cos(rad).toFloat()
                    val y1 = center.y + outerRadius * kotlin.math.sin(rad).toFloat()
                    val x2 = center.x + innerRadius * kotlin.math.cos(rad).toFloat()
                    val y2 = center.y + innerRadius * kotlin.math.sin(rad).toFloat()

                    drawLine(
                        color = Color.White,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                startAngle += sliceAngle
            }

            // Vẽ nhãn ở giữa - HIỂN THỊ NGAY LẬP TỨC
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                }

                if (selectedIndex >= 0 && selectedIndex < categoryData.size) {
                    val selectedCategory = categoryData[selectedIndex]
                    val percentage = (selectedCategory.amount / totalAmount * 100)

                    // Hiển thị phần trăm
                    paint.apply {
                        color = primaryColor.toArgb()
                        textSize = 18f * density.density
                        typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    }
                    drawText(
                        "${"%.1f".format(percentage)}%",
                        center.x,
                        center.y - 25f,
                        paint
                    )

                    // Hiển thị số tiền
                    paint.apply {
                        color = textSecondary.toArgb()
                        textSize = 14f * density.density
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    }
                    drawText(
                        formatCurrencyCompact(selectedCategory.amount),
                        center.x,
                        center.y,
                        paint
                    )

                    // Hiển thị tên category
                    paint.apply {
                        color = textPrimary.toArgb()
                        textSize = 12f * density.density
                    }
                    drawText(
                        selectedCategory.name,
                        center.x,
                        center.y + 20f,
                        paint
                    )
                } else {
                    // Hiển thị tổng khi không có selection
                    paint.apply {
                        color = primaryColor.toArgb()
                        textSize = 16f * density.density
                        typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    }
                    drawText(
                        "Tổng",
                        center.x,
                        center.y - 15f,
                        paint
                    )

                    paint.apply {
                        color = textPrimary.toArgb()
                        textSize = 14f * density.density
                    }
                    drawText(
                        formatCurrencyCompact(totalAmount),
                        center.x,
                        center.y + 15f,
                        paint
                    )
                }
            }
        }
    }
}

// Hàm tính toán góc từ tâm đến điểm click
private fun calculateAngle(center: Offset, point: Offset): Float {
    val dx = point.x - center.x
    val dy = point.y - center.y
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

    // Chuyển đổi để 0° ở trên cùng
    angle = (angle + 90) % 360
    if (angle < 0) angle += 360

    return angle
}

// Màu sắc cho Pie Chart - thêm nhiều màu hơn
private fun getPieChartColors(): List<Color> {
    return listOf(
        Color(0xFF4A6FA5), // Blue
        Color(0xFF2ECC71), // Green
        Color(0xFFE74C3C), // Red
        Color(0xFFF39C12), // Orange
        Color(0xFF9B59B6), // Purple
        Color(0xFF1ABC9C), // Teal
        Color(0xFF3498DB), // Light Blue
        Color(0xFFE67E22), // Dark Orange
        Color(0xFF16A085), // Dark Teal
        Color(0xFF27AE60), // Dark Green
        Color(0xFF8E44AD), // Dark Purple
        Color(0xFF2980B9), // Medium Blue
        Color(0xFFD35400), // Darker Orange
        Color(0xFFC0392B), // Dark Red
    )
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

// Lấy dữ liệu biểu đồ theo timeRange
@Composable
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

// Lấy dữ liệu 6 tháng gần nhất
@Composable
private fun getLast6MonthsData(
    dataType: String,
    transactions: List<Transaction>
): List<ChartData> {
    val languageViewModel = LocalLanguageViewModel.current

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

// Lấy dữ liệu năm nay và năm trước cho so sánh
@Composable
private fun getYearlyComparisonData(
    dataType: String,
    transactions: List<Transaction>
): List<ChartData> {
    val languageViewModel = LocalLanguageViewModel.current

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
    result.add(ChartData(currentYearAmount, languageViewModel.getTranslation("this_year")))

    // Dữ liệu năm trước
    val previousYearTransactions = transactions.filter { transaction ->
        val transactionDate = parseDate(transaction.date)
        val transCalendar = Calendar.getInstance().apply { time = transactionDate }
        transCalendar.get(Calendar.YEAR) == previousYear
    }
    val previousYearAmount = calculateAmountForDataType(previousYearTransactions, dataType)
    result.add(ChartData(previousYearAmount, languageViewModel.getTranslation("last_year")))

    return result
}

// Lấy dữ liệu kỳ trước để so sánh
@Composable
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

// Lấy dữ liệu 6 tháng trước đó
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

// Lấy dữ liệu năm trước nữa
@Composable
private fun getYearBeforeLastData(
    dataType: String,
    transactions: List<Transaction>
): List<ChartData> {
    val languageViewModel = LocalLanguageViewModel.current

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
    result.add(ChartData(yearBeforeLastAmount, "${languageViewModel.getTranslation("last_year")} 2"))

    return result
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

// Lấy top 5 danh mục với tên thay vì ID
@Composable
private fun getTopCategoriesWithAmount(
    dataType: String,
    transactions: List<Transaction>,
    categories: List<Category>
): List<CategoryAmount> {
    val languageViewModel = LocalLanguageViewModel.current

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
                ?.name ?: languageViewModel.getTranslation("unknown_category")

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
        "yearly" -> "${languageViewModel.getTranslation("compare")} ${languageViewModel.getTranslation("this_year")} ${languageViewModel.getTranslation("with")} ${languageViewModel.getTranslation("last_year")}"
        else -> getTimeRangeText(timeRange, languageViewModel).replaceFirstChar { it.uppercase() }
    }
}

private fun getDataTypeDisplayName(dataType: String, languageViewModel: LanguageViewModel): String {
    return when (dataType) {
        "income" -> languageViewModel.getTranslation("data_type_income").lowercase()
        "expense" -> languageViewModel.getTranslation("data_type_expense").lowercase()
        "difference" -> languageViewModel.getTranslation("data_type_difference").lowercase()
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

// Hàm tạo nhãn cho phần so sánh
private fun getCurrentPeriodLabel(timeRange: String, languageViewModel: LanguageViewModel): String {
    return when (timeRange) {
        "weekly" -> languageViewModel.getTranslation("current_week")
        "monthly" -> languageViewModel.getTranslation("current_month")
        "yearly" -> languageViewModel.getTranslation("current_year")
        else -> ""
    }
}

private fun getPreviousPeriodLabel(timeRange: String, languageViewModel: LanguageViewModel): String {
    return when (timeRange) {
        "weekly" -> languageViewModel.getTranslation("previous_week")
        "monthly" -> languageViewModel.getTranslation("previous_month")
        "yearly" -> languageViewModel.getTranslation("previous_year")
        else -> ""
    }
}

// Extension để kiểm tra range của Date
private operator fun Date.rangeTo(other: Date) = object : ClosedRange<Date> {
    override val start = this@rangeTo
    override val endInclusive = other
}