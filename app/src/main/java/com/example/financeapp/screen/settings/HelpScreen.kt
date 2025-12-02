package com.example.financeapp

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// 🎨 Màu theo HomeScreen
private val Navy = Color(0xFF0F4C75)
private val SoftGray = Color(0xFFF5F7FA)
private val TextDark = Color(0xFF2D3748)
private val TextLight = Color(0xFF718096)
private val AccentOrange = Color(0xFFED8936)
private val Green = Color(0xFF2E8B57)

// =================== Help Screen ===================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavController) {
    val languageViewModel = LocalLanguageViewModel.current

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF5F7FA)) // Giống HomeScreen
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        languageViewModel.getTranslation("help_support"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark // Màu chữ tối
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = languageViewModel.getTranslation("back"),
                            tint = TextDark // Màu icon tối
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White, // Nền trắng
                    titleContentColor = TextDark,
                    navigationIconContentColor = TextDark
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient) // Gradient giống HomeScreen
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                // ================= FAQ =================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp), clip = true), // Shadow nhẹ hơn
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White) // Nền trắng
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "❓ ${languageViewModel.getTranslation("faq")}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Navy // Màu Navy giống HomeScreen
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        FAQItemExpandable(
                            languageViewModel.getTranslation("faq_add_transaction"),
                            languageViewModel.getTranslation("faq_add_transaction_answer")
                        )
                        FAQItemExpandable(
                            languageViewModel.getTranslation("faq_add_wallet"),
                            languageViewModel.getTranslation("faq_add_wallet_answer")
                        )
                        FAQItemExpandable(
                            languageViewModel.getTranslation("faq_view_statistics"),
                            languageViewModel.getTranslation("faq_view_statistics_answer")
                        )
                        FAQItemExpandable(
                            languageViewModel.getTranslation("faq_logout"),
                            languageViewModel.getTranslation("faq_logout_answer")
                        )
                        FAQItemExpandable(
                            languageViewModel.getTranslation("faq_change_theme"),
                            languageViewModel.getTranslation("faq_change_theme_answer")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ================= Contact =================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp), clip = true),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "📞 ${languageViewModel.getTranslation("contact_support")}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            languageViewModel.getTranslation("contact_description"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLight, // Màu text phụ
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ContactInfoItem(
                            "📧 ${languageViewModel.getTranslation("email")}",
                            "support@wendyai.com",
                            languageViewModel.getTranslation("response_time")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ContactInfoItem(
                            "🌐 ${languageViewModel.getTranslation("website")}",
                            "www.wendyai.com",
                            languageViewModel.getTranslation("detailed_guide")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ContactInfoItem(
                            "🕒 ${languageViewModel.getTranslation("working_hours")}",
                            languageViewModel.getTranslation("weekdays"),
                            languageViewModel.getTranslation("working_time")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ================= Tips =================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp), clip = true),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "💡 ${languageViewModel.getTranslation("usage_tips")}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TipItem(languageViewModel.getTranslation("tip_categories"))
                        TipItem(languageViewModel.getTranslation("tip_savings"))
                        TipItem(languageViewModel.getTranslation("tip_statistics"))
                        TipItem(languageViewModel.getTranslation("tip_reminders"))
                    }
                }
            }
        }
    }
}

// ================= Expandable FAQ Item =================
@Composable
private fun FAQItemExpandable(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.White, shape = RoundedCornerShape(12.dp)) // Nền trắng
            .animateContentSize()
            .shadow(2.dp, RoundedCornerShape(12.dp)) // Shadow nhẹ
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextDark, // Màu chữ tối
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Navy, // Màu Navy
                modifier = Modifier.rotate(rotation)
            )
        }

        if (expanded) {
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight, // Màu text phụ
                lineHeight = 20.sp,
                modifier = Modifier.padding(start = 12.dp, bottom = 16.dp, end = 12.dp)
            )
        }
    }
}

// ================= Contact Item =================
@Composable
private fun ContactInfoItem(type: String, value: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = type,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextDark // Màu chính
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Navy, // Màu Navy cho giá trị
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextLight // Màu phụ
            )
        }
    }
}

// ================= Tips Item =================
@Composable
private fun TipItem(tip: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•",
            color = Navy, // Màu Navy
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = tip,
            style = MaterialTheme.typography.bodyMedium,
            color = TextDark, // Màu chính
            modifier = Modifier.weight(1f),
            lineHeight = 20.sp
        )
    }
}