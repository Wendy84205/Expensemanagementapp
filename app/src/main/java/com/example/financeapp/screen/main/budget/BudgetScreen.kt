package com.example.financeapp.screen.main.budget

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.data.models.Budget
import com.example.financeapp.data.models.BudgetPeriodType
import com.example.financeapp.data.models.isOverBudget
import com.example.financeapp.data.models.progressPercentage
import com.example.financeapp.data.models.remainingAmount
import com.example.financeapp.screen.features.formatCurrency
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    navController: NavController,
    budgetViewModel: BudgetViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
) {
    val languageViewModel = LocalLanguageViewModel.current
    val budgets by budgetViewModel.budgets.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val isLoading = false

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedBudget by remember { mutableStateOf<Budget?>(null) }

    // Load data khi vào màn hình
    LaunchedEffect(Unit) {
        budgetViewModel.startRealTimeUpdates()
    }

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                title = languageViewModel.getTranslation("budgets"),
                onBackClick = { navController.popBackStack() },
                languageViewModel = languageViewModel
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_budget") },
                containerColor = Color(0xFF2196F3),
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = languageViewModel.getTranslation("add_budget"),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        BudgetContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
            isLoading = isLoading,
            budgets = budgets,
            categories = categories,
            onEdit = { budget ->
                navController.navigate("edit_budget/${budget.id}")
            },
            onToggleStatus = { budget ->
                selectedBudget = budget
                showStatusDialog = true
            },
            onDelete = { budget ->
                selectedBudget = budget
                showDeleteDialog = true
            },
            onAddClick = {
                navController.navigate("add_budget")
            },
            languageViewModel = languageViewModel
        )
    }

    // Dialog xác nhận xóa
    if (showDeleteDialog && selectedBudget != null) {
        SimpleDeleteDialog(
            budget = selectedBudget!!,
            categories = categories,
            onConfirm = {
                budgetViewModel.deleteBudget(selectedBudget!!.id)
                showDeleteDialog = false
                selectedBudget = null
            },
            onDismiss = {
                showDeleteDialog = false
                selectedBudget = null
            },
            languageViewModel = languageViewModel
        )
    }

    // Dialog thay đổi trạng thái
    if (showStatusDialog && selectedBudget != null) {
        StatusDialog(
            budget = selectedBudget!!,
            onToggle = {
                val updatedBudget = selectedBudget!!.copy(isActive = !selectedBudget!!.isActive)
                budgetViewModel.updateFullBudget(updatedBudget)
                showStatusDialog = false
                selectedBudget = null
            },
            onDismiss = {
                showStatusDialog = false
                selectedBudget = null
            },
            languageViewModel = languageViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    languageViewModel: LanguageViewModel
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
                    contentDescription = languageViewModel.getTranslation("back"),
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
private fun BudgetContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    budgets: List<Budget>,
    categories: List<Category>,
    onEdit: (Budget) -> Unit,
    onToggleStatus: (Budget) -> Unit,
    onDelete: (Budget) -> Unit,
    onAddClick: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    Column(modifier = modifier) {
        if (isLoading) {
            LoadingState()
        } else {
            SimpleStatsCard(
                budgets = budgets,
                languageViewModel = languageViewModel
            )
            Spacer(modifier = Modifier.height(16.dp))
            BudgetList(
                budgets = budgets,
                categories = categories,
                onEdit = onEdit,
                onToggleStatus = onToggleStatus,
                onDelete = onDelete,
                onAddClick = onAddClick,
                languageViewModel = languageViewModel
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFF2196F3),
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun SimpleStatsCard(
    budgets: List<Budget>,
    languageViewModel: LanguageViewModel
) {
    val activeBudgets = budgets.count { it.isActive }
    val totalBudget = budgets.filter { it.isActive }.sumOf { it.amount }
    val totalSpent = budgets.filter { it.isActive }.sumOf { it.spentAmount }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        languageViewModel.getTranslation("total_budget"),
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        formatCurrency(totalBudget),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "$activeBudgets",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    Text(
                        languageViewModel.getTranslation("active"),
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar đơn giản
            if (totalBudget > 0) {
                val progress = (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .background(
                                if (progress > 0.8) Color(0xFFF44336) else Color(0xFF2196F3),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${languageViewModel.getTranslation("spent")}: ${formatCurrency(totalSpent)}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetList(
    budgets: List<Budget>,
    categories: List<Category>,
    onEdit: (Budget) -> Unit,
    onToggleStatus: (Budget) -> Unit,
    onDelete: (Budget) -> Unit,
    onAddClick: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (budgets.isEmpty()) {
            EmptyBudgetState(
                onAddClick = onAddClick,
                languageViewModel = languageViewModel
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgets) { budget ->
                    SimpleBudgetCard(
                        budget = budget,
                        category = categories.find { it.id == budget.categoryId },
                        onEdit = { onEdit(budget) },
                        onToggleStatus = { onToggleStatus(budget) },
                        onDelete = { onDelete(budget) },
                        languageViewModel = languageViewModel
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun SimpleBudgetCard(
    budget: Budget,
    category: Category?,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    var showMenu by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM")

    // Tên ngắn gọn cho budget period
    val periodText = when (budget.periodType) {
        BudgetPeriodType.WEEK -> languageViewModel.getTranslation("week")
        BudgetPeriodType.MONTH -> languageViewModel.getTranslation("month")
        BudgetPeriodType.QUARTER -> languageViewModel.getTranslation("quarter")
        BudgetPeriodType.YEAR -> languageViewModel.getTranslation("year")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Có thể thêm navigation chi tiết */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header với danh mục và menu
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = CenterVertically) {
                        // Icon danh mục
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    parseColor(category?.color ?: "#2196F3").copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                category?.icon ?: "💰",
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                category?.name ?: languageViewModel.getTranslation("unknown_category"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF333333)
                            )
                            Row(
                                verticalAlignment = CenterVertically
                            ) {
                                Text(
                                    "${budget.startDate.format(formatter)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF666666),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    "${budget.endDate.format(formatter)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "/$periodText",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = languageViewModel.getTranslation("menu"),
                            tint = Color(0xFF666666)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .width(180.dp)
                            .background(Color.White)
                    ) {
                        // Chỉnh sửa
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        languageViewModel.getTranslation("edit"),
                                        color = Color(0xFF333333),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            modifier = Modifier.height(42.dp)
                        )

                        Divider(
                            color = Color(0xFFEEEEEE),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Tạm dừng / Kích hoạt
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        if (budget.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        if (budget.isActive) languageViewModel.getTranslation("pause")
                                        else languageViewModel.getTranslation("activate"),
                                        color = Color(0xFF333333),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                onToggleStatus()
                            },
                            modifier = Modifier.height(42.dp)
                        )

                        Divider(
                            color = Color(0xFFEEEEEE),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Xóa
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFF44336),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        languageViewModel.getTranslation("delete"),
                                        color = Color(0xFFF44336),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            modifier = Modifier.height(42.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Thông tin số tiền
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "${formatCurrency(budget.spentAmount)} / ${formatCurrency(budget.amount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        "${languageViewModel.getTranslation("remaining")}: ${formatCurrency(budget.remainingAmount)}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }

                Text(
                    "${(budget.progressPercentage * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        !budget.isActive -> Color(0xFF999999)
                        budget.isOverBudget -> Color(0xFFF44336)
                        budget.progressPercentage > 0.8 -> Color(0xFFF44336)
                        else -> Color(0xFF2196F3)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            val progressColor = when {
                !budget.isActive -> Color(0xFF999999)
                budget.isOverBudget -> Color(0xFFF44336)
                budget.progressPercentage > 0.8 -> Color(0xFFF44336)
                else -> Color(0xFF2196F3)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(budget.progressPercentage.coerceIn(0f, 1f))
                        .height(6.dp)
                        .background(progressColor, RoundedCornerShape(3.dp))
                )
            }

            // Ghi chú (nếu có)
            budget.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null,
                        tint = Color(0xFF666666),
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        note,
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBudgetState(
    onAddClick: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.AccountBalanceWallet,
            contentDescription = languageViewModel.getTranslation("no_budgets"),
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            languageViewModel.getTranslation("no_budgets_yet"),
            color = Color(0xFF666666),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            languageViewModel.getTranslation("add_budget_to_manage_spending"),
            color = Color(0xFF999999),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            ),
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth(0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                languageViewModel.getTranslation("add_budget").uppercase(),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SimpleDeleteDialog(
    budget: Budget,
    categories: List<Category>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    val categoryName = categories.find { it.id == budget.categoryId }?.name ?:
    languageViewModel.getTranslation("this_budget")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    languageViewModel.getTranslation("delete").uppercase(),
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    languageViewModel.getTranslation("cancel").uppercase(),
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFFF44336).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = languageViewModel.getTranslation("warning"),
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                languageViewModel.getTranslation("delete_budget"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${languageViewModel.getTranslation("confirm_delete_budget")} \"$categoryName\"?",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    languageViewModel.getTranslation("action_cannot_be_undone"),
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun StatusDialog(
    budget: Budget,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    val actionText = if (budget.isActive) languageViewModel.getTranslation("pause")
    else languageViewModel.getTranslation("activate")
    val icon = if (budget.isActive) Icons.Default.Pause else Icons.Default.PlayArrow

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onToggle
            ) {
                Text(
                    actionText.uppercase(),
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    languageViewModel.getTranslation("cancel").uppercase(),
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFF2196F3).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = actionText,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                actionText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "${languageViewModel.getTranslation("confirm")} ${actionText.lowercase()} ${languageViewModel.getTranslation("this_budget")}?",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
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