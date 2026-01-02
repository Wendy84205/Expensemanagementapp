package com.example.financeapp.screen.features.recurring

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.financeapp.data.models.RecurringExpense
import com.example.financeapp.data.models.RecurringFrequency
import com.example.financeapp.viewmodel.features.RecurringExpenseViewModel
import androidx.core.graphics.toColorInt
import com.example.financeapp.screen.features.formatCurrency
import com.example.financeapp.viewmodel.transaction.CategoryViewModel
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseScreen(
    navController: NavController,
    recurringExpenseViewModel: RecurringExpenseViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
) {
    val languageViewModel = LocalLanguageViewModel.current
    val recurringExpenses by recurringExpenseViewModel.recurringExpenses.collectAsState()
    val isLoading by recurringExpenseViewModel.isLoading.collectAsState()

    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White
    val textColor = Color(0xFF333333)
    val subtitleColor = Color(0xFF666666)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<RecurringExpense?>(null) }

    LaunchedEffect(Unit) {
        recurringExpenseViewModel.loadRecurringExpenses()
    }

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                title = languageViewModel.getTranslation("recurring_expenses"),
                onBackClick = { navController.popBackStack() },
                languageViewModel = languageViewModel
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_recurring_expense") },
                containerColor = primaryColor,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = languageViewModel.getTranslation("add_recurring_expense"),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            if (isLoading) {
                LoadingState(primaryColor = primaryColor)
            } else {
                SimpleStatsCard(
                    expenses = recurringExpenses,
                    primaryColor = primaryColor,
                    languageViewModel = languageViewModel
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExpenseList(
                    expenses = recurringExpenses,
                    primaryColor = primaryColor,
                    onEdit = { expense ->
                        navController.navigate("edit_recurring_expense/${expense.id}")
                    },
                    onToggleStatus = { expense ->
                        selectedExpense = expense
                        showStatusDialog = true
                    },
                    onDelete = { expense ->
                        selectedExpense = expense
                        showDeleteDialog = true
                    },
                    onAddClick = {
                        navController.navigate("add_recurring_expense")
                    },
                    languageViewModel = languageViewModel
                )
            }
        }
    }

    if (showDeleteDialog && selectedExpense != null) {
        SimpleDeleteDialog(
            expense = selectedExpense!!,
            onConfirm = {
                recurringExpenseViewModel.deleteRecurringExpense(selectedExpense!!.id)
                showDeleteDialog = false
                selectedExpense = null
            },
            onDismiss = {
                showDeleteDialog = false
                selectedExpense = null
            },
            languageViewModel = languageViewModel
        )
    }

    if (showStatusDialog && selectedExpense != null) {
        StatusDialog(
            expense = selectedExpense!!,
            onToggle = {
                recurringExpenseViewModel.toggleRecurringExpense(selectedExpense!!.id)
                showStatusDialog = false
                selectedExpense = null
            },
            onDismiss = {
                showStatusDialog = false
                selectedExpense = null
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
private fun LoadingState(primaryColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = primaryColor,
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun SimpleStatsCard(
    expenses: List<RecurringExpense>,
    primaryColor: Color,
    languageViewModel: LanguageViewModel
) {
    val activeExpenses = expenses.count { it.isActive }
    val totalAmount = expenses.filter { it.isActive }.sumOf { it.amount }

    val totalMonthly = expenses
        .filter { it.isActive && it.getFrequencyEnum() == RecurringFrequency.MONTHLY }
        .sumOf { it.amount }

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
                        languageViewModel.getTranslation("total_expenses"),
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        formatCurrency(totalAmount),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "$activeExpenses",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        languageViewModel.getTranslation("active_lower"),
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        languageViewModel.getTranslation("monthly_expenses"),
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
                Text(
                    formatCurrency(totalMonthly),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
            }
        }
    }
}

@Composable
private fun ExpenseList(
    expenses: List<RecurringExpense>,
    primaryColor: Color,
    onEdit: (RecurringExpense) -> Unit,
    onToggleStatus: (RecurringExpense) -> Unit,
    onDelete: (RecurringExpense) -> Unit,
    onAddClick: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (expenses.isEmpty()) {
            EmptyExpenseState(
                onAddClick = onAddClick,
                primaryColor = primaryColor,
                languageViewModel = languageViewModel
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses) { expense ->
                    SimpleExpenseCard(
                        expense = expense,
                        primaryColor = primaryColor,
                        onEdit = { onEdit(expense) },
                        onToggleStatus = { onToggleStatus(expense) },
                        onDelete = { onDelete(expense) },
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
private fun SimpleExpenseCard(
    expense: RecurringExpense,
    primaryColor: Color,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    var showMenu by remember { mutableStateOf(false) }

    // Format next occurrence từ internal format sang UI format (ĐÃ FIX)
    val nextDate = try {
        RecurringExpense.formatDateForUI(expense.nextOccurrence)
    } catch (e: Exception) {
        expense.nextOccurrence // Fallback
    }

    // Format ngày đẹp hơn
    val formattedNextDate = try {
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .parse(expense.nextOccurrence)
        date?.let { sdf.format(it) } ?: nextDate
    } catch (e: Exception) {
        nextDate
    }

    // Format start date
    val startDate = try {
        RecurringExpense.formatDateForUI(expense.startDate)
    } catch (e: Exception) {
        expense.startDate
    }

    // Format end date nếu có
    val endDateText = expense.endDate?.let {
        try {
            " - ${RecurringExpense.formatDateForUI(it)}"
        } catch (e: Exception) {
            ""
        }
    } ?: ""

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
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                parseColor(expense.categoryColor).copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            expense.categoryIcon,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            expense.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            "${expense.category} • ${expense.wallet}",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
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
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = primaryColor,
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

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        if (expense.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        if (expense.isActive) languageViewModel.getTranslation("pause")
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        formatCurrency(expense.amount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        getFrequencyDisplayName(expense.getFrequencyEnum(), languageViewModel),
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "${languageViewModel.getTranslation("next")}: $formattedNextDate",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        "${languageViewModel.getTranslation("generated")}: ${expense.totalGenerated} ${languageViewModel.getTranslation("times")}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Thêm thông tin ngày bắt đầu và kết thúc
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "${languageViewModel.getTranslation("period")}: $startDate$endDateText",
                    fontSize = 11.sp,
                    color = Color(0xFF888888)
                )

                if (expense.lastGenerated != null) {
                    val lastGen = try {
                        RecurringExpense.formatDateForUI(expense.lastGenerated)
                    } catch (e: Exception) {
                        expense.lastGenerated
                    }
                    Text(
                        "${languageViewModel.getTranslation("last")}: $lastGen",
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        if (expense.isActive) primaryColor.copy(alpha = 0.3f)
                        else Color(0xFF999999).copy(alpha = 0.3f),
                        RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            if (expense.isActive) primaryColor else Color(0xFF999999),
                            RoundedCornerShape(3.dp)
                        )
                )
            }

            expense.description?.takeIf { it.isNotBlank() }?.let { note ->
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
private fun EmptyExpenseState(
    onAddClick: () -> Unit,
    primaryColor: Color,
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
            Icons.Outlined.Autorenew,
            contentDescription = languageViewModel.getTranslation("no_recurring_expenses"),
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            languageViewModel.getTranslation("no_recurring_expenses_yet"),
            color = Color(0xFF666666),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            languageViewModel.getTranslation("add_recurring_expense_to_manage"),
            color = Color(0xFF999999),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SimpleDeleteDialog(
    expense: RecurringExpense,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    languageViewModel: LanguageViewModel
) {
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
                languageViewModel.getTranslation("delete_recurring_expense"),
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
                    "${languageViewModel.getTranslation("confirm_delete_recurring_expense")} \"${expense.title}\"?",
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
    expense: RecurringExpense,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    languageViewModel: LanguageViewModel
) {
    val actionText = if (expense.isActive) languageViewModel.getTranslation("pause")
    else languageViewModel.getTranslation("activate")
    val icon = if (expense.isActive) Icons.Default.Pause else Icons.Default.PlayArrow
    val primaryColor = Color(0xFF2196F3)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onToggle
            ) {
                Text(
                    actionText.uppercase(),
                    color = primaryColor,
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
                    .background(primaryColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = actionText,
                    tint = primaryColor,
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
                "${languageViewModel.getTranslation("confirm")} ${actionText.lowercase()} \"${expense.title}\"?",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

private fun getFrequencyDisplayName(frequency: RecurringFrequency, languageViewModel: LanguageViewModel): String {
    return when (frequency) {
        RecurringFrequency.DAILY -> languageViewModel.getTranslation("daily")
        RecurringFrequency.WEEKLY -> languageViewModel.getTranslation("weekly")
        RecurringFrequency.MONTHLY -> languageViewModel.getTranslation("monthly")
        RecurringFrequency.QUARTERLY -> languageViewModel.getTranslation("quarterly")
        RecurringFrequency.YEARLY -> languageViewModel.getTranslation("annually")
    }
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