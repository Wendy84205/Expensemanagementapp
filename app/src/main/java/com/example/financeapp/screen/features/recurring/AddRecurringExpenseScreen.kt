package com.example.financeapp.screen.features.recurring

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.components.theme.getAppColors
import com.example.financeapp.data.models.RecurringExpense
import com.example.financeapp.data.models.RecurringFrequency
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringExpenseScreen(
    navController: NavController,
    onBack: () -> Unit,
    recurringExpenseViewModel: RecurringExpenseViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    existingExpense: RecurringExpense? = null
) {
    val languageViewModel = LocalLanguageViewModel.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        recurringExpenseViewModel.setCategoryViewModel(categoryViewModel)
    }

    val categories by categoryViewModel.categories.collectAsState()
    val subCategories = remember(categories) {
        categories.filter { !it.isMainCategory }
    }

    // State variables
    var title by remember { mutableStateOf(existingExpense?.title ?: "") }
    var amount by remember { 
        mutableStateOf(
            existingExpense?.amount?.let { 
                if (it == 0.0) "" 
                else if (it % 1.0 == 0.0) it.toLong().toString() 
                else it.toString() 
            } ?: ""
        ) 
    }
    var selectedCategory by remember {
        mutableStateOf(
            existingExpense?.let {
                categories.find { cat -> cat.name == it.category }
            } ?: subCategories.firstOrNull()
        )
    }
    var wallet by remember { mutableStateOf(existingExpense?.wallet ?: languageViewModel.getTranslation("main_wallet")) }
    var description by remember { mutableStateOf(existingExpense?.description ?: "") }
    var frequency by remember { mutableStateOf(existingExpense?.getFrequencyEnum() ?: RecurringFrequency.MONTHLY) }

    val initialStartDate = existingExpense?.startDate?.let {
        RecurringExpense.formatDateForUI(it)
    } ?: getTodayDateForUI()

    var startDate by remember { mutableStateOf(initialStartDate) }
    var startDateDayOfWeek by remember {
        mutableStateOf(getDayOfWeekFromDate(parseDate(startDate), languageViewModel))
    }

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

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val colors = getAppColors()
    val primaryColor = colors.primary
    val isFormValid = title.isNotBlank() && amount.toDoubleOrNull() != null && selectedCategory != null

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surface,
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.textPrimary)
                    }
                    Text(
                        if (existingExpense == null) languageViewModel.getTranslation("add_recurring_expense")
                        else languageViewModel.getTranslation("edit_recurring_expense"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surface,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Button(
                    onClick = {
                        val internalStartDate = RecurringExpense.formatDateFromUI(startDate)
                        val internalEndDate = if (hasEndDate && endDate.isNotBlank()) RecurringExpense.formatDateFromUI(endDate) else null
                        val nextOccurrence = existingExpense?.nextOccurrence ?: if (isDateBeforeOrEqual(internalStartDate, RecurringExpense.getCurrentDate())) calculateNextDate(RecurringExpense.getCurrentDate(), frequency) else internalStartDate

                        val newExpense = RecurringExpense.fromEnum(
                            id = existingExpense?.id ?: UUID.randomUUID().toString(),
                            title = title,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            category = selectedCategory?.name ?: "",
                            categoryIcon = selectedCategory?.icon ?: "💰",
                            categoryColor = selectedCategory?.color ?: "#6366F1",
                            wallet = wallet,
                            description = description.ifBlank { null },
                            frequency = frequency,
                            startDate = internalStartDate,
                            endDate = internalEndDate,
                            nextOccurrence = nextOccurrence,
                            userId = existingExpense?.userId ?: ""
                        )

                        if (existingExpense == null) {
                            recurringExpenseViewModel.addRecurringExpense(
                                title = newExpense.title, amount = newExpense.amount, category = newExpense.category,
                                categoryIcon = newExpense.categoryIcon, categoryColor = newExpense.categoryColor,
                                wallet = newExpense.wallet, description = newExpense.description,
                                frequency = newExpense.getFrequencyEnum(), startDate = newExpense.startDate, endDate = newExpense.endDate
                            )
                        } else {
                            recurringExpenseViewModel.updateRecurringExpense(newExpense)
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    enabled = isFormValid,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text(
                        if (existingExpense == null) languageViewModel.getTranslation("add_recurring_expense_button").uppercase()
                        else languageViewModel.getTranslation("update").uppercase(),
                        fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp
                    )
                }
            }
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Prominent Amount Input
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = colors.surface,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        languageViewModel.getTranslation("amount").uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BasicTextField(
                            value = amount,
                            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                color = primaryColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            cursorBrush = SolidColor(primaryColor),
                            modifier = Modifier.widthIn(min = 40.dp),
                            decorationBox = { innerTextField ->
                                if (amount.isEmpty()) {
                                    Text("0", style = MaterialTheme.typography.headlineLarge, color = colors.textMuted, textAlign = TextAlign.Center)
                                }
                                innerTextField()
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(languageViewModel.getTranslation("currency_vnd"), style = MaterialTheme.typography.titleLarge, color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Details Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Title
                    Column {
                        Text(languageViewModel.getTranslation("title"), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = title, onValueChange = { if (it.length <= 50) title = it },
                            placeholder = { Text(languageViewModel.getTranslation("title_placeholder"), color = colors.textMuted) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor, unfocusedBorderColor = colors.divider,
                                focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                                cursorColor = primaryColor,
                                focusedContainerColor = colors.background.copy(alpha = 0.3f),
                                unfocusedContainerColor = colors.background.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Category
                    Column {
                        Text(languageViewModel.getTranslation("category"), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            items(subCategories) { category ->
                                SimpleCategoryChip(category = category, isSelected = selectedCategory?.id == category.id, onClick = { selectedCategory = category }, primaryColor = primaryColor)
                            }
                        }
                    }

                    // Frequency
                    Column {
                        Text(languageViewModel.getTranslation("frequency"), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            items(RecurringFrequency.entries.toList()) { freq ->
                                SimpleFrequencyChip(frequency = freq, isSelected = frequency == freq, onClick = { frequency = freq }, primaryColor = primaryColor, languageViewModel = languageViewModel)
                            }
                        }
                    }

                    // Wallet
                    Column {
                        Text(languageViewModel.getTranslation("wallet"), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = wallet, onValueChange = { wallet = it },
                            placeholder = { Text(languageViewModel.getTranslation("main_wallet_placeholder"), color = colors.textMuted) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor, unfocusedBorderColor = colors.divider,
                                focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                                cursorColor = primaryColor,
                                focusedContainerColor = colors.background.copy(alpha = 0.3f),
                                unfocusedContainerColor = colors.background.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Date Selection
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(languageViewModel.getTranslation("start_date"), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            DateCard(date = startDate, dayOfWeek = startDateDayOfWeek, onClick = { showStartDatePicker = true }, primaryColor = primaryColor)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(languageViewModel.getTranslation("end_date_optional"), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary, modifier = Modifier.weight(1f))
                                Switch(checked = hasEndDate, onCheckedChange = { hasEndDate = it }, colors = SwitchDefaults.colors(checkedThumbColor = primaryColor))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (hasEndDate) {
                                DateCard(date = endDate, dayOfWeek = endDateDayOfWeek, onClick = { showEndDatePicker = true }, primaryColor = primaryColor)
                            } else {
                                Box(modifier = Modifier.height(56.dp).fillMaxWidth().background(colors.background.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).border(1.dp, colors.divider, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Text(languageViewModel.getTranslation("optional"), color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Notes
                    Column {
                        Text(languageViewModel.getTranslation("notes"), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description, onValueChange = { if (it.length <= 200) description = it },
                            placeholder = { Text(languageViewModel.getTranslation("add_note_placeholder"), color = colors.textMuted) },
                            modifier = Modifier.fillMaxWidth().height(100.dp), singleLine = false, maxLines = 4, shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor, unfocusedBorderColor = colors.divider,
                                focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                                cursorColor = primaryColor,
                                focusedContainerColor = colors.background.copy(alpha = 0.3f),
                                unfocusedContainerColor = colors.background.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showStartDatePicker) {
            DatePickerBottomSheetForRecurring(
                initialDate = parseDate(startDate),
                onDateSelected = { date ->
                    startDate = formatDate(date)
                    startDateDayOfWeek = getDayOfWeekFromDate(date, languageViewModel)
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false },
                title = languageViewModel.getTranslation("start_date"),
                primaryColor = primaryColor, languageViewModel = languageViewModel
            )
        }

        if (showEndDatePicker) {
            DatePickerBottomSheetForRecurring(
                initialDate = if (endDate.isNotBlank()) parseDate(endDate) else Date(),
                onDateSelected = { date ->
                    endDate = formatDate(date)
                    endDateDayOfWeek = getDayOfWeekFromDate(date, languageViewModel)
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false },
                title = languageViewModel.getTranslation("end_date"),
                primaryColor = primaryColor, languageViewModel = languageViewModel
            )
        }
    }
}

@Composable
private fun DateCard(date: String, dayOfWeek: String, onClick: () -> Unit, primaryColor: Color) {
    val colors = getAppColors()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = colors.background.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(if (date.isBlank()) "Chọn ngày" else dayOfWeek, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                if (date.isNotBlank()) {
                    Text(date, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerBottomSheetForRecurring(
    initialDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    title: String,
    primaryColor: Color,
    languageViewModel: LanguageViewModel
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.time
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = getAppColors()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).background(colors.surface)) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).background(colors.divider, RoundedCornerShape(2.dp)))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = primaryColor,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = primaryColor,
                    todayContentColor = primaryColor,
                    containerColor = colors.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Hủy")
                }
                Button(
                    onClick = { datePickerState.selectedDateMillis?.let { onDateSelected(Date(it)) } },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Xác nhận")
                }
            }
        }
    }
}

@Composable
private fun SimpleCategoryChip(category: Category, isSelected: Boolean, onClick: () -> Unit, primaryColor: Color) {
    val colors = getAppColors()
    val backgroundColor by animateColorAsState(if (isSelected) primaryColor else colors.background.copy(alpha = 0.5f), label = "")
    val contentColor by animateColorAsState(if (isSelected) Color.White else colors.textSecondary, label = "")
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, if (isSelected) primaryColor else colors.divider)
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(category.icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(category.name, color = contentColor, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun SimpleFrequencyChip(frequency: RecurringFrequency, isSelected: Boolean, onClick: () -> Unit, primaryColor: Color, languageViewModel: LanguageViewModel) {
    val colors = getAppColors()
    val backgroundColor by animateColorAsState(if (isSelected) primaryColor else colors.background.copy(alpha = 0.5f), label = "")
    val contentColor by animateColorAsState(if (isSelected) Color.White else colors.textSecondary, label = "")

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, if (isSelected) primaryColor else colors.divider)
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                when(frequency) {
                    RecurringFrequency.DAILY -> languageViewModel.getTranslation("daily")
                    RecurringFrequency.WEEKLY -> languageViewModel.getTranslation("weekly")
                    RecurringFrequency.MONTHLY -> languageViewModel.getTranslation("monthly")
                    RecurringFrequency.QUARTERLY -> languageViewModel.getTranslation("quarterly")
                    RecurringFrequency.YEARLY -> languageViewModel.getTranslation("yearly")
                },
                color = contentColor, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private fun getTodayDateForUI(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date())
}

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
        val calendar = Calendar.getInstance().apply { time = date }

        when (frequency) {
            RecurringFrequency.DAILY -> calendar.add(Calendar.DATE, 1)
            RecurringFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.MONTHLY -> {
                calendar.add(Calendar.MONTH, 1)
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (calendar.get(Calendar.DAY_OF_MONTH) > maxDay) calendar.set(Calendar.DAY_OF_MONTH, maxDay)
            }
            RecurringFrequency.QUARTERLY -> calendar.add(Calendar.MONTH, 3)
            RecurringFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        sdf.format(calendar.time)
    } catch (e: Exception) {
        fromDate
    }
}