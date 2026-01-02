package com.example.financeapp.screen.main.budget

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.data.models.Budget
import com.example.financeapp.data.models.BudgetPeriodType
import com.example.financeapp.data.models.calculateBudgetEndDate
import com.example.financeapp.screen.features.formatCurrency
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

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

    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)

    var selectedCategory by remember {
        mutableStateOf(
            existingBudget?.let {
                categories.find { cat -> cat.id == it.categoryId }
            } ?: if (subCategories.isNotEmpty()) subCategories[0] else null
        )
    }
    var amount by remember { mutableStateOf(existingBudget?.amount?.toString() ?: "") }
    var selectedPeriod by remember { mutableStateOf(existingBudget?.periodType ?: BudgetPeriodType.MONTH) }
    var note by remember { mutableStateOf(existingBudget?.note ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yy", Locale("vi"))
    val currentDate = LocalDate.now().format(dateFormatter)

    fun validateForm(): Boolean {
        if (selectedCategory == null) {
            errorMessage = languageViewModel.getTranslation("select_category_required")
            showError = true
            return false
        }

        val amountValue = amount.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            errorMessage = languageViewModel.getTranslation("amount_must_be_positive")
            showError = true
            return false
        }

        val existingActiveBudget = budgetViewModel.getBudgetForCategory(selectedCategory!!.id)
        if (existingActiveBudget != null && existingBudget?.id != existingActiveBudget.id) {
            errorMessage = languageViewModel.getTranslation("budget_already_exists")
            showError = true
            return false
        }

        return true
    }

    fun saveBudget() {
        if (!validateForm()) return

        val budgetAmount = amount.toDoubleOrNull() ?: 0.0
        val startDate = existingBudget?.startDate ?: LocalDate.now()

        val newBudget = if (existingBudget == null) {
            Budget(
                id = UUID.randomUUID().toString(),
                categoryId = selectedCategory!!.id,
                amount = budgetAmount,
                periodType = selectedPeriod,
                startDate = startDate,
                endDate = calculateBudgetEndDate(startDate, selectedPeriod),
                note = note.ifBlank { null },
                spentAmount = 0.0,
                isActive = true
            )
        } else {
            existingBudget.copy(
                categoryId = selectedCategory!!.id,
                amount = budgetAmount,
                periodType = selectedPeriod,
                note = note.ifBlank { null },
                lastModified = System.currentTimeMillis()
            )
        }

        if (existingBudget == null) {
            budgetViewModel.addBudget(newBudget)
        } else {
            budgetViewModel.updateFullBudget(newBudget)
        }
        navController.popBackStack()
    }

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
            if (showError) {
                ErrorBanner(
                    message = errorMessage,
                    onDismiss = { showError = false }
                )
            }

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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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

                    CategorySelector(
                        selectedCategory = selectedCategory,
                        subCategories = subCategories,
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        onCategorySelected = { selectedCategory = it },
                        languageViewModel = languageViewModel
                    )

                    AmountInput(
                        amount = amount,
                        onAmountChanged = { amount = it },
                        languageViewModel = languageViewModel
                    )

                    PeriodSelector(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { selectedPeriod = it },
                        languageViewModel = languageViewModel
                    )

                    NoteInput(
                        note = note,
                        onNoteChanged = { note = it },
                        languageViewModel = languageViewModel
                    )
                }
            }

            Button(
                onClick = { saveBudget() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                enabled = selectedCategory != null && amount.toDoubleOrNull() != null,
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
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        border = BorderStroke(1.dp, Color(0xFFF44336).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                message,
                fontSize = 14.sp,
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: Category?,
    subCategories: List<Category>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCategorySelected: (Category) -> Unit,
    languageViewModel: LanguageViewModel
) {
    val primaryColor = Color(0xFF2196F3)

    Column {
        Text(
            languageViewModel.getTranslation("category"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedCategory?.name ?: languageViewModel.getTranslation("select_category"),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
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
                                selectedCategory.icon,
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
                        modifier = Modifier.clickable { onExpandedChange(!expanded) }
                    )
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(Color.White)
            ) {
                if (subCategories.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                languageViewModel.getTranslation("no_categories"),
                                color = Color(0xFF666666)
                            )
                        },
                        onClick = { onExpandedChange(false) }
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
                                        Text(category.icon, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        category.name,
                                        fontSize = 14.sp,
                                        fontWeight = if (selectedCategory?.id == category.id) FontWeight.Medium else FontWeight.Normal,
                                        color = if (selectedCategory?.id == category.id) primaryColor else Color(0xFF333333)
                                    )
                                }
                            },
                            onClick = {
                                onCategorySelected(category)
                                onExpandedChange(false)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountInput(
    amount: String,
    onAmountChanged: (String) -> Unit,
    languageViewModel: LanguageViewModel
) {
    val primaryColor = Color(0xFF2196F3)

    Column {
        Text(
            languageViewModel.getTranslation("amount"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = amount,
            onValueChange = {
                if (it.matches(Regex("^\\d*\\.?\\d*$")) && it.length <= 15) {
                    onAmountChanged(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "0",
                    color = Color(0xFF888888)
                )
            },
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
}

@Composable
private fun PeriodSelector(
    selectedPeriod: BudgetPeriodType,
    onPeriodSelected: (BudgetPeriodType) -> Unit,
    languageViewModel: LanguageViewModel
) {
    val primaryColor = Color(0xFF2196F3)

    Column {
        Text(
            languageViewModel.getTranslation("period"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BudgetPeriodType.values().forEach { period ->
                val isSelected = selectedPeriod == period
                val backgroundColor = if (isSelected) primaryColor else Color.Transparent
                val textColor = if (isSelected) Color.White else Color(0xFF666666)

                TextButton(
                    onClick = { onPeriodSelected(period) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = backgroundColor,
                        contentColor = textColor
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) primaryColor else Color(0xFFDDDDDD)
                    )
                ) {
                    Text(
                        period.getDisplayName(languageViewModel),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteInput(
    note: String,
    onNoteChanged: (String) -> Unit,
    languageViewModel: LanguageViewModel
) {
    val primaryColor = Color(0xFF2196F3)

    Column {
        Text(
            languageViewModel.getTranslation("note"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= 200) onNoteChanged(it) },
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
        Text(
            "${note.length}/200",
            fontSize = 12.sp,
            color = Color(0xFF888888),
            modifier = Modifier.align(Alignment.End)
        )
    }
}

private fun parseColor(colorString: String): Color {
    val hex = if (colorString.startsWith("#")) colorString else "#$colorString"
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}