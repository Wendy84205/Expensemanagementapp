package com.example.financeapp.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(navController: NavController) {

    // Colors
    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White
    val textColor = Color(0xFF333333)
    val subtitleColor = Color(0xFF666666)

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                title = "Tiện ích mở rộng",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = backgroundColor
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // Danh mục
                        ExtensionItem(
                            icon = Icons.Default.Category,
                            title = "Quản lý danh mục",
                            subtitle = "Tùy chỉnh danh mục chi tiêu",
                            color = Color(0xFF2196F3),
                            onClick = { navController.navigate("categories") }
                        )

                        Divider(color = Color(0xFFEEEEEE))

                        // Ngân sách
                        ExtensionItem(
                            icon = Icons.Default.Money,
                            title = "Ngân sách",
                            subtitle = "Thiết lập và theo dõi ngân sách",
                            color = Color(0xFF4CAF50),
                            onClick = { navController.navigate("budgets") }
                        )

                        Divider(color = Color(0xFFEEEEEE))

                        // Chi tiêu định kỳ
                        ExtensionItem(
                            icon = Icons.Default.Repeat,
                            title = "Chi tiêu định kỳ",
                            subtitle = "Quản lý chi tiêu tự động hàng tháng",
                            color = Color(0xFFFF9800),
                            onClick = { navController.navigate("recurring_expenses") }
                        )

                        Divider(color = Color(0xFFEEEEEE))

                        // Tiết kiệm
                        ExtensionItem(
                            icon = Icons.Default.Savings,
                            title = "Mục tiêu tiết kiệm",
                            subtitle = "Theo dõi và đạt mục tiêu tiết kiệm",
                            color = Color(0xFF9C27B0),
                            onClick = { /* TODO: Navigate to savings */ },
                            isComingSoon = true
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Tiện ích nâng cao",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
                        )

                        // Quét hóa đơn
                        ExtensionItem(
                            icon = Icons.Default.Receipt,
                            title = "Quét hóa đơn",
                            subtitle = "Quét hóa đơn tự động nhập chi tiêu",
                            color = Color(0xFF00BCD4),
                            onClick = { /* TODO: Navigate to receipt scan */ },
                            isComingSoon = true
                        )

                        Divider(color = Color(0xFFEEEEEE))

                        // Phân tích AI
                        ExtensionItem(
                            icon = Icons.Default.Analytics,
                            title = "Phân tích AI",
                            subtitle = "Phân tích chi tiêu với trí tuệ nhân tạo",
                            color = Color(0xFFFF5722),
                            onClick = { /* TODO: Navigate to AI analysis */ },
                            isComingSoon = true
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopAppBar(
    title: String,
    onBackClick: () -> Unit
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
                    contentDescription = "Quay lại",
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
private fun ExtensionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    isComingSoon: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = !isComingSoon,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        color = if (isComingSoon) Color(0xFF999999) else Color(0xFF333333),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    if (isComingSoon) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEEEEEE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Sắp ra mắt",
                                fontSize = 10.sp,
                                color = Color(0xFF666666),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = if (isComingSoon) Color(0xFF999999) else Color(0xFF666666)
                )
            }

            if (!isComingSoon) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF999999)
                )
            } else {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Không khả dụng",
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}