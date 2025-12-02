package com.example.financeapp.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.CategoryViewModel
import com.example.financeapp.viewmodel.LanguageViewModel
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.data.Transaction
import com.example.financeapp.components.BottomNavBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    transactions: List<Transaction>,
    categoryViewModel: CategoryViewModel = viewModel()
) {
    var selectedTimeRange by remember { mutableStateOf("yearly") }
    var selectedDataType by remember { mutableStateOf("expense") }

    val languageViewModel = LocalLanguageViewModel.current

    val timeRanges = listOf("weekly", "monthly", "yearly")
    val dataTypes = listOf("income", "expense", "difference")

    // Màu sắc theo gói chuyên nghiệp & tin cậy
    val primaryColor = Color(0xFF0F4C75) // Xanh Navy
    val secondaryColor = Color(0xFF2E8B57) // Xanh lá đậm
    val backgroundColor = Color(0xFFF5F7FA) // Xám rất nhạt
    val surfaceColor = Color.White // Trắng
    val textPrimary = Color(0xFF2D3748) // Xám đen
    val textSecondary = Color(0xFF718096) // Xám
    val accentColor = Color(0xFFED8936) // Cam

    val gradient = Brush.verticalGradient(
        colors = listOf(primaryColor, Color(0xFF1A365D))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        languageViewModel.getTranslation("financial_fluctuations"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = languageViewModel.getTranslation("back"),
                            tint = Color.White
                        )
                    }
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
                    accentColor = accentColor
                )
            }

            // Biểu đồ biến động - CHỈ HIỂN THỊ 2 NĂM KHI CHỌN YEARLY
            item {
                if (selectedTimeRange == "yearly") {
                    YearlyComparisonChart(
                        dataType = selectedDataType,
                        transactions = transactions,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        backgroundColor = backgroundColor
                    )
                } else {
                    TrendChartSection(
                        dataType = selectedDataType,
                        timeRange = selectedTimeRange,
                        transactions = transactions,
                        primaryColor = primaryColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        backgroundColor = backgroundColor
                    )
                }
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
                elevation = 4.dp,
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
                elevation = 4.dp,
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
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = true
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = primaryColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${languageViewModel.getTranslation("total")} ${getDataTypeDisplayName(dataType, languageViewModel)} ${getTimeRangeText(timeRange, languageViewModel)}",
                fontSize = 16.sp,
                color = Color.White.copy(0.9f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                formatCurrency(totalAmount),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = getAmountColorProfessional(dataType, totalAmount, accentColor)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    comparisonText,
                    fontSize = 14.sp,
                    color = Color.White.copy(0.8f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Info,
                    contentDescription = languageViewModel.getTranslation("info"),
                    tint = Color.White.copy(0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun YearlyComparisonChart(
    dataType: String,
    transactions: List<Transaction>,
    primaryColor: Color,
    secondaryColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    backgroundColor: Color
) {
    val languageViewModel = LocalLanguageViewModel.current

    // LẤY DỮ LIỆU 2 NĂM GẦN NHẤT
    val yearlyData = getTwoYearData(dataType, transactions, languageViewModel)
    val maxAmount = yearlyData.maxOfOrNull { it.amount } ?: 0.0
    val maxChartValue = if (maxAmount > 0) maxAmount * 1.2 else 100000.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                clip = true
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                languageViewModel.getTranslation("yearly_comparison"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (yearlyData.all { it.amount == 0.0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(backgroundColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            languageViewModel.getTranslation("no_data"),
                            fontSize = 16.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            languageViewModel.getTranslation("no_transactions_time_period"),
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                    }
                }
            } else {
                // BIỂU ĐỒ 2 CỘT CHO 2 NĂM
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Trục Y
                            Column(
                                modifier = Modifier
                                    .width(40.dp)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                val step = maxChartValue / 4
                                for (i in 4 downTo 0) {
                                    val value = (step * i).toInt()
                                    Text(
                                        "${value / 1000}",
                                        fontSize = 12.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Biểu đồ 2 cột
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                // Đường lưới ngang
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    repeat(5) {
                                        Divider(
                                            color = backgroundColor,
                                            thickness = 1.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // 2 CỘT CHO 2 NĂM
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    yearlyData.forEachIndexed { index, data ->
                                        val heightRatio = if (maxChartValue > 0) {
                                            val ratio = data.amount / maxChartValue
                                            if (data.amount > 0 && ratio < 0.05f) 0.05f else ratio.toFloat()
                                        } else {
                                            0f
                                        }

                                        YearlyChartBar(
                                            heightRatio = heightRatio,
                                            label = data.label,
                                            amount = data.amount,
                                            isCurrentYear = index == yearlyData.size - 1,
                                            showAmount = true,
                                            primaryColor = primaryColor,
                                            secondaryColor = secondaryColor,
                                            textPrimary = textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "(${languageViewModel.getTranslation("thousands")})",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            YearlyComparisonInfo(yearlyData, dataType, primaryColor, secondaryColor, textPrimary, textSecondary)
        }
    }
}

@Composable
private fun YearlyChartBar(
    heightRatio: Float,
    label: String,
    amount: Double,
    isCurrentYear: Boolean,
    showAmount: Boolean = false,
    primaryColor: Color,
    secondaryColor: Color,
    textPrimary: Color
) {
    val barColor = if (isCurrentYear) primaryColor else secondaryColor
    val textColor = if (isCurrentYear) primaryColor else secondaryColor
    val fontWeight = FontWeight.Bold

    val minHeight = 4.dp
    val calculatedHeight = (120 * heightRatio).dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Hiển thị số tiền
        if (showAmount && amount > 0) {
            Text(
                text = formatCurrency(amount),
                fontSize = 12.sp,
                color = textColor,
                fontWeight = fontWeight,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(40.dp)
                .height(maxOf(calculatedHeight, minHeight))
                .background(barColor, RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Nhãn năm
        Text(
            text = label,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun YearlyComparisonInfo(
    yearlyData: List<ChartData>,
    dataType: String,
    primaryColor: Color,
    secondaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val languageViewModel = LocalLanguageViewModel.current

    if (yearlyData.size >= 2) {
        val currentYearData = yearlyData.last()
        val previousYearData = yearlyData.first()
        val difference = currentYearData.amount - previousYearData.amount
        val percentage = if (previousYearData.amount != 0.0) {
            (difference / previousYearData.amount) * 100
        } else 0.0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "${languageViewModel.getTranslation("compared_to_same_period")} ${previousYearData.label}",
                    fontSize = 14.sp,
                    color = textSecondary
                )
                Text(
                    "${if (difference >= 0) "+" else ""}${formatCurrency(difference)} (${"%.1f".format(percentage)}%)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (difference >= 0) secondaryColor else Color(0xFFF56565)
                )
            }

            Text(
                "${currentYearData.label}: ${formatCurrency(currentYearData.amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = getAmountColorProfessional(dataType, currentYearData.amount, Color(0xFFED8936))
            )
        }
    }
}

@Composable
private fun TrendChartSection(
    dataType: String,
    timeRange: String,
    transactions: List<Transaction>,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    backgroundColor: Color
) {
    val languageViewModel = LocalLanguageViewModel.current
    val chartData = getRealChartData(dataType, timeRange, transactions, languageViewModel)
    val maxAmount = chartData.maxOfOrNull { it.amount } ?: 0.0
    val maxChartValue = if (maxAmount > 0) maxAmount * 1.2 else 100000.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                clip = true
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                languageViewModel.getTranslation("fluctuations"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (chartData.all { it.amount == 0.0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(backgroundColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            languageViewModel.getTranslation("no_data"),
                            fontSize = 16.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            languageViewModel.getTranslation("no_transactions_time_period"),
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(40.dp)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                val step = maxChartValue / 4
                                for (i in 4 downTo 0) {
                                    val value = (step * i).toInt()
                                    Text(
                                        "${value / 1000}",
                                        fontSize = 12.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    repeat(5) {
                                        Divider(
                                            color = backgroundColor,
                                            thickness = 1.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    chartData.forEachIndexed { index, data ->
                                        val heightRatio = if (maxChartValue > 0) {
                                            val ratio = data.amount / maxChartValue
                                            if (data.amount > 0 && ratio < 0.05f) 0.05f else ratio.toFloat()
                                        } else {
                                            0f
                                        }
                                        ChartBarWithLabel(
                                            heightRatio = heightRatio,
                                            label = data.label,
                                            amount = data.amount,
                                            isSelected = index == chartData.size - 1,
                                            showAmount = index == chartData.size - 1,
                                            primaryColor = primaryColor,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "(${languageViewModel.getTranslation("thousands")})",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ComparisonInfo(dataType, transactions, chartData, textPrimary, textSecondary)
        }
    }
}

@Composable
private fun ChartBarWithLabel(
    heightRatio: Float,
    label: String,
    amount: Double,
    isSelected: Boolean,
    showAmount: Boolean = false,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val barColor = if (isSelected) primaryColor else Color(0xFFCBD5E0)
    val textColor = if (isSelected) primaryColor else textSecondary
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

    val minHeight = 4.dp
    val calculatedHeight = (120 * heightRatio).dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (showAmount && amount > 0) {
            Text(
                text = formatCurrency(amount),
                fontSize = 12.sp,
                color = textColor,
                fontWeight = fontWeight,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(24.dp)
                .height(maxOf(calculatedHeight, minHeight))
                .background(barColor, RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = textColor,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun ComparisonInfo(
    dataType: String,
    transactions: List<Transaction>,
    chartData: List<ChartData>,
    textPrimary: Color,
    textSecondary: Color
) {
    val languageViewModel = LocalLanguageViewModel.current
    val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                languageViewModel.getTranslation("compared_to_same_period"),
                fontSize = 14.sp,
                color = textSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.Info,
                contentDescription = languageViewModel.getTranslation("info"),
                tint = textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                currentDate,
                fontSize = 12.sp,
                color = textSecondary
            )
            Text(
                formatCurrency(chartData.lastOrNull()?.amount ?: 0.0),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = getAmountColorProfessional(dataType, chartData.lastOrNull()?.amount ?: 0.0, Color(0xFFED8936))
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
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                clip = true
            ),
        shape = RoundedCornerShape(20.dp),
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
                "${getTimeRangeText(timeRange, languageViewModel)} ${languageViewModel.getTranslation("this_year")}",
                currentData,
                textPrimary = textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            ComparisonDataRow(
                "${getPreviousTimeRangeText(timeRange, languageViewModel)} ${languageViewModel.getTranslation("last_year")}",
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
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                clip = true
            ),
        shape = RoundedCornerShape(20.dp),
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
private fun NoDataPlaceholder(textSecondary: Color) {
    val languageViewModel = LocalLanguageViewModel.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                languageViewModel.getTranslation("no_data"),
                fontSize = 16.sp,
                color = textSecondary
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

// ==================== UTILITY FUNCTIONS ====================

// Data classes
data class ChartData(
    val amount: Double,
    val label: String
)

data class CategoryAmount(
    val name: String,
    val amount: Double
)

// HÀM LẤY DỮ LIỆU 2 NĂM
private fun getTwoYearData(
    dataType: String,
    transactions: List<Transaction>,
    languageViewModel: LanguageViewModel
): List<ChartData> {
    val yearlyData = mutableListOf<ChartData>()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    // CHỈ LẤY 2 NĂM: năm trước và năm nay
    for (i in 1 downTo 0) {
        val targetYear = currentYear - i

        val yearTransactions = transactions.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
            transactionCalendar.get(Calendar.YEAR) == targetYear
        }

        val amount = calculateAmountForDataType(yearTransactions, dataType)

        val label = if (i == 0) {
            languageViewModel.getTranslation("this_year")
        } else {
            targetYear.toString()
        }

        yearlyData.add(ChartData(amount, label))
    }

    return yearlyData
}

private fun getRealChartData(
    dataType: String,
    timeRange: String,
    transactions: List<Transaction>,
    languageViewModel: LanguageViewModel
): List<ChartData> {
    return when (timeRange) {
        "monthly" -> getMonthlyData(dataType, transactions, languageViewModel)
        "weekly" -> getWeeklyData(dataType, transactions, languageViewModel)
        "yearly" -> getTwoYearData(dataType, transactions, languageViewModel) // Sử dụng hàm 2 năm
        else -> emptyList()
    }
}

private fun getMonthlyData(
    dataType: String,
    transactions: List<Transaction>,
    languageViewModel: LanguageViewModel
): List<ChartData> {
    val monthlyData = mutableListOf<ChartData>()

    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    for (i in 5 downTo 0) {
        val targetMonth = (currentMonth - i + 12) % 12
        val targetYear = if (currentMonth - i < 0) currentYear - 1 else currentYear

        val monthTransactions = transactions.filter { transaction ->
            val transactionDate = parseDate(transaction.date)
            val transactionCalendar = Calendar.getInstance().apply { time = transactionDate }
            transactionCalendar.get(Calendar.MONTH) == targetMonth &&
                    transactionCalendar.get(Calendar.YEAR) == targetYear
        }

        val amount = calculateAmountForDataType(monthTransactions, dataType)

        val label = if (i == 0) {
            languageViewModel.getTranslation("this_month")
        } else {
            "T${targetMonth + 1}"
        }

        monthlyData.add(ChartData(amount, label))
    }

    return monthlyData
}

private fun getWeeklyData(
    dataType: String,
    transactions: List<Transaction>,
    languageViewModel: LanguageViewModel
): List<ChartData> {
    val weeklyData = mutableListOf<ChartData>()
    val calendar = Calendar.getInstance()

    for (i in 5 downTo 0) {
        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.add(Calendar.WEEK_OF_YEAR, -i)

        val weekStart = tempCalendar.clone() as Calendar
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        weekStart.set(Calendar.HOUR_OF_DAY, 0)
        weekStart.set(Calendar.MINUTE, 0)
        weekStart.set(Calendar.SECOND, 0)
        weekStart.set(Calendar.MILLISECOND, 0)

        val weekEnd = tempCalendar.clone() as Calendar
        weekEnd.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        weekEnd.set(Calendar.HOUR_OF_DAY, 23)
        weekEnd.set(Calendar.MINUTE, 59)
        weekEnd.set(Calendar.SECOND, 59)
        weekEnd.set(Calendar.MILLISECOND, 999)

        val isCurrentWeek = i == 0

        val weekTransactions = transactions.filter { transaction ->
            try {
                val transactionDate = parseDate(transaction.date)
                transactionDate.time in weekStart.timeInMillis..weekEnd.timeInMillis
            } catch (e: Exception) {
                false
            }
        }

        val amount = calculateAmountForDataType(weekTransactions, dataType)

        val label = if (isCurrentWeek) {
            languageViewModel.getTranslation("this_week")
        } else {
            val startMonth = weekStart.get(Calendar.MONTH) + 1
            val endMonth = weekEnd.get(Calendar.MONTH) + 1

            if (startMonth == endMonth) {
                "${weekStart.get(Calendar.DAY_OF_MONTH)}-${weekEnd.get(Calendar.DAY_OF_MONTH)}"
            } else {
                "${weekStart.get(Calendar.DAY_OF_MONTH)}/${startMonth}-${weekEnd.get(Calendar.DAY_OF_MONTH)}/${endMonth}"
            }
        }

        weeklyData.add(ChartData(amount, label))
    }

    return weeklyData
}

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
        "income" -> languageViewModel.getTranslation("income")
        "expense" -> languageViewModel.getTranslation("spending")
        "difference" -> languageViewModel.getTranslation("difference")
        else -> ""
    }
}

private fun getAmountColorProfessional(dataType: String, amount: Double, accentColor: Color): Color {
    return when (dataType) {
        "income" -> Color(0xFF2E8B57) // Xanh lá đậm
        "expense" -> Color(0xFFDC2626) // Đỏ đậm chuyên nghiệp
        "difference" -> if (amount >= 0) Color(0xFF2E8B57) else Color(0xFFDC2626)
        else -> Color.White
    }
}
