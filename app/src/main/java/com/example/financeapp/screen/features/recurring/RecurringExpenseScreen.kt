package com.example.financeapp.screen.features.recurring

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.model.RecurringExpense
import com.example.financeapp.model.RecurringFrequency
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import androidx.core.graphics.toColorInt
import com.example.financeapp.screen.features.formatCurrency
import com.example.financeapp.viewmodel.transaction.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseScreen(
    navController: NavController,
    recurringExpenseViewModel: RecurringExpenseViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
) {
    val recurringExpenses by recurringExpenseViewModel.recurringExpenses.collectAsState()
    val isLoading by recurringExpenseViewModel.isLoading.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<RecurringExpense?>(null) }

    LaunchedEffect(Unit) {
        recurringExpenseViewModel.loadRecurringExpenses()
    }

    Scaffold(
        topBar = {
            DashboardTopAppBar(
                title = "Chi tiêu định kỳ",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            DashboardFloatingActionButton(
                onClick = { navController.navigate("add_recurring_expense") }
            )
        }
    ) { padding ->
        DashboardContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            isLoading = isLoading,
            expenses = recurringExpenses,
            onEdit = { expense ->
                navController.navigate("edit_recurring_expense/${expense.id}")
            },
            onToggle = { expense ->
                recurringExpenseViewModel.toggleRecurringExpense(expense.id)
            },
            onDelete = { expense ->
                expenseToDelete = expense
                showDeleteDialog = true
            },
            onAddClick = {
                navController.navigate("add_recurring_expense")
            }
        )
    }

    if (showDeleteDialog && expenseToDelete != null) {
        DeleteConfirmationDialog(
            expense = expenseToDelete!!,
            onConfirm = {
                recurringExpenseViewModel.deleteRecurringExpense(expenseToDelete!!.id)
                showDeleteDialog = false
                expenseToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                expenseToDelete = null
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
            contentDescription = "Thêm chi tiêu định kỳ",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun DashboardContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    expenses: List<RecurringExpense>,
    onEdit: (RecurringExpense) -> Unit,
    onToggle: (RecurringExpense) -> Unit,
    onDelete: (RecurringExpense) -> Unit,
    onAddClick: () -> Unit
) {
    Column(modifier = modifier) {
        if (isLoading) {
            LoadingState()
        } else {
            ExpenseStatsHeader(expenses)
            Spacer(modifier = Modifier.height(8.dp))
            ExpenseListSection(
                expenses = expenses,
                onEdit = onEdit,
                onToggle = onToggle,
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
private fun ExpenseStatsHeader(expenses: List<RecurringExpense>) {
    val totalMonthly = expenses
        .filter { it.isActive && it.getFrequencyEnum() == RecurringFrequency.MONTHLY }
        .sumOf { it.amount }

    val activeCount = expenses.count { it.isActive }
    val totalAmount = expenses.filter { it.isActive }.sumOf { it.amount }

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
                        "Tổng chi tiêu định kỳ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatCurrency(totalAmount),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Active count badge
                ActiveCountBadge(count = activeCount)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Monthly breakdown
            MonthlyBreakdownRow(monthlyAmount = totalMonthly)
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
private fun MonthlyBreakdownRow(monthlyAmount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Chi hàng tháng:",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            formatCurrency(monthlyAmount),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ExpenseListSection(
    expenses: List<RecurringExpense>,
    onEdit: (RecurringExpense) -> Unit,
    onToggle: (RecurringExpense) -> Unit,
    onDelete: (RecurringExpense) -> Unit,
    onAddClick: () -> Unit
) {
    if (expenses.isEmpty()) {
        EmptyExpenseState(onAddClick = onAddClick)
    } else {
        // SỬA: Dùng Box với fillMaxSize() thay vì weight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseListItem(
                        expense = expense,
                        onEdit = { onEdit(expense) },
                        onToggle = { onToggle(expense) },
                        onDelete = { onDelete(expense) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyExpenseState(onAddClick: () -> Unit) {
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
                Icons.Outlined.Autorenew,
                contentDescription = "Không có chi tiêu định kỳ",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Chưa có chi tiêu định kỳ",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Thêm chi tiêu định kỳ để quản lý chi tiêu tự động và không bỏ sót khoản chi nào",
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
            Text("Thêm chi tiêu định kỳ", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ExpenseListItem(
    expense: RecurringExpense,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
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
            ExpenseHeader(
                expense = expense,
                onEdit = onEdit,
                onToggle = onToggle,
                onDelete = onDelete
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Thông tin chi tiết
            ExpenseDetails(expense = expense)

            // Ghi chú (nếu có)
            expense.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(modifier = Modifier.height(12.dp))
                ExpenseDescription(description = description)
            }
        }
    }
}

@Composable
private fun ExpenseHeader(
    expense: RecurringExpense,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
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
                icon = expense.categoryIcon,
                color = expense.categoryColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    expense.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    expense.category,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Status và Menu
        Column(horizontalAlignment = Alignment.End) {
            StatusBadge(isActive = expense.isActive)
            Spacer(modifier = Modifier.height(8.dp))
            ExpenseMenu(
                isActive = expense.isActive,
                onEdit = onEdit,
                onToggle = onToggle,
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
private fun StatusBadge(isActive: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            if (isActive) "Đang hoạt động" else "Tạm dừng",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpenseMenu(
    isActive: Boolean,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
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
                        if (isActive) "Tạm dừng" else "Kích hoạt",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = { expanded = false; onToggle() },
                leadingIcon = {
                    Icon(
                        if (isActive) Icons.Outlined.Pause
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
private fun ExpenseDetails(expense: RecurringExpense) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                formatCurrency(expense.amount),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${expense.getFrequencyEnum().displayName} • ${expense.wallet}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "Tiếp theo: ${expense.nextOccurrence}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Đã tạo: ${expense.totalGenerated}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpenseDescription(description: String) {
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
    expense: RecurringExpense,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Xóa chi tiêu định kỳ",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                "Bạn có chắc muốn xóa \"${expense.title}\"? Hành động này không thể hoàn tác.",
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