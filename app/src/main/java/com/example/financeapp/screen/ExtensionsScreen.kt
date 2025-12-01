package com.example.financeapp.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.financeapp.rememberLanguageText

// 🎨 Màu theo gói ý 1
private val Navy = Color(0xFF0F4C75)
private val SoftGray = Color(0xFFF5F7FA)
private val TextDark = Color(0xFF2D3748)
private val TextLight = Color(0xFF718096)
private val GreenDark = Color(0xFF2E8B57)
private val OrangeAccent = Color(0xFFED8936)
private val PurpleAccent = Color(0xFF9F7AEA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(navController: NavController) {

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        rememberLanguageText("extensions"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = SoftGray
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {

            // 📌 QUẢN LÝ DANH MỤC
            ExtensionCard(
                icon = Icons.Default.Category,
                title = rememberLanguageText("category"),
                subtitle = rememberLanguageText("customize_spending_categories"),
                bgColor = Navy,
                onClick = { navController.navigate("categories") }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 💰 NGÂN SÁCH
            ExtensionCard(
                icon = Icons.Default.Money,
                title = rememberLanguageText("budgets"),
                subtitle = rememberLanguageText("set_and_track_monthly_budget"),
                bgColor = GreenDark,
                onClick = { navController.navigate("budgets") }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 🔄 CHI TIÊU ĐỊNH KỲ - THÊM MỚI
            ExtensionCard(
                icon = Icons.Default.Repeat,
                title = "Chi tiêu định kỳ",
                subtitle = "Quản lý các khoản chi tự động hàng tháng",
                bgColor = OrangeAccent,
                onClick = { navController.navigate("recurring_expenses") }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 💎 TIẾT KIỆM (Có thể thêm trong tương lai)
            ExtensionCard(
                icon = Icons.Default.Savings,
                title = "Mục tiêu tiết kiệm",
                subtitle = "Theo dõi và đạt mục tiêu tiết kiệm",
                bgColor = PurpleAccent,
                onClick = {
                    // navController.navigate("savings_goals")
                    // TODO: Thêm sau khi có tính năng
                }
            )
        }
    }
}

@Composable
fun ExtensionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                clip = true
            )
            .clickable { onClick() }
            .height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {

            // ICON với background đẹp
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(bgColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            // NỘI DUNG
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = TextLight,
                    lineHeight = 18.sp
                )
            }

            // MŨI TÊN CHUYỂN TRANG
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextLight.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}