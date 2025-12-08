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
import com.example.financeapp.LocalLanguageViewModel
import com.example.financeapp.viewmodel.settings.LanguageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(navController: NavController) {
    val languageViewModel = LocalLanguageViewModel.current

    // Colors
    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White
    val textColor = Color(0xFF333333)
    val subtitleColor = Color(0xFF666666)

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                title = languageViewModel.getTranslation("extensions"),
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
                            title = languageViewModel.getTranslation("expense_categories"),
                            subtitle = languageViewModel.getTranslation("customize_spending_categories"),
                            color = Color(0xFF2196F3),
                            onClick = { navController.navigate("categories") }
                        )

                        Divider(color = Color(0xFFEEEEEE))

                        // Ngân sách
                        ExtensionItem(
                            icon = Icons.Default.Money,
                            title = languageViewModel.getTranslation("budgets"),
                            subtitle = languageViewModel.getTranslation("set_and_track_monthly_budget"),
                            color = Color(0xFF4CAF50),
                            onClick = { navController.navigate("budgets") }
                        )

                        Divider(color = Color(0xFFEEEEEE))

                        // Chi tiêu định kỳ
                        ExtensionItem(
                            icon = Icons.Default.Repeat,
                            title = languageViewModel.getTranslation("recurring_expenses"),
                            subtitle = languageViewModel.getTranslation("manage_automatic_monthly_expenses"),
                            color = Color(0xFFFF9800),
                            onClick = { navController.navigate("recurring_expenses") }
                        )

                        Divider(color = Color(0xFFEEEEEE))

                        ExtensionItem(
                            icon = Icons.Default.Savings,
                            title = languageViewModel.getTranslation("savings_goals"),
                            subtitle = languageViewModel.getTranslation("track_and_achieve_savings_targets"),
                            color = Color(0xFF9C27B0),
                            onClick = {
                                navController.navigate("savings_goals")
                            }
                        )

                        Divider(color = Color(0xFFEEEEEE))
                        // Quét hoá đơn
                        ExtensionItem(
                            icon = Icons.Default.Receipt,
                            title = languageViewModel.getTranslation("receipt_scan"),
                            subtitle = languageViewModel.getTranslation("scan_receipts_auto_input_expenses"),
                            color = Color(0xFF00BCD4),
                            onClick = { /* TODO: Navigate to receipt scan */ },
                            isComingSoon = true,
                            languageViewModel = languageViewModel
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
    val languageViewModel = LocalLanguageViewModel.current

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
private fun ExtensionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    isComingSoon: Boolean = false,
    languageViewModel: LanguageViewModel? = null
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
                    contentDescription = title,
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
                    if (isComingSoon && languageViewModel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEEEEEE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                languageViewModel.getTranslation("coming_soon"),
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
                    contentDescription = languageViewModel?.getTranslation("unavailable") ?: "Không khả dụng",
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}