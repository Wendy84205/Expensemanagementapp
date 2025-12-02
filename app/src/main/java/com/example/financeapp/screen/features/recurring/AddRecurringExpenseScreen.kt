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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.model.RecurringExpense
import com.example.financeapp.model.RecurringFrequency
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
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
    LaunchedEffect(Unit) {
        recurringExpenseViewModel.setCategoryViewModel(categoryViewModel)
    }

    val categories by categoryViewModel.categories.collectAsState()
    val subCategories = remember(categories) {
        categories.filter { !it.isMainCategory }
    }

    // State variables
    var title by remember { mutableStateOf(existingExpense?.title ?: "") }
    var amount by remember { mutableStateOf(existingExpense?.amount?.toString() ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            existingExpense?.let {
                categories.find { cat -> cat.name == it.category }
            } ?: categories.firstOrNull()
        )
    }
    var wallet by remember { mutableStateOf(existingExpense?.wallet ?: "Ví chính") }
    var description by remember { mutableStateOf(existingExpense?.description ?: "") }
    var frequency by remember { mutableStateOf(existingExpense?.getFrequencyEnum() ?: RecurringFrequency.MONTHLY) }
    var startDate by remember { mutableStateOf(existingExpense?.startDate ?: getTodayDate()) }
    var endDate by remember { mutableStateOf(existingExpense?.endDate ?: "") }

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
                title = if (existingExpense == null) "Thêm chi tiêu định kỳ" else "Chỉnh sửa chi tiêu",
                onBackClick = { navController.popBackStack() }
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
                            startDate = startDate,
                            endDate = endDate.ifBlank { null },
                            nextOccurrence = existingExpense?.nextOccurrence ?: startDate
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
                        if (existingExpense == null) "THÊM CHI TIÊU" else "CẬP NHẬT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
        ) {
            // Form content
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
                        "Thông tin chi tiêu",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    // Tiêu đề
                    Column {
                        Text(
                            "Tiêu đề",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { if (it.length <= 50) title = it },
                            placeholder = { Text("Ví dụ: Tiền nhà, Internet...", color = subtitleColor) },
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
                            "Số tiền",
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
                                    "VND",
                                    color = subtitleColor,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }

                    // Danh mục
                    Column {
                        Text(
                            "Danh mục",
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
                                "Chưa có danh mục nào",
                                color = subtitleColor,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Tần suất
                    Column {
                        Text(
                            "Tần suất",
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
                                    primaryColor = primaryColor
                                )
                            }
                        }
                    }

                    // Ví
                    Column {
                        Text(
                            "Ví",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = wallet,
                            onValueChange = { wallet = it },
                            placeholder = { Text("Ví chính", color = subtitleColor) },
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
                            "Ngày bắt đầu",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SimpleDateChip(
                            date = startDate,
                            placeholder = "Chọn ngày",
                            onClick = { /* TODO: Implement date picker */ },
                            primaryColor = primaryColor
                        )
                    }

                    // Ngày kết thúc (tùy chọn)
                    Column {
                        Text(
                            "Ngày kết thúc (tùy chọn)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = subtitleColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SimpleDateChip(
                            date = endDate,
                            placeholder = "Không có ngày kết thúc",
                            onClick = { /* TODO: Implement date picker */ },
                            primaryColor = primaryColor
                        )
                    }

                    // Ghi chú
                    Column {
                        Text(
                            "Ghi chú",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { if (it.length <= 200) description = it },
                            placeholder = { Text("Thêm ghi chú...", color = subtitleColor) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopAppBar(
    title: String,
    onBackClick: () -> Unit
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
                    contentDescription = "Quay lại",
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
        Text(
            getSimpleFrequencyName(frequency),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

private fun getSimpleFrequencyName(frequency: RecurringFrequency): String {
    return when (frequency) {
        RecurringFrequency.DAILY -> "Hàng ngày"
        RecurringFrequency.WEEKLY -> "Hàng tuần"
        RecurringFrequency.MONTHLY -> "Hàng tháng"
        RecurringFrequency.YEARLY -> "Hàng năm"
        RecurringFrequency.QUARTERLY -> "Hàng quý"
    }
}

@Composable
private fun SimpleDateChip(
    date: String,
    placeholder: String,
    onClick: () -> Unit,
    primaryColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        border = BorderStroke(1.dp, Color(0xFFDDDDDD))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (date.isNotBlank()) date else placeholder,
                fontSize = 15.sp,
                color = if (date.isNotBlank()) Color(0xFF333333) else Color(0xFF888888),
                fontWeight = if (date.isNotBlank()) FontWeight.Medium else FontWeight.Normal
            )
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = "Chọn ngày",
                tint = Color(0xFF666666),
                modifier = Modifier.size(20.dp)
            )
        }
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

// Helper function để lấy ngày hiện tại
private fun getTodayDate(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date())
}