package com.example.financeapp.screen.main.budget

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.LocalLanguageViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import com.example.financeapp.data.models.Budget
import com.example.financeapp.data.models.BudgetPeriodType
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.data.models.calculateBudgetEndDate
import com.example.financeapp.data.models.getDisplayName
import com.example.financeapp.screen.features.formatCurrency
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    navController: NavController,
    budgetViewModel: BudgetViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    existingBudget: Budget? = null
) {
    val languageViewModel = LocalLanguageViewModel.current
    val categories by categoryViewModel.categories.collectAsState()
    val subCategories = remember(categories) {
        categories.filter { !it.isMainCategory }
    }

    // 🎨 Màu sắc
    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White
    val textColor = Color(0xFF333333)
    val subtitleColor = Color(0xFF666666)

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

    // State cho DropdownMenu
    var expanded by remember { mutableStateOf(false) }

    val isFormValid = selectedCategory != null && amount.toDoubleOrNull() != null

    // Format date
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yy", Locale("vi"))
    val currentDate = LocalDate.now().format(dateFormatter)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (existingBudget == null) languageViewModel.getTranslation("add_budget")
                        else languageViewModel.getTranslation("edit_budget"),
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = languageViewModel.getTranslation("back"),
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
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
            // Budget Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(120.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = primaryColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            languageViewModel.getTranslation("budget"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            currentDate,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        formatCurrency(amount.toDoubleOrNull() ?: 0.0),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    selectedCategory?.let { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    category.icon,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                category.name,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Form
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
                        languageViewModel.getTranslation("setup_budget"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    // Danh mục với DropdownMenu
                    Column {
                        Text(
                            languageViewModel.getTranslation("category"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Custom ExposedDropdownMenuBox
                            OutlinedTextField(
                                value = selectedCategory?.name ?: languageViewModel.getTranslation("select_category"),
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = if (selectedCategory != null) Color.Black else Color(0xFF888888),
                                    cursorColor = primaryColor
                                ),
                                leadingIcon = {
                                    selectedCategory?.let {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    parseColor(it.color).copy(alpha = 0.1f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                selectedCategory!!.icon,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = languageViewModel.getTranslation("select_category"),
                                        tint = Color(0xFF666666),
                                        modifier = Modifier.clickable { expanded = !expanded }
                                    )
                                }
                            )

                            // DropdownMenu
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                            ) {
                                if (subCategories.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(languageViewModel.getTranslation("no_categories")) },
                                        onClick = { expanded = false }
                                    )
                                } else {
                                    subCategories.forEach { category ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .background(
                                                                parseColor(category.color).copy(alpha = 0.1f),
                                                                CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            category.icon,
                                                            fontSize = 14.sp
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        category.name,
                                                        fontSize = 14.sp,
                                                        fontWeight = if (selectedCategory?.id == category.id) FontWeight.Medium else FontWeight.Normal,
                                                        color = if (selectedCategory?.id == category.id) primaryColor else textColor
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedCategory = category
                                                expanded = false
                                            },
                                            modifier = Modifier.background(
                                                if (selectedCategory?.id == category.id) primaryColor.copy(alpha = 0.05f) else Color.Transparent
                                            )
                                        )
                                    }
                                }
                            }
                        }
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
                            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0", color = Color(0xFF888888)) },
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFDDDDDD),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                cursorColor = primaryColor
                            ),
                            singleLine = true,
                            trailingIcon = {
                                Text(
                                    languageViewModel.getTranslation("currency_vnd"),
                                    color = Color(0xFF666666),
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }

                    // Chu kỳ
                    Column {
                        Text(
                            languageViewModel.getTranslation("period"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PeriodSelection(
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = { selectedPeriod = it },
                            primaryColor = primaryColor,
                            languageViewModel = languageViewModel
                        )
                    }

                    // Ghi chú
                    Column {
                        Text(
                            languageViewModel.getTranslation("note"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { if (it.length <= 200) note = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            placeholder = {
                                Text(
                                    languageViewModel.getTranslation("add_note_placeholder"),
                                    color = Color(0xFF888888)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFDDDDDD),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                cursorColor = primaryColor
                            ),
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                }
            }

            // Nút lưu
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
                    .padding(16.dp)
                    .height(50.dp),
                enabled = isFormValid,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = Color(0xFFCCCCCC)
                )
            ) {
                Text(
                    if (existingBudget == null) languageViewModel.getTranslation("add_budget_button").uppercase()
                    else languageViewModel.getTranslation("update").uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PeriodSelection(
    selectedPeriod: BudgetPeriodType,
    onPeriodSelected: (BudgetPeriodType) -> Unit,
    primaryColor: Color,
    languageViewModel: LanguageViewModel
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        BudgetPeriodType.values().forEach { period ->
            PeriodChip(
                period = period,
                isSelected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                primaryColor = primaryColor,
                languageViewModel = languageViewModel
            )
        }
    }
}

@Composable
private fun PeriodChip(
    period: BudgetPeriodType,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    languageViewModel: LanguageViewModel
) {
    val backgroundColor = if (isSelected) primaryColor else Color.Transparent
    val textColor = if (isSelected) Color.White else Color(0xFF666666)

    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) primaryColor else Color(0xFFDDDDDD)
        ),
        modifier = Modifier.defaultMinSize(minWidth = 1.dp)
    ) {
        Text(
            period.getDisplayName(languageViewModel),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// Helper function để parse color
private fun parseColor(colorString: String): Color {
    return try {
        val color = colorString.toColorInt()
        Color(color)
    } catch (e: Exception) {
        Color(0xFF2196F3) // Fallback to primary color
    }
}