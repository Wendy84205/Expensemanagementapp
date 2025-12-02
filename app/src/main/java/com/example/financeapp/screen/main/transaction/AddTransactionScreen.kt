@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.financeapp.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.data.Transaction
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.LanguageViewModel
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
    categoryViewModel: com.example.financeapp.viewmodel.CategoryViewModel = viewModel(),
    existingTransaction: Transaction? = null,
    onDelete: (() -> Unit)? = null
) {
    val languageViewModel = _root_ide_package_.com.example.financeapp.LocalLanguageViewModel.current
    
    var amount by remember { mutableStateOf(existingTransaction?.amount?.toString() ?: "") }
    var categoryId by remember { mutableStateOf(existingTransaction?.category ?: "") }
    var selectedParentCategoryId by remember { mutableStateOf<String?>(null) }
    var isIncome by remember { mutableStateOf(existingTransaction?.isIncome ?: false) }
    var selectedWallet by remember { mutableStateOf(existingTransaction?.wallet ?: "") }
    var description by remember { mutableStateOf(existingTransaction?.description ?: "") }
    var transactionDate by remember { mutableStateOf(existingTransaction?.date ?: getTodayDate()) }
    var transactionDayOfWeek by remember { mutableStateOf(existingTransaction?.dayOfWeek ?: getTodayDayOfWeek(languageViewModel)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var walletExpanded by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val transactionType = if (isIncome) "income" else "expense"
    val selectableCategoriesMap by categoryViewModel.selectableCategories.collectAsState()
    val selectableCategories = remember(selectableCategoriesMap, transactionType) {
        selectableCategoriesMap[transactionType] ?: emptyList()
    }
    
    // ✅ Lấy category đã chọn từ CategoryScreen (nếu có)
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("selectedCategoryId", null)?.collect { selectedId ->
            selectedId?.let {
                // ✅ Khi chọn category từ CategorySelectionScreen, tìm category và set parentCategoryId để hiển thị tất cả danh mục con
                val selectedCategory = selectableCategories.find { it.id == selectedId }
                if (selectedCategory != null) {
                    categoryId = selectedId
                    // ✅ Nếu category có parentCategoryId, set nó để hiển thị tất cả danh mục con thay thế 3 danh mục đầu tiên
                    if (selectedCategory.parentCategoryId != null) {
                        selectedParentCategoryId = selectedCategory.parentCategoryId
                    } else {
                        selectedParentCategoryId = null
                    }
                } else {
                    categoryId = selectedId
                    selectedParentCategoryId = null
                }
                savedStateHandle.remove<String>("selectedCategoryId")
            }
        }
        // ✅ Đảm bảo isIncome được giữ nguyên khi quay lại từ CategorySelectionScreen
        savedStateHandle?.getStateFlow<Boolean?>("isIncome", null)?.collect { savedIsIncome ->
            savedIsIncome?.let {
                isIncome = it
                savedStateHandle.remove<Boolean>("isIncome")
            }
        }
    }
    
    // ✅ Lấy danh mục con đầu tiên từ mỗi danh mục cha
    val categories by categoryViewModel.categories.collectAsState()
    val mainCategories = remember(categories, transactionType) {
        categoryViewModel.getMainCategories(transactionType).filter { it.name != "Khác" }
    }
    
    // ✅ Hiển thị danh mục: nếu đã chọn parent category thì hiển thị tất cả danh mục con, nếu không thì hiển thị 3 danh mục đầu tiên
    val displayCategories = remember(mainCategories, categories, transactionType, selectedParentCategoryId) {
        if (selectedParentCategoryId != null) {
            // ✅ Nếu đã chọn parent category, hiển thị TẤT CẢ danh mục con + "Khác"
            val allSubCategories = categoryViewModel.getSubCategories(selectedParentCategoryId!!).toMutableList()
            // ✅ Thêm "Khác" ở cuối
            allSubCategories.add(
                _root_ide_package_.com.example.financeapp.viewmodel.Category(
                    "other",
                    "Khác",
                    transactionType,
                    false,
                    null,
                    "📁",
                    "#9F7AEA"
                )
            )
            allSubCategories
        } else {
            // ✅ Nếu chưa chọn, hiển thị danh mục con đầu tiên từ mỗi danh mục cha
            val subCategories = mutableListOf<com.example.financeapp.viewmodel.Category>()
            mainCategories.forEach { mainCategory ->
                val firstSubCategory = categoryViewModel.getSubCategories(mainCategory.id).firstOrNull()
                if (firstSubCategory != null) {
                    subCategories.add(firstSubCategory)
                }
            }
            // ✅ Chỉ lấy 3 danh mục đầu tiên
            val limitedCategories = subCategories.take(3).toMutableList()
            // ✅ Thêm "Khác" ở cuối
            limitedCategories.add(
                _root_ide_package_.com.example.financeapp.viewmodel.Category(
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
    }

    val selectedCategoryInfo = selectableCategories.find { it.id == categoryId } ?: displayCategories.find { it.id == categoryId }

    val isSaveEnabled = amount.isNotBlank() && categoryId.isNotBlank() && selectedWallet.isNotBlank()

    // 🎨 Màu sắc chủ đạo
    val primaryColor = if (isIncome) Color(0xFF48BB78) else Color(0xFFE91E63)
    val backgroundColor = Color(0xFFFDF6F9)
    val cardColor = Color.White
    val textColor = Color(0xFF2D3748)
    val subtitleColor = Color(0xFF718096)

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            if (isIncome) Color(0xFF48BB78) else Color(0xFFE91E63),
            if (isIncome) Color(0xFF38A169) else Color(0xFFC2185B)
        )
    )
    val todayDayOfWeek = remember { getTodayDayOfWeek(languageViewModel) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        languageViewModel.getTranslation("record_transaction"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = languageViewModel.getTranslation("back"),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Loại giao dịch (Thu/Chi)
                    TransactionTypeSection(
                        isIncome = isIncome,
                        onTypeChange = { newIsIncome ->
                            isIncome = newIsIncome
                            // ✅ Reset selectedParentCategoryId và categoryId khi đổi loại giao dịch
                            selectedParentCategoryId = null
                            categoryId = ""
                        },
                        languageViewModel = languageViewModel,
                        primaryColor = primaryColor
                    )

                    // Nhập số tiền
                    AmountSection(
                        amount = amount,
                        onAmountChange = { newValue ->
                            if (newValue.matches(Regex("^\\d*\\.?\\d*$")) || newValue.isEmpty()) amount = newValue
                        },
                        amountText = languageViewModel.getTranslation("amount"),
                        primaryColor = primaryColor
                    )

                    // Danh mục - UI cải thiện
                    CategorySectionImproved(
                        categories = displayCategories,
                        selectedCategoryId = categoryId,
                        onCategorySelected = { selected ->
                            if (selected.id == "other") {
                                // ✅ Khi chọn "Khác", navigate đến CategorySelectionScreen
                                // ✅ Reset selectedParentCategoryId để khi quay lại sẽ hiển thị UI ban đầu
                                selectedParentCategoryId = null
                                // ✅ Lưu isIncome vào savedStateHandle để giữ nguyên khi quay lại
                                navController.currentBackStackEntry?.savedStateHandle?.set("isIncome", isIncome)
                                navController.navigate("categories?transactionType=$transactionType&returnTo=add_transaction")
                            } else {
                                categoryId = selected.id
                                // ✅ Khi chọn một trong 3 danh mục đầu tiên, set selectedParentCategoryId để hiển thị tất cả danh mục con
                                if (selected.parentCategoryId != null && selectedParentCategoryId == null) {
                                    // Chỉ set khi chọn một trong 3 danh mục đầu tiên (chưa có selectedParentCategoryId)
                                    selectedParentCategoryId = selected.parentCategoryId
                                } else if (selectedParentCategoryId != null) {
                                    // ✅ Nếu đã có selectedParentCategoryId và chọn một danh mục con, reset để quay về 3 danh mục đầu tiên
                                    selectedParentCategoryId = null
                                }
                            }
                        },
                        onOtherCategoryClick = { 
                            // ✅ Navigate đến CategorySelectionScreen để chọn danh mục
                            // ✅ Reset selectedParentCategoryId để khi quay lại sẽ hiển thị UI ban đầu
                            selectedParentCategoryId = null
                            // ✅ Lưu isIncome vào savedStateHandle để giữ nguyên khi quay lại
                            navController.currentBackStackEntry?.savedStateHandle?.set("isIncome", isIncome)
                            navController.navigate("categories?transactionType=$transactionType&returnTo=add_transaction")
                        },
                        categoryText = languageViewModel.getTranslation("category"),
                        primaryColor = primaryColor
                    )

                    // Ngày - có thể chọn ngày
                    DateSectionWithPicker(
                        date = transactionDate,
                        dayOfWeek = transactionDayOfWeek,
                        onDateClick = { showDatePicker = true },
                        transactionDateText = languageViewModel.getTranslation("transaction_date"),
                        todayText = languageViewModel.getTranslation("today"),
                        dateText = languageViewModel.getTranslation("date"),
                        primaryColor = primaryColor
                    )

                    // Ghi chú
                    DescriptionSection(
                        description = description,
                        onDescriptionChange = { description = it },
                        noteText = languageViewModel.getTranslation("note"),
                        enterTransactionDescriptionText = languageViewModel.getTranslation("enter_transaction_description"),
                        primaryColor = primaryColor
                    )

                    Spacer(Modifier.height(8.dp))

                    SaveTransactionButton(
                        isEnabled = isSaveEnabled,
                        isIncome = isIncome,
                        isEditing = existingTransaction != null,
                        languageViewModel = languageViewModel,
                        primaryColor = primaryColor
                    ) {
                        val transaction = Transaction(
                            id = existingTransaction?.id ?: generateTransactionId(),
                            date = transactionDate,
                            dayOfWeek = transactionDayOfWeek,
                            category = categoryId,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            isIncome = isIncome,
                            group = if (isIncome) languageViewModel.getTranslation("income") else languageViewModel.getTranslation(
                                "spending"
                            ),
                            wallet = selectedWallet,
                            description = description,
                            categoryIcon = selectedCategoryInfo?.icon,
                            categoryId = selectedCategoryInfo?.id ?: "",
                            categoryColor = selectedCategoryInfo?.color ?: "#667EEA",
                            title = description.ifBlank { selectedCategoryInfo?.name ?: categoryId }
                        )

                        // ✅ SỬA LỖI: Sử dụng onSave callback thay vì gọi trực tiếp ViewModel
                        // onSave sẽ xử lý add/update và navigation trong NavGraph
                        onSave(transaction)
                    }

                    // Hiển thị cảnh báo
                    val warning by transactionViewModel.warningMessage.collectAsState()
                    warning?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                            border = BorderStroke(1.dp, Color(0xFFFFEEBA))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA500),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = it,
                                    color = Color(0xFF856404),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Nút xóa (nếu đang chỉnh sửa)
                    if (existingTransaction != null && onDelete != null) {
                        DeleteTransactionButton(
                            languageViewModel = languageViewModel,
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }
            }
        }
    }

    // ✅ DatePicker Dialog
    if (showDatePicker) {
        DatePickerDialog(
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

    // Dialog xác nhận xóa
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    languageViewModel.getTranslation("delete_transaction_dialog"),
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            },
            text = {
                Text(
                    languageViewModel.getTranslation("delete_transaction_description"),
                    color = subtitleColor
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
                        languageViewModel.getTranslation("delete_action"),
                        color = Color(0xFFF56565),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        languageViewModel.getTranslation("cancel"),
                        color = primaryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }
}

@Composable
private fun TransactionTypeSection(
    isIncome: Boolean,
    onTypeChange: (Boolean) -> Unit,
    languageViewModel: LanguageViewModel,
    primaryColor: Color
) {
    Column {
        Text(
            "Loại giao dịch",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF718096),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nút Thu nhập
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isIncome) Color(0xFF48BB78) else Color(0xFFF7FAFC)
                ),
                border = BorderStroke(
                    2.dp,
                    if (isIncome) Color(0xFF48BB78) else Color(0xFFE2E8F0)
                ),
                elevation = CardDefaults.cardElevation(if (isIncome) 4.dp else 2.dp),
                onClick = { onTypeChange(true) }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (isIncome) Color.White else Color(0xFF48BB78),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        languageViewModel.getTranslation("income"),
                        color = if (isIncome) Color.White else Color(0xFF48BB78),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Nút Chi tiêu
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isIncome) Color(0xFFE91E63) else Color(0xFFF7FAFC)
                ),
                border = BorderStroke(
                    2.dp,
                    if (!isIncome) Color(0xFFE91E63) else Color(0xFFE2E8F0)
                ),
                elevation = CardDefaults.cardElevation(if (!isIncome) 4.dp else 2.dp),
                onClick = { onTypeChange(false) }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (!isIncome) Color.White else Color(0xFFE91E63),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        languageViewModel.getTranslation("spending"),
                        color = if (!isIncome) Color.White else Color(0xFFE91E63),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// COMPOSABLE: Hiển thị danh mục dưới dạng grid đẹp hơn
@Composable
private fun CategorySectionImproved(
    categories: List<com.example.financeapp.viewmodel.Category>,
    selectedCategoryId: String,
    onCategorySelected: (com.example.financeapp.viewmodel.Category) -> Unit,
    onOtherCategoryClick: () -> Unit,
    categoryText: String,
    primaryColor: Color
) {
    Column {
        Text(
            categoryText,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF718096),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // ✅ Grid layout: nếu có nhiều danh mục (>4) thì dùng grid 4 cột, nếu không thì dùng row
        if (categories.size > 4) {
            // Grid layout với 4 cột giống CategoryScreen
            val rows = categories.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCategories.forEach { category ->
                            CategoryButtonImproved(
                                category = category,
                                isSelected = selectedCategoryId == category.id,
                                onClick = { 
                                    // ✅ Tất cả category (kể cả "Khác") đều gọi onCategorySelected
                                    // Logic xử lý "Khác" đã được xử lý trong onCategorySelected callback
                                    onCategorySelected(category)
                                },
                                modifier = Modifier.weight(1f),
                                primaryColor = primaryColor
                            )
                        }
                        // Thêm spacing để căn đều
                        if (rowCategories.size < 4) {
                            repeat(4 - rowCategories.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            // Row layout cho ít danh mục
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categories.forEach { category ->
                    CategoryButtonImproved(
                        category = category,
                        isSelected = selectedCategoryId == category.id,
                        onClick = { 
                            onCategorySelected(category)
                        },
                        modifier = Modifier.weight(1f),
                        primaryColor = primaryColor
                    )
                }
            }
        }
    }
}

// COMPOSABLE: Nút danh mục - UI nhỏ gọn như ban đầu
@Composable
private fun CategoryButtonImproved(
    category: com.example.financeapp.viewmodel.Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color
) {
    val categoryColor = parseColor(category.color)

    Card(
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) categoryColor.copy(alpha = 0.15f) else Color(0xFFF7FAFC)
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 2.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 0.dp,
            color = if (isSelected) categoryColor else Color.Transparent
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(category.icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                category.name,
                color = if (isSelected) categoryColor else Color(0xFF718096),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

// COMPOSABLE: Nhập số tiền
@Composable
private fun AmountSection(
    amount: String,
    onAmountChange: (String) -> Unit,
    amountText: String,
    primaryColor: Color
) {
    Column {
        Text(
            amountText,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF718096),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            placeholder = { Text("0", color = Color(0xFFCBD5E0)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748)
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedTextColor = Color(0xFF2D3748),
                unfocusedTextColor = Color(0xFF2D3748),
                cursorColor = primaryColor
            ),
            leadingIcon = {
                Text(
                    "₫",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
        )
    }
}

// COMPOSABLE: Ngày - có thể chọn ngày với UI đẹp hơn
@Composable
private fun DateSectionWithPicker(
    date: String,
    dayOfWeek: String,
    onDateClick: () -> Unit,
    transactionDateText: String,
    todayText: String,
    dateText: String,
    primaryColor: Color
) {
    Column {
        Text(
            transactionDateText,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF718096),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDateClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                primaryColor.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = dateText,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            dayOfWeek,
                            color = Color(0xFF2D3748),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            date,
                            color = Color(0xFF718096),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Chọn ngày",
                    tint = Color(0xFFCBD5E0),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
// COMPOSABLE: Ghi chú
@Composable
private fun DescriptionSection(
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
            color = Color(0xFF718096),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = { Text(enterTransactionDescriptionText, color = Color(0xFFCBD5E0)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(16.dp),
            singleLine = false,
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedTextColor = Color(0xFF2D3748),
                unfocusedTextColor = Color(0xFF2D3748),
                cursorColor = primaryColor
            )
        )
    }
}

// COMPOSABLE: Nút lưu
@Composable
private fun SaveTransactionButton(
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
            containerColor = if (isEnabled) primaryColor else Color(0xFFE2E8F0),
            contentColor = if (isEnabled) Color.White else Color(0xFFA0AEC0)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                if (isEditing) Icons.Default.Edit else Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isEditing) languageViewModel.getTranslation("update_transaction")
                else if (isIncome) languageViewModel.getTranslation("add_income_transaction")
                else languageViewModel.getTranslation("add_expense_transaction"),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// COMPOSABLE: Nút xóa
@Composable
private fun DeleteTransactionButton(
    languageViewModel: LanguageViewModel,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFFF56565),
            containerColor = Color.Transparent
        ),
        border = BorderStroke(1.dp, Color(0xFFF56565)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = languageViewModel.getTranslation("delete_transaction"),
                tint = Color(0xFFF56565),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                languageViewModel.getTranslation("delete_transaction"),
                color = Color(0xFFF56565),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// BottomSheet chọn danh mục
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionBottomSheet(
    categories: List<com.example.financeapp.viewmodel.Category>,
    onCategorySelected: (com.example.financeapp.viewmodel.Category) -> Unit,
    onDismiss: () -> Unit,
    languageViewModel: LanguageViewModel,
    categoryViewModel: com.example.financeapp.viewmodel.CategoryViewModel,
    transactionType: String,
    primaryColor: Color
) {
    val sheetState = rememberModalBottomSheetState()
    var searchText by remember { mutableStateOf("") }

    // Lấy danh mục lớn để hiển thị theo nhóm
    val mainCategories = remember(categoryViewModel.categories, transactionType) {
        categoryViewModel.categories.value.filter {
            it.isMainCategory && it.type == transactionType
        }
    }

    // Tạo danh sách danh mục con theo nhóm
    val categoryGroups = remember(mainCategories, categories) {
        mainCategories.map { mainCategory ->
            val subCategories = categories.filter { it.parentCategoryId == mainCategory.id }
            _root_ide_package_.com.example.financeapp.CategoryGroupData(
                mainCategory.name,
                subCategories,
                getGroupColor(mainCategory.name)
            )
        }.filter { it.subCategories.isNotEmpty() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFDF6F9),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Chọn danh mục",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )
                    Text(
                        "Chọn nhóm cho danh mục mới",
                        fontSize = 14.sp,
                        color = Color(0xFF718096)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Thanh tìm kiếm
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Tìm kiếm danh mục...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF64748B)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF2D3748),
                    unfocusedTextColor = Color(0xFF2D3748)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Số lượng kết quả
            Text(
                "Tìm thấy ${categories.size} danh mục",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Danh sách danh mục
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                if (categories.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "📭",
                                    fontSize = 48.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    "Không tìm thấy danh mục",
                                    color = Color(0xFF64748B),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Thử tìm kiếm với từ khóa khác",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    items(categories) { category ->
                        CategorySelectionItem(
                            category = category,
                            onClick = { onCategorySelected(category) },
                            primaryColor = primaryColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// COMPOSABLE: Item chọn danh mục
@Composable
private fun CategorySelectionItem(
    category: com.example.financeapp.viewmodel.Category,
    onClick: () -> Unit,
    primaryColor: Color
) {
    val categoryColor = parseColor(category.color)

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(categoryColor.copy(alpha = 0.2f), CircleShape),
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
                    color = Color(0xFF2D3748)
                )
                Text(
                    text = if (category.isMainCategory) "Danh mục chính" else "Danh mục con",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Mũi tên
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFCBD5E1)
            )
        }
    }
}


// Hàm lấy màu cho từng nhóm danh mục
private fun getGroupColor(groupName: String): Color {
    return when {
        groupName.contains("sinh hoạt", ignoreCase = true) -> Color(0xFFFFCC80)
        groupName.contains("phát sinh", ignoreCase = true) -> Color(0xFFFFF59D)
        groupName.contains("cố định", ignoreCase = true) -> Color(0xFFBBDEFB)
        groupName.contains("đầu tư", ignoreCase = true) -> Color(0xFFC8E6C9)
        groupName.contains("lương", ignoreCase = true) -> Color(0xFFB2DFDB)
        groupName.contains("thu nhập", ignoreCase = true) -> Color(0xFFD1C4E9)
        else -> Color(0xFFE1BEE7)
    }
}

// Hàm utility
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

// ✅ Helper functions cho Date
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

// ✅ DatePicker Dialog - Full màn hình với background Navy đẹp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
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
    
    // Full screen với background Navy
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(_root_ide_package_.com.example.financeapp.AppColorConstants.Navy)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            // Header với background Navy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(_root_ide_package_.com.example.financeapp.AppColorConstants.Navy)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Chọn ngày giao dịch",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Chọn ngày, tháng, năm cho giao dịch",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // DatePicker với background trắng, rộng hết màn hình
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 0.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // DatePicker với padding để không bị khuất
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors(
                            selectedDayContainerColor = primaryColor,
                            todayDateBorderColor = primaryColor,
                            selectedDayContentColor = Color.White,
                            todayContentColor = primaryColor,
                            containerColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }
            
            // Buttons với background trắng
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF718096)
                        )
                    ) {
                        Text(
                            "Hủy",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                onDateSelected(Date(it))
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            "Chọn",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}