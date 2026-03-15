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
import androidx.compose.material3.ripple
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
import com.example.financeapp.components.utils.formatCurrency
import com.example.financeapp.viewmodel.transaction.Category
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.min
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import com.example.financeapp.components.theme.getAppColors
import com.example.financeapp.viewmodel.savings.SavingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    transactions: List<Transaction>,
    categoryViewModel: CategoryViewModel = viewModel(),
    savingsViewModel: SavingsViewModel
) {
    var selectedTimeRange by remember { mutableStateOf("weekly") }
    var selectedDataType by remember { mutableStateOf("expense") }

    val languageViewModel = LocalLanguageViewModel.current

    val timeRanges = listOf("weekly", "monthly", "yearly")
    val dataTypes = listOf("income", "expense", "difference")

    // Áp dụng màu sắc đồng bộ từ AppColors
    val appColors = getAppColors()
    val backgroundColor = appColors.background
    val cardColor = appColors.surface
    val primaryColor = appColors.primary
    val textPrimary = appColors.textPrimary
    val textSecondary = appColors.textSecondary
    val gridLineColor = appColors.divider
    val chartBarColor = appColors.primary
    val selectedBarColor = appColors.secondary
    val redColor = appColors.expense
    val greenColor = appColors.income

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
                        color = cardColor
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
                CompactCategoryAnalysisSection(
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
                formatCurrency(totalAmount.toFloat()),
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

    // Animation mượt mà khi biểu đồ hiển thị
    var animationTriggered by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "statisticsChartAnim"
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    // Tạo 5 bước đều nhau từ 0 đến roundedMaxAmount
    val ySteps = createYAxisSteps(roundedMaxAmount)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(8.dp)
    ) {
        // Trục Y với các giá trị tự động - SỐ LỚN ở TRÊN, 0 ở DƯỚI
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(45.dp)
                .padding(bottom = 32.dp, top = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ySteps.reversed().forEach { value ->
                    Text(
                        text = formatYAxisLabel(value),
                        fontSize = 10.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
            }
        }

        // Biểu đồ chính
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 50.dp, top = 8.dp, bottom = 32.dp, end = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartHeight = size.height
                val chartWidth = size.width

                val columnWidth = chartWidth / chartData.size
                val spacing = columnWidth * 0.2f
                val actualColumnWidth = columnWidth - spacing

                // Vẽ đường lưới ngang
                ySteps.forEach { step ->
                    val yPosition = chartHeight * (1 - step.toFloat() / roundedMaxAmount.toFloat())
                    drawLine(
                        color = gridLineColor.copy(alpha = 0.5f),
                        start = Offset(0f, yPosition),
                        end = Offset(chartWidth, yPosition),
                        strokeWidth = 1f
                    )
                }

                // Vẽ các cột biểu đồ
                chartData.forEachIndexed { index, data ->
                    val columnHeight = if (roundedMaxAmount > 0) {
                        (data.amount.toFloat() / roundedMaxAmount.toFloat()) * chartHeight * animatedProgress
                    } else 0f

                    val xPosition = index * columnWidth + spacing / 2
                    val yStartPosition = chartHeight - columnHeight
                    val barColor = if (index == chartData.size - 1) selectedBarColor else chartBarColor

                    if (columnHeight > 0) {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(xPosition, yStartPosition),
                            size = Size(actualColumnWidth, columnHeight),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }

                    // Vẽ nhãn dưới cột
                    drawContext.canvas.nativeCanvas.drawText(
                        data.label,
                        xPosition + actualColumnWidth / 2,
                        chartHeight + 25f,
                        Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 30f
                            textAlign = Paint.Align.CENTER
                        }
                    )
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
        value >= 1000000 -> String.format("%.1ftr đ", value / 1000000)
        value >= 1000 -> String.format("%.0fđ", value)
        else -> String.format("%.0fđ", value)
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
            formatCurrency(amount.toFloat()),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textPrimary
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CompactCategoryAnalysisSection(
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
            // Header
            Text(
                languageViewModel.getTranslation("category_analysis"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (categoryData.isEmpty()) {
                CompactNoDataPlaceholder(textSecondary = textSecondary)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // PIE CHART Ở TRÊN với animation
                    AnimatedCategoryPieChartSection(
                        categoryData = categoryData,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        primaryColor = primaryColor
                    )

                    HorizontalDivider(
                        color = textSecondary.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )

                    // DANH SÁCH DANH MỤC Ở DƯỚI với animation
                    AnimatedCategoryListSection(
                        categoryData = categoryData,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        primaryColor = primaryColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedCategoryPieChartSection(
    categoryData: List<CategoryAmount>,
    textPrimary: Color,
    textSecondary: Color,
    primaryColor: Color
) {
    var selectedCategoryIndex by remember { mutableStateOf(-1) }
    val pieChartColors = remember { getPieChartColors() }
    val totalAmount = categoryData.sumOf { it.amount }

    // Animation cho phần trăm khi selected
    val selectedPercentage by animateFloatAsState(
        targetValue = if (selectedCategoryIndex >= 0) {
            val selectedAmount = categoryData[selectedCategoryIndex].amount
            (selectedAmount / totalAmount * 100).toFloat()
        } else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Thông tin tổng với animation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tổng ${categoryData.size} danh mục",
                    fontSize = 12.sp,
                    color = textSecondary
                )
                Text(
                    text = formatCurrencyCompact(totalAmount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }

            // Animation cho phần thông tin selected
            AnimatedVisibility(
                visible = selectedCategoryIndex >= 0,
                enter = fadeIn(animationSpec = tween(300)) +
                        slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it }),
                exit = fadeOut(animationSpec = tween(300)) +
                        slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it })
            ) {
                if (selectedCategoryIndex >= 0 && selectedCategoryIndex < categoryData.size) {
                    val selectedCategory = categoryData[selectedCategoryIndex]

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = selectedCategory.name.take(12) + if (selectedCategory.name.length > 12) "..." else "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${"%.1f".format(selectedPercentage)}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }
            }
        }

        // Pie Chart với animation
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedPieChart(
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
private fun AnimatedCategoryListSection(
    categoryData: List<CategoryAmount>,
    textPrimary: Color,
    textSecondary: Color,
    primaryColor: Color
) {
    val totalAmount = categoryData.sumOf { it.amount }
    val pieChartColors = remember { getPieChartColors() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Header cho danh sách
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Danh mục",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            )
            Text(
                text = "Số tiền",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            )
        }

        // Danh sách danh mục với animation
        categoryData.forEachIndexed { index, category ->
            AnimatedCategoryListItem(
                category = category,
                index = index,
                totalAmount = totalAmount,
                color = pieChartColors[index % pieChartColors.size],
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                primaryColor = primaryColor
            )

            // Divider (trừ item cuối)
            if (index < categoryData.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 1.dp),
                    thickness = 0.5.dp,
                    color = textSecondary.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
private fun AnimatedCategoryListItem(
    category: CategoryAmount,
    index: Int,
    totalAmount: Double,
    color: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryColor: Color
) {
    val percentage = if (totalAmount > 0) {
        (category.amount / totalAmount * 100)
    } else {
        0.0
    }

    // State cho hover
    var isHovered by remember { mutableStateOf(false) }

    // Animation cho scale
    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200)
    )

    // Animation cho rotation
    val animatedRotation by animateFloatAsState(
        targetValue = if (isHovered) 360f else 0f,
        animationSpec = tween(durationMillis = 400)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .scale(animatedScale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false)
            ) {
                // Handle click if needed
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHovered = true
                        tryAwaitRelease()
                        isHovered = false
                    },
                    onTap = {
                        // Handle tap if needed
                    }
                )
            }
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // STT và màu với animation
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color, CircleShape)
                .graphicsLayer {
                    rotationZ = animatedRotation
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Tên danh mục và phần trăm với animation
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = category.name,
                fontSize = 12.sp,
                color = if (isHovered) primaryColor else textPrimary,
                fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Animated percentage bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(color.copy(alpha = 0.2f), RoundedCornerShape(1.dp))
                ) {
                    // Animation cho chiều rộng của progress bar
                    val animatedWidth by animateFloatAsState(
                        targetValue = percentage.toFloat() / 100f,
                        animationSpec = tween(durationMillis = 500)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedWidth)
                            .height(2.dp)
                            .background(color, RoundedCornerShape(1.dp))
                    )
                }
                Text(
                    text = "${"%.1f".format(percentage)}%",
                    fontSize = 10.sp,
                    color = textSecondary
                )
            }
        }

        // Số tiền với animation
        val animatedAmountColor by animateColorAsState(
            targetValue = if (isHovered) primaryColor else textPrimary,
            animationSpec = tween(durationMillis = 200)
        )

        val animatedFontWeight = if (isHovered) FontWeight.ExtraBold else FontWeight.Bold

        Text(
            text = formatCurrencyCompact(category.amount),
            fontSize = 12.sp,
            fontWeight = animatedFontWeight,
            color = animatedAmountColor
        )
    }
}

