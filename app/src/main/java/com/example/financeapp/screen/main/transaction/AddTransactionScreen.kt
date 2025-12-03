@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.financeapp.screen.main.transaction

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddTransactionScreen(
    navController: NavController,
    onBack: () -> Unit,
    onSave: (Transaction) -> Unit,
    transactionViewModel: TransactionViewModel = viewModel(),
    budgetViewModel: BudgetViewModel = viewModel(),
    categoryViewModel: com.example.financeapp.viewmodel.transaction.CategoryViewModel = viewModel(),
    existingTransaction: Transaction? = null,
    onDelete: (() -> Unit)? = null
) {
    val languageViewModel = com.example.financeapp.LocalLanguageViewModel.current

    var amount by remember { mutableStateOf(existingTransaction?.amount?.toString() ?: "") }
    var categoryId by remember { mutableStateOf(existingTransaction?.category ?: "") }
    var isIncome by remember { mutableStateOf(existingTransaction?.isIncome ?: false) }
    var description by remember { mutableStateOf(existingTransaction?.description ?: "") }
    var transactionDate by remember { mutableStateOf(existingTransaction?.date ?: getTodayDate()) }
    var transactionDayOfWeek by remember { mutableStateOf(existingTransaction?.dayOfWeek ?: getTodayDayOfWeek(languageViewModel)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val transactionType = if (isIncome) "income" else "expense"
    val selectableCategoriesMap by categoryViewModel.selectableCategories.collectAsState()
    val selectableCategories = remember(selectableCategoriesMap, transactionType) {
        selectableCategoriesMap[transactionType] ?: emptyList()
    }

    // Lấy danh mục đã chọn
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("selectedCategoryId", null)?.collect { selectedId ->
            selectedId?.let {
                categoryId = it
                savedStateHandle.remove<String>("selectedCategoryId")
            }
        }
    }

    // Lấy danh mục chính cho hiển thị ban đầu
    val categories by categoryViewModel.categories.collectAsState()
    val mainCategories = remember(categories, transactionType) {
        categoryViewModel.getMainCategories(transactionType).filter { it.name != "Khác" }
    }

    // Lấy 3 danh mục con đầu tiên từ mỗi danh mục cha
    val displayCategories = remember(mainCategories, categories, transactionType) {
        val subCategories = mutableListOf<com.example.financeapp.viewmodel.transaction.Category>()
        mainCategories.forEach { mainCategory ->
            val firstSubCategory = categoryViewModel.getSubCategories(mainCategory.id).firstOrNull()
            if (firstSubCategory != null) {
                subCategories.add(firstSubCategory)
            }
        }
        val limitedCategories = subCategories.take(3).toMutableList()
        // Thêm "Khác" ở cuối
        limitedCategories.add(
            com.example.financeapp.viewmodel.transaction.Category(
                "other",
                "Khác",
                transactionType,
                false,
                null,
                "📁",
                "#9F7AEA"
            )
        )
        limitedCategories
    }

    val selectedCategoryInfo = selectableCategories.find { it.id == categoryId } ?: displayCategories.find { it.id == categoryId }

    // 🎨 Màu sắc chủ đạo
    val primaryColor = if (isIncome) Color(0xFF48BB78) else Color(0xFFE91E63)
    val backgroundColor = Color(0xFFF7FAFC)
    val cardColor = Color.White
    val textColor = Color(0xFF2D3748)
    val subtitleColor = Color(0xFF718096)

    val isSaveEnabled = amount.isNotBlank() && categoryId.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        languageViewModel.getTranslation("record_transaction"),
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = languageViewModel.getTranslation("back"),
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cardColor,
                    scrolledContainerColor = cardColor
                ),
                modifier = Modifier.shadow(elevation = 2.dp, shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Card chính chứa form - nhỏ hơn
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Loại giao dịch (Thu/Chi) - UI cải thiện nhỏ hơn
                    TransactionTypeSectionCompact(
                        isIncome = isIncome,
                        onTypeChange = { newIsIncome ->
                            isIncome = newIsIncome
                            categoryId = "" // Reset category khi đổi loại
                        },
                        languageViewModel = languageViewModel,
                        primaryColor = primaryColor
                    )

                    // Nhập số tiền - UI cải thiện nhỏ hơn
                    AmountSectionCompact(
                        amount = amount,
                        onAmountChange = { newValue ->
                            if (newValue.matches(Regex("^\\d*\\.?\\d*$")) || newValue.isEmpty()) amount = newValue
                        },
                        amountText = languageViewModel.getTranslation("amount"),
                        primaryColor = primaryColor
                    )

                    // Danh mục - UI cải thiện nhỏ hơn
                    CategorySectionCompact(
                        categories = displayCategories,
                        selectedCategoryId = categoryId,
                        onCategorySelected = { selected ->
                            if (selected.id == "other") {
                                // Khi chọn "Khác", navigate đến CategorySelectionScreen
                                navController.navigate("categories?transactionType=$transactionType&returnTo=add_transaction")
                            } else {
                                categoryId = selected.id
                            }
                        },
                        onOtherCategoryClick = {
                            navController.navigate("categories?transactionType=$transactionType&returnTo=add_transaction")
                        },
                        categoryText = languageViewModel.getTranslation("category"),
                        primaryColor = primaryColor
                    )

                    // Ngày - UI cải thiện nhỏ hơn với nút chọn ngày
                    DateSectionCompact(
                        date = transactionDate,
                        dayOfWeek = transactionDayOfWeek,
                        onDateClick = { showDatePicker = true },
                        transactionDateText = languageViewModel.getTranslation("transaction_date"),
                        primaryColor = primaryColor
                    )

                    // Ghi chú - UI cải thiện nhỏ hơn
                    DescriptionSectionCompact(
                        description = description,
                        onDescriptionChange = { description = it },
                        noteText = languageViewModel.getTranslation("note"),
                        enterTransactionDescriptionText = languageViewModel.getTranslation("enter_transaction_description"),
                        primaryColor = primaryColor
                    )

                    // Nút lưu - UI cải thiện nhỏ hơn
                    SaveButtonCompact(
                        isEnabled = isSaveEnabled,
                        isIncome = isIncome,
                        isEditing = existingTransaction != null,
                        languageViewModel = languageViewModel,
                        primaryColor = primaryColor,
                        onClick = {
                            val transaction = Transaction(
                                id = existingTransaction?.id ?: generateTransactionId(),
                                date = transactionDate,
                                dayOfWeek = transactionDayOfWeek,
                                category = categoryId,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                isIncome = isIncome,
                                group = if (isIncome) languageViewModel.getTranslation("income")
                                else languageViewModel.getTranslation("spending"),
                                wallet = "Ví chính",
                                description = description,
                                categoryIcon = selectedCategoryInfo?.icon,
                                categoryId = selectedCategoryInfo?.id ?: "",
                                categoryColor = selectedCategoryInfo?.color ?: "#667EEA",
                                title = description.ifBlank { selectedCategoryInfo?.name ?: categoryId }
                            )
                            onSave(transaction)
                        }
                    )

                    // Nút xóa (nếu đang chỉnh sửa) - nhỏ hơn
                    if (existingTransaction != null && onDelete != null) {
                        DeleteButtonCompact(
                            languageViewModel = languageViewModel,
                            primaryColor = primaryColor,
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }
            }

            // Hiển thị lỗi/warning nếu có - nhỏ hơn
            val warning by transactionViewModel.warningMessage.collectAsState()
            warning?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    border = BorderStroke(1.dp, Color(0xFFFFEEBA))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    // DatePicker Bottom Sheet
    if (showDatePicker) {
        DatePickerBottomSheet(
            initialDate = parseDate(transactionDate),
            onDateSelected = { date ->
                transactionDate = formatDate(date)
                transactionDayOfWeek = getDayOfWeekFromDate(date, languageViewModel)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            primaryColor = primaryColor
        )
    }

    // Dialog xác nhận xóa - nhỏ hơn
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    languageViewModel.getTranslation("delete_transaction"),
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    languageViewModel.getTranslation("delete_transaction_description"),
                    color = subtitleColor,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete?.invoke()
                        onBack()
                    }
                ) {
                    Text(
                        languageViewModel.getTranslation("delete"),
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        languageViewModel.getTranslation("cancel"),
                        color = primaryColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ============== CÁC COMPOSABLE COMPACT (KÍCH THƯỚC NHỎ HƠN) ==============

@Composable
private fun TransactionTypeSectionCompact(
    isIncome: Boolean,
    onTypeChange: (Boolean) -> Unit,
    languageViewModel: LanguageViewModel,
    primaryColor: Color
) {
    Column {
        Text(
            "Loại giao dịch",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF4B5563),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nút Thu nhập - nhỏ hơn
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isIncome) Color(0xFF48BB78) else Color(0xFFF9FAFB)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isIncome) Color(0xFF48BB78) else Color(0xFFE5E7EB)
                ),
                elevation = CardDefaults.cardElevation(if (isIncome) 4.dp else 1.dp),
                onClick = { onTypeChange(true) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (isIncome) Color.White else Color(0xFF48BB78),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            languageViewModel.getTranslation("income"),
                            color = if (isIncome) Color.White else Color(0xFF48BB78),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Nút Chi tiêu - nhỏ hơn
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isIncome) Color(0xFFE91E63) else Color(0xFFF9FAFB)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (!isIncome) Color(0xFFE91E63) else Color(0xFFE5E7EB)
                ),
                elevation = CardDefaults.cardElevation(if (!isIncome) 4.dp else 1.dp),
                onClick = { onTypeChange(false) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (!isIncome) Color.White else Color(0xFFE91E63),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            languageViewModel.getTranslation("spending"),
                            color = if (!isIncome) Color.White else Color(0xFFE91E63),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountSectionCompact(
    amount: String,
    onAmountChange: (String) -> Unit,
    amountText: String,
    primaryColor: Color
) {
    Column {
        Text(
            amountText,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF4B5563),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "₫",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                TextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    placeholder = {
                        Text(
                            "0",
                            color = Color(0xFF9CA3AF),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color(0xFF1F2937),
                        unfocusedTextColor = Color(0xFF1F2937),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = primaryColor
                    )
                )
            }
        }
    }
}

