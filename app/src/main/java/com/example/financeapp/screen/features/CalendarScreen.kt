package com.example.financeapp.screen.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.rememberLanguageText
import com.example.financeapp.viewmodel.transaction.Category // Thêm import này
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavHostController,
    transactions: List<Transaction>,
    categories: List<Category>
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val coroutineScope = rememberCoroutineScope()

    // Màu sắc theo gói chuyên nghiệp & tin cậy
    val primaryColor = Color(0xFF0F4C75) // Xanh Navy
    val secondaryColor = Color(0xFF2E8B57) // Xanh lá đậm
    val backgroundColor = Color(0xFFF5F7FA) // Xám rất nhạt
    val surfaceColor = Color.White // Trắng
    val textPrimary = Color(0xFF2D3748) // Xám đen
    val textSecondary = Color(0xFF718096) // Xám
    val accentColor = Color(0xFFED8936) // Cam
    val incomeColor = Color(0xFF2E8B57) // Xanh lá đậm
    val expenseColor = Color(0xFFE53E3E) // Đỏ đậm

    val gradient = Brush.verticalGradient(
        colors = listOf(primaryColor, Color(0xFF1A365D))
    )

    // Lấy các text đa ngôn ngữ
    val calendarText = rememberLanguageText("calendar")
    val thisMonthText = rememberLanguageText("this_month")
    val totalIncomeText = rememberLanguageText("total_income")
    val totalExpenseText = rememberLanguageText("total_expense")
    val differenceText = rememberLanguageText("difference")
    val transactionListText = rememberLanguageText("transaction_list")
    val noTransactionsText = rememberLanguageText("no_transactions")
    val selectOtherDayText = rememberLanguageText("select_other_day")
    val previousMonthText = rememberLanguageText("previous_month")
    val nextMonthText = rememberLanguageText("next_month")

    // Lấy text cho các ngày trong tuần
    val mondayShort = rememberLanguageText("monday_short")
    val tuesdayShort = rememberLanguageText("tuesday_short")
    val wednesdayShort = rememberLanguageText("wednesday_short")
    val thursdayShort = rememberLanguageText("thursday_short")
    val fridayShort = rememberLanguageText("friday_short")
    val saturdayShort = rememberLanguageText("saturday_short")
    val sundayShort = rememberLanguageText("sunday_short")

    // Lọc giao dịch theo ngày được chọn
    val dayTransactions = remember(selectedDate, transactions) {
        transactions.filter {
            try {
                LocalDate.parse(it.date, DateTimeFormatter.ofPattern("dd/MM/yyyy")) == selectedDate
            } catch (e: Exception) {
                false
            }
        }
    }

    // Tính tổng thu - chi - chênh lệch
    val currentMonthTransactions = transactions.filter {
        try {
            val transactionDate = LocalDate.parse(it.date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            transactionDate.month == selectedDate.month && transactionDate.year == selectedDate.year
        } catch (e: Exception) {
            false
        }
    }

    val totalIncome = currentMonthTransactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = currentMonthTransactions.filter { !it.isIncome }.sumOf { it.amount }
    val difference = totalIncome - totalExpense

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        calendarText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
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
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Thống kê tháng
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp),
                            clip = true
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = thisMonthText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                CalendarStatItem(
                                    totalIncomeText,
                                    totalIncome,
                                    true,
                                    incomeColor,
                                    textSecondary
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CalendarStatItem(
                                    totalExpenseText,
                                    totalExpense,
                                    false,
                                    expenseColor,
                                    textSecondary
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CalendarStatItem(
                                    differenceText,
                                    difference,
                                    isIncome = difference >= 0,
                                    color = if (difference >= 0) incomeColor else expenseColor,
                                    textSecondary = textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lịch
                val currentMonth = YearMonth.now()
                val startMonth = currentMonth.minusMonths(12)
                val endMonth = currentMonth.plusMonths(12)
                val daysOfWeek = daysOfWeek()

                val calendarState = rememberCalendarState(
                    startMonth = startMonth,
                    endMonth = endMonth,
                    firstVisibleMonth = YearMonth.from(selectedDate),
                    firstDayOfWeek = daysOfWeek.first()
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp),
                            clip = true
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Header với nút chuyển tháng
                        CalendarHeader(
                            calendarState = calendarState,
                            onPreviousClick = {
                                coroutineScope.launch {
                                    val currentFirstMonth = calendarState.firstVisibleMonth.yearMonth
                                    val previousMonth = currentFirstMonth.minusMonths(1)
                                    if (previousMonth >= startMonth) {
                                        calendarState.animateScrollToMonth(previousMonth)
                                    }
                                }
                            },
                            onNextClick = {
                                coroutineScope.launch {
                                    val currentFirstMonth = calendarState.firstVisibleMonth.yearMonth
                                    val nextMonth = currentFirstMonth.plusMonths(1)
                                    if (nextMonth <= endMonth) {
                                        calendarState.animateScrollToMonth(nextMonth)
                                    }
                                }
                            },
                            previousMonthText = previousMonthText,
                            nextMonthText = nextMonthText,
                            primaryColor = primaryColor,
                            textPrimary = textPrimary
                        )

                        // Hiển thị các ngày trong tuần
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            daysOfWeek.forEach { dayOfWeek ->
                                Text(
                                    text = getDayOfWeekText(
                                        dayOfWeek.value,
                                        mondayShort,
                                        tuesdayShort,
                                        wednesdayShort,
                                        thursdayShort,
                                        fridayShort,
                                        saturdayShort,
                                        sundayShort
                                    ),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Hiển thị lịch
                        HorizontalCalendar(
                            state = calendarState,
                            dayContent = { day ->
                                val date = day.date
                                val isSelected = date == selectedDate

                                val dayTransactionsForDate = transactions.filter {
                                    try {
                                        LocalDate.parse(it.date, DateTimeFormatter.ofPattern("dd/MM/yyyy")) == date
                                    } catch (e: Exception) {
                                        false
                                    }
                                }

                                val hasTransactions = dayTransactionsForDate.isNotEmpty()
                                val dayTotal = dayTransactionsForDate.sumOf {
                                    if (it.isIncome) it.amount else -it.amount
                                }

                                CalendarDayCell(
                                    date = date,
                                    isSelected = isSelected,
                                    hasTransactions = hasTransactions,
                                    dayTotal = dayTotal,
                                    onClick = { selectedDate = date },
                                    primaryColor = primaryColor,
                                    textPrimary = textPrimary,
                                    incomeColor = incomeColor,
                                    expenseColor = expenseColor
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Danh sách giao dịch trong ngày
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp),
                            clip = true
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = transactionListText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )

                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Danh sách giao dịch
                        if (dayTransactions.isEmpty()) {
                            CalendarEmptyState(
                                noTransactionsText = noTransactionsText,
                                selectOtherDayText = selectOtherDayText,
                                textSecondary = textSecondary
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(dayTransactions) { transaction ->
                                    CalendarTransactionItem(
                                        transaction = transaction,
                                        categories = categories, // Truyền categories vào
                                        incomeColor = incomeColor,
                                        expenseColor = expenseColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarStatItem(
    title: String,
    amount: Double,
    isIncome: Boolean,
    color: Color,
    textSecondary: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            title,
            color = textSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatCurrency(amount),
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CalendarHeader(
    calendarState: CalendarState,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    previousMonthText: String,
    nextMonthText: String,
    primaryColor: Color,
    textPrimary: Color
) {
    val currentMonth = calendarState.firstVisibleMonth.yearMonth
    val monthName = currentMonth.month.name.lowercase()
        .replaceFirstChar { it.uppercase() }
    val year = currentMonth.year

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Nút previous
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier
                .size(36.dp)
                .background(primaryColor, CircleShape)
                .clip(CircleShape)
        ) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                contentDescription = previousMonthText,
                tint = Color.White
            )
        }

        // Tên tháng và năm
        Text(
            text = "$monthName $year",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        // Nút next
        IconButton(
            onClick = onNextClick,
            modifier = Modifier
                .size(36.dp)
                .background(primaryColor, CircleShape)
                .clip(CircleShape)
        ) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = nextMonthText,
                tint = Color.White
            )
        }
    }
}

@Composable
fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    hasTransactions: Boolean,
    dayTotal: Double,
    onClick: () -> Unit,
    primaryColor: Color,
    textPrimary: Color,
    incomeColor: Color,
    expenseColor: Color
) {
    val isToday = date == LocalDate.now()
    val dayColor = when {
        isSelected -> Color.White
        isToday -> primaryColor
        else -> textPrimary
    }
    val backgroundColor = when {
        isSelected -> primaryColor
        isToday -> primaryColor.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(backgroundColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Số ngày
            Text(
                text = date.dayOfMonth.toString(),
                color = dayColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            )

            // Hiển thị số tiền nếu có giao dịch
            if (hasTransactions && dayTotal.absoluteValue > 0) {
                val displayAmount = dayTotal.absoluteValue / 1000
                if (displayAmount >= 1) {
                    Text(
                        text = "%.0fK".format(displayAmount),
                        color = if (dayTotal >= 0) incomeColor else expenseColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarTransactionItem(
    transaction: Transaction,
    categories: List<Category>,
    incomeColor: Color,
    expenseColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    // Tìm category name - GIỐNG CÁCH CỦA TransactionListItem TRONG TRANSACTION SCREEN
    val categoryName = remember(transaction.category, categories) {
        categories.find {
            it.id == transaction.categoryId ||
                    it.id == transaction.category ||  // Kiểm tra cả id
                    it.name.equals(transaction.category, ignoreCase = true)
        }?.name ?: transaction.category.ifBlank { "Không xác định" } // Thêm fallback
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                clip = true
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon indicator - CÓ THỂ SỬA THEO HOME SCREEN NẾU CẦN
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (transaction.isIncome) incomeColor.copy(alpha = 0.1f)
                            else expenseColor.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Có thể thay đổi icon nếu muốn giống HomeScreen
                    Text(
                        if (transaction.isIncome) "↑" else "↓",
                        color = if (transaction.isIncome) incomeColor else expenseColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    // Hiển thị tên category
                    Text(
                        text = categoryName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                    Row {
                        Text(
                            text = "${transaction.date}",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                        if (transaction.description.isNotBlank()) {
                            Text(
                                text = " • ${transaction.description}",
                                color = textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Số tiền - GIỮ NGUYÊN HOẶC SỬA THEO HOME SCREEN
            Text(
                text = (if (transaction.isIncome) "+" else "-") + formatCurrency(transaction.amount),
                fontWeight = FontWeight.Bold,
                color = if (transaction.isIncome) incomeColor else expenseColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CalendarEmptyState(
    noTransactionsText: String,
    selectOtherDayText: String,
    textSecondary: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📅", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                noTransactionsText,
                color = textSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                selectOtherDayText,
                color = textSecondary.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Hàm lấy text cho các ngày trong tuần (không cần @Composable)
private fun getDayOfWeekText(
    dayOfWeek: Int,
    mondayShort: String,
    tuesdayShort: String,
    wednesdayShort: String,
    thursdayShort: String,
    fridayShort: String,
    saturdayShort: String,
    sundayShort: String
): String {
    return when (dayOfWeek) {
        1 -> mondayShort
        2 -> tuesdayShort
        3 -> wednesdayShort
        4 -> thursdayShort
        5 -> fridayShort
        6 -> saturdayShort
        7 -> sundayShort
        else -> ""
    }
}

// Hàm định dạng tiền tệ
fun formatCurrency(amount: Double): String {
    return "%,.0fđ".format(amount)
}