@Composable
private fun AnimatedPieChart(
    categoryData: List<CategoryAmount>,
    selectedIndex: Int,
    onSliceSelected: (Int) -> Unit,
    pieChartColors: List<Color>,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val totalAmount = categoryData.sumOf { it.amount }

    // Animation cho pie chart khi load - Mượt mà hơn với animateFloatAsState
    var animationTriggered by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "pieChartEntrance"
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    // Animation cho selection
    val selectionAnimation by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) 1f else 0f,
        animationSpec = tween(durationMillis = 400)
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val angle = calculateAngle(center, offset)
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
        val chartSize = min(constraints.maxWidth, constraints.maxHeight) * 0.8f
        val density = LocalDensity.current

        Canvas(
            modifier = Modifier
                .size(chartSize.dp)
        ) {
            val size = size
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f
            val innerRadius = outerRadius * 0.6f
            var startAngle = -90f

            // Vẽ các slice với animation
            categoryData.forEachIndexed { index, data ->
                val sliceAngle = (data.amount / totalAmount * 360).toFloat() * animatedProgress
                val isSelected = selectedIndex == index

                // Màu slice với animation
                val sliceColor = if (isSelected) {
                    // Animate màu khi selected
                    pieChartColors[index % pieChartColors.size].copy(
                        alpha = 0.9f * selectionAnimation + 0.7f * (1 - selectionAnimation)
                    )
                } else {
                    pieChartColors[index % pieChartColors.size].copy(alpha = 0.7f)
                }

                // Độ dày stroke với animation
                val strokeWidth = if (isSelected) {
                    (outerRadius - innerRadius) + with(density) {
                        4.dp.toPx() * selectionAnimation
                    }
                } else {
                    outerRadius - innerRadius
                }

                // Vẽ slice với animation progress
                drawArc(
                    color = sliceColor,
                    startAngle = startAngle,
                    sweepAngle = sliceAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = strokeWidth)
                )

                // Glow effect cho slice được chọn
                if (isSelected && selectionAnimation > 0) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.2f * selectionAnimation),
                        startAngle = startAngle,
                        sweepAngle = sliceAngle,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - outerRadius - 3.dp.toPx() * selectionAnimation,
                            center.y - outerRadius - 3.dp.toPx() * selectionAnimation
                        ),
                        size = Size(
                            (outerRadius + 3.dp.toPx() * selectionAnimation) * 2,
                            (outerRadius + 3.dp.toPx() * selectionAnimation) * 2
                        ),
                        style = Stroke(width = strokeWidth + 1.5f * selectionAnimation)
                    )
                }

                // Vẽ đường phân cách với animation
                if (sliceAngle > 0 && index < categoryData.size - 1 && animatedProgress > 0.5f) {
                    val separatorAngle = startAngle + sliceAngle
                    val rad = Math.toRadians(separatorAngle.toDouble())
                    val x1 = center.x + outerRadius * kotlin.math.cos(rad).toFloat()
                    val y1 = center.y + outerRadius * kotlin.math.sin(rad).toFloat()
                    val x2 = center.x + innerRadius * kotlin.math.cos(rad).toFloat()
                    val y2 = center.y + innerRadius * kotlin.math.sin(rad).toFloat()

                    drawLine(
                        color = Color.White.copy(alpha = animatedProgress),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                startAngle += sliceAngle
            }

            // Vẽ vòng tròn trắng ở giữa với animation
            drawCircle(
                color = Color.White,
                radius = innerRadius * animatedProgress,
                center = center
            )

            // Vẽ thông tin ở giữa với animation
            if (selectedIndex >= 0 && selectedIndex < categoryData.size) {
                val selectedCategory = categoryData[selectedIndex]
                val percentage = (selectedCategory.amount / totalAmount * 100)

                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    }

                    // Phần trăm với animation
                    paint.apply {
                        color = primaryColor.toArgb()
                        textSize = (12f + 2f * selectionAnimation) * density.density
                        typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    }
                    drawText(
                        "${"%.1f".format(percentage)}%",
                        center.x,
                        center.y - 5f,
                        paint
                    )
                }
            } else if (animatedProgress > 0.8f) {
                // Hiển thị tổng khi không có selection
                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = textSecondary.toArgb()
                        textSize = 10f * density.density
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    }

                    drawText(
                        "Tổng",
                        center.x,
                        center.y - 8f,
                        paint
                    )

                    paint.apply {
                        color = primaryColor.toArgb()
                        textSize = 11f * density.density
                        typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    }
                    drawText(
                        formatCurrencyCompact(totalAmount),
                        center.x,
                        center.y + 5f,
                        paint
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactNoDataPlaceholder(textSecondary: Color) {
    val languageViewModel = LocalLanguageViewModel.current

    // Animation cho placeholder
    var rotation by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            rotation += 2f
            if (rotation >= 360f) rotation = 0f
            delay(16)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    }
            ) {
                Text(
                    text = "📊",
                    fontSize = 24.sp
                )
            }
            Text(
                languageViewModel.getTranslation("no_data"),
                fontSize = 14.sp,
                color = textSecondary
            )
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

        // Ưu tiên dùng categoryId để map đúng Category, fallback về transaction.category nếu cần
        val categoryId = transaction.categoryId

        val categoryName = categories
            .find { it.id == categoryId }
            ?.name
            ?: if (transaction.category.isNotBlank()) {
                transaction.category
            } else {
                languageViewModel.getTranslation("unknown_category")
            }

        categoryMap[categoryName] = categoryMap.getOrDefault(categoryName, 0.0) + amount
    }

    // Chuyển đổi thành list và sắp xếp
    return categoryMap.map { (name, amount) ->
        CategoryAmount(name = name, amount = amount)
    }
        .filter { it.amount > 0 } // Chỉ hiển thị danh mục có số tiền
        .sortedByDescending { it.amount }
        .take(5) // Giới hạn 5 danh mục để hiển thị gọn
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

// Màu sắc cho Pie Chart
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
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"))
    return "${formatter.format(amount.toLong())} đ"
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