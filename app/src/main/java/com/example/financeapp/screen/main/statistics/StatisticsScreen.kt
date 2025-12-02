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
import com.example.financeapp.screen.formatCurrency
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
    var selectedTimeRange by remember { mutableStateOf("weekly") } // Mặc định là weekly
    var selectedDataType by remember { mutableStateOf("expense") }

    val languageViewModel = LocalLanguageViewModel.current

    val timeRanges = listOf("weekly", "monthly", "yearly")
    val dataTypes = listOf("income", "expense", "difference")

    // Màu sắc theo UI trong ảnh - CẬP NHẬT THEO ẢNH
    val backgroundColor = Color(0xFFF5F7FA) // Nền xám nhạt
    val cardColor = Color.White // Màu thẻ trắng
    val primaryColor = Color(0xFF4A6FA5) // Xanh dương từ UI (giống HomeScreen)
    val textPrimary = Color(0xFF333333) // Đen nhạt
    val textSecondary = Color(0xFF666666) // Xám đậm
    val gridLineColor = Color(0xFFE0E0E0) // Màu lưới xám nhạt
    val chartBarColor = Color(0xFF4A6FA5) // Xanh dương cho cột biểu đồ
    val selectedBarColor = Color(0xFF2E8B57) // Xanh lá đậm
    val redColor = Color(0xFFE74C3C) // Đỏ cho giảm %
    val greenColor = Color(0xFF2ECC71) // Xanh lá cho tăng %

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

            // Biểu đồ chi tiết với UI giống ảnh
            item {
                DetailedChartSection(
                    dataType = selectedDataType,
                    timeRange = selectedTimeRange,
                    transactions = transactions,
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
    val percentageChange = if (previousPeriodTotal > 0) {
        ((currentPeriodTotal - previousPeriodTotal) / previousPeriodTotal * 100)
    } else 0.0

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
                    text = "Tổng chi tiêu",
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
                    Text(
                        text = if (percentageChange >= 0) "▲ ${"%.1f".format(percentageChange)}%"
                        else "▼ ${"%.1f".format(-percentageChange)}%",
                        fontSize = 14.sp,
                        color = if (percentageChange >= 0) accentColor else redColor,
                        fontWeight = FontWeight.Bold
                    )
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

            Spacer(modifier = Modifier.height(16.dp))

            // Nút Xem thêm
            Text(
                text = "Xem thêm",
                fontSize = 14.sp,
                color = primaryColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { /* Xử lý xem thêm */ }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DynamicChartVisualization(
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
    // Tìm giá trị lớn nhất để scale trục Y
    val maxAmount = max(chartData.maxOfOrNull { it.amount } ?: 1.0, 1.0)

    // Làm tròn maxAmount lên số đẹp gần nhất (100, 200, 500, 1000, etc)
    val roundedMaxAmount = roundToNearestNiceNumber(maxAmount)

    // Tạo các bước cho trục Y (5 bước từ 0 đến roundedMaxAmount)
    val ySteps = generateNiceYSteps(roundedMaxAmount)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        // Trục Y với các giá trị tự động
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ySteps.forEach { value ->
                Text(
                    text = formatYAxisValue(value, roundedMaxAmount),
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
                    val yPosition = chartHeight * (1 - step.toFloat() / roundedMaxAmount.toFloat())

                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, yPosition),
                        end = Offset(chartWidth, yPosition),
                        strokeWidth = 1f
                    )
                }

                // Vẽ các cột biểu đồ
                chartData.forEachIndexed { index, dayData ->
                    // FIX: Chuyển đổi Double sang Float
                    val columnHeight = (dayData.amount.toFloat() / roundedMaxAmount.toFloat()) * chartHeight
                    val xPosition = index * columnWidth + spacing / 2
                    val yPosition = chartHeight - columnHeight

                    // Màu cột: mặc định xanh dương, cột cuối cùng có thể highlight
                    val barColor = if (index == chartData.size - 1) selectedBarColor else chartBarColor

                    // Vẽ cột với bo góc trên
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(xPosition, yPosition),
                        size = Size(actualColumnWidth, columnHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Vẽ nhãn dưới cột
                    drawContext.canvas.nativeCanvas.drawText(
                        dayData.label,
                        xPosition + actualColumnWidth / 2,
                        chartHeight + 20f, // FIX: Thêm f
                        Paint().apply {
                            color = android.graphics.Color.parseColor("#666666")
                            textSize = 12f
                            textAlign = Paint.Align.CENTER
                            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                        }
                    )

                    // Vẽ giá trị trên đầu cột nếu có dữ liệu và cột đủ cao
                    // FIX: Sửa điều kiện so sánh
                    if (dayData.amount > 0.0 && columnHeight > 20f) {
                        drawContext.canvas.nativeCanvas.drawText(
                            formatCurrencyCompact(dayData.amount),
                            xPosition + actualColumnWidth / 2,
                            yPosition - 8f, // FIX: Thêm f
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

    val currentData = calculateCurrentAmount(dataType, timeRange, transactions)
    val previousData = calculatePreviousPeriodAmount(dataType, timeRange, transactions)

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
                "${getTimeRangeText(timeRange, languageViewModel)} ${if (timeRange == "yearly") "" else languageViewModel.getTranslation("this_year")}",
                currentData,
                textPrimary = textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            ComparisonDataRow(
                "${getPreviousTimeRangeText(timeRange, languageViewModel)} ${if (timeRange == "yearly") "" else languageViewModel.getTranslation("last_year")}",
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
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    backgroundColor: Color,
    categoryViewModel: CategoryViewModel
) {
    val languageViewModel = LocalLanguageViewModel.current

    val hasData = transactions.isNotEmpty()

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

            if (!hasData) {
                NoDataPlaceholder(textSecondary = textSecondary)
            } else {
                CategoryAnalysisContent(dataType, transactions, languageViewModel, textPrimary, textSecondary)
            }
        }
    }
}

@Composable
private fun CategoryAnalysisContent(
    dataType: String,
    transactions: List<Transaction>,
    languageViewModel: LanguageViewModel,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            languageViewModel.getTranslation("sub_category"),
            fontSize = 14.sp,
            color = textSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            languageViewModel.getTranslation("parent_category"),
            fontSize = 14.sp,
            color = textSecondary,
            fontWeight = FontWeight.Medium
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    val categories = getTopCategoriesWithAmount(dataType, transactions)
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
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                category.name,
                fontSize = 16.sp,
                color = textPrimary
            )
            Text(
                formatCurrency(category.amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                category.name,
                fontSize = 16.sp,
                color = textPrimary
            )
            Text(
                formatCurrency(category.amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary
            )
        }
    }
}

// ==================== HÀM TIỆN ÍCH MỚI ====================

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
private fun getChartDataByTimeRange(
    dataType: String,
    timeRange: String,
    transactions: List<Transaction>
): List<ChartData> {
    return when (timeRange) {
        "weekly" -> getLastNDaysData(dataType, transactions, 7) // 7 ngày gần nhất
        "monthly" -> getLastNDaysData(dataType, transactions, 30) // 30 ngày gần nhất
        "yearly" -> getMonthlyDataForYear(dataType, transactions) // 12 tháng trong năm
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
    val dateFormat = if (days <= 30) SimpleDateFormat("dd/M", Locale.getDefault())
    else SimpleDateFormat("dd/MM", Locale.getDefault())

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

// Lấy dữ liệu theo tháng cho cả năm
private fun getMonthlyDataForYear(
    dataType: String,
    transactions: List<Transaction>
): List<ChartData> {
    val result = mutableListOf<ChartData>()
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val monthFormat = SimpleDateFormat("MM", Locale.getDefault())

    for (month in 0..11) { // 0-11 cho các tháng
        val monthTransactions = transactions.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transCalendar = Calendar.getInstance().apply { time = transactionDate }
            transCalendar.get(Calendar.YEAR) == currentYear &&
                    transCalendar.get(Calendar.MONTH) == month
        }

        val amount = calculateAmountForDataType(monthTransactions, dataType)
        result.add(ChartData(amount, "T${month + 1}"))
    }

    return result
}

// Lấy dữ liệu kỳ trước để so sánh
private fun getPreviousPeriodData(
    dataType: String,
    timeRange: String,
    transactions: List<Transaction>
): List<ChartData> {
    return when (timeRange) {
        "weekly" -> getLastNDaysData(dataType, transactions, 7) // Dùng cùng logic, nhưng đây là để so sánh
        "monthly" -> getLastNDaysData(dataType, transactions, 30)
        "yearly" -> getPreviousYearMonthlyData(dataType, transactions)
        else -> emptyList()
    }
}

// Lấy dữ liệu tháng của năm trước
private fun getPreviousYearMonthlyData(
    dataType: String,
    transactions: List<Transaction>
): List<ChartData> {
    val result = mutableListOf<ChartData>()
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.YEAR, -1)
    val previousYear = calendar.get(Calendar.YEAR)

    for (month in 0..11) {
        val monthTransactions = transactions.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transCalendar = Calendar.getInstance().apply { time = transactionDate }
            transCalendar.get(Calendar.YEAR) == previousYear &&
                    transCalendar.get(Calendar.MONTH) == month
        }

        val amount = calculateAmountForDataType(monthTransactions, dataType)
        result.add(ChartData(amount, "T${month + 1}"))
    }

    return result
}

private fun roundToNearestNiceNumber(value: Double): Double {
    if (value <= 0) return 100.0

    // PHƯƠNG PHÁP KHÔNG DÙNG pow
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

// Tạo các bước đẹp cho trục Y
private fun generateNiceYSteps(maxValue: Double): List<Double> {
    val steps = mutableListOf<Double>()
    val step = maxValue / 4 // 5 điểm (0, 1/4, 2/4, 3/4, 4/4)

    for (i in 0..4) {
        steps.add(step * i)
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

// Các hàm tiện ích cũ (giữ nguyên)
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

private fun calculatePreviousPeriodAmount(dataType: String, timeRange: String, transactions: List<Transaction>): Double {
    val calendar = Calendar.getInstance()

    return when (timeRange) {
        "monthly" -> {
            calendar.add(Calendar.MONTH, -1)
            val previousMonth = calendar.get(Calendar.MONTH)
            val previousYear = calendar.get(Calendar.YEAR)

            val previousTransactions = transactions.filter { transaction ->
                val transactionDate = parseDate(transaction.date)
                val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
                transactionCalendar.get(Calendar.MONTH) == previousMonth &&
                        transactionCalendar.get(Calendar.YEAR) == previousYear
            }

            calculateAmountForDataType(previousTransactions, dataType)
        }
        "yearly" -> {
            val previousYear = calendar.get(Calendar.YEAR) - 1
            val previousTransactions = transactions.filter { transaction ->
                val transactionDate = parseDate(transaction.date)
                val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
                transactionCalendar.get(Calendar.YEAR) == previousYear
            }

            calculateAmountForDataType(previousTransactions, dataType)
        }
        else -> 0.0
    }
}

private fun calculateCurrentAmount(dataType: String, timeRange: String, transactions: List<Transaction>): Double {
    val calendar = Calendar.getInstance()

    return when (timeRange) {
        "monthly" -> {
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            val currentTransactions = transactions.filter { transaction ->
                val transactionDate = parseDate(transaction.date)
                val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
                transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                        transactionCalendar.get(Calendar.YEAR) == currentYear
            }
            calculateAmountForDataType(currentTransactions, dataType)
        }
        "yearly" -> {
            val currentYear = calendar.get(Calendar.YEAR)
            val currentTransactions = transactions.filter { transaction ->
                val transactionDate = parseDate(transaction.date)
                val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
                transactionCalendar.get(Calendar.YEAR) == currentYear
            }
            calculateAmountForDataType(currentTransactions, dataType)
        }
        else -> calculateAmountForDataType(transactions, dataType)
    }
}

private fun getTopCategoriesWithAmount(dataType: String, transactions: List<Transaction>): List<CategoryAmount> {
    val filteredTransactions = when (dataType) {
        "income" -> transactions.filter { it.isIncome }
        "expense" -> transactions.filter { !it.isIncome }
        else -> transactions
    }

    return filteredTransactions
        .groupBy { it.category }
        .map { (category, trans) ->
            CategoryAmount(
                name = category,
                amount = trans.sumOf { it.amount.toDouble() }
            )
        }
        .sortedByDescending { it.amount }
        .take(3)
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
        "yearly" -> languageViewModel.getTranslation("compared_to_same_period")
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