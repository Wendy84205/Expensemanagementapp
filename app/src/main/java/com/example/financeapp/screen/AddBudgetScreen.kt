package com.example.financeapp.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.LocalDate
import java.util.*
import androidx.core.graphics.toColorInt
import com.example.financeapp.Budget
import com.example.financeapp.BudgetPeriodType
import com.example.financeapp.viewmodel.BudgetViewModel
import com.example.financeapp.viewmodel.Category
import com.example.financeapp.viewmodel.CategoryViewModel
import com.example.financeapp.calculateBudgetEndDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    navController: NavController,
    budgetViewModel: BudgetViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    existingBudget: Budget? = null
) {
    val categories by categoryViewModel.categories.collectAsState()
    val subCategories = remember(categories) {
        categories.filter { !it.isMainCategory }
    }

    // 🎨 Màu sắc đồng bộ với app
    val primaryColor = Color(0xFF0F4C75) // Navy
    val backgroundColor = Color(0xFFF5F7FA) // SoftGray
    val cardColor = Color.White
    val textColor = Color(0xFF2D3748)
    val subtitleColor = Color(0xFF718096)

    var selectedCategory by remember {
        mutableStateOf(
            existingBudget?.let {
                categories.find { cat -> cat.id == it.categoryId }
            } ?: categories.firstOrNull()
        )
    }
    var amount by remember { mutableStateOf(existingBudget?.amount?.toString() ?: "") }
    var selectedPeriod by remember { mutableStateOf(existingBudget?.periodType ?: BudgetPeriodType.MONTH) }
    var note by remember { mutableStateOf(existingBudget?.note ?: "") }

    val isFormValid = selectedCategory != null && amount.toDoubleOrNull() != null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (existingBudget == null) "Thêm ngân sách" else "Chỉnh sửa ngân sách",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = primaryColor
                )
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
                        val budgetAmount = amount.toDoubleOrNull() ?: 0.0
                        val newBudget = if (existingBudget == null) {
                            Budget(
                                id = UUID.randomUUID().toString(),
                                categoryId = selectedCategory?.id ?: "",
                                amount = budgetAmount,
                                periodType = selectedPeriod,
                                startDate = LocalDate.now(),
                                endDate = calculateBudgetEndDate(LocalDate.now(), selectedPeriod),
                                note = note.ifBlank { null },
                                spentAmount = 0.0,
                                isActive = true,
                                spent = 0.0
                            )
                        } else {
                            existingBudget.copy(
                                categoryId = selectedCategory?.id ?: "",
                                amount = budgetAmount,
                                periodType = selectedPeriod,
                                note = note.ifBlank { null }
                            )
                        }

                        if (existingBudget == null) {
                            budgetViewModel.addBudget(newBudget)
                        } else {
                            budgetViewModel.updateFullBudget(newBudget)
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isFormValid,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        if (existingBudget == null) "Thêm ngân sách" else "Cập nhật",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        AddBudgetFormContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor),
            categories = subCategories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            amount = amount,
            onAmountChange = { amount = it },
            selectedPeriod = selectedPeriod,
            onPeriodSelected = { selectedPeriod = it },
            note = note,
            onNoteChange = { note = it },
            isFormValid = isFormValid,
            primaryColor = primaryColor,
            backgroundColor = backgroundColor,
            cardColor = cardColor,
            textColor = textColor,
            subtitleColor = subtitleColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetFormContent(
    modifier: Modifier = Modifier,
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (com.example.financeapp.viewmodel.Category) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    selectedPeriod: com.example.financeapp.BudgetPeriodType,
    onPeriodSelected: (com.example.financeapp.BudgetPeriodType) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    isFormValid: Boolean,
    primaryColor: Color,
    backgroundColor: Color,
    cardColor: Color,
    textColor: Color,
    subtitleColor: Color
) {
    var showCategoryDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
    ) {
        // Main Content Card - Giống với Add Category/Recurring
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Tiêu đề
                Column {
                    Text(
                        "Thêm ngân sách mới",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        "Thiết lập hạn mức chi tiêu cho danh mục",
                        fontSize = 14.sp,
                        color = subtitleColor
                    )
                }

                // Form nhập liệu
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Danh mục
                    Column {
                        Text(
                            "Danh mục",
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        CategorySelectionCard(
                            selectedCategory = selectedCategory,
                            onClick = { showCategoryDialog = true },
                            primaryColor = primaryColor,
                            textColor = textColor,
                            subtitleColor = subtitleColor
                        )
                    }

                    // Số tiền
                    Column {
                        Text(
                            "Hạn mức ngân sách",
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                if (it.matches(Regex("^\\d*\\.?\\d*$"))) onAmountChange(it)
                            },
                            placeholder = { Text("0") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedLabelColor = primaryColor,
                                unfocusedLabelColor = subtitleColor,
                                cursorColor = primaryColor
                            ),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.AttachMoney,
                                    contentDescription = null,
                                    tint = subtitleColor
                                )
                            },
                            trailingIcon = {
                                Text(
                                    "VND",
                                    color = subtitleColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        )
                    }

                    // Chu kỳ
                    Column {
                        Text(
                            "Chu kỳ ngân sách",
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PeriodSelectionSection(
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = onPeriodSelected,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            subtitleColor = subtitleColor
                        )
                    }

                    // Ghi chú
                    Column {
                        Text(
                            "Ghi chú (tùy chọn)",
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { if (it.length <= 200) onNoteChange(it) },
                            placeholder = { Text("Thêm ghi chú cho ngân sách này...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedLabelColor = primaryColor,
                                unfocusedLabelColor = subtitleColor,
                                cursorColor = primaryColor
                            ),
                            singleLine = false,
                            maxLines = 4,
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Notes,
                                    contentDescription = null,
                                    tint = subtitleColor
                                )
                            },
                            trailingIcon = {
                                if (note.isNotBlank()) {
                                    Text(
                                        "${note.length}/200",
                                        fontSize = 12.sp,
                                        color = subtitleColor
                                    )
                                }
                            }
                        )
                    }

                    // Thông báo trạng thái
                    if (isFormValid) {
                        FormStatusIndicator(
                            message = "Sẵn sàng thêm ngân sách",
                            isSuccess = true,
                            primaryColor = primaryColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // BottomSheet chọn danh mục
    if (showCategoryDialog) {
        BudgetCategorySelectionBottomSheet(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { category ->
                onCategorySelected(category)
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false },
            primaryColor = primaryColor,
            backgroundColor = backgroundColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetCategorySelectionBottomSheet(
    categories: List<com.example.financeapp.viewmodel.Category>,
    selectedCategory: com.example.financeapp.viewmodel.Category?,
    onCategorySelected: (com.example.financeapp.viewmodel.Category) -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color,
    backgroundColor: Color
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Chọn danh mục",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(categories) { category ->
                    CategorySelectionItem(
                        category = category,
                        isSelected = selectedCategory?.id == category.id,
                        onClick = { onCategorySelected(category) },
                        primaryColor = primaryColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategorySelectionItem(
    category: com.example.financeapp.viewmodel.Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color
) {
    val backgroundColor = if (isSelected) {
        primaryColor.copy(alpha = 0.1f)
    } else {
        Color.White
    }

    val borderColor = if (isSelected) {
        primaryColor
    } else {
        Color(0xFFF1F5F9)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFFF8F9FA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Thông tin danh mục
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) primaryColor else Color(0xFF2D3748)
                )
            }

            // Radio button custom
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (isSelected) primaryColor else Color.Transparent,
                        CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) primaryColor else Color(0xFFCBD5E1),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        "✓",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySelectionCard(
    selectedCategory: com.example.financeapp.viewmodel.Category?,
    onClick: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    subtitleColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCategory != null) {
                // Hiển thị icon danh mục
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            parseColor(selectedCategory.color).copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        selectedCategory.icon,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    selectedCategory?.name ?: "Chọn danh mục",
                    color = if (selectedCategory != null) textColor else subtitleColor,
                    fontSize = 16.sp,
                    fontWeight = if (selectedCategory != null) FontWeight.Medium else FontWeight.Normal
                )
                if (selectedCategory != null) {
                    Text(
                        "Icon: ${selectedCategory.icon}",
                        color = subtitleColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Chọn danh mục",
                tint = subtitleColor
            )
        }
    }
}

@Composable
private fun PeriodSelectionSection(
    selectedPeriod: com.example.financeapp.BudgetPeriodType,
    onPeriodSelected: (com.example.financeapp.BudgetPeriodType) -> Unit,
    primaryColor: Color,
    textColor: Color,
    subtitleColor: Color
) {
    Column {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(com.example.financeapp.BudgetPeriodType.values()) { period ->
                PeriodCard(
                    period = period,
                    isSelected = selectedPeriod == period,
                    onClick = { onPeriodSelected(period) },
                    primaryColor = primaryColor,
                    textColor = textColor,
                    subtitleColor = subtitleColor
                )
            }
        }
    }
}

@Composable
private fun PeriodCard(
    period: com.example.financeapp.BudgetPeriodType,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    subtitleColor: Color
) {
    val containerColor = if (isSelected) {
        primaryColor.copy(alpha = 0.1f)
    } else {
        Color(0xFFF8F9FA)
    }

    val contentColor = if (isSelected) {
        primaryColor
    } else {
        subtitleColor
    }

    Card(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
            .shadow(
                elevation = if (isSelected) 4.dp else 1.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) primaryColor else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                getPeriodIcon(period),
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                getPeriodDisplayName(period),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun getPeriodIcon(period: com.example.financeapp.BudgetPeriodType): String {
    return when (period) {
        com.example.financeapp.BudgetPeriodType.WEEK -> "📆"
        com.example.financeapp.BudgetPeriodType.MONTH -> "🗓️"
        com.example.financeapp.BudgetPeriodType.QUARTER -> "📊"
        com.example.financeapp.BudgetPeriodType.YEAR -> "🎉"
    }
}

private fun getPeriodDisplayName(period: com.example.financeapp.BudgetPeriodType): String {
    return when (period) {
        com.example.financeapp.BudgetPeriodType.WEEK -> "Hàng tuần"
        com.example.financeapp.BudgetPeriodType.MONTH -> "Hàng tháng"
        com.example.financeapp.BudgetPeriodType.QUARTER -> "Hàng quý"
        com.example.financeapp.BudgetPeriodType.YEAR -> "Hàng năm"
    }
}

@Composable
private fun FormStatusIndicator(
    message: String,
    isSuccess: Boolean,
    primaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = primaryColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(primaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "✓",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = primaryColor
            )
        }
    }
}

// Helper function để parse color
@Composable
private fun parseColor(colorString: String): Color {
    return try {
        val color = colorString.toColorInt()
        Color(color)
    } catch (e: Exception) {
        Color(0xFF0F4C75) // Fallback to primary color
    }
}
