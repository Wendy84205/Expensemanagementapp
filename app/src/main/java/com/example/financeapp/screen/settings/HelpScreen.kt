package com.example.financeapp.screen.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.financeapp.LocalLanguageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavController) {
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
                title = "Trợ giúp",
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
            // Câu hỏi thường gặp
            item {
                SettingsCard(title = "Câu hỏi thường gặp") {
                    FAQItemExpandable(
                        question = "Làm thế nào để thêm giao dịch?",
                        answer = "Chuyển đến màn hình Trang chủ, nhấn nút '+' ở dưới cùng, chọn 'Thêm giao dịch', điền thông tin và nhấn 'Lưu'.",
                        primaryColor = primaryColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    FAQItemExpandable(
                        question = "Làm thế nào để thêm ví mới?",
                        answer = "Vào màn hình Ví, nhấn nút '+' ở góc dưới bên phải, nhập tên ví và số dư ban đầu.",
                        primaryColor = primaryColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    FAQItemExpandable(
                        question = "Làm thế nào để xem thống kê?",
                        answer = "Vào màn hình Thống kê từ thanh điều hướng dưới cùng để xem biểu đồ và phân tích chi tiêu.",
                        primaryColor = primaryColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    FAQItemExpandable(
                        question = "Làm thế nào để đăng xuất?",
                        answer = "Vào màn hình Cài đặt, cuộn xuống và chọn 'Đăng xuất'.",
                        primaryColor = primaryColor
                    )
                }
            }

            // Liên hệ hỗ trợ
            item {
                SettingsCard(title = "Liên hệ hỗ trợ") {
                    ContactItem(
                        icon = Icons.Default.Email,
                        title = "Email hỗ trợ",
                        value = "Wendy84205@gmail.com",
                        description = "Phản hồi trong 24h",
                        color = primaryColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    ContactItem(
                        icon = Icons.Default.Language,
                        title = "Website",
                        value = "",
                        description = "Hướng dẫn chi tiết",
                        color = Color(0xFF4CAF50)
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    ContactItem(
                        icon = Icons.Default.AccessTime,
                        title = "Giờ làm việc",
                        value = "Thứ 2 - Thứ 6",
                        description = "8:00 - 17:00",
                        color = Color(0xFFFF9800)
                    )
                }
            }

            // Mẹo sử dụng
            item {
                SettingsCard(title = "Mẹo sử dụng") {
                    TipItem(
                        text = "Phân loại chi tiêu vào đúng danh mục để thống kê chính xác",
                        primaryColor = primaryColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    TipItem(
                        text = "Thiết lập ngân sách hàng tháng để kiểm soát chi tiêu",
                        primaryColor = primaryColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    TipItem(
                        text = "Sử dụng tính năng chi tiêu định kỳ cho các khoản chi cố định",
                        primaryColor = primaryColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    TipItem(
                        text = "Xem thống kê hàng tuần để điều chỉnh chi tiêu hợp lý",
                        primaryColor = primaryColor
                    )
                }
            }

            // Thông tin ứng dụng
            item {
                SettingsCard(title = "Thông tin ứng dụng") {
                    InfoItem(
                        title = "Phiên bản",
                        value = "1.0.0",
                        color = subtitleColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    InfoItem(
                        title = "Ngày phát hành",
                        value = "Tháng 12, 2025",
                        color = subtitleColor
                    )

                    Divider(color = Color(0xFFEEEEEE))

                    InfoItem(
                        title = "Nhà phát triển",
                        value = "Finance App Team",
                        color = subtitleColor
                    )
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
private fun SettingsCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
                )
            }
            content()
        }
    }
}

@Composable
private fun FAQItemExpandable(
    question: String,
    answer: String,
    primaryColor: Color = Color(0xFF2196F3)
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Surface(
        onClick = { expanded = !expanded },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(primaryColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Help,
                        contentDescription = "Câu hỏi",
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = question,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Thu gọn" else "Mở rộng",
                    tint = Color(0xFF666666),
                    modifier = Modifier.rotate(rotation)
                )
            }

            if (expanded) {
                Text(
                    text = answer,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(start = 64.dp, end = 20.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    description: String,
    color: Color
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
            Text(
                title,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                value,
                fontSize = 15.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            description,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
private fun TipItem(
    text: String,
    primaryColor: Color = Color(0xFF2196F3)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(primaryColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "💡",
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF333333),
            modifier = Modifier.weight(1f),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun InfoItem(
    title: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color(0xFF333333),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontSize = 15.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}