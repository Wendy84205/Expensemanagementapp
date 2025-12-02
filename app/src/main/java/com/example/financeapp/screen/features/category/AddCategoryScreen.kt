package com.example.financeapp.screen.features.category

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.financeapp.viewmodel.transaction.Category
import com.example.financeapp.viewmodel.transaction.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    navController: NavController,
    viewModel: CategoryViewModel
) {
    val categories by viewModel.categories.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTypes = listOf("expense", "income")

    var categoryName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedIcon by remember { mutableStateOf("📁") }
    var selectedMainCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

    val mainCategories = remember(selectedTab, categories) {
        categories.filter { it.isMainCategory && it.type == tabTypes[selectedTab] }
    }

    // Colors
    val primaryColor = Color(0xFF2196F3)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White
    val textColor = Color(0xFF333333)
    val subtitleColor = Color(0xFF666666)

    val isFormValid = categoryName.text.isNotBlank() && selectedMainCategory != null

    LaunchedEffect(mainCategories) {
        Log.d(
            "AddCategory",
            "Có ${mainCategories.size} danh mục chính (tab: ${tabTypes[selectedTab]})"
        )
    }

    Scaffold(
        topBar = {
            SimpleTopAppBar(
                title = "Thêm danh mục",
                onBackClick = { navController.popBackStack() }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardColor)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (categoryName.text.isNotBlank() && selectedMainCategory != null) {
                            viewModel.addCategory(
                                name = categoryName.text,
                                type = tabTypes[selectedTab],
                                isMainCategory = false,
                                parentCategoryId = selectedMainCategory?.id,
                                icon = selectedIcon
                            )
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        disabledContainerColor = Color(0xFFCCCCCC)
                    )
                ) {
                    Text(
                        "THÊM DANH MỤC",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
        ) {
            // Tab Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = primaryColor,
                    modifier = Modifier.fillMaxWidth(),
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            selectedMainCategory = null
                            categoryName = TextFieldValue("")
                        },
                        text = {
                            Text(
                                text = "Chi tiêu",
                                color = if (selectedTab == 0) primaryColor else subtitleColor,
                                fontWeight = if (selectedTab == 0) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            selectedMainCategory = null
                            categoryName = TextFieldValue("")
                        },
                        text = {
                            Text(
                                text = "Thu nhập",
                                color = if (selectedTab == 1) primaryColor else subtitleColor,
                                fontWeight = if (selectedTab == 1) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Main Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "Thông tin danh mục",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    // Icon selection
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFF8F9FA), CircleShape)
                                .clip(CircleShape)
                                .clickable { showIconPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                selectedIcon,
                                fontSize = 36.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Nhấn để đổi icon",
                            color = primaryColor,
                            fontSize = 12.sp
                        )
                    }

                    // Category name
                    Column {
                        Text(
                            "Tên danh mục",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = { if (it.text.length <= 30) categoryName = it },
                            placeholder = { Text("Ví dụ: Ăn uống, Mua sắm...", color = subtitleColor) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFDDDDDD),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = primaryColor
                            ),
                            singleLine = true
                        )
                    }

                    // Parent category selection
                    Column {
                        Text(
                            "Nhóm danh mục",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF8F9FA)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFDDDDDD))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedMainCategory != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFFEEEEEE), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            selectedMainCategory?.icon ?: "📁",
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Text(
                                    selectedMainCategory?.name ?: "Chọn nhóm danh mục",
                                    color = if (selectedMainCategory != null) textColor else subtitleColor,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Chọn danh mục",
                                    tint = subtitleColor
                                )
                            }
                        }
                    }

                    // Form status
                    if (isFormValid) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E8)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color(0xFF4CAF50), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Sẵn sàng thêm danh mục",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Category Selection Dialog
    if (showCategoryDialog) {
        SimpleCategoryDialog(
            mainCategories = mainCategories,
            selectedMainCategory = selectedMainCategory,
            onCategorySelected = { category ->
                selectedMainCategory = category
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false },
            primaryColor = primaryColor
        )
    }

    // Icon Picker Dialog
    if (showIconPicker) {
        SimpleIconDialog(
            selectedIcon = selectedIcon,
            onIconSelected = { icon ->
                selectedIcon = icon
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false },
            primaryColor = primaryColor
        )
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
private fun SimpleCategoryDialog(
    mainCategories: List<Category>,
    selectedMainCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ĐÓNG", color = primaryColor, fontWeight = FontWeight.Medium)
            }
        },
        title = {
            Text(
                "Chọn nhóm danh mục",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (mainCategories.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = "Không có danh mục",
                            tint = Color(0xFFCCCCCC),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Chưa có nhóm danh mục nào",
                            color = Color(0xFF666666),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    mainCategories.forEach { category ->
                        CategoryOption(
                            category = category,
                            isSelected = selectedMainCategory?.id == category.id,
                            onClick = { onCategorySelected(category) },
                            primaryColor = primaryColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun CategoryOption(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) primaryColor.copy(alpha = 0.1f) else Color(0xFFF8F9FA),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) primaryColor else Color(0xFFEEEEEE)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFEEEEEE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    category.icon,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                category.name,
                color = if (isSelected) primaryColor else Color(0xFF333333),
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SimpleIconDialog(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color
) {
    val commonIcons = listOf(
        "🍽️", "🛍️", "🚗", "🏠", "💄", "🎮", "🏥", "❤️",
        "🧾", "👨‍👩‍👧‍👦", "📊", "🎓", "💵", "🎁", "📈", "💼",
        "☕", "🍕", "🍔", "🎬", "🎵", "📱", "💻", "✈️"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ĐÓNG", color = primaryColor, fontWeight = FontWeight.Medium)
            }
        },
        title = {
            Text(
                "Chọn icon",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(commonIcons.chunked(4)) { rowIcons ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowIcons.forEach { icon ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (icon == selectedIcon) primaryColor.copy(alpha = 0.1f) else Color(0xFFF8F9FA),
                                            CircleShape
                                        )
                                        .clickable { onIconSelected(icon) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        icon,
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}