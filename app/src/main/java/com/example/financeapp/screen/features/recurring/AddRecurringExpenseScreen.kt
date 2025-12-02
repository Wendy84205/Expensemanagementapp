package com.example.financeapp.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.financeapp.viewmodel.Category
import com.example.financeapp.viewmodel.CategoryViewModel
import com.example.financeapp.model.RecurringExpense
import com.example.financeapp.model.RecurringFrequency
import com.example.financeapp.viewmodel.RecurringExpenseViewModel
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

    Scaffold(
        topBar = {
            EnhancedFormTopAppBar(
                title = if (existingExpense == null) "Thêm chi tiêu định kỳ" else "Chỉnh sửa chi tiêu",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { padding ->
        EnhancedRecurringExpenseFormContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            categories = subCategories,
            existingExpense = existingExpense,
            onSave = { expense ->
                if (existingExpense == null) {
                    recurringExpenseViewModel.addRecurringExpense(
                        title = expense.title,
                        amount = expense.amount,
                        category = expense.category,
                        categoryIcon = expense.categoryIcon,
                        categoryColor = expense.categoryColor,
                        wallet = expense.wallet,
                        description = expense.description,
                        frequency = expense.getFrequencyEnum(),
                        startDate = expense.startDate,
                        endDate = expense.endDate
                    )
                } else {
                    recurringExpenseViewModel.updateRecurringExpense(expense)
                }
                navController.popBackStack()
            },
            onCancel = {
                navController.popBackStack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedFormTopAppBar(
    title: String,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedRecurringExpenseFormContent(
    modifier: Modifier = Modifier,
    categories: List<Category>,
    existingExpense: RecurringExpense?,
    onSave: (RecurringExpense) -> Unit,
    onCancel: () -> Unit
) {
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

    val scrollState = rememberScrollState()
    val isFormValid = title.isNotBlank() &&
            amount.toDoubleOrNull() != null &&
            selectedCategory != null

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
    ) {
        // Header Section - Được cải thiện
        EnhancedFormHeader(
            title = if (existingExpense == null) "Thêm chi tiêu định kỳ" else "Chỉnh sửa chi tiêu",
            subtitle = if (existingExpense == null) "Thiết lập chi tiêu tự động định kỳ" else "Cập nhật thông tin chi tiêu định kỳ"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main Content trong Card - Bố cục đẹp hơn
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Tiêu đề và số tiền - Bố cục cân đối hơn
                EnhancedTitleAmountSection(
                    title = title,
                    onTitleChange = { title = it },
                    amount = amount,
                    onAmountChange = { amount = it }
                )

                // Danh mục - Thiết kế card selection
                EnhancedCategorySelectionSection(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )

                // Tần suất - Dạng card đẹp hơn
                EnhancedFrequencySelectionSection(
                    selectedFrequency = frequency,
                    onFrequencySelected = { frequency = it }
                )

                // Ví - Thêm section chọn ví
                EnhancedWalletSection(
                    selectedWallet = wallet,
                    onWalletChange = { wallet = it }
                )

                // Ngày tháng - Bố cục rõ ràng hơn
                EnhancedDateSelectionSection(
                    startDate = startDate,
                    endDate = endDate,
                    onStartDateChange = { startDate = it },
                    onEndDateChange = { endDate = it }
                )

                // Ghi chú - Cải thiện chiều cao và spacing
                EnhancedDescriptionSection(
                    description = description,
                    onDescriptionChange = { description = it }
                )

                // Status indicator - Hiển thị trạng thái form
                if (isFormValid) {
                    FormStatusIndicator(
                        message = if (existingExpense == null) "Sẵn sàng thêm chi tiêu định kỳ" else "Sẵn sàng cập nhật chi tiêu",
                        isSuccess = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons - Căn giữa và padding hợp lý
        EnhancedFormActionButtons(
            onCancel = onCancel,
            onSave = {
                val newExpense = RecurringExpense.fromEnum(
                    id = existingExpense?.id ?: UUID.randomUUID().toString(),
                    title = title,
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    category = selectedCategory?.name ?: "",
                    categoryIcon = selectedCategory?.icon ?: "💰",
                    categoryColor = selectedCategory?.color ?: "#0F4C75",
                    wallet = wallet,
                    description = description.ifBlank { null },
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate.ifBlank { null },
                    nextOccurrence = existingExpense?.nextOccurrence ?: startDate
                )
                onSave(newExpense)
            },
            isSaveEnabled = isFormValid,
            saveButtonText = if (existingExpense == null) "Thêm chi tiêu" else "Cập nhật"
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EnhancedFormHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun EnhancedTitleAmountSection(
    title: String,
    onTitleChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tiêu đề - Full width
        Column {
            Text(
                "Tiêu đề chi tiêu",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 50) onTitleChange(it) },
                placeholder = { Text("Ví dụ: Tiền nhà, Internet, Bảo hiểm...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Title,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (title.isNotBlank()) {
                        Text(
                            "${title.length}/50",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        // Số tiền - Full width
        Column {
            Text(
                "Số tiền",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
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
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        Icons.Outlined.AttachMoney,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    Text(
                        "VND",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            )
        }
    }
}

@Composable
private fun EnhancedCategorySelectionSection(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (com.example.financeapp.viewmodel.Category) -> Unit
) {
    Column {
        Text(
            "Danh mục",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (categories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    EnhancedCategoryChip(
                        category = category,
                        isSelected = selectedCategory?.id == category.id,
                        onClick = { onCategorySelected(category) }
                    )
                }
            }

            // Selected category preview
            selectedCategory?.let { category ->
                Spacer(modifier = Modifier.height(12.dp))
                SelectedCategoryPreview(category = category)
            }
        } else {
            EmptyCategoryState()
        }
    }
}

@Composable
private fun EnhancedCategoryChip(
    category: com.example.financeapp.viewmodel.Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                category.icon,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                category.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun SelectedCategoryPreview(category: com.example.financeapp.viewmodel.Category) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        parseColor(category.color).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Đã chọn: ${category.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Icon: ${category.icon}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EnhancedFrequencySelectionSection(
    selectedFrequency: RecurringFrequency,
    onFrequencySelected: (RecurringFrequency) -> Unit
) {
    Column {
        Text(
            "Tần suất",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(RecurringFrequency.entries.toList()) { freq ->
                FrequencyCard(
                    frequency = freq,
                    isSelected = selectedFrequency == freq,
                    onClick = { onFrequencySelected(freq) }
                )
            }
        }
    }
}

@Composable
private fun FrequencyCard(
    frequency: RecurringFrequency,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                getFrequencyIcon(frequency),
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                frequency.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun getFrequencyIcon(frequency: RecurringFrequency): String {
    return when (frequency) {
        RecurringFrequency.DAILY -> "📅"
        RecurringFrequency.WEEKLY -> "📆"
        RecurringFrequency.MONTHLY -> "🗓️"
        RecurringFrequency.YEARLY -> "🎉"
        RecurringFrequency.QUARTERLY -> "📊" // Thêm branch QUARTERLY
    }
}

@Composable
private fun EnhancedWalletSection(
    selectedWallet: String,
    onWalletChange: (String) -> Unit
) {
    Column {
        Text(
            "Ví",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = selectedWallet,
            onValueChange = onWalletChange,
            placeholder = { Text("Ví chính") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@Composable
private fun EnhancedDateSelectionSection(
    startDate: String,
    endDate: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit
) {
    Column {
        Text(
            "Thời gian",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ngày bắt đầu
            Column {
                Text(
                    "Ngày bắt đầu",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                EnhancedDateChip(
                    date = startDate,
                    placeholder = "Chọn ngày bắt đầu",
                    onClick = { /* TODO: Implement date picker */ }
                )
            }

            // Ngày kết thúc
            Column {
                Text(
                    "Ngày kết thúc (tùy chọn)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                EnhancedDateChip(
                    date = endDate,
                    placeholder = "Không có ngày kết thúc",
                    onClick = { /* TODO: Implement date picker */ }
                )
            }
        }
    }
}

@Composable
private fun EnhancedDateChip(
    date: String,
    placeholder: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (date.isNotBlank()) date else placeholder,
                    fontSize = 15.sp,
                    color = if (date.isNotBlank()) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (date.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                )
                if (date.isNotBlank()) {
                    Text(
                        "Đã chọn",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Icon(
                Icons.Outlined.CalendarToday,
                contentDescription = "Chọn ngày",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EnhancedDescriptionSection(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Column {
        Text(
            "Ghi chú",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= 200) onDescriptionChange(it) },
            placeholder = { Text("Thêm ghi chú cho chi tiêu định kỳ này...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            singleLine = false,
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(
                    Icons.Outlined.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (description.isNotBlank()) {
                    Text(
                        "${description.length}/200",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun FormStatusIndicator(
    message: String,
    isSuccess: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isSuccess) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSuccess) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EnhancedFormActionButtons(
    onCancel: () -> Unit,
    onSave: () -> Unit,
    isSaveEnabled: Boolean,
    saveButtonText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text("Hủy", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            enabled = isSaveEnabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        ) {
            Text(saveButtonText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun EmptyCategoryState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.Category,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Chưa có danh mục nào",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Vui lòng tạo danh mục trước",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
        MaterialTheme.colorScheme.primary
    }
}

// Helper function để lấy ngày hiện tại
private fun getTodayDate(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date())
}