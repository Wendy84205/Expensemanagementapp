package com.example.financeapp.screen.features.savings

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
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
import androidx.navigation.NavController
import com.example.financeapp.data.models.SavingsGoal
import com.example.financeapp.screen.features.formatCurrency
import com.example.financeapp.viewmodel.savings.SavingsViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SavingsGoalsScreen(
    navController: NavController,
    savingsViewModel: SavingsViewModel  // DÙNG instance từ NavGraph
) {
    // 🚨 QUAN TRỌNG: KHÔNG TẠO VIEWMODEL MỚI, DÙNG instance truyền vào
    val savingsGoals by savingsViewModel.savingsGoals.collectAsState()
    val isLoading by savingsViewModel.isLoading.collectAsState()
    val error by savingsViewModel.error.collectAsState()
    val addSuccess by savingsViewModel.addSuccess.collectAsState()
    val monthlyAnalysis by savingsViewModel.monthlyAnalysis.collectAsState()

    val auth = Firebase.auth
    val currentUser by remember(auth) {
        derivedStateOf { auth.currentUser }
    }

    // Load data khi vào màn hình
    LaunchedEffect(Unit) {
        println("💰 SavingsGoalsScreen: Đang tải goals...")
        savingsViewModel.loadSavingsGoals()
    }

    // Khi quay lại từ màn hình thêm/sửa mục tiêu
    LaunchedEffect(addSuccess) {
        if (addSuccess) {
            println("🔄 SavingsGoalsScreen: Reload data sau khi thêm thành công")
            savingsViewModel.loadSavingsGoals()
            savingsViewModel.resetAddSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mục tiêu tiết kiệm",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = currentUser != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_savings_goal") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Thêm mục tiêu",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Kiểm tra đăng nhập
            if (currentUser == null) {
                NotLoggedInView(
                    onLoginClick = {
                        navController.navigate("auth")
                    }
                )
            } else {
                when {
                    isLoading -> {
                        LoadingView()
                    }
                    error != null -> {
                        ErrorView(
                            error = error!!,
                            onRetry = { savingsViewModel.loadSavingsGoals() }
                        )
                    }
                    savingsGoals.isEmpty() -> {
                        EmptySavingsGoals(
                            onAddClick = { navController.navigate("add_savings_goal") },
                            monthlyAnalysis = monthlyAnalysis
                        )
                    }
                    else -> {
                        SavingsGoalsList(
                            savingsGoals = savingsGoals,
                            navController = navController,
                            monthlyAnalysis = monthlyAnalysis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "Đang tải mục tiêu...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "Có lỗi xảy ra",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = error,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp)
            ) {
                Text(
                    "Thử lại",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SavingsGoalsList(
    savingsGoals: List<SavingsGoal>,
    navController: NavController,
    monthlyAnalysis: com.example.financeapp.viewmodel.savings.SavingsViewModel.MonthlyAnalysis
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SavingsGoalsHeader(
                totalGoals = savingsGoals.size,
                totalSaved = savingsGoals.sumOf { it.currentAmount },
                totalTarget = savingsGoals.sumOf { it.targetAmount },
                monthlyAnalysis = monthlyAnalysis
            )
        }

        items(savingsGoals, key = { it.id }) { goal ->
            SavingsGoalCard(
                goal = goal,
                onClick = {
                    navController.navigate("add_savings_goal/${goal.id}")
                }
            )
        }
    }
}

@Composable
private fun SavingsGoalsHeader(
    totalGoals: Int,
    totalSaved: Long,
    totalTarget: Long,
    monthlyAnalysis: com.example.financeapp.viewmodel.savings.SavingsViewModel.MonthlyAnalysis
) {
    val totalProgress = if (totalTarget > 0) {
        (totalSaved.toFloat() / totalTarget.toFloat() * 100).coerceAtMost(100f)
    } else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title
            Text(
                text = "📊 Tổng quan tiết kiệm",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    title = "Mục tiêu",
                    value = totalGoals.toString(),
                    icon = Icons.Default.Flag,
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    title = "Đã tiết kiệm",
                    value = formatCurrency(totalSaved.toDouble()),
                    icon = Icons.Default.Savings,
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatItem(
                    title = "Tỉ lệ tiết kiệm",
                    value = "${monthlyAnalysis.savingsRate.toInt()}%",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Progress section
            if (totalTarget > 0) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tiến độ tổng thể",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${totalProgress.toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { totalProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatCurrency(totalSaved.toDouble()),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(totalTarget.toDouble()),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Monthly insights
            if (monthlyAnalysis.savings > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tiết kiệm tháng này",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(monthlyAnalysis.savings.toDouble()),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            Icons.Default.Celebration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NotLoggedInView(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Cần đăng nhập",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Đăng nhập để xem và quản lý\nmục tiêu tiết kiệm của bạn",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                "Đăng nhập ngay",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptySavingsGoals(
    onAddClick: () -> Unit,
    monthlyAnalysis: com.example.financeapp.viewmodel.savings.SavingsViewModel.MonthlyAnalysis
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Savings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Chưa có mục tiêu tiết kiệm",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Bắt đầu tiết kiệm bằng cách tạo\nmục tiêu đầu tiên của bạn",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )

        // Show potential savings if available
        if (monthlyAnalysis.savings > 0) {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bạn có thể tiết kiệm",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(monthlyAnalysis.savings.toDouble()),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "tháng này",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "Tạo mục tiêu mới",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tiết kiệm giúp bạn đạt được\nnhững ước mơ trong tương lai",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    onClick: () -> Unit
) {
    val progress = goal.calculateProgress()
    val remainingDays = calculateRemainingDays(goal.deadline)

    // Tính toán tiền cần góp hàng tháng
    val monthlyContribution = if (goal.deadline > 0 && remainingDays > 0) {
        val remainingMonths = (remainingDays / 30).coerceAtLeast(1)
        val remainingAmount = goal.targetAmount - goal.currentAmount
        if (remainingAmount > 0 && remainingMonths > 0) {
            (remainingAmount / remainingMonths).coerceAtLeast(1)
        } else {
            0L
        }
    } else {
        0L
    }

    // Color scheme based on progress
    val cardColor = when {
        goal.isCompleted || progress >= 100 -> MaterialTheme.colorScheme.tertiaryContainer
        progress > 70 -> MaterialTheme.colorScheme.primaryContainer
        progress > 50 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val progressColor = when {
        goal.isCompleted || progress >= 100 -> MaterialTheme.colorScheme.tertiary
        progress > 70 -> MaterialTheme.colorScheme.primary
        progress > 50 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icon with progress circle
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.size(56.dp),
                            strokeWidth = 3.dp,
                            color = progressColor,
                            trackColor = progressColor.copy(alpha = 0.2f)
                        )
                        Icon(
                            getGoalIcon(goal.category),
                            contentDescription = null,
                            tint = progressColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = goal.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (goal.description.isNotEmpty()) {
                            Text(
                                text = goal.description,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.width(200.dp)
                            )
                        }
                        Text(
                            text = goal.category,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = progressColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${progress.toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.2f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatCurrency(goal.currentAmount.toDouble()),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatCurrency(goal.targetAmount.toDouble()),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GoalStatItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Còn cần",
                    value = formatCurrency((goal.targetAmount - goal.currentAmount).toDouble()),
                    color = MaterialTheme.colorScheme.error
                )

                if (monthlyContribution > 0 && remainingDays > 30) {
                    GoalStatItem(
                        icon = Icons.Default.CalendarToday,
                        title = "Mỗi tháng",
                        value = formatCurrency(monthlyContribution.toDouble()),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    GoalStatItem(
                        icon = Icons.Default.Schedule,
                        title = if (remainingDays > 0) "$remainingDays ngày" else "Không hạn",
                        value = "",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                GoalStatItem(
                    icon = if (goal.autoCalculate) Icons.Default.AutoMode else Icons.Default.ManageAccounts,
                    title = if (goal.autoCalculate) "Tự động" else "Thủ công",
                    value = if (goal.allocationPercentage > 0) "${goal.allocationPercentage}%" else "",
                    color = if (goal.autoCalculate) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GoalStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (value.isNotEmpty()) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// Helper functions
private fun calculateRemainingDays(deadline: Long): Int {
    if (deadline <= 0) return 0
    val currentTime = System.currentTimeMillis()
    val diff = deadline - currentTime
    return if (diff > 0) {
        (diff / (1000 * 60 * 60 * 24)).toInt()
    } else {
        0
    }
}

private fun getGoalIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "du lịch", "travel" -> Icons.Default.Flight
        "xe cộ", "car", "vehicle" -> Icons.Default.DirectionsCar
        "nhà cửa", "house", "home" -> Icons.Default.Home
        "giáo dục", "education", "study" -> Icons.Default.School
        "sức khỏe", "health" -> Icons.Default.MedicalServices
        "hôn nhân", "wedding" -> Icons.Default.Favorite
        "đầu tư", "investment" -> Icons.AutoMirrored.Filled.TrendingUp
        "khẩn cấp", "emergency" -> Icons.Default.Warning
        "mua sắm", "shopping" -> Icons.Default.ShoppingCart
        "giải trí", "entertainment" -> Icons.Default.Movie
        else -> Icons.Default.Savings
    }
}