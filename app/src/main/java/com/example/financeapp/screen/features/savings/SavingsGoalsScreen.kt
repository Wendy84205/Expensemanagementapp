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
    userId: String
) {
    val viewModel: SavingsViewModel = viewModel()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val auth = Firebase.auth
    val currentUser by remember(auth) {
        derivedStateOf { auth.currentUser }
    }

    // Load data khi vào màn hình
    LaunchedEffect(Unit) {
        viewModel.loadSavingsGoals()
    }

    // Refresh khi quay lại màn hình
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.route == "savings_goals") {
                viewModel.loadSavingsGoals()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Mục tiêu tiết kiệm",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF475569)
                        )
                    }
                },
                actions = {
                    if (currentUser != null) {
                        IconButton(
                            onClick = { navController.navigate("add_savings_goal") },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF3B82F6), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Thêm mục tiêu",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = currentUser != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_savings_goal") },
                    containerColor = Color(0xFF3B82F6),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = true
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Thêm mục tiêu",
                            tint = Color.White
                        )
                        Text(
                            "Thêm mục tiêu",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF3B82F6),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    "Đang tải mục tiêu...",
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(Color(0xFFFEE2E2), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Text(
                                    text = error!!,
                                    color = Color(0xFFDC2626),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                Button(
                                    onClick = { viewModel.loadSavingsGoals() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3B82F6)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.width(150.dp)
                                ) {
                                    Text("Thử lại")
                                }
                            }
                        }
                    }
                    savingsGoals.isEmpty() -> {
                        EmptySavingsGoals(
                            onAddClick = { navController.navigate("add_savings_goal") }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header với tổng số mục tiêu
                            item {
                                SavingsGoalsHeader(
                                    totalGoals = savingsGoals.size,
                                    totalSaved = savingsGoals.sumOf { it.currentAmount },
                                    totalTarget = savingsGoals.sumOf { it.targetAmount }
                                )
                            }

                            // Danh sách mục tiêu
                            items(savingsGoals) { goal ->
                                SavingsGoalCard(
                                    goal = goal,
                                    viewModel = viewModel,
                                    onClick = {
                                        navController.navigate("savings_goal_detail/${goal.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalsHeader(
    totalGoals: Int,
    totalSaved: Long,
    totalTarget: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tổng quan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GoalStat(
                    title = "Mục tiêu",
                    value = totalGoals.toString(),
                    color = Color(0xFF3B82F6)
                )
                GoalStat(
                    title = "Đã tiết kiệm",
                    value = formatCurrency(totalSaved.toDouble()),
                    color = Color(0xFF10B981)
                )
                GoalStat(
                    title = "Tổng mục tiêu",
                    value = formatCurrency(totalTarget.toDouble()),
                    color = Color(0xFF8B5CF6)
                )
            }

            // Overall progress bar
            if (totalTarget > 0) {
                Spacer(modifier = Modifier.height(20.dp))
                val overallProgress = (totalSaved.toFloat() / totalTarget.toFloat() * 100).coerceAtMost(100f)

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tiến độ tổng thể",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "${overallProgress.toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = overallProgress / 100,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = Color(0xFF3B82F6),
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalStat(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color(0xFF64748B)
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
                .size(120.dp)
                .background(Color(0xFFF1F5F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Cần đăng nhập",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Đăng nhập để xem và quản lý\nmục tiêu tiết kiệm của bạn",
            fontSize = 16.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                "Đăng nhập ngay",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptySavingsGoals(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(Color(0xFFF0F9FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Savings,
                contentDescription = null,
                tint = Color(0xFF0EA5E9),
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Chưa có mục tiêu tiết kiệm",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Bắt đầu tiết kiệm bằng cách tạo\nmục tiêu đầu tiên của bạn",
            fontSize = 16.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6)
            ),
            shape = RoundedCornerShape(14.dp),
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
                    tint = Color.White
                )
                Text(
                    "Tạo mục tiêu mới",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tiết kiệm giúp bạn đạt được\nnhững ước mơ trong tương lai",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    viewModel: SavingsViewModel,
    onClick: () -> Unit
) {
    val progress = goal.calculateProgress()
    val remainingDays = calculateRemainingDays(goal.deadline)
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

    val colors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Green
        Color(0xFFF59E0B), // Yellow
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899)  // Pink
    )

    val color = colors.getOrElse(goal.color) { colors[0] }

    val deadlineText = if (goal.deadline > 0) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(goal.deadline))
    } else "Không có hạn"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header với icon và tên
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(color.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = goal.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        if (goal.description.isNotEmpty()) {
                            Text(
                                text = goal.description,
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    }
                }

                // Progress percentage
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                goal.isCompleted || progress >= 100 -> Color(0xFF10B981).copy(alpha = 0.1f)
                                progress > 70 -> Color(0xFF3B82F6).copy(alpha = 0.1f)
                                else -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${progress.toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            goal.isCompleted || progress >= 100 -> Color(0xFF10B981)
                            progress > 70 -> Color(0xFF3B82F6)
                            else -> Color(0xFFF59E0B)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress bar với label
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatCurrency(goal.currentAmount.toDouble()),
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = formatCurrency(goal.targetAmount.toDouble()),
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = progress / 100,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = when {
                        goal.isCompleted || progress >= 100 -> Color(0xFF10B981)
                        progress > 70 -> Color(0xFF3B82F6)
                        progress > 50 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    },
                    trackColor = Color(0xFFE2E8F0)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Remaining amount
                GoalStatItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Còn cần",
                    value = formatCurrency((goal.targetAmount - goal.currentAmount).toDouble()),
                    color = Color(0xFFEF4444)
                )

                // Monthly contribution
                if (monthlyContribution > 0 && remainingDays > 30) {
                    GoalStatItem(
                        icon = Icons.Default.CalendarToday,
                        title = "Mỗi tháng",
                        value = formatCurrency(monthlyContribution.toDouble()),
                        color = Color(0xFF3B82F6)
                    )
                }

                // Deadline
                GoalStatItem(
                    icon = Icons.Default.Schedule,
                    title = if (remainingDays > 0) "Còn $remainingDays ngày" else "Hạn",
                    value = if (remainingDays > 0) "" else deadlineText,
                    color = Color(0xFF10B981)
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF64748B)
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

// Helper function để tính số ngày còn lại
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