@Composable
private fun CategorySectionCompact(
    categories: List<com.example.financeapp.viewmodel.transaction.Category>,
    selectedCategoryId: String,
    onCategorySelected: (com.example.financeapp.viewmodel.transaction.Category) -> Unit,
    onOtherCategoryClick: () -> Unit,
    categoryText: String,
    primaryColor: Color
) {
    Column {
        Text(
            categoryText,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF4B5563),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Hiển thị 3 danh mục đầu tiên + "Khác" trong grid 2x2
        val gridItems = if (categories.size >= 4) categories.take(4) else categories

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hàng đầu tiên
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gridItems.take(2).forEach { category ->
                    CategoryItemCompact(
                        category = category,
                        isSelected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category) },
                        modifier = Modifier.weight(1f),
                        primaryColor = primaryColor
                    )
                }
            }

            // Hàng thứ hai
            if (gridItems.size > 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems.drop(2).forEach { category ->
                        CategoryItemCompact(
                            category = category,
                            isSelected = selectedCategoryId == category.id,
                            onClick = { onCategorySelected(category) },
                            modifier = Modifier.weight(1f),
                            primaryColor = primaryColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItemCompact(
    category: com.example.financeapp.viewmodel.transaction.Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color
) {
    val categoryColor = parseColor(category.color)
    val backgroundColor = if (isSelected) primaryColor else Color(0xFFF9FAFB)
    val textColor = if (isSelected) Color.White else Color(0xFF374151)
    val iconColor = if (isSelected) Color.White else Color(0xFF374151)

    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 1.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) primaryColor else Color(0xFFE5E7EB)
        ),
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    category.icon,
                    fontSize = 20.sp,
                    color = iconColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    category.name,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun DateSectionCompact(
    date: String,
    dayOfWeek: String,
    onDateClick: () -> Unit,
    transactionDateText: String,
    primaryColor: Color
) {
    Column {
        Text(
            transactionDateText,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF4B5563),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDateClick() },
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
                            dayOfWeek,
                            color = Color(0xFF1F2937),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            date,
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

@Composable
private fun DescriptionSectionCompact(
    description: String,
    onDescriptionChange: (String) -> Unit,
    noteText: String,
    enterTransactionDescriptionText: String,
    primaryColor: Color
) {
    Column {
        Text(
            noteText,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF4B5563),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            TextField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = {
                    Text(
                        enterTransactionDescriptionText,
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = false,
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color(0xFF1F2937),
                    unfocusedTextColor = Color(0xFF1F2937),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = primaryColor
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )
        }
    }
}

@Composable
private fun SaveButtonCompact(
    isEnabled: Boolean,
    isIncome: Boolean,
    isEditing: Boolean,
    languageViewModel: LanguageViewModel,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEnabled) primaryColor else Color(0xFFD1D5DB),
            contentColor = if (isEnabled) Color.White else Color(0xFF9CA3AF)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isEditing) Icons.Default.Edit else Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isEditing) languageViewModel.getTranslation("update_transaction")
                else if (isIncome) languageViewModel.getTranslation("add_income_transaction")
                else languageViewModel.getTranslation("add_expense_transaction"),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun DeleteButtonCompact(
    languageViewModel: LanguageViewModel,
    primaryColor: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFFEF4444),
            containerColor = Color.Transparent
        ),
        border = BorderStroke(1.dp, Color(0xFFEF4444)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = languageViewModel.getTranslation("delete_transaction"),
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                languageViewModel.getTranslation("delete_transaction"),
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

// ============== DATE PICKER BOTTOM SHEET ==============
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerBottomSheet(
    initialDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color
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

    // Lấy chiều cao màn hình
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f) // Gần full màn hình
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Handle nhỏ
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

            // Header gọn
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Chọn ngày",
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

            // DatePicker chiếm nhiều không gian nhất
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 450.dp, max = 550.dp) // Chiều cao linh hoạt
                    .weight(1f, fill = false) // Chiếm không gian còn lại
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

            // Footer với ít padding hơn
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

                // Nút nhỏ hơn
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

// ============== HÀM UTILITY ==============

private fun getTodayDate(): String {
    val now = Calendar.getInstance().time
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return format.format(now)
}

private fun getTodayDayOfWeek(languageViewModel: LanguageViewModel): String {
    val days = listOf(
        languageViewModel.getTranslation("sunday"),
        languageViewModel.getTranslation("monday"),
        languageViewModel.getTranslation("tuesday"),
        languageViewModel.getTranslation("wednesday"),
        languageViewModel.getTranslation("thursday"),
        languageViewModel.getTranslation("friday"),
        languageViewModel.getTranslation("saturday")
    )
    val cal = Calendar.getInstance()
    return days[cal.get(Calendar.DAY_OF_WEEK) - 1]
}

private fun generateTransactionId(): String {
    return "TR_${System.currentTimeMillis()}"
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF667EEA)
    }
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