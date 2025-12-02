package com.example.financeapp.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.data.Budget
import com.example.financeapp.viewmodel.budget.BudgetViewModel
import com.example.financeapp.viewmodel.Category
import com.example.financeapp.viewmodel.CategoryViewModel
import com.example.financeapp.data.getDisplayName
import com.example.financeapp.data.isOverBudget
import com.example.financeapp.data.progressPercentage
import com.example.financeapp.data.remainingAmount
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    navController: NavController,
    budgetViewModel: BudgetViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
) {
    val budgets by budgetViewModel.budgets.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val isLoading = false // Tạm thời set false

    var showDeleteDialog by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }

    // Load data khi vào màn hình
    LaunchedEffect(Unit) {
        budgetViewModel.startRealTimeUpdates()
    }

    Scaffold(
        topBar = {
            DashboardTopAppBar(
                title = "Ngân sách",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            DashboardFloatingActionButton(
                onClick = { navController.navigate("add_budget") }
            )
        }
    ) { padding ->
        DashboardContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            isLoading = isLoading,
            budgets = budgets,
            categories = categories,
            onEdit = { budget ->
                navController.navigate("edit_budget/${budget.id}")
            },
            onDelete = { budget ->
                budgetToDelete = budget
                showDeleteDialog = true
            },
            onAddClick = {
                navController.navigate("add_budget")
            }
        )
    }

    if (showDeleteDialog && budgetToDelete != null) {
        DeleteConfirmationDialog(
            budget = budgetToDelete!!,
            categories = categories,
            onConfirm = {
                budgetViewModel.deleteBudget(budgetToDelete!!.id)
                showDeleteDialog = false
                budgetToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                budgetToDelete = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopAppBar(
    title: String,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun DashboardFloatingActionButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(64.dp)
            .shadow(8.dp, CircleShape)
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Thêm ngân sách",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun DashboardContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    budgets: List<Budget>,
    categories: List<Category>,
    onEdit: (Budget) -> Unit,
    onDelete: (Budget) -> Unit,
    onAddClick: () -> Unit
) {
    Column(modifier = modifier) {
        if (isLoading) {
            LoadingState()
        } else {
            BudgetStatsHeader(budgets = budgets, categories = categories)
            Spacer(modifier = Modifier.height(8.dp))
            BudgetListSection(
                budgets = budgets,
                categories = categories,
                onEdit = onEdit,
                onDelete = onDelete,
                onAddClick = onAddClick
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Text(
                "Đang tải...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun BudgetStatsHeader(budgets: List<Budget>, categories: List<Category>) {
    val activeBudgets = budgets.count { it.isActive }
    val totalBudget = budgets.filter { it.isActive }.sumOf { it.amount }
    val totalSpent = budgets.filter { it.isActive }.sumOf { it.spentAmount }
    val remainingBudget = totalBudget - totalSpent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Main stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Tổng ngân sách",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatCurrency(totalBudget),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Active count badge
                ActiveCountBadge(count = activeBudgets)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Monthly breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Còn lại:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatCurrency(remainingBudget),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ActiveCountBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "đang hoạt động",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BudgetListSection(
    budgets: List<Budget>,
    categories: List<Category>,
    onEdit: (Budget) -> Unit,
    onDelete: (Budget) -> Unit,
    onAddClick: () -> Unit
) {
    if (budgets.isEmpty()) {
        EmptyBudgetState(onAddClick = onAddClick)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgets) { budget ->
                    BudgetListItem(
                        budget = budget,
                        categories = categories,
                        onEdit = { onEdit(budget) },
                        onDelete = { onDelete(budget) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBudgetState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.AccountBalanceWallet,
                contentDescription = "Không có ngân sách",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Chưa có ngân sách nào",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Thêm ngân sách để quản lý chi tiêu hiệu quả hơn",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth(0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Thêm ngân sách", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BudgetListItem(
    budget: Budget,
    categories: List<Category>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val category = categories.find { it.id == budget.categoryId }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header với category và menu
            BudgetHeader(
                budget = budget,
                category = category,
                onEdit = onEdit,
                onDelete = onDelete
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Thông tin chi tiết
            BudgetDetails(budget = budget)

            // Progress bar
            Spacer(modifier = Modifier.height(12.dp))
            BudgetProgressBar(budget = budget)

            // Ghi chú (nếu có)
            budget.note?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(modifier = Modifier.height(12.dp))
                BudgetDescription(description = description)
            }
        }
    }
}

@Composable
private fun BudgetHeader(
    budget: Budget,
    category: Category?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Category info
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(
                icon = category?.icon ?: "💰",
                color = category?.color ?: "#0F4C75"
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    category?.name ?: "Unknown Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    budget.periodType.getDisplayName(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Status và Menu
        Column(horizontalAlignment = Alignment.End) {
            StatusBadge(budget = budget)
            Spacer(modifier = Modifier.height(8.dp))
            BudgetMenu(
                budget = budget,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun CategoryIcon(icon: String, color: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                parseColor(color).copy(alpha = 0.1f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            icon,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun StatusBadge(budget: Budget) {
    val (text, color) = when {
        !budget.isActive -> Pair("Tạm dừng", MaterialTheme.colorScheme.onSurfaceVariant)
        budget.isOverBudget -> Pair("Vượt hạn mức", MaterialTheme.colorScheme.error)
        budget.progressPercentage > 0.8 -> Pair("Sắp hết", MaterialTheme.colorScheme.error)
        else -> Pair("Đang hoạt động", MaterialTheme.colorScheme.primary)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                color.copy(alpha = 0.1f)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun BudgetMenu(
    budget: Budget,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            DropdownMenuItem(
                text = { Text("Sửa") },
                onClick = { expanded = false; onEdit() },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Edit,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (budget.isActive) "Tạm dừng" else "Kích hoạt",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = { expanded = false; /* TODO: Toggle budget */ },
                leadingIcon = {
                    Icon(
                        if (budget.isActive) Icons.Outlined.Pause
                        else Icons.Outlined.PlayArrow,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Xóa",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = { expanded = false; onDelete() },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

@Composable
private fun BudgetDetails(budget: Budget) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                "${formatCurrency(budget.spentAmount)} / ${formatCurrency(budget.amount)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Còn lại: ${formatCurrency(budget.remainingAmount)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${(budget.progressPercentage * 100).toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    budget.isOverBudget -> MaterialTheme.colorScheme.error
                    budget.progressPercentage > 0.8 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            Text(
                "${budget.startDate.format(formatter)} - ${budget.endDate.format(formatter)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetProgressBar(budget: Budget) {
    val progressColor = when {
        !budget.isActive -> MaterialTheme.colorScheme.onSurfaceVariant
        budget.isOverBudget -> MaterialTheme.colorScheme.error
        budget.progressPercentage > 0.8 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    LinearProgressIndicator(
        progress = budget.progressPercentage.coerceIn(0f, 1f),
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    )
}

@Composable
private fun BudgetDescription(description: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Outlined.Notes,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    budget: Budget,
    categories: List<Category>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val categoryName = categories.find { it.id == budget.categoryId }?.name ?: "Unknown Category"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Xóa ngân sách",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                "Bạn có chắc muốn xóa ngân sách \"$categoryName\"? Hành động này không thể hoàn tác.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    "Xóa",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    "Hủy",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
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