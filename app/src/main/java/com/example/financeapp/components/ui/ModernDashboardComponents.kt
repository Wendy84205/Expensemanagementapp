package com.example.financeapp.components.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financeapp.components.theme.getAppColors

@Composable
fun ModernBalanceCard(
    totalBalance: Double,
    monthlyIncome: Float,
    monthlyExpense: Float,
    modifier: Modifier = Modifier
) {
    val colors = getAppColors()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.primaryGradient)
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Tổng số dư",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = formatVNDLarge(totalBalance.toFloat()),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BalanceStatItem(
                        label = "Thu nhập",
                        amount = monthlyIncome,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconColor = colors.income
                    )
                    
                    VerticalDivider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )
                    
                    BalanceStatItem(
                        label = "Chi tiêu",
                        amount = monthlyExpense,
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        iconColor = colors.expense
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceStatItem(
    label: String,
    amount: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
            Text(
                text = formatVNDSmall(amount),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AIInsightWidget(
    insight: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = getAppColors()
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = colors.background, // Thay thế Slate 100 bằng màu nền chung
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider) // Thay thế E2E8F0 bằng màu divider chung
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.accentGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gợi ý từ Wendy AI",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = insight,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun formatVNDLarge(amount: Float): String {
    return String.format("%,.0f đ", amount).replace(",", ".")
}

private fun formatVNDSmall(amount: Float): String {
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"))
    return "${formatter.format(amount.toLong())} đ"
}
