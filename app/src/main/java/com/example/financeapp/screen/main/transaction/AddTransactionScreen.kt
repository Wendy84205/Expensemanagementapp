@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.financeapp.screen.main.transaction

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.savings.SavingsViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.viewmodel.transaction.TransactionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    onBack: () -> Unit,
    onSave: (Transaction) -> Unit,
    transactionViewModel: TransactionViewModel = viewModel(),
    budgetViewModel: BudgetViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    existingTransaction: Transaction? = null,
    onDelete: (() -> Unit)? = null,
    savingsViewModel: SavingsViewModel
) {
    val languageViewModel = LocalLanguageViewModel.current

    // State management
    var amount by remember { mutableStateOf(existingTransaction?.amount?.toString() ?: "") }
    var categoryId by remember { mutableStateOf(existingTransaction?.category ?: "") }
    var isIncome by remember { mutableStateOf(existingTransaction?.isIncome ?: false) }
    var description by remember { mutableStateOf(existingTransaction?.description ?: "") }
    var transactionDate by remember { mutableStateOf(existingTransaction?.date ?: getTodayDate()) }
    var transactionDayOfWeek by remember { mutableStateOf(
        existingTransaction?.dayOfWeek ?: getTodayDayOfWeek(languageViewModel)
    ) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Animation states
    val transactionTypeTransition = updateTransition(targetState = isIncome, label = "transactionType")
    val saveButtonEnabled by remember { derivedStateOf { amount.isNotBlank() && categoryId.isNotBlank() } }

    val transactionType = if (isIncome) "income" else "expense"
    val selectableCategoriesMap by categoryViewModel.selectableCategories.collectAsState()
    val selectableCategories = remember(selectableCategoriesMap, transactionType) {
        selectableCategoriesMap[transactionType] ?: emptyList()
    }

    // Auto-focus on amount field
    val focusRequester = remember { FocusRequester() }

    // ============== XỬ LÝ GIAO DỊCH ĐÃ QUÉT ==============
    val scannedTransaction by navController
        .previousBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<Transaction?>("scanned_transaction", null)
        ?.collectAsState()
        ?: remember { mutableStateOf(null) }

    LaunchedEffect(scannedTransaction) {
        scannedTransaction?.let { transaction ->
            amount = transaction.amount.toString()
            categoryId = transaction.category
            isIncome = transaction.isIncome
            description = transaction.description ?: ""
            transactionDate = transaction.date
            transactionDayOfWeek = transaction.dayOfWeek

            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.remove<Transaction>("scanned_transaction")
        }
    }

    // Colors based on transaction type with smooth transitions
    val primaryColor by transactionTypeTransition.animateColor(
        transitionSpec = { tween(durationMillis = 300) },
        label = "primaryColor"
    ) { isIncome ->
        if (isIncome) Color(0xFF10B981) else Color(0xFFEF4444)
    }

    val secondaryColor by transactionTypeTransition.animateColor(
        transitionSpec = { tween(durationMillis = 300) },
        label = "secondaryColor"
    ) { isIncome ->
        if (isIncome) Color(0xFF059669) else Color(0xFFDC2626)
    }

    // Gradient background
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FAFC),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0)
        ),
        startY = 0f,
        endY = 1000f
    )

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(bottom = 8.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = languageViewModel.getTranslation("back"),
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        if (existingTransaction != null) languageViewModel.getTranslation("edit_transaction")
                        else languageViewModel.getTranslation("add_new_transaction"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
        ) {
            // Animated floating background elements
            AnimatedFloatingBackground(primaryColor)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Main form container with animation
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 200)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 500, delayMillis = 200),
                                initialOffsetY = { it }
                            ),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateContentSize(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Transaction type selector with enhanced animation
                            TransactionTypeSelector(
                                isIncome = isIncome,
                                onTypeChange = { newIsIncome ->
                                    isIncome = newIsIncome
                                    categoryId = "" // Reset category khi đổi loại
                                },
                                languageViewModel = languageViewModel,
                                primaryColor = primaryColor
                            )

                            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            // Amount input with focus animation
                            AmountInputField(
                                amount = amount,
                                onAmountChange = { newValue ->
                                    if (newValue.matches(Regex("^\\d*\\.?\\d*$")) || newValue.isEmpty()) {
                                        amount = newValue
                                    }
                                },
                                languageViewModel = languageViewModel,
                                primaryColor = primaryColor,
                                focusRequester = focusRequester
                            )

                            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            // Category selector mới với horizontal scroll
                            CategorySectionHorizontal(
                                categories = selectableCategories,
                                selectedCategoryId = categoryId,
                                onCategorySelected = { selected ->
                                    categoryId = selected.id
                                },
                                languageViewModel = languageViewModel,
                                primaryColor = primaryColor
                            )

                            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            // Date selector with calendar animation
                            DateSelector(
                                date = transactionDate,
                                dayOfWeek = transactionDayOfWeek,
                                languageViewModel = languageViewModel,
                                primaryColor = primaryColor,
                                onClick = { showDatePicker = true }
                            )

                            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            // Description field with character counter
                            DescriptionField(
                                description = description,
                                onDescriptionChange = { description = it },
                                languageViewModel = languageViewModel,
                                primaryColor = primaryColor
                            )

                            // Animated save button
                            AnimatedSaveButton(
                                isEnabled = saveButtonEnabled,
                                isIncome = isIncome,
                                isEditing = existingTransaction != null,
                                languageViewModel = languageViewModel,
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                onClick = {
                                    val selectedCategoryInfo = selectableCategories.find { it.id == categoryId }
                                    val transaction = Transaction(
                                        id = existingTransaction?.id ?: generateTransactionId(),
                                        date = transactionDate,
                                        dayOfWeek = transactionDayOfWeek,
                                        category = selectedCategoryInfo?.name ?: categoryId,
                                        amount = amount.toDoubleOrNull() ?: 0.0,
                                        isIncome = isIncome,
                                        group = if (isIncome) languageViewModel.getTranslation("income")
                                        else languageViewModel.getTranslation("spending"),
                                        wallet = "Ví chính",
                                        description = description,
                                        categoryId = selectedCategoryInfo?.id ?: categoryId,
                                        categoryIcon = selectedCategoryInfo?.icon,
                                        categoryColor = selectedCategoryInfo?.color ?: "#667EEA",
                                        title = description.ifBlank { selectedCategoryInfo?.name ?: categoryId }
                                    )
                                    onSave(transaction)
                                }
                            )

                            // Delete button for editing mode
                            if (existingTransaction != null && onDelete != null) {
                                AnimatedDeleteButton(
                                    onClick = { showDeleteDialog = true },
                                    languageViewModel = languageViewModel,
                                    primaryColor = primaryColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Warning messages
                val warning by transactionViewModel.warningMessage.collectAsState()
                warning?.let { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        WarningCard(
                            message = message,
                            onDismiss = { transactionViewModel.clearWarning() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDatePicker) {
        AnimatedDatePicker(
            initialDate = parseDate(transactionDate),
            primaryColor = primaryColor,
            onDateSelected = { date ->
                transactionDate = formatDate(date)
                transactionDayOfWeek = getDayOfWeekFromDate(date, languageViewModel)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            languageViewModel = languageViewModel,
            primaryColor = primaryColor,
            onConfirm = {
                showDeleteDialog = false
                onDelete?.invoke()
                onBack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// ============ ANIMATED BACKGROUND ============

@Composable
private fun AnimatedFloatingBackground(primaryColor: Color) {
    val infiniteTransition = rememberInfiniteTransition()

    val floatAnimation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val floatAnimation2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Floating circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Circle 1
            drawCircle(
                color = primaryColor.copy(alpha = 0.05f),
                radius = 150.dp.toPx(),
                center = Offset(
                    size.width * 0.8f + cos(floatAnimation1 * (Math.PI / 180).toFloat()) * 50f,
                    size.height * 0.2f + sin(floatAnimation1 * (Math.PI / 180).toFloat()) * 50f
                )
            )

            // Circle 2
            drawCircle(
                color = primaryColor.copy(alpha = 0.03f),
                radius = 100.dp.toPx(),
                center = Offset(
                    size.width * 0.2f + cos(floatAnimation2 * (Math.PI / 180).toFloat()) * 30f,
                    size.height * 0.7f + sin(floatAnimation2 * (Math.PI / 180).toFloat()) * 30f
                )
            )
        }
    }
}

// ============ TRANSACTION TYPE SELECTOR ============

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TransactionTypeSelector(
    isIncome: Boolean,
    onTypeChange: (Boolean) -> Unit,
    languageViewModel: LanguageViewModel,
    primaryColor: Color
) {
    Column {
        Text(
            languageViewModel.getTranslation("transaction_type"),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Tab style selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Income Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isIncome) Color(0xFF10B981) else Color.Transparent
                        )
                        .clickable {
                            if (!isIncome) {
                                onTypeChange(true)
                            }
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.TrendingUp,
                            contentDescription = "Thu nhập",
                            tint = if (isIncome) Color.White else Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            languageViewModel.getTranslation("income"),
                            color = if (isIncome) Color.White else Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Expense Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (!isIncome) Color(0xFFEF4444) else Color.Transparent
                        )
                        .clickable {
                            if (isIncome) {
                                onTypeChange(false)
                            }
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.TrendingDown,
                            contentDescription = "Chi tiêu",
                            tint = if (!isIncome) Color.White else Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            languageViewModel.getTranslation("spending"),
                            color = if (!isIncome) Color.White else Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Animated indicator (optional)
        AnimatedContent(
            targetState = isIncome,
            label = "transactionTypeIndicator"
        ) { targetIsIncome ->
            if (targetIsIncome) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.padding(start = 8.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(
                            languageViewModel.getTranslation("you_are_receiving_money"),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Card(
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text(
                            languageViewModel.getTranslation("you_are_spending_money"),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============ AMOUNT INPUT FIELD ============

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AmountInputField(
    amount: String,
    onAmountChange: (String) -> Unit,
    languageViewModel: LanguageViewModel,
    primaryColor: Color,
    focusRequester: FocusRequester
) {
    var isFocused by remember { mutableStateOf(false) }

    Column {
        Text(
            languageViewModel.getTranslation("amount"),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFocused) primaryColor.copy(alpha = 0.05f) else Color(0xFFF8FAFC)
            ),
            border = BorderStroke(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) primaryColor else Color(0xFFE2E8F0)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency symbol with animation
                AnimatedContent(
                    targetState = amount.isNotBlank(),
                    transitionSpec = {
                        scaleIn(animationSpec = tween(durationMillis = 200)) + fadeIn() with
                                scaleOut(animationSpec = tween(durationMillis = 200)) + fadeOut()
                    },
                    label = "currencyAnimation"
                ) { hasAmount ->
                    if (hasAmount) {
                        Text(
                            "₫",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(primaryColor.copy(alpha = 0.1f), CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "₫",
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }

                // Amount input field
                BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    decorationBox = { innerTextField ->
                        if (amount.isEmpty()) {
                            Text(
                                "0",
                                color = Color(0xFF94A3B8),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // Format hint
        if (amount.isNotBlank()) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Text(
                    "${languageViewModel.getTranslation("entered")}: ${formatCurrencyDisplay(amount)}",
                    fontSize = 13.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

// ============ CATEGORY SELECTOR HORIZONTAL ============

@Composable
private fun CategorySectionHorizontal(
    categories: List<Category>,
    selectedCategoryId: String,
    onCategorySelected: (Category) -> Unit,
    languageViewModel: LanguageViewModel,
    primaryColor: Color
) {
    val scrollState = rememberScrollState()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                languageViewModel.getTranslation("category"),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155)
            )

            if (selectedCategoryId.isNotBlank()) {
                Text(
                    "${languageViewModel.getTranslation("selected")} • ${categories.count()}",
                    fontSize = 12.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hiển thị tất cả danh mục trong horizontal scroll
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            val containerWidth = maxWidth

            if (categories.isNotEmpty()) {
                // Tính toán item width để vừa với màn hình
                val itemWidth = containerWidth / 4.5f

                HorizontalScrollableCategoryList(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = onCategorySelected,
                    itemWidth = itemWidth,
                    primaryColor = primaryColor,
                    scrollState = scrollState
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        languageViewModel.getTranslation("no_categories_available"),
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Scroll indicator
        if (categories.size > 5) {
            Spacer(modifier = Modifier.height(8.dp))
            ScrollIndicator(
                itemCount = categories.size,
                selectedIndex = categories.indexOfFirst { it.id == selectedCategoryId },
                scrollState = scrollState,
                primaryColor = primaryColor
            )
        }
    }
}

@Composable
private fun HorizontalScrollableCategoryList(
    categories: List<Category>,
    selectedCategoryId: String,
    onCategorySelected: (Category) -> Unit,
    itemWidth: Dp,
    primaryColor: Color,
    scrollState: ScrollState
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Thêm padding bên trái
        Spacer(modifier = Modifier.width(4.dp))

        categories.forEachIndexed { index, category ->
            HorizontalCategoryItem(
                category = category,
                isSelected = selectedCategoryId == category.id,
                onClick = {
                    onCategorySelected(category)
                    // Auto scroll to center when selected
                    coroutineScope.launch {
                        // Tính toán vị trí item
                        val itemWidthPx = with(density) { itemWidth.toPx() }
                        val spacingPx = with(density) { 8.dp.toPx() }
                        val itemPosition = index * (itemWidthPx + spacingPx)

                        val containerCenter = scrollState.maxValue / 2f
                        val targetScroll = (itemPosition - containerCenter).toInt()

                        // Đảm bảo giá trị scroll nằm trong phạm vi hợp lệ
                        val adjustedScroll = targetScroll.coerceIn(0, scrollState.maxValue)
                        scrollState.animateScrollTo(
                            value = adjustedScroll,
                            animationSpec = tween(durationMillis = 300)
                        )
                    }
                },
                modifier = Modifier.width(itemWidth),
                primaryColor = primaryColor,
                index = index
            )
        }
        // Thêm padding bên phải
        Spacer(modifier = Modifier.width(4.dp))
    }
}
@Composable
private fun HorizontalCategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color,
    index: Int
) {
    val categoryColor = parseColor(category.color)
    val interactionSource = remember { MutableInteractionSource() }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "categoryBackground"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else Color(0xFFE2E8F0),
        animationSpec = tween(durationMillis = 200),
        label = "categoryBorder"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "categoryScale"
    )

    // Hiệu ứng click ripple
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressScale"
    )

    // Sử dụng Box với clickable đơn giản
    Box(
        modifier = modifier
            .fillMaxHeight()
            .scale(scale * pressScale)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                onClick = onClick
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Icon container với viền nhỏ
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) primaryColor.copy(alpha = 0.1f)
                        else categoryColor.copy(alpha = 0.1f)
                    )
                    .border(
                        width = 0.5.dp,
                        color = if (isSelected) primaryColor.copy(alpha = 0.3f)
                        else categoryColor.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    category.icon,
                    fontSize = 18.sp,
                    color = if (isSelected) primaryColor else categoryColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category name với ellipsis
            Text(
                category.name,
                color = if (isSelected) primaryColor else Color(0xFF374151),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )

            // Selected indicator
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
            }
        }
    }
}
@Composable
private fun ScrollIndicator(
    itemCount: Int,
    selectedIndex: Int,
    scrollState: ScrollState,
    primaryColor: Color
) {
    val maxScroll = scrollState.maxValue.toFloat()
    val currentScroll = scrollState.value.toFloat()
    val progress = if (maxScroll > 0) currentScroll / maxScroll else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color(0xFFF1F5F9), RoundedCornerShape(2.dp)),
        horizontalArrangement = Arrangement.Start
    ) {
        // Progress indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(maxOf(4.dp, (LocalConfiguration.current.screenWidthDp.dp * progress)))
                .background(primaryColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        )

        // Selected indicator dot
        if (selectedIndex >= 0 && itemCount > 0) {
            val screenWidth = LocalConfiguration.current.screenWidthDp
            val selectedPosition =
                (selectedIndex.toFloat() / (itemCount - 1).toFloat()) * screenWidth
            val currentPosition = progress * screenWidth
            val dotOffset = (selectedPosition - currentPosition).dp

            if (dotOffset.value >= 0 && dotOffset.value <= screenWidth) {
                Spacer(modifier = Modifier.width(dotOffset))

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
            }
        }
    }
}
// ============ DATE SELECTOR ============

@Composable
private fun DateSelector(
    date: String,
    dayOfWeek: String,
    languageViewModel: LanguageViewModel,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Column {
        Text(
            languageViewModel.getTranslation("transaction_date"),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8FAFC)
            ),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = "Ngày",
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            dayOfWeek,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            fontSize = 16.sp
                        )
                        Text(
                            date,
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Today indicator
                if (date == getTodayDate()) {
                    Box(
                        modifier = Modifier
                            .background(primaryColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            languageViewModel.getTranslation("today"),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.EditCalendar,
                        contentDescription = "Chỉnh sửa ngày",
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ============ DESCRIPTION FIELD ============

@Composable
private fun DescriptionField(
    description: String,
    onDescriptionChange: (String) -> Unit,
    languageViewModel: LanguageViewModel,
    primaryColor: Color
) {
    var isFocused by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                languageViewModel.getTranslation("note"),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155)
            )

            // Character counter
            AnimatedVisibility(
                visible = description.isNotBlank(),
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally()
            ) {
                Text(
                    "${description.length}/100",
                    fontSize = 12.sp,
                    color = if (description.length > 100) Color(0xFFEF4444) else primaryColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFocused) primaryColor.copy(alpha = 0.05f) else Color(0xFFF8FAFC)
            ),
            border = BorderStroke(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) primaryColor else Color(0xFFE2E8F0)
            )
        ) {
            BasicTextField(
                value = description,
                onValueChange = {
                    if (it.length <= 100) onDescriptionChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(16.dp)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    },
                singleLine = false,
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF0F172A)
                ),
                decorationBox = { innerTextField ->
                    if (description.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Notes,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                languageViewModel.getTranslation("enter_transaction_description"),
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    innerTextField()
                }
            )
        }
    }
}

// ============ SAVE BUTTON ============

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedSaveButton(
    isEnabled: Boolean,
    isIncome: Boolean,
    isEditing: Boolean,
    languageViewModel: LanguageViewModel,
    primaryColor: Color,
    secondaryColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.95f,
        animationSpec = tween(durationMillis = 200),
        label = "saveButtonScale"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isEnabled) primaryColor else Color(0xFFCBD5E1),
        animationSpec = tween(durationMillis = 300),
        label = "saveButtonColor"
    )

    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(scale)
            .shadow(
                elevation = if (isEnabled) 8.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            disabledContainerColor = Color(0xFFE2E8F0)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Animated icon
            AnimatedContent(
                targetState = isEnabled,
                transitionSpec = {
                    scaleIn(animationSpec = tween(durationMillis = 200)) + fadeIn() with
                            scaleOut(animationSpec = tween(durationMillis = 200)) + fadeOut()
                },
                label = "saveIconAnimation"
            ) { enabled ->
                if (enabled) {
                    Icon(
                        if (isEditing) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Animated text
            AnimatedContent(
                targetState = Triple(isEnabled, isEditing, isIncome),
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 200)) with
                            fadeOut(animationSpec = tween(durationMillis = 200))
                },
                label = "saveTextAnimation"
            ) { (enabled, editing, income) ->
                Text(
                    if (!enabled) languageViewModel.getTranslation("please_complete_all_fields")
                    else if (editing) languageViewModel.getTranslation("update_transaction")
                    else if (income) languageViewModel.getTranslation("add_income_transaction")
                    else languageViewModel.getTranslation("add_expense_transaction"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (enabled) Color.White else Color(0xFF64748B)
                )
            }
        }
    }
}

// ============ DELETE BUTTON ============

@Composable
private fun AnimatedDeleteButton(
    onClick: () -> Unit,
    languageViewModel: LanguageViewModel,
    primaryColor: Color
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "deleteButtonScale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFFCA5A5)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Xoá",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                languageViewModel.getTranslation("delete_transaction"),
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

// ============ WARNING CARD ============

@Composable
private fun WarningCard(
    message: String,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(5000)
        isVisible = false
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            border = BorderStroke(1.dp, Color(0xFFFDE68A))
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
                            .size(40.dp)
                            .background(Color(0xFFFBBF24).copy(alpha = 0.1f), CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = "Cảnh báo",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        message,
                        color = Color(0xFF92400E),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                IconButton(
                    onClick = {
                        isVisible = false
                        onDismiss()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color(0xFF92400E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ============ ANIMATED DATE PICKER ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedDatePicker(
    initialDate: Date,
    primaryColor: Color,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance().apply { time = initialDate }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis
    )
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
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
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Chọn ngày",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = primaryColor,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = primaryColor,
                    todayContentColor = primaryColor,
                    dayContentColor = Color(0xFF334155),
                    weekdayContentColor = Color(0xFF64748B)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Selected date preview
            datePickerState.selectedDateMillis?.let { millis ->
                val selectedDate = Date(millis)
                val formattedDate = formatDate(selectedDate)
                val dayOfWeek = getDayOfWeekFromDate(selectedDate, LocalLanguageViewModel.current)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = primaryColor.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Ngày đã chọn",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                            Text(
                                "$dayOfWeek, $formattedDate",
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }

                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Đã chọn",
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text("Huỷ", color = Color(0xFF64748B))
                }

                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onDateSelected(Date(it))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    ),
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Xác nhận", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ============ DELETE CONFIRMATION DIALOG ============

@Composable
private fun DeleteConfirmationDialog(
    languageViewModel: LanguageViewModel,
    primaryColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFEE2E2), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    languageViewModel.getTranslation("delete_transaction"),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        },
        text = {
            Text(
                languageViewModel.getTranslation("delete_transaction_description"),
                color = Color(0xFF64748B)
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        languageViewModel.getTranslation("cancel"),
                        color = Color(0xFF64748B)
                    )
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    Text(
                        languageViewModel.getTranslation("delete"),
                        color = Color.White
                    )
                }
            }
        }
    )
}

// ============ UTILITY FUNCTIONS ============

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
    return "TR_${System.currentTimeMillis()}_${(1000..9999).random()}"
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

private fun formatCurrencyDisplay(amount: String): String {
    return if (amount.isBlank()) "0 đ"
    else {
        val number = amount.replace(".", "").toDoubleOrNull() ?: 0.0
        String.format("%,.0f đ", number).replace(",", ".")
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF667EEA) // Default purple color
    }
}
@Composable
private fun Dp.toPx(): Float {
    val density = LocalDensity.current
    return density.run {
        this@toPx.toPx()
    }
}