package com.example.financeapp.screen.main.budget

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.components.theme.getAppColors
import com.example.financeapp.data.models.Budget
import com.example.financeapp.data.models.BudgetPeriodType
import com.example.financeapp.data.models.calculateBudgetEndDate
import com.example.financeapp.utils.CurrencyUtils
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
    onBack: () -> Unit,
    budgetViewModel: BudgetViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    existingBudget: Budget? = null
) {
    val languageViewModel = LocalLanguageViewModel.current
    val categories by categoryViewModel.categories.collectAsState()
    val focusManager = LocalFocusManager.current

    val colors = getAppColors()
    val primaryColor = colors.primary
    val subCategories = remember(categories) {
        categories.filter { !it.isMainCategory }
    }

    var selectedCategory by remember {
        mutableStateOf(
            existingBudget?.let { budget ->
                categories.find { cat -> cat.id == budget.categoryId }
            } ?: subCategories.firstOrNull()
        )
    }
    var amount by remember { 
        mutableStateOf(
            existingBudget?.amount?.let { 
                if (it == 0.0) "" 
                else if (it % 1.0 == 0.0) it.toLong().toString() 
                else it.toString() 
            } ?: ""
        ) 
    }
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = colors.textPrimary
                        )
                    }
                    Text(
                        if (existingBudget == null) languageViewModel.getTranslation("add_budget")
                        else languageViewModel.getTranslation("edit_budget"),
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
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            if (showError) {
                ErrorBanner(errorMessage) { showError = false }
            }

            // Prominent Amount Input (Momo/Premium style)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = colors.surface,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        languageViewModel.getTranslation("budget_amount").uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BasicTextField(
                            value = amount,
                            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            cursorBrush = SolidColor(primaryColor),
                            modifier = Modifier.widthIn(min = 40.dp),
                            decorationBox = { innerTextField ->
                                if (amount.isEmpty()) {
                                    Text(
                                        "0",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = colors.textMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                innerTextField()
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            languageViewModel.getTranslation("currency_vnd"),
                            style = MaterialTheme.typography.titleLarge,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        currentDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }

            // Form Content
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        languageViewModel.getTranslation("details"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    CategorySelector(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        categories = subCategories,
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        languageViewModel = languageViewModel
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PeriodSelector(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { selectedPeriod = it },
                        languageViewModel = languageViewModel
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    NoteInput(
                        note = note,
                        onNoteChanged = { note = it },
                        languageViewModel = languageViewModel
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = { saveBudget() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(
                    if (existingBudget == null) languageViewModel.getTranslation("create_budget")
                    else languageViewModel.getTranslation("update_budget"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    val colors = getAppColors()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onDismiss() },
        color = Color(0xFFFEE2E2),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444))
            Spacer(modifier = Modifier.width(8.dp))
            Text(message, color = Color(0xFFB91C1C), fontSize = 14.sp)
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    categories: List<Category>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    languageViewModel: LanguageViewModel
) {
    val colors = getAppColors()
    val primaryColor = colors.primary

    Column {
        Text(
            languageViewModel.getTranslation("category"),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box {
            OutlinedTextField(
                value = selectedCategory?.name ?: languageViewModel.getTranslation("select_category"),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = colors.divider,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = if (selectedCategory != null) colors.textPrimary else colors.textMuted,
                    cursorColor = primaryColor,
                    focusedContainerColor = colors.background.copy(alpha = 0.3f),
                    unfocusedContainerColor = colors.background.copy(alpha = 0.3f)
                ),
                leadingIcon = {
                    selectedCategory?.let {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(parseColor(it.color).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(it.icon, fontSize = 16.sp)
                        }
                    }
                },
                trailingIcon = {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.textMuted
                    )
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(colors.surface)
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(parseColor(category.color).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(category.icon, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(category.name, color = colors.textPrimary)
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

@Composable
private fun PeriodSelector(
    selectedPeriod: BudgetPeriodType,
    onPeriodSelected: (BudgetPeriodType) -> Unit,
    languageViewModel: LanguageViewModel
) {
    val colors = getAppColors()
    val primaryColor = colors.primary

    Column {
        Text(
            languageViewModel.getTranslation("period"),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BudgetPeriodType.values().forEach { type ->
                val isSelected = selectedPeriod == type
                Surface(
                    onClick = { onPeriodSelected(type) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) primaryColor else Color.Transparent,
                    contentColor = if (isSelected) Color.White else colors.textSecondary
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            type.getDisplayName(languageViewModel),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
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
    val colors = getAppColors()
    val focusManager = LocalFocusManager.current

    Column {
        Text(
            languageViewModel.getTranslation("notes"),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= 200) onNoteChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            placeholder = {
                Text(
                    languageViewModel.getTranslation("add_note_placeholder"),
                    color = colors.textMuted
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.divider,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.primary,
                focusedContainerColor = colors.background.copy(alpha = 0.3f),
                unfocusedContainerColor = colors.background.copy(alpha = 0.3f)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = false,
            maxLines = 4
        )
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}