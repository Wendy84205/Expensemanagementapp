package com.example.financeapp.screen.features.recurring

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.data.models.RecurringExpense
import com.example.financeapp.data.models.RecurringFrequency
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringExpenseScreen(
    navController: NavController,
    recurringExpenseViewModel: RecurringExpenseViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    existingExpense: RecurringExpense? = null
) {
    val languageViewModel = LocalLanguageViewModel.current

    LaunchedEffect(Unit) {
        recurringExpenseViewModel.setCategoryViewModel(categoryViewModel)
    }

    val categories by categoryViewModel.categories.collectAsState()
    val subCategories = remember(categories) {
        categories.filter { !it.isMainCategory }
    }

    // State variables với chuyển đổi ngày
    var title by remember { mutableStateOf(existingExpense?.title ?: "") }
    var amount by remember { mutableStateOf(existingExpense?.amount?.toString() ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            existingExpense?.let {
                categories.find { cat -> cat.name == it.category }
            } ?: categories.firstOrNull()
        )
    }
    var wallet by remember { mutableStateOf(existingExpense?.wallet ?: languageViewModel.getTranslation("main_wallet")) }
    var description by remember { mutableStateOf(existingExpense?.description ?: "") }
    var frequency by remember { mutableStateOf(existingExpense?.getFrequencyEnum() ?: RecurringFrequency.MONTHLY) }

    // Chuyển đổi ngày từ internal format sang UI format
    val initialStartDate = existingExpense?.startDate?.let {
        RecurringExpense.formatDateForUI(it)
    } ?: getTodayDateForUI()

    var startDate by remember { mutableStateOf(initialStartDate) }
    var startDateDayOfWeek by remember {
        mutableStateOf(getDayOfWeekFromDate(parseDate(startDate), languageViewModel))
    }

    // End date state
    val hasExistingEndDate = existingExpense?.endDate != null
    var hasEndDate by remember { mutableStateOf(hasExistingEndDate) }

    val initialEndDate = existingExpense?.endDate?.let {
        RecurringExpense.formatDateForUI(it)
    } ?: ""

    var endDate by remember { mutableStateOf(initialEndDate) }
    var endDateDayOfWeek by remember {
        mutableStateOf(
            if (endDate.isNotBlank()) getDayOfWeekFromDate(parseDate(endDate), languageViewModel)
            else ""
        )
    }

    // State cho DatePicker
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Colors
    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White
    val textColor = Color(0xFF333333)
    val subtitleColor = Color(0xFF666666)

    val isFormValid = title.isNotBlank() &&
            amount.toDoubleOrNull() != null &&
            selectedCategory != null

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                title = if (existingExpense == null) languageViewModel.getTranslation("add_recurring_expense")
                else languageViewModel.getTranslation("edit_recurring_expense"),
                onBackClick = { navController.popBackStack() },
                languageViewModel = languageViewModel
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardColor)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        // CHUYỂN ĐỔI NGÀY TỪ UI FORMAT SANG INTERNAL FORMAT
                        val internalStartDate = RecurringExpense.formatDateFromUI(startDate)
                        val internalEndDate = if (hasEndDate && endDate.isNotBlank()) {
                            RecurringExpense.formatDateFromUI(endDate)
                        } else null

                        val nextOccurrence = existingExpense?.nextOccurrence ?:
                        if (isDateBeforeOrEqual(internalStartDate, RecurringExpense.getCurrentDate())) {
                            // Nếu start date đã qua, tính next occurrence từ hôm nay
                            calculateNextDate(RecurringExpense.getCurrentDate(), frequency)
                        } else {
                            internalStartDate
                        }

                        val newExpense = RecurringExpense.fromEnum(
                            id = existingExpense?.id ?: UUID.randomUUID().toString(),
                            title = title,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            category = selectedCategory?.name ?: "",
                            categoryIcon = selectedCategory?.icon ?: "💰",
                            categoryColor = selectedCategory?.color ?: "#2196F3",
                            wallet = wallet,
                            description = description.ifBlank { null },
                            frequency = frequency,
                            startDate = internalStartDate,
                            endDate = internalEndDate,
                            nextOccurrence = nextOccurrence,
                            isActive = existingExpense?.isActive ?: true,
                            userId = existingExpense?.userId ?: "",
                            totalGenerated = existingExpense?.totalGenerated ?: 0,
                            lastGenerated = existingExpense?.lastGenerated
                        )

                        if (existingExpense == null) {
                            recurringExpenseViewModel.addRecurringExpense(
                                title = newExpense.title,
                                amount = newExpense.amount,
                                category = newExpense.category,
                                categoryIcon = newExpense.categoryIcon,
                                categoryColor = newExpense.categoryColor,
                                wallet = newExpense.wallet,
                                description = newExpense.description,
                                frequency = newExpense.getFrequencyEnum(),
                                startDate = newExpense.startDate,
                                endDate = newExpense.endDate
                            )
                        } else {
                            recurringExpenseViewModel.updateRecurringExpense(newExpense)
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        disabledContainerColor = Color(0xFFCCCCCC)
                    )
                ) {
                    Text(
                        if (existingExpense == null) languageViewModel.getTranslation("add_recurring_expense_button").uppercase()
                        else languageViewModel.getTranslation("update").uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backgroundColor)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            languageViewModel.getTranslation("expense_info"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        // Tiêu đề
                        Column {
                            Text(
                                languageViewModel.getTranslation("title"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = title,
                                onValueChange = { if (it.length <= 50) title = it },
                                placeholder = { Text(languageViewModel.getTranslation("title_placeholder"), color = subtitleColor) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    cursorColor = primaryColor
                                )
                            )
                        }

                        // Số tiền
                        Column {
                            Text(
                                languageViewModel.getTranslation("amount"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = amount,
                                onValueChange = {
                                    if (it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it
                                },
                                placeholder = { Text("0", color = subtitleColor) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    cursorColor = primaryColor
                                ),
                                trailingIcon = {
                                    Text(
                                        languageViewModel.getTranslation("currency_vnd"),
                                        color = subtitleColor,
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }

                        // Danh mục
                        Column {
                            Text(
                                languageViewModel.getTranslation("category"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (subCategories.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(subCategories) { category ->
                                        SimpleCategoryChip(
                                            category = category,
                                            isSelected = selectedCategory?.id == category.id,
                                            onClick = { selectedCategory = category },
                                            primaryColor = primaryColor
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    languageViewModel.getTranslation("no_categories"),
                                    color = subtitleColor,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Tần suất
                        Column {
                            Text(
                                languageViewModel.getTranslation("frequency"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(RecurringFrequency.entries.toList()) { freq ->
                                    SimpleFrequencyChip(
                                        frequency = freq,
                                        isSelected = frequency == freq,
                                        onClick = { frequency = freq },
                                        primaryColor = primaryColor,
                                        languageViewModel = languageViewModel
                                    )
                                }
                            }
                        }

                        // Ví
                        Column {
                            Text(
                                languageViewModel.getTranslation("wallet"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = wallet,
                                onValueChange = { wallet = it },
                                placeholder = { Text(languageViewModel.getTranslation("main_wallet_placeholder"), color = subtitleColor) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    cursorColor = primaryColor
                                )
                            )
                        }

                        // Ngày bắt đầu
                        Column {
                            Text(
                                languageViewModel.getTranslation("start_date"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showStartDatePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(
                                                    primaryColor.copy(alpha = 0.08f),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.CalendarToday,
                                                contentDescription = "Ngày",
                                                tint = primaryColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                startDateDayOfWeek,
                                                color = Color(0xFF1F2937),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                startDate,
                                                color = Color(0xFF6B7280),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Chọn ngày",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Ngày kết thúc (optional)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    languageViewModel.getTranslation("end_date_optional"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )

                                Switch(
                                    checked = hasEndDate,
                                    onCheckedChange = { hasEndDate = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = primaryColor,
                                        checkedTrackColor = primaryColor.copy(alpha = 0.5f)
                                    )
                                )
                            }

                            if (hasEndDate) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showEndDatePicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(
                                                        primaryColor.copy(alpha = 0.08f),
                                                        RoundedCornerShape(10.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Filled.CalendarToday,
                                                    contentDescription = "Ngày kết thúc",
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    if (endDate.isNotEmpty()) endDateDayOfWeek
                                                    else languageViewModel.getTranslation("select_end_date"),
                                                    color = Color(0xFF1F2937),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    endDate.ifEmpty { languageViewModel.getTranslation("no_date_selected") },
                                                    color = Color(0xFF6B7280),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "Chọn ngày",
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Ghi chú
                        Column {
                            Text(
                                languageViewModel.getTranslation("notes"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = description,
                                onValueChange = { if (it.length <= 200) description = it },
                                placeholder = { Text(languageViewModel.getTranslation("add_note_placeholder"), color = subtitleColor) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                singleLine = false,
                                maxLines = 3,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    cursorColor = primaryColor
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // DatePicker cho start date
    if (showStartDatePicker) {
        DatePickerBottomSheetForRecurring(
            initialDate = parseDate(startDate),
            onDateSelected = { date ->
                startDate = formatDate(date)
                startDateDayOfWeek = getDayOfWeekFromDate(date, languageViewModel)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
            primaryColor = primaryColor,
            title = "Chọn ngày bắt đầu"
        )
    }

    // DatePicker cho end date
    if (showEndDatePicker) {
        DatePickerBottomSheetForRecurring(
            initialDate = if (endDate.isNotBlank()) parseDate(endDate) else Date(),
            onDateSelected = { date ->
                endDate = formatDate(date)
                endDateDayOfWeek = getDayOfWeekFromDate(date, languageViewModel)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
            primaryColor = primaryColor,
            title = "Chọn ngày kết thúc"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerBottomSheetForRecurring(
    initialDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color,
    title: String = "Chọn ngày"
) {
    val calendar = Calendar.getInstance().apply {
        time = initialDate
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis
    )

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    val configuration = LocalConfiguration.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFD1D5DB), RoundedCornerShape(2.dp))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 450.dp, max = 550.dp)
                    .weight(1f, fill = false)
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        selectedDayContainerColor = primaryColor,
                        selectedDayContentColor = Color.White,
                        todayDateBorderColor = primaryColor,
                        todayContentColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(Color.White)
            ) {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = Date(millis)
                    val formattedDate = formatDate(selectedDate)
                    val dayOfWeek = getDayOfWeekFromDate(selectedDate, LocalLanguageViewModel.current)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = primaryColor.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Đã chọn:",
                                    color = Color(0xFF6B7280),
                                    fontSize = 11.sp
                                )
                                Text(
                                    "$dayOfWeek, $formattedDate",
                                    color = Color(0xFF1F2937),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Đã chọn",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                    ) {
                        Text("Hủy", fontSize = 14.sp, color = Color(0xFF6B7280))
                    }

                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                onDateSelected(Date(it))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        enabled = datePickerState.selectedDateMillis != null
                    ) {
                        Text("Xác nhận", fontSize = 14.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = languageViewModel.getTranslation("back"),
                    tint = Color(0xFF333333)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
private fun SimpleCategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color
) {
    val backgroundColor = if (isSelected) primaryColor else Color(0xFFEEEEEE)
    val textColor = if (isSelected) Color.White else Color(0xFF666666)

    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.defaultMinSize(minWidth = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                category.icon,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                category.name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SimpleFrequencyChip(
    frequency: RecurringFrequency,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    languageViewModel: LanguageViewModel
) {
    val backgroundColor = if (isSelected) primaryColor else Color(0xFFEEEEEE)
    val textColor = if (isSelected) Color.White else Color(0xFF666666)

    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.defaultMinSize(minWidth = 1.dp)
    ) {
        Text(
            getSimpleFrequencyName(frequency, languageViewModel),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

private fun getSimpleFrequencyName(frequency: RecurringFrequency, languageViewModel: LanguageViewModel): String {
    return when (frequency) {
        RecurringFrequency.DAILY -> languageViewModel.getTranslation("daily")
        RecurringFrequency.WEEKLY -> languageViewModel.getTranslation("weekly")
        RecurringFrequency.MONTHLY -> languageViewModel.getTranslation("monthly")
        RecurringFrequency.YEARLY -> languageViewModel.getTranslation("yearly")
        RecurringFrequency.QUARTERLY -> languageViewModel.getTranslation("quarterly")
    }
}

@Composable
private fun parseColor(colorString: String): Color {
    return try {
        val color = colorString.toColorInt()
        Color(color)
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}

// Helper function để lấy ngày hiện tại theo UI format
private fun getTodayDateForUI(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date())
}

// ============== HÀM UTILITY ==============
private fun parseDate(dateString: String): Date {
    return try {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        format.parse(dateString) ?: Date()
    } catch (e: Exception) {
        Date()
    }
}

private fun formatDate(date: Date): String {
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return format.format(date)
}

private fun getDayOfWeekFromDate(date: Date, languageViewModel: LanguageViewModel): String {
    val days = listOf(
        languageViewModel.getTranslation("sunday"),
        languageViewModel.getTranslation("monday"),
        languageViewModel.getTranslation("tuesday"),
        languageViewModel.getTranslation("wednesday"),
        languageViewModel.getTranslation("thursday"),
        languageViewModel.getTranslation("friday"),
        languageViewModel.getTranslation("saturday")
    )
    val cal = Calendar.getInstance().apply { time = date }
    return days[cal.get(Calendar.DAY_OF_WEEK) - 1]
}

private fun isDateBeforeOrEqual(date1: String, date2: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val d1 = sdf.parse(date1)
        val d2 = sdf.parse(date2)
        d1 != null && d2 != null && (d1.before(d2) || d1 == d2)
    } catch (e: Exception) {
        false
    }
}

private fun calculateNextDate(fromDate: String, frequency: RecurringFrequency): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(fromDate) ?: return fromDate

        val calendar = Calendar.getInstance()
        calendar.time = date

        when (frequency) {
            RecurringFrequency.DAILY -> calendar.add(Calendar.DATE, 1)
            RecurringFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.MONTHLY -> {
                calendar.add(Calendar.MONTH, 1)
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
                if (currentDay > maxDay) {
                    calendar.set(Calendar.DAY_OF_MONTH, maxDay)
                }
            }
            RecurringFrequency.QUARTERLY -> calendar.add(Calendar.MONTH, 3)
            RecurringFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }

        sdf.format(calendar.time)
    } catch (e: Exception) {
        fromDate
    }
}